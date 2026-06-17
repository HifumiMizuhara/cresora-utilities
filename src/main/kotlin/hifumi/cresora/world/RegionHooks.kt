package hifumi.cresora.world

import hifumi.cresora.story.StoryFlagService
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Polls player positions inside cresora_world once per second. When a player crosses into a new
 * region (or enters one for the first time), the region is recorded as a `region_visited_<id>`
 * story flag (persisted via [StoryFlagService]) and the region name is shown on the action bar.
 *
 * Outside cresora_world the tracker is cleared so re-entry re-fires the announcement.
 */
object RegionHooks {
    private const val VISITED_FLAG_PREFIX = "region_visited_"
    private val currentRegion: MutableMap<UUID, String?> = ConcurrentHashMap()

    fun init() {
        ServerTickEvents.END_SERVER_TICK.register { server ->
            if (server.overworld.time % 20L != 0L) return@register
            for (player in server.playerManager.playerList) {
                tickPlayer(player)
            }
        }
    }

    private fun tickPlayer(player: ServerPlayerEntity) {
        if (!CresoraWorldKeys.isCresoraWorld(player.world.registryKey)) {
            currentRegion.remove(player.uuid)
            return
        }
        val region = RegionContentRegistry.regionAt(player.blockX, player.blockZ)
        val regionId = region?.id
        if (currentRegion[player.uuid] == regionId) return

        currentRegion[player.uuid] = regionId
        if (region == null) return

        val firstVisit = !StoryFlagService.hasFlag(player, VISITED_FLAG_PREFIX + region.id)
        if (firstVisit) {
            StoryFlagService.setFlag(player, VISITED_FLAG_PREFIX + region.id)
            player.sendMessage(
                Text.literal("◆ ").formatted(Formatting.GOLD)
                    .append(Text.translatable(region.nameKey).formatted(Formatting.WHITE))
                    .append(Text.literal(" ◆").formatted(Formatting.GOLD)),
                true
            )
        }
    }
}
