package hifumi.cresora

import net.minecraft.client.gui.DrawContext
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.text.Text

class MasqueradeLoadoutScreen(
    handler: MasqueradeLoadoutScreenHandler,
    inventory: PlayerInventory,
    title: Text
) : ArtifactChestScreenBase<MasqueradeLoadoutScreenHandler>(handler, inventory, title, 4) {
    override fun drawExtraForeground(context: DrawContext, mouseX: Int, mouseY: Int) {
        context.drawText(
            textRenderer,
            Text.translatable("screen.cresora.masquerade.rank", handler.currentRank()),
            8,
            6,
            0xFF241C14.toInt(),
            false
        )
        context.drawText(
            textRenderer,
            Text.translatable("screen.cresora.masquerade.loadout.count", handler.selectedCount(), 4),
            88,
            6,
            0xFF241C14.toInt(),
            false
        )
    }
}
