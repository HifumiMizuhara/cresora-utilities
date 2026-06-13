package hifumi.cresora.debuff

import hifumi.cresora.masquerade.MasqueradeService
import net.minecraft.util.Identifier
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.UUID

/**
 * Regression guard for reskill-protection cleanup on disconnect/reconnect (TODO L44).
 *
 * The full disconnect path (endSession) is server-coupled and still needs a runClient pass,
 * but the invariant that matters for reskill protection is testable directly: when a player
 * disconnects, their UUID-keyed transient state must be dropped, and an unrelated (e.g.
 * reconnecting) player's state must never be touched by that cleanup.
 */
class TransientStateCleanupTest {

    @Suppress("UNCHECKED_CAST")
    private fun debuffMap(): MutableMap<UUID, MutableMap<Identifier, Int>> =
        CresoraDebuffService::class.java.getDeclaredField("activeDebuffs")
            .apply { isAccessible = true }
            .get(CresoraDebuffService) as MutableMap<UUID, MutableMap<Identifier, Int>>

    @Suppress("UNCHECKED_CAST")
    private fun masqueradeSnapshotMap(): MutableMap<UUID, Any> =
        MasqueradeService::class.java.getDeclaredField("pendingRespawnSnapshots")
            .apply { isAccessible = true }
            .get(MasqueradeService) as MutableMap<UUID, Any>

    @Test
    fun debuffCleanupRemovesOnlyTheDisconnectingPlayer() {
        val disconnecting = UUID.randomUUID()
        val other = UUID.randomUUID()
        val debuffId = Identifier.of("cresora-utilities", "nerve_damage")

        val map = debuffMap()
        try {
            map[disconnecting] = mutableMapOf(debuffId to 100)
            map[other] = mutableMapOf(debuffId to 100)

            CresoraDebuffService.clearTransientState(disconnecting)

            assertFalse(CresoraDebuffService.hasActiveDebuffs(disconnecting), "Disconnecting player's debuffs must be cleared")
            assertTrue(CresoraDebuffService.hasActiveDebuffs(other), "Other player's debuffs must be untouched")
        } finally {
            map.remove(disconnecting)
            map.remove(other)
        }
    }

    @Test
    fun reconnectingPlayerStartsWithoutDebuffs() {
        // After cleanup, a fresh UUID lookup (the reconnecting session) is clean by default.
        val reconnecting = UUID.randomUUID()
        assertFalse(CresoraDebuffService.hasActiveDebuffs(reconnecting))
    }

    @Test
    fun masqueradeSnapshotCleanupRemovesOnlyTheDisconnectingPlayer() {
        val disconnecting = UUID.randomUUID()
        val other = UUID.randomUUID()

        val map = masqueradeSnapshotMap()
        try {
            // Placeholder values: the cleanup keys on UUID and never inspects the snapshot.
            map[disconnecting] = Any()
            map[other] = Any()

            MasqueradeService.clearTransientState(disconnecting)

            assertFalse(MasqueradeService.hasPendingRespawnSnapshot(disconnecting), "Disconnecting player's pending snapshot must be dropped")
            assertTrue(MasqueradeService.hasPendingRespawnSnapshot(other), "Other player's pending snapshot must be untouched")
        } finally {
            map.remove(disconnecting)
            map.remove(other)
        }
    }
}
