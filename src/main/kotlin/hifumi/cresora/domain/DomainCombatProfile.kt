package hifumi.cresora.domain
import hifumi.cresora.adventurerank.AdventureRankProgression
import hifumi.cresora.combat.CombatBalanceProfileRegistry
object DomainCombatProfile {
    data class Scaling(
        val healthScalar: Double,
        val defenseScalar: Double,
        val toughnessScalar: Double,
        val damageScalar: Double
    )

    fun scaling(rank: Int, elite: Boolean, boss: Boolean = false): Scaling {
        val progress = AdventureRankProgression.normalizedProgress(rank)
        val profile = CombatBalanceProfileRegistry.current().domain
        val eliteBonus = if (elite || boss) 1.0 else 0.0
        val bossBonus = if (boss) 1.0 else 0.0
        return Scaling(
            healthScalar = profile.normalHealthBase + progress * profile.normalHealthGrowth + eliteBonus * profile.eliteHealthBonus + bossBonus * profile.bossHealthBonus,
            defenseScalar = 1.0 + progress * profile.defenseGrowth + eliteBonus * profile.eliteDefenseBonus + bossBonus * profile.bossDefenseBonus,
            toughnessScalar = 1.0 + progress * profile.toughnessGrowth + eliteBonus * profile.eliteToughnessBonus + bossBonus * profile.bossToughnessBonus,
            damageScalar = 1.0 + progress * profile.damageGrowth + eliteBonus * profile.eliteDamageBonus + bossBonus * profile.bossDamageBonus
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
