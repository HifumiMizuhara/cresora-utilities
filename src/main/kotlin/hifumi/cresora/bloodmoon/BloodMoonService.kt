package hifumi.cresora.bloodmoon
import hifumi.cresora.Join
import hifumi.cresora.adventurerank.AdventureRankMobAccess
import hifumi.cresora.adventurerank.AdventureRankService
import hifumi.cresora.combat.FieldMobPackService
import hifumi.cresora.credits.CreditsService
import hifumi.cresora.resonance.ResonanceCurrencyType
import hifumi.cresora.resonance.ResonanceService
import hifumi.cresora.equipment.EquipmentDefinitions
import hifumi.cresora.equipment.EquipmentStackSupport
import hifumi.cresora.equipment.EquipmentGenerationService
import hifumi.cresora.equipment.EquipmentRarity
import hifumi.cresora.equipment.ArtifactSpecialItemSupport
import hifumi.cresora.weapon.WeaponContentRegistry
import hifumi.cresora.weapon.WeaponStackSupport
import hifumi.cresora.weapon.WeaponRarity
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents
import net.fabricmc.fabric.api.event.player.AttackBlockCallback
import net.fabricmc.fabric.api.event.player.UseBlockCallback
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents
import net.minecraft.block.BedBlock
import net.minecraft.block.Block
import net.minecraft.block.BlockState
import net.minecraft.block.Blocks
import net.minecraft.block.HorizontalFacingBlock
import net.minecraft.block.enums.BedPart
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityType
import net.minecraft.entity.effect.StatusEffectInstance
import net.minecraft.entity.effect.StatusEffects
import net.minecraft.entity.SpawnReason
import net.minecraft.entity.boss.WitherEntity
import net.minecraft.entity.decoration.DisplayEntity
import net.minecraft.entity.mob.HostileEntity
import net.minecraft.entity.mob.MobEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.particle.ParticleTypes
import net.minecraft.registry.Registries
import net.minecraft.registry.RegistryKey
import net.minecraft.registry.RegistryKeys
import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.server.world.ServerWorld
import net.minecraft.sound.SoundCategory
import net.minecraft.sound.SoundEvents
import net.minecraft.text.Text
import net.minecraft.util.ActionResult
import net.minecraft.util.Formatting
import net.minecraft.util.Hand
import net.minecraft.util.Identifier
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.hit.HitResult
import net.minecraft.world.RaycastContext
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.util.math.Vec3d
import net.minecraft.world.GameRules
import net.minecraft.world.Heightmap
import net.minecraft.world.World
import java.util.UUID
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

internal data class BloodMoonBedKey(
    val worldKey: RegistryKey<World>,
    val first: BlockPos,
    val second: BlockPos
) {
    fun contains(pos: BlockPos): Boolean = pos == first || pos == second

    fun center(): Vec3d {
        val centerX = (first.x + second.x) * 0.5 + 0.5
        val centerZ = (first.z + second.z) * 0.5 + 0.5
        val y = minOf(first.y, second.y)
        return Vec3d(centerX, y + 1.0, centerZ)
    }
}

private data class BloodMoonPendingConfirm(
    val bedKey: BloodMoonBedKey,
    val expiresAtTick: Long
)

private data class BloodMoonRewardChest(
    val ownerUuid: UUID,
    val worldKey: RegistryKey<World>,
    val pos: BlockPos,
    val reward: BloodMoonRewardBundle
)

private data class BloodMoonWorldPosKey(
    val worldKey: RegistryKey<World>,
    val pos: BlockPos
)

internal data class MobBlockBreakState(val targetPos: BlockPos, val startTick: Long)

private data class ResolvedBloodMoonRewardChest(
    val ownerUuid: UUID,
    val rewardSeed: Long
)

private data class BloodMoonRewardBundle(
    val credits: Int,
    val chordProgression: Int,
    val substituteChord: Int,
    val equipmentStacks: List<ItemStack>,
    val specialStacks: List<ItemStack>,
    val weaponStacks: List<ItemStack>
) {
    fun allStacks(): List<ItemStack> = equipmentStacks + specialStacks + weaponStacks
}

internal enum class BloodMoonBattlePhase {
    COMBAT,
    RESTING,
    COMPLETED
}

internal interface BloodMoonBattleState {
    val phase: BloodMoonBattlePhase
    fun tick(session: BloodMoonBattleSession, server: MinecraftServer, world: ServerWorld): BloodMoonBattleState
}

internal class RestingState : BloodMoonBattleState {
    override val phase: BloodMoonBattlePhase = BloodMoonBattlePhase.RESTING

    override fun tick(session: BloodMoonBattleSession, server: MinecraftServer, world: ServerWorld): BloodMoonBattleState {
        if (world.time >= session.restUntilTick) {
            session.nextWaveTick = world.time + BloodMoonService.START_DELAY_TICKS
            session.lastActionBarSecond = -1
            return PreparingState()
        }
        BloodMoonService.showRestStatus(server, world, session)
        return this
    }
}

internal class PreparingState : BloodMoonBattleState {
    override val phase: BloodMoonBattlePhase = BloodMoonBattlePhase.COMBAT

    override fun tick(session: BloodMoonBattleSession, server: MinecraftServer, world: ServerWorld): BloodMoonBattleState {
        if (world.time >= session.nextWaveTick) {
            if (session.nextWaveNumber > BloodMoonService.WAVE_COUNT) {
                BloodMoonService.completeBattle(server, session)
                return CompletedState
            }
            BloodMoonService.spawnWave(server, world, session)
            return CombatState()
        }
        BloodMoonService.showPrepareStatus(server, world, session)
        return this
    }
}

internal class CombatState : BloodMoonBattleState {
    override val phase: BloodMoonBattlePhase = BloodMoonBattlePhase.COMBAT

    override fun tick(session: BloodMoonBattleSession, server: MinecraftServer, world: ServerWorld): BloodMoonBattleState {
        if (session.activeMobUuids.isEmpty()) {
            session.clearedWaveCount = maxOf(session.clearedWaveCount, session.activeWaveNumber)
            session.activeWaveNumber = 0
            if (session.clearedWaveCount >= BloodMoonService.WAVE_COUNT) {
                BloodMoonService.completeBattle(server, session)
                return CompletedState
            }
            session.restUntilTick = world.time + BloodMoonService.REST_TICKS
            session.lastActionBarSecond = -1
            session.bedInvulnerableUntilNextWave = true
            BloodMoonService.serverMessageToParticipants(
                server,
                session,
                Text.translatable("message.cresora.blood_moon.wave_cleared", session.clearedWaveCount, BloodMoonService.WAVE_COUNT).formatted(Formatting.GREEN),
                false
            )
            return RestingState()
        }
        return this
    }
}

internal object CompletedState : BloodMoonBattleState {
    override val phase: BloodMoonBattlePhase = BloodMoonBattlePhase.COMPLETED
    override fun tick(session: BloodMoonBattleSession, server: MinecraftServer, world: ServerWorld): BloodMoonBattleState = this
}

internal class BloodMoonBattleSession(
    val id: UUID,
    val bedKey: BloodMoonBedKey,
    val ownerUuid: UUID,
    val startedDayIndex: Long,
    val originalBedStates: Map<BlockPos, BlockState>
) {
    val participants: MutableSet<UUID> = linkedSetOf()
    val activeMobUuids: MutableSet<UUID> = linkedSetOf()
    val mobBedAttackCooldowns: MutableMap<UUID, Long> = linkedMapOf()
    val mobBlockBreakStates: MutableMap<UUID, MobBlockBreakState> = linkedMapOf()
    val triggeredBedGuardThresholds: MutableSet<Int> = linkedSetOf()
    var currentState: BloodMoonBattleState = PreparingState()
    val phase: BloodMoonBattlePhase get() = currentState.phase
    var activeWaveNumber: Int = 0
    var nextWaveNumber: Int = 1
    var clearedWaveCount: Int = 0
    var nextWaveTick: Long = 0L
    var restUntilTick: Long = 0L
    var lastActionBarSecond: Int = -1
    var bedDurabilityPercent: Double = 100.0
    var bedInvulnerableUntilNextWave: Boolean = false
    var bedStatusDisplayUuid: UUID? = null

    fun battleStacks(): Int = clearedWaveCount
}

object BloodMoonService {
    private const val BATTLE_RADIUS = 50.0
    private const val BATTLE_RADIUS_SQUARED = BATTLE_RADIUS * BATTLE_RADIUS
    private const val BED_TNT_PROTECTION_RADIUS = 6.0
    private const val BED_TNT_PROTECTION_RADIUS_SQUARED = BED_TNT_PROTECTION_RADIUS * BED_TNT_PROTECTION_RADIUS
    private const val BED_STATE_SWAP_FLAGS = Block.NOTIFY_LISTENERS or Block.FORCE_STATE
    private const val BED_ATTACK_RANGE = 2.75
    private const val BED_ATTACK_RANGE_SQUARED = BED_ATTACK_RANGE * BED_ATTACK_RANGE
    private const val BED_ATTACK_INTERVAL_TICKS = 30L
    private const val BED_PATH_REFRESH_TICKS = 15L
    private const val BED_PATH_SPEED = 1.1
    const val WAVE_COUNT = 20
    private const val REST_SECONDS = 30L
    val REST_TICKS = REST_SECONDS * 20L
    const val START_DELAY_TICKS = 20L
    private const val CONFIRM_TIMEOUT_TICKS = 20L * 5L
    private const val MOB_DUPLICATION_CHANCE = 0.20
    private const val PLAYER_DAMAGE_PER_STACK = 0.10
    private const val PLAYER_HP_PER_STACK = 0.20
    private const val GLOW_DURATION_TICKS = 20 * 60 * 10
    private const val ZERO_SPAWN_RETRY_TICKS = 40L
    private const val MAX_SPAWN_TRIES_PER_MOB = 6
    private const val MOB_BLOCK_BREAK_INTERVAL_TICKS = 100L
    private const val ELITE_BLOCK_BREAK_INTERVAL_TICKS = 60L
    private const val MOB_BLOCK_BREAK_REACH_SQUARED = 6.25

    private val battleMobTypes = listOf(
        "minecraft:zombie",
        "minecraft:skeleton",
        "minecraft:spider",
        "minecraft:creeper",
        "minecraft:drowned",
        "minecraft:witch",
        "minecraft:enderman",
        "minecraft:pillager",
        "minecraft:vindicator"
    )

    private val pendingConfirmations: MutableMap<UUID, BloodMoonPendingConfirm> = linkedMapOf()
    private val rewardChestsByPos: MutableMap<BloodMoonWorldPosKey, BloodMoonRewardChest> = linkedMapOf()
    private val savedRespawnsByPlayer: MutableMap<UUID, ServerPlayerEntity.Respawn?> = linkedMapOf()
    private val pendingRespawnRestoresByPlayer: MutableMap<UUID, ServerPlayerEntity.Respawn?> = linkedMapOf()

    private var activeSession: BloodMoonBattleSession? = null
    private var persistentState: BloodMoonPersistentState? = null
    private var stateLoaded: Boolean = false
    private var rewardChestState: BloodMoonRewardChestPersistentState? = null

    fun init() {
        ServerTickEvents.END_SERVER_TICK.register(ServerTickEvents.EndTick { server -> tick(server) })

        UseBlockCallback.EVENT.register(UseBlockCallback { player, world, hand, hitResult ->
            onUseBlock(player, world, hand, hitResult)
        })

        AttackBlockCallback.EVENT.register { player, world, hand, pos, direction ->
            onAttackBlock(player, world, hand, pos, direction)
        }
        PlayerBlockBreakEvents.BEFORE.register(PlayerBlockBreakEvents.Before { world, player, pos, state, _ ->
            onBeforeBlockBreak(world as? ServerWorld, player as? ServerPlayerEntity, pos)
        })

        ServerPlayConnectionEvents.JOIN.register(ServerPlayConnectionEvents.Join { handler, _, server ->
            onPlayerJoin(server, handler.player)
        })

        ServerPlayConnectionEvents.DISCONNECT.register(ServerPlayConnectionEvents.Disconnect { handler, _ ->
            clearTransientState(handler.player)
        })

        ServerPlayerEvents.COPY_FROM.register(ServerPlayerEvents.CopyFrom { _, newPlayer, _ ->
            restoreAfterRespawn(newPlayer)
        })
    }

    fun clearTransientState(player: ServerPlayerEntity) {
        pendingConfirmations.remove(player.uuid)
    }

    fun debugStop(server: MinecraftServer): Boolean {
        return teardownBloodMoon(server, clearNightState = false)
    }

    fun stopAndClearNight(server: MinecraftServer): Boolean = teardownBloodMoon(server, clearNightState = true)

    fun hasActiveBattle(): Boolean = activeSession != null

    fun playerDamageMultiplier(player: PlayerEntity): Double {
        val serverPlayer = player as? ServerPlayerEntity ?: return 1.0
        val session = activeSession ?: return 1.0
        if (!session.participants.contains(serverPlayer.uuid) || session.phase == BloodMoonBattlePhase.COMPLETED) {
            return 1.0
        }
        return 1.0 + session.battleStacks() * PLAYER_DAMAGE_PER_STACK
    }

    fun playerHealthMultiplier(player: PlayerEntity): Double {
        val serverPlayer = player as? ServerPlayerEntity ?: return 1.0
        val session = activeSession ?: return 1.0
        if (!session.participants.contains(serverPlayer.uuid) || session.phase == BloodMoonBattlePhase.COMPLETED) {
            return 1.0
        }
        return 1.0 + session.battleStacks() * PLAYER_HP_PER_STACK
    }

    fun damageMultiplier(attacker: Entity?): Double {
        val hostile = attacker as? HostileEntity ?: return 1.0
        val session = activeSession ?: return 1.0
        if (!session.activeMobUuids.contains(hostile.uuid)) {
            return 1.0
        }
        return 1.0 + session.battleStacks() * 0.03
    }

    fun maybeDuplicateNaturalSpawn(hostile: MobEntity, world: ServerWorld, spawnReason: SpawnReason) {
        if (spawnReason != SpawnReason.NATURAL && spawnReason != SpawnReason.CHUNK_GENERATION) {
            return
        }
        val server = world.server ?: return
        if (!MoonPhaseService.isBloodMoon(server)) {
            return
        }
        if (world.random.nextDouble() >= MOB_DUPLICATION_CHANCE) {
            return
        }
        val duplicate = hostile.type.spawn(
            world,
            null,
            BlockPos.ofFloored(hostile.x, hostile.y, hostile.z),
            SpawnReason.EVENT,
            true,
            false
        ) as? MobEntity ?: return
        val rank = AdventureRankService.mobRank(hostile)
        val duplicateAccess = duplicate as? AdventureRankMobAccess ?: return
        duplicateAccess.cresoraSetMobAdventureRank(rank)
        FieldMobPackService.markExplicit(duplicate as MobEntity, (hostile as? AdventureRankMobAccess)?.cresoraIsEliteMob() == true)
        AdventureRankService.applyMobScaling(duplicate as MobEntity, rank)
        (duplicate as MobEntity).setTarget(hostile.getTarget())
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
        val serverWorld = world as? ServerWorld ?: return ActionResult.PASS
        val hitPos = hitResult.blockPos.toImmutable()
        val rewardKey = rewardChestKey(serverWorld.registryKey, hitPos)

        rewardChestsByPos[rewardKey]?.let { chest ->
            return handleRewardChestUse(serverPlayer, serverWorld, chest)
        }
        if (tryRecoverRewardChest(serverWorld, hitPos) != null) {
            return handleRewardChestUse(serverPlayer, serverWorld, rewardChestsByPos[rewardKey]!!)
        }

        val activeBattle = activeSession
        if (activeBattle != null &&
            isTntIgniteAttempt(serverPlayer, serverWorld.getBlockState(hitPos), hand) &&
            isWithinProtectedBedBlastZone(serverWorld, hitPos, activeBattle)
        ) {
            maybeWarnBedProtected(serverPlayer)
            return ActionResult.FAIL
        }

        val state = serverWorld.getBlockState(hitPos)
        if (state.block !is BedBlock) {
            return ActionResult.PASS
        }
        if (!MoonPhaseService.isBloodMoon(server)) {
            return ActionResult.PASS
        }

        val bedKey = bedKey(serverWorld, hitResult.blockPos, state)
        val session = activeSession
        if (session != null) {
            if (!sameBed(session.bedKey, bedKey)) {
                serverPlayer.sendMessage(Text.translatable("message.cresora.blood_moon.session_active").formatted(Formatting.RED), false)
                return ActionResult.FAIL
            }
            if (!session.participants.contains(serverPlayer.uuid)) {
                serverPlayer.sendMessage(Text.translatable("message.cresora.blood_moon.session_active").formatted(Formatting.RED), false)
                return ActionResult.FAIL
            }
            joinActiveBattle(server, serverPlayer, session)
            return ActionResult.SUCCESS
        }

        ensureStateLoaded(server)
        if (persistentState?.lockedBattleDayIndex == currentDayIndex(server)) {
            player.sendMessage(Text.translatable("message.cresora.blood_moon.already_cleared").formatted(Formatting.GRAY), true)
            return ActionResult.SUCCESS
        }

        val pending = pendingConfirmations[serverPlayer.uuid]
        if (pending != null && pending.expiresAtTick >= serverWorld.time && sameBed(pending.bedKey, bedKey)) {
            startBattle(server, serverPlayer, bedKey)
            pendingConfirmations.remove(serverPlayer.uuid)
            return ActionResult.SUCCESS
        }

        pendingConfirmations[serverPlayer.uuid] = BloodMoonPendingConfirm(bedKey, serverWorld.time + CONFIRM_TIMEOUT_TICKS)
        serverPlayer.sendMessage(Text.translatable("message.cresora.blood_moon.confirm").formatted(Formatting.LIGHT_PURPLE), false)
        return ActionResult.SUCCESS
    }

    private fun onAttackBlock(
        player: PlayerEntity,
        world: World,
        hand: Hand,
        pos: BlockPos,
        direction: Direction
    ): ActionResult {
        if (world.isClient || hand != Hand.MAIN_HAND) {
            return ActionResult.PASS
        }
        val serverPlayer = player as? ServerPlayerEntity ?: return ActionResult.PASS
        val serverWorld = world as? ServerWorld ?: return ActionResult.PASS
        val rewardKey = rewardChestKey(serverWorld.registryKey, pos.toImmutable())

        if (rewardChestsByPos.containsKey(rewardKey) || tryRecoverRewardChest(serverWorld, pos.toImmutable()) != null) {
            return ActionResult.FAIL
        }

        val session = activeSession ?: return ActionResult.PASS
        if (isProtectedBattleBed(serverWorld, pos, session)) {
            maybeWarnBedProtected(serverPlayer)
            return ActionResult.FAIL
        }
        return ActionResult.PASS
    }

    private fun onBeforeBlockBreak(world: ServerWorld?, player: ServerPlayerEntity?, pos: BlockPos): Boolean {
        if (world == null || player == null) {
            return true
        }
        val session = activeSession ?: return true
        if (isProtectedBattleBed(world, pos, session)) {
            maybeWarnBedProtected(player)
            return false
        }
        return true
    }

    private fun handleRewardChestUse(serverPlayer: ServerPlayerEntity, world: ServerWorld, chest: BloodMoonRewardChest): ActionResult {
        val session = activeSession
        if (session != null && session.phase != BloodMoonBattlePhase.COMPLETED) {
            serverPlayer.sendMessage(Text.translatable("message.cresora.blood_moon.reward_locked").formatted(Formatting.GRAY), false)
            return ActionResult.SUCCESS
        }
        if (chest.ownerUuid != serverPlayer.uuid) {
            serverPlayer.sendMessage(Text.translatable("message.cresora.blood_moon.reward_not_yours").formatted(Formatting.GRAY), false)
            return ActionResult.SUCCESS
        }
        if (!isOurRewardChest(world.getBlockState(chest.pos).block)) {
            rewardChestsByPos.remove(rewardChestKey(world.registryKey, chest.pos))
            return ActionResult.SUCCESS
        }

        for (stack in chest.reward.allStacks()) {
            giveStack(serverPlayer, stack)
        }
        if (chest.reward.credits > 0) {
            CreditsService.addCredits(serverPlayer, chest.reward.credits)
        }
        if (chest.reward.chordProgression > 0) {
            ResonanceService.addCurrency(serverPlayer, ResonanceCurrencyType.CHORD_PROGRESSION, chest.reward.chordProgression)
        }
        if (chest.reward.substituteChord > 0) {
            ResonanceService.addCurrency(serverPlayer, ResonanceCurrencyType.SUBSTITUTE_CHORD, chest.reward.substituteChord)
        }
        val rankXpReward = bloodMoonRankXpReward(serverPlayer)
        if (rankXpReward > 0) {
            AdventureRankService.addXp(serverPlayer, rankXpReward)
        }
        world.setBlockState(chest.pos, Blocks.AIR.defaultState)
        rewardChestsByPos.remove(rewardChestKey(world.registryKey, chest.pos))
        rewardChestState?.let { BloodMoonRewardChestStateService.remove(it, world.registryKey.value.toString(), chest.pos) }
        world.playSound(null, chest.pos, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundCategory.PLAYERS, 0.9f, 1.1f)
        serverPlayer.sendMessage(Text.translatable("message.cresora.blood_moon.reward_claimed").formatted(Formatting.AQUA), false)
        return ActionResult.SUCCESS
    }

    private fun tick(server: MinecraftServer) {
        ensureStateLoaded(server)
        ensureRewardStateLoaded(server)
        purgeExpiredConfirmations(server)
        cleanupRewardChests(server)
        unlockBattleIfNeeded(server)

        val session = activeSession
        if (session != null) {
            tickBattleSession(server, session)
            updatePersistentSession(session)
        } else {
            clearPersistentSession()
        }

        tickBloodMoonTime(server)
        recoverMobGriefingIfDirty(server)
    }

    private fun ensureStateLoaded(server: MinecraftServer) {
        if (stateLoaded) return
        val world = server.overworld
        persistentState = world.persistentStateManager.getOrCreate(BloodMoonPersistentState.TYPE)
        stateLoaded = true

        val saved = persistentState?.activeSession
        if (saved != null) {
            val worldKey = RegistryKey.of<World>(RegistryKeys.WORLD, Identifier.of(saved.bedKey.worldId))
            val bedKey = BloodMoonBedKey(
                worldKey,
                BlockPos(saved.bedKey.firstX, saved.bedKey.firstY, saved.bedKey.firstZ),
                BlockPos(saved.bedKey.secondX, saved.bedKey.secondY, saved.bedKey.secondZ)
            )
            val originalBedStates: Map<BlockPos, BlockState> = if (saved.originalBedStates.isNotEmpty()) {
                saved.originalBedStates.associate { s ->
                    val pos = BlockPos(s.x, s.y, s.z)
                    val block = Registries.BLOCK.get(Identifier.of(s.blockId))
                    val blockState = if (block is BedBlock) {
                        val facing = Direction.values().firstOrNull { it.asString() == s.facing } ?: Direction.NORTH
                        val part = if (s.part == "head") BedPart.HEAD else BedPart.FOOT
                        block.defaultState
                            .with(HorizontalFacingBlock.FACING, facing)
                            .with(BedBlock.PART, part)
                            .with(BedBlock.OCCUPIED, s.occupied)
                    } else {
                        block.defaultState
                    }
                    pos to blockState
                }
            } else {
                // Fallback for old saves without persisted bed states: read current world state
                val world = server.getWorld(bedKey.worldKey)
                mapOf(
                    bedKey.first to (world?.getBlockState(bedKey.first) ?: Blocks.RED_BED.defaultState),
                    bedKey.second to (world?.getBlockState(bedKey.second) ?: Blocks.RED_BED.defaultState)
                )
            }
            val session = BloodMoonBattleSession(
                saved.id,
                bedKey,
                saved.ownerUuid,
                saved.startedDayIndex,
                originalBedStates
            )
            session.participants.addAll(saved.participants)
            session.clearedWaveCount = saved.clearedWaveCount
            session.nextWaveNumber = saved.nextWaveNumber
            session.bedDurabilityPercent = saved.bedDurabilityPercent
            session.nextWaveTick = saved.nextWaveTick
            session.restUntilTick = saved.restUntilTick
            session.activeMobUuids.addAll(saved.activeMobUuids)
            session.currentState = when (saved.phase) {
                "RESTING" -> RestingState()
                "COMPLETED" -> CompletedState
                else -> PreparingState()
            }
            activeSession = session
        }
    }

    private fun updatePersistentSession(session: BloodMoonBattleSession) {
        val state = persistentState ?: return
        val savedBedKey = SavedBloodMoonBedKey(
            session.bedKey.worldKey.value.toString(),
            session.bedKey.first.x, session.bedKey.first.y, session.bedKey.first.z,
            session.bedKey.second.x, session.bedKey.second.y, session.bedKey.second.z
        )
        val savedOriginalBedStates = session.originalBedStates.map { (pos, blockState) ->
            val facing = if (blockState.block is BedBlock) blockState.get(HorizontalFacingBlock.FACING).asString() else "north"
            val part = if (blockState.block is BedBlock) blockState.get(BedBlock.PART).asString() else "foot"
            val occupied = blockState.block is BedBlock && blockState.get(BedBlock.OCCUPIED)
            SavedBedBlockState(pos.x, pos.y, pos.z, Registries.BLOCK.getId(blockState.block).toString(), facing, part, occupied)
        }
        state.activeSession = SavedBloodMoonSession(
            session.id,
            savedBedKey,
            session.ownerUuid,
            session.startedDayIndex,
            session.participants.toList(),
            session.clearedWaveCount,
            session.nextWaveNumber,
            session.bedDurabilityPercent,
            session.phase.name,
            session.nextWaveTick,
            session.restUntilTick,
            session.activeMobUuids.toList(),
            savedOriginalBedStates
        )
        state.markDirty()
    }

    private fun clearPersistentSession() {
        val state = persistentState ?: return
        if (state.activeSession != null) {
            state.activeSession = null
            state.markDirty()
        }
    }

    private fun tickBloodMoonTime(server: MinecraftServer) {
        if (activeSession == null) {
            return
        }
        val world = server.overworld
        val phase = world.timeOfDay % 24000L
        when {
            phase < 12000L -> world.timeOfDay -= phase + 6000L
            phase >= 23000L -> world.timeOfDay -= phase - 18000L
        }
    }

    private fun tickBattleSession(server: MinecraftServer, session: BloodMoonBattleSession) {
        val world = server.getWorld(session.bedKey.worldKey) ?: run {
            restoreParticipantRespawns(server, session.participants)
            activeSession = null
            return
        }
        if (session.phase == BloodMoonBattlePhase.COMPLETED) {
            activeSession = null
            return
        }

        if (!isBattleBedIntact(world, session)) {
            failBattle(server, world, session)
            return
        }
        syncBattleBedDisplay(world, session)
        tickSpecialBedVisuals(world, session)
        if (!hasOnlineParticipants(server, session)) {
            abandonBattle(server, world, session)
            return
        }
        updateBattleMobPressure(world, session)
        enforceParticipants(server, world, session)
        cleanupActiveMobs(server, world, session)

        session.currentState = session.currentState.tick(session, server, world)
    }

    private fun cleanupActiveMobs(server: MinecraftServer, world: ServerWorld, session: BloodMoonBattleSession) {
        val iterator = session.activeMobUuids.iterator()
        while (iterator.hasNext()) {
            val mobUuid = iterator.next()
            val entity = world.getEntity(mobUuid)
            if (entity == null || !entity.isAlive || entity.isRemoved) {
                iterator.remove()
                session.mobBedAttackCooldowns.remove(mobUuid)
                session.mobBlockBreakStates.remove(mobUuid)
            }
        }
    }

    internal fun spawnWave(server: MinecraftServer, world: ServerWorld, session: BloodMoonBattleSession) {
        val waveNumber = session.nextWaveNumber.coerceAtLeast(1)
        val participantCount = session.participants.size.coerceAtLeast(1)
        val target = server.playerManager.playerList.firstOrNull { session.participants.contains(it.uuid) } ?: return
        val waveRank = (AdventureRankService.getRank(target) + waveNumber * 2 + participantCount).coerceAtLeast(1)
        val baseCount = 5 + waveNumber + participantCount
        val healthScalar = 1.0 + (waveNumber - 1) * 0.05
        val defenseScalar = 1.0 + (waveNumber - 1) * 0.025
        session.bedInvulnerableUntilNextWave = false

        var spawnedCount = 0
        repeat(baseCount) { index ->
            val entityType = Registries.ENTITY_TYPE.get(Identifier.of(pickBattleMobId(world.random, waveNumber, index)))
            val hostile = spawnHostileWithRetry(world, entityType, session.bedKey.center(), index) ?: return@repeat
            val access = hostile as? AdventureRankMobAccess ?: return@repeat
            access.cresoraSetMobAdventureRank(waveRank)
            FieldMobPackService.markExplicit(hostile, waveNumber >= 8 && index % 5 == 0)
            AdventureRankService.applyMobScaling(hostile, waveRank, healthScalar, defenseScalar, defenseScalar)
            applyBattleMobVisuals(hostile)
            hostile.target = null
            session.activeMobUuids += hostile.uuid
            spawnedCount += 1
        }

        if (waveNumber == WAVE_COUNT) {
            repeat(2) { eliteIndex ->
                val wither = spawnHostileWithRetry(world, EntityType.WITHER, session.bedKey.center(), baseCount + eliteIndex + 19) as? WitherEntity ?: return@repeat
                val access = wither as? AdventureRankMobAccess ?: return@repeat
                access.cresoraSetMobAdventureRank((waveRank + 12).coerceAtLeast(1))
                FieldMobPackService.markExplicit(wither, true)
                AdventureRankService.applyMobScaling(wither, waveRank + 12, 1.25, 1.25, 1.25)
                applyBattleMobVisuals(wither)
                wither.target = null
                session.activeMobUuids += wither.uuid
                spawnedCount += 1
            }
        }

        if (spawnedCount <= 0) {
            session.nextWaveTick = world.time + ZERO_SPAWN_RETRY_TICKS
            serverMessageToParticipants(
                server,
                session,
                Text.translatable("message.cresora.blood_moon.wave_spawn_retry", waveNumber).formatted(Formatting.RED),
                false
            )
            return
        }

        session.activeWaveNumber = waveNumber
        session.nextWaveNumber += 1
        session.nextWaveTick = world.time
        session.lastActionBarSecond = -1
        serverMessageToParticipants(
            server,
            session,
            Text.translatable("message.cresora.blood_moon.wave_started", waveNumber, WAVE_COUNT).formatted(Formatting.GOLD),
            false
        )
    }

    internal fun completeBattle(server: MinecraftServer, session: BloodMoonBattleSession) {
        if (session.phase == BloodMoonBattlePhase.COMPLETED) {
            return
        }
        restoreMobGriefing(server)
        restoreBattleBed(server, session)
        spawnRewardChests(server, session)
        restoreParticipantRespawns(server, session.participants)
        serverMessageToParticipants(server, session, Text.translatable("message.cresora.blood_moon.victory").formatted(Formatting.GOLD), false)
        activeSession = null
        clearPersistentSession()
    }

    private fun spawnRewardChests(server: MinecraftServer, session: BloodMoonBattleSession) {
        val world = server.getWorld(session.bedKey.worldKey) ?: return
        val participants = session.participants.toList().ifEmpty { listOf(session.ownerUuid) }
        val positions = findChestPlacements(world, session.bedKey, participants.size)
        repeat(participants.size) { index ->
            val owner = participants[index]
            val pos = positions.getOrNull(index)
            if (pos == null || !placeRewardChest(world, pos)) {
                server.playerManager.getPlayer(owner)?.let { player ->
                    val fallback = buildRewardBundle(session.id.mostSignificantBits xor owner.mostSignificantBits xor index.toLong())
                    fallback.allStacks().forEach { giveStack(player, it) }
                    if (fallback.credits > 0) {
                        CreditsService.addCredits(player, fallback.credits)
                    }
                    val rankXpReward = bloodMoonRankXpReward(player)
                    if (rankXpReward > 0) {
                        AdventureRankService.addXp(player, rankXpReward)
                    }
                    player.sendMessage(Text.translatable("message.cresora.blood_moon.reward_fallback").formatted(Formatting.YELLOW), false)
                }
                return@repeat
            }
            val seed = session.id.leastSignificantBits + index.toLong()
            val reward = buildRewardBundle(seed)
            val chest = BloodMoonRewardChest(owner, world.registryKey, pos.toImmutable(), reward)
            rewardChestsByPos[rewardChestKey(world.registryKey, pos.toImmutable())] = chest
            rewardChestState?.let {
                BloodMoonRewardChestStateService.put(it, world.registryKey.value.toString(), pos.toImmutable(), owner, seed)
            }
        }
    }

    private fun buildRewardBundle(seed: Long): BloodMoonRewardBundle {
        val random = net.minecraft.util.math.random.Random.create(seed)
        val equipmentStacks = listOf(
            EquipmentDefinitions.HINAGATA_WAND,
            EquipmentDefinitions.HINAGATA_HAT,
            EquipmentDefinitions.HINAGATA_GLASSES,
            EquipmentDefinitions.HINAGATA_ARMOR,
            EquipmentDefinitions.HINAGATA_BOOTS
        ).map { ref ->
            val definition = ref.resolve()
            val stack = ItemStack(EquipmentStackSupport.itemForDefinitionId(definition.id) ?: error("Missing equipment item: ${definition.id}"))
            EquipmentStackSupport.syncEquipmentData(
                stack,
                EquipmentGenerationService.createEquipment(
                    random = random,
                    definition = definition,
                    startingLevel = EquipmentRarity.FIVE_STAR.maxLevel,
                    forcedRarity = EquipmentRarity.FIVE_STAR
                )
            )
            stack
        }

        val specialStacks = mutableListOf<ItemStack>()
        ArtifactSpecialItemSupport.itemForDefinitionId("blood_note")?.let { specialStacks += ItemStack(it) }

        val weaponStacks = mutableListOf<ItemStack>()
        if (random.nextDouble() < 0.20) {
            val definition = WeaponContentRegistry.requireWeapon("lossless_crown")
            weaponStacks += WeaponStackSupport.createWeaponStack(definition, WeaponRarity.FIVE_STAR, 1, 1)
        }
        if (random.nextDouble() < 0.50) {
            val definition = WeaponContentRegistry.requireWeapon("blood_tear")
            weaponStacks += WeaponStackSupport.createWeaponStack(definition, WeaponRarity.FOUR_STAR, 1, 1)
        }

        return BloodMoonRewardBundle(
            credits = 100_000,
            chordProgression = 1300,
            substituteChord = 650,
            equipmentStacks = equipmentStacks,
            specialStacks = specialStacks,
            weaponStacks = weaponStacks
        )
    }

    private fun findChestPlacements(world: ServerWorld, bedKey: BloodMoonBedKey, needed: Int): List<BlockPos> {
        val positions = mutableListOf<BlockPos>()
        val center = bedKey.center()
        for (radius in 2..12) {
            for (step in 0 until 16) {
                val angle = (Math.PI / 8.0) * step
                val x = (center.x + cos(angle) * radius).toInt()
                val z = (center.z + sin(angle) * radius).toInt()
                val y = bedKey.first.y
                val pos = BlockPos(x, y, z)
                if (canPlaceChest(world, pos)) {
                    positions += pos.toImmutable()
                }
            }
        }
        if (positions.isEmpty()) {
            positions += bedKey.first.up().toImmutable()
        }
        if (positions.size < needed) {
            for (x in -8..8) {
                for (z in -8..8) {
                    val pos = BlockPos(center.x.toInt() + x, bedKey.first.y, center.z.toInt() + z)
                    if (canPlaceChest(world, pos)) {
                        positions += pos.toImmutable()
                        if (positions.distinct().size >= needed) {
                            return positions.distinct()
                        }
                    }
                }
            }
        }
        return positions.distinct()
    }

    private fun placeRewardChest(world: ServerWorld, pos: BlockPos): Boolean {
        if (!canPlaceChest(world, pos)) {
            return false
        }
        world.setBlockState(pos, Blocks.CHEST.defaultState)
        return true
    }

    private fun canPlaceChest(world: ServerWorld, pos: BlockPos): Boolean {
        val floorPos = pos.down()
        return world.getBlockState(pos).isAir &&
            world.getBlockState(pos.up()).isAir &&
            world.getBlockState(floorPos).isSideSolidFullSquare(world, floorPos, Direction.UP)
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

    private fun startBattle(server: MinecraftServer, player: ServerPlayerEntity, bedKey: BloodMoonBedKey) {
        ensureStateLoaded(server)
        if (persistentState?.lockedBattleDayIndex == currentDayIndex(server)) {
            player.sendMessage(Text.translatable("message.cresora.blood_moon.already_cleared").formatted(Formatting.GRAY), false)
            return
        }
        if (activeSession != null) {
            player.sendMessage(Text.translatable("message.cresora.blood_moon.session_active").formatted(Formatting.RED), false)
            return
        }
        val world = server.getWorld(bedKey.worldKey) ?: return
        val session = BloodMoonBattleSession(
            UUID.randomUUID(),
            bedKey,
            player.uuid,
            currentDayIndex(server),
            mapOf(
                bedKey.first to world.getBlockState(bedKey.first),
                bedKey.second to world.getBlockState(bedKey.second)
            )
        )
        session.participants += collectNearbyParticipants(server, world, bedKey)
        session.participants += player.uuid
        activeSession = session

        applySpecialBed(world, session)
        applyBattleRespawns(server, session)
        setMobGriefing(server, false)
        session.nextWaveTick = world.time + START_DELAY_TICKS

        updatePersistentSession(session)
        persistentState?.lockedBattleDayIndex = session.startedDayIndex
        persistentState?.markDirty()

        serverMessageToParticipants(server, session, Text.translatable("message.cresora.blood_moon.started").formatted(Formatting.LIGHT_PURPLE), false)
        server.playerManager.playerList
            .filter { session.participants.contains(it.uuid) }
            .forEach { participant ->
                teleportPlayer(participant, world, bedKey.center(), participant.yaw, participant.pitch)
            }
    }

    private fun joinActiveBattle(server: MinecraftServer, player: ServerPlayerEntity, session: BloodMoonBattleSession) {
        val world = server.getWorld(session.bedKey.worldKey) ?: return
        val isNewParticipant = session.participants.add(player.uuid)
        setBattleRespawn(player, world, session.bedKey.first)
        teleportPlayer(player, world, session.bedKey.center(), player.yaw, player.pitch)
        player.sendMessage(
            Text.translatable(if (isNewParticipant) "message.cresora.blood_moon.joined" else "message.cresora.blood_moon.rejoined")
                .formatted(Formatting.GOLD),
            false
        )
    }

    private fun applyBattleRespawns(server: MinecraftServer, session: BloodMoonBattleSession) {
        val world = server.getWorld(session.bedKey.worldKey) ?: return
        for (participant in server.playerManager.playerList.filter { session.participants.contains(it.uuid) }) {
            setBattleRespawn(participant, world, session.bedKey.first)
        }
    }

    private fun onPlayerJoin(server: MinecraftServer, player: ServerPlayerEntity) {
        val session = activeSession ?: run {
            restoreAfterRespawn(player)
            return
        }
        if (!session.participants.contains(player.uuid)) {
            restoreAfterRespawn(player)
            return
        }
        val world = server.getWorld(session.bedKey.worldKey) ?: return
        teleportPlayer(player, world, session.bedKey.center(), player.yaw, player.pitch)
        player.sendMessage(Text.translatable("message.cresora.blood_moon.rejoined").formatted(Formatting.AQUA), false)
    }

    private fun enforceParticipants(server: MinecraftServer, world: ServerWorld, session: BloodMoonBattleSession) {
        val center = session.bedKey.center()
        for (participantId in session.participants) {
            val player = server.playerManager.getPlayer(participantId) ?: continue
            if (player.world.registryKey != world.registryKey || player.squaredDistanceTo(center) > BATTLE_RADIUS_SQUARED) {
                teleportPlayer(player, world, center, player.yaw, player.pitch)
            }
        }
    }

    internal fun showRestStatus(server: MinecraftServer, world: ServerWorld, session: BloodMoonBattleSession) {
        val remainingSeconds = ((session.restUntilTick - world.time).coerceAtLeast(0L) / 20L).toInt()
        if (remainingSeconds == session.lastActionBarSecond) {
            return
        }
        session.lastActionBarSecond = remainingSeconds
        serverMessageToParticipants(
            server,
            session,
            Text.translatable("message.cresora.blood_moon.resting", remainingSeconds).formatted(Formatting.GRAY),
            true
        )
    }

    internal fun showPrepareStatus(server: MinecraftServer, world: ServerWorld, session: BloodMoonBattleSession) {
        val remainingSeconds = ((session.nextWaveTick - world.time).coerceAtLeast(0L) / 20L).toInt()
        if (remainingSeconds == session.lastActionBarSecond) {
            return
        }
        session.lastActionBarSecond = remainingSeconds
        serverMessageToParticipants(
            server,
            session,
            Text.translatable("message.cresora.blood_moon.prepare", session.nextWaveNumber, WAVE_COUNT, remainingSeconds).formatted(Formatting.GRAY),
            true
        )
    }

    private fun cleanupRewardChests(server: MinecraftServer) {
        val iterator = rewardChestsByPos.entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            val key = entry.key
            val chest = entry.value
            val world = server.getWorld(chest.worldKey) ?: run {
                iterator.remove()
                continue
            }
            val state = world.getBlockState(chest.pos)
            val resolved = tryRecoverRewardChest(world, chest.pos)
            if (state.isAir || !isOurRewardChest(state.block) || resolved == null) {
                iterator.remove()
                rewardChestState?.let { BloodMoonRewardChestStateService.remove(it, key.worldKey.value.toString(), key.pos) }
            }
        }
    }

    private fun purgeExpiredConfirmations(server: MinecraftServer) {
        val now = server.overworld.time
        val iterator = pendingConfirmations.entries.iterator()
        while (iterator.hasNext()) {
            if (iterator.next().value.expiresAtTick < now) {
                iterator.remove()
            }
        }
    }

    private fun unlockBattleIfNeeded(server: MinecraftServer) {
        if (activeSession != null) {
            return
        }
        val locked = persistentState?.lockedBattleDayIndex ?: -1L
        if (locked < 0L) {
            return
        }
        if (locked == currentDayIndex(server)) {
            return
        }
        persistentState?.lockedBattleDayIndex = -1L
        persistentState?.markDirty()
    }

    internal fun serverMessageToParticipants(server: MinecraftServer, session: BloodMoonBattleSession, message: Text, actionBar: Boolean) {
        server.playerManager.playerList
            .filter { session.participants.contains(it.uuid) }
            .forEach { it.sendMessage(message, actionBar) }
    }

    private fun hasOnlineParticipants(server: MinecraftServer, session: BloodMoonBattleSession): Boolean {
        return session.participants.any { server.playerManager.getPlayer(it) != null }
    }

    private fun abandonBattle(server: MinecraftServer, world: ServerWorld, session: BloodMoonBattleSession) {
        for (mobUuid in session.activeMobUuids) {
            world.getEntity(mobUuid)?.discard()
        }
        session.activeMobUuids.clear()
        removeBattleBedDisplay(world, session)
        restoreBattleBed(server, session)
        restoreMobGriefing(server)
        restoreParticipantRespawns(server, session.participants)
        activeSession = null
    }

    private fun applyBattleMobVisuals(hostile: HostileEntity) {
        hostile.addStatusEffect(StatusEffectInstance(StatusEffects.GLOWING, GLOW_DURATION_TICKS, 0, false, false))
    }

    private fun spawnHostileWithRetry(
        world: ServerWorld,
        type: EntityType<*>,
        center: Vec3d,
        salt: Int
    ): HostileEntity? {
        repeat(MAX_SPAWN_TRIES_PER_MOB) { attempt ->
            val spawnPos = randomBattleSpawnPosition(world, center, salt * 17 + attempt)
            val hostile = type.spawn(world, null, spawnPos, SpawnReason.EVENT, true, false) as? HostileEntity
            if (hostile != null) {
                return hostile
            }
        }
        return null
    }

    private fun randomBattleSpawnPosition(world: ServerWorld, center: Vec3d, salt: Int): BlockPos {
        val radiusX = 11 + world.random.nextInt(9)
        val radiusZ = 11 + world.random.nextInt(9)
        val xSign = if (world.random.nextBoolean()) 1 else -1
        val zSign = if (world.random.nextBoolean()) 1 else -1
        val x = center.x.toInt() + xSign * radiusX
        val z = center.z.toInt() + zSign * radiusZ
        val surfaceY = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z)
        return BlockPos(x, surfaceY + 1, z)
    }

    private fun pickBattleMobId(random: net.minecraft.util.math.random.Random, waveNumber: Int, index: Int): String {
        return when {
            waveNumber >= 15 && index % 3 == 0 -> battleMobTypes[5]
            waveNumber >= 10 && index % 4 == 0 -> battleMobTypes[7]
            waveNumber >= 8 && index % 2 == 0 -> battleMobTypes[6]
            else -> battleMobTypes[random.nextInt(battleMobTypes.size)]
        }
    }

    private fun setMobGriefing(server: MinecraftServer, value: Boolean) {
        val rule = server.gameRules.get(GameRules.DO_MOB_GRIEFING)
        if (persistentState?.originalMobGriefing == null) {
            persistentState?.originalMobGriefing = rule.get()
            persistentState?.markDirty()
        }
        rule.set(value, server)
    }

    private fun restoreMobGriefing(server: MinecraftServer) {
        val original = persistentState?.originalMobGriefing ?: return
        server.gameRules.get(GameRules.DO_MOB_GRIEFING).set(original, server)
        persistentState?.originalMobGriefing = null
        persistentState?.markDirty()
    }

    private fun recoverMobGriefingIfDirty(server: MinecraftServer) {
        if (activeSession != null || persistentState?.originalMobGriefing == null) {
            return
        }
        restoreMobGriefing(server)
    }

    private fun applySpecialBed(world: ServerWorld, session: BloodMoonBattleSession) {
        setBattleBedStates(
            world,
            session,
            specialBedStateFrom(session.originalBedStates.getValue(session.bedKey.first)),
            specialBedStateFrom(session.originalBedStates.getValue(session.bedKey.second))
        )
    }

    private fun restoreBattleBed(server: MinecraftServer, session: BloodMoonBattleSession) {
        val world = server.getWorld(session.bedKey.worldKey) ?: return
        val firstState = world.getBlockState(session.bedKey.first)
        val secondState = world.getBlockState(session.bedKey.second)
        if ((firstState.block is BedBlock || firstState.isAir) && (secondState.block is BedBlock || secondState.isAir)) {
            setBattleBedStates(
                world,
                session,
                session.originalBedStates.getValue(session.bedKey.first),
                session.originalBedStates.getValue(session.bedKey.second)
            )
        }
    }

    private fun destroyBattleBed(world: ServerWorld, session: BloodMoonBattleSession) {
        for (pos in session.originalBedStates.keys) {
            world.spawnParticles(ParticleTypes.EXPLOSION, pos.x + 0.5, pos.y + 0.7, pos.z + 0.5, 1, 0.15, 0.15, 0.15, 0.0)
        }
        removeBattleBedDisplay(world, session)
        setBattleBedStates(world, session, Blocks.AIR.defaultState, Blocks.AIR.defaultState)
        world.playSound(null, session.bedKey.first, SoundEvents.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, SoundCategory.HOSTILE, 1.0f, 0.8f)
    }

    private fun setBattleBedStates(world: ServerWorld, session: BloodMoonBattleSession, firstState: BlockState, secondState: BlockState) {
        world.setBlockState(session.bedKey.first, firstState, BED_STATE_SWAP_FLAGS)
        world.setBlockState(session.bedKey.second, secondState, BED_STATE_SWAP_FLAGS)
    }

    private fun specialBedStateFrom(state: BlockState): BlockState {
        return Blocks.RED_BED.defaultState
            .with(HorizontalFacingBlock.FACING, state.get(HorizontalFacingBlock.FACING))
            .with(BedBlock.PART, state.get(BedBlock.PART))
            .with(BedBlock.OCCUPIED, false)
    }

    private fun setBattleRespawn(player: ServerPlayerEntity, world: ServerWorld, pos: BlockPos) {
        if (!savedRespawnsByPlayer.containsKey(player.uuid)) {
            savedRespawnsByPlayer[player.uuid] = player.respawn
        }
        pendingRespawnRestoresByPlayer.remove(player.uuid)
        player.setSpawnPoint(ServerPlayerEntity.Respawn(world.registryKey, pos, 0.0f, true), false)
    }

    private fun restoreParticipantRespawns(server: MinecraftServer, participantIds: Collection<UUID>) {
        for (participantId in participantIds) {
            if (!savedRespawnsByPlayer.containsKey(participantId)) {
                continue
            }
            val originalRespawn = savedRespawnsByPlayer.remove(participantId)
            val player = server.playerManager.getPlayer(participantId)
            if (player != null) {
                player.setSpawnPoint(originalRespawn, false)
            } else {
                pendingRespawnRestoresByPlayer[participantId] = originalRespawn
            }
        }
    }

    private fun restoreAfterRespawn(player: ServerPlayerEntity) {
        val session = activeSession
        if (session != null && session.participants.contains(player.uuid)) {
            return
        }
        if (!pendingRespawnRestoresByPlayer.containsKey(player.uuid)) {
            return
        }
        val originalRespawn = pendingRespawnRestoresByPlayer.remove(player.uuid)
        player.setSpawnPoint(originalRespawn, false)
    }

    private fun teleportPlayer(player: ServerPlayerEntity, world: ServerWorld, position: Vec3d, yaw: Float, pitch: Float) {
        player.teleport(world, position.x, position.y, position.z, setOf(), yaw, pitch, false)
    }

    private fun sameBed(first: BloodMoonBedKey, second: BloodMoonBedKey): Boolean {
        return first.worldKey == second.worldKey &&
            ((first.first == second.first && first.second == second.second) || (first.first == second.second && first.second == second.first))
    }

    private fun bedKey(world: ServerWorld, pos: BlockPos, state: BlockState): BloodMoonBedKey {
        val facing = state.get(HorizontalFacingBlock.FACING)
        val part = state.get(BedBlock.PART)
        val other = if (part == BedPart.FOOT) {
            pos.offset(facing)
        } else {
            pos.offset(facing.opposite)
        }
        val first = pos.toImmutable()
        val second = other.toImmutable()
        return if (compareBlockPos(first, second) <= 0) {
            BloodMoonBedKey(world.registryKey, first, second)
        } else {
            BloodMoonBedKey(world.registryKey, second, first)
        }
    }

    private fun compareBlockPos(left: BlockPos, right: BlockPos): Int {
        return when {
            left.x != right.x -> left.x.compareTo(right.x)
            left.y != right.y -> left.y.compareTo(right.y)
            else -> left.z.compareTo(right.z)
        }
    }

    private fun collectNearbyParticipants(server: MinecraftServer, world: ServerWorld, bedKey: BloodMoonBedKey): Set<UUID> {
        val center = bedKey.center()
        return server.playerManager.playerList
            .filter { it.world.registryKey == world.registryKey && it.squaredDistanceTo(center) <= BATTLE_RADIUS_SQUARED }
            .mapTo(linkedSetOf()) { it.uuid }
    }

    private fun isBattleBedIntact(world: ServerWorld, session: BloodMoonBattleSession): Boolean {
        return session.originalBedStates.keys.all { world.getBlockState(it).block is BedBlock }
    }

    private fun isWithinProtectedBedBlastZone(world: ServerWorld, pos: BlockPos, session: BloodMoonBattleSession): Boolean {
        return session.bedKey.worldKey == world.registryKey &&
            pos.toCenterPos().squaredDistanceTo(session.bedKey.center()) <= BED_TNT_PROTECTION_RADIUS_SQUARED
    }

    private fun isProtectedBattleBed(world: ServerWorld, pos: BlockPos, session: BloodMoonBattleSession): Boolean {
        return session.bedKey.worldKey == world.registryKey && session.bedKey.contains(pos)
    }

    private fun maybeWarnBedProtected(player: ServerPlayerEntity) {
        player.sendMessage(Text.translatable("message.cresora.blood_moon.bed_protected").formatted(Formatting.GRAY), true)
    }

    private fun isTntIgniteAttempt(player: ServerPlayerEntity, state: BlockState, hand: Hand): Boolean {
        if (!state.isOf(Blocks.TNT)) {
            return false
        }
        val stack = player.getStackInHand(hand)
        return stack.isOf(Items.FLINT_AND_STEEL) || stack.isOf(Items.FIRE_CHARGE)
    }

    private fun tickSpecialBedVisuals(world: ServerWorld, session: BloodMoonBattleSession) {
        val center = session.bedKey.center()
        world.spawnParticles(ParticleTypes.SOUL_FIRE_FLAME, center.x, center.y + 0.2, center.z, 2, 0.25, 0.1, 0.25, 0.0)
    }

    private fun syncBattleBedDisplay(world: ServerWorld, session: BloodMoonBattleSession) {
        val display = resolveBattleBedDisplay(world, session) ?: createBattleBedDisplay(world, session)
        val center = session.bedKey.center()
        display.setPosition(center.x, center.y + 1.35, center.z)
        display.setText(battleBedStatusText(session))
    }

    private fun resolveBattleBedDisplay(world: ServerWorld, session: BloodMoonBattleSession): DisplayEntity.TextDisplayEntity? {
        val uuid = session.bedStatusDisplayUuid ?: return null
        return world.getEntity(uuid) as? DisplayEntity.TextDisplayEntity
    }

    private fun createBattleBedDisplay(world: ServerWorld, session: BloodMoonBattleSession): DisplayEntity.TextDisplayEntity {
        val center = session.bedKey.center()
        val display = DisplayEntity.TextDisplayEntity(EntityType.TEXT_DISPLAY, world)
        display.setPosition(center.x, center.y + 1.35, center.z)
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
        display.setText(battleBedStatusText(session))
        world.spawnEntity(display)
        session.bedStatusDisplayUuid = display.uuid
        return display
    }

    private fun removeBattleBedDisplay(world: ServerWorld, session: BloodMoonBattleSession) {
        resolveBattleBedDisplay(world, session)?.discard()
        session.bedStatusDisplayUuid = null
    }

    private fun updateBattleMobPressure(world: ServerWorld, session: BloodMoonBattleSession) {
        for (mobUuid in session.activeMobUuids.toList()) {
            if (activeSession !== session) {
                return
            }
            val hostile = world.getEntity(mobUuid) as? HostileEntity ?: continue
            if (!hostile.isAlive || hostile.isRemoved) {
                continue
            }
            steerMobTowardBed(hostile, session.bedKey.center())
            tickMobBlockBreaking(world, session, hostile)
            maybeDamageBattleBed(world, session, hostile)
        }
    }

    private fun tickMobBlockBreaking(world: ServerWorld, session: BloodMoonBattleSession, hostile: HostileEntity) {
        if (session.phase != BloodMoonBattlePhase.COMBAT) {
            session.mobBlockBreakStates.remove(hostile.uuid)
            return
        }
        val bedCenter = session.bedKey.center()
        val raycast = world.raycast(
            RaycastContext(hostile.eyePos, bedCenter, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, hostile)
        )
        val blockTarget: BlockPos? = if (raycast.type == HitResult.Type.BLOCK) {
            val hitPos = raycast.blockPos
            if (!session.bedKey.contains(hitPos) &&
                hostile.squaredDistanceTo(hitPos.toCenterPos()) <= MOB_BLOCK_BREAK_REACH_SQUARED) {
                val state = world.getBlockState(hitPos)
                if (!state.isAir && state.getHardness(world, hitPos) >= 0.0f) hitPos.toImmutable() else null
            } else null
        } else null

        val existing = session.mobBlockBreakStates[hostile.uuid]
        if (blockTarget == null) {
            session.mobBlockBreakStates.remove(hostile.uuid)
            return
        }
        if (existing == null || existing.targetPos != blockTarget) {
            session.mobBlockBreakStates[hostile.uuid] = MobBlockBreakState(blockTarget, world.time)
            return
        }
        val isElite = (hostile as? AdventureRankMobAccess)?.cresoraIsEliteMob() == true
        val interval = if (isElite) ELITE_BLOCK_BREAK_INTERVAL_TICKS else MOB_BLOCK_BREAK_INTERVAL_TICKS
        if (world.time - existing.startTick >= interval) {
            val blockState = world.getBlockState(blockTarget)
            if (!blockState.isAir) {
                world.playSound(null, blockTarget, blockState.soundGroup.breakSound, SoundCategory.BLOCKS, 1.0f, 0.8f)
                world.breakBlock(blockTarget, false)
            }
            session.mobBlockBreakStates.remove(hostile.uuid)
        }
    }

    private fun steerMobTowardBed(hostile: HostileEntity, center: Vec3d) {
        hostile.target = null
        if (hostile.age % BED_PATH_REFRESH_TICKS.toInt() != 0) {
            return
        }
        hostile.navigation.startMovingTo(center.x, center.y, center.z, BED_PATH_SPEED)
    }

    private fun maybeDamageBattleBed(world: ServerWorld, session: BloodMoonBattleSession, hostile: HostileEntity) {
        if (session.phase != BloodMoonBattlePhase.COMBAT || session.bedInvulnerableUntilNextWave) {
            return
        }
        val bedCenter = session.bedKey.center()
        if (hostile.squaredDistanceTo(bedCenter) > BED_ATTACK_RANGE_SQUARED) {
            return
        }
        val raycast = world.raycast(RaycastContext(hostile.eyePos, bedCenter, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, hostile))
        if (raycast.type == HitResult.Type.BLOCK) {
            return
        }
        val nextAttackTick = session.mobBedAttackCooldowns[hostile.uuid] ?: 0L
        if (world.time < nextAttackTick) {
            return
        }
        session.mobBedAttackCooldowns[hostile.uuid] = world.time + BED_ATTACK_INTERVAL_TICKS
        hostile.swingHand(Hand.MAIN_HAND)
        world.playSound(null, session.bedKey.first, SoundEvents.ENTITY_ZOMBIE_ATTACK_WOODEN_DOOR, SoundCategory.HOSTILE, 0.9f, 0.9f)
        applyBattleBedDamage(world, session, battleBedDamage(hostile))
    }

    private fun applyBattleBedDamage(world: ServerWorld, session: BloodMoonBattleSession, amount: Double) {
        if (amount <= 0.0 || session.bedInvulnerableUntilNextWave) {
            return
        }
        val before = session.bedDurabilityPercent
        val nextThreshold = nextBedGuardThreshold(session, before)
        val after = before - amount
        if (nextThreshold != null && after < nextThreshold) {
            session.bedDurabilityPercent = nextThreshold.toDouble()
            session.triggeredBedGuardThresholds += nextThreshold
            session.bedInvulnerableUntilNextWave = true
            world.playSound(null, session.bedKey.first, SoundEvents.BLOCK_RESPAWN_ANCHOR_CHARGE, SoundCategory.BLOCKS, 0.85f, 1.35f)
            serverMessageToParticipants(
                world.server,
                session,
                Text.translatable("message.cresora.blood_moon.bed_guard", nextThreshold).formatted(Formatting.RED),
                false
            )
            return
        }
        if (after <= 0.0) {
            session.bedDurabilityPercent = 0.0
            failBattle(world.server, world, session)
            return
        }
        session.bedDurabilityPercent = after
        world.spawnParticles(ParticleTypes.DAMAGE_INDICATOR, session.bedKey.center().x, session.bedKey.center().y + 0.2, session.bedKey.center().z, 3, 0.25, 0.1, 0.25, 0.0)
    }

    private fun battleBedDamage(hostile: HostileEntity): Double {
        val rank = AdventureRankService.mobRank(hostile)
        val eliteBonus = if ((hostile as? AdventureRankMobAccess)?.cresoraIsEliteMob() == true) 1.5 else 0.0
        val witherBonus = if (hostile is WitherEntity) 2.5 else 0.0
        return (1.5 + rank * 0.12 + eliteBonus + witherBonus).coerceAtMost(12.0)
    }

    private fun nextBedGuardThreshold(session: BloodMoonBattleSession, durabilityBeforeHit: Double): Int? {
        return listOf(75, 50, 25).firstOrNull { threshold ->
            threshold !in session.triggeredBedGuardThresholds && durabilityBeforeHit > threshold
        }
    }

    private fun tryRecoverRewardChest(world: ServerWorld, pos: BlockPos): ResolvedBloodMoonRewardChest? {
        val state = world.getBlockState(pos)
        if (!isOurRewardChest(state.block)) {
            return null
        }
        val persisted = rewardChestState?.let {
            BloodMoonRewardChestStateService.find(it, world.registryKey.value.toString(), pos)
        } ?: return null
        val owner = runCatching { UUID.fromString(persisted.ownerUuid) }.getOrNull() ?: return null
        val seed = persisted.rewardSeed
        val rewardKey = rewardChestKey(world.registryKey, pos)
        if (!rewardChestsByPos.containsKey(rewardKey)) {
            rewardChestsByPos[rewardKey] = BloodMoonRewardChest(owner, world.registryKey, pos, buildRewardBundle(seed))
        }
        return ResolvedBloodMoonRewardChest(owner, seed)
    }

    private fun ensureRewardStateLoaded(server: MinecraftServer) {
        if (rewardChestState == null) {
            rewardChestState = BloodMoonRewardChestStateService.get(server)
        }
    }

    private fun battleBedStatusText(session: BloodMoonBattleSession): Text {
        val key = if (session.bedInvulnerableUntilNextWave) {
            "message.cresora.blood_moon.bed_status_guarded"
        } else {
            "message.cresora.blood_moon.bed_status"
        }
        return Text.translatable(key, formatBedDurability(session.bedDurabilityPercent))
            .formatted(battleBedStatusColor(session.bedDurabilityPercent))
    }

    private fun battleBedStatusColor(durabilityPercent: Double): Formatting {
        return when {
            durabilityPercent <= 25.0 -> Formatting.DARK_RED
            durabilityPercent <= 50.0 -> Formatting.RED
            durabilityPercent <= 75.0 -> Formatting.GOLD
            else -> Formatting.GREEN
        }
    }

    private fun formatBedDurability(value: Double): String {
        val rounded = kotlin.math.round(value * 10.0) / 10.0
        return if (abs(rounded - rounded.roundToInt().toDouble()) < 1.0e-6) {
            rounded.roundToInt().toString()
        } else {
            rounded.toString()
        }
    }

    private fun teardownBloodMoon(server: MinecraftServer, clearNightState: Boolean): Boolean {
        var changed = false
        if (clearNightState) {
            val moonChanged = MoonPhaseService.clearBloodMoon(server)
            changed = changed || moonChanged
        }
        if (pendingConfirmations.isNotEmpty()) {
            pendingConfirmations.clear()
            changed = true
        }
        val session = activeSession
        if (session != null) {
            val world = server.getWorld(session.bedKey.worldKey)
            if (world != null) {
                for (mobUuid in session.activeMobUuids) {
                    world.getEntity(mobUuid)?.discard()
                }
                removeBattleBedDisplay(world, session)
                restoreBattleBed(server, session)
            }
            restoreParticipantRespawns(server, session.participants)
            activeSession = null
            changed = true
        }
        if ((persistentState?.lockedBattleDayIndex ?: -1L) >= 0L) {
            persistentState?.lockedBattleDayIndex = -1L
            persistentState?.markDirty()
            changed = true
        }
        if (persistentState?.originalMobGriefing != null) {
            restoreMobGriefing(server)
            changed = true
        }
        return changed
    }

    private fun isOurRewardChest(block: Block): Boolean = block == Blocks.CHEST

    private fun rewardChestKey(worldKey: RegistryKey<World>, pos: BlockPos): BloodMoonWorldPosKey {
        return BloodMoonWorldPosKey(worldKey, pos)
    }

    private fun bloodMoonRankXpReward(player: ServerPlayerEntity): Int {
        val requiredXp = AdventureRankService.getProgress(player).requiredXp ?: return 0
        return ceil(requiredXp * 0.5).toInt()
    }

    private fun failBattle(server: MinecraftServer, world: ServerWorld, session: BloodMoonBattleSession) {
        destroyBattleBed(world, session)
        for (mobUuid in session.activeMobUuids) {
            world.getEntity(mobUuid)?.discard()
        }
        session.activeMobUuids.clear()
        restoreMobGriefing(server)
        restoreParticipantRespawns(server, session.participants)
        serverMessageToParticipants(
            server,
            session,
            Text.translatable("message.cresora.blood_moon.failed").formatted(Formatting.DARK_RED),
            false
        )
        activeSession = null
    }

    private fun currentDayIndex(server: MinecraftServer): Long = server.overworld.timeOfDay / 24000L
}
