package hifumi.cresora

import net.minecraft.entity.LivingEntity
import net.minecraft.entity.damage.DamageSource
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.server.network.ServerPlayerEntity

object EquipmentEffectHookService {

    fun onEquipChanged(player: ServerPlayerEntity) {
        dispatch(player, EquipmentEffectTrigger.EQUIP_CHANGED)
    }

    fun onTick(player: ServerPlayerEntity) {
        dispatch(player, EquipmentEffectTrigger.TICK)
    }

    fun onAttackDealt(player: PlayerEntity, target: LivingEntity, damage: Double) {
        if (damage <= 0.0 || player !is ServerPlayerEntity) {
            return
        }
        dispatch(player, EquipmentEffectTrigger.ATTACK_DEALT)
    }

    fun onDamageTaken(player: PlayerEntity, source: DamageSource, damage: Double) {
        if (damage <= 0.0 || player !is ServerPlayerEntity) {
            return
        }
        dispatch(player, EquipmentEffectTrigger.DAMAGE_TAKEN)
    }

    fun onKill(player: ServerPlayerEntity, target: LivingEntity) {
        dispatch(player, EquipmentEffectTrigger.KILL)
    }

    private fun dispatch(player: ServerPlayerEntity, trigger: EquipmentEffectTrigger) {
        for (activeSetBonus in EquipmentPlayerSupport.getActiveSetBonuses(player)) {
            for (hook in activeSetBonus.bonus.effectHooks) {
                if (hook.trigger != trigger) {
                    continue
                }
                throw IllegalStateException(
                    "Unsupported equipment effect hook '${hook.effectId}' was loaded for trigger '${hook.trigger.id}'. " +
                        "Reject unsupported hooks during content load instead of ignoring them at runtime."
                )
            }
        }
    }
}
