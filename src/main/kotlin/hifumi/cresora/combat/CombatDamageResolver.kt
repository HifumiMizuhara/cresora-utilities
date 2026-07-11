package hifumi.cresora.combat

import hifumi.cresora.StatType
import hifumi.cresora.bloodmoon.BloodMoonService
import hifumi.cresora.equipment.EquipmentPlayerSupport
import hifumi.cresora.weapon.WeaponCombatSupport
import hifumi.cresora.weapon.WeaponSkillService
import hifumi.cresora.weapon.WeaponStackSupport
import net.minecraft.server.network.ServerPlayerEntity

data class CombatDamageRequest(
    val baseDamage: Double,
    val damageType: CombatDamageType,
    val allDamageBonusRatio: Double = 0.0,
    val externalDamageMultiplier: Double = 1.0,
    val critRateRatio: Double = 0.0,
    val critDamageRatio: Double = 0.0,
    val targetResistanceRatio: Double = 0.0,
    val resistanceShredRatio: Double = 0.0,
    val canCrit: Boolean = true,
    val trueDamage: Boolean = false
)

data class ResolvedCombatDamage(
    val damage: Double,
    val critical: Boolean,
    val critMultiplier: Double,
    val effectiveResistanceRatio: Double,
    val trueDamage: Boolean
)

object CombatDamageResolver {
    @JvmStatic
    fun resolve(request: CombatDamageRequest, critRoll: Double? = null): ResolvedCombatDamage {
        val base = request.baseDamage.coerceAtLeast(0.0)
        if (request.trueDamage) {
            return ResolvedCombatDamage(base, false, 1.0, 0.0, true)
        }

        val profile = CombatBalanceProfileRegistry.current()
        val damageBonus = (request.allDamageBonusRatio + request.externalDamageMultiplier - 1.0)
            .coerceIn(0.0, profile.maxDamageBonusRatio)
        val critRate = request.critRateRatio.coerceIn(0.0, 1.0)
        val critDamage = request.critDamageRatio.coerceIn(0.0, profile.maxCritDamageRatio)
        val critical = request.canCrit && critRoll != null && critRoll < critRate
        val critMultiplier = if (critical) 1.0 + critDamage else 1.0
        val resistance = effectiveMobResistanceRatio(request.targetResistanceRatio, request.resistanceShredRatio)

        return ResolvedCombatDamage(
            damage = base * (1.0 + damageBonus) * critMultiplier * (1.0 - resistance),
            critical = critical,
            critMultiplier = critMultiplier,
            effectiveResistanceRatio = resistance,
            trueDamage = false
        )
    }

    @JvmStatic
    fun resolvePlayerOffense(player: ServerPlayerEntity, baseDamage: Double, canCrit: Boolean = true): ResolvedCombatDamage {
        val totals = EquipmentPlayerSupport.getAggregatedStats(player)
        val definition = WeaponStackSupport.getDefinition(player.mainHandStack)
        val data = WeaponStackSupport.getWeaponData(player.mainHandStack)
        val weaponCritRate = definition?.let { WeaponCombatSupport.totalCritRateBonusPercent(player, it, data) } ?: 0.0
        val weaponCritDamage = definition?.let { WeaponCombatSupport.totalCritDamageBonusPercent(player, it) / 100.0 } ?: 0.0
        val weaponAllDamage = if (definition != null && data != null) {
            (WeaponCombatSupport.allDamageBonusPercent(definition, data) + WeaponSkillService.allDamageBonusPercent(player, definition.id)) / 100.0
        } else {
            0.0
        }

        return resolve(
            CombatDamageRequest(
                baseDamage = baseDamage,
                damageType = CombatDamageTypeSupport.playerAttackDamageType(player),
                allDamageBonusRatio = (totals[StatType.ALL_DMG_BONUS] ?: 0.0) / 100.0 + weaponAllDamage,
                externalDamageMultiplier = BloodMoonService.playerDamageMultiplier(player),
                critRateRatio = CombatStatSupport.effectiveCritRateRatio(totals) + weaponCritRate / 100.0,
                critDamageRatio = CombatStatSupport.effectiveCritDamageRatio(totals) + weaponCritDamage,
                canCrit = canCrit
            ),
            if (canCrit) player.random.nextDouble() else null
        )
    }

    @JvmStatic
    fun effectiveMobResistanceRatio(baseResistanceRatio: Double, resistanceShredRatio: Double): Double {
        val profile = CombatBalanceProfileRegistry.current()
        return (baseResistanceRatio - resistanceShredRatio)
            .coerceIn(profile.minMobResistancePercent / 100.0, profile.maxMobResistancePercent / 100.0)
    }

    @JvmStatic
    fun applyMobResistance(amount: Double, baseResistanceRatio: Double, resistanceShredRatio: Double): Double {
        return amount.coerceAtLeast(0.0) * (1.0 - effectiveMobResistanceRatio(baseResistanceRatio, resistanceShredRatio))
    }
}
