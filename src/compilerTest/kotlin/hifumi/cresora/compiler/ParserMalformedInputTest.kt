package hifumi.cresora.compiler

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class ParserMalformedInputTest {

    private fun assertParseFails(source: String, expectedMessagePart: String) {
        val ex = assertThrows<RuntimeException> { parse(source) }
        assertTrue(
            ex.message!!.contains(expectedMessagePart),
            "expected message to contain '$expectedMessagePart' but was: ${ex.message}"
        )
    }

    @Test
    fun `unexpected top-level token`() {
        assertParseFails("banana {}", "Unexpected top-level token 'banana'")
    }

    @Test
    fun `unclosed weapon brace reaches EOF`() {
        assertParseFails(
            """
            weapon "Broken" {
                id: "broken"
            """.trimIndent(),
            "Expect '}' after weapon body"
        )
    }

    @Test
    fun `weapon missing stats block`() {
        assertParseFails(
            """
            weapon "No Stats" {
                id: "no_stats"
                rarity: "rare"
                base_item: "minecraft:iron_sword"
            }
            """.trimIndent(),
            "missing required stats block"
        )
    }

    @Test
    fun `weapon missing id`() {
        assertParseFails(
            """
            weapon "No Id" {
                rarity: "rare"
                base_item: "minecraft:iron_sword"
                stats { base_damage: 1.0 }
            }
            """.trimIndent(),
            "missing required id"
        )
    }

    @Test
    fun `weapon missing rarity`() {
        assertParseFails(
            """
            weapon "No Rarity" {
                id: "no_rarity"
                base_item: "minecraft:iron_sword"
                stats { base_damage: 1.0 }
            }
            """.trimIndent(),
            "missing required rarity"
        )
    }

    @Test
    fun `weapon missing base_item`() {
        assertParseFails(
            """
            weapon "No Base" {
                id: "no_base"
                rarity: "rare"
                stats { base_damage: 1.0 }
            }
            """.trimIndent(),
            "missing required base_item"
        )
    }

    @Test
    fun `unknown weapon field`() {
        assertParseFails(
            """
            weapon "Bad Field" {
                id: "bad_field"
                sharpness: "very"
            }
            """.trimIndent(),
            "Unknown weapon field 'sharpness'"
        )
    }

    @Test
    fun `invalid role is rejected`() {
        assertParseFails(
            """
            weapon "Bad Role" {
                id: "bad_role"
                role: warrior
            }
            """.trimIndent(),
            "Unknown role 'warrior'"
        )
    }

    @Test
    fun `unknown stats field`() {
        assertParseFails(
            """
            weapon "Bad Stat" {
                id: "bad_stat"
                rarity: "rare"
                base_item: "minecraft:iron_sword"
                stats { swing_speed: 1.0 }
            }
            """.trimIndent(),
            "Unknown stats field 'swing_speed'"
        )
    }

    @Test
    fun `unknown skill field`() {
        assertParseFails(
            """
            weapon "Bad Skill" {
                id: "bad_skill"
                rarity: "rare"
                base_item: "minecraft:iron_sword"
                stats { base_damage: 1.0 }
                skill "S" {
                    mana_cost: 5
                }
            }
            """.trimIndent(),
            "Unknown skill field 'mana_cost'"
        )
    }

    @Test
    fun `spirit block requires a name`() {
        assertParseFails(
            """
            weapon "Nameless Spirit" {
                id: "nameless_spirit"
                rarity: "5_star"
                base_item: "minecraft:diamond_sword"
                stats { base_damage: 1.0 }
                spirit {
                    bond_stage 1 {
                        title: "spirit.test.bond.1.title"
                        story: "spirit.test.bond.1.story"
                    }
                }
            }
            """.trimIndent(),
            "Spirit block is missing required name"
        )
    }

    @Test
    fun `spirit bond stage range is validated`() {
        assertParseFails(
            """
            weapon "Bad Spirit Stage" {
                id: "bad_spirit_stage"
                rarity: "5_star"
                base_item: "minecraft:diamond_sword"
                stats { base_damage: 1.0 }
                spirit {
                    name: "spirit.test.name"
                    bond_stage 7 {
                        title: "spirit.test.bond.7.title"
                        story: "spirit.test.bond.7.story"
                    }
                }
            }
            """.trimIndent(),
            "Spirit bond stage must be between 1 and 6"
        )
    }

    @Test
    fun `open_skill_menu requires sub-skill and duration`() {
        val ex = assertThrows<RuntimeException> {
            weaponWithHandler("""open_skill_menu("only_one")""")
        }
        assertTrue(ex.message!!.contains("open_skill_menu requires at least one sub-skill and a duration"))
    }

    @Test
    fun `send_localized_message requires key and color`() {
        val ex = assertThrows<RuntimeException> {
            weaponWithHandler("""send_localized_message("key.only")""")
        }
        assertTrue(ex.message!!.contains("send_localized_message requires at least a key and a color"))
    }

    @Test
    fun `unsupported block action name`() {
        val ex = assertThrows<RuntimeException> {
            weaponWithHandler(
                """
                whirlwind(3.0) {
                    apply_mark(target, "x", 1s)
                }
                """.trimIndent()
            )
        }
        assertTrue(ex.message!!.contains("Unsupported block action 'whirlwind'"))
    }

    @Test
    fun `artifact missing id`() {
        assertParseFails(
            """
            artifact "No Id Set" {
                set 2 {
                    stats { crit_damage: 10.0 }
                }
            }
            """.trimIndent(),
            "missing required id"
        )
    }

    @Test
    fun `unknown artifact bonus field`() {
        assertParseFails(
            """
            artifact "Bad Bonus" {
                id: "bad_bonus"
                set 2 {
                    glow: "yes"
                }
            }
            """.trimIndent(),
            "Unknown artifact bonus field"
        )
    }

    @Test
    fun `unknown buff field`() {
        assertParseFails(
            """
            weapon "Bad Buff" {
                id: "bad_buff"
                rarity: "rare"
                base_item: "minecraft:iron_sword"
                stats { base_damage: 1.0 }
                skill "S" {
                    effect_id: "s"
                    buff "b" {
                        sparkle: 1
                    }
                }
            }
            """.trimIndent(),
            "Unknown buff field 'sparkle'"
        )
    }
}
