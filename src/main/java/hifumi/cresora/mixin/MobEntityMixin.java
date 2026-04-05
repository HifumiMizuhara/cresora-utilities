package hifumi.cresora.mixin;

import hifumi.cresora.AdventureRankMobAccess;
import hifumi.cresora.AdventureRankService;
import hifumi.cresora.FieldMobPackService;
import net.minecraft.entity.mob.HostileEntity;
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
    @Unique
    private boolean cresora$eliteMob = false;
    @Unique
    private String cresora$mobPackId = "";

    @Inject(method = "writeCustomData", at = @At("TAIL"))
    private void cresora$writeMobAdventureRank(WriteView view, CallbackInfo ci) {
        if (this.cresora$mobAdventureRank > 0) {
            view.putInt(AdventureRankService.INSTANCE.mobRankKey(), this.cresora$mobAdventureRank);
        }
        if (this.cresora$eliteMob) {
            view.putBoolean(FieldMobPackService.INSTANCE.eliteKey(), true);
        }
        if (!this.cresora$mobPackId.isBlank()) {
            view.putString(FieldMobPackService.INSTANCE.packIdKey(), this.cresora$mobPackId);
        }
    }

    @Inject(method = "readCustomData", at = @At("TAIL"))
    private void cresora$readMobAdventureRank(ReadView view, CallbackInfo ci) {
        this.cresora$mobAdventureRank = view.getInt(AdventureRankService.INSTANCE.mobRankKey(), 0);
        this.cresora$eliteMob = view.getBoolean(FieldMobPackService.INSTANCE.eliteKey(), false);
        this.cresora$mobPackId = view.getString(FieldMobPackService.INSTANCE.packIdKey(), "");
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void cresora$refreshMobDisplay(CallbackInfo ci) {
        if (!((Object)this instanceof HostileEntity hostile)) {
            return;
        }
        if (hostile.getWorld().isClient()) {
            return;
        }
        if (hostile.age % 10 == 0) {
            AdventureRankService.INSTANCE.refreshMobDisplay(hostile);
        }
    }

    @Override
    public int cresoraGetMobAdventureRank() {
        return this.cresora$mobAdventureRank;
    }

    @Override
    public void cresoraSetMobAdventureRank(int rank) {
        this.cresora$mobAdventureRank = rank;
    }

    @Override
    public boolean cresoraIsEliteMob() {
        return this.cresora$eliteMob;
    }

    @Override
    public void cresoraSetEliteMob(boolean elite) {
        this.cresora$eliteMob = elite;
    }

    @Override
    public String cresoraGetMobPackId() {
        return this.cresora$mobPackId;
    }

    @Override
    public void cresoraSetMobPackId(String packId) {
        this.cresora$mobPackId = packId == null ? "" : packId;
    }
}
