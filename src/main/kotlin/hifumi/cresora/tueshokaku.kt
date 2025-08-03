package hifumi.cresora

import net.minecraft.item.Item
import net.minecraft.item.Item.Settings
import net.minecraft.item.ItemStack
import net.minecraft.item.tooltip.TooltipType
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import kotlin.math.roundToInt

class tueshokaku(settings: Settings) : Item(settings) {
    override fun getDefaultStack(): ItemStack {
        val stack = super.getDefaultStack()
        // デフォルトでレベル1のコンポーネントを付与する
        stack.set(ModDataComponents.LEVEL, 1)
        return stack
    }
    override fun appendTooltip(
        stack: ItemStack,
        context: TooltipContext,
        tooltip: MutableList<Text>,
        type: TooltipType
    ) {
        // データコンポーネントからレベルを取得
        var level = stack.getOrDefault(ModDataComponents.LEVEL, 1)

        // ツールチップに「Level: X」と表示する
        if (level > 8) level = 8
        val possibility=listOf<Int>(1,2,3,4,5,6,7,8)
        val prob=(possibility[level-1]/8.0*1000).roundToInt()/10.0
        tooltip.add(Text.literal("$prob%").formatted(Formatting.GRAY))
        super.appendTooltip(stack, context, tooltip, type)
    }
}