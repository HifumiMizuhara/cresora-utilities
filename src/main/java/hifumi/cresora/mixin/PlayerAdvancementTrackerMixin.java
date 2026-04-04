package hifumi.cresora.mixin;

import hifumi.cresora.CreditsService;
import net.minecraft.advancement.AdvancementEntry;
import net.minecraft.advancement.AdvancementProgress;
import net.minecraft.advancement.PlayerAdvancementTracker;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerAdvancementTracker.class)
public abstract class PlayerAdvancementTrackerMixin {
    @Shadow
    public abstract AdvancementProgress getProgress(AdvancementEntry advancement);

    @Shadow
    @Final
    private ServerPlayerEntity owner;

    @Inject(method = "grantCriterion", at = @At("HEAD"))
    private void cresora$captureAdvancementState(AdvancementEntry advancement, String criterionName, CallbackInfoReturnable<Boolean> cir) {
        // no-op anchor to keep local variable table stable across mappings
    }

    @Inject(method = "grantCriterion", at = @At("RETURN"))
    private void cresora$awardAdvancementCredits(AdvancementEntry advancement, String criterionName, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ()) {
            return;
        }
        AdvancementProgress progress = this.getProgress(advancement);
        if (progress == null || !progress.isDone()) {
            return;
        }
        CreditsService.INSTANCE.addAdvancementReward(this.owner, advancement);
    }
}
