package hifumi.cresora

object WeaponUpgradeService {
    fun baseUpgradeCost(definition: WeaponDefinition, currentLevel: Int): Int {
        val level = currentLevel.coerceAtLeast(1)
        return definition.upgrades.baseCscQuadraticCoefficient * level * level
    }

    fun skillUpgradeCost(definition: WeaponDefinition, currentLevel: Int): Int {
        val level = currentLevel.coerceAtLeast(1)
        return definition.upgrades.skillCscLinearCoefficient * level
    }
}
