package hifumi.cresora.mixin;

import hifumi.cresora.adventurerank.AdventureRankAccess;
import hifumi.cresora.adventurerank.AdventureRankProgression;
import hifumi.cresora.adventurerank.AdventureRankService;
import hifumi.cresora.credits.CreditsAccess;
import hifumi.cresora.credits.CreditsService;
import hifumi.cresora.masquerade.MasqueradeProgressAccess;
import hifumi.cresora.masquerade.MasqueradeProgressService;
import hifumi.cresora.resonance.ResonanceAccess;
import hifumi.cresora.resonance.ResonanceService;
import hifumi.cresora.story.StoryProgressAccess;
import hifumi.cresora.story.StoryProgressService;
import hifumi.cresora.weapon.WeaponSkillAccess;
import java.util.Map;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayerEntity.class)
public class ServerPlayerEntityMixin implements AdventureRankAccess, CreditsAccess, WeaponSkillAccess, ResonanceAccess, StoryProgressAccess, MasqueradeProgressAccess {
    @Unique
    private int cresora$adventureRank = AdventureRankProgression.MIN_RANK;

    @Unique
    private int cresora$adventureRankXp = 0;

    @Unique
    private int cresora$credits = 0;

    @Unique
    private int cresora$chordProgression = 0;

    @Unique
    private int cresora$substituteChord = 0;

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

    @Unique
    private final Map<String, Double> cresora$cooldowns = new java.util.LinkedHashMap<>();

    @Unique
    private int cresora$limitedPityPulls = 0;

    @Unique
    private int cresora$standardPulls = 0;

    @Unique
    private int cresora$limitedFourStarPulls = 0;

    @Unique
    private int cresora$standardFourStarPulls = 0;

    @Unique
    private int cresora$deepPityStreak = 0;

    @Unique
    private boolean cresora$limitedFiveStarGuaranteed = false;

    @Unique
    private boolean cresora$arpeggioReady = false;

    @Unique
    private long cresora$lastDailyLoginEpochDay = -1L;

    @Unique
    private String cresora$storyClearsRaw = "";

    @Unique
    private String cresora$masqueradeCurrentSeasonId = "";

    @Unique
    private int cresora$masqueradeBestWave = 0;

    @Unique
    private int cresora$masqueradeAttemptCount = 0;

    @Unique
    private int cresora$masqueradeTotalClearedWaves = 0;

    @Unique
    private String cresora$masqueradeArchiveRaw = "";

    @Inject(method = "writeCustomData", at = @At("TAIL"))
    private void cresora$writeAdventureRank(WriteView view, CallbackInfo ci) {
        view.putInt(AdventureRankService.INSTANCE.playerRankKey(), this.cresora$adventureRank);
        view.putInt(AdventureRankService.INSTANCE.playerRankXpKey(), this.cresora$adventureRankXp);
        view.putInt(CreditsService.INSTANCE.playerCreditsKey(), this.cresora$credits);
        view.putInt(ResonanceService.INSTANCE.chordProgressionKey(), this.cresora$chordProgression);
        view.putInt(ResonanceService.INSTANCE.substituteChordKey(), this.cresora$substituteChord);
        view.putInt(ResonanceService.INSTANCE.limitedPityKey(), this.cresora$limitedPityPulls);
        view.putInt(ResonanceService.INSTANCE.standardPullsKey(), this.cresora$standardPulls);
        view.putInt(ResonanceService.INSTANCE.limitedFourStarPullsKey(), this.cresora$limitedFourStarPulls);
        view.putInt(ResonanceService.INSTANCE.standardFourStarPullsKey(), this.cresora$standardFourStarPulls);
        view.putInt(ResonanceService.INSTANCE.deepPityStreakKey(), this.cresora$deepPityStreak);
        view.putInt(ResonanceService.INSTANCE.limitedFiveStarGuaranteedKey(), this.cresora$limitedFiveStarGuaranteed ? 1 : 0);
        view.putInt(ResonanceService.INSTANCE.arpeggioReadyKey(), this.cresora$arpeggioReady ? 1 : 0);
        view.putLong(ResonanceService.INSTANCE.dailyLoginEpochDayKey(), this.cresora$lastDailyLoginEpochDay);
        view.putString(StoryProgressService.INSTANCE.playerStoryClearsKey(), this.cresora$storyClearsRaw);
        view.putString(MasqueradeProgressService.INSTANCE.playerSeasonIdKey(), this.cresora$masqueradeCurrentSeasonId);
        view.putInt(MasqueradeProgressService.INSTANCE.playerBestWaveKey(), this.cresora$masqueradeBestWave);
        view.putInt(MasqueradeProgressService.INSTANCE.playerAttemptCountKey(), this.cresora$masqueradeAttemptCount);
        view.putInt(MasqueradeProgressService.INSTANCE.playerTotalClearedWavesKey(), this.cresora$masqueradeTotalClearedWaves);
        view.putString(MasqueradeProgressService.INSTANCE.playerArchiveKey(), this.cresora$masqueradeArchiveRaw);
        view.putString("cresora_weapon_cooldowns", hifumi.cresora.weapon.WeaponSkillService.INSTANCE.serializeCooldowns(this.cresora$cooldowns));
    }

    @Inject(method = "readCustomData", at = @At("TAIL"))
    private void cresora$readAdventureRank(ReadView view, CallbackInfo ci) {
        this.cresora$adventureRank = view.getInt(AdventureRankService.INSTANCE.playerRankKey(), AdventureRankProgression.MIN_RANK);
        this.cresora$adventureRankXp = view.getInt(AdventureRankService.INSTANCE.playerRankXpKey(), 0);
        this.cresora$credits = view.getInt(CreditsService.INSTANCE.playerCreditsKey(), 0);
        this.cresora$chordProgression = view.getInt(ResonanceService.INSTANCE.chordProgressionKey(), 0);
        this.cresora$substituteChord = view.getInt(ResonanceService.INSTANCE.substituteChordKey(), 0);
        this.cresora$limitedPityPulls = view.getInt(ResonanceService.INSTANCE.limitedPityKey(), 0);
        this.cresora$standardPulls = view.getInt(ResonanceService.INSTANCE.standardPullsKey(), 0);
        this.cresora$limitedFourStarPulls = view.getInt(ResonanceService.INSTANCE.limitedFourStarPullsKey(), 0);
        this.cresora$standardFourStarPulls = view.getInt(ResonanceService.INSTANCE.standardFourStarPullsKey(), 0);
        this.cresora$deepPityStreak = view.getInt(ResonanceService.INSTANCE.deepPityStreakKey(), 0);
        this.cresora$limitedFiveStarGuaranteed = view.getInt(ResonanceService.INSTANCE.limitedFiveStarGuaranteedKey(), 0) != 0;
        this.cresora$arpeggioReady = view.getInt(ResonanceService.INSTANCE.arpeggioReadyKey(), 0) != 0;
        this.cresora$lastDailyLoginEpochDay = view.getLong(ResonanceService.INSTANCE.dailyLoginEpochDayKey(), -1L);
        this.cresora$storyClearsRaw = view.getString(StoryProgressService.INSTANCE.playerStoryClearsKey(), "");
        this.cresora$masqueradeCurrentSeasonId = view.getString(MasqueradeProgressService.INSTANCE.playerSeasonIdKey(), "");
        this.cresora$masqueradeBestWave = view.getInt(MasqueradeProgressService.INSTANCE.playerBestWaveKey(), 0);
        this.cresora$masqueradeAttemptCount = view.getInt(MasqueradeProgressService.INSTANCE.playerAttemptCountKey(), 0);
        this.cresora$masqueradeTotalClearedWaves = view.getInt(MasqueradeProgressService.INSTANCE.playerTotalClearedWavesKey(), 0);
        this.cresora$masqueradeArchiveRaw = view.getString(MasqueradeProgressService.INSTANCE.playerArchiveKey(), "");
        String serializedCooldowns = view.getString("cresora_weapon_cooldowns", "");
        this.cresora$cooldowns.clear();
        this.cresora$cooldowns.putAll(hifumi.cresora.weapon.WeaponSkillService.INSTANCE.deserializeCooldowns(serializedCooldowns));
    }

    @Inject(method = "onDeath", at = @At("HEAD"))
    private void cresora$restoreHotbarImmediatelyOnDeath(net.minecraft.entity.damage.DamageSource source, CallbackInfo ci) {
        ServerPlayerEntity player = (ServerPlayerEntity) (Object) this;
        if (hifumi.cresora.weapon.HotbarOverrideService.INSTANCE.isOverridden(player)) {
            hifumi.cresora.weapon.HotbarOverrideService.INSTANCE.restoreHotbar(player);
        }
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

    @Override
    public Map<String, Double> cresoraGetCooldowns() {
        return this.cresora$cooldowns;
    }

    @Override
    public int cresoraGetLimitedPityPulls() {
        return this.cresora$limitedPityPulls;
    }

    @Override
    public int cresoraGetChordProgression() {
        return this.cresora$chordProgression;
    }

    @Override
    public void cresoraSetChordProgression(int value) {
        this.cresora$chordProgression = value;
    }

    @Override
    public int cresoraGetSubstituteChord() {
        return this.cresora$substituteChord;
    }

    @Override
    public void cresoraSetSubstituteChord(int value) {
        this.cresora$substituteChord = value;
    }

    @Override
    public void cresoraSetLimitedPityPulls(int value) {
        this.cresora$limitedPityPulls = value;
    }

    @Override
    public int cresoraGetStandardPulls() {
        return this.cresora$standardPulls;
    }

    @Override
    public void cresoraSetStandardPulls(int value) {
        this.cresora$standardPulls = value;
    }

    @Override
    public int cresoraGetLimitedFourStarPulls() {
        return this.cresora$limitedFourStarPulls;
    }

    @Override
    public void cresoraSetLimitedFourStarPulls(int value) {
        this.cresora$limitedFourStarPulls = value;
    }

    @Override
    public int cresoraGetStandardFourStarPulls() {
        return this.cresora$standardFourStarPulls;
    }

    @Override
    public void cresoraSetStandardFourStarPulls(int value) {
        this.cresora$standardFourStarPulls = value;
    }

    @Override
    public int cresoraGetDeepPityStreak() {
        return this.cresora$deepPityStreak;
    }

    @Override
    public void cresoraSetDeepPityStreak(int value) {
        this.cresora$deepPityStreak = value;
    }

    @Override
    public boolean cresoraIsLimitedFiveStarGuaranteed() {
        return this.cresora$limitedFiveStarGuaranteed;
    }

    @Override
    public void cresoraSetLimitedFiveStarGuaranteed(boolean value) {
        this.cresora$limitedFiveStarGuaranteed = value;
    }

    @Override
    public boolean cresoraGetArpeggioReady() {
        return this.cresora$arpeggioReady;
    }

    @Override
    public void cresoraSetArpeggioReady(boolean value) {
        this.cresora$arpeggioReady = value;
    }

    @Override
    public long cresoraGetLastDailyLoginEpochDay() {
        return this.cresora$lastDailyLoginEpochDay;
    }

    @Override
    public void cresoraSetLastDailyLoginEpochDay(long value) {
        this.cresora$lastDailyLoginEpochDay = value;
    }

    @Override
    public String cresoraGetStoryClearsRaw() {
        return this.cresora$storyClearsRaw;
    }

    @Override
    public void cresoraSetStoryClearsRaw(String value) {
        this.cresora$storyClearsRaw = value == null ? "" : value;
    }

    @Override
    public String cresoraGetMasqueradeCurrentSeasonId() {
        return this.cresora$masqueradeCurrentSeasonId;
    }

    @Override
    public void cresoraSetMasqueradeCurrentSeasonId(String value) {
        this.cresora$masqueradeCurrentSeasonId = value == null ? "" : value;
    }

    @Override
    public int cresoraGetMasqueradeBestWave() {
        return this.cresora$masqueradeBestWave;
    }

    @Override
    public void cresoraSetMasqueradeBestWave(int value) {
        this.cresora$masqueradeBestWave = value;
    }

    @Override
    public int cresoraGetMasqueradeAttemptCount() {
        return this.cresora$masqueradeAttemptCount;
    }

    @Override
    public void cresoraSetMasqueradeAttemptCount(int value) {
        this.cresora$masqueradeAttemptCount = value;
    }

    @Override
    public int cresoraGetMasqueradeTotalClearedWaves() {
        return this.cresora$masqueradeTotalClearedWaves;
    }

    @Override
    public void cresoraSetMasqueradeTotalClearedWaves(int value) {
        this.cresora$masqueradeTotalClearedWaves = value;
    }

    @Override
    public String cresoraGetMasqueradeArchiveRaw() {
        return this.cresora$masqueradeArchiveRaw;
    }

    @Override
    public void cresoraSetMasqueradeArchiveRaw(String value) {
        this.cresora$masqueradeArchiveRaw = value == null ? "" : value;
    }
}
