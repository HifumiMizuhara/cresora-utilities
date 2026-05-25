package hifumi.cresora.treasure

import hifumi.cresora.CreSoraUtilities
import hifumi.cresora.weapon.WeaponRarity
import hifumi.cresora.combat.ArenaManager
import hifumi.cresora.combat.FieldMobPackService
import hifumi.cresora.credits.CreditsService
import hifumi.cresora.resonance.ResonanceService
import hifumi.cresora.resonance.ResonanceCurrencyType
import hifumi.cresora.equipment.ArtifactSpecialItem
import hifumi.cresora.equipment.EquipmentContentRegistry
import hifumi.cresora.equipment.EquipmentStackSupport
import hifumi.cresora.equipment.EquipmentRarity
import hifumi.cresora.equipment.EquipmentGenerationService
import hifumi.cresora.adventurerank.AdventureRankService
import hifumi.cresora.adventurerank.AdventureRankMobAccess
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.fabricmc.fabric.api.event.player.UseBlockCallback
import net.fabricmc.fabric.api.event.player.UseItemCallback
import net.minecraft.block.Block
import net.minecraft.block.BlockState
import net.minecraft.block.Blocks
import net.minecraft.entity.decoration.DisplayEntity
import net.minecraft.entity.EntityType
import net.minecraft.entity.mob.HostileEntity
import net.minecraft.entity.SpawnReason
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
import net.minecraft.registry.Registries
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

    private const val TRIGGER_DISTANCE_SQ = 36.0 // 6 blocks
    private const val RESET_DISTANCE_SQ = 1024.0 // 32 blocks
    private const val ELITE_HEALTH_SCALAR = 2.5
    private const val ELITE_DEFENSE_SCALAR = 2.0

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

    private data class ChestChallenge(
        val chestKey: ChestKey,
        val ownerId: UUID,
        val stars: Int,
        val guardianUuids: MutableSet<UUID> = linkedSetOf(),
        var displayEntityUuid: UUID? = null,
        var triggered: Boolean = false,
        var completed: Boolean = false
    )

    // Using our custom block for all chests to completely prevent conflict with vanilla player chests
    private val rewards = listOf(
        ChestReward(3, 400, 100, 70, CreSoraUtilities.RESONANT_CACHE_BLOCK, ParticleTypes.HAPPY_VILLAGER),
        ChestReward(4, 1_000, 125, 22, CreSoraUtilities.RESONANT_CACHE_BLOCK, ParticleTypes.ENCHANT),
        ChestReward(5, 1_500, 150, 8, CreSoraUtilities.RESONANT_CACHE_BLOCK, ParticleTypes.END_ROD)
    )

    private val activeKeysByOwner: MutableMap<UUID, MutableSet<ChestKey>> = linkedMapOf()
    private val activeByKey: MutableMap<ChestKey, ActiveChest> = linkedMapOf()
    private val activeChallenges: MutableMap<ChestKey, ChestChallenge> = linkedMapOf()
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

        ServerLivingEntityEvents.AFTER_DEATH.register(ServerLivingEntityEvents.AfterDeath { entity, damageSource ->
            val killer = damageSource.attacker as? ServerPlayerEntity
            if (entity is HostileEntity) {
                onGuardianKilled(entity, killer)
            }
        })
    }

    private fun tick(server: MinecraftServer) {
        ensureStateLoaded(server)
        val now = server.overworld.time
        cleanup(server)
        if (now % 10L == 0L) {
            tickChallenges(server)
        }
        if (now % 20L == 0L) {
            emitParticles(server)
        }
    }

    private fun tickChallenges(server: MinecraftServer) {
        val now = server.overworld.time
        for (chest in activeByKey.values) {
            var challenge = activeChallenges[chest.key]

            if (challenge != null && challenge.completed) {
                if (now % 20L == 0L) {
                    val world = server.getWorld(chest.key.worldKey) ?: continue
                    if (isChunkLoaded(world, chest.key.pos)) {
                        world.spawnParticles(
                            ParticleTypes.HAPPY_VILLAGER,
                            chest.key.pos.x + 0.5,
                            chest.key.pos.y + 1.05,
                            chest.key.pos.z + 0.5,
                            3,
                            0.25,
                            0.1,
                            0.25,
                            0.0
                        )
                    }
                }
                continue
            }

            val world = server.getWorld(chest.key.worldKey) ?: continue
            if (!isChunkLoaded(world, chest.key.pos)) {
                continue
            }

            val owner = server.playerManager.getPlayer(chest.ownerId)

            if (challenge == null) {
                if (owner != null && owner.world.registryKey == chest.key.worldKey && owner.squaredDistanceTo(chest.key.pos.toCenterPos()) <= TRIGGER_DISTANCE_SQ) {
                    val newChallenge = ChestChallenge(chest.key, chest.ownerId, chest.reward.stars)
                    activeChallenges[chest.key] = newChallenge
                    triggerChallenge(server, world, chest, newChallenge, owner)
                }
            } else {
                val distanceSquared = owner?.let {
                    if (it.world.registryKey == chest.key.worldKey) it.squaredDistanceTo(chest.key.pos.toCenterPos()) else 99999.0
                } ?: 99999.0

                if (owner == null || !owner.isAlive || distanceSquared > RESET_DISTANCE_SQ) {
                    resetChallenge(server, world, challenge)
                } else {
                    val iterator = challenge.guardianUuids.iterator()
                    var remaining = 0
                    while (iterator.hasNext()) {
                        val uuid = iterator.next()
                        val entity = world.getEntity(uuid)
                        if (entity == null || !entity.isAlive || entity.isRemoved) {
                            iterator.remove()
                        } else {
                            remaining++
                            if (entity is HostileEntity && (entity.target == null || entity.target != owner)) {
                                entity.target = owner
                            }
                        }
                    }

                    if (remaining == 0) {
                        completeChallenge(server, world, chest, challenge, owner)
                    } else {
                        syncChallengeDisplay(world, chest, challenge, remaining)
                        spawnActiveChallengeParticles(world, chest.key.pos)
                    }
                }
            }
        }
    }

    private fun spawnActiveChallengeParticles(world: ServerWorld, pos: BlockPos) {
        val centerX = pos.x + 0.5
        val centerY = pos.y + 0.5
        val centerZ = pos.z + 0.5
        val radius = 1.5
        val steps = 8
        for (i in 0 until steps) {
            val angle = i * (PI * 2.0 / steps)
            val px = centerX + radius * cos(angle)
            val pz = centerZ + radius * sin(angle)
            world.spawnParticles(
                ParticleTypes.SOUL_FIRE_FLAME,
                px,
                centerY,
                pz,
                1,
                0.0,
                0.0,
                0.0,
                0.0
            )
        }
    }

    private fun triggerChallenge(server: MinecraftServer, world: ServerWorld, chest: ActiveChest, challenge: ChestChallenge, player: ServerPlayerEntity) {
        challenge.triggered = true
        val rank = AdventureRankService.getRank(player)
        val count = when (challenge.stars) {
            3 -> 3
            4 -> 3
            5 -> 4
            else -> 2
        }

        for (i in 0 until count) {
            val isElite = when (challenge.stars) {
                3 -> false
                4 -> i == 0
                5 -> i < 2
                else -> false
            }

            val type = if (isElite && challenge.stars == 5 && i == 0) {
                EntityType.WITHER_SKELETON
            } else if (world.random.nextBoolean()) {
                EntityType.ZOMBIE
            } else {
                EntityType.SKELETON
            }

            val guardian = spawnGuardian(world, type, chest.key.pos, player, isElite, rank)
            if (guardian != null) {
                challenge.guardianUuids.add(guardian.uuid)
            }
        }

        if (challenge.guardianUuids.isEmpty()) {
            challenge.triggered = false
            activeChallenges.remove(chest.key)
            player.sendMessage(
                Text.literal("§c[共鸣探索] 守护者唤醒失败，请重试或在更开阔的位置放置宝箱！"),
                false
            )
            return
        }

        world.playSound(
            null,
            chest.key.pos,
            SoundEvents.ENTITY_WITHER_SPAWN,
            SoundCategory.HOSTILE,
            0.6f,
            1.2f
        )

        player.sendMessage(
            Text.literal("§6[共鸣探索] §f守护者已被唤醒，击败它们以解锁共鸣宝箱！"),
            false
        )

        syncChallengeDisplay(world, chest, challenge, challenge.guardianUuids.size)
    }

    private fun spawnGuardian(world: ServerWorld, entityType: EntityType<out HostileEntity>, pos: BlockPos, player: ServerPlayerEntity, elite: Boolean, rank: Int): HostileEntity? {
        val spawnPos = findGuardianSpawnSpot(world, pos) ?: pos.up(2)
        val entity = entityType.create(world, null, spawnPos, SpawnReason.EVENT, true, false) ?: return null
        entity.refreshPositionAndAngles(spawnPos.x + 0.5, spawnPos.y + 0.05, spawnPos.z + 0.5, world.random.nextFloat() * 360f, 0f)

        val access = entity as? AdventureRankMobAccess
        if (access != null) {
            access.cresoraSetMobAdventureRank(rank)
            FieldMobPackService.markExplicit(entity, elite)
        }

        val healthScalar = if (elite) ELITE_HEALTH_SCALAR else 1.0
        val defenseScalar = if (elite) ELITE_DEFENSE_SCALAR else 1.0
        AdventureRankService.applyMobScaling(entity, rank, healthScalar, defenseScalar, defenseScalar)

        entity.isGlowing = true
        entity.target = player

        world.spawnEntity(entity)
        return entity
    }

    private fun findGuardianSpawnSpot(world: ServerWorld, chestPos: BlockPos): BlockPos? {
        for (attempt in 0..16) {
            val dx = world.random.nextInt(5) - 2
            val dz = world.random.nextInt(5) - 2
            if (dx == 0 && dz == 0) continue
            val candidate = chestPos.add(dx, 0, dz)
            for (dy in -3..3) {
                val finalPos = candidate.up(dy)
                if (world.getBlockState(finalPos).isAir &&
                    world.getBlockState(finalPos.up()).isAir &&
                    world.getBlockState(finalPos.down()).isSideSolidFullSquare(world, finalPos.down(), Direction.UP)) {
                    return finalPos
                }
            }
        }
        return null
    }

    private fun resetChallenge(server: MinecraftServer, world: ServerWorld, challenge: ChestChallenge) {
        challenge.displayEntityUuid?.let { uuid ->
            world.getEntity(uuid)?.discard()
        }
        challenge.displayEntityUuid = null

        challenge.guardianUuids.forEach { uuid ->
            world.getEntity(uuid)?.discard()
        }
        challenge.guardianUuids.clear()

        challenge.triggered = false
        challenge.completed = false
        activeChallenges.remove(challenge.chestKey)

        val owner = server.playerManager.getPlayer(challenge.ownerId)
        owner?.sendMessage(
            Text.literal("§c[共鸣探索] §f由于你离宝箱过远或不幸死亡，共鸣挑战已重置。"),
            false
        )
    }

    private fun completeChallenge(server: MinecraftServer, world: ServerWorld, chest: ActiveChest, challenge: ChestChallenge, player: ServerPlayerEntity) {
        challenge.completed = true

        challenge.displayEntityUuid?.let { uuid ->
            world.getEntity(uuid)?.discard()
        }
        challenge.displayEntityUuid = null

        world.playSound(
            null,
            chest.key.pos,
            SoundEvents.UI_TOAST_CHALLENGE_COMPLETE,
            SoundCategory.PLAYERS,
            0.85f,
            1.1f
        )

        world.spawnParticles(
            ParticleTypes.HAPPY_VILLAGER,
            chest.key.pos.x + 0.5,
            chest.key.pos.y + 1.25,
            chest.key.pos.z + 0.5,
            20,
            0.5,
            0.5,
            0.5,
            0.1
        )

        player.sendMessage(
            Text.literal("§6[共鸣探索] §a共鸣挑战成功！你可以开启宝箱了。"),
            false
        )
    }

    private fun syncChallengeDisplay(world: ServerWorld, chest: ActiveChest, challenge: ChestChallenge, remaining: Int) {
        val display = resolveChallengeDisplay(world, challenge) ?: createChallengeDisplay(world, chest, challenge, remaining)
        display.setText(Text.literal("§6[共鸣挑战] §f击败守护者！ §7(剩余: $remaining)"))
    }

    private fun resolveChallengeDisplay(world: ServerWorld, challenge: ChestChallenge): DisplayEntity.TextDisplayEntity? {
        val uuid = challenge.displayEntityUuid ?: return null
        return world.getEntity(uuid) as? DisplayEntity.TextDisplayEntity
    }

    private fun createChallengeDisplay(world: ServerWorld, chest: ActiveChest, challenge: ChestChallenge, remaining: Int): DisplayEntity.TextDisplayEntity {
        val display = DisplayEntity.TextDisplayEntity(EntityType.TEXT_DISPLAY, world)
        display.setPosition(chest.key.pos.x + 0.5, chest.key.pos.y + 1.25, chest.key.pos.z + 0.5)
        display.setBillboardMode(DisplayEntity.BillboardMode.CENTER)
        display.setViewRange(1.5f)
        display.setDisplayWidth(0.0f)
        display.setDisplayHeight(0.0f)
        display.setShadowStrength(0.0f)
        display.setBackground(0)
        display.setTextOpacity((-1).toByte())
        val flags =
            (DisplayEntity.TextDisplayEntity.SHADOW_FLAG.toInt() or DisplayEntity.TextDisplayEntity.SEE_THROUGH_FLAG.toInt()).toByte()
        display.setDisplayFlags(flags)
        display.setNoGravity(true)
        display.isInvulnerable = true
        display.isSilent = true
        display.setText(Text.literal("§6[共鸣挑战] §f击败守护者！ §7(剩余: $remaining)"))
        world.spawnEntity(display)
        challenge.displayEntityUuid = display.uuid
        return display
    }

    private fun onGuardianKilled(entity: HostileEntity, killer: ServerPlayerEntity?) {
        val entityUuid = entity.uuid
        val challenge = activeChallenges.values.find { it.guardianUuids.contains(entityUuid) } ?: return
        challenge.guardianUuids.remove(entityUuid)

        val targetPlayer = killer ?: entity.world.server?.playerManager?.getPlayer(challenge.ownerId) ?: return
        val amount = if ((entity as? AdventureRankMobAccess)?.cresoraIsEliteMob() == true) 100 else 40
        val totalCredits = CreditsService.addCredits(targetPlayer, amount)

        targetPlayer.sendMessage(
            Text.literal("§6[共鸣探索] §f击杀守护者：CSC +$amount | 当前 CSC ${ArtifactSpecialItem.formatWholeNumber(totalCredits)}"),
            true
        )

        val random = entity.random
        val lootStack = ItemStack(net.minecraft.item.Items.GOLD_NUGGET, random.nextBetween(1, 3))
        val itemEntity = net.minecraft.entity.ItemEntity(entity.world, entity.x, entity.y, entity.z, lootStack)
        entity.world.spawnEntity(itemEntity)
    }

    private fun grantUpgradedChallengeRewards(player: ServerPlayerEntity, stars: Int) {
        val random = player.random
        val isEquipment = random.nextBoolean()

        if (isEquipment) {
            val candidateDefinitions = EquipmentContentRegistry.equipmentDefinitions()
            if (candidateDefinitions.isNotEmpty()) {
                val definition = candidateDefinitions[random.nextInt(candidateDefinitions.size)]
                val item = EquipmentStackSupport.itemForDefinitionId(definition.id)
                if (item != null) {
                    val rarity = when (stars) {
                        3 -> EquipmentRarity.THREE_STAR
                        4 -> EquipmentRarity.FOUR_STAR
                        5 -> EquipmentRarity.FIVE_STAR
                        else -> EquipmentRarity.THREE_STAR
                    }
                    val level = random.nextBetween(0, 4)
                    val stack = ItemStack(item)
                    val data = EquipmentGenerationService.createEquipment(
                        random = random,
                        definition = definition,
                        startingLevel = level,
                        forcedRarity = rarity
                    )
                    EquipmentStackSupport.syncEquipmentData(stack, data)
                    giveStack(player, stack)
                    player.sendMessage(
                        Text.literal("§6[共鸣奖励] §f获得了圣遗物残响: ").append(stack.name),
                        false
                    )
                }
            }
        } else {
            val weaponRarity = when (stars) {
                3 -> WeaponRarity.THREE_STAR
                4 -> WeaponRarity.FOUR_STAR
                5 -> if (random.nextBoolean()) WeaponRarity.FIVE_STAR else WeaponRarity.FOUR_STAR
                else -> WeaponRarity.THREE_STAR
            }
            val itemId = Identifier.of(CreSoraUtilities.MOD_ID, weaponRarity.fragmentItemId())
            val item = Registries.ITEM.get(itemId)
            if (item != net.minecraft.item.Items.AIR) {
                val count = if (stars == 5) random.nextBetween(1, 2) else 1
                val stack = ItemStack(item, count)
                giveStack(player, stack)
                player.sendMessage(
                    Text.literal("§6[共鸣奖励] §f获得了武器碎片: ").append(stack.name).append(" x$count"),
                    false
                )
            }
        }
    }

    private fun giveStack(player: ServerPlayerEntity, stack: ItemStack) {
        val remaining = stack.copy()
        if (player.inventory.insertStack(remaining)) {
            return
        }
        if (!remaining.isEmpty) {
            player.dropItem(remaining, false)
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
                iterator.remove()
                toRemove.add(chest)
            }
        }

        for (chest in toRemove) {
            val world = server.getWorld(chest.key.worldKey)
            if (world != null) {
                if (isChunkLoaded(world, chest.key.pos)) {
                    val state = world.getBlockState(chest.key.pos)
                    if (isOurChestBlock(state.block)) {
                        world.setBlockState(chest.key.pos, Blocks.AIR.defaultState)
                    }
                }
            }
            unregisterChest(chest, server)
        }
    }

    fun clearTransientState(player: ServerPlayerEntity) {
        val server = player.server ?: return
        val toReset = activeChallenges.values.filter { it.ownerId == player.uuid && it.triggered && !it.completed }
        for (challenge in toReset) {
            val world = server.getWorld(challenge.chestKey.worldKey) ?: continue
            resetChallenge(server, world, challenge)
        }
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
                0.1,
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

        if (chest.ownerId != serverPlayer.uuid) {
            val ownerName = server.playerManager.getPlayer(chest.ownerId)?.name?.string ?: "其他玩家"
            serverPlayer.sendMessage(
                Text.translatable("message.cresora.treasure_chest.not_owner", ownerName).formatted(Formatting.RED),
                true
            )
            return ActionResult.FAIL
        }

        val challenge = activeChallenges.getOrPut(key) {
            ChestChallenge(key, chest.ownerId, chest.reward.stars)
        }
        if (!challenge.triggered) {
            triggerChallenge(server, playerWorld, chest, challenge, serverPlayer)
            return ActionResult.FAIL
        }
        if (!challenge.completed) {
            serverPlayer.sendMessage(
                Text.literal("§c请先击败周围的守护者！"),
                true
            )
            return ActionResult.FAIL
        }

        val creditsTotal = CreditsService.addCredits(serverPlayer, chest.reward.credits)
        val chordTotal = ResonanceService.addCurrency(serverPlayer, ResonanceCurrencyType.CHORD_PROGRESSION, chest.reward.chordProgression)

        grantUpgradedChallengeRewards(serverPlayer, chest.reward.stars)

        removeChestBlock(playerWorld, chest)
        unregisterChest(chest, server)
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

    private fun registerChest(chest: ActiveChest) {
        activeByKey[chest.key] = chest
        activeKeysByOwner.getOrPut(chest.ownerId) { linkedSetOf() }.add(chest.key)
        syncPersistentState()
    }

    private fun activeChestCount(ownerId: UUID): Int {
        return activeKeysByOwner[ownerId]?.size ?: 0
    }

    private fun unregisterChest(chest: ActiveChest, server: MinecraftServer) {
        val challenge = activeChallenges.remove(chest.key)
        if (challenge != null) {
            val world = server.getWorld(chest.key.worldKey)
            if (world != null) {
                challenge.displayEntityUuid?.let { uuid ->
                    world.getEntity(uuid)?.discard()
                }
                challenge.guardianUuids.forEach { uuid ->
                    world.getEntity(uuid)?.discard()
                }
            }
        }
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
        activeChallenges.clear()
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
