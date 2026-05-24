package hifumi.cresora.weapon
import hifumi.cresora.equipment.ArtifactUiFlow
import net.minecraft.component.type.TooltipDisplayComponent
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.item.tooltip.TooltipType
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
        textConsumer.accept(
            Text.translatable(
                "item.cresora.weapon.role",
                Text.translatable(resolved.role.translationKey())
            ).formatted(Formatting.GRAY)
        )
        textConsumer.accept(Text.translatable("item.cresora.weapon.base_level", data.baseLevel, resolved.maxBaseLevel).formatted(Formatting.GRAY))
        textConsumer.accept(Text.translatable("item.cresora.weapon.skill_level", data.skillLevel, resolved.maxSkillLevel).formatted(Formatting.GRAY))
        textConsumer.accept(
            Text.translatable(
                "item.cresora.weapon.attack",
                formatNumber(WeaponCombatSupport.attackDamage(resolved, data))
            ).formatted(Formatting.RED)
        )
        textConsumer.accept(
            Text.translatable(
                "item.cresora.weapon.damage_type",
                Text.translatable(resolved.damageType.translationKey())
            ).formatted(Formatting.GRAY)
        )
        if (resolved.critRateBonusPercent > 0.0) {
            textConsumer.accept(
                Text.translatable(
                    "item.cresora.weapon.crit_rate_bonus",
                    formatNumber(resolved.critRateBonusPercent)
                ).formatted(Formatting.YELLOW)
            )
        }
        val currentAllDamageBonus = WeaponCombatSupport.allDamageBonusPercent(resolved, data)
        if (currentAllDamageBonus > 0.0) {
            textConsumer.accept(
                Text.translatable(
                    "item.cresora.weapon.all_damage_bonus",
                    formatNumber(currentAllDamageBonus)
                ).formatted(Formatting.LIGHT_PURPLE)
            )
        }
        textConsumer.accept(
            buildSkillTooltipLine(resolved, data).copy().formatted(Formatting.AQUA)
        )
        if (resolved.id == "hanwu_juanxue") {
            textConsumer.accept(Text.translatable("item.cresora.weapon.passive.hanwu_juanxue").formatted(Formatting.BLUE))
            textConsumer.accept(Text.translatable("item.cresora.weapon.special.hanwu_juanxue").formatted(Formatting.WHITE))
        }
        if (resolved.id == "cadenza_allegro") {
            textConsumer.accept(Text.translatable("item.cresora.weapon.passive.cadenza_allegro").formatted(Formatting.BLUE))
        }
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
                ArtifactUiFlow.openWeaponUpgrade(user as ServerPlayerEntity)
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
        val heartValue = formatNumber(WeaponCombatSupport.skillValueHearts(definition, data))
        val secondaryHeartValue = formatNumber(WeaponCombatSupport.secondarySkillValueHearts(definition, data))
        val percentValue = formatNumber(WeaponCombatSupport.skillValuePercent(definition, data))
        val secondaryPercentValue = formatNumber(WeaponCombatSupport.secondarySkillValuePercent(definition, data))
        return when (definition.skill.effectId) {
            "heal" -> Text.translatable(
                "item.cresora.weapon.skill_line_heal",
                effectName,
                heartValue,
                Text.translatable("item.cresora.weapon.unit.hearts"),
                definition.skill.cooldownSeconds
            )
            "flame_aura" -> Text.translatable(
                "item.cresora.weapon.skill_line_flame_aura",
                effectName,
                percentValue,
                Text.translatable("item.cresora.weapon.unit.seconds"),
                formatNumber(definition.skill.radiusMeters),
                definition.skill.durationSeconds,
                definition.skill.cooldownSeconds
            )
            "current_hp_true_damage" -> Text.translatable(
                "item.cresora.weapon.skill_line_percent_burst",
                effectName,
                percentValue,
                Text.translatable("item.cresora.weapon.unit.percent_current_hp"),
                formatNumber(definition.skill.radiusMeters),
                definition.skill.cooldownSeconds
            )
            "snow_frost" -> Text.translatable(
                "item.cresora.weapon.skill_line_snow_frost",
                effectName,
                heartValue,
                Text.translatable("item.cresora.weapon.unit.hearts"),
                definition.skill.durationSeconds,
                definition.skill.cooldownSeconds
            )
            "healing_aura" -> Text.translatable(
                "item.cresora.weapon.skill_line_healing_aura",
                effectName,
                heartValue,
                Text.translatable("item.cresora.weapon.unit.hearts"),
                secondaryHeartValue,
                Text.translatable("item.cresora.weapon.unit.hearts"),
                formatNumber(definition.skill.radiusMeters),
                definition.skill.durationSeconds,
                definition.skill.cooldownSeconds
            )
            "dark_lux" -> Text.translatable(
                "item.cresora.weapon.skill_line_dark_lux",
                effectName,
                definition.skill.durationSeconds,
                definition.skill.cooldownSeconds
            )
            "sunlit_haste" -> Text.translatable(
                "item.cresora.weapon.skill_line_sunlit_haste",
                effectName,
                percentValue,
                secondaryPercentValue,
                definition.skill.durationSeconds,
                definition.skill.cooldownSeconds
            )
            "baa_mimic" -> {
                val targetCount = when {
                    data.baseLevel >= 61 -> 3
                    data.baseLevel >= 41 -> 2
                    else -> 1
                }
                Text.translatable(
                    "item.cresora.weapon.skill_line_baa_mimic",
                    effectName,
                    targetCount,
                    formatNumber(definition.skill.radiusMeters),
                    definition.skill.cooldownSeconds
                )
            }
            "orchid_pavilion_echo" -> Text.translatable(
                "item.cresora.weapon.skill_line_orchid_pavilion_echo",
                effectName,
                definition.skill.durationSeconds,
                formatNumber(definition.skill.tickIntervalSeconds),
                definition.skill.cooldownSeconds
            )
            "none" -> Text.translatable(
                "item.cresora.weapon.skill_line_none",
                effectName
            )
            else -> Text.translatable(
                "item.cresora.weapon.skill_line_shield",
                effectName,
                heartValue,
                Text.translatable("item.cresora.weapon.unit.hearts"),
                definition.skill.durationSeconds,
                definition.skill.cooldownSeconds
            )
        }
    }
}
