package hifumi.cresora.mixin;

import hifumi.cresora.bloodmoon.BloodMoonService;
import hifumi.cresora.combat.CombatDamageType;
import hifumi.cresora.combat.CombatDamageTypeSupport;
import hifumi.cresora.combat.CombatFeedbackService;
import hifumi.cresora.combat.CombatDamageResolver;
import hifumi.cresora.combat.ResolvedCombatDamage;
import hifumi.cresora.combat.CombatStatSupport;
import hifumi.cresora.equipment.EquipmentPlayerSupport;
import hifumi.cresora.masquerade.MasqueradeService;
import hifumi.cresora.weapon.WeaponCombatSupport;
import hifumi.cresora.weapon.WeaponData;
import hifumi.cresora.weapon.WeaponDefinition;
import hifumi.cresora.weapon.WeaponSkillService;
import hifumi.cresora.weapon.WeaponStackSupport;

import hifumi.cresora.StatType;

import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;

@Mixin(PlayerEntity.class)
public class PlayerEntityMixin {
    @Inject(method = "canFoodHeal", at = @At("HEAD"), cancellable = true)
    private void cresora$disableVanillaNaturalRegen(CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(false);
    }

    @ModifyVariable(method = "damage", at = @At("HEAD"), argsOnly = true)
    private float cresora$applyIncomingDamageReduction(float amount, ServerWorld world, DamageSource source) {
        PlayerEntity player = (PlayerEntity) (Object) this;
        amount = MasqueradeService.INSTANCE.adjustIncomingDamage(player, source, amount);
        if (amount <= 0.0F) {
            return 0.0F;
        }
        Map<StatType, Double> totals = EquipmentPlayerSupport.getAggregatedStats(player);
        CombatDamageType damageType = CombatDamageTypeSupport.damageSourceType(source);
        double reduction = CombatDamageTypeSupport.effectiveResistanceRatio(totals, damageType);
        if (reduction <= 0.0) {
            return amount;
        }
        if (player instanceof ServerPlayerEntity) {
            ServerPlayerEntity serverPlayer = (ServerPlayerEntity) player;
            reduction = MasqueradeService.INSTANCE.clampPlayerDamageReduction(serverPlayer, reduction);
        }
        reduction = Math.min(0.95, Math.max(0.0, reduction));
        return (float) (amount * (1.0 - reduction));
    }

    @Inject(method = "getDamageAgainst", at = @At("RETURN"), cancellable = true)
    private void cresora$applyOffenseStats(CallbackInfoReturnable<Float> cir) {
        PlayerEntity player = (PlayerEntity) (Object) this;
        if (!(player instanceof ServerPlayerEntity serverPlayer)) {
            return;
        }
        ResolvedCombatDamage resolved = CombatDamageResolver.resolvePlayerOffense(serverPlayer, cir.getReturnValueF(), true);
        if (resolved.getCritical()) {
            CombatFeedbackService.INSTANCE.recordCrit(serverPlayer, resolved.getCritMultiplier());
            player.playSound(SoundEvents.ENTITY_PLAYER_ATTACK_CRIT, 0.8F, 1.0F);
        }
        cir.setReturnValue((float) resolved.getDamage());
    }
}
