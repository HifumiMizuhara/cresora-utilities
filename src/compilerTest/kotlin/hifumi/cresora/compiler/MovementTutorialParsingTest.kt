package hifumi.cresora.compiler

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class MovementTutorialParsingTest {

    @Test
    fun `resonant chord tutorial steps are parsed into movement AST`() {
        val movement = parse(movementWithTutorial(twoSteps = true)).filterIsInstance<MovementDefNode>().single()

        assertEquals(2, movement.resonantChordTutorialSteps.size)
        val first = movement.resonantChordTutorialSteps.first()
        assertEquals("reaction.cresora.discord", first.reactionKey)
        assertEquals("message.cresora.effect.discord", first.effectKey)
        assertEquals(listOf("high_note", "bass_note"), first.weapons.map(GrantedWeaponNode::weaponId))
    }

    @Test
    fun `tutorial step requires exactly two weapons`() {
        val source = movementWithTutorial(twoSteps = false).replace(
            "weapon(\"bass_note\", TWO_STAR)",
            ""
        )

        val error = assertThrows<RuntimeException> { parse(source) }
        assertEquals(
            "Tutorial step 'reaction.cresora.discord' must grant exactly two weapons",
            error.message
        )
    }

    @Test
    fun `tutorial reaction keys must be unique`() {
        val source = movementWithTutorial(twoSteps = true).replace(
            "reaction.cresora.crescendo",
            "reaction.cresora.discord"
        )

        val error = assertThrows<RuntimeException> { parse(source) }
        assertEquals("Resonant chord tutorial reaction keys must be unique", error.message)
    }

    @Test
    fun `tutorial step requires distinct temporary weapons`() {
        val duplicateWeapon = movementWithTutorial(twoSteps = false).replace("bass_note", "high_note")
        val duplicateError = assertThrows<RuntimeException> { parse(duplicateWeapon) }
        assertEquals(
            "Tutorial step 'reaction.cresora.discord' must grant two distinct weapons",
            duplicateError.message
        )

        val persistentWeapon = movementWithTutorial(twoSteps = false).replace(
            "weapon(\"high_note\", FIVE_STAR)",
            "weapon(\"high_note\", FIVE_STAR, remove_on_exit: false)"
        )
        val persistentError = assertThrows<RuntimeException> { parse(persistentWeapon) }
        assertEquals(
            "Tutorial step 'reaction.cresora.discord' weapons must use remove_on_exit: true",
            persistentError.message
        )
    }

    private fun movementWithTutorial(twoSteps: Boolean): String = """
        movement "Tutorial" {
            id: "tutorial"
            phase battle {
                resonant_chord_tutorial {
                    step "reaction.cresora.discord" {
                        effect_key: "message.cresora.effect.discord"
                        granted_weapons [
                            weapon("high_note", FIVE_STAR),
                            weapon("bass_note", TWO_STAR)
                        ]
                    }
                    ${if (twoSteps) secondStep else ""}
                }
                wave 1 {
                    spawns [spawn("minecraft:zombie")]
                }
            }
            rewards {
                credits: 0
            }
        }
    """.trimIndent()

    private val secondStep = """
        step "reaction.cresora.crescendo" {
            effect_key: "message.cresora.effect.crescendo"
            granted_weapons [
                weapon("high_note", FIVE_STAR),
                weapon("melody_note", FOUR_STAR)
            ]
        }
    """.trimIndent()
}
