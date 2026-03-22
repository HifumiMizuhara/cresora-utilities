package hifumi.cresora.mixin;

import hifumi.cresora.EquipmentPlayerSupport;
import hifumi.cresora.StatType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.Map;

@Mixin(LivingEntity.class)
public class LivingEntityMixin {
    @ModifyVariable(method = "damage", at = @At("HEAD"), argsOnly = true)
    private float cresora$applyDamageReduction(float amount) {
        if (!((Object) this instanceof PlayerEntity player)) {
            return amount;
        }
        Map<StatType, Double> totals = EquipmentPlayerSupport.getAggregatedStats(player);
        double reduction = totals.getOrDefault(StatType.DAMAGE_REDUCTION, 0.0) / 100.0;
        if (reduction <= 0.0) {
            return amount;
        }
        reduction = Math.min(0.95, Math.max(0.0, reduction));
        return (float) (amount * (1.0 - reduction));
    }
}
