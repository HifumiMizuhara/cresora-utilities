package hifumi.cresora

import net.minecraft.client.gui.DrawContext
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.text.Text

class DomainRewardScreen(
    handler: DomainRewardScreenHandler,
    inventory: PlayerInventory,
    title: Text
) : ArtifactChestScreenBase<DomainRewardScreenHandler>(handler, inventory, title, 1) {
    override fun drawExtraForeground(context: DrawContext, mouseX: Int, mouseY: Int) {
        context.drawText(
            textRenderer,
            Text.translatable("screen.cresora.domain.reward_hint"),
            86,
            6,
            0xFF241C14.toInt(),
            false
        )
    }
}
