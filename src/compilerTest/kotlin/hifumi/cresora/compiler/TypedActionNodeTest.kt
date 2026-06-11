package hifumi.cresora.compiler

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class TypedActionNodeTest {

    private fun singleAction(handlerBody: String): ActionNode =
        handlerActions(weaponWithHandler(handlerBody)).single()

    private fun assertFails(handlerBody: String, expectedMessagePart: String) {
        val ex = assertThrows<RuntimeException> { weaponWithHandler(handlerBody) }
        assertTrue(
            ex.message!!.contains(expectedMessagePart),
            "expected message to contain '$expectedMessagePart' but was: ${ex.message}"
        )
    }

    @Test
    fun `apply_mark with seconds literal`() {
        val node = assertInstanceOf(ApplyMarkActionNode::class.java, singleAction("""apply_mark(target, "lux", 30s)"""))
        assertEquals("target", node.target)
        assertEquals("lux", node.markId)
        assertEquals(DurationValue.Seconds(30.0), node.duration)
        assertEquals("600L", node.duration.toTicksExpression())
    }

    @Test
    fun `apply_mark with bare number keeps tick semantics`() {
        val node = assertInstanceOf(ApplyMarkActionNode::class.java, singleAction("""apply_mark(target, "curse", 100)"""))
        assertEquals(DurationValue.Ticks(100), node.duration)
        assertEquals("100L", node.duration.toTicksExpression())
    }

    @Test
    fun `apply_mark arity is validated`() {
        assertFails("""apply_mark(target, "lux")""", "apply_mark expects 3 argument(s), got 2")
    }

    @Test
    fun `apply_mark mark id must be a string literal`() {
        assertFails("""apply_mark(target, lux, 30s)""", "mark id must be a string literal")
    }

    @Test
    fun `apply_mark rejects non-numeric duration`() {
        assertFails("""apply_mark(target, "lux", forever)""", "apply_mark duration must be a number")
    }

    @Test
    fun `grant_invulnerability parses target and duration`() {
        val node = assertInstanceOf(
            GrantInvulnerabilityActionNode::class.java,
            singleAction("""grant_invulnerability(player, 5s)""")
        )
        assertEquals("player", node.target)
        assertEquals(DurationValue.Seconds(5.0), node.duration)
    }

    @Test
    fun `add_buff defaults to one stack`() {
        val node = assertInstanceOf(AddBuffActionNode::class.java, singleAction("""add_buff("focus")"""))
        assertEquals("focus", node.buffId)
        assertEquals(1, node.stacks)
    }

    @Test
    fun `add_buff parses explicit stacks`() {
        val node = assertInstanceOf(AddBuffActionNode::class.java, singleAction("""add_buff("focus", 3)"""))
        assertEquals(3, node.stacks)
    }

    @Test
    fun `add_buff rejects non-integer stacks`() {
        assertFails("""add_buff("focus", many)""", "add_buff stacks must be an integer")
    }

    @Test
    fun `start_cooldown without args uses skill cooldown`() {
        val node = assertInstanceOf(StartCooldownActionNode::class.java, singleAction("start_cooldown()"))
        assertNull(node.duration)
    }

    @Test
    fun `start_cooldown with explicit duration`() {
        val node = assertInstanceOf(StartCooldownActionNode::class.java, singleAction("start_cooldown(34s)"))
        assertEquals(DurationValue.Seconds(34.0), node.duration)
        assertEquals("680L", node.duration!!.toTicksExpression())
    }

    @Test
    fun `send_message uppercases and validates the color`() {
        val node = assertInstanceOf(
            SendMessageActionNode::class.java,
            singleAction("""send_message("key.activated", "light_purple")""")
        )
        assertEquals("key.activated", node.key)
        assertEquals("LIGHT_PURPLE", node.color)
    }

    @Test
    fun `send_message defaults to white`() {
        val node = assertInstanceOf(SendMessageActionNode::class.java, singleAction("""send_message("key.activated")"""))
        assertEquals("WHITE", node.color)
    }

    @Test
    fun `send_message rejects unknown colors`() {
        assertFails("""send_message("key", "sparkly")""", "is not a Minecraft Formatting color")
    }

    @Test
    fun `apply_status_effect parses effect id, duration and amplifier`() {
        val node = assertInstanceOf(
            ApplyStatusEffectActionNode::class.java,
            singleAction("""apply_status_effect("minecraft:strength", 10s, 1)""")
        )
        assertEquals("minecraft:strength", node.effectId)
        assertEquals(DurationValue.Seconds(10.0), node.duration)
        assertEquals(1, node.amplifier)
    }

    @Test
    fun `apply_status_effect amplifier defaults to zero`() {
        val node = assertInstanceOf(
            ApplyStatusEffectActionNode::class.java,
            singleAction("""apply_status_effect("minecraft:speed", 100)""")
        )
        assertEquals(0, node.amplifier)
    }

    @Test
    fun `skill_duration resolves to skill duration expression`() {
        val node = assertInstanceOf(
            ApplyMarkActionNode::class.java,
            singleAction("""apply_mark(target, "lux", skill_duration)""")
        )
        assertEquals(DurationValue.SkillDuration, node.duration)
        assertEquals("(definition.skill.durationSeconds * 20).toLong()", node.duration.toTicksExpression())
    }

    @Test
    fun `typed commands inside execute blocks are parsed`() {
        val weapon = weaponWithHandler(
            """
            execute {
                val x = 1;
                add_buff("focus", 2)
            }
            """.trimIndent()
        )
        val execute = assertInstanceOf(ExecuteActionNode::class.java, handlerActions(weapon).single())
        val addBuff = execute.statements.filterIsInstance<AddBuffActionNode>().single()
        assertEquals(2, addBuff.stacks)
    }
}
