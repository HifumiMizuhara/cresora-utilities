package hifumi.cresora.mixin;

import hifumi.cresora.AdventureRankAccess;
import hifumi.cresora.AdventureRankProgression;
import hifumi.cresora.AdventureRankService;
import hifumi.cresora.CreditsAccess;
import hifumi.cresora.CreditsService;
import hifumi.cresora.WeaponSkillAccess;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayerEntity.class)
public class ServerPlayerEntityMixin implements AdventureRankAccess, CreditsAccess, WeaponSkillAccess {
    @Unique
    private int cresora$adventureRank = AdventureRankProgression.MIN_RANK;

    @Unique
    private int cresora$adventureRankXp = 0;

    @Unique
    private int cresora$credits = 0;

    @Unique
    private float cresora$shieldHp = 0.0F;

    @Unique
    private long cresora$shieldExpireTick = 0L;

    @Unique
    private String cresora$shieldWeaponId = null;

    @Unique
    private long cresora$skillCooldownExpireTick = 0L;

    @Unique
    private String cresora$skillCooldownWeaponId = null;

    @Inject(method = "writeCustomData", at = @At("TAIL"))
    private void cresora$writeAdventureRank(WriteView view, CallbackInfo ci) {
        view.putInt(AdventureRankService.INSTANCE.playerRankKey(), this.cresora$adventureRank);
        view.putInt(AdventureRankService.INSTANCE.playerRankXpKey(), this.cresora$adventureRankXp);
        view.putInt(CreditsService.INSTANCE.playerCreditsKey(), this.cresora$credits);
    }

    @Inject(method = "readCustomData", at = @At("TAIL"))
    private void cresora$readAdventureRank(ReadView view, CallbackInfo ci) {
        this.cresora$adventureRank = view.getInt(AdventureRankService.INSTANCE.playerRankKey(), AdventureRankProgression.MIN_RANK);
        this.cresora$adventureRankXp = view.getInt(AdventureRankService.INSTANCE.playerRankXpKey(), 0);
        this.cresora$credits = view.getInt(CreditsService.INSTANCE.playerCreditsKey(), 0);
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

    @Override
    public int cresoraGetCredits() {
        return this.cresora$credits;
    }

    @Override
    public void cresoraSetCredits(int value) {
        this.cresora$credits = value;
    }

    @Override
    public float cresoraGetShieldHp() {
        return this.cresora$shieldHp;
    }

    @Override
    public void cresoraSetShieldHp(float value) {
        this.cresora$shieldHp = value;
    }

    @Override
    public long cresoraGetShieldExpireTick() {
        return this.cresora$shieldExpireTick;
    }

    @Override
    public void cresoraSetShieldExpireTick(long value) {
        this.cresora$shieldExpireTick = value;
    }

    @Override
    public String cresoraGetShieldWeaponId() {
        return this.cresora$shieldWeaponId;
    }

    @Override
    public void cresoraSetShieldWeaponId(String value) {
        this.cresora$shieldWeaponId = value;
    }

    @Override
    public long cresoraGetSkillCooldownExpireTick() {
        return this.cresora$skillCooldownExpireTick;
    }

    @Override
    public void cresoraSetSkillCooldownExpireTick(long value) {
        this.cresora$skillCooldownExpireTick = value;
    }

    @Override
    public String cresoraGetSkillCooldownWeaponId() {
        return this.cresora$skillCooldownWeaponId;
    }

    @Override
    public void cresoraSetSkillCooldownWeaponId(String value) {
        this.cresora$skillCooldownWeaponId = value;
    }
}
