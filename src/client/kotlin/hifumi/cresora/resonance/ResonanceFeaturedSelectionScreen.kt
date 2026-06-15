package hifumi.cresora.resonance

import hifumi.cresora.equipment.ArtifactBookScreenBase
import net.minecraft.client.gui.DrawContext
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.text.Text

class ResonanceFeaturedSelectionScreen(
    handler: ResonanceFeaturedSelectionScreenHandler,
    inventory: PlayerInventory,
    title: Text
) : ArtifactBookScreenBase<ResonanceFeaturedSelectionScreenHandler>(handler, inventory, title) {

    override fun getActiveTab(): Int = 4

    override fun init() {
        super.init()

        // Position selectable weapon slots 0..35 across both pages in 3x6 grids
        for (i in 0 until 36) {
            if (i < 18) {
                handler.slots[i].x = 40 + (i % 3) * 24
                handler.slots[i].y = 40 + (i / 3) * 24
            } else {
                handler.slots[i].x = 225 + ((i - 18) % 3) * 24
                handler.slots[i].y = 40 + ((i - 18) / 3) * 24
            }
        }

        // Hide player inventory slots
        for (i in 36 until handler.slots.size) {
            handler.slots[i].x = -2000
            handler.slots[i].y = -2000
        }
    }

    override fun drawBackground(context: DrawContext, delta: Float, mouseX: Int, mouseY: Int) {
        super.drawBackground(context, delta, mouseX, mouseY)

        // Draw slot frames for the 36 slots
        for (i in 0 until 36) {
            if (i < 18) {
                drawSlotFrame(context, x + 40 + (i % 3) * 24, y + 40 + (i / 3) * 24)
            } else {
                drawSlotFrame(context, x + 225 + ((i - 18) % 3) * 24, y + 40 + ((i - 18) / 3) * 24)
            }
        }
    }

    override fun drawForeground(context: DrawContext, mouseX: Int, mouseY: Int) {
        super.drawForeground(context, mouseX, mouseY)

        // Left Page Title
        drawCenteredText(context, Text.translatable("screen.cresora.resonance.featured_selection"), 97, 20, 0xFF3C3024.toInt())

        // Right Page Title
        drawCenteredText(context, Text.translatable("screen.cresora.resonance.featured_selection"), 282, 20, 0xFF3C3024.toInt())
    }

    private fun drawCenteredText(context: DrawContext, text: Text, centerX: Int, y: Int, color: Int) {
        val width = textRenderer.getWidth(text)
        context.drawText(textRenderer, text, centerX - width / 2, y, color, false)
    }
}
