package hifumi.cresora

import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.inventory.Inventory
import net.minecraft.inventory.SimpleInventory
import net.minecraft.item.ItemStack
import net.minecraft.screen.ScreenHandler
import net.minecraft.screen.slot.Slot
import net.minecraft.util.Identifier

class DomainRewardScreenHandler(
    syncId: Int,
    playerInventory: PlayerInventory
) : ScreenHandler(CreSoraUtilities.DOMAIN_REWARD_SCREEN_HANDLER, syncId) {

    companion object {
        private const val ROWS = 1
        private const val REWARD_SLOT_COUNT = 9
    }

    private val rewardInventory: Inventory = object : SimpleInventory(REWARD_SLOT_COUNT) {}

    constructor(
        syncId: Int,
        playerInventory: PlayerInventory,
        displayStacks: List<ItemStack>
    ) : this(syncId, playerInventory) {
        for (index in 0 until REWARD_SLOT_COUNT) {
            rewardInventory.setStack(index, displayStacks.getOrNull(index)?.copy() ?: ArtifactDisplayStackFactory.fillerDisplay())
        }
    }

    init {
        for (index in 0 until REWARD_SLOT_COUNT) {
            addSlot(object : Slot(rewardInventory, index, 8 + index * 18, 18) {
                override fun canInsert(stack: ItemStack): Boolean = false
                override fun canTakeItems(playerEntity: PlayerEntity): Boolean = false
                override fun getBackgroundSprite(): Identifier? = null
            })
        }

        addPlayerSlots(playerInventory, ROWS)
    }

    override fun canUse(player: PlayerEntity): Boolean = true

    override fun quickMove(player: PlayerEntity, slotIndex: Int): ItemStack = ItemStack.EMPTY

    private fun addPlayerSlots(playerInventory: PlayerInventory, rows: Int) {
        val inventoryStartY = 18 + rows * 18 + 14
        val hotbarY = inventoryStartY + 58
        for (row in 0 until 3) {
            for (column in 0 until 9) {
                addSlot(Slot(playerInventory, column + row * 9 + 9, 8 + column * 18, inventoryStartY + row * 18))
            }
        }

        for (column in 0 until 9) {
            addSlot(Slot(playerInventory, column, 8 + column * 18, hotbarY))
        }
    }
}
