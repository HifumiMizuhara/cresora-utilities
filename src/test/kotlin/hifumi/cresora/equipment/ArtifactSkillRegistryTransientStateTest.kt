package hifumi.cresora.equipment

import java.util.UUID
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ArtifactSkillRegistryTransientStateTest {

    @Suppress("UNCHECKED_CAST")
    private fun handlers(): MutableMap<String, ArtifactSkillHandler> =
        ArtifactSkillRegistry::class.java.getDeclaredField("handlers")
            .apply { isAccessible = true }
            .get(ArtifactSkillRegistry) as MutableMap<String, ArtifactSkillHandler>

    @Test
    fun `clears each registered artifact handler once on player cleanup`() {
        val playerId = UUID.randomUUID()
        val handler = RecordingHandler()
        val handlerMap = handlers()

        try {
            ArtifactSkillRegistry.register("transient_state_test_primary", handler)
            ArtifactSkillRegistry.register("transient_state_test_alias", handler)

            ArtifactSkillRegistry.clearTransientState(playerId)

            assertEquals(listOf(playerId), handler.clearedPlayerIds)
        } finally {
            handlerMap.remove("transient_state_test_primary")
            handlerMap.remove("transient_state_test_alias")
        }
    }

    private class RecordingHandler : ArtifactSkillHandler {
        val clearedPlayerIds = mutableListOf<UUID>()

        override fun clearTransientState(playerId: UUID) {
            clearedPlayerIds += playerId
        }
    }
}
