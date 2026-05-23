package hifumi.cresora.weapon
object WeaponUpgradeService {
    const val BREAKTHROUGH_ATTACK_FACTOR = 0.15
    const val BREAKTHROUGH_1_CRIT_RATE_BONUS = 5.0
    const val BREAKTHROUGH_2_CRIT_RATE_BONUS = 10.0
    const val BREAKTHROUGH_2_ALL_DAMAGE_BONUS = 10.0

    fun baseUpgradeCost(definition: WeaponDefinition, currentLevel: Int): Int {
        val level = currentLevel.coerceAtLeast(1)
        return definition.upgrades.baseCscQuadraticCoefficient * level * level
    }

    fun skillUpgradeCost(definition: WeaponDefinition, currentLevel: Int): Int {
        val level = currentLevel.coerceAtLeast(1)
        return definition.upgrades.skillCscLinearCoefficient * level +
            definition.upgrades.skillCscQuadraticCoefficient * level * level
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
                WeaponRarity.TWO_STAR -> 5_000
                WeaponRarity.THREE_STAR -> 10_000
                WeaponRarity.FOUR_STAR -> 25_000
                WeaponRarity.FIVE_STAR -> 50_000
            }
            2 -> when (rarity) {
                WeaponRarity.TWO_STAR -> 20_000
                WeaponRarity.THREE_STAR -> 40_000
                WeaponRarity.FOUR_STAR -> 100_000
                WeaponRarity.FIVE_STAR -> 200_000
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
}
