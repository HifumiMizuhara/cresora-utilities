package hifumi.cresora

import net.minecraft.block.Blocks
import net.minecraft.entity.Entity
import net.minecraft.entity.ItemEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.SpawnReason
import net.minecraft.entity.damage.DamageSource
import net.minecraft.entity.mob.HostileEntity
import net.minecraft.item.ItemStack
import net.minecraft.registry.Registries
import net.minecraft.registry.RegistryKey
import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.server.world.ServerWorld
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Box
import net.minecraft.util.math.Vec3d
import net.minecraft.world.World
import java.util.UUID
import kotlin.math.max

data class MasqueradeStartResult(
    val success: Boolean,
    val translationKey: String,
    val args: List<Any> = emptyList()
)

private data class MasqueradeReturnPoint(
    val worldKey: RegistryKey<World>,
    val position: Vec3d,
    val yaw: Float,
    val pitch: Float
)

private data class MasqueradeInventorySnapshot(
    val mainInventory: List<ItemStack>,
    val offHand: ItemStack,
    val selectedSlot: Int
)

private data class MasqueradeRuntimeMob(
    val sessionId: UUID,
    val damageScalar: Double,
    val damageTakenMultiplier: Double
)

private enum class MasqueradePhase {
    PREPARING_WAVE,
    COMBAT,
    SUPPORT_SELECTION
}

private class MasqueradeSession(
    val id: UUID,
    val playerUuid: UUID,
    val arenaCenter: BlockPos,
    val returnPoint: MasqueradeReturnPoint,
    val inventorySnapshot: MasqueradeInventorySnapshot
) {
    val activeMobUuids: MutableSet<UUID> = linkedSetOf()
    val acquiredSupportBuffIds: MutableSet<String> = linkedSetOf()
    var phase: MasqueradePhase = MasqueradePhase.PREPARING_WAVE
    var nextWaveIndex: Int = 0
    var clearedWaveCount: Int = 0
    var nextEventTick: Long = 0L
    var currentWaveStartedTick: Long = 0L
    var lastActionBarSecond: Int = -1
    var supportCandidateIds: List<String> = emptyList()
    var supportScreenReopenTick: Long = 0L
    var skipMultipleOfFourCharges: Int = 0
    var guardCooldownExpireTick: Long = 0L

    fun currentWaveNumber(): Int = nextWaveIndex + 1

    fun loanMarker(): String = id.toString()
}

object MasqueradeService {
    private const val ARENA_FAIL_DISTANCE_SQUARED = 34.0 * 34.0
    private const val START_DELAY_TICKS = 40L
    private const val BETWEEN_WAVE_DELAY_TICKS = 20L
    private const val SUPPORT_REOPEN_TICKS = 20L
    private const val MAX_PLAYER_DAMAGE_REDUCTION_RATIO = 0.50

    private val sessionsByPlayer: MutableMap<UUID, MasqueradeSession> = linkedMapOf()
    private val sessionsById: MutableMap<UUID, MasqueradeSession> = linkedMapOf()
    private val mobRuntime: MutableMap<UUID, MasqueradeRuntimeMob> = linkedMapOf()
    private val pendingRespawnSnapshots: MutableMap<UUID, MasqueradeInventorySnapshot> = linkedMapOf()

    fun hasActiveSession(player: ServerPlayerEntity): Boolean = sessionsByPlayer.containsKey(player.uuid)

    fun clampPlayerDamageReduction(player: ServerPlayerEntity, rawReductionRatio: Double): Double {
        if (!hasActiveSession(player)) {
            return rawReductionRatio
        }
        return rawReductionRatio.coerceAtMost(MAX_PLAYER_DAMAGE_REDUCTION_RATIO)
    }

    fun currentWave(player: ServerPlayerEntity): Int = sessionsByPlayer[player.uuid]?.currentWaveNumber() ?: 0

    fun clearedWaveCount(player: ServerPlayerEntity): Int = sessionsByPlayer[player.uuid]?.clearedWaveCount ?: 0

    fun supportCandidateIds(player: ServerPlayerEntity): List<String> = sessionsByPlayer[player.uuid]?.supportCandidateIds ?: emptyList()

    fun getAggregatedSupportStats(player: net.minecraft.entity.player.PlayerEntity): Map<StatType, Double> {
        val serverPlayer = player as? ServerPlayerEntity ?: return emptyMap()
        val session = sessionsByPlayer[serverPlayer.uuid] ?: return emptyMap()
        val totals = linkedMapOf<StatType, Double>()
        for (buffId in session.acquiredSupportBuffIds) {
            val definition = runCatching { MasqueradeContentRegistry.supportBuff(buffId) }.getOrNull() ?: continue
            if (definition.effectId != "stat_bonus" || definition.statType == null) {
                continue
            }
            totals[definition.statType] = (totals[definition.statType] ?: 0.0) + definition.statValue
        }
        return totals
    }

    fun startSession(player: ServerPlayerEntity, selectedInventorySlots: List<Int>): MasqueradeStartResult {
        val definition = MasqueradeContentRegistry.definition()
        MasqueradeProgressService.getProgress(player)
        if (sessionsByPlayer.containsKey(player.uuid)) {
            return MasqueradeStartResult(false, "screen.cresora.masquerade.already_active")
        }
        if (DomainService.hasActiveSession(player)) {
            return MasqueradeStartResult(false, "screen.cresora.masquerade.blocked_domain")
        }
        if (StoryService.hasActiveSession(player)) {
            return MasqueradeStartResult(false, "screen.cresora.masquerade.blocked_story")
        }
        val rank = AdventureRankService.getRank(player)
        if (rank < definition.unlockRank) {
            return MasqueradeStartResult(false, "screen.cresora.masquerade.locked", listOf(definition.unlockRank))
        }
        val normalizedSlots = selectedInventorySlots.distinct()
        if (normalizedSlots.isEmpty()) {
            return MasqueradeStartResult(false, "screen.cresora.masquerade.no_weapon")
        }
        if (normalizedSlots.size > 4) {
            return MasqueradeStartResult(false, "screen.cresora.masquerade.too_many_weapons")
        }
        val selectedWeapons = normalizedSlots.mapNotNull { slot ->
            player.inventory.getStack(slot).takeIf(WeaponStackSupport::isWeapon)?.copy()
        }
        if (selectedWeapons.size != normalizedSlots.size) {
            return MasqueradeStartResult(false, "screen.cresora.masquerade.invalid_loadout")
        }

        val server = player.server ?: return MasqueradeStartResult(false, "screen.cresora.masquerade.invalid")
        val world = ArenaManager.getDomainWorld(server) ?: return MasqueradeStartResult(false, "screen.cresora.domain.no_world")
        
        val arenaCenter = ArenaManager.getArenaPosForPlayer(player.uuid)
        ArenaManager.ensureArena(world, arenaCenter)

        val snapshot = snapshotInventory(player)
        val session = MasqueradeSession(
            id = UUID.randomUUID(),
            playerUuid = player.uuid,
            arenaCenter = arenaCenter,
            returnPoint = MasqueradeReturnPoint(player.world.registryKey, player.pos, player.yaw, player.pitch),
            inventorySnapshot = snapshot
        )
        session.nextEventTick = world.time + START_DELAY_TICKS
        sessionsByPlayer[player.uuid] = session
        sessionsById[session.id] = session

        prepareRunInventory(player, session, selectedWeapons)
        player.health = player.maxHealth
        teleportPlayer(player, world, Vec3d(arenaCenter.x + 0.5, arenaCenter.y + 1.0, arenaCenter.z + 0.5), 180.0f, 0.0f)
        player.sendMessage(Text.translatable("screen.cresora.masquerade.entered"), false)
        return MasqueradeStartResult(true, "screen.cresora.masquerade.entered")
    }

    fun selectSupportBuff(player: ServerPlayerEntity, buffId: String): MasqueradeStartResult {
        val session = sessionsByPlayer[player.uuid] ?: return MasqueradeStartResult(false, "screen.cresora.masquerade.no_active_session")
        if (session.phase != MasqueradePhase.SUPPORT_SELECTION) {
            return MasqueradeStartResult(false, "screen.cresora.masquerade.support.not_ready")
        }
        if (!session.supportCandidateIds.contains(buffId)) {
            return MasqueradeStartResult(false, "screen.cresora.masquerade.support.invalid")
        }
        session.acquiredSupportBuffIds += buffId
        val definition = MasqueradeContentRegistry.supportBuff(buffId)
        when (definition.effectId) {
            "skip_multiple_of_four" -> session.skipMultipleOfFourCharges += 1
            "guard_every_five_wave" -> Unit
            "stat_bonus" -> Unit
        }
        session.supportCandidateIds = emptyList()
        session.phase = MasqueradePhase.PREPARING_WAVE
        session.nextEventTick = (player.world as? ServerWorld)?.time?.plus(BETWEEN_WAVE_DELAY_TICKS) ?: 0L
        player.sendMessage(Text.translatable("screen.cresora.masquerade.support.selected", Text.translatable(definition.nameKey)), false)
        return MasqueradeStartResult(true, "screen.cresora.masquerade.support.selected", listOf(Text.translatable(definition.nameKey)))
    }

    fun tick(server: MinecraftServer) {
        for (session in sessionsById.values.toList()) {
            tickSession(server, session)
        }
    }

    fun onPlayerDeath(player: ServerPlayerEntity) {
        val session = sessionsByPlayer[player.uuid] ?: return
        val server = player.server ?: return
        endSession(server, session, player, EndReason.DEATH)
    }

    fun onPlayerDisconnect(player: ServerPlayerEntity) {
        val session = sessionsByPlayer[player.uuid] ?: return
        val server = player.server ?: return
        endSession(server, session, player, EndReason.DISCONNECT)
    }

    fun clearTransientState(player: ServerPlayerEntity) {
        pendingRespawnSnapshots.remove(player.uuid)
    }

    fun restoreAfterRespawn(newPlayer: ServerPlayerEntity) {
        val snapshot = pendingRespawnSnapshots.remove(newPlayer.uuid) ?: return
        restoreSnapshot(newPlayer, snapshot)
    }

    fun damageMultiplier(attacker: Entity?): Double {
        val runtime = attacker?.uuid?.let(mobRuntime::get) ?: return 1.0
        return runtime.damageScalar
    }

    fun damageTakenMultiplier(target: HostileEntity): Double {
        return mobRuntime[target.uuid]?.damageTakenMultiplier ?: 1.0
    }

    fun adjustIncomingDamage(player: PlayerEntity, source: DamageSource, amount: Float): Float {
        val serverPlayer = player as? ServerPlayerEntity ?: return amount
        val session = sessionsByPlayer[serverPlayer.uuid] ?: return amount
        if (!session.acquiredSupportBuffIds.contains("guard_every_five_wave")) {
            return amount
        }
        val definition = MasqueradeContentRegistry.supportBuff("guard_every_five_wave")
        if (definition.triggerWaveMultiple <= 0 || session.currentWaveNumber() % definition.triggerWaveMultiple != 0) {
            return amount
        }
        val runtime = source.attacker?.uuid?.let(mobRuntime::get) ?: return amount
        if (runtime.sessionId != session.id) {
            return amount
        }
        val now = currentWorldTime(serverPlayer)
        if (now < session.guardCooldownExpireTick) {
            return amount
        }
        session.guardCooldownExpireTick = now + definition.cooldownSeconds * 20L
        if (serverPlayer.random.nextDouble() <= definition.procChance) {
            serverPlayer.sendMessage(Text.translatable("screen.cresora.masquerade.support.guard_triggered").formatted(Formatting.AQUA), true)
            return 0.0f
        }
        return amount
    }

    private enum class EndReason {
        CLEARED,
        TIMEOUT,
        DEATH,
        LEAVE_ARENA,
        DISCONNECT
    }

    private fun tickSession(server: MinecraftServer, session: MasqueradeSession) {
        val player = server.playerManager.getPlayer(session.playerUuid)
        if (player == null) {
            cleanupSession(server, session)
            return
        }
        
        val world = ArenaManager.getDomainWorld(server) ?: return
        if (player.world.registryKey != ArenaManager.DOMAIN_WORLD_KEY || player.squaredDistanceTo(session.arenaCenter.toCenterPos()) > ARENA_FAIL_DISTANCE_SQUARED) {
            endSession(server, session, player, EndReason.LEAVE_ARENA)
            return
        }

        cleanupInactiveMobs(world, session)

        when (session.phase) {
            MasqueradePhase.PREPARING_WAVE -> tickPreparingPhase(world, session, player)
            MasqueradePhase.COMBAT -> tickCombatPhase(server, world, session, player)
            MasqueradePhase.SUPPORT_SELECTION -> tickSupportSelectionPhase(world, session, player)
        }
    }

    private fun tickPreparingPhase(world: ServerWorld, session: MasqueradeSession, player: ServerPlayerEntity) {
        if (world.time < session.nextEventTick) {
            return
        }
        val definition = MasqueradeContentRegistry.definition()
        if (session.nextWaveIndex >= definition.maxWaveCount) {
            endSession(player.server ?: return, session, player, EndReason.CLEARED)
            return
        }
        val wave = MasqueradeContentRegistry.wave(session.currentWaveNumber())
        if (session.skipMultipleOfFourCharges > 0 && wave.waveNumber % 4 == 0) {
            session.skipMultipleOfFourCharges -= 1
            session.clearedWaveCount += 1
            session.nextWaveIndex += 1
            player.sendMessage(Text.translatable("screen.cresora.masquerade.wave_skipped", wave.waveNumber), false)
            afterWaveCleared(world, session, player)
            return
        }
        spawnWave(world, session, player, wave)
    }

    private fun tickCombatPhase(server: MinecraftServer, world: ServerWorld, session: MasqueradeSession, player: ServerPlayerEntity) {
        val wave = MasqueradeContentRegistry.wave(session.currentWaveNumber())
        val elapsedTicks = (world.time - session.currentWaveStartedTick).coerceAtLeast(0L)
        val remainingSeconds = max(0, wave.timeLimitSeconds - (elapsedTicks / 20L).toInt())
        if (remainingSeconds != session.lastActionBarSecond) {
            session.lastActionBarSecond = remainingSeconds
            player.sendMessage(
                Text.translatable(
                    "screen.cresora.masquerade.timer",
                    session.currentWaveNumber(),
                    MasqueradeContentRegistry.definition().maxWaveCount,
                    remainingSeconds
                ),
                true
            )
        }
        if (remainingSeconds <= 0) {
            endSession(server, session, player, EndReason.TIMEOUT)
            return
        }
        if (session.activeMobUuids.isEmpty()) {
            session.clearedWaveCount += 1
            session.nextWaveIndex += 1
            afterWaveCleared(world, session, player)
        }
    }

    private fun tickSupportSelectionPhase(world: ServerWorld, session: MasqueradeSession, player: ServerPlayerEntity) {
        if (session.supportCandidateIds.isEmpty()) {
            session.phase = MasqueradePhase.PREPARING_WAVE
            session.nextEventTick = world.time + BETWEEN_WAVE_DELAY_TICKS
            return
        }
        if (world.time >= session.supportScreenReopenTick) {
            if (player.currentScreenHandler !is MasqueradeSupportScreenHandler) {
                ArtifactUiFlow.openMasqueradeSupport(player)
            }
            session.supportScreenReopenTick = world.time + SUPPORT_REOPEN_TICKS
        }
    }

    private fun spawnWave(world: ServerWorld, session: MasqueradeSession, player: ServerPlayerEntity, wave: MasqueradeWaveDefinition) {
        for ((spawnIndex, spawn) in wave.spawns.withIndex()) {
            val entityType = Registries.ENTITY_TYPE.get(Identifier.of(spawn.entityTypeId))
            repeat(spawn.count.coerceAtLeast(1)) { countIndex ->
                val spawnPos = BlockPos.ofFloored(
                    session.arenaCenter.x + 0.5 + randomOffset(world.random, spawnIndex * 17 + countIndex),
                    session.arenaCenter.y + 1.0,
                    session.arenaCenter.z + 0.5 + randomOffset(world.random, spawnIndex * 37 + countIndex + 11)
                )
                val hostile = entityType.spawn(world, null, spawnPos, SpawnReason.EVENT, true, false) as? HostileEntity ?: return@repeat
                val access = hostile as? AdventureRankMobAccess ?: return@repeat
                access.cresoraSetMobAdventureRank(spawn.rank)
                FieldMobPackService.markExplicit(hostile, spawn.elite)
                AdventureRankService.applyMobScaling(
                    hostile,
                    spawn.rank,
                    spawn.modifiers.healthScalar,
                    spawn.modifiers.defenseScalar,
                    spawn.modifiers.toughnessScalar
                )
                hostile.target = player
                if (spawn.elite) {
                    hostile.setPersistent()
                }
                mobRuntime[hostile.uuid] = MasqueradeRuntimeMob(
                    session.id,
                    spawn.modifiers.damageScalar,
                    (1.0 - spawn.modifiers.damageReductionPercent / 100.0).coerceIn(0.05, 1.0)
                )
                session.activeMobUuids += hostile.uuid
            }
        }
        session.phase = MasqueradePhase.COMBAT
        session.currentWaveStartedTick = world.time
        session.lastActionBarSecond = -1
        player.sendMessage(Text.translatable("screen.cresora.masquerade.wave_started", wave.waveNumber, wave.timeLimitSeconds), false)
    }

    private fun afterWaveCleared(world: ServerWorld, session: MasqueradeSession, player: ServerPlayerEntity) {
        if (session.nextWaveIndex >= MasqueradeContentRegistry.definition().maxWaveCount) {
            endSession(player.server ?: return, session, player, EndReason.CLEARED)
            return
        }
        val nextWaveNumber = session.currentWaveNumber()
        val candidates = rollSupportCandidates(player, session, nextWaveNumber)
        if (candidates.isEmpty()) {
            session.phase = MasqueradePhase.PREPARING_WAVE
            session.nextEventTick = world.time + BETWEEN_WAVE_DELAY_TICKS
            player.sendMessage(Text.translatable("screen.cresora.masquerade.wave_cleared", session.clearedWaveCount), false)
            return
        }
        session.phase = MasqueradePhase.SUPPORT_SELECTION
        session.supportCandidateIds = candidates
        session.supportScreenReopenTick = world.time
        player.sendMessage(Text.translatable("screen.cresora.masquerade.support.prompt"), false)
        ArtifactUiFlow.openMasqueradeSupport(player)
    }

    private fun rollSupportCandidates(player: ServerPlayerEntity, session: MasqueradeSession, upcomingWaveNumber: Int): List<String> {
        val eligible = MasqueradeContentRegistry.supportBuffs().filter { buff ->
            buff.id !in session.acquiredSupportBuffIds &&
                upcomingWaveNumber in buff.eligibility.minWave..buff.eligibility.maxWave
        }.toMutableList()
        if (eligible.isEmpty()) {
            return emptyList()
        }
        val selected = mutableListOf<String>()
        repeat(3.coerceAtMost(eligible.size)) {
            val totalWeight = eligible.sumOf(MasqueradeSupportBuffDefinition::weight)
            var roll = player.random.nextDouble() * totalWeight
            val iterator = eligible.iterator()
            var chosen: MasqueradeSupportBuffDefinition? = null
            while (iterator.hasNext()) {
                val candidate = iterator.next()
                roll -= candidate.weight
                if (roll <= 0.0) {
                    chosen = candidate
                    iterator.remove()
                    break
                }
            }
            val resolved = chosen ?: eligible.removeLastOrNull()
            if (resolved != null) {
                selected += resolved.id
            }
        }
        return selected
    }

    private fun endSession(server: MinecraftServer, session: MasqueradeSession, player: ServerPlayerEntity?, reason: EndReason) {
        val reward = MasqueradeContentRegistry.definition().rewardPerClearedWave
        val cleared = session.clearedWaveCount
        val credits = cleared * reward.creditsPerWave
        val chordProgression = cleared * reward.chordProgressionPerWave
        if (player != null) {
            MasqueradeProgressService.recordRun(player, cleared)
            if (credits > 0) {
                CreditsService.addCredits(player, credits)
            }
            if (chordProgression > 0) {
                ResonanceService.addCurrency(player, ResonanceCurrencyType.CHORD_PROGRESSION, chordProgression)
            }
        }
        cleanupSession(server, session)
        when (reason) {
            EndReason.DEATH -> {
                cleanupRunItems(server.overworld, player?.pos ?: session.arenaCenter.toCenterPos(), session.loanMarker())
                pendingRespawnSnapshots[session.playerUuid] = session.inventorySnapshot
            }
            EndReason.DISCONNECT -> {
                pendingRespawnSnapshots.remove(session.playerUuid)
            }
            else -> if (player != null && player.isAlive) {
                restoreSnapshot(player, session.inventorySnapshot)
                restorePlayerPosition(server, player, session.returnPoint)
            }
        }
        if (player != null && player.isAlive) {
            when (reason) {
                EndReason.CLEARED -> player.sendMessage(Text.translatable("screen.cresora.masquerade.completed", cleared), false)
                EndReason.TIMEOUT -> player.sendMessage(Text.translatable("screen.cresora.masquerade.timeout", cleared), false)
                EndReason.LEAVE_ARENA -> player.sendMessage(Text.translatable("screen.cresora.masquerade.failed_leave", cleared), false)
                EndReason.DISCONNECT -> Unit
                EndReason.DEATH -> Unit
            }
            val displayStacks = ArtifactDisplayStackFactory.masqueradeResultDisplayStacks(cleared, credits, chordProgression)
            if (displayStacks.isNotEmpty()) {
                ArtifactUiFlow.openRewardSummary(player, Text.translatable("screen.cresora.masquerade.result"), displayStacks)
            }
        }
    }

    private fun cleanupSession(server: MinecraftServer, session: MasqueradeSession) {
        val world = ArenaManager.getDomainWorld(server) ?: return
        for (mobUuid in session.activeMobUuids) {
            (world.getEntity(mobUuid) as? HostileEntity)?.discard()
            mobRuntime.remove(mobUuid)
        }
        session.activeMobUuids.clear()
        cleanupRunItems(world, session.arenaCenter.toCenterPos(), session.loanMarker())
        sessionsByPlayer.remove(session.playerUuid)
        sessionsById.remove(session.id)
    }

    private fun cleanupInactiveMobs(world: ServerWorld, session: MasqueradeSession) {
        val iterator = session.activeMobUuids.iterator()
        while (iterator.hasNext()) {
            val mobUuid = iterator.next()
            val entity = world.getEntity(mobUuid) as? HostileEntity
            if (entity == null || entity.isRemoved || !entity.isAlive) {
                mobRuntime.remove(mobUuid)
                iterator.remove()
            }
        }
    }

    private fun snapshotInventory(player: ServerPlayerEntity): MasqueradeInventorySnapshot {
        val mainInventory = (0 until 36).map { slot -> player.inventory.getStack(slot).copy() }
        return MasqueradeInventorySnapshot(
            mainInventory = mainInventory,
            offHand = player.offHandStack.copy(),
            selectedSlot = player.inventory.selectedSlot
        )
    }

    private fun prepareRunInventory(player: ServerPlayerEntity, session: MasqueradeSession, selectedWeapons: List<ItemStack>) {
        for (slot in 0 until 36) {
            player.inventory.setStack(slot, ItemStack.EMPTY)
        }
        player.setStackInHand(net.minecraft.util.Hand.OFF_HAND, ItemStack.EMPTY)
        for ((index, stack) in selectedWeapons.withIndex()) {
            val prepared = stack.copy()
            prepared.set(ModDataComponents.MASQUERADE_SESSION_ID, session.loanMarker())
            player.inventory.setStack(index, prepared)
        }
        player.inventory.selectedSlot = 0
        player.inventory.markDirty()
        player.playerScreenHandler.sendContentUpdates()
    }

    private fun restoreSnapshot(player: ServerPlayerEntity, snapshot: MasqueradeInventorySnapshot) {
        for (slot in 0 until 36) {
            player.inventory.setStack(slot, snapshot.mainInventory.getOrNull(slot)?.copy() ?: ItemStack.EMPTY)
        }
        player.setStackInHand(net.minecraft.util.Hand.OFF_HAND, snapshot.offHand.copy())
        player.inventory.selectedSlot = snapshot.selectedSlot.coerceIn(0, 8)
        player.inventory.markDirty()
        player.playerScreenHandler.sendContentUpdates()
    }

    private fun cleanupRunItems(world: ServerWorld, center: Vec3d, marker: String) {
        val searchBox = Box(center.x - 40.0, center.y - 20.0, center.z - 40.0, center.x + 40.0, center.y + 20.0, center.z + 40.0)
        val entities = world.getOtherEntities(null, searchBox) { entity ->
            entity is ItemEntity && entity.stack.get(ModDataComponents.MASQUERADE_SESSION_ID) == marker
        }
        for (entity in entities) {
            entity.discard()
        }
    }

    private fun restorePlayerPosition(server: MinecraftServer, player: ServerPlayerEntity, returnPoint: MasqueradeReturnPoint) {
        val world = server.getWorld(returnPoint.worldKey) ?: server.overworld
        teleportPlayer(player, world, returnPoint.position, returnPoint.yaw, returnPoint.pitch)
    }

    private fun teleportPlayer(player: ServerPlayerEntity, world: ServerWorld, position: Vec3d, yaw: Float, pitch: Float) {
        player.teleport(world, position.x, position.y, position.z, setOf(), yaw, pitch, false)
    }

    private fun currentWorldTime(player: ServerPlayerEntity): Long {
        return (player.world as? ServerWorld)?.time ?: 0L
    }

    private fun randomOffset(random: net.minecraft.util.math.random.Random, salt: Int): Double {
        val direction = if ((salt + random.nextInt(1000)) % 2 == 0) 1.0 else -1.0
        return direction * (2.5 + random.nextDouble() * 4.0)
    }
}
