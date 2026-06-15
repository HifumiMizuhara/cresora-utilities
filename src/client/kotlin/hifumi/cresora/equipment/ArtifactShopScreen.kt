package hifumi.cresora.equipment

import net.minecraft.client.gui.DrawContext
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.text.Text
import java.util.Locale

class ArtifactShopScreen(
    handler: ArtifactShopScreenHandler,
    inventory: PlayerInventory,
    title: Text
) : ArtifactBookScreenBase<ArtifactShopScreenHandler>(handler, inventory, title) {

    override fun getActiveTab(): Int = 5

    override fun init() {
        super.init()

        // Position shop offer slots 0..17 on the left page in a 3x6 grid
        for (i in 0 until 18) {
            handler.slots[i].x = 40 + (i % 3) * 24
            handler.slots[i].y = 40 + (i / 3) * 24
        }

        // Position sell slots on the right page
        handler.slots[18].x = 215 // SELL_INPUT_SLOT
        handler.slots[18].y = 35

        handler.slots[19].x = 265 // SELL_PREVIEW_SLOT
        handler.slots[19].y = 35

        handler.slots[22].x = 315 // SELL_BUTTON_SLOT
        handler.slots[22].y = 35

        // Hide filler slots (20, 21, 23, 24, 25) and back slot (26)
        for (i in listOf(20, 21, 23, 24, 25, 26)) {
            handler.slots[i].x = -2000
            handler.slots[i].y = -2000
        }

        // Position player inventory slots (27..62) on the right page
        // Main inventory slots 27..53 (3 rows of 9)
        for (row in 0 until 3) {
            for (col in 0 until 9) {
                val idx = 27 + col + row * 9
                handler.slots[idx].x = 200 + col * 18
                handler.slots[idx].y = 95 + row * 18
            }
        }
        // Hotbar slots 54..62 (1 row of 9)
        for (col in 0 until 9) {
            val idx = 54 + col
            handler.slots[idx].x = 200 + col * 18
            handler.slots[idx].y = 155
        }
    }

    override fun drawBackground(context: DrawContext, delta: Float, mouseX: Int, mouseY: Int) {
        super.drawBackground(context, delta, mouseX, mouseY)

        // Draw slot frames for offers
        for (i in 0 until 18) {
            drawSlotFrame(context, x + 40 + (i % 3) * 24, y + 40 + (i / 3) * 24)
        }

        // Draw slot frames for sell slots
        drawSlotFrame(context, x + 215, y + 35)
        drawSlotFrame(context, x + 265, y + 35)
        drawSlotFrame(context, x + 315, y + 35)

        // Draw slot frames for player inventory
        for (row in 0 until 3) {
            for (col in 0 until 9) {
                drawSlotFrame(context, x + 200 + col * 18, y + 95 + row * 18)
            }
        }
        for (col in 0 until 9) {
            drawSlotFrame(context, x + 200 + col * 18, y + 155)
        }
    }

    override fun drawForeground(context: DrawContext, mouseX: Int, mouseY: Int) {
        super.drawForeground(context, mouseX, mouseY)

        // Left Page Title
        drawCenteredText(context, Text.translatable("screen.cresora.shop"), 97, 20, 0xFF3C3024.toInt())

        // Right Page Title
        drawCenteredText(context, Text.translatable("screen.cresora.shop.recycle_title"), 282, 20, 0xFF3C3024.toInt())

        // Credits & Sell value on Right Page
        context.drawText(
            textRenderer,
            Text.translatable("screen.cresora.shop.credits", String.format(Locale.ROOT, "%,d", handler.currentCredits())),
            215,
            60,
            0xFF3C3024.toInt(),
            false
        )
        context.drawText(
            textRenderer,
            Text.translatable("screen.cresora.shop.sell_value", String.format(Locale.ROOT, "%,d", handler.currentSellPrice())),
            215,
            72,
            0xFF3C3024.toInt(),
            false
        )
    }

    private fun drawCenteredText(context: DrawContext, text: Text, centerX: Int, y: Int, color: Int) {
        val width = textRenderer.getWidth(text)
        context.drawText(textRenderer, text, centerX - width / 2, y, color, false)
    }
}
