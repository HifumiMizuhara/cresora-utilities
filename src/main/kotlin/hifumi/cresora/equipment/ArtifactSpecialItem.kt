package hifumi.cresora.equipment
import net.minecraft.component.DataComponentTypes
import net.minecraft.component.type.TooltipDisplayComponent
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.item.tooltip.TooltipType
import net.minecraft.text.Text
import java.util.function.Consumer

class ArtifactSpecialItem(
    val definitionId: String,
    settings: Settings
) : Item(settings) {
    override fun getName(stack: ItemStack): Text {
        return Text.translatable(ArtifactSpecialItemRegistry.definition(definitionId).translationKeyId)
    }

    override fun appendTooltip(
        stack: ItemStack,
        context: TooltipContext,
        displayComponent: TooltipDisplayComponent,
        textConsumer: Consumer<Text>,
        type: TooltipType
    ) {
        val definition = ArtifactSpecialItemRegistry.definition(definitionId)
        textConsumer.accept(Text.translatable("item.cresora.special_item.shop_price", formatWholeNumber(definition.shopPrice)))
        definition.mobDrop?.let { mobDrop ->
            textConsumer.accept(
                Text.translatable(
                    "item.cresora.special_item.mob_drop",
                    mobDrop.rankMin,
                    mobDrop.rankMax,
                    formatPercent(mobDrop.maxChance)
                )
            )
        }
        super.appendTooltip(stack, context, displayComponent, textConsumer, type)
    }

    override fun getDefaultStack(): ItemStack {
        val stack = super.getDefaultStack()
        stack.set(DataComponentTypes.CUSTOM_NAME, Text.translatable(ArtifactSpecialItemRegistry.definition(definitionId).translationKeyId))
        return stack
    }

    companion object {
        fun formatWholeNumber(value: Int): String = "%,d".format(java.util.Locale.ROOT, value)

        fun formatPercent(value: Double): String = "%.1f%%".format(java.util.Locale.ROOT, value * 100.0)
    }
}
