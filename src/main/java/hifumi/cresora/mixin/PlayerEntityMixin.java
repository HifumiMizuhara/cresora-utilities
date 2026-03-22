package hifumi.cresora.mixin;

import hifumi.cresora.CombatStatSupport;
import hifumi.cresora.EquipmentPlayerSupport;
import hifumi.cresora.StatType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;
import java.util.Locale;

@Mixin(PlayerEntity.class)
public class PlayerEntityMixin {
    @Inject(method = "getDamageAgainst", at = @At("RETURN"), cancellable = true)
    private void cresora$applyOffenseStats(CallbackInfoReturnable<Float> cir) {
        PlayerEntity player = (PlayerEntity) (Object) this;
        Map<StatType, Double> totals = EquipmentPlayerSupport.getAggregatedStats(player);

        double allBonus = totals.getOrDefault(StatType.ALL_DMG_BONUS, 0.0) / 100.0;
        double critRate = CombatStatSupport.effectiveCritRateRatio(totals);
        double critDamage = CombatStatSupport.effectiveCritDamageRatio(totals);
        double damageMultiplier = 1.0 + Math.max(0.0, allBonus);

        double result = cir.getReturnValueF() * damageMultiplier;
        boolean critTriggered = false;
        if (critRate > 0.0) {
            if (player.getRandom().nextDouble() < critRate) {
                double critMultiplier = 1.0 + Math.max(0.0, critDamage);
                result *= critMultiplier;
                critTriggered = true;
                if (allBonus > 0.0) {
                    player.sendMessage(
                        Text.translatable(
                            "combat.cresora.damage_combo",
                            String.format(Locale.ROOT, "%.2f", damageMultiplier),
                            String.format(Locale.ROOT, "%.2f", critMultiplier)
                        ),
                        true
                    );
                } else {
                    player.sendMessage(
                        Text.translatable("combat.cresora.crit_triggered", String.format(Locale.ROOT, "%.2f", critMultiplier)),
                        true
                    );
                }
                player.playSound(SoundEvents.ENTITY_PLAYER_ATTACK_CRIT, 0.8F, 1.0F);
            }
        }
        if (!critTriggered && allBonus > 0.0) {
            player.sendMessage(
                Text.translatable("combat.cresora.damage_bonus", String.format(Locale.ROOT, "%.2f", damageMultiplier)),
                true
            );
        }
        cir.setReturnValue((float) result);
    }
}
