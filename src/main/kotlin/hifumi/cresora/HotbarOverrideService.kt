package hifumi.cresora

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.minecraft.item.ItemStack
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import java.util.*

object HotbarOverrideService {
    private data class OverrideSession(
        val originalHotbar: Array<ItemStack>,
        val expireTick: Long,
        val weaponId: String
    )

    private val sessions = mutableMapOf<UUID, OverrideSession>()

    fun init() {
        ServerTickEvents.END_SERVER_TICK.register { server ->
            val now = server.overworld.time
            val toRestore = mutableListOf<UUID>()
            
            for ((uuid, session) in sessions) {
                if (now >= session.expireTick) {
                    toRestore.add(uuid)
                }
            }

            for (uuid in toRestore) {
                server.playerManager.getPlayer(uuid)?.let { restoreHotbar(it) }
            }
        }
    }

    fun overrideHotbar(player: ServerPlayerEntity, weaponId: String, subSkillEffectIds: List<String>, durationTicks: Long) {
        if (sessions.containsKey(player.uuid)) {
            restoreHotbar(player)
        }

        val hotbar = Array(9) { player.inventory.getStack(it).copy() }
        val now = player.server?.overworld?.time ?: 0L
        
        sessions[player.uuid] = OverrideSession(hotbar, now + durationTicks, weaponId)

        // Clear hotbar and set sub-skill items
        for (i in 0 until 9) {
            if (i < subSkillEffectIds.size) {
                val effectId = subSkillEffectIds[i]
                val dummyStack = createSubSkillStack(effectId)
                player.inventory.setStack(i, dummyStack)
            } else {
                player.inventory.setStack(i, ItemStack.EMPTY)
            }
        }
        
        player.inventory.selectedSlot = 0
        player.sendMessage(Text.translatable("message.cresora.hotbar_overridden").formatted(Formatting.GOLD), true)
    }

    fun restoreHotbar(player: ServerPlayerEntity) {
        val session = sessions.remove(player.uuid) ?: return
        
        for (i in 0 until 9) {
            player.inventory.setStack(i, session.originalHotbar[i])
        }
        
        player.sendMessage(Text.translatable("message.cresora.hotbar_restored").formatted(Formatting.GRAY), true)
    }

    fun isOverridden(player: ServerPlayerEntity): Boolean = sessions.containsKey(player.uuid)

    private fun createSubSkillStack(effectId: String): ItemStack {
        val stack = ItemStack(CreSoraUtilities.SUB_SKILL_DUMMY)
        stack.set(ModDataComponents.SUB_SKILL_EFFECT_ID, effectId)
        return stack
    }
}
