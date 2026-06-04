package hifumi.cresora.equipment
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.damage.DamageSource
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.server.network.ServerPlayerEntity
import hifumi.cresora.equipment.ArtifactSkillRegistry

object EquipmentEffectHookService {

    fun onEquipChanged(player: ServerPlayerEntity) {
        for (activeSetBonus in EquipmentPlayerSupport.getActiveSetBonuses(player)) {
            for (hook in activeSetBonus.bonus.effectHooks) {
                if (hook.trigger == EquipmentEffectTrigger.EQUIP_CHANGED) {
                    ArtifactSkillRegistry.getHandler(hook.effectId)?.onEquipChanged(player)
                }
            }
        }
    }

    fun onTick(player: ServerPlayerEntity) {
        for (activeSetBonus in EquipmentPlayerSupport.getActiveSetBonuses(player)) {
            for (hook in activeSetBonus.bonus.effectHooks) {
                if (hook.trigger == EquipmentEffectTrigger.TICK) {
                    ArtifactSkillRegistry.getHandler(hook.effectId)?.onTick(player)
                }
            }
        }
    }

    fun onAttackDealt(player: PlayerEntity, target: LivingEntity, damage: Double) {
        if (damage <= 0.0 || player !is ServerPlayerEntity) {
            return
        }
        for (activeSetBonus in EquipmentPlayerSupport.getActiveSetBonuses(player)) {
            for (hook in activeSetBonus.bonus.effectHooks) {
                if (hook.trigger == EquipmentEffectTrigger.ATTACK_DEALT) {
                    ArtifactSkillRegistry.getHandler(hook.effectId)?.onAttackDealt(player, target, damage)
                }
            }
        }
    }

    fun onDamageTaken(player: PlayerEntity, source: DamageSource, damage: Double) {
        if (damage <= 0.0 || player !is ServerPlayerEntity) {
            return
        }
        for (activeSetBonus in EquipmentPlayerSupport.getActiveSetBonuses(player)) {
            for (hook in activeSetBonus.bonus.effectHooks) {
                if (hook.trigger == EquipmentEffectTrigger.DAMAGE_TAKEN) {
                    ArtifactSkillRegistry.getHandler(hook.effectId)?.onDamageTaken(player, source, damage)
                }
            }
        }
    }

    fun onKill(player: ServerPlayerEntity, target: LivingEntity) {
        for (activeSetBonus in EquipmentPlayerSupport.getActiveSetBonuses(player)) {
            for (hook in activeSetBonus.bonus.effectHooks) {
                if (hook.trigger == EquipmentEffectTrigger.KILL) {
                    ArtifactSkillRegistry.getHandler(hook.effectId)?.onKill(player, target)
                }
            }
        }
    }

    // dispatch function removed in favor of explicit calls for type safety

    fun getDisplayStacks(player: ServerPlayerEntity, buffId: String, rawStacks: Int): Int {
        var total = rawStacks
        for (activeSetBonus in EquipmentPlayerSupport.getActiveSetBonuses(player)) {
            for (hook in activeSetBonus.bonus.effectHooks) {
                val handler = ArtifactSkillRegistry.getHandler(hook.effectId) ?: continue
                total += handler.getDisplayStackBonus(player, buffId)
            }
        }
        return total
    }
}
