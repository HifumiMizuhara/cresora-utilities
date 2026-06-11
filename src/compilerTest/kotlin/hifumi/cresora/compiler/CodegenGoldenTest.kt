package hifumi.cresora.compiler

import java.io.File
import java.nio.file.Path
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

/**
 * Snapshot tests for the code generators. They exist to catch accidental emission
 * drift; when compiler output changes intentionally, update the .expected resources
 * (the test writes the actual output under build/golden-actual/ on mismatch).
 */
class CodegenGoldenTest {

    @TempDir
    lateinit var tempDir: Path

    private val weaponFixture = """
        weapon "Golden Fixture" {
            id: "golden_fixture"
            rarity: "epic"
            base_item: "minecraft:iron_sword"
            damage_type: physical
            role: guard
            stats {
                base_damage: 6.0
                damage_per_level: 0.4
                speed: 1.6
                max_base_level: 70
                max_skill_level: 10
            }
            skill "Golden Strike" {
                effect_id: "golden_strike"
                cooldown: 20s
                duration: 10s
                base_value: 4.0
                value_per_level: 0.5
                buff "golden_focus" {
                    max_stacks: 3
                    duration: 10s
                    stats {
                        crit_rate: 5.0
                    }
                }
                on_activate {
                    add_buff("golden_focus", 1)
                    start_cooldown()
                    area_of_effect(5.0) {
                        apply_mark(target, "gold", 30s)
                    }
                    send_localized_message("item.cresora.weapon.skill.golden.activated", "GOLD")
                    execute {
                        val state = goldenFocusStates[player.uuid];
                        if (state != null) {
                            heal(2.0)
                        }
                    }
                }
                on_damage_dealt {
                    deal_true_damage(2.5)
                }
            }
        }
    """.trimIndent()

    private val artifactFixture = """
        artifact "Golden Set" {
            id: "golden_set"
            set 4 {
                buff "golden_echo" {
                    max_stacks: 2
                    duration: 8s
                    stats {
                        all_damage: 4.0
                    }
                }
                on_attack_dealt {
                    add_buff("golden_echo", 1);
                    apply_mark(target, "echo", 100);
                }
            }
        }
    """.trimIndent()

    private fun assertMatchesGolden(actualFile: File, expectedResource: String) {
        val actual = actualFile.readText().replace("\r\n", "\n")
        val resource = javaClass.getResource("/golden/$expectedResource")
        val dumpDir = File("build/golden-actual").apply { mkdirs() }
        val dump = File(dumpDir, expectedResource)
        if (resource == null) {
            dump.writeText(actual)
            fail<Unit>("Missing golden resource /golden/$expectedResource — actual output written to ${dump.absolutePath}")
        }
        val expected = resource!!.readText().replace("\r\n", "\n")
        if (expected != actual) {
            dump.writeText(actual)
            // assertEquals shows the diff; the dump makes updating the expectation easy
        }
        assertEquals(expected, actual)
    }

    @Test
    fun `weapon codegen matches golden output`() {
        val inputDir = tempDir.resolve("cresora").toFile().apply { mkdirs() }
        val outputDir = tempDir.resolve("kotlin").toFile()
        val jsonFile = tempDir.resolve("resources/data/cresora-utilities/cresora/cwc_weapon_content.json").toFile()
        File(inputDir, "golden_fixture.cresora").writeText(weaponFixture)

        CresoraCompiler(inputDir, outputDir, jsonFile).compile()

        assertMatchesGolden(
            File(outputDir, "hifumi/cresora/skill/generated/GoldenFixtureSkill.kt"),
            "GoldenFixtureSkill.kt.expected"
        )
        assertMatchesGolden(jsonFile, "cwc_weapon_content.json.expected")
    }

    @Test
    fun `artifact codegen matches golden output`() {
        val inputDir = tempDir.resolve("cresora").toFile().apply { mkdirs() }
        val outputDir = tempDir.resolve("kotlin").toFile()
        val jsonFile = tempDir.resolve("resources/data/cresora-utilities/cresora/cac_artifact_content.json").toFile()
        File(inputDir, "golden_set.artifact").writeText(artifactFixture)

        ArtifactCompiler(inputDir, outputDir, jsonFile).compile()

        assertMatchesGolden(
            File(outputDir, "hifumi/cresora/equipment/generated/ArtifactSkillGoldenSet4pcOnAttackDealt.kt"),
            "ArtifactSkillGoldenSet4pcOnAttackDealt.kt.expected"
        )
    }
}
