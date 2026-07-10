package hifumi.cresora.leyline

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.UUID

class LeyLineAccessPolicyTest {

    @Test
    fun onlyThePendingLeyLinesOwnerCanStartIt() {
        val owner = UUID.randomUUID()
        val otherPlayer = UUID.randomUUID()

        assertEquals(
            LeyLineStartAuthorization.AUTHORIZED,
            LeyLineAccessPolicy.authorizeStart(owner, owner)
        )
        assertEquals(
            LeyLineStartAuthorization.NOT_OWNER,
            LeyLineAccessPolicy.authorizeStart(owner, otherPlayer)
        )
        assertEquals(
            LeyLineStartAuthorization.MISSING_PENDING_LEY_LINE,
            LeyLineAccessPolicy.authorizeStart(null, owner)
        )
    }

    @Test
    fun occupiedPositionsCannotBeReused() {
        assertTrue(LeyLineAccessPolicy.canPlace(hasPendingLeyLine = false, hasActiveSession = false))
        assertFalse(LeyLineAccessPolicy.canPlace(hasPendingLeyLine = true, hasActiveSession = false))
        assertFalse(LeyLineAccessPolicy.canPlace(hasPendingLeyLine = false, hasActiveSession = true))
    }

    @Test
    fun completionCanOnlyBeAwardedToTheOwner() {
        val owner = UUID.randomUUID()

        assertTrue(LeyLineAccessPolicy.canReceiveCompletion(owner, owner))
        assertFalse(LeyLineAccessPolicy.canReceiveCompletion(owner, UUID.randomUUID()))
    }
}
