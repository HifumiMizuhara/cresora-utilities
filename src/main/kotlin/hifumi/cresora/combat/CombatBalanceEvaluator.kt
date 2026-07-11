package hifumi.cresora.combat

import hifumi.cresora.adventurerank.AdventureRankProgression

enum class CombatBuildTier(val displayName: String) {
    UNFINISHED("unfinished"),
    STANDARD("standard"),
    MAXED("maxed")
}

data class CombatBalanceSnapshot(
    val rank: Int,
    val tier: CombatBuildTier,
    val dps: Double,
    val normalTtkSeconds: Double,
    val eliteTtkSeconds: Double,
    val eventWaveTtkSeconds: Double,
    val normalHitPercentMaxHealth: Double,
    val eliteHitPercentMaxHealth: Double,
    val bossHitPercentMaxHealth: Double
)

object CombatBalanceEvaluator {
    val checkpoints: List<Int> = listOf(1, 20, 40, 55, 70)

    fun evaluate(rank: Int, tier: CombatBuildTier): CombatBalanceSnapshot {
        val normalizedRank = AdventureRankProgression.sanitizeRank(rank)
        val progress = AdventureRankProgression.normalizedProgress(normalizedRank)
        val profile = CombatBalanceProfileRegistry.current()
        val normalHealth = 20.0 * (1.0 + profile.survivor.healthGrowth * progress + phaseIndex(normalizedRank) * profile.survivor.healthPhaseBonus)
        val eliteHealth = normalHealth * profile.field.eliteHealthScalar * (3.0 + progress * 1.60)
        val dps = when (tier) {
            CombatBuildTier.UNFINISHED -> 3.8 + progress * 8.0
            CombatBuildTier.STANDARD -> 5.0 + progress * 13.0
            CombatBuildTier.MAXED -> 10.0 + progress * 35.0
        }
        val maxHealth = when (tier) {
            CombatBuildTier.UNFINISHED -> 20.0 + progress * 12.0
            CombatBuildTier.STANDARD -> 28.0 + progress * 38.0
            CombatBuildTier.MAXED -> 38.0 + progress * 74.0
        }
        val normalHit = 4.0 * (1.0 + profile.survivor.damageGrowth * progress)
        val eliteHit = normalHit * profile.field.eliteDamageScalar * 1.55
        val bossHit = normalHit * profile.field.bossDamageScalar * 2.05
        val eventWaveHealth = (normalHealth * 4.0 + eliteHealth) * (2.8 + progress * 0.5)

        return CombatBalanceSnapshot(
            rank = normalizedRank,
            tier = tier,
            dps = dps,
            normalTtkSeconds = normalHealth / dps,
            eliteTtkSeconds = eliteHealth / dps,
            eventWaveTtkSeconds = eventWaveHealth / dps,
            normalHitPercentMaxHealth = normalHit / maxHealth * 100.0,
            eliteHitPercentMaxHealth = eliteHit / maxHealth * 100.0,
            bossHitPercentMaxHealth = bossHit / maxHealth * 100.0
        )
    }

    fun snapshots(): List<CombatBalanceSnapshot> = checkpoints.flatMap { rank ->
        CombatBuildTier.entries.map { tier -> evaluate(rank, tier) }
    }

    private fun phaseIndex(rank: Int): Int = when {
        rank >= 56 -> 2
        rank >= 31 -> 1
        else -> 0
    }
}

object CombatBalanceReportMain {
    @JvmStatic
    fun main(args: Array<String>) {
        CombatBalanceProfileRegistry.init()
        println("rank\tbuild\tdps\tnormal_ttk\telite_ttk\tevent_wave\tnormal_hit%\telite_hit%\tboss_hit%")
        for (snapshot in CombatBalanceEvaluator.snapshots()) {
            println(
                listOf(
                    snapshot.rank,
                    snapshot.tier.displayName,
                    format(snapshot.dps),
                    format(snapshot.normalTtkSeconds),
                    format(snapshot.eliteTtkSeconds),
                    format(snapshot.eventWaveTtkSeconds),
                    format(snapshot.normalHitPercentMaxHealth),
                    format(snapshot.eliteHitPercentMaxHealth),
                    format(snapshot.bossHitPercentMaxHealth)
                ).joinToString("\t")
            )
        }
    }

    private fun format(value: Double): String = "%.2f".format(java.util.Locale.ROOT, value)
}
