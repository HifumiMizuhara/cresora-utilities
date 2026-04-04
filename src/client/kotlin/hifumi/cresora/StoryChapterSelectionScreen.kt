package hifumi.cresora

import net.minecraft.client.gui.DrawContext
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.text.Text

class StoryChapterSelectionScreen(
    handler: StoryChapterSelectionScreenHandler,
    inventory: PlayerInventory,
    title: Text
) : ArtifactChestScreenBase<StoryChapterSelectionScreenHandler>(handler, inventory, title, 1) {
    override fun drawExtraForeground(context: DrawContext, mouseX: Int, mouseY: Int) {
        context.drawText(
            textRenderer,
            Text.translatable("screen.cresora.story.rank", handler.currentRank()),
            8,
            6,
            0xFF241C14.toInt(),
            false
        )
    }
}
