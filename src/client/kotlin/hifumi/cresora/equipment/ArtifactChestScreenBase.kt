package hifumi.cresora.equipment
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.client.render.RenderLayer
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.screen.ScreenHandler
import net.minecraft.text.Text

abstract class ArtifactChestScreenBase<T : ScreenHandler>(
    handler: T,
    inventory: PlayerInventory,
    title: Text,
    private val rows: Int
) : HandledScreen<T>(handler, inventory, title) {

    init {
        backgroundWidth = 176
        backgroundHeight = 114 + rows * 18
        titleX = 8
        titleY = 6
        playerInventoryTitleX = 8
        playerInventoryTitleY = backgroundHeight - 94
    }

    protected open fun getBackgroundTexture(): net.minecraft.util.Identifier {
        return net.minecraft.util.Identifier.of("minecraft", "textures/gui/container/generic_54.png")
    }

    override fun drawBackground(context: DrawContext, delta: Float, mouseX: Int, mouseY: Int) {
        // バニラのスプライト Identifier
        val BACKGROUND_TEXTURE = net.minecraft.util.Identifier.of("minecraft", "container/generic_54")
        
        // 1.21.2 以降では net.minecraft.client.gl.RenderPipelines.GUI を指定する
        context.drawGuiTexture(net.minecraft.client.gl.RenderPipelines.GUI, BACKGROUND_TEXTURE, x, y, backgroundWidth, backgroundHeight)
    }

    override fun drawForeground(context: DrawContext, mouseX: Int, mouseY: Int) {
        context.drawText(textRenderer, title, titleX, titleY, 0xFF404040.toInt(), false)
        context.drawText(textRenderer, playerInventoryTitle, playerInventoryTitleX, playerInventoryTitleY, 0xFF404040.toInt(), false)
        drawExtraForeground(context, mouseX, mouseY)
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        renderBackground(context, mouseX, mouseY, delta)
        super.render(context, mouseX, mouseY, delta)
        drawMouseoverTooltip(context, mouseX, mouseY)
    }

    protected open fun drawExtraForeground(context: DrawContext, mouseX: Int, mouseY: Int) {}
}
