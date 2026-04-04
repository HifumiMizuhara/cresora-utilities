package hifumi.cresora

import net.minecraft.entity.player.PlayerEntity

object InventoryGate {
    private const val MAIN_INVENTORY_SLOT_COUNT = 36

    fun hasFreeMainSlot(player: PlayerEntity): Boolean {
        return freeMainSlots(player) > 0
    }

    fun freeMainSlots(player: PlayerEntity): Int {
        return (0 until MAIN_INVENTORY_SLOT_COUNT).count { slot ->
            player.inventory.getStack(slot).isEmpty
        }
    }
}
