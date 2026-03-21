package hifumi.cresora

import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.client.gui.widget.ButtonWidget
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.text.Text
import kotlin.math.roundToInt

class UpgradeScreen(
    handler: UpgradeScreenHandler,
    inventory: PlayerInventory,
    title: Text
) : HandledScreen<UpgradeScreenHandler>(handler, inventory, title) {

    companion object {
        private const val PANEL_WIDTH = 176
        private const val PANEL_HEIGHT = 166
        private const val TITLE_COLOR = 0xFF1F1710.toInt()
        private const val LABEL_COLOR = 0xFF5F503D.toInt()
        private const val VALUE_COLOR = 0xFF241C14.toInt()
        private const val MUTED_COLOR = 0xFF7A6650.toInt()
        private const val READY_COLOR = 0xFF1F6A52.toInt()
        private const val ERROR_COLOR = 0xFF9B332E.toInt()
        private const val SYMBOL_COLOR = 0xFF8B6A34.toInt()
        private const val FRAME_SHADOW = 0xFF2E241B.toInt()
        private const val FRAME_DARK = 0xFF6A4E31.toInt()
        private const val FRAME_LIGHT = 0xFFF5E9CF.toInt()
        private const val BODY_FILL = 0xFFE3D1AF.toInt()
        private const val PANEL_FILL = 0xFFF0E2C2.toInt()
        private const val PANEL_SHADOW = 0xFFC8AF82.toInt()
        private const val HEADER_DARK = 0xFF27423C.toInt()
        private const val HEADER_LIGHT = 0xFF5C8C83.toInt()
        private const val LEFT_ACCENT = 0xFFB68A47.toInt()
        private const val RIGHT_ACCENT = 0xFF5E877E.toInt()
        private const val STATUS_BG = 0xFFECDCB8.toInt()
        private const val STATUS_BORDER = 0xFF9D7C4D.toInt()
        private const val SLOT_FILL = 0xFF221810.toInt()
    }

    private lateinit var upgradeButton: ButtonWidget

    init {
        backgroundWidth = PANEL_WIDTH
        backgroundHeight = PANEL_HEIGHT
        titleX = 8
        titleY = 6
        playerInventoryTitleX = 8
        playerInventoryTitleY = 74
    }

    override fun init() {
        super.init()
        upgradeButton = ButtonWidget.builder(Text.translatable("screen.cresora.upgrade.button")) {
            client?.interactionManager?.clickButton(handler.syncId, UpgradeScreenHandler.BUTTON_UPGRADE)
        }.dimensions(x + 123, y + 47, 44, 20).build()
        addDrawableChild(upgradeButton)
    }

    override fun handledScreenTick() {
        super.handledScreenTick()
        val player = client?.player ?: return
        val preview = handler.getPreview(player)
        upgradeButton.active = preview.canUpgrade
        upgradeButton.message = if (preview.canUpgrade) {
            Text.translatable("screen.cresora.upgrade.button")
        } else {
            Text.translatable("screen.cresora.upgrade.button_locked")
        }
    }

    override fun drawBackground(context: DrawContext, delta: Float, mouseX: Int, mouseY: Int) {
        drawContainer(context, x, y, backgroundWidth, backgroundHeight)
        drawHeaderBand(context)
        drawInfoPanels(context)
        drawStatusBar(context)
        drawSlotFrame(context, x + 26, y + 29)
        drawSlotFrame(context, x + 75, y + 29)
        drawPreviewSlotFrame(context, x + 135, y + 25)
        drawInventorySlots(context)
    }

    override fun drawForeground(context: DrawContext, mouseX: Int, mouseY: Int) {
        context.drawText(textRenderer, title, titleX, titleY, TITLE_COLOR, false)
        context.drawText(textRenderer, playerInventoryTitle, playerInventoryTitleX, playerInventoryTitleY, LABEL_COLOR, false)
        context.drawCenteredTextWithShadow(textRenderer, Text.literal("+"), 58, 36, SYMBOL_COLOR)
        context.drawCenteredTextWithShadow(textRenderer, Text.literal("=>"), 114, 36, SYMBOL_COLOR)
        renderPreview(context)
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        renderBackground(context, mouseX, mouseY, delta)
        super.render(context, mouseX, mouseY, delta)
        drawMouseoverTooltip(context, mouseX, mouseY)
    }

    private fun renderPreview(context: DrawContext) {
        val player = client?.player ?: return
        val preview = handler.getPreview(player)
        val successRate = (preview.successRatePermille / 10.0).let {
            (it * 10.0).roundToInt() / 10.0
        }
        val levelText = Text.translatable(
            "screen.cresora.upgrade.level_compact",
            preview.currentLevel,
            preview.resultLevel
        )

        val rateText = if (preview.successRatePermille > 0) {
            Text.literal("$successRate%")
        } else {
            Text.translatable("screen.cresora.upgrade.rate_empty")
        }

        context.drawCenteredTextWithShadow(textRenderer, levelText, 56, 53, VALUE_COLOR)
        drawMetricLine(context, Text.literal("RATE"), rateText, 15, 63, VALUE_COLOR, 30)
        drawMetricLine(context, Text.literal("XP"), Text.literal(handler.getXpCost().toString()), 70, 63, VALUE_COLOR, 16)

        val statusText = getCompactStatusText(preview)
        val statusColor = if (preview.canUpgrade) READY_COLOR else ERROR_COLOR
        context.drawCenteredTextWithShadow(textRenderer, statusText, 88, 79, statusColor)
    }

    private fun drawContainer(context: DrawContext, x: Int, y: Int, width: Int, height: Int) {
        context.fill(x, y, x + width, y + height, FRAME_SHADOW)
        context.fill(x + 1, y + 1, x + width - 1, y + height - 1, FRAME_LIGHT)
        context.fill(x + 2, y + 2, x + width - 2, y + height - 2, FRAME_DARK)
        context.fill(x + 3, y + 3, x + width - 3, y + height - 3, BODY_FILL)
    }

    private fun drawInventorySlots(context: DrawContext) {
        for (row in 0 until 3) {
            for (column in 0 until 9) {
                drawSlotFrame(context, x + 7 + column * 18, y + 83 + row * 18)
            }
        }

        for (column in 0 until 9) {
            drawSlotFrame(context, x + 7 + column * 18, y + 141)
        }
    }

    private fun drawHeaderBand(context: DrawContext) {
        context.fill(x + 5, y + 4, x + backgroundWidth - 5, y + 16, HEADER_DARK)
        context.fill(x + 6, y + 5, x + backgroundWidth - 6, y + 15, HEADER_LIGHT)
        context.fill(x + 7, y + 6, x + backgroundWidth - 7, y + 14, FRAME_LIGHT)
    }

    private fun drawInfoPanels(context: DrawContext) {
        drawInsetPanel(context, x + 8, y + 18, 108, 52, LEFT_ACCENT)
        drawInsetPanel(context, x + 121, y + 18, 47, 52, RIGHT_ACCENT)
    }

    private fun drawStatusBar(context: DrawContext) {
        context.fill(x + 8, y + 73, x + 168, y + 87, STATUS_BORDER)
        context.fill(x + 9, y + 74, x + 167, y + 86, STATUS_BG)
        context.fill(x + 10, y + 75, x + 166, y + 85, FRAME_LIGHT)
    }

    private fun drawInsetPanel(context: DrawContext, x: Int, y: Int, width: Int, height: Int, accentColor: Int) {
        context.fill(x, y, x + width, y + height, FRAME_DARK)
        context.fill(x + 1, y + 1, x + width - 1, y + height - 1, FRAME_LIGHT)
        context.fill(x + 2, y + 2, x + width - 2, y + height - 2, PANEL_SHADOW)
        context.fill(x + 3, y + 3, x + width - 3, y + height - 3, PANEL_FILL)
        context.fill(x + 4, y + 4, x + width - 4, y + 6, accentColor)
        context.fill(x + 4, y + height - 7, x + width - 4, y + height - 5, accentColor)
    }

    private fun drawSlotFrame(context: DrawContext, x: Int, y: Int) {
        context.fill(x, y, x + 18, y + 18, PANEL_SHADOW)
        context.fill(x + 1, y + 1, x + 17, y + 17, FRAME_LIGHT)
        context.fill(x + 1, y + 1, x + 2, y + 17, FRAME_DARK)
        context.fill(x + 1, y + 1, x + 17, y + 2, FRAME_DARK)
        context.fill(x + 3, y + 3, x + 16, y + 16, SLOT_FILL)
    }

    private fun drawPreviewSlotFrame(context: DrawContext, x: Int, y: Int) {
        drawSlotFrame(context, x, y)
        context.fill(x + 20, y + 7, x + 34, y + 9, RIGHT_ACCENT)
        context.fill(x + 32, y + 5, x + 34, y + 11, RIGHT_ACCENT)
    }

    private fun drawMetricLine(context: DrawContext, label: Text, value: Text, x: Int, y: Int, valueColor: Int, valueOffset: Int = 36) {
        context.drawText(textRenderer, label, x, y, MUTED_COLOR, false)
        context.drawText(textRenderer, value, x + valueOffset, y, valueColor, false)
    }

    private fun getCompactStatusText(preview: UpgradeLogic.Preview): Text {
        return when (preview.messageKey) {
            "screen.cresora.upgrade.need_base" -> Text.translatable("screen.cresora.upgrade.need_base_short")
            "screen.cresora.upgrade.insert_material" -> Text.translatable("screen.cresora.upgrade.insert_material_short")
            "screen.cresora.upgrade.invalid_material" -> Text.translatable("screen.cresora.upgrade.invalid_material_short")
            "screen.cresora.upgrade.ready_tool" -> Text.translatable("screen.cresora.upgrade.ready_short")
            "screen.cresora.upgrade.ready_wand" -> Text.translatable("screen.cresora.upgrade.ready_short")
            "item.cresora.not_enough_xp" -> Text.translatable("screen.cresora.upgrade.no_xp_short")
            else -> Text.empty()
        }
    }
}
