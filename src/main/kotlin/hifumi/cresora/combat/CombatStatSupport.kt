package hifumi.cresora.combat
import hifumi.cresora.StatType
object CombatStatSupport {
    const val BASE_CRIT_RATE_PERCENT = 5.0
    const val BASE_CRIT_DAMAGE_PERCENT = 50.0

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
        return (effectiveDisplayValue(StatType.CRIT_RATE, totals) / 100.0).coerceIn(0.0, 1.0)
    }

    @JvmStatic
    fun effectiveCritDamageRatio(totals: Map<StatType, Double>): Double {
        return (effectiveDisplayValue(StatType.CRIT_DMG, totals) / 100.0).coerceAtLeast(0.0)
    }
}
