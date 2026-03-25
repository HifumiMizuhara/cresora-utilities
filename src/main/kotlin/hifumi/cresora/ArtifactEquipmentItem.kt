package hifumi.cresora

import dev.emi.trinkets.api.SlotReference
import dev.emi.trinkets.api.Trinket
import net.minecraft.component.type.TooltipDisplayComponent
import net.minecraft.entity.LivingEntity
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.item.tooltip.TooltipType
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import net.minecraft.util.math.random.Random
import java.util.function.Consumer

open class ArtifactEquipmentItem(
    val definition: EquipmentDefinition,
    settings: Settings
) : Item(settings), Trinket {

    override fun appendTooltip(
        stack: ItemStack,
        context: TooltipContext,
        displayComponent: TooltipDisplayComponent,
        textConsumer: Consumer<Text>,
        type: TooltipType
    ) {
        val data = EquipmentStackSupport.getEquipmentData(stack)
            ?: EquipmentStackSupport.defaultEquipmentData(definition, EquipmentStackSupport.getCompatibilityLevel(stack))

        textConsumer.accept(Text.translatable(definition.setId.translationKey()).formatted(Formatting.LIGHT_PURPLE))
        textConsumer.accept(
            Text.translatable(
                "item.cresora.equipment.slot_line",
                Text.translatable(definition.slotType.translationKey())
            ).formatted(Formatting.DARK_AQUA)
        )
        textConsumer.accept(Text.translatable(data.rarity.translationKey()).formatted(Formatting.GOLD))
        textConsumer.accept(
            Text.translatable("item.cresora.equipment.level", data.level, data.rarity.maxLevel).formatted(Formatting.GRAY)
        )
        textConsumer.accept(
            Text.translatable("item.cresora.equipment.main_stat", EquipmentStatCalculator.formatStatLine(data.mainStat)).formatted(Formatting.AQUA)
        )

        for (subStat in data.subStats) {
            textConsumer.accept(
                Text.translatable("item.cresora.equipment.sub_stat", EquipmentStatCalculator.formatStatLine(subStat)).formatted(Formatting.GRAY)
            )
        }

        val nextGrowthLevel = ((data.level / 4) + 1) * 4
        if (nextGrowthLevel <= data.rarity.maxLevel) {
            textConsumer.accept(
                Text.translatable("item.cresora.equipment.next_growth", nextGrowthLevel).formatted(Formatting.DARK_GREEN)
            )
        } else {
            textConsumer.accept(Text.translatable("item.cresora.equipment.maxed").formatted(Formatting.DARK_GREEN))
        }

        super.appendTooltip(stack, context, displayComponent, textConsumer, type)
    }

    override fun getDefaultStack(): ItemStack {
        val stack = super.getDefaultStack()
        EquipmentStackSupport.syncEquipmentData(
            stack,
            EquipmentGenerationService.createEquipment(Random.create(), definition)
        )
        return stack
    }

    override fun canEquip(stack: ItemStack, ref: SlotReference, entity: LivingEntity): Boolean {
        val slotType = ref.inventory().slotType
        return slotType.group == definition.slotType.trinketGroup && slotType.name == definition.slotType.trinketSlot
    }
}
