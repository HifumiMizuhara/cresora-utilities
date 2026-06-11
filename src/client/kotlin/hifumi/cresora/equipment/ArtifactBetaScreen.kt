package hifumi.cresora.equipment
import net.minecraft.client.gui.DrawContext
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.text.Text

class ArtifactBetaScreen(
    handler: ArtifactBetaScreenHandler,
    inventory: PlayerInventory,
    title: Text
) : ArtifactChestScreenBase<ArtifactBetaScreenHandler>(handler, inventory, title, 3) {
    override fun shouldDrawTitle(): Boolean = false

    override fun drawExtraForeground(context: DrawContext, mouseX: Int, mouseY: Int) {
        context.drawText(
            textRenderer,
            Text.translatable("screen.cresora.beta.count", handler.selectedCount(), 2),
            96,
            6,
            0xFF241C14.toInt(),
            false
        )
    }
}
