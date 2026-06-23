package hifumi.cresora.guide

import net.minecraft.component.DataComponentTypes
import net.minecraft.component.type.LoreComponent
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.text.Text

/**
 * Builds the synced display stacks for the records hub screen. Each stack carries its label as
 * CUSTOM_NAME and its detail lines as LORE so the client can render them as text without a custom
 * packet (mirrors [GuideDisplayStackFactory]).
 */
object RecordsDisplayStackFactory {
    fun backDisplay(): ItemStack =
        ItemStack(Items.ARROW).apply {
            set(DataComponentTypes.CUSTOM_NAME, Text.translatable("screen.cresora.guide.back"))
        }

    fun subTabDisplay(subTab: Int, active: Boolean): ItemStack {
        val (item, labelKey) = when (subTab) {
            RecordsScreenHandler.SUBTAB_DISCOVERY -> Items.FILLED_MAP to "screen.cresora.records.tab.discovery"
            RecordsScreenHandler.SUBTAB_BOND -> Items.NOTE_BLOCK to "screen.cresora.records.tab.bond"
            else -> Items.NETHER_STAR to "screen.cresora.records.tab.achievement"
        }
        return ItemStack(item).apply {
            set(DataComponentTypes.CUSTOM_NAME, Text.translatable(labelKey))
            set(
                DataComponentTypes.LORE,
                LoreComponent(
                    listOf(
                        Text.translatable(
                            if (active) "screen.cresora.records.tab.active" else "screen.cresora.records.tab.inactive"
                        )
                    )
                )
            )
        }
    }

    fun regionEntry(record: GuideRegionRecord): ItemStack {
        return if (record.discovered) {
            ItemStack(Items.FILLED_MAP).apply {
                set(DataComponentTypes.CUSTOM_NAME, Text.translatable(record.nameKey))
                set(
                    DataComponentTypes.LORE,
                    LoreComponent(
                        listOf(
                            Text.translatable(record.descriptionKey),
                            Text.translatable("screen.cresora.records.discovered")
                        )
                    )
                )
            }
        } else {
            ItemStack(Items.MAP).apply {
                set(DataComponentTypes.CUSTOM_NAME, Text.translatable("screen.cresora.records.undiscovered"))
                set(
                    DataComponentTypes.LORE,
                    LoreComponent(
                        listOf(Text.translatable("message.cresora.region.locked", record.unlockRank))
                    )
                )
            }
        }
    }

    fun spiritEntry(record: GuideSpiritBondRecord): ItemStack {
        if (!record.encountered) {
            return ItemStack(Items.GRAY_DYE).apply {
                set(DataComponentTypes.CUSTOM_NAME, Text.translatable("screen.cresora.records.bond.unmet"))
            }
        }
        val lore = mutableListOf<Text>(
            Text.translatable("screen.cresora.records.bond.stage", record.stage, record.maxStage)
        )
        val pointsForNextStage = record.pointsForNextStage
        if (pointsForNextStage != null) {
            lore.add(Text.translatable("screen.cresora.records.bond.next", pointsForNextStage))
        } else {
            lore.add(Text.translatable("screen.cresora.records.bond.max"))
        }
        return ItemStack(Items.AMETHYST_SHARD).apply {
            set(DataComponentTypes.CUSTOM_NAME, Text.translatable(record.nameKey))
            set(DataComponentTypes.LORE, LoreComponent(lore))
        }
    }

    /**
     * Wraps an entry stack with a "selected" indicator so the currently focused card is visible
     * in the entry list itself, not only in the detail pane.
     */
    fun selectedMarker(entry: ItemStack): ItemStack {
        val name = entry.get(DataComponentTypes.CUSTOM_NAME) ?: Text.empty()
        return entry.apply {
            set(
                DataComponentTypes.CUSTOM_NAME,
                Text.literal("▶ ").formatted(net.minecraft.util.Formatting.AQUA).append(name)
            )
        }
    }

    /** Long-form detail stack for the left pane: region description + discovery status. */
    fun regionDetail(record: GuideRegionRecord): ItemStack {
        val item = if (record.discovered) Items.FILLED_MAP else Items.MAP
        val lore = if (record.discovered) {
            listOf(
                Text.translatable(record.descriptionKey),
                Text.translatable("screen.cresora.records.discovered")
            )
        } else {
            listOf(
                Text.translatable("message.cresora.region.locked", record.unlockRank),
                Text.translatable("commands.cresora.guide.discovery.undiscovered")
            )
        }
        return ItemStack(item).apply {
            set(DataComponentTypes.CUSTOM_NAME, Text.translatable(record.nameKey))
            set(DataComponentTypes.LORE, LoreComponent(lore))
        }
    }

    /**
     * Long-form detail stack for the left pane: bond stage + current stage title + story.
     * The story paragraph is the primary consumer of [GuideSpiritBondRecord.currentStageStoryKey],
     * which the compact entry list cannot display.
     */
    fun spiritDetail(record: GuideSpiritBondRecord): ItemStack {
        if (!record.encountered) {
            return ItemStack(Items.GRAY_DYE).apply {
                set(DataComponentTypes.CUSTOM_NAME, Text.translatable("screen.cresora.records.bond.unmet"))
            }
        }
        val lore = mutableListOf<Text>(
            Text.translatable("screen.cresora.records.bond.stage", record.stage, record.maxStage)
        )
        val pointsForNextStage = record.pointsForNextStage
        if (pointsForNextStage != null) {
            lore.add(Text.translatable("screen.cresora.records.bond.next", pointsForNextStage))
        } else {
            lore.add(Text.translatable("screen.cresora.records.bond.max"))
        }
        record.currentStageTitleKey?.let { titleKey ->
            lore.add(Text.translatable("screen.cresora.records.bond.current_title", Text.translatable(titleKey)))
        }
        record.currentStageStoryKey?.let { storyKey ->
            lore.add(Text.translatable("screen.cresora.records.bond.current_story", Text.translatable(storyKey)))
        }
        return ItemStack(Items.AMETHYST_SHARD).apply {
            set(DataComponentTypes.CUSTOM_NAME, Text.translatable(record.nameKey))
            set(DataComponentTypes.LORE, LoreComponent(lore))
        }
    }

    fun achievementEntries(summary: GuideAchievementSummary): List<ItemStack> {
        return listOf(
            summaryEntry(Items.WRITTEN_BOOK, "screen.cresora.records.summary.chapters", Text.literal(summary.guideChaptersCompleted.toString())),
            summaryEntry(Items.PAPER, "screen.cresora.records.summary.tasks", Text.literal(summary.guideTasksClaimed.toString())),
            summaryEntry(Items.WRITABLE_BOOK, "screen.cresora.records.summary.story", Text.literal(summary.storyStagesCleared.toString())),
            summaryEntry(Items.FILLED_MAP, "screen.cresora.records.summary.regions", Text.literal("${summary.regionsDiscovered} / ${summary.regionsTotal}")),
            summaryEntry(Items.AMETHYST_SHARD, "screen.cresora.records.summary.spirits", Text.literal("${summary.spiritsEncountered} / ${summary.spiritsTotal}")),
            summaryEntry(Items.NETHER_STAR, "screen.cresora.records.summary.maxbond", Text.literal(summary.spiritsMaxBonded.toString()))
        )
    }

    private fun summaryEntry(item: Item, titleKey: String, value: Text): ItemStack {
        return ItemStack(item).apply {
            set(DataComponentTypes.CUSTOM_NAME, Text.translatable(titleKey))
            set(DataComponentTypes.LORE, LoreComponent(listOf(value)))
        }
    }
}
