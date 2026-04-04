package hifumi.cresora.mixin;

import hifumi.cresora.AdventureRankService;
import hifumi.cresora.CombatDamageType;
import hifumi.cresora.CombatDamageTypeSupport;
import hifumi.cresora.EquipmentEffectHookService;
import hifumi.cresora.MasqueradeService;
import hifumi.cresora.MobCombatProfileRegistry;
import hifumi.cresora.MusicEchoContentRegistry;
import hifumi.cresora.NaturalRegenService;
import hifumi.cresora.StoryService;
import hifumi.cresora.WeaponSkillService;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public class LivingEntityMixin {
    @Unique
    private float cresora$preDamageHealth;

    @ModifyVariable(method = "damage", at = @At("HEAD"), argsOnly = true)
    private float cresora$applyCombatScaling(float amount, net.minecraft.server.world.ServerWorld world, DamageSource source) {
        if ((Object) this instanceof MobEntity) {
            amount = (float) (amount * MusicEchoContentRegistry.INSTANCE.mobDamageTakenMultiplier());
        }
        if ((Object) this instanceof HostileEntity hostile) {
            amount = (float) (amount * MasqueradeService.INSTANCE.damageTakenMultiplier(hostile));
            amount = (float) (amount * StoryService.INSTANCE.damageTakenMultiplier(hostile));
            CombatDamageType damageType = CombatDamageTypeSupport.damageSourceType(source);
            double resistanceRatio = MobCombatProfileRegistry.INSTANCE.resistancePercent(hostile.getType(), damageType) / 100.0D;
            if (resistanceRatio > 0.0D) {
                amount = (float) (amount * (1.0D - resistanceRatio));
            }
        }
        double enemyMultiplier = AdventureRankService.INSTANCE.damageMultiplier(source);
        if (enemyMultiplier > 1.0D) {
            amount = (float) (amount * enemyMultiplier);
        }
        if (!((Object) this instanceof PlayerEntity)) {
            return amount;
        }
        PlayerEntity player = (PlayerEntity) (Object) this;
        if (player instanceof ServerPlayerEntity serverPlayer) {
            amount = WeaponSkillService.INSTANCE.absorbDamage(serverPlayer, amount);
        }
        return amount;
    }

    @Inject(method = "damage", at = @At("HEAD"))
    private void cresora$captureIncomingDamage(net.minecraft.server.world.ServerWorld world, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (source.getAttacker() instanceof ServerPlayerEntity serverPlayer) {
            NaturalRegenService.INSTANCE.markCombat(serverPlayer);
        }
        if ((Object) this instanceof ServerPlayerEntity serverPlayer && source.getAttacker() instanceof LivingEntity) {
            NaturalRegenService.INSTANCE.markCombat(serverPlayer);
        }
        this.cresora$preDamageHealth = ((LivingEntity)(Object)this).getHealth();
    }

    @Inject(method = "damage", at = @At("RETURN"))
    private void cresora$showMobDamage(net.minecraft.server.world.ServerWorld world, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ()) {
            return;
        }
        LivingEntity entity = (LivingEntity) (Object) this;
        float damageDone = Math.max(0.0F, this.cresora$preDamageHealth - entity.getHealth());
        if (damageDone <= 0.0F) {
            return;
        }
        if (entity instanceof PlayerEntity player) {
            EquipmentEffectHookService.INSTANCE.onDamageTaken(player, source, damageDone);
            if (player instanceof ServerPlayerEntity serverPlayer) {
                AdventureRankService.INSTANCE.showPlayerDamageFeedback(serverPlayer, source, damageDone);
            }
            return;
        }
        if (!(entity instanceof HostileEntity hostile)) {
            return;
        }
        if (source.getAttacker() instanceof PlayerEntity player) {
            EquipmentEffectHookService.INSTANCE.onAttackDealt(player, hostile, damageDone);
            if (player instanceof ServerPlayerEntity serverPlayer) {
                WeaponSkillService.INSTANCE.onAttackDealt(serverPlayer, hostile, damageDone);
            }
        }
        AdventureRankService.INSTANCE.showMobDamage(hostile, source, damageDone);
        AdventureRankService.INSTANCE.refreshMobDisplay(hostile);
    }
}
