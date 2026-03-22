package hifumi.cresora

import net.minecraft.util.math.random.Random

object EquipmentGenerationService {
    private val MAIN_STAT_POOL = StatType.entries.filter(StatType::allowedAsMainStat)
    private val SUB_STAT_POOL = StatType.entries

    fun createPendant(random: Random, startingLevel: Int = 0, forcedRarity: EquipmentRarity? = null): EquipmentData {
        val rarity = forcedRarity ?: rollRarity(random)
        val mainType = MAIN_STAT_POOL[random.nextInt(MAIN_STAT_POOL.size)]
        var data = EquipmentData(
            rarity = rarity,
            level = 0,
            mainStat = StatEntry(mainType, rollMainStatValue(mainType, rarity, random)),
            subStats = emptyList(),
            upgradeCount = 0
        )

        repeat(rarity.initialSubStatCount) {
            data = data.copy(subStats = data.subStats + rollNewSubStat(data, random))
        }

        return EquipmentUpgradeService.applyLevels(
            data = data.normalized(),
            levelGain = startingLevel.coerceAtLeast(0),
            random = random
        )
    }

    fun rollNewSubStat(data: EquipmentData, random: Random): StatEntry {
        val excludedTypes = data.subStats.mapTo(mutableSetOf()) { it.type }
        excludedTypes += data.mainStat.type
        val candidates = SUB_STAT_POOL.filterNot(excludedTypes::contains)
        val type = candidates[random.nextInt(candidates.size)]
        return StatEntry(type, rollSubStatValue(type, data.rarity, random))
    }

    fun rollSubStatIncrease(type: StatType, rarity: EquipmentRarity, random: Random): Double {
        return rollValue(type, rarity, subStat = true, random = random)
    }

    fun rollMainStatIncrease(type: StatType, rarity: EquipmentRarity, random: Random): Double {
        return rollValue(type, rarity, subStat = false, random = random) * 0.45
    }

    private fun rollRarity(random: Random): EquipmentRarity {
        return when (random.nextInt(100)) {
            in 0..49 -> EquipmentRarity.THREE_STAR
            in 50..84 -> EquipmentRarity.FOUR_STAR
            else -> EquipmentRarity.FIVE_STAR
        }
    }

    private fun rollMainStatValue(type: StatType, rarity: EquipmentRarity, random: Random): Double {
        return rollValue(type, rarity, subStat = false, random = random)
    }

    private fun rollSubStatValue(type: StatType, rarity: EquipmentRarity, random: Random): Double {
        return rollValue(type, rarity, subStat = true, random = random)
    }

    private fun rollValue(type: StatType, rarity: EquipmentRarity, subStat: Boolean, random: Random): Double {
        val rarityScale = when (rarity) {
            EquipmentRarity.THREE_STAR -> 1.0
            EquipmentRarity.FOUR_STAR -> 1.35
            EquipmentRarity.FIVE_STAR -> 1.7
        }
        val subScale = if (subStat) 0.68 else 1.0
        val base = when (type) {
            StatType.ATK_FLAT -> 2.2
            StatType.ATK_PERCENT -> 2.1
            StatType.HP_FLAT -> 8.0
            StatType.HP_PERCENT -> 2.4
            StatType.DEF_FLAT -> 2.0
            StatType.DEF_PERCENT -> 2.1
            StatType.CRIT_RATE -> 1.4
            StatType.CRIT_DMG -> 2.8
            StatType.ALL_DMG_BONUS -> 1.8
            StatType.DAMAGE_REDUCTION -> 1.6
        }
        val variance = 0.92 + random.nextDouble() * 0.16
        return ((base * rarityScale * subScale * variance) * 100.0).toInt() / 100.0
    }
}
