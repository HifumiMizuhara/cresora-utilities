package hifumi.cresora

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.minecraft.entity.attribute.EntityAttributes
import net.minecraft.server.command.CommandManager.literal
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import net.minecraft.text.MutableText

object Commands {
    fun init() {
        CommandRegistrationCallback.EVENT.register { dispatcher, registryAccess, environment ->
            dispatcher.register(
                literal("about")
                    .executes { context ->
                        context.source.sendFeedback({Text.translatable("commands.cresora.about", version)} , false)
                        1
                    }
            )
            dispatcher.register(
                literal("cresora_stats")
                    .requires { source -> source.entity is ServerPlayerEntity }
                    .executes { context ->
                        val player = context.source.playerOrThrow
                        val totals = EquipmentPlayerSupport.getAggregatedStats(player)
                        val attackDamage = player.getAttributeValue(EntityAttributes.ATTACK_DAMAGE)
                        val maxHealth = player.getAttributeValue(EntityAttributes.MAX_HEALTH)
                        val armor = player.getAttributeValue(EntityAttributes.ARMOR)

                        context.source.sendFeedback({ Text.translatable("commands.cresora.stats.header") }, false)
                        context.source.sendFeedback({ Text.translatable("commands.cresora.stats.equipment_section") }, false)
                        context.source.sendFeedback(
                            { buildStatGroupLine(totals, StatType.ATK_FLAT, StatType.ATK_PERCENT, StatType.HP_FLAT, StatType.HP_PERCENT) },
                            false
                        )
                        context.source.sendFeedback(
                            { buildStatGroupLine(totals, StatType.DEF_FLAT, StatType.DEF_PERCENT, StatType.CRIT_RATE, StatType.CRIT_DMG) },
                            false
                        )
                        context.source.sendFeedback(
                            { buildStatGroupLine(totals, StatType.ALL_DMG_BONUS, StatType.DAMAGE_REDUCTION) },
                            false
                        )
                        context.source.sendFeedback({ Text.translatable("commands.cresora.stats.attribute_section") }, false)
                        context.source.sendFeedback(
                            { buildAttributeLine(attackDamage, maxHealth, armor) },
                            false
                        )
                        context.source.sendFeedback({ Text.translatable("commands.cresora.stats.base_note") }, false)
                        1
                    }
            )

        }
    }

    private fun buildStatGroupLine(totals: Map<StatType, Double>, vararg types: StatType): Text {
        val line: MutableText = Text.empty()
        types.forEachIndexed { index, type ->
            if (index > 0) {
                line.append(Text.literal(" | "))
            }
            line.append(statSegment(Text.translatable(type.translationKey()), formatValue(type, CombatStatSupport.effectiveDisplayValue(type, totals))))
        }
        return line
    }

    private fun buildAttributeLine(attackDamage: Double, maxHealth: Double, armor: Double): Text {
        return Text.empty()
            .append(statSegment(Text.translatable("commands.cresora.stats.attack_damage"), formatNumber(attackDamage)))
            .append(Text.literal(" | "))
            .append(statSegment(Text.translatable("commands.cresora.stats.max_health"), formatNumber(maxHealth)))
            .append(Text.literal(" | "))
            .append(statSegment(Text.translatable("commands.cresora.stats.armor"), formatNumber(armor)))
    }

    private fun statSegment(label: Text, value: String): MutableText {
        return Text.empty()
            .append(label)
            .append(Text.literal(" "))
            .append(Text.literal(value))
    }

    private fun formatValue(type: StatType, value: Double): String {
        val number = formatNumber(value)
        return if (type.isPercent()) "$number%" else number
    }

    private fun formatNumber(value: Double): String {
        val rounded = kotlin.math.round(value * 100.0) / 100.0
        return if (rounded % 1.0 == 0.0) {
            rounded.toInt().toString()
        } else {
            rounded.toString()
        }
    }
}
