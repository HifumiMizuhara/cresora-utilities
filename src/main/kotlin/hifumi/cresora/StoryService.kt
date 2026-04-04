package hifumi.cresora

import net.minecraft.block.Blocks
import net.minecraft.entity.SpawnReason
import net.minecraft.entity.mob.HostileEntity
import net.minecraft.registry.Registries
import net.minecraft.registry.RegistryKey
import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.server.world.ServerWorld
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d
import net.minecraft.world.World
import java.util.UUID

data class StoryStartResult(
    val success: Boolean,
    val translationKey: String,
    val args: List<Any> = emptyList()
)

private data class StoryArenaSlot(
    val index: Int,
    val center: BlockPos
)

private data class StoryReturnPoint(
    val worldKey: RegistryKey<World>,
    val position: Vec3d,
    val yaw: Float,
    val pitch: Float
)

private enum class StoryPhase {
    PRE_STORY,
    COUNTDOWN,
    COMBAT,
    POST_STORY
}

private data class StoryRuntimeMob(
    val sessionId: UUID,
    val damageTakenMultiplier: Double,
    val trueDamageImmune: Boolean
)

private class StorySession(
    val id: UUID,
    val playerUuid: UUID,
    val chapterId: String,
    val arenaIndex: Int,
    val returnPoint: StoryReturnPoint
) {
    val activeMobUuids: MutableSet<UUID> = linkedSetOf()
    var phase: StoryPhase = StoryPhase.PRE_STORY
    var dialogueIndex: Int = 0
    var dialogueAwaitingInput: Boolean = false
    var phaseInitialized: Boolean = false
    var countdownValue: Int = 3
    var nextEventTick: Long = 0L
    var waveIndex: Int = 0
    var preparingNextWave: Boolean = true
    var combatStartTick: Long = -1L
    var lastSurviveSecondsShown: Int = -1

    fun chapter(): StoryChapterDefinition = StoryContentRegistry.requireChapter(chapterId)

    fun loanMarker(): String = id.toString()
}

object StoryService {
    private const val ARENA_Y = 180
    private const val ARENA_RADIUS = 7
    private const val ARENA_FAIL_DISTANCE_SQUARED = 30.0 * 30.0
    private const val COUNTDOWN_INTERVAL_TICKS = 20L
    private const val START_DELAY_TICKS = 20L

    private val ARENA_SLOTS: List<StoryArenaSlot> = listOf(
        StoryArenaSlot(0, BlockPos(0, ARENA_Y, 128)),
        StoryArenaSlot(1, BlockPos(96, ARENA_Y, 128)),
        StoryArenaSlot(2, BlockPos(192, ARENA_Y, 128)),
        StoryArenaSlot(3, BlockPos(288, ARENA_Y, 128))
    )

    private val sessionsByPlayer: MutableMap<UUID, StorySession> = linkedMapOf()
    private val sessionsById: MutableMap<UUID, StorySession> = linkedMapOf()
    private val sessionByArenaIndex: MutableMap<Int, UUID> = linkedMapOf()
    private val mobRuntime: MutableMap<UUID, StoryRuntimeMob> = linkedMapOf()

    fun hasActiveSession(player: ServerPlayerEntity): Boolean = sessionsByPlayer.containsKey(player.uuid)

    fun damageTakenMultiplier(hostile: HostileEntity): Double {
        return mobRuntime[hostile.uuid]?.damageTakenMultiplier ?: 1.0
    }

    fun allowsTrueDamage(hostile: HostileEntity): Boolean {
        return !(mobRuntime[hostile.uuid]?.trueDamageImmune ?: false)
    }

    fun startSession(player: ServerPlayerEntity, chapterId: String): StoryStartResult {
        val chapter = runCatching { StoryContentRegistry.requireChapter(chapterId) }.getOrElse {
            return StoryStartResult(false, "commands.cresora.story.invalid", listOf(chapterId))
        }
        if (sessionsByPlayer.containsKey(player.uuid)) {
            return StoryStartResult(false, "commands.cresora.story.already_active")
        }
        if (DomainService.hasActiveSession(player)) {
            return StoryStartResult(false, "commands.cresora.story.blocked_domain")
        }
        if (MasqueradeService.hasActiveSession(player)) {
            return StoryStartResult(false, "commands.cresora.story.blocked_masquerade")
        }
        val rank = AdventureRankService.getRank(player)
        if (rank < chapter.unlockRank) {
            return StoryStartResult(false, "commands.cresora.story.locked", listOf(chapter.unlockRank))
        }
        val prerequisite = StoryProgressService.missingPrerequisite(player, chapter)
        if (prerequisite != null) {
            return StoryStartResult(false, "commands.cresora.story.locked_prerequisite", listOf(prerequisite))
        }
        if (!chapter.linkedDomainId.isNullOrBlank()) {
            return DomainService.startLinkedStoryStage(player, chapter)
        }
        val requiredFreeSlots = StoryContentRegistry.requiredFreeMainSlots(chapter)
        if (InventoryGate.freeMainSlots(player) < requiredFreeSlots) {
            return StoryStartResult(false, "commands.cresora.story.inventory_full", listOf(requiredFreeSlots))
        }
        val arena = allocateArena() ?: return StoryStartResult(false, "commands.cresora.story.no_arena")
        val server = player.server ?: return StoryStartResult(false, "commands.cresora.story.invalid", listOf(chapterId))
        val storyWorld = server.overworld
        ensureArena(storyWorld, arena.center)

        val session = StorySession(
            id = UUID.randomUUID(),
            playerUuid = player.uuid,
            chapterId = chapter.id,
            arenaIndex = arena.index,
            returnPoint = StoryReturnPoint(player.world.registryKey, player.pos, player.yaw, player.pitch)
        )
        session.nextEventTick = storyWorld.time + START_DELAY_TICKS
        sessionsByPlayer[player.uuid] = session
        sessionsById[session.id] = session
        sessionByArenaIndex[arena.index] = session.id

        grantStoryWeapons(player, session)
        player.health = player.maxHealth
        teleportPlayer(player, storyWorld, Vec3d(arena.center.x + 0.5, arena.center.y + 1.0, arena.center.z + 0.5), 180.0f, 0.0f)
        val chapterLabel = StoryTextRegistry.chapterLabel(StoryTextRegistry.resolvePlayerLocale(player), chapter)
        return StoryStartResult(true, "commands.cresora.story.started", listOf(chapterLabel))
    }

    fun tick(server: MinecraftServer) {
        for (session in sessionsById.values.toList()) {
            tickSession(server, session)
        }
    }

    fun onPlayerDeath(player: ServerPlayerEntity) {
        val session = sessionsByPlayer[player.uuid] ?: return
        val server = player.server ?: return
        failSession(server, session, player, "commands.cresora.story.failed", restorePlayer = false)
    }

    fun onPlayerDisconnect(player: ServerPlayerEntity) {
        val session = sessionsByPlayer[player.uuid] ?: return
        val server = player.server ?: return
        failSession(server, session, player, "commands.cresora.story.failed", restorePlayer = false)
    }

    fun handleDialogueAction(player: ServerPlayerEntity, actionId: String) {
        val session = sessionsByPlayer[player.uuid] ?: return
        if (session.phase != StoryPhase.PRE_STORY && session.phase != StoryPhase.POST_STORY) {
            return
        }
        if (!session.dialogueAwaitingInput) {
            return
        }
        val world = player.server?.overworld ?: return
        when (actionId) {
            StoryDialogueActionPayload.ACTION_CONTINUE -> {
                session.dialogueIndex += 1
            }

            StoryDialogueActionPayload.ACTION_SKIP -> {
                session.dialogueIndex = currentDialogueLines(session).size
            }

            else -> return
        }
        session.dialogueAwaitingInput = false
        session.nextEventTick = world.time
    }

    private fun tickSession(server: MinecraftServer, session: StorySession) {
        val player = server.playerManager.getPlayer(session.playerUuid)
        if (player == null) {
            failSession(server, session, null, "commands.cresora.story.failed", restorePlayer = false)
            return
        }
        if ((player.world as? ServerWorld)?.registryKey != World.OVERWORLD || player.squaredDistanceTo(sessionArena(session)) > ARENA_FAIL_DISTANCE_SQUARED) {
            failSession(server, session, player, "commands.cresora.story.failed", restorePlayer = true)
            return
        }

        val world = server.overworld
        cleanupInactiveMobs(world, session)

        when (session.phase) {
            StoryPhase.PRE_STORY -> tickDialoguePhase(world, session, player, session.chapter().preBattleStory, StoryPhase.COMBAT)
            StoryPhase.COUNTDOWN -> tickCountdownPhase(world, session, player)
            StoryPhase.COMBAT -> tickCombatPhase(world, session, player)
            StoryPhase.POST_STORY -> tickDialoguePhase(world, session, player, session.chapter().postBattleStory, null)
        }
    }

    private fun tickDialoguePhase(
        world: ServerWorld,
        session: StorySession,
        player: ServerPlayerEntity,
        lines: List<StoryDialogueLine>,
        nextPhase: StoryPhase?
    ) {
        if (world.time < session.nextEventTick) {
            return
        }
        if (!session.phaseInitialized) {
            session.phaseInitialized = true
            session.dialogueAwaitingInput = false
        }
        if (session.dialogueAwaitingInput) {
            return
        }
        val line = lines.getOrNull(session.dialogueIndex)
        if (line != null) {
            StoryDialogueNetworking.showDialogue(
                player,
                StoryDisplayText.chapterTitle(player, session.chapter()),
                StoryDisplayText.resolveLine(player, line),
                dialogueObjectiveText(session, player),
                dialogueHints(session, player)
            )
            session.dialogueAwaitingInput = true
            return
        }
        when (nextPhase) {
            StoryPhase.COMBAT -> {
                session.phase = StoryPhase.COUNTDOWN
                resetPhaseState(session, world.time + 20L)
                session.countdownValue = 3
                session.waveIndex = 0
                session.preparingNextWave = true
            }
            null -> completeSession(player.server ?: return, session, player)
            else -> {
                session.phase = nextPhase
                resetPhaseState(session, world.time + 20L)
            }
        }
    }

    private fun tickCountdownPhase(world: ServerWorld, session: StorySession, player: ServerPlayerEntity) {
        if (world.time < session.nextEventTick) {
            return
        }
        if (!session.phaseInitialized) {
            session.phaseInitialized = true
        }
        if (session.countdownValue > 0) {
            StoryDialogueNetworking.showCountdown(
                player,
                StoryDisplayText.chapterTitle(player, session.chapter()),
                dialogueObjectiveText(session, player),
                dialogueHints(session, player),
                session.countdownValue
            )
            session.countdownValue -= 1
            session.nextEventTick = world.time + COUNTDOWN_INTERVAL_TICKS
            return
        }
        StoryDialogueNetworking.close(player)
        session.phase = StoryPhase.COMBAT
        resetPhaseState(session, world.time)
        session.combatStartTick = world.time
        session.nextEventTick = world.time
        session.lastSurviveSecondsShown = -1
    }

    private fun tickCombatPhase(world: ServerWorld, session: StorySession, player: ServerPlayerEntity) {
        val chapter = session.chapter()
        if (chapter.battleObjective.type == StoryBattleObjectiveType.SURVIVE_TIME) {
            if (session.combatStartTick < 0L) {
                session.combatStartTick = world.time
            }
            val surviveTicks = chapter.battleObjective.durationSeconds.coerceAtLeast(1) * 20L
            val elapsedTicks = (world.time - session.combatStartTick).coerceAtLeast(0L)
            val remainingSeconds = ((((surviveTicks - elapsedTicks).coerceAtLeast(0L)) + 19L) / 20L).toInt()
            if (remainingSeconds > 0 && remainingSeconds != session.lastSurviveSecondsShown) {
                player.sendMessage(Text.translatable("commands.cresora.story.survive_remaining", remainingSeconds), true)
                session.lastSurviveSecondsShown = remainingSeconds
            }
            if (elapsedTicks >= surviveTicks) {
                session.phase = StoryPhase.POST_STORY
                resetPhaseState(session, world.time + 20L)
                for (mobUuid in session.activeMobUuids.toList()) {
                    (world.getEntity(mobUuid) as? HostileEntity)?.discard()
                    mobRuntime.remove(mobUuid)
                }
                session.activeMobUuids.clear()
                return
            }
        }
        val waves = session.chapter().battle
        if (session.preparingNextWave) {
            if (world.time >= session.nextEventTick) {
                val wave = waves.getOrNull(session.waveIndex)
                if (wave == null) {
                    session.phase = StoryPhase.POST_STORY
                    resetPhaseState(session, world.time + 20L)
                } else {
                    spawnWave(world, session, player, wave)
                }
            }
            return
        }

        if (session.activeMobUuids.isEmpty()) {
            session.waveIndex += 1
            session.preparingNextWave = true
            val nextWave = waves.getOrNull(session.waveIndex)
            session.nextEventTick = world.time + (nextWave?.spawnDelayTicks?.toLong() ?: 20L).coerceAtLeast(20L)
        }
    }

    private fun spawnWave(world: ServerWorld, session: StorySession, player: ServerPlayerEntity, wave: StoryBattleWaveDefinition) {
        val waveNumber = session.waveIndex + 1
        for ((index, spawn) in wave.spawns.withIndex()) {
            val entityType = Registries.ENTITY_TYPE.get(Identifier.of(spawn.entityTypeId))
            repeat(spawn.count) { offsetIndex ->
                val spawnPos = BlockPos.ofFloored(
                    sessionArena(session).x + randomOffset(world.random, index * 17 + offsetIndex),
                    sessionArena(session).y + 1,
                    sessionArena(session).z + randomOffset(world.random, index * 31 + offsetIndex + 7)
                )
                val hostile = entityType.spawn(world, null, spawnPos, SpawnReason.EVENT, true, false) as? HostileEntity ?: return@repeat
                val access = hostile as? AdventureRankMobAccess ?: return@repeat
                access.cresoraSetMobAdventureRank(wave.enemyRank)
                AdventureRankService.applyMobScaling(hostile, wave.enemyRank)
                hostile.target = player
                mobRuntime[hostile.uuid] = StoryRuntimeMob(
                    session.id,
                    (1.0 - wave.modifiers.damageReductionPercent / 100.0).coerceAtLeast(0.0),
                    wave.modifiers.trueDamageImmune
                )
                session.activeMobUuids += hostile.uuid
            }
        }
        session.preparingNextWave = false
        val chapterLabel = StoryTextRegistry.chapterLabel(StoryTextRegistry.resolvePlayerLocale(player), session.chapter())
        player.sendMessage(Text.translatable("commands.cresora.story.wave", chapterLabel, waveNumber, session.chapter().battle.size, wave.enemyRank), false)
    }

    private fun completeSession(server: MinecraftServer, session: StorySession, player: ServerPlayerEntity) {
        val chapter = session.chapter()
        val reward = chapter.rewards
        val firstClear = StoryProgressService.markCleared(player, chapter.id)
        val sessionRank = DomainCombatProfile.sessionRank(AdventureRankService.getRank(player))
        val domainRewards = chapter.domainRewardIds
            .map { DomainService.generateOneOffRewards(player, it, sessionRank) }
            .fold(DomainRewardResult(emptyList(), 0, 0), ::mergeDomainRewards)
        val chapterLabel = StoryTextRegistry.chapterLabel(StoryTextRegistry.resolvePlayerLocale(player), chapter)
        StoryDialogueNetworking.close(player)
        cleanupSession(server, session, player)
        restorePlayerPosition(server, player, session.returnPoint)
        val droppedItemCount = deliverRewardItems(player, domainRewards.items)
        if (domainRewards.credits > 0) {
            CreditsService.addCredits(player, domainRewards.credits)
        }
        if (domainRewards.rankXp > 0) {
            AdventureRankService.addXp(player, domainRewards.rankXp)
        }
        if (firstClear) {
            if (reward.credits > 0) {
                CreditsService.addCredits(player, reward.credits)
            }
            for (currency in reward.resonanceCurrencies) {
                if (currency.amount > 0) {
                    ResonanceService.addCurrency(player, currency.type, currency.amount)
                }
            }
        }
        player.sendMessage(Text.translatable("commands.cresora.story.cleared", chapterLabel), false)
        if (droppedItemCount > 0) {
            player.sendMessage(Text.translatable("screen.cresora.domain.reward_overflow", ArtifactSpecialItem.formatWholeNumber(droppedItemCount)), false)
        }
        if (!firstClear && domainRewards.items.isEmpty() && domainRewards.credits <= 0 && domainRewards.rankXp <= 0) {
            player.sendMessage(Text.translatable("commands.cresora.story.reward_already_claimed", chapterLabel), false)
            return
        }
        if (!firstClear) {
            player.sendMessage(Text.translatable("commands.cresora.story.reward_already_claimed", chapterLabel), false)
        }
        val rewardDisplays = buildList {
            if (firstClear) {
                addAll(StoryDisplayStackFactory.rewardDisplayStacks(StoryRewardResult(reward.credits, reward.resonanceCurrencies)))
            }
            addAll(DomainDisplayStackFactory.rewardDisplayStacks(domainRewards))
        }
        if (rewardDisplays.isNotEmpty()) {
            ArtifactUiFlow.openRewardSummary(
                player,
                Text.translatable("screen.cresora.story.reward"),
                rewardDisplays
            )
        }
    }

    private fun failSession(
        server: MinecraftServer,
        session: StorySession,
        player: ServerPlayerEntity?,
        messageKey: String,
        restorePlayer: Boolean
    ) {
        val chapterName = player?.let { StoryTextRegistry.chapterLabel(StoryTextRegistry.resolvePlayerLocale(it), session.chapter()) }
            ?: session.chapter().displayName
        player?.let(StoryDialogueNetworking::close)
        cleanupSession(server, session, player)
        if (player != null && restorePlayer && player.isAlive) {
            restorePlayerPosition(server, player, session.returnPoint)
        }
        player?.sendMessage(Text.translatable(messageKey, chapterName), false)
    }

    private fun cleanupSession(server: MinecraftServer, session: StorySession, player: ServerPlayerEntity?) {
        val world = server.overworld
        for (mobUuid in session.activeMobUuids) {
            (world.getEntity(mobUuid) as? HostileEntity)?.discard()
            mobRuntime.remove(mobUuid)
        }
        session.activeMobUuids.clear()
        removeStoryLoanWeapons(player, session.loanMarker())
        sessionsByPlayer.remove(session.playerUuid)
        sessionsById.remove(session.id)
        sessionByArenaIndex.remove(session.arenaIndex)
    }

    private fun cleanupInactiveMobs(world: ServerWorld, session: StorySession) {
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

    private fun grantStoryWeapons(player: ServerPlayerEntity, session: StorySession) {
        for (granted in session.chapter().grantedWeapons) {
            val definition = WeaponContentRegistry.requireWeapon(granted.weaponId)
            val stack = WeaponStackSupport.createWeaponStack(definition, granted.rarity, granted.baseLevel, granted.skillLevel)
            if (granted.removeOnExit) {
                stack.set(ModDataComponents.STORY_LOAN_SESSION_ID, session.loanMarker())
            }
            insertIntoMainInventory(player, stack)
        }
        player.inventory.markDirty()
        player.playerScreenHandler.sendContentUpdates()
    }

    private fun removeStoryLoanWeapons(player: ServerPlayerEntity?, marker: String) {
        val actualPlayer = player ?: return
        for (slot in 0 until actualPlayer.inventory.size()) {
            val stack = actualPlayer.inventory.getStack(slot)
            if (stack.get(ModDataComponents.STORY_LOAN_SESSION_ID) == marker) {
                actualPlayer.inventory.setStack(slot, net.minecraft.item.ItemStack.EMPTY)
            }
        }
        if (actualPlayer.mainHandStack.get(ModDataComponents.STORY_LOAN_SESSION_ID) == marker) {
            actualPlayer.setStackInHand(net.minecraft.util.Hand.MAIN_HAND, net.minecraft.item.ItemStack.EMPTY)
        }
        if (actualPlayer.offHandStack.get(ModDataComponents.STORY_LOAN_SESSION_ID) == marker) {
            actualPlayer.setStackInHand(net.minecraft.util.Hand.OFF_HAND, net.minecraft.item.ItemStack.EMPTY)
        }
        actualPlayer.inventory.markDirty()
        actualPlayer.playerScreenHandler.sendContentUpdates()
    }

    private fun insertIntoMainInventory(player: ServerPlayerEntity, stack: net.minecraft.item.ItemStack) {
        for (slot in 0 until 36) {
            if (player.inventory.getStack(slot).isEmpty) {
                player.inventory.setStack(slot, stack)
                return
            }
        }
        error("Failed to insert story-granted weapon despite entry gate reserving free slots")
    }

    private fun restorePlayerPosition(server: MinecraftServer, player: ServerPlayerEntity, returnPoint: StoryReturnPoint) {
        val world = server.getWorld(returnPoint.worldKey) ?: server.overworld
        teleportPlayer(player, world, returnPoint.position, returnPoint.yaw, returnPoint.pitch)
    }

    private fun mergeDomainRewards(left: DomainRewardResult, right: DomainRewardResult): DomainRewardResult {
        return DomainRewardResult(
            items = left.items + right.items,
            credits = left.credits + right.credits,
            rankXp = left.rankXp + right.rankXp
        )
    }

    private fun deliverRewardItems(player: ServerPlayerEntity, rewards: List<net.minecraft.item.ItemStack>): Int {
        var droppedItemCount = 0
        for (reward in rewards) {
            if (reward.isEmpty) {
                continue
            }
            val remaining = reward.copy()
            player.inventory.insertStack(remaining)
            if (!remaining.isEmpty) {
                droppedItemCount += remaining.count
                player.dropItem(remaining, false)
            }
        }
        player.playerScreenHandler.sendContentUpdates()
        return droppedItemCount
    }

    private fun teleportPlayer(player: ServerPlayerEntity, world: ServerWorld, position: Vec3d, yaw: Float, pitch: Float) {
        player.teleport(world, position.x, position.y, position.z, setOf(), yaw, pitch, false)
    }

    private fun allocateArena(): StoryArenaSlot? {
        return ARENA_SLOTS.firstOrNull { !sessionByArenaIndex.containsKey(it.index) }
    }

    private fun ensureArena(world: ServerWorld, center: BlockPos) {
        for (x in -ARENA_RADIUS..ARENA_RADIUS) {
            for (z in -ARENA_RADIUS..ARENA_RADIUS) {
                val floorPos = center.add(x, 0, z)
                val isBorder = kotlin.math.abs(x) == ARENA_RADIUS || kotlin.math.abs(z) == ARENA_RADIUS
                world.setBlockState(floorPos, if (isBorder) Blocks.DEEPSLATE_BRICKS.defaultState else Blocks.SMOOTH_STONE.defaultState)
                for (y in 1..5) {
                    val airPos = floorPos.up(y)
                    if (isBorder && y <= 2) {
                        world.setBlockState(airPos, Blocks.TINTED_GLASS.defaultState)
                    } else {
                        world.setBlockState(airPos, Blocks.AIR.defaultState)
                    }
                }
            }
        }
        world.setBlockState(center, Blocks.SEA_LANTERN.defaultState)
    }

    private fun sessionArena(session: StorySession): Vec3d {
        val arena = ARENA_SLOTS.first { it.index == session.arenaIndex }
        return Vec3d(arena.center.x + 0.5, arena.center.y + 1.0, arena.center.z + 0.5)
    }

    private fun randomOffset(random: net.minecraft.util.math.random.Random, salt: Int): Double {
        val direction = if ((salt + random.nextInt(1000)) % 2 == 0) 1.0 else -1.0
        return direction * (2.0 + random.nextDouble() * 3.2)
    }

    private fun currentDialogueLines(session: StorySession): List<StoryDialogueLine> {
        return when (session.phase) {
            StoryPhase.PRE_STORY -> session.chapter().preBattleStory
            StoryPhase.POST_STORY -> session.chapter().postBattleStory
            else -> emptyList()
        }
    }

    private fun dialogueObjectiveText(session: StorySession, player: ServerPlayerEntity): Text? {
        return when (session.phase) {
            StoryPhase.PRE_STORY, StoryPhase.COUNTDOWN -> StoryDisplayText.objectiveText(session.chapter())
            StoryPhase.POST_STORY, StoryPhase.COMBAT -> null
        }
    }

    private fun dialogueHints(session: StorySession, player: ServerPlayerEntity): List<Text> {
        return when (session.phase) {
            StoryPhase.PRE_STORY, StoryPhase.COUNTDOWN -> StoryDisplayText.combatHintTexts(player, session.chapter())
            StoryPhase.POST_STORY, StoryPhase.COMBAT -> emptyList()
        }
    }

    private fun resetPhaseState(session: StorySession, nextEventTick: Long) {
        session.phaseInitialized = false
        session.dialogueAwaitingInput = false
        session.dialogueIndex = 0
        session.nextEventTick = nextEventTick
    }
}
