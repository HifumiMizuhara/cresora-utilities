package hifumi.cresora.compiler

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Test

class AreaOfEffectParsingTest {

    private fun parseAoe(handlerBody: String): AreaOfEffectActionNode {
        val weapon = weaponWithHandler(handlerBody)
        return handlerActions(weapon).filterIsInstance<AreaOfEffectActionNode>().single()
    }

    @Test
    fun `decimal radius is parsed`() {
        val aoe = parseAoe(
            """
            area_of_effect(5.0) {
                apply_mark(target, "lux", 30s)
            }
            """.trimIndent()
        )
        assertEquals(5.0, aoe.radius)
    }

    @Test
    fun `integer radius is parsed as double`() {
        val aoe = parseAoe(
            """
            area_of_effect(3) {
                apply_mark(target, "lux", 1s)
            }
            """.trimIndent()
        )
        assertEquals(3.0, aoe.radius)
    }

    @Test
    fun `missing radius falls back to default 5,0`() {
        val aoe = parseAoe(
            """
            area_of_effect() {
                apply_mark(target, "lux", 1s)
            }
            """.trimIndent()
        )
        assertEquals(5.0, aoe.radius)
    }

    @Test
    fun `non-numeric radius falls back to default 5,0`() {
        val aoe = parseAoe(
            """
            area_of_effect(wide) {
                apply_mark(target, "lux", 1s)
            }
            """.trimIndent()
        )
        assertEquals(5.0, aoe.radius)
    }

    @Test
    fun `nested known instruction parses as InstructionCallNode`() {
        val aoe = parseAoe(
            """
            area_of_effect(5.0) {
                apply_mark(target, "lux", 30s)
            }
            """.trimIndent()
        )
        val nested = assertInstanceOf(InstructionCallNode::class.java, aoe.actions.single())
        assertEquals("apply_mark", nested.functionName)
        assertEquals(listOf("target", "\"lux\"", "30s"), nested.arguments)
    }

    @Test
    fun `multiple nested actions preserve order`() {
        val aoe = parseAoe(
            """
            area_of_effect(2.5) {
                apply_mark(target, "a", 1s);
                send_localized_message("key.x", "RED")
                ignite(3)
            }
            """.trimIndent()
        )
        assertEquals(3, aoe.actions.size)
        val first = assertInstanceOf(InstructionCallNode::class.java, aoe.actions[0])
        assertEquals("apply_mark", first.functionName)
        val second = assertInstanceOf(SendLocalizedMessageActionNode::class.java, aoe.actions[1])
        assertEquals("key.x", second.key)
        assertEquals("RED", second.color)
        val third = assertInstanceOf(InstructionCallNode::class.java, aoe.actions[2])
        assertEquals("ignite", third.functionName)
    }

    @Test
    fun `area_of_effect inside execute block`() {
        val weapon = weaponWithHandler(
            """
            execute {
                val anchor = player.blockPos;
                area_of_effect(4.0) {
                    apply_mark(target, "m", 5s)
                }
            }
            """.trimIndent()
        )
        val execute = assertInstanceOf(ExecuteActionNode::class.java, handlerActions(weapon).single())
        val aoe = execute.statements.filterIsInstance<AreaOfEffectActionNode>().single()
        assertEquals(4.0, aoe.radius)
        assertInstanceOf(InstructionCallNode::class.java, aoe.actions.single())
    }

    @Test
    fun `sibling actions around the aoe block are kept`() {
        val weapon = weaponWithHandler(
            """
            start_cooldown()
            area_of_effect(5.0) {
                apply_mark(target, "lux", 30s)
            }
            send_localized_message("done", "AQUA")
            """.trimIndent()
        )
        val actions = handlerActions(weapon)
        assertEquals(3, actions.size)
        assertInstanceOf(InstructionCallNode::class.java, actions[0])
        assertInstanceOf(AreaOfEffectActionNode::class.java, actions[1])
        assertInstanceOf(SendLocalizedMessageActionNode::class.java, actions[2])
    }
}
