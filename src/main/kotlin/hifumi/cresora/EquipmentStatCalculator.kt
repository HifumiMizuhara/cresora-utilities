package hifumi.cresora

import net.minecraft.text.Text
import java.util.EnumMap
import kotlin.math.roundToInt

object EquipmentStatCalculator {
    data class AttributeBonuses(
        val attackFlat: Double,
        val attackScalar: Double,
        val healthFlat: Double,
        val healthScalar: Double,
        val armorFlat: Double,
        val armorScalar: Double
    )

    fun aggregate(data: EquipmentData): Map<StatType, Double> {
        val totals = EnumMap<StatType, Double>(StatType::class.java)
        for (entry in listOf(data.mainStat) + data.subStats) {
            totals[entry.type] = (totals[entry.type] ?: 0.0) + entry.value
        }
        return totals
    }

    fun calculateAttributeBonuses(data: EquipmentData): AttributeBonuses {
        return calculateAttributeBonuses(aggregate(data))
    }

    fun calculateAttributeBonuses(totals: Map<StatType, Double>): AttributeBonuses {
        return AttributeBonuses(
            attackFlat = totals[StatType.ATK_FLAT] ?: 0.0,
            attackScalar = (totals[StatType.ATK_PERCENT] ?: 0.0) / 100.0,
            healthFlat = totals[StatType.HP_FLAT] ?: 0.0,
            healthScalar = (totals[StatType.HP_PERCENT] ?: 0.0) / 100.0,
            armorFlat = totals[StatType.DEF_FLAT] ?: 0.0,
            armorScalar = (totals[StatType.DEF_PERCENT] ?: 0.0) / 100.0
        )
    }

    fun formatStatLine(entry: StatEntry): Text {
        val value = if (entry.type.isPercent()) {
            "${formatNumber(entry.value)}%"
        } else {
            formatNumber(entry.value)
        }
        return Text.translatable("item.cresora.equipment.stat_line", Text.translatable(entry.type.translationKey()), value)
    }

    fun score(data: EquipmentData): Double {
        val totals = aggregate(data)
        return data.level * 10.0 +
            (totals[StatType.ATK_FLAT] ?: 0.0) * 1.0 +
            (totals[StatType.ATK_PERCENT] ?: 0.0) * 1.4 +
            (totals[StatType.ALL_DMG_BONUS] ?: 0.0) * 1.5 +
            (totals[StatType.CRIT_RATE] ?: 0.0) * 1.7 +
            (totals[StatType.CRIT_DMG] ?: 0.0) * 1.1 +
            (totals[StatType.HP_PERCENT] ?: 0.0) * 0.5 +
            (totals[StatType.DEF_PERCENT] ?: 0.0) * 0.4 +
            (totals[StatType.DAMAGE_REDUCTION] ?: 0.0) * 0.8
    }

    private fun formatNumber(value: Double): String {
        val rounded = ((value * 10.0).roundToInt()) / 10.0
        return if (rounded % 1.0 == 0.0) {
            rounded.toInt().toString()
        } else {
            rounded.toString()
        }
    }
}
