package hifumi.cresora

import net.minecraft.component.type.TooltipDisplayComponent
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.item.tooltip.TooltipType
import net.minecraft.screen.SimpleNamedScreenHandlerFactory
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import net.minecraft.util.ActionResult
import net.minecraft.util.Formatting
import net.minecraft.util.Hand
import net.minecraft.world.World
import java.util.Locale
import java.util.function.Consumer

class CresoraWeaponItem(
    val definition: WeaponDefinitionRef,
    settings: Settings
) : Item(settings) {
    override fun getDefaultStack(): ItemStack {
        val stack = super.getDefaultStack()
        WeaponStackSupport.syncWeaponData(stack, WeaponStackSupport.defaultWeaponData(definition.resolve()))
        return stack
    }

    override fun appendTooltip(
        stack: ItemStack,
        context: TooltipContext,
        displayComponent: TooltipDisplayComponent,
        textConsumer: Consumer<Text>,
        type: TooltipType
    ) {
        val resolved = definition.resolve()
        val data = WeaponStackSupport.getWeaponData(stack) ?: WeaponStackSupport.defaultWeaponData(resolved)
        textConsumer.accept(Text.translatable(data.rarity.translationKey()).formatted(Formatting.GOLD))
        textConsumer.accept(Text.translatable("item.cresora.weapon.base_level", data.baseLevel, resolved.maxBaseLevel).formatted(Formatting.GRAY))
        textConsumer.accept(Text.translatable("item.cresora.weapon.skill_level", data.skillLevel, resolved.maxSkillLevel).formatted(Formatting.GRAY))
        textConsumer.accept(
            Text.translatable(
                "item.cresora.weapon.attack",
                formatNumber(WeaponCombatSupport.attackDamage(resolved, data))
            ).formatted(Formatting.RED)
        )
        textConsumer.accept(
            buildSkillTooltipLine(resolved, data).copy().formatted(Formatting.AQUA)
        )
        textConsumer.accept(Text.translatable("item.cresora.weapon.sneak_upgrade").formatted(Formatting.DARK_GREEN))
        super.appendTooltip(stack, context, displayComponent, textConsumer, type)
    }

    override fun use(world: World, user: PlayerEntity, hand: Hand): ActionResult {
        val stack = user.getStackInHand(hand)
        if (hand != Hand.MAIN_HAND) {
            return ActionResult.PASS
        }
        if (user.isSneaking) {
            if (!world.isClient) {
                user.openHandledScreen(
                    SimpleNamedScreenHandlerFactory(
                        { syncId, playerInventory, _ -> WeaponUpgradeScreenHandler(syncId, playerInventory) },
                        Text.translatable("screen.cresora.weapon_upgrade")
                    )
                )
            }
            return ActionResult.SUCCESS
        }
        if (world.isClient) {
            return ActionResult.SUCCESS
        }
        val serverPlayer = user as? ServerPlayerEntity ?: return ActionResult.FAIL
        return WeaponSkillService.tryActivate(serverPlayer, stack)
    }

    private fun formatNumber(value: Double): String {
        return String.format(Locale.ROOT, "%.1f", value)
    }

    private fun buildSkillTooltipLine(definition: WeaponDefinition, data: WeaponData): Text {
        val effectName = Text.translatable("item.cresora.weapon.skill.${definition.skill.effectId}")
        val value = formatNumber(WeaponCombatSupport.skillValueHearts(definition, data))
        return when (definition.skill.effectId) {
            "heal" -> Text.translatable(
                "item.cresora.weapon.skill_line_heal",
                effectName,
                value,
                Text.translatable("item.cresora.weapon.unit.hearts"),
                definition.skill.cooldownSeconds
            )
            else -> Text.translatable(
                "item.cresora.weapon.skill_line_shield",
                effectName,
                value,
                Text.translatable("item.cresora.weapon.unit.hearts"),
                definition.skill.durationSeconds,
                definition.skill.cooldownSeconds
            )
        }
    }
}
