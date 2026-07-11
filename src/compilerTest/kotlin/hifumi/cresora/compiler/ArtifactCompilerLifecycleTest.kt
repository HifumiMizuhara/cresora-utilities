package hifumi.cresora.compiler

import com.google.gson.JsonParser
import java.io.File
import java.nio.file.Path
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class ArtifactCompilerLifecycleTest {

    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `artifact buff gets a state tick when DSL omits on_tick`() {
        val hooks = compileArtifact(
            """
            artifact "Timed Set" {
                id: "timed_set"
                set 4 {
                    buff "focus" {
                        duration: 5s
                    }
                    on_attack_dealt {
                        add_buff("focus")
                    }
                }
            }
            """.trimIndent()
        )

        assertEquals(listOf("attack_dealt"), hooks.map { it.get("trigger").asString })

        val generatedClass = File(
            tempDir.toFile(),
            "kotlin/hifumi/cresora/equipment/generated/ArtifactSkillTimedSet4pcOnAttackDealt.kt"
        ).readText()
        assertTrue(generatedClass.contains("override fun onTransientStateTick"))
    }

    @Test
    fun `state tick generation does not replace an explicit DSL on_tick handler`() {
        val hooks = compileArtifact(
            """
            artifact "Explicit Tick Set" {
                id: "explicit_tick_set"
                set 4 {
                    buff "focus" {
                        duration: 5s
                    }
                    on_tick {
                        add_buff("focus")
                    }
                }
            }
            """.trimIndent()
        )

        assertEquals(listOf("tick"), hooks.map { it.get("trigger").asString })

        val generatedClass = File(
            tempDir.toFile(),
            "kotlin/hifumi/cresora/equipment/generated/ArtifactSkillExplicitTickSet4pcOnTick.kt"
        ).readText()
        assertTrue(generatedClass.contains("override fun onTick"))
        assertTrue(generatedClass.contains("override fun onTransientStateTick"))
    }

    private fun compileArtifact(source: String): List<com.google.gson.JsonObject> {
        val inputDir = tempDir.resolve("cresora").toFile().apply { mkdirs() }
        val outputDir = tempDir.resolve("kotlin").toFile()
        val jsonFile = tempDir.resolve("resources/data/cresora-utilities/cresora/cac_artifact_content.json").toFile()
        File(inputDir, "fixture.artifact").writeText(source)

        ArtifactCompiler(inputDir, outputDir, jsonFile).compile()

        val root = JsonParser.parseString(jsonFile.readText()).asJsonObject
        return root.getAsJsonArray("sets")
            .single()
            .asJsonObject
            .getAsJsonArray("bonuses")
            .single()
            .asJsonObject
            .getAsJsonArray("effectHooks")
            .map { it.asJsonObject }
    }
}
