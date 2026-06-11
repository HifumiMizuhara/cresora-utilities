package hifumi.cresora

import hifumi.cresora.equipment.ArtifactChestScreenBase
import net.minecraft.client.gui.DrawContext
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.text.Text
import java.util.Locale

class CresoraMenuScreen(
    handler: CresoraMenuScreenHandler,
    inventory: PlayerInventory,
    title: Text
) : ArtifactChestScreenBase<CresoraMenuScreenHandler>(handler, inventory, title, 1) {
    override fun shouldDrawTitle(): Boolean = false

    override fun drawExtraForeground(context: DrawContext, mouseX: Int, mouseY: Int) {
        context.drawText(
            textRenderer,
            Text.translatable("screen.cresora.domain.rank", handler.currentRank()),
            8,
            6,
            0xFF241C14.toInt(),
            false
        )
        context.drawText(
            textRenderer,
            Text.translatable("screen.cresora.domain.credits", String.format(Locale.ROOT, "%,d", handler.currentCredits())),
            86,
            6,
            0xFF241C14.toInt(),
            false
        )
    }
}
