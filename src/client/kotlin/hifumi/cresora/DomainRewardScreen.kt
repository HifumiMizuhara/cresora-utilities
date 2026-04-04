package hifumi.cresora

import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.widget.ButtonWidget
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.text.Text

class DomainRewardScreen(
    handler: DomainRewardScreenHandler,
    inventory: PlayerInventory,
    title: Text
) : ArtifactChestScreenBase<DomainRewardScreenHandler>(handler, inventory, title, 1) {
    override fun init() {
        super.init()
        addDrawableChild(
            ButtonWidget.builder(Text.translatable("screen.cresora.domain.reward_done")) {
                close()
            }.dimensions(x + 118, y + 4, 50, 12).build()
        )
    }

    override fun drawExtraForeground(context: DrawContext, mouseX: Int, mouseY: Int) {
        context.drawText(textRenderer, Text.translatable("screen.cresora.domain.reward_hint"), 76, 6, 0xFF241C14.toInt(), false)
    }
}
