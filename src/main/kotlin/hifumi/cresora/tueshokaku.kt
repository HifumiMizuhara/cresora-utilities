package hifumi.cresora

import net.minecraft.item.Item
import net.minecraft.item.Item.Settings
import net.minecraft.item.ItemStack
import net.minecraft.item.tooltip.TooltipType
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import net.minecraft.component.type.TooltipDisplayComponent
import java.util.function.Consumer
import kotlin.math.roundToInt

class tueshokaku(settings: Settings) : Item(settings) {
    companion object {
        const val MAX_LEVEL = 8

        fun normalizeLevel(stack: ItemStack): Int {
            val rawLevel = stack.getOrDefault(ModDataComponents.LEVEL, 1)
            val normalizedLevel = rawLevel.coerceIn(1, MAX_LEVEL)
            if (rawLevel != normalizedLevel) {
                stack.set(ModDataComponents.LEVEL, normalizedLevel)
            }
            return normalizedLevel
        }
    }

    override fun getDefaultStack(): ItemStack {
        val stack = super.getDefaultStack()
        // デフォルトでレベル1のコンポーネントを付与する
        stack.set(ModDataComponents.LEVEL, 1)
        return stack
    }
    override fun appendTooltip(
        stack: ItemStack,
        context: TooltipContext,
        displayComponent: TooltipDisplayComponent,
        textConsumer: Consumer<Text>,
        type: TooltipType
    ) {
        // データコンポーネントからレベルを取得
        val rawLevel = stack.getOrDefault(ModDataComponents.LEVEL, 1)
        val level = rawLevel.coerceIn(1, MAX_LEVEL)

        // ツールチップに「Level: X」と表示する
        val prob = (level / MAX_LEVEL.toDouble() * 1000).roundToInt() / 10.0
        textConsumer.accept(Text.literal("$prob%").formatted(Formatting.GRAY))
        super.appendTooltip(stack, context, displayComponent, textConsumer, type)
    }
}
