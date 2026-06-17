package hifumi.cresora.story
import net.minecraft.server.network.ServerPlayerEntity

object StoryProgressService {
    private const val PLAYER_STORY_CLEARS_KEY = "cresora_story_clears"
    private const val DELIMITER = ";"

    fun playerStoryClearsKey(): String = PLAYER_STORY_CLEARS_KEY

    fun clearedChapterIds(player: ServerPlayerEntity): Set<String> {
        val access = player as? StoryProgressAccess ?: return emptySet()
        return access.cresoraGetStoryClearsRaw()
            .split(DELIMITER)
            .map(String::trim)
            .filter(String::isNotEmpty)
            .toSortedSet()
    }

    fun isCleared(player: ServerPlayerEntity, chapterId: String): Boolean {
        return clearedChapterIds(player).contains(chapterId)
    }

    fun markCleared(player: ServerPlayerEntity, chapterId: String): Boolean {
        val access = player as? StoryProgressAccess ?: return false
        val alreadyCleared = isCleared(player, chapterId)
        if (alreadyCleared) {
            return false
        }
        val updated = clearedChapterIds(player).toMutableSet()
        updated += chapterId
        access.cresoraSetStoryClearsRaw(updated.sorted().joinToString(DELIMITER))
        hifumi.cresora.guide.GuideService.onStoryStageClear(player, chapterId)
        return true
    }

    fun copyTo(oldPlayer: ServerPlayerEntity, newPlayer: ServerPlayerEntity) {
        val oldAccess = oldPlayer as? StoryProgressAccess ?: return
        val newAccess = newPlayer as? StoryProgressAccess ?: return
        newAccess.cresoraSetStoryClearsRaw(oldAccess.cresoraGetStoryClearsRaw())
        StoryFlagService.copyTo(oldPlayer, newPlayer)
    }

    fun missingPrerequisite(player: ServerPlayerEntity, chapter: StoryChapterDefinition): String? {
        val prerequisite = chapter.prerequisiteChapterId?.takeUnless(String::isBlank) ?: return null
        return if (isCleared(player, prerequisite)) null else prerequisite
    }
}
