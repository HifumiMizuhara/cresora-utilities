package hifumi.cresora

import net.minecraft.util.math.random.Random

object AdventureRankProfile {
    data class LootBonus(
        val levelBonus: Int,
        val rarityUpgradeChance: Double,
        val guaranteedExtraCount: Int,
        val bonusExtraCountChance: Double
    )

    fun healthMultiplier(rank: Int): Double {
        val normalized = AdventureRankProgression.sanitizeRank(rank)
        return 1.0 + (normalized - 1) * 0.18
    }

    fun damageMultiplier(rank: Int): Double {
        val normalized = AdventureRankProgression.sanitizeRank(rank)
        return 1.0 + (normalized - 1) * 0.12
    }

    fun lootBonus(rank: Int): LootBonus {
        val normalized = AdventureRankProgression.sanitizeRank(rank)
        return when (normalized) {
            in 1..3 -> LootBonus(0, 0.0, 0, 0.0)
            in 4..6 -> LootBonus(1, 0.0, 0, 0.2)
            in 7..9 -> LootBonus(2, 0.35, 0, 0.45)
            else -> LootBonus(3, 0.75, 1, 0.5)
        }
    }

    fun upgradeRarity(base: EquipmentRarity, rank: Int, random: Random): EquipmentRarity {
        val bonus = lootBonus(rank)
        if (bonus.rarityUpgradeChance <= 0.0) {
            return base
        }
        return if (random.nextDouble() < bonus.rarityUpgradeChance) {
            when (base) {
                EquipmentRarity.THREE_STAR -> EquipmentRarity.FOUR_STAR
                EquipmentRarity.FOUR_STAR -> EquipmentRarity.FIVE_STAR
                EquipmentRarity.FIVE_STAR -> EquipmentRarity.FIVE_STAR
            }
        } else {
            base
        }
    }

    fun extraUpgradeMaterialCount(rank: Int, random: Random): Int {
        val bonus = lootBonus(rank)
        var total = bonus.guaranteedExtraCount
        if (bonus.bonusExtraCountChance > 0.0 && random.nextDouble() < bonus.bonusExtraCountChance) {
            total++
        }
        return total
    }
}
