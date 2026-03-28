package hifumi.cresora

import dev.emi.trinkets.api.SlotReference
import dev.emi.trinkets.api.Trinket
import net.minecraft.component.type.TooltipDisplayComponent
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.item.tooltip.TooltipType
import net.minecraft.screen.SimpleNamedScreenHandlerFactory
import net.minecraft.text.MutableText
import net.minecraft.text.Text
import net.minecraft.util.ActionResult
import net.minecraft.util.Formatting
import net.minecraft.util.Hand
import net.minecraft.util.math.random.Random
import net.minecraft.world.World
import java.util.function.Consumer

open class ArtifactEquipmentItem(
    val definition: EquipmentDefinitionRef,
    settings: Settings
) : Item(settings), Trinket {

    override fun appendTooltip(
        stack: ItemStack,
        context: TooltipContext,
        displayComponent: TooltipDisplayComponent,
        textConsumer: Consumer<Text>,
        type: TooltipType
    ) {
        val resolvedDefinition = definition.resolve()
        val data = EquipmentStackSupport.getEquipmentData(stack)
            ?: EquipmentStackSupport.defaultEquipmentData(resolvedDefinition, EquipmentStackSupport.getCompatibilityLevel(stack))

        textConsumer.accept(Text.translatable(resolvedDefinition.setDefinition().translationKey()).formatted(Formatting.LIGHT_PURPLE))
        textConsumer.accept(
            Text.translatable(
                "item.cresora.equipment.slot_line",
                Text.translatable(resolvedDefinition.slotType().translationKey())
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

        val setDefinition = resolvedDefinition.setDefinition()
        for (bonus in setDefinition.allBonuses()) {
            textConsumer.accept(
                Text.translatable(
                    "item.cresora.equipment.set_bonus_line",
                    bonus.requiredPieces,
                    formatSetBonusDescription(bonus)
                ).formatted(Formatting.BLUE)
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
            EquipmentGenerationService.createEquipment(Random.create(), definition.resolve())
        )
        return stack
    }

    override fun canEquip(stack: ItemStack, ref: SlotReference, entity: LivingEntity): Boolean {
        val slotDefinition = definition.resolve().slotType()
        val slotType = ref.inventory().slotType
        return slotType.group == slotDefinition.trinketGroup && slotType.name == slotDefinition.trinketSlot
    }

    override fun use(world: World, user: PlayerEntity, hand: Hand): ActionResult {
        val resolvedDefinition = definition.resolve()
        if (!resolvedDefinition.opensUpgradeScreen || hand != Hand.MAIN_HAND) {
            return ActionResult.PASS
        }
        if (!world.isClient) {
            user.openHandledScreen(
                SimpleNamedScreenHandlerFactory(
                    { syncId, playerInventory, _ -> UpgradeScreenHandler(syncId, playerInventory) },
                    Text.translatable("screen.cresora.upgrade")
                )
            )
        }
        return ActionResult.SUCCESS
    }

    private fun formatSetBonusDescription(bonus: EquipmentSetBonus): Text {
        if (bonus.stats.isEmpty() && bonus.effectHooks.isEmpty()) {
            return Text.translatable("item.cresora.equipment.set_bonus_none")
        }

        val line: MutableText = Text.empty()
        var appendedParts = 0
        bonus.stats.forEachIndexed { index, statEntry ->
            if (index > 0) {
                line.append(Text.literal(", "))
            }
            line.append(EquipmentStatCalculator.formatStatLine(statEntry))
            appendedParts++
        }
        bonus.effectHooks.forEach { hook ->
            if (appendedParts > 0) {
                line.append(Text.literal(", "))
            }
            line.append(
                hook.descriptionKey()?.let(Text::translatable)
                    ?: Text.literal(hook.effectId)
            )
            appendedParts++
        }
        return line
    }
}
