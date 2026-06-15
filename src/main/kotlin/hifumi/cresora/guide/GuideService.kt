package hifumi.cresora.guide

import hifumi.cresora.adventurerank.AdventureRankService
import hifumi.cresora.credits.CreditsService
import hifumi.cresora.resonance.ResonanceCurrencyType
import hifumi.cresora.resonance.ResonanceService
import hifumi.cresora.story.StoryProgressService
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text

object GuideService {
    fun getPlayerChapter(player: ServerPlayerEntity): Int {
        val access = player as? GuideProgressAccess ?: return 1
        return access.cresoraGetGuideCurrentChapter().coerceAtLeast(1)
    }

    fun getClaimedTasks(player: ServerPlayerEntity): Set<String> {
        val access = player as? GuideProgressAccess ?: return emptySet()
        return access.cresoraGetGuideClaimedTasks()
            .split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toSet()
    }

    private fun setClaimedTasks(player: ServerPlayerEntity, claimed: Set<String>) {
        val access = player as? GuideProgressAccess ?: return
        access.cresoraSetGuideClaimedTasks(claimed.joinToString(","))
    }

    fun getClaimedChapters(player: ServerPlayerEntity): Set<Int> {
        val access = player as? GuideProgressAccess ?: return emptySet()
        return access.cresoraGetGuideClaimedChapters()
            .split(",")
            .map { it.trim() }
            .mapNotNull { it.toIntOrNull() }
            .toSet()
    }

    private fun setClaimedChapters(player: ServerPlayerEntity, claimed: Set<Int>) {
        val access = player as? GuideProgressAccess ?: return
        access.cresoraSetGuideClaimedChapters(claimed.joinToString(","))
    }

    fun getProgressMap(player: ServerPlayerEntity): Map<String, Int> {
        val access = player as? GuideProgressAccess ?: return emptyMap()
        val raw = access.cresoraGetGuideProgressMap()
        val map = mutableMapOf<String, Int>()
        if (raw.isBlank()) return map
        for (entry in raw.split(";")) {
            val parts = entry.split(":")
            if (parts.size == 2) {
                val k = parts[0]
                val v = parts[1].toIntOrNull() ?: 0
                map[k] = v
            }
        }
        return map
    }

    private fun setProgressMap(player: ServerPlayerEntity, map: Map<String, Int>) {
        val access = player as? GuideProgressAccess ?: return
        val raw = map.entries.joinToString(";") { "${it.key}:${it.value}" }
        access.cresoraSetGuideProgressMap(raw)
    }

    fun getTaskProgress(player: ServerPlayerEntity, task: GuideTask): Int {
        return when (task.type) {
            GuideTaskType.OBTAIN_CSC -> CreditsService.getCredits(player)
            GuideTaskType.REACH_RANK -> AdventureRankService.getRank(player)
            GuideTaskType.CLEAR_STORY_STAGE -> {
                if (StoryProgressService.isCleared(player, task.detailParam)) 1 else 0
            }
            else -> {
                getProgressMap(player)[task.id] ?: 0
            }
        }
    }

    private fun incrementEventProgress(player: ServerPlayerEntity, type: GuideTaskType, amount: Int, maxMode: Boolean = false) {
        val currentChapterIndex = getPlayerChapter(player)
        val chapter = GuideContentRegistry.getChapter(currentChapterIndex) ?: return
        val map = getProgressMap(player).toMutableMap()
        var updated = false

        for (task in chapter.tasks) {
            if (task.type == type) {
                val currentVal = map[task.id] ?: 0
                if (maxMode) {
                    map[task.id] = maxOf(currentVal, amount)
                } else {
                    map[task.id] = currentVal + amount
                }
                updated = true
            }
        }

        if (updated) {
            setProgressMap(player, map)
        }
    }

    fun onKillHostile(player: ServerPlayerEntity) {
        incrementEventProgress(player, GuideTaskType.KILL_HOSTILE, 1)
    }

    fun onResonancePull(player: ServerPlayerEntity, count: Int) {
        incrementEventProgress(player, GuideTaskType.RESONANCE_PULL, count)
    }

    fun onWeaponUpgrade(player: ServerPlayerEntity, newLevel: Int) {
        // We support both count-based (upgrade once) and level-based (upgrade to level 10)
        // If target > 1, we assume it wants the target level (maxMode = true)
        // If target == 1, it's upgrade once (maxMode = false, increment by 1)
        val currentChapterIndex = getPlayerChapter(player)
        val chapter = GuideContentRegistry.getChapter(currentChapterIndex) ?: return
        val map = getProgressMap(player).toMutableMap()
        var updated = false

        for (task in chapter.tasks) {
            if (task.type == GuideTaskType.UPGRADE_WEAPON) {
                if (task.targetValue > 1) {
                    val currentVal = map[task.id] ?: 0
                    map[task.id] = maxOf(currentVal, newLevel)
                    updated = true
                } else {
                    val currentVal = map[task.id] ?: 0
                    map[task.id] = currentVal + 1
                    updated = true
                }
            }
        }

        if (updated) {
            setProgressMap(player, map)
        }
    }

    fun onStoryStageClear(player: ServerPlayerEntity, stageId: String) {
        // Since CLEAR_STORY_STAGE is state-based (we directly check StoryProgressService.isCleared),
        // we don't need to save anything to the progress map, but we could trigger updates if screens are open.
    }

    fun claimTaskReward(player: ServerPlayerEntity, taskId: String): Boolean {
        val task = GuideContentRegistry.getTask(taskId) ?: return false
        val claimed = getClaimedTasks(player).toMutableSet()
        if (claimed.contains(taskId)) return false

        val progress = getTaskProgress(player, task)
        if (progress < task.targetValue) return false

        claimed.add(taskId)
        setClaimedTasks(player, claimed)

        if (task.cscReward > 0) {
            CreditsService.addCredits(player, task.cscReward)
        }
        if (task.chordReward > 0) {
            ResonanceService.addCurrency(player, ResonanceCurrencyType.CHORD_PROGRESSION, task.chordReward)
        }

        player.sendMessage(
            Text.translatable("guide.cresora.claim_success", Text.translatable(task.translationKey)),
            false
        )
        return true
    }

    fun claimChapterReward(player: ServerPlayerEntity, chapterIndex: Int): Boolean {
        val currentChapter = getPlayerChapter(player)
        if (chapterIndex != currentChapter) return false

        val chapter = GuideContentRegistry.getChapter(chapterIndex) ?: return false

        val claimedTasks = getClaimedTasks(player)
        for (task in chapter.tasks) {
            if (!claimedTasks.contains(task.id)) {
                // Must claim all individual tasks first
                return false
            }
        }

        val claimedChapters = getClaimedChapters(player).toMutableSet()
        if (claimedChapters.contains(chapterIndex)) return false

        claimedChapters.add(chapterIndex)
        setClaimedChapters(player, claimedChapters)

        if (chapter.cscReward > 0) {
            CreditsService.addCredits(player, chapter.cscReward)
        }
        if (chapter.chordReward > 0) {
            ResonanceService.addCurrency(player, ResonanceCurrencyType.CHORD_PROGRESSION, chapter.chordReward)
        }

        // Advance to next chapter
        val nextChapter = chapterIndex + 1
        val access = player as? GuideProgressAccess
        if (access != null) {
            access.cresoraSetGuideCurrentChapter(nextChapter)
        }

        player.sendMessage(
            Text.translatable("guide.cresora.chapter_complete", chapterIndex),
            false
        )
        return true
    }

    fun copyTo(oldPlayer: ServerPlayerEntity, newPlayer: ServerPlayerEntity) {
        val oldAccess = oldPlayer as? GuideProgressAccess ?: return
        val newAccess = newPlayer as? GuideProgressAccess ?: return
        newAccess.cresoraSetGuideCurrentChapter(oldAccess.cresoraGetGuideCurrentChapter())
        newAccess.cresoraSetGuideClaimedTasks(oldAccess.cresoraGetGuideClaimedTasks())
        newAccess.cresoraSetGuideClaimedChapters(oldAccess.cresoraGetGuideClaimedChapters())
        newAccess.cresoraSetGuideProgressMap(oldAccess.cresoraGetGuideProgressMap())
    }
}
