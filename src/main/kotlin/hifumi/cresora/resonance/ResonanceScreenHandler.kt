package hifumi.cresora.resonance
import hifumi.cresora.CreSoraUtilities
import hifumi.cresora.ScreenSyncSupport
import hifumi.cresora.equipment.ArtifactDisplayStackFactory
import hifumi.cresora.equipment.ArtifactUiFlow
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

class ResonanceScreenHandler(
    syncId: Int,
    private val playerInventory: PlayerInventory
) : ScreenHandler(CreSoraUtilities.RESONANCE_SCREEN_HANDLER, syncId) {

    companion object {
        private const val ROWS = 1
        private const val SLOT_COUNT = 9
        private const val FEATURED_SELECT_SLOT = 0
        private const val LIMITED_SLOT = 2
        private const val STANDARD_SLOT = 6
        private const val PROPERTY_LIMITED_PITY = 0
        private const val PROPERTY_STANDARD_PULLS = 2
        private const val PROPERTY_LIMITED_CURRENCY = 4
        private const val PROPERTY_STANDARD_CURRENCY = 6
        private const val PROPERTY_ARPEGGIO_READY = 8
    }

    private val displayInventory: Inventory = object : SimpleInventory(SLOT_COUNT) {}
    private val properties: PropertyDelegate = ArrayPropertyDelegate(9)

    private val limitedBanner = ResonanceContentRegistry.banner("limited_lakeside")
    private val standardBanner = ResonanceContentRegistry.banner("standard_ensemble")

    init {
        addProperties(properties)
        refreshOffers()
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
        refreshOffers()
        super.sendContentUpdates()
    }

    override fun quickMove(player: PlayerEntity, slotIndex: Int): ItemStack = ItemStack.EMPTY

    override fun onSlotClick(slotIndex: Int, button: Int, actionType: SlotActionType, player: PlayerEntity) {
        if (slotIndex in 0 until SLOT_COUNT) {
            if (actionType == SlotActionType.PICKUP || actionType == SlotActionType.QUICK_MOVE) {
                val serverPlayer = player as? ServerPlayerEntity ?: return
                when (slotIndex) {
                    FEATURED_SELECT_SLOT -> ArtifactUiFlow.openResonanceFeaturedSelection(serverPlayer)
                    LIMITED_SLOT -> {
                        if (ResonanceService.getSelectedFeaturedWeaponId(serverPlayer) == null) {
                            player.sendMessage(Text.translatable("screen.cresora.resonance.featured_not_selected"), false)
                            ArtifactUiFlow.openResonanceFeaturedSelection(serverPlayer)
                        } else {
                            attemptPull(serverPlayer, limitedBanner)
                        }
                    }
                    STANDARD_SLOT -> attemptPull(serverPlayer, standardBanner)
                    8 -> ArtifactUiFlow.openGuide(serverPlayer)
                }
            }
            return
        }
        super.onSlotClick(slotIndex, button, actionType, player)
    }

    fun limitedPity(): Int = ScreenSyncSupport.readInt(properties, PROPERTY_LIMITED_PITY)

    fun standardPulls(): Int = ScreenSyncSupport.readInt(properties, PROPERTY_STANDARD_PULLS)

    fun limitedCurrency(): Int = ScreenSyncSupport.readInt(properties, PROPERTY_LIMITED_CURRENCY)

    fun standardCurrency(): Int = ScreenSyncSupport.readInt(properties, PROPERTY_STANDARD_CURRENCY)

    fun arpeggioReady(): Boolean = properties.get(PROPERTY_ARPEGGIO_READY) > 0

    private fun attemptPull(player: ServerPlayerEntity, banner: ResonanceBannerDefinition) {
        when (val outcome = ResonanceService.pull(player, banner)) {
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
                ArtifactUiFlow.openResonanceResult(player, result)
            }

            ResonanceService.PullOutcome.NotEnoughCurrency -> {
                player.sendMessage(Text.translatable("screen.cresora.resonance.not_enough_currency", banner.cost), false)
            }

            ResonanceService.PullOutcome.FeaturedNotSelected -> {
                player.sendMessage(Text.translatable("screen.cresora.resonance.featured_not_selected"), false)
                ArtifactUiFlow.openResonanceFeaturedSelection(player)
            }

            ResonanceService.PullOutcome.NoFiveStarWeaponsAvailable -> {
                player.sendMessage(Text.translatable("screen.cresora.resonance.no_five_star_available"), false)
            }
        }
    }

    private fun refreshOffers() {
        val player = playerInventory.player as? ServerPlayerEntity ?: return
        val progress = ResonanceService.getProgress(player)
        ScreenSyncSupport.writeInt(properties, PROPERTY_LIMITED_PITY, progress.limitedPityPulls)
        ScreenSyncSupport.writeInt(properties, PROPERTY_STANDARD_PULLS, progress.standardPulls)
        ScreenSyncSupport.writeInt(properties, PROPERTY_LIMITED_CURRENCY, ResonanceService.currencyCount(player, limitedBanner))
        ScreenSyncSupport.writeInt(properties, PROPERTY_STANDARD_CURRENCY, ResonanceService.currencyCount(player, standardBanner))
        properties.set(PROPERTY_ARPEGGIO_READY, if (progress.arpeggioReady) 1 else 0)

        for (index in 0 until SLOT_COUNT) {
            displayInventory.setStack(index, ArtifactDisplayStackFactory.fillerDisplay())
        }
        displayInventory.setStack(
            FEATURED_SELECT_SLOT,
            ArtifactDisplayStackFactory.resonanceFeaturedSelectDisplay(progress.selectedFeaturedWeaponId)
        )
        displayInventory.setStack(
            LIMITED_SLOT,
            ArtifactDisplayStackFactory.resonanceBannerDisplay(
                limitedBanner,
                progress.limitedPityPulls,
                progress.arpeggioReady,
                progress.selectedFeaturedWeaponId
            )
        )
        displayInventory.setStack(
            STANDARD_SLOT,
            ArtifactDisplayStackFactory.resonanceBannerDisplay(standardBanner, progress.standardPulls, false, null)
        )
        displayInventory.setStack(8, hifumi.cresora.guide.GuideDisplayStackFactory.backDisplay())
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
