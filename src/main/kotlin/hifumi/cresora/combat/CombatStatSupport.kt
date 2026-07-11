package hifumi.cresora.combat
import hifumi.cresora.StatType
object CombatStatSupport {
    const val BASE_CRIT_RATE_PERCENT = 5.0
    const val BASE_CRIT_DAMAGE_PERCENT = 50.0
    const val MAX_ATTRIBUTE_SCALAR_RATIO = 1.50
    const val MIN_ATTRIBUTE_SCALAR_RATIO = -0.75
    const val MAX_DAMAGE_BONUS_RATIO = 1.50
    const val MAX_CRIT_DAMAGE_RATIO = 2.50
    const val MAX_PLAYER_RESISTANCE_PERCENT = 75.0

    @JvmStatic
    fun effectiveDisplayValue(type: StatType, totals: Map<StatType, Double>): Double {
        return when (type) {
            StatType.CRIT_RATE -> BASE_CRIT_RATE_PERCENT + (totals[StatType.CRIT_RATE] ?: 0.0)
            StatType.CRIT_DMG -> BASE_CRIT_DAMAGE_PERCENT + (totals[StatType.CRIT_DMG] ?: 0.0)
            StatType.PHYSICAL_RESISTANCE -> CombatDamageTypeSupport.effectiveResistancePercent(totals, CombatDamageType.PHYSICAL)
            StatType.ARCANE_RESISTANCE -> CombatDamageTypeSupport.effectiveResistancePercent(totals, CombatDamageType.ARCANE)
            else -> totals[type] ?: 0.0
        }
    }

    @JvmStatic
    fun effectiveCritRateRatio(totals: Map<StatType, Double>): Double {
        return cappedCritRateRatio(effectiveDisplayValue(StatType.CRIT_RATE, totals) / 100.0)
    }

    @JvmStatic
    fun effectiveCritDamageRatio(totals: Map<StatType, Double>): Double {
        return cappedCritDamageRatio(effectiveDisplayValue(StatType.CRIT_DMG, totals) / 100.0)
    }

    @JvmStatic
    fun cappedAttributeScalarRatio(value: Double): Double {
        return value.coerceIn(MIN_ATTRIBUTE_SCALAR_RATIO, MAX_ATTRIBUTE_SCALAR_RATIO)
    }

    @JvmStatic
    fun cappedPercentScalar(percent: Double): Double {
        return cappedAttributeScalarRatio(percent / 100.0)
    }

    @JvmStatic
    fun cappedDamageBonusRatio(value: Double): Double {
        return value.coerceIn(0.0, CombatBalanceProfileRegistry.current().maxDamageBonusRatio)
    }

    @JvmStatic
    fun additiveDamageMultiplier(allDamageBonusRatio: Double, externalMultiplier: Double): Double {
        val externalBonus = externalMultiplier - 1.0
        return 1.0 + (allDamageBonusRatio + externalBonus).coerceIn(0.0, CombatBalanceProfileRegistry.current().maxDamageBonusRatio)
    }

    @JvmStatic
    fun cappedCritRateRatio(value: Double): Double {
        return value.coerceIn(0.0, 1.0)
    }

    @JvmStatic
    fun cappedCritDamageRatio(value: Double): Double {
        return value.coerceIn(0.0, CombatBalanceProfileRegistry.current().maxCritDamageRatio)
    }
}
