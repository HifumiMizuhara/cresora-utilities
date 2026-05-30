package hifumi.cresora.domain

import hifumi.cresora.equipment.ArtifactChestScreenBase
import hifumi.cresora.leyline.LeyLineElement
import hifumi.cresora.leyline.LeyLineSelectionScreenHandler
import net.minecraft.client.gui.DrawContext
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.item.ItemStack
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import java.util.Locale

class LeyLineSelectionScreen(
    handler: LeyLineSelectionScreenHandler,
    inventory: PlayerInventory,
    title: Text
) : ArtifactChestScreenBase<LeyLineSelectionScreenHandler>(handler, inventory, title, 1) {

    override fun drawExtraForeground(context: DrawContext, mouseX: Int, mouseY: Int) {
        // Draw Adventure Rank
        context.drawText(
            textRenderer,
            Text.translatable("screen.cresora.domain.rank", handler.currentRank()),
            8,
            6,
            0xFF241C14.toInt(),
            false
        )
        // Draw Credits
        context.drawText(
            textRenderer,
            Text.translatable("screen.cresora.domain.credits", String.format(Locale.ROOT, "%,d", handler.currentCredits())),
            86,
            6,
            0xFF241C14.toInt(),
            false
        )
    }

    override fun getTooltipFromItem(stack: ItemStack): List<Text> {
        val list = super.getTooltipFromItem(stack).toMutableList()
        val slot = focusedSlot
        if (slot != null && slot.id in 0 until LeyLineSelectionScreenHandler.TIER_SLOT_COUNT) {
            val tier = slot.id + 1
            val unlockRank = handler.getTierUnlockRank(tier)
            val element = handler.getElement()

            list.add(Text.empty())
            list.add(Text.translatable("screen.cresora.leyline.req_rank", unlockRank).formatted(Formatting.GRAY))

            val dropsText = when (element) {
                LeyLineElement.WOOD -> "screen.cresora.leyline.drops_wood"
                LeyLineElement.FIRE -> "screen.cresora.leyline.drops_fire"
                LeyLineElement.WATER -> "screen.cresora.leyline.drops_water"
                LeyLineElement.GOLD -> "screen.cresora.leyline.drops_gold"
                LeyLineElement.SUN -> "screen.cresora.leyline.drops_sun"
                LeyLineElement.MOON -> "screen.cresora.leyline.drops_moon"
                LeyLineElement.EARTH -> "screen.cresora.leyline.drops_earth"
            }
            list.add(Text.translatable(dropsText).formatted(Formatting.GOLD))

            if (handler.currentRank() < unlockRank) {
                list.add(Text.translatable("screen.cresora.leyline.locked").formatted(Formatting.RED))
            } else {
                list.add(Text.translatable("screen.cresora.leyline.click_to_start").formatted(Formatting.GREEN))
            }
        }
        return list
    }
}
