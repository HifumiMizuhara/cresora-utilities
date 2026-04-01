package hifumi.cresora

import com.mojang.serialization.Codec

enum class StatType(val id: String) {
    ATK_FLAT("atk_flat"),
    ATK_PERCENT("atk_percent"),
    HP_FLAT("hp_flat"),
    HP_PERCENT("hp_percent"),
    DEF_FLAT("def_flat"),
    DEF_PERCENT("def_percent"),
    CRIT_RATE("crit_rate"),
    CRIT_DMG("crit_dmg"),
    ALL_DMG_BONUS("all_dmg_bonus"),
    PHYSICAL_RESISTANCE("physical_resistance"),
    ARCANE_RESISTANCE("arcane_resistance"),
    DAMAGE_REDUCTION("damage_reduction");

    fun translationKey(): String = "item.cresora.equipment.stat.$id"

    fun isLegacyOnly(): Boolean {
        return this == DAMAGE_REDUCTION
    }

    fun isPercent(): Boolean {
        return when (this) {
            ATK_PERCENT, HP_PERCENT, DEF_PERCENT, CRIT_RATE, CRIT_DMG, ALL_DMG_BONUS, PHYSICAL_RESISTANCE, ARCANE_RESISTANCE, DAMAGE_REDUCTION -> true
            ATK_FLAT, HP_FLAT, DEF_FLAT -> false
        }
    }

    fun allowedAsMainStat(): Boolean {
        return when (this) {
            CRIT_RATE, CRIT_DMG -> false
            ATK_FLAT, ATK_PERCENT, HP_FLAT, HP_PERCENT, DEF_FLAT, DEF_PERCENT, ALL_DMG_BONUS, PHYSICAL_RESISTANCE, ARCANE_RESISTANCE, DAMAGE_REDUCTION -> true
        }
    }

    companion object {
        private val BY_ID = entries.associateBy(StatType::id)

        @JvmStatic
        fun activeEntries(): List<StatType> = entries.filter { !it.isLegacyOnly() }

        val CODEC: Codec<StatType> = Codec.STRING.xmap(
            { id -> BY_ID[id] ?: throw IllegalArgumentException("Unknown stat type: $id") },
            StatType::id
        )
    }
}
