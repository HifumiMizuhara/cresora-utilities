package hifumi.cresora.guide

import hifumi.cresora.equipment.ArtifactBookScreenBase
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.widget.ButtonWidget
import net.minecraft.component.DataComponentTypes
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.item.ItemStack
import net.minecraft.screen.slot.SlotActionType
import net.minecraft.text.Text

class RecordsScreen(
    handler: RecordsScreenHandler,
    inventory: PlayerInventory,
    title: Text
) : ArtifactBookScreenBase<RecordsScreenHandler>(handler, inventory, title) {

    private val subTabButtons = arrayOfNulls<ButtonWidget>(3)
    private lateinit var prevButton: ButtonWidget
    private lateinit var nextButton: ButtonWidget
    private lateinit var backButton: ButtonWidget

    private val entrySlots = RecordsScreenHandler.ENTRY_SLOTS

    companion object {
        private const val ROW_TOP = 28
        private const val ROW_HEIGHT = 28
        private val SUBTAB_LABEL_KEYS = arrayOf(
            "screen.cresora.records.tab.discovery",
            "screen.cresora.records.tab.bond",
            "screen.cresora.records.tab.achievement"
        )
    }

    override fun getActiveTab(): Int = 6

    override fun init() {
        super.init()
        val mc = client

        // Hide the selector/back display slots; they are driven through buttons instead.
        for (slot in handler.slots) {
            slot.x = -2000
            slot.y = -2000
        }
        // Position entry icon slots down the right page.
        for (i in entrySlots.indices) {
            val slot = handler.slots[entrySlots[i]]
            slot.x = 205
            slot.y = ROW_TOP + i * ROW_HEIGHT + 4
        }

        // Left-page sub-tab buttons.
        for (i in 0 until 3) {
            val idx = i
            val btn = ButtonWidget.builder(Text.translatable(SUBTAB_LABEL_KEYS[i])) {
                mc?.interactionManager?.clickSlot(handler.syncId, idx, 0, SlotActionType.PICKUP, mc.player)
            }.dimensions(x + 25, y + 45 + i * 26, 140, 20).build()
            subTabButtons[i] = btn
            addDrawableChild(btn)
        }

        prevButton = ButtonWidget.builder(Text.literal("<")) {
            mc?.interactionManager?.clickSlot(handler.syncId, 6, 0, SlotActionType.PICKUP, mc.player)
        }.dimensions(x + 205, y + 192, 18, 16).build()

        nextButton = ButtonWidget.builder(Text.literal(">")) {
            mc?.interactionManager?.clickSlot(handler.syncId, 7, 0, SlotActionType.PICKUP, mc.player)
        }.dimensions(x + 352, y + 192, 18, 16).build()

        backButton = ButtonWidget.builder(Text.translatable("screen.cresora.guide.back")) {
            mc?.interactionManager?.clickSlot(handler.syncId, 8, 0, SlotActionType.PICKUP, mc.player)
        }.dimensions(x + 25, y + 175, 140, 20).build()

        addDrawableChild(prevButton)
        addDrawableChild(nextButton)
        addDrawableChild(backButton)

        updateButtonStates()
    }

    private fun updateButtonStates() {
        val active = handler.activeSubTab()
        for (i in 0 until 3) {
            subTabButtons[i]?.active = (i != active)
        }
        val totalPages = handler.totalPages()
        val page = handler.currentPage()
        prevButton.visible = totalPages > 1
        nextButton.visible = totalPages > 1
        prevButton.active = page > 0
        nextButton.active = page < totalPages - 1
    }

    override fun handledScreenTick() {
        super.handledScreenTick()
        updateButtonStates()
    }

    private fun drawCenteredText(context: DrawContext, text: Text, centerX: Int, top: Int, color: Int) {
        val width = textRenderer.getWidth(text)
        context.drawText(textRenderer, text, centerX - width / 2, top, color, false)
    }

    override fun drawBackground(context: DrawContext, delta: Float, mouseX: Int, mouseY: Int) {
        super.drawBackground(context, delta, mouseX, mouseY)

        // Right-page entry cards + icon slot frames.
        for (i in entrySlots.indices) {
            val stack = handler.slots[entrySlots[i]].stack
            if (stack.isEmpty) continue
            val cardTop = y + ROW_TOP + i * ROW_HEIGHT
            context.fill(x + 200, cardTop, x + 365, cardTop + ROW_HEIGHT - 3, 0xFFEADFCE.toInt())
            context.fill(x + 200, cardTop, x + 365, cardTop + 1, 0xFFD2C7B8.toInt())
            context.fill(x + 200, cardTop + ROW_HEIGHT - 4, x + 365, cardTop + ROW_HEIGHT - 3, 0xFFD2C7B8.toInt())
            drawSlotFrame(context, x + 205, cardTop + 4)
        }
    }

    override fun drawForeground(context: DrawContext, mouseX: Int, mouseY: Int) {
        super.drawForeground(context, mouseX, mouseY)

        // Left page: active sub-tab title.
        val active = handler.activeSubTab().coerceIn(0, 2)
        drawCenteredText(context, Text.translatable(SUBTAB_LABEL_KEYS[active]), 97, 18, 0xFF3C3024.toInt())

        // Left page: detail pane when an entry is selected, otherwise the aggregate ratio.
        val detailStack = handler.slots[RecordsScreenHandler.DETAIL_SLOT].stack
        if (!detailStack.isEmpty) {
            drawDetailPane(context, detailStack)
        } else if (handler.ratioTotal() > 0) {
            drawCenteredText(
                context,
                Text.literal("${handler.ratioCurrent()} / ${handler.ratioTotal()}"),
                97,
                132,
                0xFF5C9E32.toInt()
            )
        }

        // Right page: entry name + up to two detail lines.
        for (i in entrySlots.indices) {
            val stack = handler.slots[entrySlots[i]].stack
            if (stack.isEmpty) continue
            val rowTop = ROW_TOP + i * ROW_HEIGHT
            context.drawText(textRenderer, stack.name, 227, rowTop + 2, 0xFF3C3024.toInt(), false)
            val lore = stack.get(DataComponentTypes.LORE)?.lines ?: emptyList()
            if (lore.isNotEmpty()) {
                context.drawText(textRenderer, lore[0], 227, rowTop + 12, 0xFF5F503D.toInt(), false)
            }
            if (lore.size >= 2) {
                context.drawText(textRenderer, lore[1], 227, rowTop + 21, 0xFF83715D.toInt(), false)
            }
        }

        // Page indicator under the right page.
        if (handler.totalPages() > 1) {
            drawCenteredText(
                context,
                Text.translatable("screen.cresora.records.page", handler.currentPage() + 1, handler.totalPages()),
                282,
                196,
                0xFF3C3024.toInt()
            )
        }
    }

    /**
     * Renders the selected entry's long-form detail on the left page. Lines [0] and [1] are the
     * compact status (stage + next/max); subsequent lines are the title label and the story
     * paragraph, which is wrapped to fit the left-page width.
     */
    private fun drawDetailPane(context: DrawContext, detailStack: ItemStack) {
        val leftX = 25
        val paneWidth = 140
        var rowY = 128

        // Detail header (the entry name).
        context.drawText(textRenderer, detailStack.name, leftX, rowY, 0xFF3C3024.toInt(), false)
        rowY += 12

        val lore = detailStack.get(DataComponentTypes.LORE)?.lines ?: emptyList()
        // Status lines (stage, next/max) render single-line.
        for (i in 0..minOf(1, lore.lastIndex)) {
            context.drawText(textRenderer, lore[i], leftX, rowY, 0xFF5F503D.toInt(), false)
            rowY += 10
        }
        // Title label (lore index 2 if present).
        if (lore.size >= 3) {
            context.drawText(textRenderer, lore[2], leftX, rowY, 0xFF355E73.toInt(), false)
            rowY += 10
        }
        // Story paragraph (lore index 3 if present): wrap to pane width.
        if (lore.size >= 4) {
            for (line in textRenderer.wrapLines(lore[3], paneWidth)) {
                context.drawText(textRenderer, line, leftX, rowY, 0xFF83715D.toInt(), false)
                rowY += 9
            }
        }
    }

    override fun drawMouseoverTooltip(context: DrawContext, x: Int, y: Int) {
        // Entry details render inline; item tooltips would obscure the book layout.
    }
}
