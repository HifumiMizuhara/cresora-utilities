package hifumi.cresora.equipment
import net.minecraft.client.gui.DrawContext
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.text.Text

class ArtifactAlphaScreen(
    handler: ArtifactAlphaScreenHandler,
    inventory: PlayerInventory,
    title: Text
) : ArtifactChestScreenBase<ArtifactAlphaScreenHandler>(handler, inventory, title, 6) {
    override fun shouldDrawTitle(): Boolean = false

    override fun drawExtraForeground(context: DrawContext, mouseX: Int, mouseY: Int) {
        context.drawText(
            textRenderer,
            Text.translatable("screen.cresora.alpha.hint"),
            90,
            6,
            0xFF241C14.toInt(),
            false
        )
    }
}
