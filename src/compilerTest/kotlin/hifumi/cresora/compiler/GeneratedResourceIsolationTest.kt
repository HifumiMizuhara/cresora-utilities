package hifumi.cresora.compiler

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.io.File
import java.nio.file.Path
import java.security.MessageDigest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class GeneratedResourceIsolationTest {

    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `asset compilation preserves source resources and clears removed DSL output`() {
        val projectRoot = tempDir.toFile()
        val inputDir = File(projectRoot, "src/main/cresora").apply { mkdirs() }
        val sourceResourcesDir = File(projectRoot, "src/main/resources")
        val kotlinOutDir = File(projectRoot, "build/generated/cresora/kotlin")
        val resourceOutDir = File(projectRoot, "build/generated/cresora/resources")

        val manualItem = """{"model":{"type":"minecraft:model","model":"minecraft:item/stone"}}""" + "\n"
        val manualModel = """{"parent":"minecraft:item/generated"}""" + "\n"
        writeSourceResource(sourceResourcesDir, "assets/cresora-utilities/lang/en_us.json", """{"base.key":"Base value"}""" + "\n")
        writeSourceResource(sourceResourcesDir, "assets/cresora-utilities/items/manual.json", manualItem)
        writeSourceResource(sourceResourcesDir, "assets/cresora-utilities/models/item/manual.json", manualModel)

        val weaponDsl = File(inputDir, "generated_fixture.cresora").apply { writeText(weaponFixture) }
        val artifactDsl = File(inputDir, "generated_fixture.artifact").apply { writeText(artifactFixture) }
        val movementDsl = File(inputDir, "generated_fixture.movement").apply { writeText(movementFixture) }
        val sourceHashes = fileHashes(sourceResourcesDir)

        compileCresoraAssets(projectRoot, kotlinOutDir, resourceOutDir, sourceResourcesDir)

        assertEquals(sourceHashes, fileHashes(sourceResourcesDir))
        assertEquals(manualItem, File(resourceOutDir, "assets/cresora-utilities/items/manual.json").readText())
        assertEquals(manualModel, File(resourceOutDir, "assets/cresora-utilities/models/item/manual.json").readText())

        val generatedLang = jsonObject(resourceOutDir, "assets/cresora-utilities/lang/en_us.json")
        assertEquals("Base value", generatedLang.get("base.key").asString)
        assertEquals("Generated Fixture", generatedLang.get("item.cresora-utilities.generated_fixture").asString)
        assertEquals("Generated Artifact", generatedLang.get("item.cresora-utilities.equipment.set.generated_artifact").asString)
        assertTrue(File(resourceOutDir, "assets/cresora-utilities/items/generated_fixture.json").isFile)
        assertTrue(File(resourceOutDir, "assets/cresora-utilities/models/item/generated_fixture.json").isFile)
        assertTrue(File(kotlinOutDir, "hifumi/cresora/skill/generated/GeneratedFixtureSkill.kt").isFile)

        assertEquals(
            1,
            jsonObject(resourceOutDir, "data/cresora-utilities/cresora/cwc_weapon_content.json")
                .getAsJsonArray("weaponDefinitions").size()
        )
        assertEquals(
            1,
            jsonObject(resourceOutDir, "data/cresora-utilities/cresora/cac_artifact_content.json")
                .getAsJsonArray("sets").size()
        )
        assertEquals(
            1,
            jsonObject(resourceOutDir, "data/cresora-utilities/cresora/story_content.json")
                .getAsJsonArray("chapters").size()
        )
        assertEquals(
            "Generated story",
            jsonObject(resourceOutDir, "data/cresora-utilities/cresora/story_texts.json")
                .getAsJsonObject("texts")
                .getAsJsonObject("en_us")
                .get("story.generated").asString
        )

        assertTrue(weaponDsl.delete())
        assertTrue(artifactDsl.delete())
        assertTrue(movementDsl.delete())
        compileCresoraAssets(projectRoot, kotlinOutDir, resourceOutDir, sourceResourcesDir)

        assertEquals(sourceHashes, fileHashes(sourceResourcesDir))
        assertEquals(
            0,
            jsonObject(resourceOutDir, "data/cresora-utilities/cresora/cwc_weapon_content.json")
                .getAsJsonArray("weaponDefinitions").size()
        )

        val artifactJson = jsonObject(resourceOutDir, "data/cresora-utilities/cresora/cac_artifact_content.json")
        listOf("slots", "sets", "equipmentDefinitions", "dropProfiles", "mobLoot").forEach { key ->
            assertEquals(0, artifactJson.getAsJsonArray(key).size(), key)
        }

        assertEquals(
            0,
            jsonObject(resourceOutDir, "data/cresora-utilities/cresora/story_content.json")
                .getAsJsonArray("chapters").size()
        )
        assertEquals(
            0,
            jsonObject(resourceOutDir, "data/cresora-utilities/cresora/story_texts.json")
                .getAsJsonObject("texts").size()
        )
        assertTrue(File(kotlinOutDir, "hifumi/cresora/equipment/generated/CompiledArtifactRegistry.kt").isFile)
        assertFalse(File(kotlinOutDir, "hifumi/cresora/skill/generated/GeneratedFixtureSkill.kt").exists())
        assertEquals(manualItem, File(resourceOutDir, "assets/cresora-utilities/items/manual.json").readText())
        assertEquals(manualModel, File(resourceOutDir, "assets/cresora-utilities/models/item/manual.json").readText())
        assertEquals(
            0,
            jsonObject(resourceOutDir, "assets/cresora-utilities/items/sub_skill_dummy.json")
                .getAsJsonObject("model")
                .getAsJsonArray("cases").size()
        )
        assertFalse(File(resourceOutDir, "assets/cresora-utilities/items/generated_fixture.json").exists())
        assertFalse(File(resourceOutDir, "assets/cresora-utilities/models/item/generated_fixture.json").exists())
    }

    private fun writeSourceResource(root: File, relativePath: String, content: String) {
        File(root, relativePath).apply {
            parentFile.mkdirs()
            writeText(content)
        }
    }

    private fun jsonObject(root: File, relativePath: String): JsonObject =
        JsonParser.parseString(File(root, relativePath).readText()).asJsonObject

    private fun fileHashes(root: File): Map<String, String> =
        root.walkTopDown()
            .filter { it.isFile }
            .associate { file ->
                file.relativeTo(root).invariantSeparatorsPath to sha256(file.readBytes())
            }

    private fun sha256(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

    private companion object {
        val weaponFixture = """
            weapon "Generated Fixture" {
                id: "generated_fixture"
                rarity: "2_star"
                base_item: "minecraft:iron_sword"
                damage_type: physical
                role: guard
                stats {
                    base_damage: 5.0
                    damage_per_level: 0.5
                    speed: 1.6
                    max_base_level: 60
                    max_skill_level: 10
                }
                skill "Generated Strike" {
                    effect_id: "generated_strike"
                    cooldown: 10s
                    duration: 5s
                    base_value: 1.0
                    value_per_level: 0.0
                    on_activate {
                        start_cooldown()
                    }
                }
                translations {
                    en_us { name: "Generated Fixture" }
                }
            }
        """.trimIndent()

        val artifactFixture = """
            artifact "Generated Artifact" {
                id: "generated_artifact"
                set 2 {
                    stats {
                        atk_percent: 12.0
                    }
                }
                translations {
                    en_us { name: "Generated Artifact" }
                }
            }
        """.trimIndent()

        val movementFixture = """
            movement "Generated Movement" {
                id: "generated_movement"
                sort_order: 1
                unlock_rank: 1
                translations {
                    en_us { "story.generated": "Generated story" }
                }
            }
        """.trimIndent()
    }
}
