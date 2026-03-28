package hifumi.cresora

import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.inventory.Inventory
import net.minecraft.inventory.SimpleInventory
import net.minecraft.item.ItemStack
import net.minecraft.screen.ScreenHandler
import net.minecraft.screen.slot.Slot
import net.minecraft.screen.slot.SlotActionType
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import net.minecraft.util.Identifier

class ArtifactAlphaScreenHandler(
    syncId: Int,
    private val playerInventory: PlayerInventory
) : ScreenHandler(CreSoraUtilities.ARTIFACT_ALPHA_SCREEN_HANDLER, syncId) {

    companion object {
        private const val ROWS = 6
        private const val OPTION_SLOT_COUNT = ROWS * 9
    }

    private val optionInventory: Inventory = object : SimpleInventory(OPTION_SLOT_COUNT) {}
    private var baseStack: ItemStack = ItemStack.EMPTY
    private var materialStack: ItemStack = ItemStack.EMPTY
    private var completed = false

    constructor(
        syncId: Int,
        playerInventory: PlayerInventory,
        baseStack: ItemStack,
        materialStack: ItemStack
    ) : this(syncId, playerInventory) {
        this.baseStack = baseStack
        this.materialStack = materialStack
        refreshOptions()
    }

    init {
        for (index in 0 until OPTION_SLOT_COUNT) {
            val row = index / 9
            val column = index % 9
            addSlot(object : Slot(optionInventory, index, 8 + column * 18, 18 + row * 18) {
                override fun canInsert(stack: ItemStack): Boolean = false
                override fun canTakeItems(playerEntity: PlayerEntity): Boolean = false
                override fun getBackgroundSprite(): Identifier? = null
            })
        }

        addPlayerSlots(playerInventory, ROWS)
    }

    override fun canUse(player: PlayerEntity): Boolean = true

    override fun quickMove(player: PlayerEntity, slotIndex: Int): ItemStack = ItemStack.EMPTY

    override fun onSlotClick(slotIndex: Int, button: Int, actionType: SlotActionType, player: PlayerEntity) {
        if (slotIndex in 0 until OPTION_SLOT_COUNT) {
            if (actionType == SlotActionType.PICKUP || actionType == SlotActionType.QUICK_MOVE) {
                attemptSelection(slotIndex, player)
            }
            return
        }
        super.onSlotClick(slotIndex, button, actionType, player)
    }

    override fun onClosed(player: PlayerEntity) {
        super.onClosed(player)
        if (player.world.isClient || completed) {
            return
        }

        if (!baseStack.isEmpty) {
            playerInventory.offerOrDrop(baseStack)
        }
        if (!materialStack.isEmpty) {
            playerInventory.offerOrDrop(materialStack)
        }
        baseStack = ItemStack.EMPTY
        materialStack = ItemStack.EMPTY
    }

    private fun currentCandidates(): List<EquipmentDefinition> {
        return ArtifactSpecialUpgradeService.alphaCandidates(baseStack).take(OPTION_SLOT_COUNT)
    }

    private fun attemptSelection(slotIndex: Int, player: PlayerEntity) {
        val serverPlayer = player as? ServerPlayerEntity ?: return
        val targetDefinition = currentCandidates().getOrNull(slotIndex) ?: return
        val result = ArtifactSpecialUpgradeService.performAlphaSelection(serverPlayer, baseStack, materialStack, targetDefinition.id) ?: return

        completed = true
        baseStack = ItemStack.EMPTY
        materialStack = ItemStack.EMPTY
        ArtifactUiFlow.openUpgradeScreen(serverPlayer, result, ItemStack.EMPTY)
    }

    private fun refreshOptions() {
        val candidates = currentCandidates()
        for (index in 0 until OPTION_SLOT_COUNT) {
            optionInventory.setStack(index, candidates.getOrNull(index)?.let(ArtifactDisplayStackFactory::alphaCandidateDisplay) ?: ItemStack.EMPTY)
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
