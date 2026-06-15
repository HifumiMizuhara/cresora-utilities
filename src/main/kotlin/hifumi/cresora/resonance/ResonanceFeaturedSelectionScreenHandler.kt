package hifumi.cresora.resonance
import hifumi.cresora.CreSoraUtilities
import hifumi.cresora.equipment.ArtifactDisplayStackFactory
import hifumi.cresora.equipment.ArtifactUiFlow
import hifumi.cresora.weapon.WeaponContentRegistry
import hifumi.cresora.weapon.WeaponRarity
import hifumi.cresora.weapon.WeaponStackSupport
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

class ResonanceFeaturedSelectionScreenHandler(
    syncId: Int,
    private val playerInventory: PlayerInventory
) : ScreenHandler(CreSoraUtilities.RESONANCE_FEATURED_SELECTION_SCREEN_HANDLER, syncId) {

    companion object {
        const val ROWS = 4
        const val SLOT_COUNT = ROWS * 9
        // 下段右端を「戻る」スロットに固定
        private val BACK_SLOT = SLOT_COUNT - 1
    }

    private val displayInventory: Inventory = object : SimpleInventory(SLOT_COUNT) {}
    private val selectableIds: List<String> = ResonanceService.selectableFeaturedWeaponIds()

    init {
        refreshDisplay()
        for (row in 0 until ROWS) {
            for (column in 0 until 9) {
                val index = row * 9 + column
                addSlot(object : Slot(displayInventory, index, 8 + column * 18, 18 + row * 18) {
                    override fun canInsert(stack: ItemStack): Boolean = false

                    override fun canTakeItems(playerEntity: PlayerEntity): Boolean = false

                    override fun getBackgroundSprite(): Identifier? = null
                })
            }
        }
        addPlayerSlots(playerInventory)
    }

    override fun canUse(player: PlayerEntity): Boolean = true

    override fun quickMove(player: PlayerEntity, slotIndex: Int): ItemStack = ItemStack.EMPTY

    override fun sendContentUpdates() {
        refreshDisplay()
        super.sendContentUpdates()
    }

    override fun onSlotClick(slotIndex: Int, button: Int, actionType: SlotActionType, player: PlayerEntity) {
        if (slotIndex in 0 until SLOT_COUNT) {
            if (actionType == SlotActionType.PICKUP || actionType == SlotActionType.QUICK_MOVE) {
                val serverPlayer = player as? ServerPlayerEntity ?: return
                if (slotIndex == BACK_SLOT) {
                    ArtifactUiFlow.openResonance(serverPlayer)
                    return
                }
                if (slotIndex < selectableIds.size) {
                    val targetId = selectableIds[slotIndex]
                    val current = ResonanceService.getSelectedFeaturedWeaponId(serverPlayer)
                    if (current == targetId) {
                        serverPlayer.sendMessage(
                            Text.translatable(
                                "screen.cresora.resonance.featured_already_selected",
                                Text.translatable("item.cresora-utilities.$targetId")
                            ),
                            false
                        )
                    } else {
                        val ok = ResonanceService.setSelectedFeaturedWeaponId(serverPlayer, targetId)
                        if (ok) {
                            serverPlayer.sendMessage(
                                Text.translatable(
                                    "screen.cresora.resonance.featured_selected",
                                    Text.translatable("item.cresora-utilities.$targetId")
                                ),
                                false
                            )
                        }
                    }
                    ArtifactUiFlow.openResonance(serverPlayer)
                }
            }
            return
        }
        super.onSlotClick(slotIndex, button, actionType, player)
    }

    private fun refreshDisplay() {
        val player = playerInventory.player as? ServerPlayerEntity
        val currentSelection = player?.let(ResonanceService::getSelectedFeaturedWeaponId)
        for (index in 0 until SLOT_COUNT) {
            displayInventory.setStack(index, ArtifactDisplayStackFactory.fillerDisplay())
        }
        selectableIds.forEachIndexed { index, weaponId ->
            if (index >= BACK_SLOT) return@forEachIndexed
            val definition = WeaponContentRegistry.requireWeapon(weaponId)
            val stack = WeaponStackSupport.createWeaponStack(definition, WeaponRarity.FIVE_STAR, 1, 1)
            displayInventory.setStack(
                index,
                ArtifactDisplayStackFactory.resonanceFeaturedOptionDisplay(
                    stack,
                    weaponId == currentSelection
                )
            )
        }
        displayInventory.setStack(BACK_SLOT, ArtifactDisplayStackFactory.resonanceReturnButtonDisplay())
    }

    private fun addPlayerSlots(playerInventory: PlayerInventory) {
        val inventoryStartY = 18 + ROWS * 18 + 14
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
