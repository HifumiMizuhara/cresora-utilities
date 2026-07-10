package hifumi.cresora

import com.google.gson.JsonParser
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path

class MoonAltarRecipeResourceTest {

    @Test
    fun `moon altar recipe uses the 1_21 registry path and result schema`() {
        assertTrue(Files.isRegularFile(RECIPE_PATH), "Moon altar recipe must be registered under data/<namespace>/recipe")
        assertFalse(Files.exists(LEGACY_RECIPE_PATH), "Legacy data/<namespace>/recipes path must not remain")

        val recipe = Files.newBufferedReader(RECIPE_PATH).use { reader ->
            JsonParser.parseReader(reader).asJsonObject
        }

        assertEquals("minecraft:crafting_shaped", recipe.get("type").asString)
        assertEquals("cresora-utilities:moon_brick", recipe.getAsJsonObject("key").get("M").asString)

        val result = recipe.getAsJsonObject("result")
        assertEquals("cresora-utilities:moon_altar", result.get("id").asString)
        assertEquals(1, result.get("count").asInt)
        assertFalse(result.has("item"), "1.21 recipe results use 'id', not the legacy 'item' field")
    }

    private companion object {
        val RECIPE_PATH: Path = Path.of("src/main/resources/data/cresora-utilities/recipe/moon_altar.json")
        val LEGACY_RECIPE_PATH: Path = Path.of("src/main/resources/data/cresora-utilities/recipes/moon_altar.json")
    }
}
