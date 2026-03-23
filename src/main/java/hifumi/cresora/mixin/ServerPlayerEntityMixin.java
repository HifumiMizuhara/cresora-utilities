package hifumi.cresora.mixin;

import hifumi.cresora.AdventureRankAccess;
import hifumi.cresora.AdventureRankProgression;
import hifumi.cresora.AdventureRankService;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayerEntity.class)
public class ServerPlayerEntityMixin implements AdventureRankAccess {
    @Unique
    private int cresora$adventureRank = AdventureRankProgression.MIN_RANK;

    @Unique
    private int cresora$adventureRankXp = 0;

    @Inject(method = "writeCustomData", at = @At("TAIL"))
    private void cresora$writeAdventureRank(WriteView view, CallbackInfo ci) {
        view.putInt(AdventureRankService.INSTANCE.playerRankKey(), this.cresora$adventureRank);
        view.putInt(AdventureRankService.INSTANCE.playerRankXpKey(), this.cresora$adventureRankXp);
    }

    @Inject(method = "readCustomData", at = @At("TAIL"))
    private void cresora$readAdventureRank(ReadView view, CallbackInfo ci) {
        this.cresora$adventureRank = view.getInt(AdventureRankService.INSTANCE.playerRankKey(), AdventureRankProgression.MIN_RANK);
        this.cresora$adventureRankXp = view.getInt(AdventureRankService.INSTANCE.playerRankXpKey(), 0);
    }

    @Override
    public int cresoraGetAdventureRank() {
        return this.cresora$adventureRank;
    }

    @Override
    public void cresoraSetAdventureRank(int rank) {
        this.cresora$adventureRank = rank;
    }

    @Override
    public int cresoraGetAdventureRankXp() {
        return this.cresora$adventureRankXp;
    }

    @Override
    public void cresoraSetAdventureRankXp(int xp) {
        this.cresora$adventureRankXp = xp;
    }
}
