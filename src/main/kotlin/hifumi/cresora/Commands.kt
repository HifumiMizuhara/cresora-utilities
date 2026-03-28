package hifumi.cresora

import com.mojang.brigadier.arguments.IntegerArgumentType.getInteger
import com.mojang.brigadier.arguments.IntegerArgumentType.integer
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.minecraft.command.argument.EntityArgumentType
import net.minecraft.entity.attribute.EntityAttributes
import net.minecraft.server.command.CommandManager.argument
import net.minecraft.server.command.CommandManager.literal
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.MutableText
import net.minecraft.text.Text
import java.util.Locale

object Commands {
    fun init() {
        CommandRegistrationCallback.EVENT.register { dispatcher, _, _ ->
            dispatcher.register(
                literal("about")
                    .executes { context ->
                        context.source.sendFeedback({ Text.translatable("commands.cresora.about", version) }, false)
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
                        context.source.sendFeedback({ buildRankSummaryLine(player) }, false)
                        context.source.sendFeedback({ buildCreditsSummaryLine(player) }, false)
                        context.source.sendFeedback({ Text.translatable("commands.cresora.stats.equipment_section") }, false)
                        context.source.sendFeedback({ buildSetSummaryLine(player) }, false)
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

            dispatcher.register(
                literal("cresora_rank")
                    .executes { context ->
                        showRank(context.source, context.source.playerOrThrow)
                        1
                    }
                    .then(
                        literal("set")
                            .requires { source -> source.hasPermissionLevel(2) }
                            .then(
                                argument("player", EntityArgumentType.player())
                                    .then(
                                        argument("rank", integer(AdventureRankProgression.MIN_RANK, AdventureRankProgression.MAX_RANK))
                                            .executes { context ->
                                                val target = EntityArgumentType.getPlayer(context, "player")
                                                val rank = getInteger(context, "rank")
                                                AdventureRankService.setRank(target, rank)
                                                context.source.sendFeedback(
                                                    {
                                                        Text.translatable(
                                                            "commands.cresora.rank.set_feedback",
                                                            target.displayName,
                                                            AdventureRankService.getProgress(target).rank
                                                        )
                                                    },
                                                    true
                                                )
                                                1
                                            }
                                    )
                            )
                    )
                    .then(
                        literal("addxp")
                            .requires { source -> source.hasPermissionLevel(2) }
                            .then(
                                argument("player", EntityArgumentType.player())
                                    .then(
                                        argument("value", integer(1))
                                            .executes { context ->
                                                val target = EntityArgumentType.getPlayer(context, "player")
                                                val value = getInteger(context, "value")
                                                AdventureRankService.addRawXp(target, value)
                                                val progress = AdventureRankService.getProgress(target)
                                                context.source.sendFeedback(
                                                    { buildAdminRankFeedback("commands.cresora.rank.addxp_feedback", target, progress, value) },
                                                    true
                                                )
                                                1
                                            }
                                    )
                            )
                    )
                    .then(
                        literal("setxp")
                            .requires { source -> source.hasPermissionLevel(2) }
                            .then(
                                argument("player", EntityArgumentType.player())
                                    .then(
                                        argument("value", integer(0))
                                            .executes { context ->
                                                val target = EntityArgumentType.getPlayer(context, "player")
                                                val value = getInteger(context, "value")
                                                AdventureRankService.setProgressXp(target, value)
                                                val progress = AdventureRankService.getProgress(target)
                                                context.source.sendFeedback(
                                                    { buildAdminRankFeedback("commands.cresora.rank.setxp_feedback", target, progress, value) },
                                                    true
                                                )
                                                1
                                            }
                                    )
                            )
                    )
            )

            dispatcher.register(
                literal("cresora_credits")
                    .executes { context ->
                        showCredits(context.source, context.source.playerOrThrow)
                        1
                    }
                    .then(
                        literal("get")
                            .requires { source -> source.hasPermissionLevel(2) }
                            .then(
                                argument("player", EntityArgumentType.player())
                                    .executes { context ->
                                        val target = EntityArgumentType.getPlayer(context, "player")
                                        context.source.sendFeedback(
                                            { buildCreditsTargetLine(target) },
                                            false
                                        )
                                        1
                                    }
                            )
                    )
                    .then(
                        literal("add")
                            .requires { source -> source.hasPermissionLevel(2) }
                            .then(
                                argument("player", EntityArgumentType.player())
                                    .then(
                                        argument("amount", integer(1))
                                            .executes { context ->
                                                val target = EntityArgumentType.getPlayer(context, "player")
                                                val amount = getInteger(context, "amount")
                                                val total = CreditsService.addCredits(target, amount)
                                                context.source.sendFeedback(
                                                    {
                                                        Text.translatable(
                                                            "commands.cresora.credits.add_feedback",
                                                            formatWholeNumber(amount),
                                                            target.displayName,
                                                            formatWholeNumber(total)
                                                        )
                                                    },
                                                    true
                                                )
                                                1
                                            }
                                    )
                            )
                    )
                    .then(
                        literal("set")
                            .requires { source -> source.hasPermissionLevel(2) }
                            .then(
                                argument("player", EntityArgumentType.player())
                                    .then(
                                        argument("amount", integer(0))
                                            .executes { context ->
                                                val target = EntityArgumentType.getPlayer(context, "player")
                                                val amount = getInteger(context, "amount")
                                                val total = CreditsService.setCredits(target, amount)
                                                context.source.sendFeedback(
                                                    {
                                                        Text.translatable(
                                                            "commands.cresora.credits.set_feedback",
                                                            target.displayName,
                                                            formatWholeNumber(total)
                                                        )
                                                    },
                                                    true
                                                )
                                                1
                                            }
                                    )
                            )
                    )
            )

            dispatcher.register(
                literal("cresora_shop")
                    .requires { source -> source.entity is ServerPlayerEntity }
                    .executes { context ->
                        ArtifactUiFlow.openShop(context.source.playerOrThrow)
                        1
                    }
            )

            dispatcher.register(
                literal("cresora_domain")
                    .requires { source -> source.entity is ServerPlayerEntity }
                    .executes { context ->
                        ArtifactUiFlow.openDomainSelection(context.source.playerOrThrow)
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
            line.append(
                statSegment(
                    Text.translatable(type.translationKey()),
                    formatValue(type, CombatStatSupport.effectiveDisplayValue(type, totals))
                )
            )
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

    private fun buildRankSummaryLine(player: ServerPlayerEntity): Text {
        val progress = AdventureRankService.getProgress(player)
        return if (progress.isMaxRank()) {
            Text.translatable("commands.cresora.stats.rank_line_max", progress.rank)
        } else {
            Text.translatable("commands.cresora.stats.rank_line", progress.rank, progress.currentXp, progress.requiredXp)
        }
    }

    private fun buildCreditsSummaryLine(player: ServerPlayerEntity): Text {
        return Text.translatable("commands.cresora.stats.credits_line", formatWholeNumber(CreditsService.getCredits(player)))
    }

    private fun buildSetSummaryLine(player: ServerPlayerEntity): Text {
        val summaries = EquipmentPlayerSupport.getActiveSetSummaries(player)
        if (summaries.isEmpty()) {
            return Text.translatable("commands.cresora.stats.sets_none")
        }

        val line = Text.empty().append(Text.translatable("commands.cresora.stats.sets_label")).append(Text.literal(" "))
        summaries.forEachIndexed { index, summary ->
            if (index > 0) {
                line.append(Text.literal(" | "))
            }
            val thresholds = summary.activeThresholds.joinToString("/")
            line.append(
                Text.translatable(
                    "commands.cresora.stats.sets_entry",
                    Text.translatable(summary.set.translationKey()),
                    summary.pieceCount,
                    EquipmentContentRegistry.equipmentCountForSet(summary.set.id),
                    thresholds
                )
            )
        }
        return line
    }

    private fun showRank(source: ServerCommandSource, player: ServerPlayerEntity) {
        val progress = AdventureRankService.getProgress(player)
        source.sendFeedback({ Text.translatable("commands.cresora.rank.header") }, false)
        source.sendFeedback(
            {
                if (progress.isMaxRank()) {
                    Text.translatable("commands.cresora.rank.current_max", progress.rank)
                } else {
                    Text.translatable(
                        "commands.cresora.rank.current",
                        progress.rank,
                        progress.currentXp,
                        progress.requiredXp,
                        (progress.requiredXp ?: 0) - progress.currentXp
                    )
                }
            },
            false
        )
    }

    private fun showCredits(source: ServerCommandSource, player: ServerPlayerEntity) {
        source.sendFeedback({ Text.translatable("commands.cresora.credits.header") }, false)
        source.sendFeedback({ buildCreditsSummaryLine(player) }, false)
    }

    private fun buildCreditsTargetLine(player: ServerPlayerEntity): Text {
        return Text.translatable(
            "commands.cresora.credits.target",
            player.displayName,
            formatWholeNumber(CreditsService.getCredits(player))
        )
    }

    private fun buildAdminRankFeedback(
        key: String,
        player: ServerPlayerEntity,
        progress: AdventureRankProgression.Progress,
        value: Int
    ): Text {
        return if (progress.isMaxRank()) {
            Text.translatable(key, player.displayName, value, progress.rank, 0, 0)
        } else {
            Text.translatable(key, player.displayName, value, progress.rank, progress.currentXp, progress.requiredXp)
        }
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

    private fun formatWholeNumber(value: Int): String {
        return String.format(Locale.ROOT, "%,d", value)
    }
}
