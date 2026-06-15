package hifumi.cresora.resonance

import hifumi.cresora.equipment.ArtifactBookScreenBase
import net.minecraft.client.gui.DrawContext
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.text.Text

class ResonanceScreen(
    handler: ResonanceScreenHandler,
    inventory: PlayerInventory,
    title: Text
) : ArtifactBookScreenBase<ResonanceScreenHandler>(handler, inventory, title) {

    override fun getActiveTab(): Int = 4

    override fun init() {
        super.init()

        // Position featured, limited, standard slots on the right page
        handler.slots[0].x = 215
        handler.slots[0].y = 60

        handler.slots[2].x = 275
        handler.slots[2].y = 60

        handler.slots[6].x = 335
        handler.slots[6].y = 60

        // Hide filler slots and back slot
        for (i in listOf(1, 3, 4, 5, 7, 8)) {
            handler.slots[i].x = -2000
            handler.slots[i].y = -2000
        }

        // Hide player inventory slots
        for (i in 9 until handler.slots.size) {
            handler.slots[i].x = -2000
            handler.slots[i].y = -2000
        }
    }

    override fun drawBackground(context: DrawContext, delta: Float, mouseX: Int, mouseY: Int) {
        super.drawBackground(context, delta, mouseX, mouseY)

        // Draw slot frames for featured, limited, standard slots
        drawSlotFrame(context, x + 215, y + 60)
        drawSlotFrame(context, x + 275, y + 60)
        drawSlotFrame(context, x + 335, y + 60)
    }

    override fun drawForeground(context: DrawContext, mouseX: Int, mouseY: Int) {
        super.drawForeground(context, mouseX, mouseY)

        // Left Page Title
        drawCenteredText(context, Text.translatable("screen.cresora.resonance"), 97, 20, 0xFF3C3024.toInt())

        // Stats on Left Page
        context.drawText(textRenderer, Text.translatable("screen.cresora.resonance.limited_pity", handler.limitedPity()), 20, 50, 0xFF3C3024.toInt(), false)
        context.drawText(textRenderer, Text.translatable("screen.cresora.resonance.standard_pulls", handler.standardPulls()), 20, 70, 0xFF3C3024.toInt(), false)
        context.drawText(textRenderer, Text.translatable("screen.cresora.resonance.currency_line", handler.limitedCurrency(), handler.standardCurrency()), 20, 100, 0xFF5F503D.toInt(), false)

        // Right Page Title
        drawCenteredText(context, Text.translatable("screen.cresora.resonance"), 282, 20, 0xFF3C3024.toInt())

        // Labels under Slots
        drawCenteredText(context, Text.translatable("screen.cresora.resonance.featured_label"), 224, 84, 0xFF5F503D.toInt())
        drawCenteredText(context, Text.translatable("screen.cresora.resonance.limited_label"), 284, 84, 0xFF5F503D.toInt())
        drawCenteredText(context, Text.translatable("screen.cresora.resonance.standard_label"), 344, 84, 0xFF5F503D.toInt())
    }

    private fun drawCenteredText(context: DrawContext, text: Text, centerX: Int, y: Int, color: Int) {
        val width = textRenderer.getWidth(text)
        context.drawText(textRenderer, text, centerX - width / 2, y, color, false)
    }
}
