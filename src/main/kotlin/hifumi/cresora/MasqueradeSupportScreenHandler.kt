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

class MasqueradeSupportScreenHandler(
    syncId: Int,
    private val playerInventory: PlayerInventory
) : ScreenHandler(CreSoraUtilities.MASQUERADE_SUPPORT_SCREEN_HANDLER, syncId) {

    companion object {
        private const val ROWS = 1
        private const val SLOT_COUNT = 9
        private val OPTION_SLOTS = listOf(2, 4, 6)
        private const val PROPERTY_CURRENT_WAVE = 0
        private const val PROPERTY_MAX_WAVE = 1
        private const val PROPERTY_CLEARED_WAVE = 2
    }

    private val displayInventory: Inventory = object : SimpleInventory(SLOT_COUNT) {}
    private val properties: PropertyDelegate = ArrayPropertyDelegate(3)
    private var candidateIds: List<String> = emptyList()

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
                val optionIndex = OPTION_SLOTS.indexOf(slotIndex)
                if (optionIndex >= 0) {
                    selectOption(optionIndex, player)
                }
            }
            return
        }
    }

    fun currentWave(): Int = properties.get(PROPERTY_CURRENT_WAVE)

    fun maxWave(): Int = properties.get(PROPERTY_MAX_WAVE)

    fun clearedWave(): Int = properties.get(PROPERTY_CLEARED_WAVE)

    private fun selectOption(optionIndex: Int, player: PlayerEntity) {
        val serverPlayer = player as? ServerPlayerEntity ?: return
        val buffId = candidateIds.getOrNull(optionIndex) ?: return
        val result = MasqueradeService.selectSupportBuff(serverPlayer, buffId)
        if (result.success) {
            player.closeHandledScreen()
        } else {
            player.sendMessage(Text.translatable(result.translationKey, *result.args.toTypedArray()), false)
        }
        sendContentUpdates()
    }

    private fun refreshEntries() {
        candidateIds = (playerInventory.player as? ServerPlayerEntity)?.let(MasqueradeService::supportCandidateIds) ?: emptyList()
        for (index in 0 until SLOT_COUNT) {
            displayInventory.setStack(index, ItemStack.EMPTY)
        }
        for ((index, buffId) in candidateIds.withIndex()) {
            val definition = runCatching { MasqueradeContentRegistry.supportBuff(buffId) }.getOrNull() ?: continue
            val slot = OPTION_SLOTS.getOrNull(index) ?: continue
            displayInventory.setStack(slot, ArtifactDisplayStackFactory.masqueradeSupportDisplay(definition))
        }
    }

    private fun refreshProperties() {
        val serverPlayer = playerInventory.player as? ServerPlayerEntity ?: return
        properties.set(PROPERTY_CURRENT_WAVE, MasqueradeService.currentWave(serverPlayer))
        properties.set(PROPERTY_MAX_WAVE, MasqueradeContentRegistry.definition().maxWaveCount)
        properties.set(PROPERTY_CLEARED_WAVE, MasqueradeService.clearedWaveCount(serverPlayer))
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
