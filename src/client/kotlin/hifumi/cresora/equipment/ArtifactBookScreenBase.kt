package hifumi.cresora.equipment

import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.client.gui.widget.ButtonWidget
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.screen.ScreenHandler
import net.minecraft.text.Text

abstract class ArtifactBookScreenBase<T : ScreenHandler>(
    handler: T,
    inventory: PlayerInventory,
    title: Text
) : HandledScreen<T>(handler, inventory, title) {

    private lateinit var bookCloseButton: ButtonWidget

    init {
        backgroundWidth = 380
        backgroundHeight = 220
        titleX = -2000
        titleY = -2000
        playerInventoryTitleX = -2000
        playerInventoryTitleY = -2000
    }

    abstract fun getActiveTab(): Int

    companion object {
        private const val TAB_WIDTH = 52

        private val TAB_LABEL_KEYS = listOf(
            "screen.cresora.book.tab.guide",
            "screen.cresora.book.tab.story",
            "screen.cresora.book.tab.domain",
            "screen.cresora.book.tab.resonance",
            "screen.cresora.book.tab.shop",
            "screen.cresora.book.tab.records"
        )

        private var savedMouseX: Double? = null
        private var savedMouseY: Double? = null
        private var savedTime: Long = 0L
    }

    override fun init() {
        super.init()
        val mc = client
        if (mc != null) {
            val mx = savedMouseX
            val my = savedMouseY
            if (mx != null && my != null && (System.currentTimeMillis() - savedTime) < 1000) {
                org.lwjgl.glfw.GLFW.glfwSetCursorPos(mc.window.handle, mx, my)
            }
        }
        savedMouseX = null
        savedMouseY = null

        bookCloseButton = ButtonWidget.builder(Text.literal("✕")) {
            mc?.player?.closeHandledScreen()
        }.dimensions(x + 372, y + 12, 14, 18).build()
        addDrawableChild(bookCloseButton)
    }

    override fun removed() {
        val mc = client
        if (mc != null) {
            savedMouseX = mc.mouse.x
            savedMouseY = mc.mouse.y
            savedTime = System.currentTimeMillis()
        }
        super.removed()
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        renderBackground(context, mouseX, mouseY, delta)
        super.render(context, mouseX, mouseY, delta)
        drawMouseoverTooltip(context, mouseX, mouseY)
    }

    override fun drawMouseoverTooltip(context: DrawContext, x: Int, y: Int) {
        // Book pages render their details inline; item tooltips obscure the layout.
    }

    override fun drawBackground(context: DrawContext, delta: Float, mouseX: Int, mouseY: Int) {
        // Draw Book Cover Shadow / Outer Frame
        context.fill(x, y, x + backgroundWidth, y + backgroundHeight, 0xFF2C231A.toInt())
        context.fill(x + 1, y + 1, x + backgroundWidth - 1, y + backgroundHeight - 1, 0xFF423427.toInt())
        // Gold Outer Ring Border on Cover
        context.fill(x + 4, y + 4, x + backgroundWidth - 4, y + backgroundHeight - 4, 0xFFC0A678.toInt())
        context.fill(x + 5, y + 5, x + backgroundWidth - 5, y + backgroundHeight - 5, 0xFF423427.toInt())

        val activeTab = getActiveTab()

        // Draw Left Decorative Tabs (Genshin Style)
        for (tab in 1..6) {
            val tabY = y + 20 + (tab - 1) * 30
            if (tab == activeTab) {
                // Active Tab background
                context.fill(x - TAB_WIDTH, tabY, x, tabY + 25, 0xFFF5EFEB.toInt())
                context.fill(x - TAB_WIDTH, tabY, x - TAB_WIDTH + 2, tabY + 25, 0xFF5C9E32.toInt()) // Green Indicator
            } else {
                // Inactive Tab background
                context.fill(x - TAB_WIDTH + 2, tabY, x, tabY + 25, 0xFF5E4F3E.toInt())
            }
        }

        // Draw Left Page & Right Page
        context.fill(x + 10, y + 10, x + 185, y + 210, 0xFFF5EFEB.toInt())
        context.fill(x + 195, y + 10, x + 370, y + 210, 0xFFF5EFEB.toInt())

        // Draw Crease Shadow
        context.fill(x + 185, y + 10, x + 187, y + 210, 0xFFE5DDD5.toInt())
        context.fill(x + 193, y + 10, x + 195, y + 210, 0xFFE5DDD5.toInt())
        context.fill(x + 187, y + 10, x + 193, y + 210, 0xFFD3C9BE.toInt()) // Crease center

        // Draw Close Tab on the Right
        context.fill(x + 370, y + 10, x + 386, y + 32, 0xFF5E4F3E.toInt())
    }

    override fun drawForeground(context: DrawContext, mouseX: Int, mouseY: Int) {
        val activeTab = getActiveTab()

        TAB_LABEL_KEYS.forEachIndexed { index, key ->
            drawTabText(context, Text.translatable(key), index + 1, activeTab)
        }
    }

    private fun drawTabText(context: DrawContext, text: Text, tabIdx: Int, activeTab: Int) {
        val tabY = 23 + (tabIdx - 1) * 30
        val color = if (tabIdx == activeTab) 0xFF3C3024.toInt() else 0xFFC0B2A3.toInt()
        val xOffset = -TAB_WIDTH + (TAB_WIDTH - textRenderer.getWidth(text)) / 2
        context.drawText(textRenderer, text, xOffset, tabY + 8, color, false)
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        val relativeX = mouseX - x
        val relativeY = mouseY - y
        val mc = client ?: return super.mouseClicked(mouseX, mouseY, button)

        if (relativeX in -TAB_WIDTH.toDouble()..0.0) {
            for (tab in 1..6) {
                val tabYStart = 20.0 + (tab - 1) * 30.0
                val tabYEnd = tabYStart + 25.0
                if (relativeY in tabYStart..tabYEnd) {
                    if (tab == getActiveTab()) return true
                    when (tab) {
                        1 -> mc.player?.networkHandler?.sendChatCommand("cresora")
                        2 -> mc.player?.networkHandler?.sendChatCommand("cresora_story")
                        3 -> mc.player?.networkHandler?.sendChatCommand("cresora_domain")
                        4 -> mc.player?.networkHandler?.sendChatCommand("cresora_resonance")
                        5 -> mc.player?.networkHandler?.sendChatCommand("cresora_shop")
                        6 -> mc.player?.networkHandler?.sendChatCommand("cresora_records")
                    }
                    return true
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button)
    }

    protected fun drawSlotFrame(context: DrawContext, slotX: Int, slotY: Int) {
        context.fill(slotX, slotY, slotX + 18, slotY + 18, 0xFF423427.toInt())
        context.fill(slotX + 1, slotY + 1, slotX + 17, slotY + 17, 0xFFDCD3C1.toInt())
    }
}
