package hifumi.cresora.resonance
import hifumi.cresora.CreSoraUtilities
import hifumi.cresora.equipment.ArtifactDisplayStackFactory
import hifumi.cresora.equipment.ArtifactUiFlow
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

class ResonanceResultScreenHandler(
    syncId: Int,
    private val playerInventory: PlayerInventory
) : ScreenHandler(CreSoraUtilities.RESONANCE_RESULT_SCREEN_HANDLER, syncId) {

    companion object {
        private const val ROWS = 1
        private const val SLOT_COUNT = 9
        private const val RETURN_SLOT = 2
        private const val RESULT_SLOT = 4
        private const val REPULL_SLOT = 6
    }

    private val displayInventory: Inventory = object : SimpleInventory(SLOT_COUNT) {}
    private var bannerId: String? = null

    constructor(
        syncId: Int,
        playerInventory: PlayerInventory,
        result: ResonanceService.PullResult
    ) : this(syncId, playerInventory) {
        bannerId = result.banner.id
        refreshDisplay(result)
    }

    init {
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

    override fun quickMove(player: PlayerEntity, slotIndex: Int): ItemStack = ItemStack.EMPTY

    override fun onSlotClick(slotIndex: Int, button: Int, actionType: SlotActionType, player: PlayerEntity) {
        if (slotIndex in 0 until SLOT_COUNT) {
            if (actionType == SlotActionType.PICKUP || actionType == SlotActionType.QUICK_MOVE) {
                when (slotIndex) {
                    RETURN_SLOT -> {
                        val serverPlayer = player as? ServerPlayerEntity ?: return
                        ArtifactUiFlow.openResonance(serverPlayer)
                    }

                    REPULL_SLOT -> repull(player)
                }
            }
            return
        }
        super.onSlotClick(slotIndex, button, actionType, player)
    }

    private fun repull(player: PlayerEntity) {
        val serverPlayer = player as? ServerPlayerEntity ?: return
        val banner = bannerId?.let(ResonanceContentRegistry::banner) ?: return
        when (val outcome = ResonanceService.pull(serverPlayer, banner)) {
            is ResonanceService.PullOutcome.Success -> {
                val result = outcome.result
                player.sendMessage(
                    Text.translatable(
                        "screen.cresora.resonance.pull_success",
                        Text.translatable(result.banner.translationKey),
                        Text.translatable(result.pulledWeapon.item.translationKey),
                        Text.translatable(result.rarity.translationKey())
                    ),
                    false
                )
                bannerId = result.banner.id
                refreshDisplay(result)
                sendContentUpdates()
            }

            ResonanceService.PullOutcome.NotEnoughCurrency -> {
                player.sendMessage(Text.translatable("screen.cresora.resonance.not_enough_currency", banner.cost), false)
            }

            ResonanceService.PullOutcome.FeaturedNotSelected -> {
                player.sendMessage(Text.translatable("screen.cresora.resonance.featured_not_selected"), false)
                ArtifactUiFlow.openResonanceFeaturedSelection(serverPlayer)
            }

            ResonanceService.PullOutcome.NoFiveStarWeaponsAvailable -> {
                player.sendMessage(Text.translatable("screen.cresora.resonance.no_five_star_available"), false)
            }
        }
    }

    private fun refreshDisplay(result: ResonanceService.PullResult) {
        for (index in 0 until SLOT_COUNT) {
            displayInventory.setStack(index, ArtifactDisplayStackFactory.fillerDisplay())
        }
        displayInventory.setStack(RETURN_SLOT, ArtifactDisplayStackFactory.resonanceReturnButtonDisplay())
        displayInventory.setStack(RESULT_SLOT, ArtifactDisplayStackFactory.resonanceResultDisplay(result))
        displayInventory.setStack(
            REPULL_SLOT,
            ArtifactDisplayStackFactory.resonanceRepullButtonDisplay(
                result.banner,
                result.banner.cost <= ResonanceService.currencyCount(playerInventory.player as ServerPlayerEntity, result.banner)
            )
        )
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
