package hifumi.cresora.story

import hifumi.cresora.equipment.ArtifactBookScreenBase
import net.minecraft.client.gui.DrawContext
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.text.Text

class StoryChapterSelectionScreen(
    handler: StoryChapterSelectionScreenHandler,
    inventory: PlayerInventory,
    title: Text
) : ArtifactBookScreenBase<StoryChapterSelectionScreenHandler>(handler, inventory, title) {

    override fun getActiveTab(): Int = 2

    override fun init() {
        super.init()

        // Position chapter group slots 0..7 on the right page
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

        // Draw slot frames for chapter slots on the right page
        for (i in 0 until 8) {
            val col = i % 4
            val row = i / 4
            drawSlotFrame(context, x + 215 + col * 32, y + 50 + row * 32)
        }
    }

    override fun drawForeground(context: DrawContext, mouseX: Int, mouseY: Int) {
        super.drawForeground(context, mouseX, mouseY)

        // Left Page Title
        drawCenteredText(context, Text.translatable("screen.cresora.story"), 97, 20, 0xFF3C3024.toInt())

        // Description
        drawCenteredText(context, Text.translatable("screen.cresora.story.description_1"), 97, 60, 0xFF5F503D.toInt())
        drawCenteredText(context, Text.translatable("screen.cresora.story.description_2"), 97, 72, 0xFF5F503D.toInt())
        drawCenteredText(context, Text.translatable("screen.cresora.story.description_3"), 97, 96, 0xFF83715D.toInt())

        // Player Adventure Rank
        drawCenteredText(context, Text.translatable("screen.cresora.story.rank", handler.currentRank()), 97, 140, 0xFF3C3024.toInt())

        // Right Page Title
        drawCenteredText(context, Text.translatable("screen.cresora.story.chapters_title"), 282, 20, 0xFF3C3024.toInt())
    }

    private fun drawCenteredText(context: DrawContext, text: Text, centerX: Int, y: Int, color: Int) {
        val width = textRenderer.getWidth(text)
        context.drawText(textRenderer, text, centerX - width / 2, y, color, false)
    }
}
