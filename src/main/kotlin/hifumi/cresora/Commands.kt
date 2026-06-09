package hifumi.cresora

import hifumi.cresora.adventurerank.AdventureRankProgression
import hifumi.cresora.adventurerank.AdventureRankService
import hifumi.cresora.bloodmoon.BloodMoonService
import hifumi.cresora.bloodmoon.MoonPhase
import hifumi.cresora.bloodmoon.MoonPhaseService
import hifumi.cresora.bloodmoon.SpecialMoonPhase
import hifumi.cresora.combat.CombatStatSupport
import hifumi.cresora.credits.CreditsService
import hifumi.cresora.equipment.ArtifactUiFlow
import hifumi.cresora.equipment.EquipmentContentRegistry
import hifumi.cresora.equipment.EquipmentPlayerSupport
import hifumi.cresora.resonance.ResonanceCurrencyType
import hifumi.cresora.resonance.ResonanceService
import hifumi.cresora.story.StoryContentRegistry
import hifumi.cresora.story.StoryProgressService
import hifumi.cresora.story.StoryService
import hifumi.cresora.story.StoryTextRegistry
import com.mojang.brigadier.arguments.IntegerArgumentType.getInteger
import com.mojang.brigadier.arguments.IntegerArgumentType.integer
import com.mojang.brigadier.arguments.StringArgumentType.getString
import com.mojang.brigadier.arguments.StringArgumentType.word
import com.mojang.brigadier.suggestion.SuggestionProvider
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
    private val MOON_PHASE_SUGGESTIONS = SuggestionProvider<ServerCommandSource> { _, builder ->
        MoonPhase.entries.forEach { builder.suggest(it.id) }
        builder.buildFuture()
    }
    private val SPECIAL_MOON_SUGGESTIONS = SuggestionProvider<ServerCommandSource> { _, builder ->
        SpecialMoonPhase.entries.forEach { builder.suggest(it.id) }
        builder.buildFuture()
    }

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
                literal("cresora")
                    .requires { source -> source.entity is ServerPlayerEntity }
                    .executes { context ->
                        ArtifactUiFlow.openMenu(context.source.playerOrThrow)
                        1
                    }
                    .then(
                        literal("moon")
                            .requires { source -> source.hasPermissionLevel(2) }
                            .executes { context ->
                                showMoon(context.source)
                                1
                            }
                            .then(
                                literal("set")
                                    .then(
                                        argument("phase", word())
                                            .suggests(MOON_PHASE_SUGGESTIONS)
                                            .executes { context ->
                                                val server = context.source.server
                                                val phaseName = getString(context, "phase")
                                                val phase = MoonPhase.fromId(phaseName)
                                                if (phase == null) {
                                                    context.source.sendFeedback({ Text.translatable("commands.cresora.moon.invalid", phaseName, MoonPhase.IDS) }, false)
                                                    return@executes 0
                                                }
                                                MoonPhaseService.setPhaseOffset(server, phase)
                                                context.source.sendFeedback({ Text.translatable("commands.cresora.moon.set", Text.literal(phase.id), Text.literal(phase.displayName)) }, true)
                                                1
                                            }
                                    )
                            )
                            .then(
                                literal("clear_special")
                                    .executes { context ->
                                        val server = context.source.server
                                        val changed = BloodMoonService.stopAndClearNight(server).also {
                                            if (!it) {
                                                MoonPhaseService.clearSpecialMoon(server)
                                            }
                                        }
                                        MoonPhaseService.refreshLoadedHostiles(server)
                                        context.source.sendFeedback({ Text.translatable("commands.cresora.moon.cleared_special") }, true)
                                        if (changed) 1 else 0
                                    }
                            )
                            .then(
                                literal("stop_blood_moon")
                                    .executes { context ->
                                        val server = context.source.server
                                        val moonChanged = MoonPhaseService.clearBloodMoon(server)
                                        val battleChanged = BloodMoonService.debugStop(server)
                                        MoonPhaseService.refreshLoadedHostiles(server)
                                        val feedbackKey = if (moonChanged || battleChanged) {
                                            "commands.cresora.moon.stopped_blood_moon"
                                        } else {
                                            "commands.cresora.moon.no_blood_moon"
                                        }
                                        context.source.sendFeedback({ Text.translatable(feedbackKey) }, true)
                                        1
                                    }
                            )
                            .then(
                                literal("set_special")
                                    .then(
                                        argument("special", word())
                                            .suggests(SPECIAL_MOON_SUGGESTIONS)
                                            .executes { context ->
                                                val server = context.source.server
                                                val specialName = getString(context, "special")
                                                val special = SpecialMoonPhase.fromId(specialName)
                                                if (special == null) {
                                                    context.source.sendFeedback({ Text.translatable("commands.cresora.moon.invalid_special", specialName, SpecialMoonPhase.IDS) }, false)
                                                    return@executes 0
                                                }
                                                if (BloodMoonService.hasActiveBattle() && special != SpecialMoonPhase.BLOOD_MOON) {
                                                    context.source.sendError(Text.translatable("commands.cresora.moon.blocked_during_blood_war"))
                                                    return@executes 0
                                                }
                                                MoonPhaseService.setSpecialMoon(server, special)
                                                MoonPhaseService.refreshLoadedHostiles(server)
                                                context.source.sendFeedback({ Text.translatable("commands.cresora.moon.set_special", Text.literal(special.id), Text.literal(special.displayName)) }, true)
                                                1
                                            }
                                    )
                            )
                    )
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
                            { buildStatGroupLine(totals, StatType.ALL_DMG_BONUS, StatType.PHYSICAL_RESISTANCE, StatType.ARCANE_RESISTANCE) },
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

            dispatcher.register(
                literal("cresora_masquerade")
                    .requires { source -> source.entity is ServerPlayerEntity }
                    .executes { context ->
                        ArtifactUiFlow.openMasqueradeLoadout(context.source.playerOrThrow)
                        1
                    }
            )

            dispatcher.register(
                literal("cresora_story")
                    .requires { source -> source.entity is ServerPlayerEntity }
                    .executes { context ->
                        ArtifactUiFlow.openStoryChapterSelection(context.source.playerOrThrow)
                        1
                    }
                    .then(
                        literal("list")
                            .executes { context ->
                                showStoryChapters(context.source)
                                1
                            }
                    )
                    .then(
                        literal("start")
                            .then(
                                argument("chapter_id", word())
                                    .executes { context ->
                                        val player = context.source.playerOrThrow
                                        val chapterId = getString(context, "chapter_id")
                                        val result = StoryService.startSession(player, chapterId)
                                        context.source.sendFeedback(
                                            { Text.translatable(result.translationKey, *result.args.toTypedArray()) },
                                            false
                                        )
                                        if (result.success) 1 else 0
                                    }
                            )
                    )
            )

            dispatcher.register(
                literal("cresora_resonance")
                    .executes { context ->
                        ArtifactUiFlow.openResonance(context.source.playerOrThrow)
                        1
                    }
                    .then(
                        literal("currency")
                            .executes { context ->
                                showResonanceCurrency(context.source, context.source.playerOrThrow)
                                1
                            }
                    )
                    .then(
                        literal("get")
                            .requires { source -> source.hasPermissionLevel(2) }
                            .then(
                                argument("player", EntityArgumentType.player())
                                    .executes { context ->
                                        val target = EntityArgumentType.getPlayer(context, "player")
                                        context.source.sendFeedback({ buildResonanceTargetLine(target) }, false)
                                        1
                                    }
                            )
                    )
                    .then(
                        literal("add")
                            .requires { source -> source.hasPermissionLevel(2) }
                            .then(resonanceCurrencyLiteral(1, "commands.cresora.resonance.add_feedback") { target, type, amount ->
                                ResonanceService.addCurrency(target, type, amount)
                            })
                    )
                    .then(
                        literal("set")
                            .requires { source -> source.hasPermissionLevel(2) }
                            .then(resonanceCurrencyLiteral(0, "commands.cresora.resonance.set_feedback") { target, type, amount ->
                                ResonanceService.setCurrency(target, type, amount)
                            })
                    )
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

    private fun showResonanceCurrency(source: ServerCommandSource, player: ServerPlayerEntity) {
        source.sendFeedback({ Text.translatable("commands.cresora.resonance.header") }, false)
        source.sendFeedback({ buildResonanceCurrencyLine(player) }, false)
    }

    private fun showMoon(source: ServerCommandSource) {
        source.sendFeedback({ Text.translatable("commands.cresora.moon.header") }, false)
        source.sendFeedback({ MoonPhaseService.summaryLine(source.server) }, false)
    }

    private fun buildCreditsTargetLine(player: ServerPlayerEntity): Text {
        return Text.translatable(
            "commands.cresora.credits.target",
            player.displayName,
            formatWholeNumber(CreditsService.getCredits(player))
        )
    }

    private fun showStoryChapters(source: ServerCommandSource) {
        source.sendFeedback({ Text.translatable("commands.cresora.story.header") }, false)
        val player = source.player
        val locale = StoryTextRegistry.resolvePlayerLocale(player)
        for (chapter in StoryContentRegistry.chapters()) {
            val missingPrerequisite = if (player != null) StoryProgressService.missingPrerequisite(player, chapter) else chapter.prerequisiteChapterId
            val title = StoryTextRegistry.chapterTitle(locale, chapter)
            val prerequisiteLabel = missingPrerequisite?.let {
                runCatching { StoryTextRegistry.chapterLabel(locale, StoryContentRegistry.requireChapter(it)) }.getOrDefault(it)
            }
            source.sendFeedback(
                {
                    if (prerequisiteLabel != null) {
                        Text.translatable("commands.cresora.story.entry_prerequisite", chapter.displayName, title, prerequisiteLabel)
                    } else {
                        Text.translatable(
                            "commands.cresora.story.entry",
                            chapter.displayName,
                            title,
                            chapter.unlockRank
                        )
                    }
                },
                false
            )
        }
    }

    private fun buildResonanceTargetLine(player: ServerPlayerEntity): Text {
        return Text.translatable(
            "commands.cresora.resonance.target",
            player.displayName,
            formatWholeNumber(ResonanceService.getCurrency(player, ResonanceCurrencyType.CHORD_PROGRESSION)),
            formatWholeNumber(ResonanceService.getCurrency(player, ResonanceCurrencyType.SUBSTITUTE_CHORD))
        )
    }

    private fun buildResonanceCurrencyLine(player: ServerPlayerEntity): Text {
        return Text.translatable(
            "commands.cresora.resonance.line",
            formatWholeNumber(ResonanceService.getCurrency(player, ResonanceCurrencyType.CHORD_PROGRESSION)),
            formatWholeNumber(ResonanceService.getCurrency(player, ResonanceCurrencyType.SUBSTITUTE_CHORD))
        )
    }

    private fun resonanceCurrencyLiteral(
        minimumAmount: Int,
        feedbackKey: String,
        operation: (ServerPlayerEntity, ResonanceCurrencyType, Int) -> Int
    ) = argument("player", EntityArgumentType.player())
        .then(
            literal(ResonanceCurrencyType.CHORD_PROGRESSION.id)
                .then(
                    argument("amount", integer(minimumAmount))
                        .executes { context ->
                            val target = EntityArgumentType.getPlayer(context, "player")
                            val amount = getInteger(context, "amount")
                            val total = operation(target, ResonanceCurrencyType.CHORD_PROGRESSION, amount)
                            context.source.sendFeedback(
                                {
                                    Text.translatable(
                                        feedbackKey,
                                        Text.translatable(ResonanceCurrencyType.CHORD_PROGRESSION.translationKey),
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
        .then(
            literal(ResonanceCurrencyType.SUBSTITUTE_CHORD.id)
                .then(
                    argument("amount", integer(minimumAmount))
                        .executes { context ->
                            val target = EntityArgumentType.getPlayer(context, "player")
                            val amount = getInteger(context, "amount")
                            val total = operation(target, ResonanceCurrencyType.SUBSTITUTE_CHORD, amount)
                            context.source.sendFeedback(
                                {
                                    Text.translatable(
                                        feedbackKey,
                                        Text.translatable(ResonanceCurrencyType.SUBSTITUTE_CHORD.translationKey),
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
