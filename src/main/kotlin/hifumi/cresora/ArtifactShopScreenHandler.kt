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
        private const val ROWS = 1
        private const val SHOP_SLOT_COUNT = 9
        private const val PROPERTY_AVAILABLE_CREDITS_LOW = 0
    }

    private val shopDefinitions = ArtifactSpecialItemRegistry.shopDefinitions().take(SHOP_SLOT_COUNT)
    private val shopInventory: Inventory = object : SimpleInventory(SHOP_SLOT_COUNT) {}
    private val properties: PropertyDelegate = ArrayPropertyDelegate(2)

    init {
        refreshCreditProperty()
        addProperties(properties)
        refreshShopOffers()

        for (index in 0 until SHOP_SLOT_COUNT) {
            addSlot(object : Slot(shopInventory, index, 8 + index * 18, 18) {
                override fun canInsert(stack: ItemStack): Boolean = false
                override fun canTakeItems(playerEntity: PlayerEntity): Boolean = false
                override fun getBackgroundSprite(): Identifier? = null
            })
        }

        addPlayerSlots(playerInventory, ROWS)
    }

    override fun canUse(player: PlayerEntity): Boolean = true

    override fun sendContentUpdates() {
        refreshCreditProperty()
        refreshShopOffers()
        super.sendContentUpdates()
    }

    override fun quickMove(player: PlayerEntity, slotIndex: Int): ItemStack = ItemStack.EMPTY

    override fun onSlotClick(slotIndex: Int, button: Int, actionType: SlotActionType, player: PlayerEntity) {
        if (slotIndex in 0 until SHOP_SLOT_COUNT) {
            if (actionType == SlotActionType.PICKUP || actionType == SlotActionType.QUICK_MOVE) {
                attemptPurchase(slotIndex, player)
            }
            return
        }
        super.onSlotClick(slotIndex, button, actionType, player)
    }

    fun currentCredits(): Int = ScreenSyncSupport.readInt(properties, PROPERTY_AVAILABLE_CREDITS_LOW)

    private fun attemptPurchase(slotIndex: Int, player: PlayerEntity) {
        val serverPlayer = player as? ServerPlayerEntity ?: return
        val definition = shopDefinitions.getOrNull(slotIndex) ?: return
        if (!CreditsService.spendCredits(serverPlayer, definition.shopPrice)) {
            player.sendMessage(Text.translatable("item.cresora.not_enough_credits"), false)
            return
        }

        val item = ArtifactSpecialItemSupport.itemForDefinitionId(definition.id) ?: return
        player.inventory.offerOrDrop(ItemStack(item))
        player.sendMessage(
            Text.translatable(
                "screen.cresora.shop.purchase_success",
                Text.translatable(definition.translationKeyId),
                ArtifactSpecialItem.formatWholeNumber(definition.shopPrice)
            ),
            false
        )
        sendContentUpdates()
    }

    private fun refreshShopOffers() {
        for (index in 0 until SHOP_SLOT_COUNT) {
            val definition = shopDefinitions.getOrNull(index)
            shopInventory.setStack(index, definition?.let(ArtifactDisplayStackFactory::shopDisplay) ?: ItemStack.EMPTY)
        }
    }

    private fun refreshCreditProperty() {
        val serverPlayer = playerInventory.player as? ServerPlayerEntity ?: return
        ScreenSyncSupport.writeInt(properties, PROPERTY_AVAILABLE_CREDITS_LOW, CreditsService.getCredits(serverPlayer))
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
