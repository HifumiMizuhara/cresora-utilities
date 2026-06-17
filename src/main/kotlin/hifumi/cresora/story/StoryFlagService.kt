package hifumi.cresora.story

import net.minecraft.server.network.ServerPlayerEntity

object StoryFlagService {
    private const val PLAYER_STORY_FLAGS_KEY = "cresora_story_flags"
    private const val DELIMITER = ","

    fun playerStoryFlagsKey(): String = PLAYER_STORY_FLAGS_KEY

    fun getFlags(player: ServerPlayerEntity): Set<String> {
        val access = player as? StoryFlagAccess ?: return emptySet()
        return access.cresoraGetStoryFlagsRaw()
            .split(DELIMITER)
            .map(String::trim)
            .filter(String::isNotEmpty)
            .toSortedSet()
    }

    fun hasFlag(player: ServerPlayerEntity, flagKey: String): Boolean {
        return getFlags(player).contains(flagKey)
    }

    fun setFlag(player: ServerPlayerEntity, flagKey: String) {
        val access = player as? StoryFlagAccess ?: return
        val updated = getFlags(player).toMutableSet()
        updated += flagKey
        access.cresoraSetStoryFlagsRaw(updated.sorted().joinToString(DELIMITER))
    }

    fun unsetFlag(player: ServerPlayerEntity, flagKey: String) {
        val access = player as? StoryFlagAccess ?: return
        val updated = getFlags(player).toMutableSet()
        updated -= flagKey
        access.cresoraSetStoryFlagsRaw(updated.sorted().joinToString(DELIMITER))
    }

    fun checkFlags(player: ServerPlayerEntity, required: Map<String, Boolean>): Boolean {
        return required.all { (key, mustHave) ->
            hasFlag(player, key) == mustHave
        }
    }

    fun copyTo(oldPlayer: ServerPlayerEntity, newPlayer: ServerPlayerEntity) {
        val oldAccess = oldPlayer as? StoryFlagAccess ?: return
        val newAccess = newPlayer as? StoryFlagAccess ?: return
        newAccess.cresoraSetStoryFlagsRaw(oldAccess.cresoraGetStoryFlagsRaw())
    }
}
