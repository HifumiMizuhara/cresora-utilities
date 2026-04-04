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
import net.minecraft.util.Identifier

class CresoraMenuScreenHandler(
    syncId: Int,
    private val playerInventory: PlayerInventory
) : ScreenHandler(CreSoraUtilities.CRESORA_MENU_SCREEN_HANDLER, syncId) {

    companion object {
        private const val ROWS = 1
        private const val SLOT_COUNT = 9
        private const val STORY_SLOT = 1
        private const val DOMAIN_SLOT = 3
        private const val MASQUERADE_SLOT = 4
        private const val SHOP_SLOT = 5
        private const val RESONANCE_SLOT = 7
        private const val PROPERTY_AVAILABLE_CREDITS_LOW = 0
        private const val PROPERTY_PLAYER_RANK = 2
    }

    private val displayInventory: Inventory = object : SimpleInventory(SLOT_COUNT) {}
    private val properties: PropertyDelegate = ArrayPropertyDelegate(3)

    init {
        addProperties(properties)
        refreshProperties()
        refreshEntries()
        for (index in 0 until SLOT_COUNT) {
            addSlot(object : Slot(displayInventory, index, 8 + index * 18, 18) {
                override fun canInsert(stack: ItemStack): Boolean = false
                override fun canTakeItems(playerEntity: PlayerEntity): Boolean = false
                override fun getBackgroundSprite(): Identifier? = null
            })
        }
        addPlayerSlots(playerInventory, ROWS)
    }

    override fun canUse(player: PlayerEntity): Boolean = true

    override fun sendContentUpdates() {
        refreshProperties()
        refreshEntries()
        super.sendContentUpdates()
    }

    override fun quickMove(player: PlayerEntity, slotIndex: Int): ItemStack = ItemStack.EMPTY

    override fun onSlotClick(slotIndex: Int, button: Int, actionType: SlotActionType, player: PlayerEntity) {
        if (slotIndex in 0 until SLOT_COUNT) {
            if (actionType == SlotActionType.PICKUP || actionType == SlotActionType.QUICK_MOVE) {
                openMenuSlot(slotIndex, player)
            }
            return
        }
        super.onSlotClick(slotIndex, button, actionType, player)
    }

    fun currentCredits(): Int = ScreenSyncSupport.readInt(properties, PROPERTY_AVAILABLE_CREDITS_LOW)

    fun currentRank(): Int = properties.get(PROPERTY_PLAYER_RANK)

    private fun openMenuSlot(slotIndex: Int, player: PlayerEntity) {
        val serverPlayer = player as? ServerPlayerEntity ?: return
        when (slotIndex) {
            STORY_SLOT -> ArtifactUiFlow.openStoryChapterSelection(serverPlayer)
            DOMAIN_SLOT -> ArtifactUiFlow.openDomainSelection(serverPlayer)
            MASQUERADE_SLOT -> ArtifactUiFlow.openMasqueradeLoadout(serverPlayer)
            SHOP_SLOT -> ArtifactUiFlow.openShop(serverPlayer)
            RESONANCE_SLOT -> ArtifactUiFlow.openResonance(serverPlayer)
        }
    }

    private fun refreshEntries() {
        for (index in 0 until SLOT_COUNT) {
            displayInventory.setStack(index, ItemStack.EMPTY)
        }
        displayInventory.setStack(STORY_SLOT, ArtifactDisplayStackFactory.cresoraMenuDisplay("story"))
        displayInventory.setStack(DOMAIN_SLOT, ArtifactDisplayStackFactory.cresoraMenuDisplay("domain"))
        displayInventory.setStack(MASQUERADE_SLOT, ArtifactDisplayStackFactory.cresoraMenuDisplay("masquerade"))
        displayInventory.setStack(SHOP_SLOT, ArtifactDisplayStackFactory.cresoraMenuDisplay("shop"))
        displayInventory.setStack(RESONANCE_SLOT, ArtifactDisplayStackFactory.cresoraMenuDisplay("resonance"))
    }

    private fun refreshProperties() {
        val serverPlayer = playerInventory.player as? ServerPlayerEntity ?: return
        ScreenSyncSupport.writeInt(properties, PROPERTY_AVAILABLE_CREDITS_LOW, CreditsService.getCredits(serverPlayer))
        properties.set(PROPERTY_PLAYER_RANK, AdventureRankService.getRank(serverPlayer))
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
