package hifumi.cresora.domain
import hifumi.cresora.InventoryGate
import hifumi.cresora.adventurerank.AdventureRankMobAccess
import hifumi.cresora.adventurerank.AdventureRankProgression
import hifumi.cresora.adventurerank.AdventureRankService
import hifumi.cresora.combat.ArenaManager
import hifumi.cresora.combat.FieldMobPackService
import hifumi.cresora.credits.CreditsService
import hifumi.cresora.equipment.ArtifactSpecialItem
import hifumi.cresora.equipment.ArtifactUiFlow
import hifumi.cresora.equipment.EquipmentContentRegistry
import hifumi.cresora.equipment.EquipmentGenerationService
import hifumi.cresora.equipment.EquipmentStackSupport
import hifumi.cresora.masquerade.MasqueradeService
import hifumi.cresora.story.StoryChapterDefinition
import hifumi.cresora.story.StoryContentRegistry
import hifumi.cresora.story.StoryProgressService
import hifumi.cresora.story.StoryService
import hifumi.cresora.resonance.ResonanceCurrencyType
import hifumi.cresora.resonance.ResonanceService
import hifumi.cresora.story.StoryStartResult
import hifumi.cresora.story.StoryTextRegistry
import hifumi.cresora.weapon.WeaponContentRegistry
import hifumi.cresora.weapon.WeaponStackSupport
import hifumi.cresora.weapon.WeaponRole
import hifumi.cresora.CreSoraUtilities
import net.minecraft.block.Blocks
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityType
import net.minecraft.entity.SpawnReason
import net.minecraft.entity.mob.HostileEntity
import net.minecraft.entity.mob.MobEntity
import net.minecraft.item.ItemStack
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

data class DomainRewardResult(
    val items: List<ItemStack>,
    val credits: Int,
    val rankXp: Int,
    val chordProgression: Int = 0,
    val substituteChord: Int = 0
)

data class DomainStartResult(
    val success: Boolean,
    val translationKey: String,
    val args: List<Any> = emptyList()
)

private data class DomainReturnPoint(
    val worldKey: RegistryKey<World>,
    val position: Vec3d,
    val yaw: Float,
    val pitch: Float
)

private data class DomainRuntimeMob(
    val sessionId: UUID,
    val damageMultiplier: Double
)

private class DomainSession(
    val id: UUID,
    val playerUuid: UUID,
    val domainId: String,
    val arenaCenter: BlockPos,
    val returnPoint: DomainReturnPoint,
    val sessionRank: Int,
    val linkedStoryChapterId: String? = null
) {
    val activeMobUuids: MutableSet<UUID> = linkedSetOf()
    var nextStageIndex: Int = 0
    var nextWaveIndex: Int = 0
    var preparingNextWave: Boolean = true
    var nextSpawnTick: Long = 0L

    fun definition(): DomainDefinition = DomainContentRegistry.requireDomain(domainId)

    fun pendingWave(): DomainWaveDefinition? {
        val stage = definition().stages.getOrNull(nextStageIndex) ?: return null
        return stage.waves.getOrNull(nextWaveIndex)
    }

    fun consumePendingWave() {
        val stage = definition().stages.getOrNull(nextStageIndex) ?: return
        nextWaveIndex++
        if (nextWaveIndex >= stage.waves.size) {
            nextStageIndex++
            nextWaveIndex = 0
        }
    }

    fun totalWaveCount(): Int = definition().totalWaveCount()

    fun clearedWaveCount(): Int {
        var total = 0
        val definition = definition()
        for (stageIndex in 0 until nextStageIndex.coerceAtMost(definition.stages.size)) {
            total += definition.stages[stageIndex].waves.size
        }
        return total + nextWaveIndex
    }
}

object DomainService {
    private const val ARENA_FAIL_DISTANCE_SQUARED = 32.0 * 32.0
    private const val START_DELAY_TICKS = 40L

    private val sessionsByPlayer: MutableMap<UUID, DomainSession> = linkedMapOf()
    private val sessionsById: MutableMap<UUID, DomainSession> = linkedMapOf()
    private val mobRuntime: MutableMap<UUID, DomainRuntimeMob> = linkedMapOf()

    fun hasActiveSession(player: ServerPlayerEntity): Boolean = sessionsByPlayer.containsKey(player.uuid)

    fun generateOneOffRewards(
        player: ServerPlayerEntity,
        domainId: String,
        sessionRank: Int = DomainCombatProfile.sessionRank(AdventureRankService.getRank(player))
    ): DomainRewardResult {
        val domain = DomainContentRegistry.requireDomain(domainId)
        return generateRewardsForProfile(player, domain.rewardProfileId, sessionRank)
    }

    fun startSession(player: ServerPlayerEntity, domainId: String): DomainStartResult {
        val domain = runCatching { DomainContentRegistry.requireDomain(domainId) }.getOrElse {
            return DomainStartResult(false, "screen.cresora.domain.invalid")
        }
        return startSessionInternal(player, domain, null)
    }

    fun startLinkedStoryStage(player: ServerPlayerEntity, chapter: StoryChapterDefinition): StoryStartResult {
        val domainId = chapter.linkedDomainId ?: return StoryStartResult(false, "commands.cresora.story.invalid", listOf(chapter.id))
        val domain = runCatching { DomainContentRegistry.requireDomain(domainId) }.getOrElse {
            return StoryStartResult(false, "commands.cresora.story.invalid", listOf(chapter.id))
        }
        val result = startSessionInternal(player, domain, chapter.id)
        return StoryStartResult(result.success, result.translationKey, result.args)
    }

    private fun startSessionInternal(
        player: ServerPlayerEntity,
        domain: DomainDefinition,
        linkedStoryChapterId: String?
    ): DomainStartResult {
        if (sessionsByPlayer.containsKey(player.uuid)) {
            return DomainStartResult(false, if (linkedStoryChapterId == null) "screen.cresora.domain.already_active" else "commands.cresora.story.already_active")
        }
        if (StoryService.hasActiveSession(player)) {
            return DomainStartResult(false, if (linkedStoryChapterId == null) "screen.cresora.domain.blocked_story" else "commands.cresora.story.already_active")
        }
        if (MasqueradeService.hasActiveSession(player)) {
            return DomainStartResult(false, if (linkedStoryChapterId == null) "screen.cresora.domain.blocked_masquerade" else "commands.cresora.story.blocked_masquerade")
        }
        val rank = AdventureRankService.getRank(player)
        if (linkedStoryChapterId == null && rank < domain.unlockRank) {
            return DomainStartResult(false, if (linkedStoryChapterId == null) "screen.cresora.domain.locked" else "commands.cresora.story.locked", listOf(domain.unlockRank))
        }
        if (!InventoryGate.hasFreeMainSlot(player)) {
            return DomainStartResult(false, if (linkedStoryChapterId == null) "screen.cresora.domain.inventory_full" else "commands.cresora.story.inventory_full", listOf(1))
        }
        if (!CreditsService.hasCredits(player, domain.entryCostCsc)) {
            return DomainStartResult(false, "screen.cresora.domain.not_enough_credits", listOf(ArtifactSpecialItem.formatWholeNumber(domain.entryCostCsc)))
        }

        val server = player.server ?: return DomainStartResult(false, if (linkedStoryChapterId == null) "screen.cresora.domain.invalid" else "commands.cresora.story.invalid")
        val arenaWorld = ArenaManager.getDomainWorld(server, domain.themeId) ?: return DomainStartResult(false, "screen.cresora.domain.no_world")

        if (!CreditsService.spendCredits(player, domain.entryCostCsc)) {
            return DomainStartResult(false, "screen.cresora.domain.not_enough_credits", listOf(ArtifactSpecialItem.formatWholeNumber(domain.entryCostCsc)))
        }

        val arenaCenter = ArenaManager.getArenaPosForPlayer(player.uuid, domain.themeId)
        ArenaManager.ensureArena(arenaWorld, arenaCenter, domain.themeId)

        val session = DomainSession(
            id = UUID.randomUUID(),
            playerUuid = player.uuid,
            domainId = domain.id,
            arenaCenter = arenaCenter,
            returnPoint = DomainReturnPoint(player.world.registryKey, player.pos, player.yaw, player.pitch),
            sessionRank = DomainCombatProfile.sessionRank(rank),
            linkedStoryChapterId = linkedStoryChapterId
        )
        session.nextSpawnTick = arenaWorld.time + START_DELAY_TICKS
        sessionsByPlayer[player.uuid] = session
        sessionsById[session.id] = session

        player.health = player.maxHealth
        teleportPlayer(player, arenaWorld, Vec3d(arenaCenter.x + 0.5, arenaCenter.y + 1.0, arenaCenter.z + 0.5), 180.0f, 0.0f)
        if (linkedStoryChapterId == null) {
            player.sendMessage(Text.translatable("screen.cresora.domain.entered", Text.translatable(domain.nameKey), session.sessionRank), false)
            return DomainStartResult(true, "screen.cresora.domain.entered", listOf(Text.translatable(domain.nameKey), session.sessionRank))
        }
        val chapter = StoryContentRegistry.requireChapter(linkedStoryChapterId)
        val chapterLabel = StoryTextRegistry.chapterLabel(StoryTextRegistry.resolvePlayerLocale(player), chapter)
        player.sendMessage(Text.translatable("commands.cresora.story.started", chapterLabel), false)
        return DomainStartResult(true, "commands.cresora.story.started", listOf(chapterLabel))
    }

    fun tick(server: MinecraftServer) {
        val iterator = sessionsById.values.toList()
        for (session in iterator) {
            tickSession(server, session)
        }
    }

    fun onPlayerDeath(player: ServerPlayerEntity) {
        val session = sessionsByPlayer[player.uuid] ?: return
        val server = player.server ?: return
        failSession(server, session, player, "screen.cresora.domain.failed_death", restorePlayer = false)
    }

    fun onPlayerDisconnect(player: ServerPlayerEntity) {
        val session = sessionsByPlayer[player.uuid] ?: return
        val server = player.server ?: return
        failSession(server, session, null, "screen.cresora.domain.failed_leave", restorePlayer = false)
    }

    fun damageMultiplier(attacker: Entity?): Double {
        val runtime = attacker?.uuid?.let(mobRuntime::get) ?: return 1.0
        return runtime.damageMultiplier
    }

    fun isDomainMob(entity: MobEntity): Boolean = mobRuntime.containsKey(entity.uuid)

    private fun tickSession(server: MinecraftServer, session: DomainSession) {
        val player = server.playerManager.getPlayer(session.playerUuid)
        if (player == null) {
            failSession(server, session, null, "screen.cresora.domain.failed_leave", restorePlayer = false)
            return
        }
        
        val themeId = session.definition().themeId
        val world = ArenaManager.getDomainWorld(server, themeId) ?: return
        if (player.world.registryKey != world.registryKey || player.squaredDistanceTo(session.arenaCenter.toCenterPos()) > ARENA_FAIL_DISTANCE_SQUARED) {
            failSession(server, session, player, "screen.cresora.domain.failed_leave", restorePlayer = true)
            return
        }

        cleanupInactiveMobs(world, session)

        if (session.preparingNextWave) {
            if (world.time >= session.nextSpawnTick) {
                val pendingWave = session.pendingWave()
                if (pendingWave == null) {
                    completeSession(server, session, player)
                } else {
                    spawnPendingWave(world, session, player, pendingWave)
                }
            }
            return
        }

        if (session.activeMobUuids.isEmpty()) {
            val pendingWave = session.pendingWave()
            if (pendingWave == null) {
                completeSession(server, session, player)
            } else {
                session.preparingNextWave = true
                session.nextSpawnTick = world.time + pendingWave.spawnDelayTicks.toLong().coerceAtLeast(20L)
            }
        }
    }

    private fun spawnPendingWave(
        world: ServerWorld,
        session: DomainSession,
        player: ServerPlayerEntity,
        wave: DomainWaveDefinition
    ) {
        val domain = session.definition()
        val spawnRank = AdventureRankProgression.sanitizeRank(session.sessionRank + wave.levelOffset)
        val scaling = DomainCombatProfile.scaling(spawnRank, wave.elite)
        val pool = DomainContentRegistry.requireMobPool(domain.mobPoolId)
        val totalWaveIndex = session.clearedWaveCount() + 1

        repeat(wave.count.coerceAtLeast(1)) { index ->
            val entityType = rollEntityType(pool, world.random) ?: return@repeat
            val spawnPos = BlockPos.ofFloored(
                session.arenaCenter.x + 0.5 + randomOffset(world.random, index),
                session.arenaCenter.y + 1.0,
                session.arenaCenter.z + 0.5 + randomOffset(world.random, index + 13)
            )
            val hostile = entityType.spawn(world, null, spawnPos, SpawnReason.EVENT, true, false) as? MobEntity ?: return@repeat
            val access = hostile as? AdventureRankMobAccess ?: return@repeat
            access.cresoraSetMobAdventureRank(spawnRank)
            FieldMobPackService.markExplicit(hostile, wave.elite)
            AdventureRankService.applyMobScaling(hostile, spawnRank, scaling.healthScalar, scaling.defenseScalar, scaling.toughnessScalar)
            hostile.target = player
            if (wave.elite) {
                hostile.setPersistent()
            }
            mobRuntime[hostile.uuid] = DomainRuntimeMob(session.id, scaling.damageScalar)
            session.activeMobUuids += hostile.uuid
        }

        session.consumePendingWave()
        session.preparingNextWave = false
        player.sendMessage(
            Text.translatable("screen.cresora.domain.wave", Text.translatable(domain.nameKey), totalWaveIndex, domain.totalWaveCount(), spawnRank),
            false
        )
    }

    private fun completeSession(server: MinecraftServer, session: DomainSession, player: ServerPlayerEntity) {
        val result = generateRewards(player, session)
        cleanupSession(server, session)
        restorePlayerPosition(server, player, session.returnPoint)
        val droppedItemCount = deliverRewardItems(player, result.items)
        if (result.credits > 0) {
            CreditsService.addCredits(player, result.credits)
        }
        if (result.rankXp > 0) {
            AdventureRankService.addXp(player, result.rankXp)
        }
        if (result.chordProgression > 0) {
            ResonanceService.addCurrency(player, ResonanceCurrencyType.CHORD_PROGRESSION, result.chordProgression)
        }
        if (result.substituteChord > 0) {
            ResonanceService.addCurrency(player, ResonanceCurrencyType.SUBSTITUTE_CHORD, result.substituteChord)
        }
        val linkedStoryChapterId = session.linkedStoryChapterId
        if (linkedStoryChapterId == null) {
            player.sendMessage(Text.translatable("screen.cresora.domain.cleared", Text.translatable(session.definition().nameKey)), false)
        } else {
            StoryProgressService.markCleared(player, linkedStoryChapterId)
            val chapter = StoryContentRegistry.requireChapter(linkedStoryChapterId)
            val chapterLabel = StoryTextRegistry.chapterLabel(StoryTextRegistry.resolvePlayerLocale(player), chapter)
            player.sendMessage(Text.translatable("commands.cresora.story.cleared", chapterLabel), false)
        }
        if (droppedItemCount > 0) {
            player.sendMessage(Text.translatable("screen.cresora.domain.reward_overflow", ArtifactSpecialItem.formatWholeNumber(droppedItemCount)), false)
        }
        ArtifactUiFlow.openDomainReward(player, DomainDisplayStackFactory.rewardDisplayStacks(result))
    }

    private fun failSession(
        server: MinecraftServer,
        session: DomainSession,
        player: ServerPlayerEntity?,
        messageKey: String,
        restorePlayer: Boolean
    ) {
        cleanupSession(server, session)
        if (player != null && restorePlayer && player.isAlive) {
            restorePlayerPosition(server, player, session.returnPoint)
        }
        if (player == null) {
            return
        }
        val linkedStoryChapterId = session.linkedStoryChapterId
        if (linkedStoryChapterId == null) {
            player.sendMessage(Text.translatable(messageKey, Text.translatable(session.definition().nameKey)), false)
            return
        }
        val chapter = StoryContentRegistry.requireChapter(linkedStoryChapterId)
        val chapterLabel = StoryTextRegistry.chapterLabel(StoryTextRegistry.resolvePlayerLocale(player), chapter)
        player.sendMessage(Text.translatable("commands.cresora.story.failed", chapterLabel), false)
    }

    private fun cleanupSession(server: MinecraftServer, session: DomainSession) {
        val world = ArenaManager.getDomainWorld(server) ?: return
        for (mobUuid in session.activeMobUuids) {
            world.getEntity(mobUuid)?.discard()
            mobRuntime.remove(mobUuid)
        }
        session.activeMobUuids.clear()
        sessionsByPlayer.remove(session.playerUuid)
        sessionsById.remove(session.id)
    }

    private fun cleanupInactiveMobs(world: ServerWorld, session: DomainSession) {
        val iterator = session.activeMobUuids.iterator()
        while (iterator.hasNext()) {
            val mobUuid = iterator.next()
            val entity = world.getEntity(mobUuid) as? MobEntity
            if (entity == null || entity.isRemoved || !entity.isAlive) {
                mobRuntime.remove(mobUuid)
                iterator.remove()
            }
        }
    }

    private fun generateRewards(player: ServerPlayerEntity, session: DomainSession): DomainRewardResult {
        return generateRewardsForProfile(player, session.definition().rewardProfileId, session.sessionRank)
    }

    private fun generateRewardsForProfile(
        player: ServerPlayerEntity,
        rewardProfileId: String,
        sessionRank: Int
    ): DomainRewardResult {
        val profile = DomainRewardProfileRegistry.requireProfile(rewardProfileId)
        val random = player.random
        val items = mutableListOf<ItemStack>()

        profile.artifactReward?.let { artifact ->
            val candidateDefinitions = EquipmentContentRegistry.equipmentDefinitions().filter { it.setId == artifact.setId }
            if (candidateDefinitions.isNotEmpty()) {
                val count = randomCount(random, artifact.minCount, artifact.maxCount)
                repeat(count) {
                    val definition = candidateDefinitions[random.nextInt(candidateDefinitions.size)]
                    val item = EquipmentStackSupport.itemForDefinitionId(definition.id) ?: return@repeat
                    val rarity = artifact.rarityWeights.roll(random)
                    val level = randomCount(random, artifact.minLevel, minOf(artifact.maxLevel, rarity.maxLevel))
                    val stack = ItemStack(item)
                    val data = EquipmentGenerationService.createEquipment(
                        random = random,
                        definition = definition,
                        startingLevel = level,
                        forcedRarity = rarity
                    )
                    EquipmentStackSupport.syncEquipmentData(stack, data)
                    items += stack
                }
            }
        }

        profile.weaponFragmentReward?.let { weapon ->
            val definition = WeaponContentRegistry.requireWeapon(weapon.weaponId)
            val item = WeaponStackSupport.fragmentItem(definition.id)
            if (item != null) {
                val count = randomCount(random, weapon.minCount, weapon.maxCount)
                if (count > 0) {
                    items += ItemStack(item, count)
                }
            }
        }

        profile.proofReward?.let { proof ->
            val count = randomCount(random, proof.minCount, proof.maxCount)
            if (count > 0) {
                val role = proof.role ?: WeaponRole.entries[random.nextInt(WeaponRole.entries.size)]
                val item = CreSoraUtilities.getRoleProofItem(role)
                items += ItemStack(item, count)
            }
        }

        profile.insightReward?.let { insight ->
            val count = randomCount(random, insight.minCount, insight.maxCount)
            if (count > 0) {
                val role = insight.role ?: WeaponRole.entries[random.nextInt(WeaponRole.entries.size)]
                val item = CreSoraUtilities.getRoleInsightItem(role)
                items += ItemStack(item, count)
            }
        }

        val credits = (profile.currencyReward.creditsBase + profile.currencyReward.creditsPerRank * sessionRank)
            .coerceAtLeast(0)
        val rankXp = (profile.currencyReward.rankXpBase + profile.currencyReward.rankXpPerRank * sessionRank)
            .coerceAtLeast(0)
        return DomainRewardResult(items, credits, rankXp, profile.chordProgressionReward, profile.substituteChordReward)
    }

    private fun deliverRewardItems(player: ServerPlayerEntity, rewards: List<ItemStack>): Int {
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

    private fun restorePlayerPosition(server: MinecraftServer, player: ServerPlayerEntity, returnPoint: DomainReturnPoint) {
        val world = server.getWorld(returnPoint.worldKey) ?: server.overworld
        teleportPlayer(player, world, returnPoint.position, returnPoint.yaw, returnPoint.pitch)
    }

    private fun teleportPlayer(player: ServerPlayerEntity, world: ServerWorld, position: Vec3d, yaw: Float, pitch: Float) {
        player.teleport(world, position.x, position.y, position.z, setOf(), yaw, pitch, false)
    }

    private fun randomOffset(random: net.minecraft.util.math.random.Random, salt: Int): Double {
        val direction = if ((salt + random.nextInt(1000)) % 2 == 0) 1.0 else -1.0
        return direction * (2.0 + random.nextDouble() * 3.2)
    }

    private fun rollEntityType(pool: DomainMobPool, random: net.minecraft.util.math.random.Random): EntityType<*>? {
        val totalWeight = pool.entries.sumOf { it.weight.coerceAtLeast(0) }
        if (totalWeight <= 0) {
            return null
        }
        var roll = random.nextInt(totalWeight)
        for (entry in pool.entries) {
            roll -= entry.weight.coerceAtLeast(0)
            if (roll < 0) {
                return Registries.ENTITY_TYPE.get(Identifier.of(entry.entityTypeId))
            }
        }
        return Registries.ENTITY_TYPE.get(Identifier.of(pool.entries.last().entityTypeId))
    }

    private fun randomCount(random: net.minecraft.util.math.random.Random, min: Int, max: Int): Int {
        if (max <= min) {
            return min.coerceAtLeast(0)
        }
        return random.nextBetween(min, max).coerceAtLeast(0)
    }
}
