package hifumi.cresora.mixin;

import hifumi.cresora.AdventureRankService;
import hifumi.cresora.EquipmentEffectHookService;
import hifumi.cresora.EquipmentPlayerSupport;
import hifumi.cresora.StatType;
import hifumi.cresora.WeaponSkillService;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;
import java.util.Locale;

@Mixin(LivingEntity.class)
public class LivingEntityMixin {
    @Unique
    private float cresora$preDamageHealth;

    @ModifyVariable(method = "damage", at = @At("HEAD"), argsOnly = true)
    private float cresora$applyCombatScaling(float amount, net.minecraft.server.world.ServerWorld world, DamageSource source) {
        double enemyMultiplier = AdventureRankService.INSTANCE.damageMultiplier(source);
        if (enemyMultiplier > 1.0D) {
            amount = (float) (amount * enemyMultiplier);
        }
        if (!((Object) this instanceof PlayerEntity player)) {
            return amount;
        }
        Map<StatType, Double> totals = EquipmentPlayerSupport.getAggregatedStats(player);
        double reduction = totals.getOrDefault(StatType.DAMAGE_REDUCTION, 0.0) / 100.0;
        if (reduction > 0.0) {
            reduction = Math.min(0.95, Math.max(0.0, reduction));
            double finalMultiplier = 1.0 - reduction;
            player.sendMessage(
                Text.translatable("combat.cresora.damage_reduced", String.format(Locale.ROOT, "%.2f", finalMultiplier)),
                true
            );
            amount = (float) (amount * finalMultiplier);
        }
        if (player instanceof ServerPlayerEntity serverPlayer) {
            amount = WeaponSkillService.INSTANCE.absorbDamage(serverPlayer, amount);
        }
        return amount;
    }

    @Inject(method = "damage", at = @At("HEAD"))
    private void cresora$captureIncomingDamage(net.minecraft.server.world.ServerWorld world, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        this.cresora$preDamageHealth = ((LivingEntity)(Object)this).getHealth();
    }

    @Inject(method = "damage", at = @At("RETURN"))
    private void cresora$showMobDamage(net.minecraft.server.world.ServerWorld world, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ()) {
            return;
        }
        if (!((Object)this instanceof HostileEntity hostile)) {
            return;
        }
        float damageDone = Math.max(0.0F, this.cresora$preDamageHealth - hostile.getHealth());
        if (damageDone <= 0.0F) {
            return;
        }
        if (source.getAttacker() instanceof PlayerEntity player) {
            EquipmentEffectHookService.INSTANCE.onAttackDealt(player, hostile, damageDone);
        }
        if ((Object) this instanceof PlayerEntity player) {
            EquipmentEffectHookService.INSTANCE.onDamageTaken(player, source, damageDone);
        }
        AdventureRankService.INSTANCE.showMobDamage(hostile, source, damageDone);
        AdventureRankService.INSTANCE.refreshMobDisplay(hostile);
    }
}
