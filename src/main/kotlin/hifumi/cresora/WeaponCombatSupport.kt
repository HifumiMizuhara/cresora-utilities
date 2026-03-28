package hifumi.cresora

object WeaponCombatSupport {
    private const val PLAYER_BASE_ATTACK_DAMAGE = 1.0
    private const val PLAYER_BASE_ATTACK_SPEED = 4.0

    fun attackDamage(definition: WeaponDefinition, data: WeaponData): Double {
        return definition.baseAttackDamage + (data.baseLevel - 1).coerceAtLeast(0) * definition.attackDamagePerLevel
    }

    fun attackDamageModifier(definition: WeaponDefinition, data: WeaponData): Double {
        return attackDamage(definition, data) - PLAYER_BASE_ATTACK_DAMAGE
    }

    fun attackSpeedModifier(definition: WeaponDefinition): Double {
        return definition.totalAttackSpeed - PLAYER_BASE_ATTACK_SPEED
    }

    fun skillValueHearts(definition: WeaponDefinition, data: WeaponData): Double {
        return definition.skill.shieldBaseHearts + definition.skill.shieldPerLevelHearts * data.skillLevel
    }

    fun skillValueHp(definition: WeaponDefinition, data: WeaponData): Float {
        return (skillValueHearts(definition, data) * 2.0).toFloat()
    }

    fun shieldHearts(definition: WeaponDefinition, data: WeaponData): Double {
        return skillValueHearts(definition, data)
    }

    fun shieldHp(definition: WeaponDefinition, data: WeaponData): Float {
        return skillValueHp(definition, data)
    }

    fun healHearts(definition: WeaponDefinition, data: WeaponData): Double {
        return skillValueHearts(definition, data)
    }

    fun healHp(definition: WeaponDefinition, data: WeaponData): Float {
        return skillValueHp(definition, data)
    }
}
