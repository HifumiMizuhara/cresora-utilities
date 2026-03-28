package hifumi.cresora.mixin;

import hifumi.cresora.AdventureRankRewardClassifier;
import hifumi.cresora.AdventureRankRewardSource;
import hifumi.cresora.AdventureRankService;
import hifumi.cresora.CreditsRewardClassifier;
import hifumi.cresora.CreditsRewardSource;
import hifumi.cresora.CreditsService;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemEntity.class)
public abstract class ItemEntityMixin {
    @Unique
    private ItemStack cresora$pickupSnapshot = ItemStack.EMPTY;

    @Inject(method = "onPlayerCollision", at = @At("HEAD"))
    private void cresora$capturePickupSnapshot(PlayerEntity player, CallbackInfo ci) {
        ItemEntity self = (ItemEntity) (Object) this;
        this.cresora$pickupSnapshot = self.getStack().copy();
    }

    @Inject(method = "onPlayerCollision", at = @At("TAIL"))
    private void cresora$awardPickupRankXp(PlayerEntity player, CallbackInfo ci) {
        if (!(player instanceof ServerPlayerEntity serverPlayer) || this.cresora$pickupSnapshot.isEmpty()) {
            return;
        }

        ItemEntity self = (ItemEntity) (Object) this;
        int pickedCount = this.cresora$pickupSnapshot.getCount() - self.getStack().getCount();
        if (pickedCount <= 0) {
            this.cresora$pickupSnapshot = ItemStack.EMPTY;
            return;
        }

        AdventureRankRewardSource source = AdventureRankRewardClassifier.INSTANCE.classify(this.cresora$pickupSnapshot);
        if (source != null) {
            AdventureRankService.INSTANCE.addReward(serverPlayer, source, pickedCount);
        }

        CreditsRewardSource creditsSource = CreditsRewardClassifier.INSTANCE.classify(this.cresora$pickupSnapshot);
        if (creditsSource != null) {
            CreditsService.INSTANCE.addPickupReward(serverPlayer, creditsSource, pickedCount);
        }

        this.cresora$pickupSnapshot = ItemStack.EMPTY;
    }
}
