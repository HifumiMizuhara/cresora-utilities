package hifumi.cresora.weapon
object WeaponCombatSupport {
    private const val PLAYER_BASE_ATTACK_DAMAGE = 1.0
    private const val PLAYER_BASE_ATTACK_SPEED = 4.0

    fun attackDamage(definition: WeaponDefinition, data: WeaponData): Double {
        val curve = definition.attackCurve
        val tuning = hifumi.cresora.combat.CombatBalanceProfileRegistry.current()
        val raw = if (curve.isEmpty()) {
            definition.baseAttackDamage * tuning.weaponBaseDamageScalar +
                (data.baseLevel - 1).coerceAtLeast(0) * definition.attackDamagePerLevel * tuning.weaponLevelGrowthScalar
        } else {
            val level = data.baseLevel.coerceIn(1, definition.maxBaseLevel)
            if (level <= curve.first().level) {
                curve.first().attackDamage
            } else {
                var calculated = curve.last().attackDamage
                for (index in 1 until curve.size) {
                    val previous = curve[index - 1]
                    val next = curve[index]
                    if (level > next.level) {
                        continue
                    }
                    val span = (next.level - previous.level).coerceAtLeast(1)
                    val progress = (level - previous.level).toDouble() / span.toDouble()
                    calculated = previous.attackDamage + (next.attackDamage - previous.attackDamage) * progress
                    break
                }
                calculated
            }
        }
        return raw * (1.0 + WeaponUpgradeService.breakthroughAttackFactor() * data.breakthrough)
    }

    fun attackDamageModifier(definition: WeaponDefinition, data: WeaponData): Double {
        return attackDamage(definition, data) - PLAYER_BASE_ATTACK_DAMAGE
    }

    fun attackSpeedModifier(definition: WeaponDefinition): Double {
        return definition.totalAttackSpeed - PLAYER_BASE_ATTACK_SPEED
    }

    fun critRateBonusPercent(definition: WeaponDefinition, data: WeaponData? = null): Double {
        val baseCrit = definition.critRateBonusPercent
        val btBonus = WeaponUpgradeService.breakthroughCritRateBonus(data?.breakthrough ?: 0)
        return baseCrit + btBonus
    }

    fun totalCritRateBonusPercent(player: net.minecraft.server.network.ServerPlayerEntity, definition: WeaponDefinition, data: WeaponData? = null): Double {
        return critRateBonusPercent(definition, data) + WeaponSkillService.critRateBonusPercent(player, definition.id)
    }

    fun totalCritDamageBonusPercent(player: net.minecraft.server.network.ServerPlayerEntity, definition: WeaponDefinition): Double {
        return WeaponSkillService.critDamageBonusPercent(player, definition.id)
    }

    fun allDamageBonusPercent(definition: WeaponDefinition, data: WeaponData): Double {
        val maxBonus = definition.maxAllDamageBonusPercent.coerceAtLeast(0.0)
        val normalBonus = if (maxBonus <= 0.0 || definition.maxBaseLevel <= 1) {
            0.0
        } else {
            val progress = (data.baseLevel.coerceIn(1, definition.maxBaseLevel) - 1).toDouble() / (definition.maxBaseLevel - 1).toDouble()
            maxBonus * progress.coerceIn(0.0, 1.0)
        }
        val btBonus = WeaponUpgradeService.breakthroughAllDamageBonus(data.breakthrough)
        return normalBonus + btBonus
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

    fun healHp(player: net.minecraft.server.network.ServerPlayerEntity, amount: Float) {
        player.heal(amount)
    }

    fun healHp(player: net.minecraft.server.network.ServerPlayerEntity, amount: Double) {
        player.heal(amount.toFloat())
    }

    fun grantShield(player: net.minecraft.server.network.ServerPlayerEntity, amount: Float, durationTicks: Long) {
        WeaponSkillService.grantShield(player, amount, durationTicks, null)
    }

    fun grantShield(player: net.minecraft.server.network.ServerPlayerEntity, amount: Double, durationTicks: Long) {
        WeaponSkillService.grantShield(player, amount.toFloat(), durationTicks, null)
    }

    fun applyStatusEffect(player: net.minecraft.server.network.ServerPlayerEntity, effectId: String, durationTicks: Int, amplifier: Int = 0) {
        val entry = net.minecraft.registry.Registries.STATUS_EFFECT.getEntry(net.minecraft.util.Identifier.of(effectId)).orElse(null) ?: return
        player.addStatusEffect(net.minecraft.entity.effect.StatusEffectInstance(entry, durationTicks, amplifier))
    }
}
