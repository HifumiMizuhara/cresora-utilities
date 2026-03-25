package hifumi.cresora

import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.inventory.Inventory
import net.minecraft.inventory.SimpleInventory
import net.minecraft.item.ItemStack
import net.minecraft.screen.ScreenHandler
import net.minecraft.screen.slot.Slot
import net.minecraft.text.Text
import net.minecraft.util.Identifier

class UpgradeScreenHandler(
    syncId: Int,
    private val playerInventory: PlayerInventory
) : ScreenHandler(CreSoraUtilities.UPGRADE_SCREEN_HANDLER, syncId) {

    companion object {
        const val BUTTON_UPGRADE = 0
        private const val TARGET_SLOT = 0
        private const val MATERIAL_SLOT = 1
        private const val CUSTOM_SLOT_COUNT = 2
    }

    private val upgradeInventory: Inventory = object : SimpleInventory(CUSTOM_SLOT_COUNT) {
        override fun markDirty() {
            super.markDirty()
            onContentChanged(this)
        }
    }

    init {
        addSlot(object : Slot(upgradeInventory, TARGET_SLOT, 27, 30) {
            override fun canInsert(stack: ItemStack): Boolean = EquipmentStackSupport.isEquipment(stack)
            override fun getMaxItemCount(): Int = 1
            override fun getBackgroundSprite(): Identifier? = null
        })
        addSlot(object : Slot(upgradeInventory, MATERIAL_SLOT, 76, 30) {
            override fun canInsert(stack: ItemStack): Boolean {
                return stack.isOf(CreSoraUtilities.TUESHOKAKU) || EquipmentStackSupport.isEquipment(stack)
            }

            override fun getMaxItemCount(stack: ItemStack): Int {
                return if (EquipmentStackSupport.isEquipment(stack)) 1 else super.getMaxItemCount(stack)
            }

            override fun getBackgroundSprite(): Identifier? = null
        })

        addPlayerSlots(playerInventory)

        if (!playerInventory.player.world.isClient) {
            tryMoveSelectedWand()
        }
    }

    override fun canUse(player: PlayerEntity): Boolean = true

    override fun quickMove(player: PlayerEntity, slotIndex: Int): ItemStack {
        val slot = slots.getOrNull(slotIndex) ?: return ItemStack.EMPTY
        if (!slot.hasStack()) {
            return ItemStack.EMPTY
        }

        val originalStack = slot.stack
        val movedStack = originalStack.copy()

        if (slotIndex < CUSTOM_SLOT_COUNT) {
            if (!insertItem(originalStack, CUSTOM_SLOT_COUNT, slots.size, true)) {
                return ItemStack.EMPTY
            }
        } else {
            val targetIndex = when {
                EquipmentStackSupport.isEquipment(originalStack) && !slots[TARGET_SLOT].hasStack() -> TARGET_SLOT
                (originalStack.isOf(CreSoraUtilities.TUESHOKAKU) || EquipmentStackSupport.isEquipment(originalStack)) -> MATERIAL_SLOT
                else -> return ItemStack.EMPTY
            }

            if (!insertItem(originalStack, targetIndex, targetIndex + 1, false)) {
                return ItemStack.EMPTY
            }
        }

        if (originalStack.isEmpty) {
            slot.stack = ItemStack.EMPTY
        } else {
            slot.markDirty()
        }

        return movedStack
    }

    override fun onButtonClick(player: PlayerEntity, id: Int): Boolean {
        if (id != BUTTON_UPGRADE) {
            return super.onButtonClick(player, id)
        }

        val baseStack = slots[TARGET_SLOT].stack
        val materialStack = slots[MATERIAL_SLOT].stack
        val result = UpgradeLogic.attemptUpgrade(player, baseStack, materialStack)
        player.sendMessage(result.message, false)
        sendContentUpdates()
        return true
    }

    override fun onClosed(player: PlayerEntity) {
        super.onClosed(player)
        if (player.world.isClient) {
            return
        }

        for (slotIndex in 0 until CUSTOM_SLOT_COUNT) {
            val stack = upgradeInventory.removeStack(slotIndex)
            if (!stack.isEmpty) {
                playerInventory.offerOrDrop(stack)
            }
        }
    }

    fun getBaseStack(): ItemStack = slots[TARGET_SLOT].stack

    fun getMaterialStack(): ItemStack = slots[MATERIAL_SLOT].stack

    fun getPreview(player: PlayerEntity): UpgradeLogic.Preview {
        return UpgradeLogic.getPreview(getBaseStack(), getMaterialStack(), player)
    }

    fun getXpCost(): Int = UpgradeLogic.XP_COST

    fun getScreenTitle(): Text = Text.translatable("screen.cresora.upgrade")

    private fun tryMoveSelectedWand() {
        val selectedSlot = playerInventory.selectedSlot
        val selectedStack = playerInventory.getStack(selectedSlot)
        if (!EquipmentStackSupport.isEquipment(selectedStack) || slots[TARGET_SLOT].hasStack()) {
            return
        }

        val extracted = playerInventory.removeStack(selectedSlot, 1)
        if (!extracted.isEmpty) {
            slots[TARGET_SLOT].stack = extracted
        }
    }

    private fun addPlayerSlots(playerInventory: PlayerInventory) {
        for (row in 0 until 3) {
            for (column in 0 until 9) {
                addSlot(Slot(playerInventory, column + row * 9 + 9, 8 + column * 18, 84 + row * 18))
            }
        }

        for (column in 0 until 9) {
            addSlot(Slot(playerInventory, column, 8 + column * 18, 142))
        }
    }
}
