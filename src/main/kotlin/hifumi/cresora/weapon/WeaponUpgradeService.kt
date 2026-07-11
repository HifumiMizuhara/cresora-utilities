package hifumi.cresora.weapon

import hifumi.cresora.combat.CombatBalanceProfileRegistry

object WeaponUpgradeService {
    fun breakthroughAttackFactor(): Double = CombatBalanceProfileRegistry.current().weaponBreakthroughAttackFactor

    fun breakthroughCritRateBonus(breakthrough: Int): Double {
        return breakthrough.coerceIn(0, 2) * CombatBalanceProfileRegistry.current().weaponBreakthroughCritRateBonus
    }

    fun breakthroughAllDamageBonus(breakthrough: Int): Double {
        return if (breakthrough.coerceIn(0, 2) >= 2) CombatBalanceProfileRegistry.current().weaponBreakthroughAllDamageBonus else 0.0
    }

    fun baseUpgradeCost(definition: WeaponDefinition, currentLevel: Int): Int {
        val level = currentLevel.coerceAtLeast(1)
        return when {
            level <= 20 -> 250 + level * 180
            level <= 40 -> 4_000 + (level - 20) * 650
            else -> 17_000 + (level - 40) * 1_400
        }
    }

    fun skillUpgradeCost(definition: WeaponDefinition, currentLevel: Int): Int {
        val level = currentLevel.coerceAtLeast(1)
        val configured = definition.upgrades.skillCscLinearCoefficient * level +
            definition.upgrades.skillCscQuadraticCoefficient * level * level
        return (configured * 0.6).toInt().coerceAtLeast(750)
    }

    fun skillArtifactCost(definition: WeaponDefinition, currentLevel: Int): Int {
        val level = currentLevel.coerceAtLeast(1)
        return definition.upgrades.skillArtifactCountPerLevel * level
    }

    fun levelCap(definition: WeaponDefinition, breakthrough: Int): Int {
        val bt = breakthrough.coerceIn(0, 2)
        return when (bt) {
            0 -> definition.maxBaseLevel / 2
            1 -> (definition.maxBaseLevel * 3) / 4
            else -> definition.maxBaseLevel
        }
    }

    fun maxSkillLevelForBreakthrough(breakthrough: Int): Int {
        val bt = breakthrough.coerceIn(0, 2)
        return when (bt) {
            0 -> 3
            1 -> 7
            else -> 10
        }
    }

    fun breakthroughCscCost(rarity: WeaponRarity, targetBreakthrough: Int): Int {
        return when (targetBreakthrough) {
            1 -> when (rarity) {
                WeaponRarity.TWO_STAR -> 4_000
                WeaponRarity.THREE_STAR -> 8_000
                WeaponRarity.FOUR_STAR -> 20_000
                WeaponRarity.FIVE_STAR -> 40_000
            }
            2 -> when (rarity) {
                WeaponRarity.TWO_STAR -> 16_000
                WeaponRarity.THREE_STAR -> 32_000
                WeaponRarity.FOUR_STAR -> 80_000
                WeaponRarity.FIVE_STAR -> 160_000
            }
            else -> 0
        }
    }

    fun breakthroughFragmentCost(rarity: WeaponRarity, targetBreakthrough: Int): Int {
        return when (targetBreakthrough) {
            1 -> when (rarity) {
                WeaponRarity.TWO_STAR -> 4
                WeaponRarity.THREE_STAR -> 6
                WeaponRarity.FOUR_STAR -> 8
                WeaponRarity.FIVE_STAR -> 10
            }
            2 -> when (rarity) {
                WeaponRarity.TWO_STAR -> 8
                WeaponRarity.THREE_STAR -> 12
                WeaponRarity.FOUR_STAR -> 16
                WeaponRarity.FIVE_STAR -> 20
            }
            else -> 0
        }
    }

    fun breakthroughRoleMaterialCost(rarity: WeaponRarity, targetBreakthrough: Int): Int {
        return when (targetBreakthrough) {
            1 -> when (rarity) {
                WeaponRarity.TWO_STAR -> 1
                WeaponRarity.THREE_STAR -> 2
                WeaponRarity.FOUR_STAR -> 4
                WeaponRarity.FIVE_STAR -> 8
            }
            2 -> when (rarity) {
                WeaponRarity.TWO_STAR -> 1
                WeaponRarity.THREE_STAR -> 2
                WeaponRarity.FOUR_STAR -> 4
                WeaponRarity.FIVE_STAR -> 8
            }
            else -> 0
        }
    }
}
