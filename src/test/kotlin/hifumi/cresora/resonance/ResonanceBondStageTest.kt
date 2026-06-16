package hifumi.cresora.resonance

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ResonanceBondStageTest {

    @Test
    fun bondPointsMapToExpectedStages() {
        assertEquals(1, ResonanceService.spiritBondStageForPoints(0))
        assertEquals(1, ResonanceService.spiritBondStageForPoints(39))
        assertEquals(2, ResonanceService.spiritBondStageForPoints(40))
        assertEquals(2, ResonanceService.spiritBondStageForPoints(99))
        assertEquals(3, ResonanceService.spiritBondStageForPoints(100))
        assertEquals(4, ResonanceService.spiritBondStageForPoints(200))
        assertEquals(5, ResonanceService.spiritBondStageForPoints(350))
        assertEquals(6, ResonanceService.spiritBondStageForPoints(550))
        assertEquals(6, ResonanceService.spiritBondStageForPoints(2_000))
    }
}
