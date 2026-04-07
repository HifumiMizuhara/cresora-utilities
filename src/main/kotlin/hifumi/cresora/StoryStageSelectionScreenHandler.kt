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

class StoryStageSelectionScreenHandler(
    syncId: Int,
    private val playerInventory: PlayerInventory
) : ScreenHandler(CreSoraUtilities.STORY_STAGE_SELECTION_SCREEN_HANDLER, syncId) {

    companion object {
        private const val ROWS = 1
        private const val SLOT_COUNT = 9
        private const val PROPERTY_PLAYER_RANK = 0
    }

    private val displayInventory: Inventory = object : SimpleInventory(SLOT_COUNT) {}
    private val properties: PropertyDelegate = ArrayPropertyDelegate(1)
    private var chapterGroup: String = ""
    private var chapters: List<StoryChapterDefinition> = emptyList()

    constructor(
        syncId: Int,
        playerInventory: PlayerInventory,
        chapterGroup: String
    ) : this(syncId, playerInventory) {
        this.chapterGroup = chapterGroup
        this.chapters = StoryContentRegistry.chaptersForGroup(chapterGroup).take(SLOT_COUNT)
        refreshEntries()
    }

    init {
        addProperties(properties)
        refreshProperties()
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
                attemptStart(slotIndex, player)
            }
            return
        }
        super.onSlotClick(slotIndex, button, actionType, player)
    }

    fun currentRank(): Int = properties.get(PROPERTY_PLAYER_RANK)

    private fun attemptStart(slotIndex: Int, player: PlayerEntity) {
        val serverPlayer = player as? ServerPlayerEntity ?: return
        val chapter = chapters.getOrNull(slotIndex) ?: return
        val result = StoryService.startSession(serverPlayer, chapter.id)
        if (result.success) {
            player.closeHandledScreen()
        } else {
            player.sendMessage(Text.translatable(result.translationKey, *result.args.toTypedArray()), false)
            sendContentUpdates()
        }
    }

    private fun refreshEntries() {
        val serverPlayer = playerInventory.player as? ServerPlayerEntity
        for (index in 0 until SLOT_COUNT) {
            displayInventory.setStack(
                index,
                chapters.getOrNull(index)?.let { ArtifactDisplayStackFactory.storyChapterEntryDisplay(it, serverPlayer) } ?: ArtifactDisplayStackFactory.fillerDisplay()
            )
        }
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
