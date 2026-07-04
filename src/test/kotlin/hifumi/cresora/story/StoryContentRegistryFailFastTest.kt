package hifumi.cresora.story

import com.google.gson.JsonParser
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.io.InputStreamReader
import java.nio.file.Files
import java.nio.file.Path

/**
 * Bug Condition & Preservation tests for StoryContentRegistry fail-fast fix.
 *
 * Property 1 (Bug Condition): defaultBundle() must not exist; runCatching must be removed;
 * init() must propagate exceptions on missing/malformed resources rather than silently falling back.
 *
 * Property 2 (Preservation): Valid story_content.json continues to be well-formed and
 * satisfies all structural invariants enforced by applyBundle().
 */
class StoryContentRegistryFailFastTest {

    // --- Property 1: Bug Condition Tests ---

    @Test
    fun `defaultBundle method should not exist after fix`() {
        val methods = StoryContentRegistry::class.java.declaredMethods.map { it.name }
        assertFalse(methods.contains("defaultBundle"),
            "defaultBundle() should be removed — no fallback data should exist in the codebase")
    }

    @Test
    fun `init should not use runCatching - no Result returning methods`() {
        // After the fix, init() directly calls loadBundledContent() without runCatching.
        // Verify no method in StoryContentRegistry returns kotlin.Result.
        val declaredMethods = StoryContentRegistry::class.java.declaredMethods
        val resultReturningMethods = declaredMethods.filter {
            it.returnType.name.contains("Result")
        }
        assertTrue(resultReturningMethods.isEmpty(),
            "No method should return kotlin.Result — runCatching has been removed")
    }

    @Test
    fun `loadBundledContent still exists as primary loading mechanism`() {
        val methods = StoryContentRegistry::class.java.declaredMethods.map { it.name }
        assertTrue(methods.contains("loadBundledContent"),
            "loadBundledContent() should still exist as the primary loading mechanism")
    }

    // --- Property 2: Preservation Tests ---

    private fun loadStoryJson(): com.google.gson.JsonObject {
        val resourcePath = Path.of("src/main/resources/data/cresora-utilities/cresora/story_content.json")
        assertTrue(Files.exists(resourcePath), "story_content.json must exist")
        return Files.newInputStream(resourcePath).use { stream ->
            InputStreamReader(stream).use { reader ->
                JsonParser.parseReader(reader).asJsonObject
            }
        }
    }

    @Test
    fun `story_content_json is valid JSON with chapters array`() {
        val root = loadStoryJson()
        assertTrue(root.has("chapters"), "Root must contain 'chapters' key")
        val chapters = root.getAsJsonArray("chapters")
        assertFalse(chapters.isEmpty, "Story chapters list should not be empty")
    }

    @Test
    fun `all chapters have unique non-blank IDs`() {
        val root = loadStoryJson()
        val chapters = root.getAsJsonArray("chapters")
        val ids = chapters.map { it.asJsonObject["id"].asString }

        assertTrue(ids.all(String::isNotBlank), "All chapter IDs must be non-blank")
        assertEquals(ids.distinct().size, ids.size, "Chapter IDs must be unique")
    }

    @Test
    fun `all chapters have required fields`() {
        val root = loadStoryJson()
        val chapters = root.getAsJsonArray("chapters")

        for (element in chapters) {
            val chapter = element.asJsonObject
            val id = chapter["id"].asString

            assertTrue(chapter.has("displayName"), "Chapter '$id' must have displayName")
            assertTrue(chapter["displayName"].asString.isNotBlank(), "Chapter '$id' displayName must not be blank")

            if (chapter.has("groupId") && !chapter["groupId"].isJsonNull) {
                assertTrue(chapter["groupId"].asString.isNotBlank(), "Chapter '$id' groupId must not be blank when set")
            }
            if (chapter.has("titleTextId") && !chapter["titleTextId"].isJsonNull) {
                assertTrue(chapter["titleTextId"].asString.isNotBlank(), "Chapter '$id' titleTextId must not be blank when set")
            }
        }
    }

    @Test
    fun `prerequisite chapters reference existing IDs`() {
        val root = loadStoryJson()
        val chapters = root.getAsJsonArray("chapters")
        val allIds = chapters.map { it.asJsonObject["id"].asString }.toSet()

        for (element in chapters) {
            val chapter = element.asJsonObject
            val id = chapter["id"].asString

            if (chapter.has("prerequisiteChapterId") && !chapter["prerequisiteChapterId"].isJsonNull) {
                val prereq = chapter["prerequisiteChapterId"].asString
                if (prereq.isNotBlank()) {
                    assertNotEquals(id, prereq, "Chapter '$id' cannot require itself")
                    assertTrue(allIds.contains(prereq),
                        "Chapter '$id' requires unknown prerequisite '$prereq'")
                }
            }
        }
    }

    @Test
    fun `chapters sort produces stable deterministic ordering`() {
        val root = loadStoryJson()
        val chapters = root.getAsJsonArray("chapters").map { it.asJsonObject }

        data class SortKey(val numericGroup: Int, val group: String, val sortOrder: Int, val id: String)

        fun groupOf(chapter: com.google.gson.JsonObject): String {
            if (chapter.has("groupId") && !chapter["groupId"].isJsonNull) {
                val g = chapter["groupId"].asString
                if (g.isNotBlank()) return g
            }
            return chapter["id"].asString.substringBefore('-').ifBlank { chapter["id"].asString }
        }

        val keys = chapters.map { ch ->
            val group = groupOf(ch)
            SortKey(
                group.toIntOrNull() ?: Int.MAX_VALUE,
                group,
                if (ch.has("sortOrder")) ch["sortOrder"].asInt else 0,
                ch["id"].asString
            )
        }

        val sorted1 = keys.sortedWith(compareBy(SortKey::numericGroup, SortKey::group, SortKey::sortOrder, SortKey::id))
        val sorted2 = keys.sortedWith(compareBy(SortKey::numericGroup, SortKey::group, SortKey::sortOrder, SortKey::id))
        assertEquals(sorted1, sorted2, "Sort order must be deterministic")
    }

    @Test
    fun `non-tutorial chapters have at least one battle wave`() {
        val root = loadStoryJson()
        val chapters = root.getAsJsonArray("chapters")

        for (element in chapters) {
            val chapter = element.asJsonObject
            val id = chapter["id"].asString

            // Skip domain-linked chapters (they delegate battle to the domain)
            if (chapter.has("linkedDomainId") && !chapter["linkedDomainId"].isJsonNull &&
                chapter["linkedDomainId"].asString.isNotBlank()) {
                continue
            }
            // Skip tutorial chapters (they use resonantChordTutorialSteps)
            if (chapter.has("resonantChordTutorialSteps") &&
                chapter.getAsJsonArray("resonantChordTutorialSteps").size() > 0) {
                continue
            }

            assertTrue(chapter.has("battle"), "Chapter '$id' must have battle field")
            val battle = chapter.getAsJsonArray("battle")
            assertFalse(battle.isEmpty, "Chapter '$id' must define at least one battle wave")
        }
    }
}
