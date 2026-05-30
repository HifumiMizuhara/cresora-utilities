package hifumi.cresora.leyline

import hifumi.cresora.CreSoraUtilities
import hifumi.cresora.ScreenSyncSupport
import hifumi.cresora.adventurerank.AdventureRankService
import hifumi.cresora.credits.CreditsService
import net.minecraft.component.DataComponentTypes
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
import net.minecraft.util.math.BlockPos

class LeyLineSelectionScreenHandler(
    syncId: Int,
    private val playerInventory: PlayerInventory,
    val properties: PropertyDelegate = ArrayPropertyDelegate(7)
) : ScreenHandler(CreSoraUtilities.LEY_LINE_SELECTION_SCREEN_HANDLER, syncId) {

    companion object {
        const val PROPERTY_CREDITS_LOW = 0
        const val PROPERTY_CREDITS_HIGH = 1
        const val PROPERTY_RANK = 2
        const val PROPERTY_ELEMENT = 3
        const val PROPERTY_POS_X = 4
        const val PROPERTY_POS_Y = 5
        const val PROPERTY_POS_Z = 6

        const val TIER_SLOT_COUNT = 4
    }

    private val selectionInventory: Inventory = object : SimpleInventory(TIER_SLOT_COUNT) {}

    init {
        addProperties(properties)
        refreshProperties()
        refreshEntries()

        // 4 selectable tier slots
        for (index in 0 until TIER_SLOT_COUNT) {
            addSlot(object : Slot(selectionInventory, index, 35 + index * 24, 18) {
                override fun canInsert(stack: ItemStack): Boolean = false
                override fun canTakeItems(playerEntity: PlayerEntity): Boolean = false
            })
        }

        addPlayerSlots(playerInventory)
    }

    override fun canUse(player: PlayerEntity): Boolean {
        val pos = getBlockPos()
        return player.squaredDistanceTo(pos.x + 0.5, pos.y + 0.5, pos.z + 0.5) <= 64.0
    }

    override fun sendContentUpdates() {
        refreshProperties()
        refreshEntries()
        super.sendContentUpdates()
    }

    override fun quickMove(player: PlayerEntity, slotIndex: Int): ItemStack = ItemStack.EMPTY

    override fun onSlotClick(slotIndex: Int, button: Int, actionType: SlotActionType, player: PlayerEntity) {
        if (slotIndex in 0 until TIER_SLOT_COUNT) {
            if (actionType == SlotActionType.PICKUP || actionType == SlotActionType.QUICK_MOVE) {
                attemptStart(slotIndex + 1, player)
            }
            return
        }
        super.onSlotClick(slotIndex, button, actionType, player)
    }

    fun currentCredits(): Int = ScreenSyncSupport.readInt(properties, PROPERTY_CREDITS_LOW)

    fun currentRank(): Int = properties.get(PROPERTY_RANK)

    fun getElement(): LeyLineElement {
        val ord = properties.get(PROPERTY_ELEMENT).coerceIn(0, LeyLineElement.entries.size - 1)
        return LeyLineElement.entries[ord]
    }

    fun getBlockPos(): BlockPos {
        return BlockPos(
            properties.get(PROPERTY_POS_X),
            properties.get(PROPERTY_POS_Y),
            properties.get(PROPERTY_POS_Z)
        )
    }

    fun getTierUnlockRank(tier: Int): Int {
        return when (tier) {
            1 -> 1
            2 -> 10
            3 -> 20
            4 -> 35
            else -> 1
        }
    }

    private fun attemptStart(tier: Int, player: PlayerEntity) {
        val serverPlayer = player as? ServerPlayerEntity ?: return
        val pos = getBlockPos()
        val element = getElement()
        
        val result = LeyLineService.startEvent(serverPlayer, pos, element, tier)
        if (result.success) {
            player.closeHandledScreen()
        } else {
            player.sendMessage(Text.translatable(result.translationKey, *result.args.toTypedArray()), false)
            sendContentUpdates()
        }
    }

    private fun refreshEntries() {
        val element = getElement()
        val keyItem = CreSoraUtilities.LEY_LINE_KEYS[element] ?: return
        for (index in 0 until TIER_SLOT_COUNT) {
            val tier = index + 1
            val stack = ItemStack(keyItem, tier)
            stack.set(
                DataComponentTypes.CUSTOM_NAME,
                Text.translatable("screen.cresora.leyline.tier_title", tier)
            )
            selectionInventory.setStack(index, stack)
        }
    }

    private fun refreshProperties() {
        val serverPlayer = playerInventory.player as? ServerPlayerEntity ?: return
        ScreenSyncSupport.writeInt(properties, PROPERTY_CREDITS_LOW, CreditsService.getCredits(serverPlayer))
        properties.set(PROPERTY_RANK, AdventureRankService.getRank(serverPlayer))
    }

    private fun addPlayerSlots(playerInventory: PlayerInventory) {
        val inventoryStartY = 50
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
