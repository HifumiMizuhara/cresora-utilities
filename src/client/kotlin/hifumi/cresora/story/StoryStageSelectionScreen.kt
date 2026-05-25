package hifumi.cresora.story
import hifumi.cresora.equipment.ArtifactChestScreenBase
import net.minecraft.client.gui.DrawContext
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.text.Text

class StoryStageSelectionScreen(
    handler: StoryStageSelectionScreenHandler,
    inventory: PlayerInventory,
    title: Text
) : ArtifactChestScreenBase<StoryStageSelectionScreenHandler>(handler, inventory, title, 1) {
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
