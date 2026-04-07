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

class MasqueradeLoadoutScreenHandler(
    syncId: Int,
    private val playerInventory: PlayerInventory
) : ScreenHandler(CreSoraUtilities.MASQUERADE_LOADOUT_SCREEN_HANDLER, syncId) {

    private data class SelectableWeapon(
        val inventorySlot: Int,
        val stack: ItemStack
    )

    companion object {
        private const val ROWS = 4
        private const val SLOT_COUNT = ROWS * 9
        private const val SELECTED_SLOT_COUNT = 4
        private const val START_SLOT = 4
        private const val INFO_SEASON_SLOT = 5
        private const val INFO_CURRENT_SLOT = 6
        private const val INFO_PREVIOUS_SLOT = 7
        private const val INFO_ATTEMPT_SLOT = 8
        private const val OPTION_START_SLOT = 9
        private const val OPTION_SLOT_COUNT = 27
        private const val PROPERTY_SELECTED_COUNT = 0
        private const val PROPERTY_PLAYER_RANK = 1
    }

    private val displayInventory: Inventory = object : SimpleInventory(SLOT_COUNT) {}
    private val properties: PropertyDelegate = ArrayPropertyDelegate(2)
    private val eligibleWeapons: MutableList<SelectableWeapon> = mutableListOf()
    private val selectedInventorySlots: MutableList<Int> = mutableListOf()

    init {
        addProperties(properties)
        refreshEligibleWeapons()
        refreshProperties()
        refreshEntries()
        for (index in 0 until SLOT_COUNT) {
            val row = index / 9
            val column = index % 9
            addSlot(object : Slot(displayInventory, index, 8 + column * 18, 18 + row * 18) {
                override fun canInsert(stack: ItemStack): Boolean = false
                override fun canTakeItems(playerEntity: PlayerEntity): Boolean = false
                override fun getBackgroundSprite(): Identifier? = null
            })
        }
        addPlayerSlots(playerInventory, ROWS)
    }

    override fun canUse(player: PlayerEntity): Boolean = true

    override fun sendContentUpdates() {
        refreshEligibleWeapons()
        refreshProperties()
        refreshEntries()
        super.sendContentUpdates()
    }

    override fun quickMove(player: PlayerEntity, slotIndex: Int): ItemStack = ItemStack.EMPTY

    override fun onSlotClick(slotIndex: Int, button: Int, actionType: SlotActionType, player: PlayerEntity) {
        if (slotIndex in 0 until SLOT_COUNT) {
            if (actionType == SlotActionType.PICKUP || actionType == SlotActionType.QUICK_MOVE) {
                when {
                    slotIndex in 0 until SELECTED_SLOT_COUNT -> unselectByOrder(slotIndex)
                    slotIndex == START_SLOT -> attemptStart(player)
                    slotIndex in OPTION_START_SLOT until (OPTION_START_SLOT + OPTION_SLOT_COUNT) -> toggleSelection(slotIndex)
                }
            }
            return
        }
    }

    fun currentRank(): Int = properties.get(PROPERTY_PLAYER_RANK)

    fun selectedCount(): Int = properties.get(PROPERTY_SELECTED_COUNT)

    private fun toggleSelection(slotIndex: Int) {
        val weapon = eligibleWeapons.getOrNull(slotIndex - OPTION_START_SLOT) ?: return
        if (selectedInventorySlots.contains(weapon.inventorySlot)) {
            selectedInventorySlots.remove(weapon.inventorySlot)
        } else if (selectedInventorySlots.size < SELECTED_SLOT_COUNT) {
            selectedInventorySlots += weapon.inventorySlot
        }
        refreshEntries()
        sendContentUpdates()
    }

    private fun unselectByOrder(orderIndex: Int) {
        if (orderIndex !in 0 until selectedInventorySlots.size) {
            return
        }
        selectedInventorySlots.removeAt(orderIndex)
        refreshEntries()
        sendContentUpdates()
    }

    private fun attemptStart(player: PlayerEntity) {
        val serverPlayer = player as? ServerPlayerEntity ?: return
        val result = MasqueradeService.startSession(serverPlayer, selectedInventorySlots.toList())
        if (result.success) {
            player.closeHandledScreen()
        } else {
            player.sendMessage(Text.translatable(result.translationKey, *result.args.toTypedArray()), false)
            sendContentUpdates()
        }
    }

    private fun refreshEligibleWeapons() {
        val selectedSet = selectedInventorySlots.toSet()
        eligibleWeapons.clear()
        for (slot in 0 until 36) {
            val stack = playerInventory.getStack(slot)
            if (!WeaponStackSupport.isWeapon(stack)) {
                continue
            }
            eligibleWeapons += SelectableWeapon(slot, stack.copy())
        }
        selectedInventorySlots.removeIf { selected -> selectedSet.contains(selected) && eligibleWeapons.none { it.inventorySlot == selected } }
    }

    private fun refreshEntries() {
        for (index in 0 until SLOT_COUNT) {
            displayInventory.setStack(index, ArtifactDisplayStackFactory.fillerDisplay())
        }
        for ((order, inventorySlot) in selectedInventorySlots.withIndex()) {
            val weapon = eligibleWeapons.firstOrNull { it.inventorySlot == inventorySlot } ?: continue
            displayInventory.setStack(order, ArtifactDisplayStackFactory.masqueradeLoadoutSelectedDisplay(weapon.stack, order + 1))
        }
        displayInventory.setStack(START_SLOT, ArtifactDisplayStackFactory.masqueradeLoadoutStartButtonDisplay(selectedInventorySlots.isNotEmpty()))
        val serverPlayer = playerInventory.player as? ServerPlayerEntity
        if (serverPlayer != null) {
            val progress = MasqueradeProgressService.getProgress(serverPlayer)
            displayInventory.setStack(INFO_SEASON_SLOT, ArtifactDisplayStackFactory.masqueradeSeasonDisplay(progress.current))
            displayInventory.setStack(INFO_CURRENT_SLOT, ArtifactDisplayStackFactory.masqueradeCurrentRecordDisplay(progress.current))
            displayInventory.setStack(INFO_PREVIOUS_SLOT, ArtifactDisplayStackFactory.masqueradePreviousRecordDisplay(progress.previousSeason()))
            displayInventory.setStack(INFO_ATTEMPT_SLOT, ArtifactDisplayStackFactory.masqueradeAttemptDisplay(progress.current))
        }
        for (index in 0 until OPTION_SLOT_COUNT) {
            val weapon = eligibleWeapons.getOrNull(index) ?: continue
            val selectedOrder = selectedInventorySlots.indexOf(weapon.inventorySlot) + 1
            displayInventory.setStack(
                OPTION_START_SLOT + index,
                ArtifactDisplayStackFactory.masqueradeLoadoutOptionDisplay(weapon.stack, selectedOrder)
            )
        }
        properties.set(PROPERTY_SELECTED_COUNT, selectedInventorySlots.size)
    }

    private fun refreshProperties() {
        val serverPlayer = playerInventory.player as? ServerPlayerEntity ?: return
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
