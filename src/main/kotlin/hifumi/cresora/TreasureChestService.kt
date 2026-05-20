package hifumi.cresora

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.fabricmc.fabric.api.event.player.UseBlockCallback
import net.fabricmc.fabric.api.event.player.UseItemCallback
import net.minecraft.block.Block
import net.minecraft.block.BlockState
import net.minecraft.block.Blocks
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemStack
import net.minecraft.particle.ParticleTypes
import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.server.world.ServerWorld
import net.minecraft.sound.SoundCategory
import net.minecraft.sound.SoundEvents
import net.minecraft.state.property.Properties
import net.minecraft.text.Text
import net.minecraft.util.ActionResult
import net.minecraft.util.Formatting
import net.minecraft.util.Hand
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.util.math.Vec3d
import net.minecraft.world.Heightmap
import net.minecraft.world.World
import net.minecraft.registry.RegistryKey
import net.minecraft.registry.RegistryKeys
import net.minecraft.util.Identifier
import java.util.UUID
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

object TreasureChestService {
    private const val MAX_ACTIVE_CHESTS_PER_PLAYER = 5
    private const val MIN_RADIUS = 10
    private const val MAX_RADIUS = 25
    private const val SPAWN_ATTEMPTS = 32

    private data class ChestReward(
        val stars: Int,
        val credits: Int,
        val chordProgression: Int,
        val weight: Int,
        val block: Block,
        val particle: net.minecraft.particle.ParticleEffect
    )

    private data class ChestKey(
        val worldKey: net.minecraft.registry.RegistryKey<World>,
        val pos: BlockPos
    )

    private data class ActiveChest(
        val ownerId: UUID,
        val key: ChestKey,
        val reward: ChestReward,
        val expireTime: Long
    )

    // Using our custom block for all chests to completely prevent conflict with vanilla player chests
    private val rewards = listOf(
        ChestReward(3, 400, 100, 70, CreSoraUtilities.RESONANT_CACHE_BLOCK, ParticleTypes.HAPPY_VILLAGER),
        ChestReward(4, 1_000, 125, 22, CreSoraUtilities.RESONANT_CACHE_BLOCK, ParticleTypes.ENCHANT),
        ChestReward(5, 1_500, 150, 8, CreSoraUtilities.RESONANT_CACHE_BLOCK, ParticleTypes.END_ROD)
    )

    private val activeKeysByOwner: MutableMap<UUID, MutableSet<ChestKey>> = linkedMapOf()
    private val activeByKey: MutableMap<ChestKey, ActiveChest> = linkedMapOf()
    private var persistentState: TreasureChestPersistentState? = null
    private var stateLoaded: Boolean = false

    fun init() {
        ServerTickEvents.END_SERVER_TICK.register(ServerTickEvents.EndTick { server ->
            tick(server)
        })

        UseBlockCallback.EVENT.register(UseBlockCallback { player, world, hand, hitResult ->
            onUseBlock(player, world, hand, hitResult)
        })

        UseItemCallback.EVENT.register(UseItemCallback { player, world, hand ->
            onUseItem(player, world, hand)
        })
    }

    private fun tick(server: MinecraftServer) {
        ensureStateLoaded(server)
        val now = server.overworld.time
        cleanup(server)
        if (now % 20L == 0L) {
            emitParticles(server)
        }
    }

    private fun cleanup(server: MinecraftServer) {
        val iterator = activeByKey.values.iterator()
        val toRemove = mutableListOf<ActiveChest>()
        val now = server.overworld.time

        while (iterator.hasNext()) {
            val chest = iterator.next()
            val world = server.getWorld(chest.key.worldKey)
            val isExpired = chest.expireTime != 0L && now >= chest.expireTime
            val shouldRemove = world == null || isExpired || (isChunkLoaded(world, chest.key.pos) && !isOurChestBlock(world.getBlockState(chest.key.pos).block))
            if (shouldRemove) {
                iterator.remove() // Safely remove from activeByKey using iterator
                toRemove.add(chest)
            }
        }

        // Safely unregister and clear block outside of the activeByKey iterator loop to prevent ConcurrentModificationException
        for (chest in toRemove) {
            val world = server.getWorld(chest.key.worldKey)
            if (world != null) {
                // If it expired, only remove the chest block from the world if the chunk is currently loaded.
                // This prevents remote force-loading of chunks (needless chunk I/O) in the hot tick loop.
                if (isChunkLoaded(world, chest.key.pos)) {
                    val state = world.getBlockState(chest.key.pos)
                    if (isOurChestBlock(state.block)) {
                        world.setBlockState(chest.key.pos, Blocks.AIR.defaultState)
                    }
                }
            }
            unregisterChest(chest)
        }
    }

    fun clearTransientState(player: ServerPlayerEntity) {
        // Passive timer is deleted, this remains as a registry cleanup placeholder if needed
    }

    private fun emitParticles(server: MinecraftServer) {
        for (chest in activeByKey.values) {
            val world = server.getWorld(chest.key.worldKey) ?: continue
            if (!isChunkLoaded(world, chest.key.pos)) {
                continue
            }
            if (!isOurChestBlock(world.getBlockState(chest.key.pos).block)) {
                continue
            }
            world.spawnParticles(
                chest.reward.particle,
                chest.key.pos.x + 0.5,
                chest.key.pos.y + 1.05,
                chest.key.pos.z + 0.5,
                6,
                0.25,
                0.15,
                0.25,
                0.0
            )
        }
    }

    private fun onUseItem(player: PlayerEntity, world: World, hand: Hand): ActionResult {
        val stack = player.getStackInHand(hand)
        if (world.isClient || stack.item != CreSoraUtilities.RESONANT_LOCATOR_ITEM) {
            return ActionResult.PASS
        }

        val serverPlayer = player as? ServerPlayerEntity ?: return ActionResult.PASS
        val server = serverPlayer.server ?: return ActionResult.PASS
        ensureStateLoaded(server)

        if (serverPlayer.itemCooldownManager.isCoolingDown(stack)) {
            return ActionResult.FAIL
        }

        val playerWorld = serverPlayer.world as ServerWorld
        if (playerWorld.registryKey == ArenaManager.DOMAIN_WORLD_KEY) {
            serverPlayer.sendMessage(Text.translatable("message.cresora.treasure_chest.cannot_use_here").formatted(Formatting.RED), true)
            return ActionResult.FAIL
        }

        if (activeChestCount(serverPlayer.uuid) >= MAX_ACTIVE_CHESTS_PER_PLAYER) {
            serverPlayer.sendMessage(Text.translatable("message.cresora.treasure_chest.too_many").formatted(Formatting.RED), true)
            return ActionResult.FAIL
        }

        val pos = findSpawnPosition(playerWorld, serverPlayer)
        if (pos == null) {
            serverPlayer.sendMessage(Text.translatable("message.cresora.treasure_chest.no_safe_spot").formatted(Formatting.RED), true)
            return ActionResult.FAIL
        }

        val reward = rollReward(serverPlayer)
        if (!placeChest(playerWorld, pos, serverPlayer.horizontalFacing.opposite, reward.block)) {
            serverPlayer.sendMessage(Text.translatable("message.cresora.treasure_chest.placement_failed").formatted(Formatting.RED), true)
            return ActionResult.FAIL
        }

        // Chest has a dynamic lifespan (TTL) of 10 minutes (12000 ticks)
        val expireTicks = 20L * 60L * 10L
        val expireTime = playerWorld.time + expireTicks

        val chest = ActiveChest(
            ownerId = serverPlayer.uuid,
            key = ChestKey(playerWorld.registryKey, pos.toImmutable()),
            reward = reward,
            expireTime = expireTime
        )
        registerChest(chest)

        if (!serverPlayer.isCreative) {
            stack.decrement(1)
        }

        // Set a 12-second cooldown to prevent locator spawn spam
        serverPlayer.itemCooldownManager.set(stack, 20 * 12)

        playerWorld.playSound(
            null,
            pos,
            SoundEvents.BLOCK_BEACON_ACTIVATE,
            SoundCategory.PLAYERS,
            1.0f,
            1.2f
        )

        serverPlayer.sendMessage(
            Text.translatable(
                "message.cresora.treasure_chest.spawned",
                reward.stars,
                pos.x,
                pos.y,
                pos.z
            ).formatted(Formatting.GOLD),
            false
        )

        // Draw a beautiful high-fidelity guide path particle trail from the player eye to the newly spawned cache coordinates!
        spawnGuideTrail(playerWorld, serverPlayer.eyePos, pos)

        return ActionResult.SUCCESS
    }

    private fun spawnGuideTrail(world: ServerWorld, start: Vec3d, endPos: BlockPos) {
        val end = Vec3d(endPos.x + 0.5, endPos.y + 0.5, endPos.z + 0.5)
        val diff = end.subtract(start)
        val steps = (diff.length() * 2.0).roundToInt().coerceAtLeast(6)
        for (i in 0..steps) {
            val progress = i.toDouble() / steps.toDouble()
            val point = start.add(diff.multiply(progress))
            world.spawnParticles(
                ParticleTypes.END_ROD,
                point.x,
                point.y,
                point.z,
                1,
                0.0,
                0.0,
                0.0,
                0.0
            )
        }
    }

    private fun onUseBlock(
        player: PlayerEntity,
        world: World,
        hand: Hand,
        hitResult: BlockHitResult
    ): ActionResult {
        if (world.isClient || hand != Hand.MAIN_HAND) {
            return ActionResult.PASS
        }
        val serverPlayer = player as? ServerPlayerEntity ?: return ActionResult.PASS
        val server = serverPlayer.server ?: return ActionResult.PASS
        ensureStateLoaded(server)
        val playerWorld = serverPlayer.world as ServerWorld
        val key = ChestKey(playerWorld.registryKey, hitResult.blockPos.toImmutable())
        val chest = activeByKey[key] ?: return ActionResult.PASS

        // Strict ownership check to prevent chest theft by other players
        if (chest.ownerId != serverPlayer.uuid) {
            val ownerName = server.playerManager.getPlayer(chest.ownerId)?.name?.string ?: "其他玩家"
            serverPlayer.sendMessage(
                Text.translatable("message.cresora.treasure_chest.not_owner", ownerName).formatted(Formatting.RED),
                true
            )
            return ActionResult.FAIL
        }

        val creditsTotal = CreditsService.addCredits(serverPlayer, chest.reward.credits)
        val chordTotal = ResonanceService.addCurrency(serverPlayer, ResonanceCurrencyType.CHORD_PROGRESSION, chest.reward.chordProgression)
        removeChestBlock(playerWorld, chest)
        unregisterChest(chest)
        serverPlayer.sendMessage(
            Text.translatable(
                "message.cresora.treasure_chest.opened",
                chest.reward.stars,
                chest.reward.credits,
                chest.reward.chordProgression,
                ArtifactSpecialItem.formatWholeNumber(creditsTotal),
                ArtifactSpecialItem.formatWholeNumber(chordTotal)
            ).formatted(Formatting.AQUA),
            false
        )
        playerWorld.playSound(
            null,
            chest.key.pos,
            SoundEvents.UI_TOAST_CHALLENGE_COMPLETE,
            SoundCategory.PLAYERS,
            0.85f,
            1.1f
        )
        return ActionResult.SUCCESS
    }

    private fun activeChestCount(ownerId: UUID): Int {
        return activeKeysByOwner[ownerId]?.size ?: 0
    }

    private fun registerChest(chest: ActiveChest) {
        activeByKey[chest.key] = chest
        activeKeysByOwner.getOrPut(chest.ownerId) { linkedSetOf() }.add(chest.key)
        syncPersistentState()
    }

    private fun unregisterChest(chest: ActiveChest) {
        activeByKey.remove(chest.key)
        val keys = activeKeysByOwner[chest.ownerId]
        if (keys != null) {
            keys.remove(chest.key)
            if (keys.isEmpty()) {
                activeKeysByOwner.remove(chest.ownerId)
            }
        }
        syncPersistentState()
    }

    private fun removeChestBlock(world: ServerWorld, chest: ActiveChest) {
        if (isOurChestBlock(world.getBlockState(chest.key.pos).block)) {
            world.setBlockState(chest.key.pos, Blocks.AIR.defaultState)
        }
    }

    private fun isOurChestBlock(block: Block): Boolean {
        return rewards.any { it.block == block }
    }

    private fun rollReward(player: ServerPlayerEntity): ChestReward {
        val totalWeight = rewards.sumOf(ChestReward::weight)
        var roll = player.random.nextInt(totalWeight.coerceAtLeast(1))
        for (reward in rewards) {
            roll -= reward.weight
            if (roll < 0) {
                return reward
            }
        }
        return rewards.first()
    }

    private fun placeChest(world: ServerWorld, pos: BlockPos, facing: Direction, block: Block): Boolean {
        if (!canPlaceChest(world, pos)) {
            return false
        }
        var state: BlockState = block.defaultState
        if (state.contains(Properties.HORIZONTAL_FACING)) {
            state = state.with(Properties.HORIZONTAL_FACING, facing)
        }
        return world.setBlockState(pos, state)
    }

    private fun canPlaceChest(world: ServerWorld, pos: BlockPos): Boolean {
        if (!world.worldBorder.contains(pos)) {
            return false
        }
        val floorPos = pos.down()
        if (!world.getFluidState(pos).isEmpty || !world.getFluidState(pos.up()).isEmpty) {
            return false
        }
        return world.getBlockState(pos).isAir &&
            world.getBlockState(pos.up()).isAir &&
            world.getBlockState(floorPos).isSideSolidFullSquare(world, floorPos, Direction.UP)
    }

    private fun findSpawnPosition(world: ServerWorld, player: ServerPlayerEntity): BlockPos? {
        repeat(SPAWN_ATTEMPTS) {
            val angle = player.random.nextDouble() * (PI * 2.0)
            val radius = player.random.nextBetween(MIN_RADIUS, MAX_RADIUS)
            val x = (player.x + cos(angle) * radius.toDouble()).roundToInt()
            val z = (player.z + sin(angle) * radius.toDouble()).roundToInt()
            val playerY = player.blockPos.y
            val local = findNearbyStandingSpot(world, x, z, playerY)
            if (local != null) {
                return local
            }
            val surfaceY = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z)
            val surfacePos = BlockPos(x, surfaceY, z)
            if (canPlaceChest(world, surfacePos)) {
                return surfacePos
            }
        }
        return null
    }

    private fun findNearbyStandingSpot(world: ServerWorld, x: Int, z: Int, centerY: Int): BlockPos? {
        for (y in (centerY + 4) downTo (centerY - 8)) {
            val candidate = BlockPos(x, y, z)
            if (canPlaceChest(world, candidate)) {
                return candidate
            }
        }
        return null
    }

    private fun ensureStateLoaded(server: MinecraftServer) {
        if (stateLoaded) {
            return
        }
        val manager = server.overworld.persistentStateManager
        val state = manager.getOrCreate(TreasureChestPersistentState.TYPE)
        persistentState = state
        restoreFromPersistentState(server, state)
        stateLoaded = true
    }

    private fun restoreFromPersistentState(server: MinecraftServer, state: TreasureChestPersistentState) {
        activeKeysByOwner.clear()
        activeByKey.clear()
        for (savedChest in state.chests) {
            val restored = restoreChest(savedChest) ?: continue
            if (server.getWorld(restored.key.worldKey) == null) {
                continue
            }
            activeByKey[restored.key] = restored
            activeKeysByOwner.getOrPut(restored.ownerId) { linkedSetOf() }.add(restored.key)
        }
        syncPersistentState()
    }

    private fun restoreChest(savedChest: SavedTreasureChest): ActiveChest? {
        val ownerId = runCatching { UUID.fromString(savedChest.ownerUuid) }.getOrNull() ?: return null
        val worldId = runCatching { Identifier.of(savedChest.worldId) }.getOrNull() ?: return null
        val reward = restoreReward(savedChest)
        return ActiveChest(
            ownerId = ownerId,
            key = ChestKey(RegistryKey.of(RegistryKeys.WORLD, worldId), BlockPos(savedChest.x, savedChest.y, savedChest.z)),
            reward = reward,
            expireTime = savedChest.expireTime
        )
    }

    private fun restoreReward(savedChest: SavedTreasureChest): ChestReward {
        val template = rewards.firstOrNull { it.stars == savedChest.stars } ?: rewards.first()
        return ChestReward(
            stars = savedChest.stars,
            credits = savedChest.credits,
            chordProgression = savedChest.chordProgression,
            weight = template.weight,
            block = template.block,
            particle = template.particle
        )
    }

    private fun syncPersistentState() {
        val state = persistentState ?: return
        state.chests.clear()
        state.chests.addAll(
            activeByKey.values.map { chest ->
                SavedTreasureChest(
                    ownerUuid = chest.ownerId.toString(),
                    worldId = chest.key.worldKey.value.toString(),
                    x = chest.key.pos.x,
                    y = chest.key.pos.y,
                    z = chest.key.pos.z,
                    stars = chest.reward.stars,
                    credits = chest.reward.credits,
                    chordProgression = chest.reward.chordProgression,
                    expireTime = chest.expireTime
                )
            }
        )
        state.markDirty()
    }

    private fun isChunkLoaded(world: ServerWorld, pos: BlockPos): Boolean {
        return world.chunkManager.isChunkLoaded(pos.x shr 4, pos.z shr 4)
    }
}
