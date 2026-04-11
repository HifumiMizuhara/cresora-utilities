package hifumi.cresora

object WeaponCombatSupport {
    private const val PLAYER_BASE_ATTACK_DAMAGE = 1.0
    private const val PLAYER_BASE_ATTACK_SPEED = 4.0

    fun attackDamage(definition: WeaponDefinition, data: WeaponData): Double {
        val curve = definition.attackCurve
        if (curve.isEmpty()) {
            return definition.baseAttackDamage + (data.baseLevel - 1).coerceAtLeast(0) * definition.attackDamagePerLevel
        }
        val level = data.baseLevel.coerceIn(1, definition.maxBaseLevel)
        if (level <= curve.first().level) {
            return curve.first().attackDamage
        }
        for (index in 1 until curve.size) {
            val previous = curve[index - 1]
            val next = curve[index]
            if (level > next.level) {
                continue
            }
            val span = (next.level - previous.level).coerceAtLeast(1)
            val progress = (level - previous.level).toDouble() / span.toDouble()
            return previous.attackDamage + (next.attackDamage - previous.attackDamage) * progress
        }
        return curve.last().attackDamage
    }

    fun attackDamageModifier(definition: WeaponDefinition, data: WeaponData): Double {
        return attackDamage(definition, data) - PLAYER_BASE_ATTACK_DAMAGE
    }

    fun attackSpeedModifier(definition: WeaponDefinition): Double {
        return definition.totalAttackSpeed - PLAYER_BASE_ATTACK_SPEED
    }

    fun critRateBonusPercent(definition: WeaponDefinition): Double = definition.critRateBonusPercent

    fun totalCritRateBonusPercent(player: net.minecraft.server.network.ServerPlayerEntity, definition: WeaponDefinition): Double {
        var bonus = critRateBonusPercent(definition)
        for (handler in hifumi.cresora.skill.WeaponSkillRegistry.allHandlers()) {
            bonus += handler.getCritRateBonus(player)
        }
        return bonus
    }

    fun totalCritDamageBonusPercent(player: net.minecraft.server.network.ServerPlayerEntity, definition: WeaponDefinition): Double {
        var bonus = 0.0
        // Legacy check for specific weapons if needed, or just use the new dynamic system
        bonus += WeaponSkillService.critDamageBonusPercent(player, definition.id)
        for (handler in hifumi.cresora.skill.WeaponSkillRegistry.allHandlers()) {
            bonus += handler.getCritDamageBonus(player)
        }
        return bonus
    }

    fun allDamageBonusPercent(definition: WeaponDefinition, data: WeaponData): Double {
        val maxBonus = definition.maxAllDamageBonusPercent.coerceAtLeast(0.0)
        if (maxBonus <= 0.0 || definition.maxBaseLevel <= 1) {
            return 0.0
        }
        val progress = (data.baseLevel.coerceIn(1, definition.maxBaseLevel) - 1).toDouble() / (definition.maxBaseLevel - 1).toDouble()
        return maxBonus * progress.coerceIn(0.0, 1.0)
    }

    fun skillValueHearts(definition: WeaponDefinition, data: WeaponData): Double {
        return definition.skill.baseValue + definition.skill.valuePerLevel * data.skillLevel
    }

    fun secondarySkillValueHearts(definition: WeaponDefinition, data: WeaponData): Double {
        return definition.skill.secondaryBaseValue + definition.skill.secondaryValuePerLevel * data.skillLevel
    }

    fun skillValuePercent(definition: WeaponDefinition, data: WeaponData): Double {
        return definition.skill.baseValue + definition.skill.valuePerLevel * data.skillLevel
    }

    fun secondarySkillValuePercent(definition: WeaponDefinition, data: WeaponData): Double {
        return definition.skill.secondaryBaseValue + definition.skill.secondaryValuePerLevel * data.skillLevel
    }

    fun skillValueHp(definition: WeaponDefinition, data: WeaponData): Float {
        return (skillValueHearts(definition, data) * 2.0).toFloat()
    }

    fun secondarySkillValueHp(definition: WeaponDefinition, data: WeaponData): Float {
        return (secondarySkillValueHearts(definition, data) * 2.0).toFloat()
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

    fun currentHpTrueDamageRatio(definition: WeaponDefinition, data: WeaponData): Double {
        return (skillValuePercent(definition, data) / 100.0).coerceAtLeast(0.0)
    }
}
