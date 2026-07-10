package hifumi.cresora.weapon

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.UUID

class WeaponSkillServiceTransientStateTest {

    @Suppress("UNCHECKED_CAST")
    private fun <T> stateMap(name: String): MutableMap<UUID, T> {
        return WeaponSkillService::class.java.getDeclaredField(name)
            .apply { isAccessible = true }
            .get(WeaponSkillService) as MutableMap<UUID, T>
    }

    private fun pruneOfflinePlayers(activePlayerIds: Set<UUID>) {
        WeaponSkillService::class.java.getDeclaredMethod("pruneOfflinePlayerState", Set::class.java)
            .apply { isAccessible = true }
            .invoke(WeaponSkillService, activePlayerIds)
    }

    @Test
    fun offlinePlayerPruningDoesNotEraseLiveTargetEffects() {
        val targetId = UUID.randomUUID()
        val offlinePlayerId = UUID.randomUUID()
        val targetMarks = stateMap<MutableMap<String, Long>>("targetMarks")
        val invulnerabilityTicks = stateMap<Long>("invulnerabilityTicks")
        val soulBreakStacks = stateMap<Int>("soulBreakStacks")
        val soulBreakExpireTick = stateMap<Long>("soulBreakExpireTick")
        val taoStacks = stateMap<Int>("taoStacks")

        try {
            targetMarks[targetId] = mutableMapOf("lux" to Long.MAX_VALUE)
            invulnerabilityTicks[targetId] = Long.MAX_VALUE
            soulBreakStacks[targetId] = 3
            soulBreakExpireTick[targetId] = Long.MAX_VALUE
            taoStacks[offlinePlayerId] = 1

            pruneOfflinePlayers(emptySet())

            assertTrue(targetMarks.containsKey(targetId))
            assertTrue(invulnerabilityTicks.containsKey(targetId))
            assertTrue(soulBreakStacks.containsKey(targetId))
            assertTrue(soulBreakExpireTick.containsKey(targetId))
            assertFalse(taoStacks.containsKey(offlinePlayerId))
        } finally {
            targetMarks.remove(targetId)
            invulnerabilityTicks.remove(targetId)
            soulBreakStacks.remove(targetId)
            soulBreakExpireTick.remove(targetId)
            taoStacks.remove(offlinePlayerId)
        }
    }
}
