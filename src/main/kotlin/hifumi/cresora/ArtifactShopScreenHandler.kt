package hifumi.cresora

import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.inventory.Inventory
import net.minecraft.inventory.SimpleInventory
import net.minecraft.item.ItemStack
import net.minecraft.screen.ArrayPropertyDelegate
import net.minecraft.screen.PropertyDelegate
import net.minecraft.screen.ScreenHandler
import net.minecraft.screen.slot.Slot
import net.minecraft.screen.slot.SlotActionType
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import net.minecraft.util.Identifier

class ArtifactShopScreenHandler(
    syncId: Int,
    private val playerInventory: PlayerInventory
) : ScreenHandler(CreSoraUtilities.ARTIFACT_SHOP_SCREEN_HANDLER, syncId) {

    companion object {
        private const val ROWS = 3
        private const val SHOP_SLOT_COUNT = 18
        private const val TOTAL_SLOT_COUNT = 27
        private const val SELL_INPUT_SLOT = 18
        private const val SELL_PREVIEW_SLOT = 19
        private const val SELL_BUTTON_SLOT = 22
        private const val PROPERTY_AVAILABLE_CREDITS_LOW = 0
        private const val PROPERTY_SELL_PRICE_LOW = 2
    }

    private val shopDefinitions = ShopContentRegistry.shopOffers().take(SHOP_SLOT_COUNT)
    private var syncingInventory = false
    private val shopInventory: Inventory = object : SimpleInventory(TOTAL_SLOT_COUNT) {
        override fun markDirty() {
            super.markDirty()
            if (!syncingInventory) {
                refreshCreditProperty()
                refreshShopOffers()
                refreshSellWidgets()
            }
        }
    }
    private val properties: PropertyDelegate = ArrayPropertyDelegate(4)

    init {
        refreshCreditProperty()
        addProperties(properties)
        refreshShopOffers()
        refreshSellWidgets()

        for (index in 0 until SHOP_SLOT_COUNT) {
            addSlot(readonlySlot(index, 8 + (index % 9) * 18, 18 + (index / 9) * 18))
        }

        addSlot(object : Slot(shopInventory, SELL_INPUT_SLOT, 8, 54) {
            override fun canInsert(stack: ItemStack): Boolean = ArtifactSellService.preview(stack).sellable
            override fun getMaxItemCount(): Int = 1
            override fun getBackgroundSprite(): Identifier? = null
        })
        addSlot(readonlySlot(SELL_PREVIEW_SLOT, 26, 54))
        addSlot(readonlySlot(20, 44, 54))
        addSlot(readonlySlot(21, 62, 54))
        addSlot(readonlySlot(SELL_BUTTON_SLOT, 80, 54))
        addSlot(readonlySlot(23, 98, 54))
        addSlot(readonlySlot(24, 116, 54))
        addSlot(readonlySlot(25, 134, 54))
        addSlot(readonlySlot(26, 152, 54))

        addPlayerSlots(playerInventory, ROWS)
    }

    override fun canUse(player: PlayerEntity): Boolean = true

    override fun sendContentUpdates() {
        refreshCreditProperty()
        refreshShopOffers()
        refreshSellWidgets()
        super.sendContentUpdates()
    }

    override fun quickMove(player: PlayerEntity, slotIndex: Int): ItemStack {
        val slot = slots.getOrNull(slotIndex) ?: return ItemStack.EMPTY
        if (!slot.hasStack()) {
            return ItemStack.EMPTY
        }
        val original = slot.stack
        val moved = original.copy()

        if (slotIndex < TOTAL_SLOT_COUNT) {
            if (slotIndex != SELL_INPUT_SLOT) {
                return ItemStack.EMPTY
            }
            if (!insertItem(original, TOTAL_SLOT_COUNT, slots.size, true)) {
                return ItemStack.EMPTY
            }
        } else {
            if (!ArtifactSellService.preview(original).sellable || slots[SELL_INPUT_SLOT].hasStack()) {
                return ItemStack.EMPTY
            }
            val single = original.copyWithCount(1)
            slots[SELL_INPUT_SLOT].setStack(single)
            original.decrement(1)
        }

        if (original.isEmpty) {
            slot.setStack(ItemStack.EMPTY)
        } else {
            slot.markDirty()
        }
        sendContentUpdates()
        return moved
    }

    override fun onSlotClick(slotIndex: Int, button: Int, actionType: SlotActionType, player: PlayerEntity) {
        if (slotIndex in 0 until SHOP_SLOT_COUNT) {
            if (actionType == SlotActionType.PICKUP || actionType == SlotActionType.QUICK_MOVE) {
                attemptPurchase(slotIndex, player)
            }
            return
        }
        if (slotIndex == SELL_BUTTON_SLOT || slotIndex == SELL_PREVIEW_SLOT) {
            if (actionType == SlotActionType.PICKUP || actionType == SlotActionType.QUICK_MOVE) {
                attemptSell(player)
            }
            return
        }
        super.onSlotClick(slotIndex, button, actionType, player)
    }

    fun currentCredits(): Int = ScreenSyncSupport.readInt(properties, PROPERTY_AVAILABLE_CREDITS_LOW)

    fun currentSellPrice(): Int = ScreenSyncSupport.readInt(properties, PROPERTY_SELL_PRICE_LOW)

    private fun attemptPurchase(slotIndex: Int, player: PlayerEntity) {
        val serverPlayer = player as? ServerPlayerEntity ?: return
        val offer = shopDefinitions.getOrNull(slotIndex) ?: return
        if (!CreditsService.spendCredits(serverPlayer, offer.price)) {
            player.sendMessage(Text.translatable("item.cresora.not_enough_credits"), false)
            return
        }

        offer.grant(serverPlayer)
        player.sendMessage(
            Text.translatable(
                "screen.cresora.shop.purchase_success",
                offer.displayName(),
                ArtifactSpecialItem.formatWholeNumber(offer.price)
            ),
            false
        )
        sendContentUpdates()
    }

    private fun attemptSell(player: PlayerEntity) {
        val serverPlayer = player as? ServerPlayerEntity ?: return
        val stack = shopInventory.getStack(SELL_INPUT_SLOT)
        val preview = ArtifactSellService.preview(stack)
        if (!preview.sellable || preview.totalPrice <= 0) {
            player.sendMessage(Text.translatable("screen.cresora.shop.sell_invalid"), false)
            return
        }

        val soldName = stack.name
        CreditsService.addCredits(serverPlayer, preview.totalPrice)
        setInventoryStack(SELL_INPUT_SLOT, ItemStack.EMPTY)
        player.sendMessage(
            Text.translatable(
                "screen.cresora.shop.sell_success",
                soldName,
                ArtifactSpecialItem.formatWholeNumber(preview.totalPrice)
            ),
            false
        )
        sendContentUpdates()
    }

    private fun refreshShopOffers() {
        for (index in 0 until SHOP_SLOT_COUNT) {
            val definition = shopDefinitions.getOrNull(index)
            setInventoryStack(index, definition?.createDisplayStack() ?: ItemStack.EMPTY)
        }
    }

    private fun refreshCreditProperty() {
        val serverPlayer = playerInventory.player as? ServerPlayerEntity ?: return
        ScreenSyncSupport.writeInt(properties, PROPERTY_AVAILABLE_CREDITS_LOW, CreditsService.getCredits(serverPlayer))
    }

    private fun refreshSellWidgets() {
        val preview = ArtifactSellService.preview(shopInventory.getStack(SELL_INPUT_SLOT))
        ScreenSyncSupport.writeInt(properties, PROPERTY_SELL_PRICE_LOW, preview.totalPrice)
        setInventoryStack(SELL_PREVIEW_SLOT, ArtifactDisplayStackFactory.shopSellPreview(preview))
        setInventoryStack(SELL_BUTTON_SLOT, ArtifactDisplayStackFactory.shopSellButton(preview))
    }

    private fun setInventoryStack(index: Int, stack: ItemStack) {
        syncingInventory = true
        shopInventory.setStack(index, stack)
        syncingInventory = false
    }

    private fun readonlySlot(index: Int, x: Int, y: Int): Slot {
        return object : Slot(shopInventory, index, x, y) {
            override fun canInsert(stack: ItemStack): Boolean = false
            override fun canTakeItems(playerEntity: PlayerEntity): Boolean = false
            override fun getBackgroundSprite(): Identifier? = null
        }
    }

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
