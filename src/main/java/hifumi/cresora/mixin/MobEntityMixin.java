package hifumi.cresora.mixin;

import hifumi.cresora.AdventureRankMobAccess;
import hifumi.cresora.AdventureRankService;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MobEntity.class)
public class MobEntityMixin implements AdventureRankMobAccess {
    @Unique
    private int cresora$mobAdventureRank = 0;

    @Inject(method = "writeCustomData", at = @At("TAIL"))
    private void cresora$writeMobAdventureRank(WriteView view, CallbackInfo ci) {
        if (this.cresora$mobAdventureRank > 0) {
            view.putInt(AdventureRankService.INSTANCE.mobRankKey(), this.cresora$mobAdventureRank);
        }
    }

    @Inject(method = "readCustomData", at = @At("TAIL"))
    private void cresora$readMobAdventureRank(ReadView view, CallbackInfo ci) {
        this.cresora$mobAdventureRank = view.getInt(AdventureRankService.INSTANCE.mobRankKey(), 0);
    }

    @Override
    public int cresoraGetMobAdventureRank() {
        return this.cresora$mobAdventureRank;
    }

    @Override
    public void cresoraSetMobAdventureRank(int rank) {
        this.cresora$mobAdventureRank = rank;
    }
}
