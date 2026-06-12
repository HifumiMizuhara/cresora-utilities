package hifumi.cresora.equipment

import com.google.gson.JsonParser
import com.mojang.serialization.JsonOps
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class EquipmentEffectHookCodecTest {

    @Test
    fun testKnownTriggerParses() {
        val json = JsonParser.parseString(
            """{"trigger": "attack_dealt", "effectId": "burst", "parameters": {"chance": "50"}}"""
        )
        val hook = EquipmentEffectHook.CODEC.parse(JsonOps.INSTANCE, json)
            .getOrThrow { message -> IllegalArgumentException(message) }

        assertEquals(EquipmentEffectTrigger.ATTACK_DEALT, hook.trigger)
        assertEquals("burst", hook.effectId)
        assertEquals("50", hook.parameters["chance"])
    }

    @Test
    fun testUnsupportedTriggerRejected() {
        val json = JsonParser.parseString(
            """{"trigger": "on_dawn", "effectId": "burst"}"""
        )
        // Codec.STRING.xmap rethrows the IllegalArgumentException directly (it is not caught
        // into a failed DataResult), so the parse call itself propagates the exception.
        val exception = assertThrows(IllegalArgumentException::class.java) {
            EquipmentEffectHook.CODEC.parse(JsonOps.INSTANCE, json)
        }
        assertTrue(
            exception.message!!.contains("Unknown equipment effect trigger"),
            "Unexpected error: ${exception.message}"
        )
    }

    @Test
    fun testAllDeclaredTriggersRoundTrip() {
        for (trigger in EquipmentEffectTrigger.entries) {
            val json = JsonParser.parseString(
                """{"trigger": "${trigger.id}", "effectId": "effect"}"""
            )
            val hook = EquipmentEffectHook.CODEC.parse(JsonOps.INSTANCE, json)
                .getOrThrow { message -> IllegalArgumentException("Failed for ${trigger.id}: $message") }
            assertEquals(trigger, hook.trigger)
        }
    }

    @Test
    fun testUnsupportedTriggerInsideSetBonusRejected() {
        // Ensures the failure propagates through the nested effectHooks list of a set bonus.
        val json = JsonParser.parseString(
            """
            {
              "requiredPieces": 4,
              "effectHooks": [
                {"trigger": "kill", "effectId": "ok"},
                {"trigger": "bogus_trigger", "effectId": "bad"}
              ]
            }
            """.trimIndent()
        )
        assertThrows(IllegalArgumentException::class.java) {
            EquipmentSetBonus.CODEC.parse(JsonOps.INSTANCE, json)
        }
    }
}
