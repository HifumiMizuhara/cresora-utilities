package hifumi.cresora.domain
import hifumi.cresora.adventurerank.AdventureRankProgression
object DomainCombatProfile {
    data class Scaling(
        val healthScalar: Double,
        val defenseScalar: Double,
        val toughnessScalar: Double,
        val damageScalar: Double
    )

    fun scaling(rank: Int, elite: Boolean, boss: Boolean = false): Scaling {
        val progress = AdventureRankProgression.normalizedProgress(rank)
        val eliteBonus = if (elite || boss) 1.0 else 0.0
        val bossBonus = if (boss) 1.0 else 0.0
        return Scaling(
            healthScalar = 0.92 + progress * 0.42 + eliteBonus * 0.32 + bossBonus * 0.56,
            defenseScalar = 1.0 + progress * 0.18 + eliteBonus * 0.18 + bossBonus * 0.27,
            toughnessScalar = 1.0 + progress * 0.12 + eliteBonus * 0.14 + bossBonus * 0.21,
            damageScalar = 1.0 + progress * 0.18 + eliteBonus * 0.16 + bossBonus * 0.19
        )
    }

    fun sessionRank(playerRank: Int): Int {
        val rank = AdventureRankProgression.sanitizeRank(playerRank)
        return when {
            rank <= 20 -> rank
            rank <= 40 -> rank
            rank <= 55 -> rank
            else -> rank
        }
    }

    fun recommendedBand(rank: Int): Pair<Int, Int> {
        val normalized = AdventureRankProgression.sanitizeRank(rank)
        return when {
            normalized <= 20 -> 1 to 20
            normalized <= 40 -> 21 to 40
            normalized <= 55 -> 41 to 55
            else -> 56 to 70
        }
    }
}
