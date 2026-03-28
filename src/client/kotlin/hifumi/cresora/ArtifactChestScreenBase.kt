package hifumi.cresora

import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.screen.ScreenHandler
import net.minecraft.text.Text

abstract class ArtifactChestScreenBase<T : ScreenHandler>(
    handler: T,
    inventory: PlayerInventory,
    title: Text,
    private val rows: Int
) : HandledScreen<T>(handler, inventory, title) {

    init {
        backgroundWidth = 176
        backgroundHeight = 114 + rows * 18
        titleX = 8
        titleY = 6
        playerInventoryTitleX = 8
        playerInventoryTitleY = backgroundHeight - 94
    }

    override fun drawBackground(context: DrawContext, delta: Float, mouseX: Int, mouseY: Int) {
        context.fill(x, y, x + backgroundWidth, y + backgroundHeight, 0xFF241B12.toInt())
        context.fill(x + 2, y + 2, x + backgroundWidth - 2, y + backgroundHeight - 2, 0xFFF0E2C2.toInt())
        context.fill(x + 5, y + 5, x + backgroundWidth - 5, y + 15, 0xFF5C8C83.toInt())

        for (row in 0 until rows) {
            for (column in 0 until 9) {
                drawSlotFrame(context, x + 7 + column * 18, y + 17 + row * 18)
            }
        }

        val inventoryTop = y + 17 + rows * 18 + 14
        for (row in 0 until 3) {
            for (column in 0 until 9) {
                drawSlotFrame(context, x + 7 + column * 18, inventoryTop - 1 + row * 18)
            }
        }
        for (column in 0 until 9) {
            drawSlotFrame(context, x + 7 + column * 18, inventoryTop + 57)
        }
    }

    override fun drawForeground(context: DrawContext, mouseX: Int, mouseY: Int) {
        context.drawText(textRenderer, title, titleX, titleY, 0xFF241C14.toInt(), false)
        context.drawText(textRenderer, playerInventoryTitle, playerInventoryTitleX, playerInventoryTitleY, 0xFF5F503D.toInt(), false)
        drawExtraForeground(context, mouseX, mouseY)
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        renderBackground(context, mouseX, mouseY, delta)
        super.render(context, mouseX, mouseY, delta)
        drawMouseoverTooltip(context, mouseX, mouseY)
    }

    protected open fun drawExtraForeground(context: DrawContext, mouseX: Int, mouseY: Int) {}

    private fun drawSlotFrame(context: DrawContext, slotX: Int, slotY: Int) {
        context.fill(slotX, slotY, slotX + 18, slotY + 18, 0xFFC8AF82.toInt())
        context.fill(slotX + 1, slotY + 1, slotX + 17, slotY + 17, 0xFFF5E9CF.toInt())
        context.fill(slotX + 2, slotY + 2, slotX + 16, slotY + 16, 0xFF221810.toInt())
    }
}
