package hifumi.cresora.guide

import hifumi.cresora.CreSoraUtilities
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
import net.minecraft.util.Identifier

/**
 * Server-driven records hub screen. The active sub-tab (discovery / bond / achievement) and page
 * live in the property delegate; entry slots are filled from [GuideService] aggregations and synced
 * to the client. Sub-tab switching and pagination are handled entirely server-side via slot clicks,
 * so the client only forwards clicks and renders the synced slots/properties.
 */
class RecordsScreenHandler(
    syncId: Int,
    private val playerInventory: PlayerInventory
) : ScreenHandler(CreSoraUtilities.RECORDS_SCREEN_HANDLER, syncId) {

    companion object {
        private const val SLOT_COUNT = 18

        const val SUBTAB_DISCOVERY = 0
        const val SUBTAB_BOND = 1
        const val SUBTAB_ACHIEVEMENT = 2

        private const val SUBTAB_DISCOVERY_SLOT = 0
        private const val SUBTAB_BOND_SLOT = 1
        private const val SUBTAB_ACHIEVEMENT_SLOT = 2
        private const val PREV_PAGE_SLOT = 6
        private const val NEXT_PAGE_SLOT = 7
        private const val BACK_SLOT = 8

        val ENTRY_SLOTS = intArrayOf(9, 10, 11, 12, 13, 14)
        const val ENTRIES_PER_PAGE = 6
        const val DETAIL_SLOT = 15

        private const val PROPERTY_SUBTAB = 0
        private const val PROPERTY_PAGE = 1
        private const val PROPERTY_TOTAL_PAGES = 2
        private const val PROPERTY_RATIO_CURRENT = 3
        private const val PROPERTY_RATIO_TOTAL = 4
        private const val PROPERTY_SELECTED_ENTRY = 5
        const val NO_SELECTION = -1
    }

    private val displayInventory: Inventory = object : SimpleInventory(SLOT_COUNT) {}
    private val properties: PropertyDelegate = ArrayPropertyDelegate(6)

    init {
        addProperties(properties)
        properties.set(PROPERTY_SUBTAB, SUBTAB_DISCOVERY)
        properties.set(PROPERTY_PAGE, 0)
        properties.set(PROPERTY_TOTAL_PAGES, 1)
        properties.set(PROPERTY_SELECTED_ENTRY, NO_SELECTION)

        refreshEntries()

        for (index in 0 until SLOT_COUNT) {
            addSlot(object : Slot(displayInventory, index, 0, 0) {
                override fun canInsert(stack: ItemStack): Boolean = false
                override fun canTakeItems(playerEntity: PlayerEntity): Boolean = false
                override fun getBackgroundSprite(): Identifier? = null
            })
        }
    }

    override fun canUse(player: PlayerEntity): Boolean = true

    override fun quickMove(player: PlayerEntity, slotIndex: Int): ItemStack = ItemStack.EMPTY

    override fun sendContentUpdates() {
        refreshEntries()
        super.sendContentUpdates()
    }

    fun activeSubTab(): Int = properties.get(PROPERTY_SUBTAB)
    fun currentPage(): Int = properties.get(PROPERTY_PAGE)
    fun totalPages(): Int = properties.get(PROPERTY_TOTAL_PAGES).coerceAtLeast(1)
    fun ratioCurrent(): Int = properties.get(PROPERTY_RATIO_CURRENT)
    fun ratioTotal(): Int = properties.get(PROPERTY_RATIO_TOTAL)
    fun selectedEntryIndex(): Int = properties.get(PROPERTY_SELECTED_ENTRY)

    override fun onSlotClick(slotIndex: Int, button: Int, actionType: SlotActionType, player: PlayerEntity) {
        if (slotIndex in 0 until SLOT_COUNT) {
            if (actionType == SlotActionType.PICKUP || actionType == SlotActionType.QUICK_MOVE) {
                val serverPlayer = player as? ServerPlayerEntity ?: return
                when (slotIndex) {
                    SUBTAB_DISCOVERY_SLOT -> setSubTab(SUBTAB_DISCOVERY)
                    SUBTAB_BOND_SLOT -> setSubTab(SUBTAB_BOND)
                    SUBTAB_ACHIEVEMENT_SLOT -> setSubTab(SUBTAB_ACHIEVEMENT)
                    PREV_PAGE_SLOT -> changePage(-1)
                    NEXT_PAGE_SLOT -> changePage(1)
                    BACK_SLOT -> {
                        ArtifactUiFlow.openGuide(serverPlayer)
                        return
                    }
                    else -> {
                        // Entry slots toggle selection; absolute index is page * ENTRIES_PER_PAGE + slotOffset.
                        val slotOffset = slotIndex - ENTRY_SLOTS.first()
                        if (slotOffset in 0 until ENTRIES_PER_PAGE) {
                            val absoluteIndex = properties.get(PROPERTY_PAGE) * ENTRIES_PER_PAGE + slotOffset
                            val current = properties.get(PROPERTY_SELECTED_ENTRY)
                            properties.set(
                                PROPERTY_SELECTED_ENTRY,
                                if (current == absoluteIndex) NO_SELECTION else absoluteIndex
                            )
                        }
                    }
                }
                refreshEntries()
            }
            return
        }
        super.onSlotClick(slotIndex, button, actionType, player)
    }

    private fun setSubTab(tab: Int) {
        if (properties.get(PROPERTY_SUBTAB) != tab) {
            properties.set(PROPERTY_SUBTAB, tab)
            properties.set(PROPERTY_PAGE, 0)
            properties.set(PROPERTY_SELECTED_ENTRY, NO_SELECTION)
        }
    }

    private fun changePage(delta: Int) {
        val newPage = (properties.get(PROPERTY_PAGE) + delta).coerceIn(0, totalPages() - 1)
        if (properties.get(PROPERTY_PAGE) != newPage) {
            properties.set(PROPERTY_PAGE, newPage)
            properties.set(PROPERTY_SELECTED_ENTRY, NO_SELECTION)
        }
    }

    private fun refreshEntries() {
        for (index in 0 until SLOT_COUNT) {
            displayInventory.setStack(index, ItemStack.EMPTY)
        }

        val subTab = properties.get(PROPERTY_SUBTAB)
        displayInventory.setStack(SUBTAB_DISCOVERY_SLOT, RecordsDisplayStackFactory.subTabDisplay(SUBTAB_DISCOVERY, subTab == SUBTAB_DISCOVERY))
        displayInventory.setStack(SUBTAB_BOND_SLOT, RecordsDisplayStackFactory.subTabDisplay(SUBTAB_BOND, subTab == SUBTAB_BOND))
        displayInventory.setStack(SUBTAB_ACHIEVEMENT_SLOT, RecordsDisplayStackFactory.subTabDisplay(SUBTAB_ACHIEVEMENT, subTab == SUBTAB_ACHIEVEMENT))
        displayInventory.setStack(BACK_SLOT, RecordsDisplayStackFactory.backDisplay())

        // Entry slots and ratio/page properties are server-authoritative; on the client the synced
        // stacks and properties already reflect the latest server state.
        val player = playerInventory.player as? ServerPlayerEntity ?: return

        // Aggregate the active sub-tab's records once, then derive the entry stacks, the ratio
        // counters, and the detail pane from that single snapshot — each GuideService.getXxxRecords
        // call re-parses persisted player data, so recomputing per consumer would waste work every
        // sync tick the screen is open.
        val view = buildView(player, subTab)
        val entries = view.entries
        val ratioCurrent = view.ratioCurrent
        val ratioTotal = view.ratioTotal

        val totalPages = if (entries.isEmpty()) 1 else (entries.size + ENTRIES_PER_PAGE - 1) / ENTRIES_PER_PAGE
        properties.set(PROPERTY_TOTAL_PAGES, totalPages)
        val page = properties.get(PROPERTY_PAGE).coerceIn(0, totalPages - 1)
        properties.set(PROPERTY_PAGE, page)
        properties.set(PROPERTY_RATIO_CURRENT, ratioCurrent)
        properties.set(PROPERTY_RATIO_TOTAL, ratioTotal)

        // Clamp selection to the current entry list; an out-of-range selection (after a page/tab
        // switch that wasn't routed through setSubTab/changePage, or data shrinkage) is cleared.
        val selection = properties.get(PROPERTY_SELECTED_ENTRY)
        val clampedSelection = if (selection in 0 until entries.size) selection else NO_SELECTION
        if (selection != clampedSelection) {
            properties.set(PROPERTY_SELECTED_ENTRY, clampedSelection)
        }

        val start = page * ENTRIES_PER_PAGE
        for (i in ENTRY_SLOTS.indices) {
            val absoluteIndex = start + i
            val entry = entries.getOrNull(absoluteIndex) ?: ItemStack.EMPTY
            if (absoluteIndex == clampedSelection) {
                displayInventory.setStack(ENTRY_SLOTS[i], RecordsDisplayStackFactory.selectedMarker(entry))
            } else {
                displayInventory.setStack(ENTRY_SLOTS[i], entry)
            }
        }

        // Detail pane: encode the selected record's title + story as LORE on a dedicated synced
        // stack so the client can render the long-form text without a custom packet.
        val detailStack = if (clampedSelection != NO_SELECTION) {
            view.detailAt(clampedSelection)
        } else {
            ItemStack.EMPTY
        }
        displayInventory.setStack(DETAIL_SLOT, detailStack)
    }

    /**
     * One snapshot of the active sub-tab's data: the pre-built entry stacks, the left-page ratio
     * counters, and a detail-stack lookup — all derived from a single read of the persisted records
     * so [refreshEntries] never re-aggregates the same data per consumer.
     */
    private class RecordsView(
        val entries: List<ItemStack>,
        val ratioCurrent: Int,
        val ratioTotal: Int,
        private val detail: (Int) -> ItemStack
    ) {
        fun detailAt(absoluteIndex: Int): ItemStack = detail(absoluteIndex)
    }

    private fun buildView(player: ServerPlayerEntity, subTab: Int): RecordsView {
        return when (subTab) {
            SUBTAB_DISCOVERY -> {
                val records = GuideService.getRegionRecords(player)
                RecordsView(
                    entries = records.map(RecordsDisplayStackFactory::regionEntry),
                    ratioCurrent = records.count { it.discovered },
                    ratioTotal = records.size,
                    detail = { index ->
                        records.getOrNull(index)?.let(RecordsDisplayStackFactory::regionDetail) ?: ItemStack.EMPTY
                    }
                )
            }
            SUBTAB_BOND -> {
                val records = GuideService.getSpiritBondRecords(player)
                RecordsView(
                    entries = records.map(RecordsDisplayStackFactory::spiritEntry),
                    ratioCurrent = records.count { it.encountered },
                    ratioTotal = records.size,
                    detail = { index ->
                        records.getOrNull(index)?.let(RecordsDisplayStackFactory::spiritDetail) ?: ItemStack.EMPTY
                    }
                )
            }
            else -> RecordsView(
                entries = RecordsDisplayStackFactory.achievementEntries(GuideService.getAchievementSummary(player)),
                ratioCurrent = 0,
                ratioTotal = 0, // achievement view has no single ratio
                detail = { ItemStack.EMPTY }
            )
        }
    }
}
