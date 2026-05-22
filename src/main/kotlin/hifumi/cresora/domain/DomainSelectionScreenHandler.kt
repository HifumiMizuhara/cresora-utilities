package hifumi.cresora.domain
import hifumi.cresora.CreSoraUtilities
import hifumi.cresora.ScreenSyncSupport
import hifumi.cresora.adventurerank.AdventureRankService
import hifumi.cresora.credits.CreditsService
import hifumi.cresora.equipment.ArtifactDisplayStackFactory
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
import java.util.Locale

class DomainSelectionScreenHandler(
    syncId: Int,
    private val playerInventory: PlayerInventory
) : ScreenHandler(CreSoraUtilities.DOMAIN_SELECTION_SCREEN_HANDLER, syncId) {

    companion object {
        private const val ROWS = 1
        private const val DOMAIN_SLOT_COUNT = 9
        private const val PROPERTY_AVAILABLE_CREDITS_LOW = 0
        private const val PROPERTY_PLAYER_RANK = 2
    }

    private val domainDefinitions = DomainContentRegistry.domains().take(DOMAIN_SLOT_COUNT)
    private val domainInventory: Inventory = object : SimpleInventory(DOMAIN_SLOT_COUNT) {}
    private val properties: PropertyDelegate = ArrayPropertyDelegate(3)

    init {
        addProperties(properties)
        refreshProperties()
        refreshEntries()

        for (index in 0 until DOMAIN_SLOT_COUNT) {
            addSlot(object : Slot(domainInventory, index, 8 + index * 18, 18) {
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
        if (slotIndex in 0 until DOMAIN_SLOT_COUNT) {
            if (actionType == SlotActionType.PICKUP || actionType == SlotActionType.QUICK_MOVE) {
                attemptStart(slotIndex, player)
            }
            return
        }
        super.onSlotClick(slotIndex, button, actionType, player)
    }

    fun currentCredits(): Int = ScreenSyncSupport.readInt(properties, PROPERTY_AVAILABLE_CREDITS_LOW)

    fun currentRank(): Int = properties.get(PROPERTY_PLAYER_RANK)

    fun domainForSlot(slotIndex: Int): DomainDefinition? = domainDefinitions.getOrNull(slotIndex)

    fun recommendedBandText(slotIndex: Int): Text {
        val domain = domainForSlot(slotIndex) ?: return Text.empty()
        val band = DomainCombatProfile.recommendedBand(maxOf(currentRank(), domain.unlockRank))
        return Text.translatable("screen.cresora.domain.band", band.first, band.second)
    }

    fun mainRewardText(slotIndex: Int): Text {
        val domain = domainForSlot(slotIndex) ?: return Text.empty()
        return DomainDisplayStackFactory.rewardLabel(DomainRewardProfileRegistry.requireProfile(domain.rewardProfileId))
    }

    fun entryCostText(slotIndex: Int): String {
        val domain = domainForSlot(slotIndex) ?: return "0"
        return String.format(Locale.ROOT, "%,d", domain.entryCostCsc)
    }

    private fun attemptStart(slotIndex: Int, player: PlayerEntity) {
        val serverPlayer = player as? ServerPlayerEntity ?: return
        val domain = domainDefinitions.getOrNull(slotIndex) ?: return
        val result = DomainService.startSession(serverPlayer, domain.id)
        if (result.success) {
            player.closeHandledScreen()
        } else {
            player.sendMessage(Text.translatable(result.translationKey, *result.args.toTypedArray()), false)
            sendContentUpdates()
        }
    }

    private fun refreshEntries() {
        for (index in 0 until DOMAIN_SLOT_COUNT) {
            domainInventory.setStack(index, domainDefinitions.getOrNull(index)?.let(DomainDisplayStackFactory::domainDisplay) ?: ArtifactDisplayStackFactory.fillerDisplay())
        }
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
