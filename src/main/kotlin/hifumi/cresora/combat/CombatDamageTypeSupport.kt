package hifumi.cresora.combat
import hifumi.cresora.StatType
import hifumi.cresora.weapon.WeaponDefinition
import hifumi.cresora.weapon.WeaponStackSupport
import net.minecraft.entity.Entity
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.damage.DamageSource
import net.minecraft.entity.damage.DamageTypes
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.registry.tag.DamageTypeTags

object CombatDamageTypeSupport {
    @JvmStatic
    fun weaponDamageType(definition: WeaponDefinition?): CombatDamageType {
        return definition?.damageType ?: CombatDamageType.PHYSICAL
    }

    @JvmStatic
    fun playerAttackDamageType(player: PlayerEntity): CombatDamageType {
        return weaponDamageType(WeaponStackSupport.getDefinition(player.mainHandStack))
    }

    @JvmStatic
    fun entityAttackDamageType(entity: Entity?): CombatDamageType? {
        return when (entity) {
            is PlayerEntity -> playerAttackDamageType(entity)
            is LivingEntity -> MobCombatProfileRegistry.attackType(entity.type)
            else -> null
        }
    }

    @JvmStatic
    fun damageSourceType(source: DamageSource): CombatDamageType {
        entityAttackDamageType(source.attacker ?: source.source)?.let { return it }
        if (source.attacker is PlayerEntity) {
            return playerAttackDamageType(source.attacker as PlayerEntity)
        }
        if (
            source.isOf(DamageTypes.MAGIC) ||
            source.isOf(DamageTypes.INDIRECT_MAGIC) ||
            source.isOf(DamageTypes.LIGHTNING_BOLT) ||
            source.isOf(DamageTypes.DRAGON_BREATH) ||
            source.isOf(DamageTypes.WITHER) ||
            source.isOf(DamageTypes.SONIC_BOOM) ||
            source.isIn(DamageTypeTags.IS_FIRE) ||
            source.isIn(DamageTypeTags.IS_FREEZING)
        ) {
            return CombatDamageType.ARCANE
        }
        return CombatDamageType.PHYSICAL
    }

    @JvmStatic
    fun effectiveResistancePercent(totals: Map<StatType, Double>, damageType: CombatDamageType): Double {
        val legacy = totals[StatType.DAMAGE_REDUCTION] ?: 0.0
        val typed = when (damageType) {
            CombatDamageType.PHYSICAL -> totals[StatType.PHYSICAL_RESISTANCE] ?: 0.0
            CombatDamageType.ARCANE -> totals[StatType.ARCANE_RESISTANCE] ?: 0.0
        }
        return (legacy + typed).coerceAtLeast(0.0)
    }

    @JvmStatic
    fun effectiveResistanceRatio(totals: Map<StatType, Double>, damageType: CombatDamageType): Double {
        return effectiveResistancePercent(totals, damageType) / 100.0
    }
}
