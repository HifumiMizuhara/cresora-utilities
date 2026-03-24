package hifumi.cresora

import net.minecraft.entity.EntityType
import net.minecraft.util.math.random.Random
import kotlin.math.pow
import kotlin.math.roundToInt

object AdventureRankProfile {
    const val MOB_HEALTH_CAP: Double = 500.0

    data class LootBonus(
        val levelBonus: Int,
        val rarityUpgradeChance: Double,
        val guaranteedExtraCount: Int,
        val bonusExtraCountChance: Double
    )

    data class DefenseBonus(
        val armorFlat: Double,
        val toughnessFlat: Double
    )

    fun healthMultiplier(entityType: EntityType<*>, rank: Int): Double {
        val progress = AdventureRankProgression.normalizedProgress(rank)
        return when (entityType) {
            EntityType.WARDEN -> 1.0 + 0.08 * progress.pow(1.40)
            EntityType.SKELETON -> 1.0 + 8.5 * progress.pow(1.18)
            EntityType.ZOMBIE -> 1.0 + 11.0 * progress.pow(1.22)
            else -> 1.0 + 7.2 * progress.pow(1.16)
        }
    }

    fun defenseBonus(entityType: EntityType<*>, rank: Int, overflowHealth: Double): DefenseBonus {
        val progress = AdventureRankProgression.normalizedProgress(rank)
        val baseArmor = when (entityType) {
            EntityType.WARDEN -> 8.0 + 12.0 * progress.pow(1.10)
            EntityType.SKELETON -> 2.0 + 9.0 * progress.pow(1.15)
            EntityType.ZOMBIE -> 2.5 + 11.5 * progress.pow(1.18)
            else -> 1.5 + 8.0 * progress.pow(1.12)
        }
        val baseToughness = when (entityType) {
            EntityType.WARDEN -> 4.0 + 7.0 * progress.pow(1.08)
            EntityType.SKELETON -> 0.5 + 3.0 * progress.pow(1.16)
            EntityType.ZOMBIE -> 1.0 + 4.0 * progress.pow(1.18)
            else -> 0.5 + 2.6 * progress.pow(1.14)
        }

        if (overflowHealth <= 0.0) {
            return DefenseBonus(baseArmor, baseToughness)
        }

        val overflowArmorCoefficient = when (entityType) {
            EntityType.WARDEN -> 0.40
            EntityType.SKELETON -> 0.32
            EntityType.ZOMBIE -> 0.36
            else -> 0.28
        }
        val overflowToughnessCoefficient = when (entityType) {
            EntityType.WARDEN -> 0.12
            EntityType.SKELETON -> 0.08
            EntityType.ZOMBIE -> 0.09
            else -> 0.07
        }

        return DefenseBonus(
            armorFlat = baseArmor + (overflowHealth / 10.0) * overflowArmorCoefficient,
            toughnessFlat = baseToughness + (overflowHealth / 25.0) * overflowToughnessCoefficient
        )
    }

    fun damageMultiplier(entityType: EntityType<*>, rank: Int): Double {
        val progress = AdventureRankProgression.normalizedProgress(rank)
        val cap = when (entityType) {
            EntityType.WARDEN -> 1.45
            EntityType.SKELETON -> 1.10
            EntityType.ZOMBIE -> 0.95
            else -> 1.00
        }
        return 1.0 + cap * progress.pow(1.12)
    }

    fun lootBonus(rank: Int): LootBonus {
        val progress = AdventureRankProgression.normalizedProgress(rank)
        return when {
            progress < 0.15 -> LootBonus(0, 0.0, 0, 0.0)
            progress < 0.35 -> LootBonus(1, 0.10, 0, 0.15)
            progress < 0.55 -> LootBonus(2, 0.22, 0, 0.30)
            progress < 0.80 -> LootBonus(3, 0.45, 1, 0.35)
            else -> LootBonus(4, 0.70, 1, 0.60)
        }
    }

    fun killXp(entityType: EntityType<*>, rank: Int): Int {
        val progress = AdventureRankProgression.normalizedProgress(rank)
        val base = when (entityType) {
            EntityType.ZOMBIE -> 8.0
            EntityType.SKELETON -> 12.0
            EntityType.WARDEN -> 180.0
            else -> 10.0
        }
        val bonus = when (entityType) {
            EntityType.ZOMBIE -> 72.0
            EntityType.SKELETON -> 96.0
            EntityType.WARDEN -> 520.0
            else -> 80.0
        }
        return (base + bonus * progress.pow(1.05)).roundToInt()
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
