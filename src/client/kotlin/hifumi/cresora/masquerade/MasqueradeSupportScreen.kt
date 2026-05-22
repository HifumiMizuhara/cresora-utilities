package hifumi.cresora.masquerade
import hifumi.cresora.equipment.ArtifactChestScreenBase
import net.minecraft.client.gui.DrawContext
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.text.Text

class MasqueradeSupportScreen(
    handler: MasqueradeSupportScreenHandler,
    inventory: PlayerInventory,
    title: Text
) : ArtifactChestScreenBase<MasqueradeSupportScreenHandler>(handler, inventory, title, 1) {
    override fun drawExtraForeground(context: DrawContext, mouseX: Int, mouseY: Int) {
        context.drawText(
            textRenderer,
            Text.translatable("screen.cresora.masquerade.support.wave", handler.currentWave(), handler.maxWave()),
            8,
            6,
            0xFF241C14.toInt(),
            false
        )
        context.drawText(
            textRenderer,
            Text.translatable("screen.cresora.masquerade.support.cleared", handler.clearedWave()),
            98,
            6,
            0xFF241C14.toInt(),
            false
        )
    }
}
