package hifumi.cresora.domain

import hifumi.cresora.equipment.ArtifactBookScreenBase
import net.minecraft.client.gui.DrawContext
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.text.Text
import java.util.Locale

class DomainSelectionScreen(
    handler: DomainSelectionScreenHandler,
    inventory: PlayerInventory,
    title: Text
) : ArtifactBookScreenBase<DomainSelectionScreenHandler>(handler, inventory, title) {

    override fun getActiveTab(): Int = 3

    override fun init() {
        super.init()

        // Position domain slots 0..7 in a 4x2 grid on the right page
        for (i in 0 until 8) {
            val col = i % 4
            val row = i / 4
            handler.slots[i].x = 215 + col * 32
            handler.slots[i].y = 50 + row * 32
        }

        // Hide back slot
        handler.slots[8].x = -2000
        handler.slots[8].y = -2000

        // Hide player inventory slots
        for (i in 9 until handler.slots.size) {
            handler.slots[i].x = -2000
            handler.slots[i].y = -2000
        }
    }

    override fun drawBackground(context: DrawContext, delta: Float, mouseX: Int, mouseY: Int) {
        super.drawBackground(context, delta, mouseX, mouseY)

        // Draw slot frames for active domain slots on the right page
        for (i in 0 until 8) {
            val col = i % 4
            val row = i / 4
            drawSlotFrame(context, x + 215 + col * 32, y + 50 + row * 32)
        }
    }

    override fun drawForeground(context: DrawContext, mouseX: Int, mouseY: Int) {
        super.drawForeground(context, mouseX, mouseY)

        // Left Page Title
        drawCenteredText(context, Text.translatable("screen.cresora.domain"), 97, 20, 0xFF3C3024.toInt())

        // Right Page Title
        drawCenteredText(context, Text.translatable("screen.cresora.domain.list_title"), 282, 20, 0xFF3C3024.toInt())

        // Hovered Domain Info
        val slot = focusedSlot
        if (slot != null && slot.id in 0 until 8) {
            val domainIndex = slot.id
            val domain = handler.domainForSlot(domainIndex)
            if (domain != null) {
                // Name
                context.drawText(textRenderer, Text.translatable(domain.nameKey), 20, 50, 0xFF3C3024.toInt(), false)
                // Recommended Band
                context.drawText(textRenderer, handler.recommendedBandText(domainIndex), 20, 70, 0xFF5F503D.toInt(), false)
                // Cost
                context.drawText(textRenderer, Text.translatable("screen.cresora.domain.cost", handler.entryCostText(domainIndex)), 20, 90, 0xFF5F503D.toInt(), false)
                // Reward
                context.drawText(textRenderer, handler.mainRewardText(domainIndex), 20, 110, 0xFF83715D.toInt(), false)
            }
        } else {
            drawCenteredText(context, Text.translatable("screen.cresora.domain.select_hint"), 97, 60, 0xFF5F503D.toInt())
        }

        // Stats at the bottom of the left page
        context.drawText(textRenderer, Text.translatable("screen.cresora.domain.rank", handler.currentRank()), 20, 150, 0xFF3C3024.toInt(), false)
        context.drawText(textRenderer, Text.translatable("screen.cresora.domain.credits", String.format(Locale.ROOT, "%,d", handler.currentCredits())), 20, 162, 0xFF3C3024.toInt(), false)
    }

    private fun drawCenteredText(context: DrawContext, text: Text, centerX: Int, y: Int, color: Int) {
        val width = textRenderer.getWidth(text)
        context.drawText(textRenderer, text, centerX - width / 2, y, color, false)
    }
}
