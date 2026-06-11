package hifumi.cresora.resonance
import hifumi.cresora.equipment.ArtifactChestScreenBase
import net.minecraft.client.gui.DrawContext
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.text.Text

class ResonanceScreen(
    handler: ResonanceScreenHandler,
    inventory: PlayerInventory,
    title: Text
) : ArtifactChestScreenBase<ResonanceScreenHandler>(handler, inventory, title, 1) {
    override fun shouldDrawTitle(): Boolean = false

    override fun drawExtraForeground(context: DrawContext, mouseX: Int, mouseY: Int) {
        context.drawText(
            textRenderer,
            Text.translatable("screen.cresora.resonance.limited_pity", handler.limitedPity()),
            8,
            6,
            0xFF241C14.toInt(),
            false
        )
        context.drawText(
            textRenderer,
            Text.translatable("screen.cresora.resonance.standard_pulls", handler.standardPulls()),
            88,
            6,
            0xFF241C14.toInt(),
            false
        )
        context.drawText(
            textRenderer,
            Text.translatable("screen.cresora.resonance.currency_line", handler.limitedCurrency(), handler.standardCurrency()),
            8,
            42,
            0xFF5F503D.toInt(),
            false
        )
    }
}
