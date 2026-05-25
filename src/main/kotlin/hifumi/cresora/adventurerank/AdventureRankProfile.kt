package hifumi.cresora.adventurerank
import hifumi.cresora.combat.HostileRewardFamilies
import hifumi.cresora.combat.HostileRewardFamily
import hifumi.cresora.equipment.EquipmentRarity
import net.minecraft.entity.EntityType
import net.minecraft.util.math.random.Random
import kotlin.math.pow

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
        return when (HostileRewardFamilies.classify(entityType)) {
            HostileRewardFamily.SURVIVOR -> 1.0 + 8.8 * progress.pow(1.18)
            HostileRewardFamily.ASSAULT -> 1.0 + 8.4 * progress.pow(1.17)
            HostileRewardFamily.ARCANE -> 1.0 + 7.8 * progress.pow(1.19)
            HostileRewardFamily.ELITE -> 1.0 + 9.8 * progress.pow(1.20)
            HostileRewardFamily.RELIC -> 1.0 + 0.08 * progress.pow(1.40)
        }
    }

    fun defenseBonus(entityType: EntityType<*>, rank: Int, overflowHealth: Double): DefenseBonus {
        val progress = AdventureRankProgression.normalizedProgress(rank)
        val family = HostileRewardFamilies.classify(entityType)
        val baseArmor = when (family) {
            HostileRewardFamily.SURVIVOR -> 2.2 + 10.6 * progress.pow(1.16)
            HostileRewardFamily.ASSAULT -> 1.8 + 9.2 * progress.pow(1.14)
            HostileRewardFamily.ARCANE -> 1.6 + 8.6 * progress.pow(1.15)
            HostileRewardFamily.ELITE -> 3.2 + 12.4 * progress.pow(1.12)
            HostileRewardFamily.RELIC -> 8.0 + 12.0 * progress.pow(1.10)
        }
        val baseToughness = when (family) {
            HostileRewardFamily.SURVIVOR -> 0.8 + 3.8 * progress.pow(1.16)
            HostileRewardFamily.ASSAULT -> 0.6 + 3.2 * progress.pow(1.15)
            HostileRewardFamily.ARCANE -> 0.8 + 3.6 * progress.pow(1.14)
            HostileRewardFamily.ELITE -> 1.6 + 4.8 * progress.pow(1.12)
            HostileRewardFamily.RELIC -> 4.0 + 7.0 * progress.pow(1.08)
        }

        if (overflowHealth <= 0.0) {
            return DefenseBonus(baseArmor, baseToughness)
        }

        val overflowArmorCoefficient = when (family) {
            HostileRewardFamily.SURVIVOR -> 0.34
            HostileRewardFamily.ASSAULT -> 0.30
            HostileRewardFamily.ARCANE -> 0.28
            HostileRewardFamily.ELITE -> 0.38
            HostileRewardFamily.RELIC -> 0.40
        }
        val overflowToughnessCoefficient = when (family) {
            HostileRewardFamily.SURVIVOR -> 0.08
            HostileRewardFamily.ASSAULT -> 0.08
            HostileRewardFamily.ARCANE -> 0.07
            HostileRewardFamily.ELITE -> 0.10
            HostileRewardFamily.RELIC -> 0.12
        }

        return DefenseBonus(
            armorFlat = baseArmor + (overflowHealth / 10.0) * overflowArmorCoefficient,
            toughnessFlat = baseToughness + (overflowHealth / 25.0) * overflowToughnessCoefficient
        )
    }

    fun damageMultiplier(entityType: EntityType<*>, rank: Int): Double {
        val progress = AdventureRankProgression.normalizedProgress(rank)
        val cap = when (HostileRewardFamilies.classify(entityType)) {
            HostileRewardFamily.SURVIVOR -> 0.92
            HostileRewardFamily.ASSAULT -> 1.08
            HostileRewardFamily.ARCANE -> 1.15
            HostileRewardFamily.ELITE -> 1.24
            HostileRewardFamily.RELIC -> 1.45
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
        val level = AdventureRankProgression.sanitizeRank(rank)
        val reward = when (HostileRewardFamilies.classify(entityType)) {
            HostileRewardFamily.SURVIVOR -> 5 + level
            HostileRewardFamily.ASSAULT -> 8 + level * 2
            HostileRewardFamily.ARCANE -> 10 + level * 2
            HostileRewardFamily.ELITE -> 18 + level * 3
            HostileRewardFamily.RELIC -> 180 + level * 6
        }
        return reward.coerceAtLeast(0)
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
