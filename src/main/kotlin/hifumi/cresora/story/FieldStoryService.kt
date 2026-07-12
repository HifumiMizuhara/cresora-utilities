package hifumi.cresora.story

import hifumi.cresora.adventurerank.AdventureRankMobAccess
import hifumi.cresora.adventurerank.AdventureRankService
import hifumi.cresora.combat.FieldMobPackService
import hifumi.cresora.credits.CreditsService
import hifumi.cresora.domain.DomainRewardResult
import hifumi.cresora.domain.DomainService
import hifumi.cresora.masquerade.MasqueradeService
import hifumi.cresora.resonance.ResonanceService
import hifumi.cresora.world.CresoraWorldKeys
import hifumi.cresora.world.SpiritGuideService
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents
import net.minecraft.entity.SpawnReason
import net.minecraft.entity.mob.MobEntity
import net.minecraft.registry.Registries
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.server.world.ServerWorld
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d
import java.util.UUID

/**
 * World-space story sessions.  These retain the chapter reward/progress model while
 * letting a chapter begin through dialogue and resolve at a location in Cresora World.
 */
object FieldStoryService {
    private const val OPENING_CHAPTER_ID = "0-0"
    private const val START_RADIUS_SQUARED = 5.0 * 5.0

    private data class Session(
        val playerUuid: UUID,
        val chapterId: String,
        val encounterPos: BlockPos,
        val activeMobs: MutableSet<UUID> = linkedSetOf(),
        var battleStarted: Boolean = false
    )

    private val sessions = mutableMapOf<UUID, Session>()

    fun isFieldChapter(chapterId: String): Boolean = chapterId == OPENING_CHAPTER_ID

    fun init() {
        ServerTickEvents.END_SERVER_TICK.register(::tick)
        ServerLivingEntityEvents.AFTER_DEATH.register(ServerLivingEntityEvents.AfterDeath { entity, _ ->
            (entity as? ServerPlayerEntity)?.let(::abort)
        })
        ServerPlayConnectionEvents.DISCONNECT.register(ServerPlayConnectionEvents.Disconnect { handler, _ ->
            abort(handler.player)
        })
    }

    fun hasActiveSession(player: ServerPlayerEntity): Boolean = sessions.containsKey(player.uuid)

    fun startFromDialogue(player: ServerPlayerEntity, chapterId: String): Boolean {
        if (chapterId != OPENING_CHAPTER_ID || StoryProgressService.isCleared(player, chapterId)) return false
        if (hasActiveSession(player) || StoryService.hasActiveSession(player) || DomainService.hasActiveSession(player) || MasqueradeService.hasActiveSession(player)) return false
        if (!CresoraWorldKeys.isCresoraWorld(player.world.registryKey)) return false

        val chapter = StoryContentRegistry.requireChapter(chapterId)
        if (AdventureRankService.getRank(player) < chapter.unlockRank) return false
        if (StoryProgressService.missingPrerequisite(player, chapter) != null) return false

        sessions[player.uuid] = Session(player.uuid, chapterId, SpiritGuideService.OPENING_ENCOUNTER_POS)
        StoryFlagService.setFlag(player, "field_story_0_0_active")
        player.sendMessage(Text.translatable("message.cresora.field_story.0_0.started").formatted(Formatting.AQUA), false)
        player.sendMessage(Text.translatable("message.cresora.field_story.0_0.objective").formatted(Formatting.GRAY), true)
        return true
    }

    private fun tick(server: net.minecraft.server.MinecraftServer) {
        for (session in sessions.values.toList()) {
            val player = server.playerManager.getPlayer(session.playerUuid) ?: run {
                sessions.remove(session.playerUuid)
                continue
            }
            val world = server.getWorld(CresoraWorldKeys.CRESORA_WORLD) ?: continue
            if (player.world.registryKey != CresoraWorldKeys.CRESORA_WORLD) continue

            if (!session.battleStarted) {
                if (player.squaredDistanceTo(session.encounterPos.toCenterPos()) <= START_RADIUS_SQUARED) {
                    startBattle(player, world, session)
                }
                continue
            }

            session.activeMobs.removeIf { mobId ->
                val mob = world.getEntity(mobId) as? MobEntity
                mob == null || !mob.isAlive || mob.isRemoved
            }
            if (session.activeMobs.isEmpty()) {
                complete(player, session)
            }
        }
    }

    private fun startBattle(player: ServerPlayerEntity, world: ServerWorld, session: Session) {
        val chapter = StoryContentRegistry.requireChapter(session.chapterId)
        val wave = chapter.battle.firstOrNull() ?: run {
            complete(player, session)
            return
        }
        session.battleStarted = true
        for ((spawnIndex, spawn) in wave.spawns.withIndex()) {
            val entityType = Registries.ENTITY_TYPE.get(Identifier.of(spawn.entityTypeId))
            repeat(spawn.count) { offset ->
                val pos = session.encounterPos.add((spawnIndex + offset) % 3 - 1, 1, 2 + (spawnIndex * 2 + offset) % 3)
                val mob = entityType.spawn(world, null, pos, SpawnReason.EVENT, true, false) as? MobEntity ?: return@repeat
                (mob as? AdventureRankMobAccess)?.let {
                    it.cresoraSetMobAdventureRank(wave.enemyRank)
                    AdventureRankService.applyMobScaling(mob, wave.enemyRank)
                }
                mob.target = player
                FieldMobPackService.markExplicit(mob, false)
                session.activeMobs += mob.uuid
            }
        }
        player.sendMessage(Text.translatable("message.cresora.field_story.0_0.encounter").formatted(Formatting.GOLD), false)
    }

    private fun complete(player: ServerPlayerEntity, session: Session) {
        val chapter = StoryContentRegistry.requireChapter(session.chapterId)
        sessions.remove(player.uuid)
        StoryFlagService.unsetFlag(player, "field_story_0_0_active")
        StoryFlagService.setFlag(player, "chapter_0_0_completed")
        val firstClear = StoryProgressService.markCleared(player, chapter.id)
        if (firstClear) {
            CreditsService.addCredits(player, chapter.rewards.credits)
            chapter.rewards.resonanceCurrencies.forEach { reward ->
                if (reward.amount > 0) ResonanceService.addCurrency(player, reward.type, reward.amount)
            }
        }
        val domainRewards = chapter.domainRewardIds
            .map { DomainService.generateOneOffRewards(player, it, AdventureRankService.getRank(player)) }
            .fold(DomainRewardResult(emptyList(), 0, 0)) { left, right ->
                DomainRewardResult(left.items + right.items, left.credits + right.credits, left.rankXp + right.rankXp)
            }
        if (domainRewards.credits > 0) CreditsService.addCredits(player, domainRewards.credits)
        if (domainRewards.rankXp > 0) AdventureRankService.addXp(player, domainRewards.rankXp)
        domainRewards.items.forEach { reward ->
            val remainder = reward.copy()
            player.inventory.insertStack(remainder)
            if (!remainder.isEmpty) player.dropItem(remainder, false)
        }
        player.inventory.markDirty()
        player.playerScreenHandler.sendContentUpdates()
        StoryDialogueNetworking.showSimpleDialogue(
            player,
            StoryDisplayText.chapterTitle(player, chapter),
            chapter.postBattleStory.map { StoryDisplayText.resolveLine(player, it) }
        )
        player.sendMessage(Text.translatable("message.cresora.field_story.0_0.complete").formatted(Formatting.GREEN), false)
    }

    private fun abort(player: ServerPlayerEntity) {
        val session = sessions.remove(player.uuid) ?: return
        player.server?.getWorld(CresoraWorldKeys.CRESORA_WORLD)?.let { world ->
            session.activeMobs.forEach { mobId -> world.getEntity(mobId)?.discard() }
        }
        StoryFlagService.unsetFlag(player, "field_story_0_0_active")
    }
}
