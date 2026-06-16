package hifumi.cresora.weapon

import hifumi.cresora.resonance.ResonanceService
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.minecraft.item.ItemStack
import net.minecraft.server.network.ServerPlayerEntity

object SpiritBondService {
    fun init() {
        ServerTickEvents.END_SERVER_TICK.register { server ->
            if (server.overworld.time % 20L != 0L) {
                return@register
            }
            for (player in server.playerManager.playerList) {
                syncInventory(player)
            }
        }
    }

    fun syncInventory(player: ServerPlayerEntity) {
        for (slot in 0 until player.inventory.size()) {
            syncStack(player, player.inventory.getStack(slot))
        }
    }

    private fun syncStack(player: ServerPlayerEntity, stack: ItemStack) {
        if (stack.isEmpty) {
            return
        }
        val definition = WeaponStackSupport.getDefinition(stack) ?: return
        if (definition.spirit == null) {
            return
        }
        val data = WeaponStackSupport.getWeaponData(stack) ?: return
        val targetStage = ResonanceService.spiritBondStage(player, definition.id)
        if (data.spiritBondStage == targetStage) {
            return
        }
        WeaponStackSupport.syncWeaponData(
            stack,
            data.copy(spiritBondStage = targetStage)
        )
    }
}
