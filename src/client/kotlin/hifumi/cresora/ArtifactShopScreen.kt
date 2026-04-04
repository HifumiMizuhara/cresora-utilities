package hifumi.cresora

import net.minecraft.client.gui.DrawContext
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.text.Text
import java.util.Locale

class ArtifactShopScreen(
    handler: ArtifactShopScreenHandler,
    inventory: PlayerInventory,
    title: Text
) : ArtifactChestScreenBase<ArtifactShopScreenHandler>(handler, inventory, title, 3) {
    override fun drawExtraForeground(context: DrawContext, mouseX: Int, mouseY: Int) {
        context.drawText(
            textRenderer,
            Text.translatable("screen.cresora.shop.credits", String.format(Locale.ROOT, "%,d", handler.currentCredits())),
            86,
            6,
            0xFF241C14.toInt(),
            false
        )
        context.drawText(
            textRenderer,
            Text.translatable("screen.cresora.shop.sell_value", String.format(Locale.ROOT, "%,d", handler.currentSellPrice())),
            82,
            16,
            0xFF241C14.toInt(),
            false
        )
    }
}
