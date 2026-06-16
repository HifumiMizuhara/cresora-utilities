package hifumi.cresora.adventurerank
import hifumi.cresora.combat.HostileRewardFamilies
import hifumi.cresora.combat.HostileRewardFamily
import hifumi.cresora.equipment.EquipmentRarity
import net.minecraft.entity.EntityType
import net.minecraft.util.math.random.Random

object AdventureRankProfile {
    const val MOB_HEALTH_CAP: Double = 360.0

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
        val phaseBonus = phaseIndex(rank) * 0.10
        return when (HostileRewardFamilies.classify(entityType)) {
            HostileRewardFamily.SURVIVOR -> 1.0 + 2.0 * progress + phaseBonus
            HostileRewardFamily.ASSAULT -> 1.0 + 2.2 * progress + phaseBonus
            HostileRewardFamily.ARCANE -> 1.0 + 1.9 * progress + phaseBonus
            HostileRewardFamily.ELITE -> 1.0 + 2.6 * progress + phaseBonus * 1.5
            HostileRewardFamily.RELIC -> 1.0 + 0.08 * progress
        }
    }

    fun defenseBonus(entityType: EntityType<*>, rank: Int, overflowHealth: Double): DefenseBonus {
        val progress = AdventureRankProgression.normalizedProgress(rank)
        val phase = phaseIndex(rank)
        val family = HostileRewardFamilies.classify(entityType)
        val baseArmor = when (family) {
            HostileRewardFamily.SURVIVOR -> 1.0 + 3.0 * progress + phase * 0.8
            HostileRewardFamily.ASSAULT -> 1.0 + 3.2 * progress + phase * 0.8
            HostileRewardFamily.ARCANE -> 0.8 + 2.8 * progress + phase * 0.7
            HostileRewardFamily.ELITE -> 1.6 + 4.4 * progress + phase * 1.1
            HostileRewardFamily.RELIC -> 5.0 + 4.0 * progress
        }
        val baseToughness = when (family) {
            HostileRewardFamily.SURVIVOR -> 0.2 + 0.9 * progress + phase * 0.2
            HostileRewardFamily.ASSAULT -> 0.2 + 0.9 * progress + phase * 0.2
            HostileRewardFamily.ARCANE -> 0.3 + 1.0 * progress + phase * 0.2
            HostileRewardFamily.ELITE -> 0.6 + 1.4 * progress + phase * 0.3
            HostileRewardFamily.RELIC -> 2.5 + 1.6 * progress
        }

        if (overflowHealth <= 0.0) {
            return DefenseBonus(baseArmor, baseToughness)
        }

        val overflowArmorCoefficient = when (family) {
            HostileRewardFamily.SURVIVOR -> 0.12
            HostileRewardFamily.ASSAULT -> 0.11
            HostileRewardFamily.ARCANE -> 0.10
            HostileRewardFamily.ELITE -> 0.15
            HostileRewardFamily.RELIC -> 0.22
        }
        val overflowToughnessCoefficient = when (family) {
            HostileRewardFamily.SURVIVOR -> 0.03
            HostileRewardFamily.ASSAULT -> 0.03
            HostileRewardFamily.ARCANE -> 0.03
            HostileRewardFamily.ELITE -> 0.04
            HostileRewardFamily.RELIC -> 0.07
        }

        return DefenseBonus(
            armorFlat = baseArmor + (overflowHealth / 10.0) * overflowArmorCoefficient,
            toughnessFlat = baseToughness + (overflowHealth / 25.0) * overflowToughnessCoefficient
        )
    }

    fun damageMultiplier(entityType: EntityType<*>, rank: Int): Double {
        val progress = AdventureRankProgression.normalizedProgress(rank)
        val cap = when (HostileRewardFamilies.classify(entityType)) {
            HostileRewardFamily.SURVIVOR -> 0.55
            HostileRewardFamily.ASSAULT -> 0.65
            HostileRewardFamily.ARCANE -> 0.70
            HostileRewardFamily.ELITE -> 0.82
            HostileRewardFamily.RELIC -> 1.00
        }
        return 1.0 + cap * progress + phaseIndex(rank) * 0.04
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

    private fun phaseIndex(rank: Int): Int {
        val normalized = AdventureRankProgression.sanitizeRank(rank)
        return when {
            normalized >= 56 -> 2
            normalized >= 31 -> 1
            else -> 0
        }
    }
}
