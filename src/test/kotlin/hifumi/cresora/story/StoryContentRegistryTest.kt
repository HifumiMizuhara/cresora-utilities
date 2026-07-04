package hifumi.cresora.story

import com.google.gson.JsonParser
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.InputStreamReader
import java.nio.file.Files
import java.nio.file.Path

class StoryContentRegistryTest {

    @Test
    fun testStoryContentJsonIncludesTutorialChapter() {
        val resourcePath = Path.of("build/generated/cresora/resources/data/cresora-utilities/cresora/story_content.json")
        val root = Files.newInputStream(resourcePath).use { stream ->
            InputStreamReader(stream).use { reader ->
                JsonParser.parseReader(reader).asJsonObject
            }
        }

        val chapters = root.getAsJsonArray("chapters")
        assertFalse(chapters.isEmpty, "Story chapters list should not be empty")

        val tutorial = chapters.mapNotNull { it.asJsonObject }.firstOrNull { it["id"].asString == "0-3" }
        assertNotNull(tutorial, "0-3 tutorial chapter should exist")

        val chapter = tutorial!!
        assertEquals("0-2", chapter["prerequisiteChapterId"].asString)
        assertEquals(50, chapter["sortOrder"].asInt)
        assertEquals(listOf("resonance_practice"), chapter.getAsJsonArray("domainRewardIds").map { it.asString })
        assertEquals(5, chapter.getAsJsonArray("preBattleStory").size())
        assertEquals(3, chapter.getAsJsonArray("combatHints").size())
        assertFalse(chapter.has("grantedWeapons"), "Tutorial weapons should be defined per step")
        assertFalse(chapter.has("battleObjective"), "Tutorial completion should be driven by its configured steps")
        val tutorialSteps = chapter.getAsJsonArray("resonantChordTutorialSteps")
        assertEquals(6, tutorialSteps.size())
        assertEquals("reaction.cresora.discord", tutorialSteps.first().asJsonObject["reactionKey"].asString)
        assertTrue(tutorialSteps.all { it.asJsonObject.getAsJsonArray("weapons").size() == 2 })
        assertEquals(1, chapter.getAsJsonArray("battle").size())
        assertEquals(1, chapter.getAsJsonArray("battle").single().asJsonObject.getAsJsonArray("spawns").single().asJsonObject["count"].asInt)

        val rewards = chapter.getAsJsonObject("rewards")
        assertEquals(1500, rewards["credits"].asInt)
        assertEquals(2, rewards.getAsJsonArray("resonanceCurrencies").size())
    }
}
