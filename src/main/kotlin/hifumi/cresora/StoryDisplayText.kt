package hifumi.cresora

import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text

object StoryDisplayText {
    fun chapterTitle(player: ServerPlayerEntity?, chapter: StoryChapterDefinition): Text {
        val locale = StoryTextRegistry.resolvePlayerLocale(player)
        return Text.literal(StoryTextRegistry.chapterLabel(locale, chapter))
    }

    fun resolveLine(player: ServerPlayerEntity?, line: StoryDialogueLine): ResolvedStoryLine {
        val locale = StoryTextRegistry.resolvePlayerLocale(player)
        val body = when {
            !line.textId.isNullOrBlank() -> Text.literal(StoryTextRegistry.resolve(locale, line.textId))
            !line.text.isNullOrBlank() -> Text.literal(line.text)
            else -> Text.empty()
        }
        val speaker = when {
            !line.speakerId.isNullOrBlank() -> Text.literal(StoryTextRegistry.resolve(locale, line.speakerId))
            !line.speaker.isNullOrBlank() -> Text.literal(line.speaker)
            else -> null
        }?.takeUnless { it.string.isBlank() }
        return ResolvedStoryLine(speaker, body)
    }

    fun combatHintTexts(player: ServerPlayerEntity?, chapter: StoryChapterDefinition): List<Text> {
        return chapter.combatHints.map { resolveLine(player, it).body }
    }

    fun objectiveText(chapter: StoryChapterDefinition): Text {
        if (!chapter.linkedDomainId.isNullOrBlank()) {
            return Text.translatable("screen.cresora.story.objective.domain_clear")
        }
        return when (chapter.battleObjective.type) {
            StoryBattleObjectiveType.SURVIVE_TIME -> Text.translatable(
                "screen.cresora.story.objective.survive_time",
                chapter.battleObjective.durationSeconds.coerceAtLeast(1)
            )

            StoryBattleObjectiveType.DEFEAT_ALL -> Text.translatable("screen.cresora.story.objective.defeat_all")
        }
    }
}

data class ResolvedStoryLine(
    val speaker: Text?,
    val body: Text
)
