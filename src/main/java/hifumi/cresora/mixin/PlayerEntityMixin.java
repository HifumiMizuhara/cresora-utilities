package hifumi.cresora.mixin;

import hifumi.cresora.bloodmoon.BloodMoonService;
import hifumi.cresora.combat.CombatDamageType;
import hifumi.cresora.combat.CombatDamageTypeSupport;
import hifumi.cresora.combat.CombatFeedbackService;
import hifumi.cresora.combat.CombatStatSupport;
import hifumi.cresora.debuff.CresoraDebuffService;
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
        if (player instanceof ServerPlayerEntity serverPlayer) {
            amount = (float) (amount * CresoraDebuffService.INSTANCE.getIncomingDamageMultiplier(serverPlayer));
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
        double debuffMultiplier = 1.0;
        if (player instanceof ServerPlayerEntity serverPlayer) {
            debuffMultiplier = CresoraDebuffService.INSTANCE.getAttackMultiplier(serverPlayer);
        }
        Map<StatType, Double> totals = EquipmentPlayerSupport.getAggregatedStats(player);
        WeaponDefinition weaponDefinition = WeaponStackSupport.INSTANCE.getDefinition(player.getMainHandStack());
        WeaponData weaponData = WeaponStackSupport.INSTANCE.getWeaponData(player.getMainHandStack());
        
        double weaponCritRateBonus = 0.0;
        double weaponCritDamageBonus = 0.0;
        
        if (player instanceof ServerPlayerEntity serverPlayer && weaponDefinition != null) {
            weaponCritRateBonus = WeaponCombatSupport.INSTANCE.totalCritRateBonusPercent(serverPlayer, weaponDefinition);
            weaponCritDamageBonus = WeaponCombatSupport.INSTANCE.totalCritDamageBonusPercent(serverPlayer, weaponDefinition) / 100.0;
        } else if (weaponDefinition != null) {
            weaponCritRateBonus = WeaponCombatSupport.INSTANCE.critRateBonusPercent(weaponDefinition);
        }

        double weaponAllDamageBonus = weaponDefinition != null && weaponData != null
            ? WeaponCombatSupport.INSTANCE.allDamageBonusPercent(weaponDefinition, weaponData) / 100.0
            : 0.0;

        double allBonus = totals.getOrDefault(StatType.ALL_DMG_BONUS, 0.0) / 100.0 + weaponAllDamageBonus;
        double critRate = Math.min(1.0, CombatStatSupport.effectiveCritRateRatio(totals) + weaponCritRateBonus / 100.0);
        double critDamage = CombatStatSupport.effectiveCritDamageRatio(totals) + weaponCritDamageBonus;
        double damageMultiplier = 1.0 + Math.max(0.0, allBonus);
        double bloodMoonMultiplier = BloodMoonService.INSTANCE.playerDamageMultiplier(player);

        double result = cir.getReturnValueF() * damageMultiplier * debuffMultiplier * bloodMoonMultiplier;
        if (critRate > 0.0) {
            if (player.getRandom().nextDouble() < critRate) {
                double critMultiplier = 1.0 + Math.max(0.0, critDamage);
                result *= critMultiplier;
                if (player instanceof ServerPlayerEntity) {
                    ServerPlayerEntity serverPlayer = (ServerPlayerEntity) player;
                    CombatFeedbackService.INSTANCE.recordCrit(serverPlayer, critMultiplier);
                }
                player.playSound(SoundEvents.ENTITY_PLAYER_ATTACK_CRIT, 0.8F, 1.0F);
            }
        }
        cir.setReturnValue((float) result);
    }
}
