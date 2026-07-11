package hifumi.cresora.combat

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CombatBalanceEvaluatorTest {
    @Test
    fun bundledProfileLoadsBeforeBalanceEvaluation() {
        CombatBalanceProfileRegistry.init()

        assertTrue(CombatBalanceProfileRegistry.current().version >= 1)
    }

    @Test
    fun standardAndMaxedCheckpointsMeetTheDeclaredTargets() {
        for (rank in CombatBalanceEvaluator.checkpoints) {
            val standard = CombatBalanceEvaluator.evaluate(rank, CombatBuildTier.STANDARD)
            val maxed = CombatBalanceEvaluator.evaluate(rank, CombatBuildTier.MAXED)

            assertTrue(standard.normalTtkSeconds in 3.0..5.0)
            assertTrue(standard.eliteTtkSeconds in 12.0..18.0)
            assertTrue(maxed.normalTtkSeconds <= 2.0)
            assertTrue(maxed.eliteTtkSeconds in 6.0..10.0)
            assertTrue(maxed.eventWaveTtkSeconds in 30.0..45.0)
            assertTrue(maxed.normalHitPercentMaxHealth <= 35.0)
            assertTrue(maxed.eliteHitPercentMaxHealth <= 50.0)
            assertTrue(maxed.bossHitPercentMaxHealth <= 65.0)
        }
    }
}
