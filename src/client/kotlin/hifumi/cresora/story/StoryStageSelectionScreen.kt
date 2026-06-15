package hifumi.cresora.story

import hifumi.cresora.equipment.ArtifactBookScreenBase
import net.minecraft.client.gui.DrawContext
import net.minecraft.component.DataComponentTypes
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.text.Text

class StoryStageSelectionScreen(
    handler: StoryStageSelectionScreenHandler,
    inventory: PlayerInventory,
    title: Text
) : ArtifactBookScreenBase<StoryStageSelectionScreenHandler>(handler, inventory, title) {

    override fun getActiveTab(): Int = 2

    override fun init() {
        super.init()

        // Position stage slots 0..8 in a 3x3 grid on the right page
        for (i in 0 until 9) {
            val col = i % 3
            val row = i / 3
            handler.slots[i].x = 220 + col * 32
            handler.slots[i].y = 50 + row * 32
        }

        // Hide player inventory slots
        for (i in 9 until handler.slots.size) {
            handler.slots[i].x = -2000
            handler.slots[i].y = -2000
        }
    }

    override fun drawBackground(context: DrawContext, delta: Float, mouseX: Int, mouseY: Int) {
        super.drawBackground(context, delta, mouseX, mouseY)

        // Draw slot frames for active stage slots on the right page
        for (i in 0 until 9) {
            val col = i % 3
            val row = i / 3
            drawSlotFrame(context, x + 220 + col * 32, y + 50 + row * 32)
        }
    }

    override fun drawForeground(context: DrawContext, mouseX: Int, mouseY: Int) {
        super.drawForeground(context, mouseX, mouseY)

        // Left Page Title
        drawCenteredText(context, Text.translatable("screen.cresora.story"), 97, 20, 0xFF3C3024.toInt())

        // Right Page Title
        drawCenteredText(context, Text.translatable("screen.cresora.story.stages_title"), 282, 20, 0xFF3C3024.toInt())

        // Hovered Stage Info on Left Page
        val slot = focusedSlot
        if (slot != null && slot.id in 0 until 9) {
            val stack = handler.slots[slot.id].stack
            if (!stack.isEmpty && stack.item != net.minecraft.item.Items.GRAY_STAINED_GLASS_PANE) {
                // Name
                context.drawText(textRenderer, stack.name, 20, 50, 0xFF3C3024.toInt(), false)

                // Lore
                val lore = stack.get(DataComponentTypes.LORE)?.lines ?: emptyList()
                for (idx in lore.indices) {
                    context.drawText(textRenderer, lore[idx], 20, 70 + idx * 12, 0xFF5F503D.toInt(), false)
                }
            } else {
                drawCenteredText(context, Text.translatable("screen.cresora.story.select_stage_hint"), 97, 60, 0xFF5F503D.toInt())
            }
        } else {
            drawCenteredText(context, Text.translatable("screen.cresora.story.select_stage_hint"), 97, 60, 0xFF5F503D.toInt())
        }
    }

    private fun drawCenteredText(context: DrawContext, text: Text, centerX: Int, y: Int, color: Int) {
        val width = textRenderer.getWidth(text)
        context.drawText(textRenderer, text, centerX - width / 2, y, color, false)
    }
}
