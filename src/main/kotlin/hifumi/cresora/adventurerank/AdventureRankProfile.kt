package hifumi.cresora.adventurerank
import hifumi.cresora.combat.HostileRewardFamilies
import hifumi.cresora.combat.HostileRewardFamily
import hifumi.cresora.combat.CombatBalanceProfileRegistry
import hifumi.cresora.combat.CombatFamilyBalance
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
        val balance = balanceFor(entityType)
        return 1.0 + balance.healthGrowth * progress + phaseIndex(rank) * balance.healthPhaseBonus
    }

    fun defenseBonus(entityType: EntityType<*>, rank: Int, overflowHealth: Double): DefenseBonus {
        val progress = AdventureRankProgression.normalizedProgress(rank)
        val balance = balanceFor(entityType)
        val phase = phaseIndex(rank)
        val baseArmor = balance.armorBase + balance.armorGrowth * progress + phase * balance.armorPhaseBonus
        val baseToughness = balance.toughnessBase + balance.toughnessGrowth * progress + phase * balance.toughnessPhaseBonus

        if (overflowHealth <= 0.0) {
            return DefenseBonus(baseArmor, baseToughness)
        }

        return DefenseBonus(
            armorFlat = baseArmor + (overflowHealth / 10.0) * balance.overflowArmorPerTenHealth,
            toughnessFlat = baseToughness + (overflowHealth / 25.0) * balance.overflowToughnessPerTwentyFiveHealth
        )
    }

    fun damageMultiplier(entityType: EntityType<*>, rank: Int): Double {
        val progress = AdventureRankProgression.normalizedProgress(rank)
        val balance = balanceFor(entityType)
        return 1.0 + balance.damageGrowth * progress + phaseIndex(rank) * balance.damagePhaseBonus
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

    private fun balanceFor(entityType: EntityType<*>): CombatFamilyBalance {
        val profile = CombatBalanceProfileRegistry.current()
        return when (HostileRewardFamilies.classify(entityType)) {
            HostileRewardFamily.SURVIVOR -> profile.survivor
            HostileRewardFamily.ASSAULT -> profile.assault
            HostileRewardFamily.ARCANE -> profile.arcane
            HostileRewardFamily.ELITE -> profile.elite
            HostileRewardFamily.RELIC -> profile.relic
        }
    }
}
