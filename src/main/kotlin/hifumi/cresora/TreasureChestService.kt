package hifumi.cresora

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.fabricmc.fabric.api.event.player.UseBlockCallback
import net.minecraft.block.Block
import net.minecraft.block.BlockState
import net.minecraft.block.Blocks
import net.minecraft.entity.player.PlayerEntity
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
    private const val INITIAL_DELAY_TICKS = 20L * 45L
    private const val RESPAWN_DELAY_TICKS = 20L * 75L
    private const val MAX_ACTIVE_CHESTS_PER_PLAYER = 5
    private const val MIN_RADIUS = 7
    private const val MAX_RADIUS = 15
    private const val SPAWN_ATTEMPTS = 24

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
        val reward: ChestReward
    )

    private val rewards = listOf(
        ChestReward(3, 400, 100, 70, Blocks.CHEST, ParticleTypes.HAPPY_VILLAGER),
        ChestReward(4, 1_000, 125, 22, Blocks.TRAPPED_CHEST, ParticleTypes.ENCHANT),
        ChestReward(5, 1_500, 150, 8, Blocks.ENDER_CHEST, ParticleTypes.END_ROD)
    )

    private val activeKeysByOwner: MutableMap<UUID, MutableSet<ChestKey>> = linkedMapOf()
    private val activeByKey: MutableMap<ChestKey, ActiveChest> = linkedMapOf()
    private val nextSpawnTickByPlayer: MutableMap<UUID, Long> = linkedMapOf()
    private var persistentState: TreasureChestPersistentState? = null
    private var stateLoaded: Boolean = false

    fun init() {
        ServerTickEvents.END_SERVER_TICK.register(ServerTickEvents.EndTick { server ->
            tick(server)
        })

        UseBlockCallback.EVENT.register(UseBlockCallback { player, world, hand, hitResult ->
            onUseBlock(player, world, hand, hitResult)
        })
    }

    private fun tick(server: MinecraftServer) {
        ensureStateLoaded(server)
        val now = server.overworld.time
        cleanup(server)
        for (player in server.playerManager.playerList) {
            maybeSpawnFor(player, now)
        }
        if (now % 20L == 0L) {
            emitParticles(server)
        }
    }

    private fun cleanup(server: MinecraftServer) {
        val iterator = activeByKey.values.iterator()
        while (iterator.hasNext()) {
            val chest = iterator.next()
            val world = server.getWorld(chest.key.worldKey)
            val shouldRemove = world == null || !isOurChestBlock(world.getBlockState(chest.key.pos).block)
            if (!shouldRemove) {
                continue
            }
            iterator.remove()
            unregisterChest(chest)
        }
    }

    private fun emitParticles(server: MinecraftServer) {
        for (chest in activeByKey.values) {
            val world = server.getWorld(chest.key.worldKey) ?: continue
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

    private fun maybeSpawnFor(player: ServerPlayerEntity, now: Long) {
        if (activeChestCount(player.uuid) >= MAX_ACTIVE_CHESTS_PER_PLAYER) {
            return
        }
        val playerWorld = player.world as ServerWorld
        val nextAllowed = nextSpawnTickByPlayer[player.uuid]
        if (nextAllowed == null) {
            nextSpawnTickByPlayer[player.uuid] = now + INITIAL_DELAY_TICKS
            return
        }
        if (now < nextAllowed) {
            return
        }
        val pos = findSpawnPosition(playerWorld, player) ?: run {
            nextSpawnTickByPlayer[player.uuid] = now + 20L * 15L
            return
        }
        val reward = rollReward(player)
        if (!placeChest(playerWorld, pos, player.horizontalFacing.opposite, reward.block)) {
            nextSpawnTickByPlayer[player.uuid] = now + 20L * 15L
            return
        }
        val chest = ActiveChest(
            ownerId = player.uuid,
            key = ChestKey(playerWorld.registryKey, pos.toImmutable()),
            reward = reward
        )
        registerChest(chest)
        nextSpawnTickByPlayer[player.uuid] = now + RESPAWN_DELAY_TICKS
        player.sendMessage(
            Text.translatable(
                "message.cresora.treasure_chest.spawned",
                reward.stars,
                pos.x,
                pos.y,
                pos.z
            ).formatted(Formatting.GOLD),
            false
        )
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
            val world = server.getWorld(restored.key.worldKey) ?: continue
            if (!isOurChestBlock(world.getBlockState(restored.key.pos).block)) {
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
            reward = reward
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
                    chordProgression = chest.reward.chordProgression
                )
            }
        )
        state.markDirty()
    }
}
