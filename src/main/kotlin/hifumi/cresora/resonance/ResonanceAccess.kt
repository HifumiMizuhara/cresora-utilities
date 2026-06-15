package hifumi.cresora.resonance
interface ResonanceAccess {
    fun cresoraGetChordProgression(): Int

    fun cresoraSetChordProgression(value: Int)

    fun cresoraGetSubstituteChord(): Int

    fun cresoraSetSubstituteChord(value: Int)

    fun cresoraGetLimitedPityPulls(): Int

    fun cresoraSetLimitedPityPulls(value: Int)

    fun cresoraGetStandardPulls(): Int

    fun cresoraSetStandardPulls(value: Int)

    fun cresoraGetLimitedFourStarPulls(): Int

    fun cresoraSetLimitedFourStarPulls(value: Int)

    fun cresoraGetStandardFourStarPulls(): Int

    fun cresoraSetStandardFourStarPulls(value: Int)

    fun cresoraGetDeepPityStreak(): Int

    fun cresoraSetDeepPityStreak(value: Int)

    fun cresoraIsLimitedFiveStarGuaranteed(): Boolean

    fun cresoraSetLimitedFiveStarGuaranteed(value: Boolean)

    fun cresoraGetArpeggioReady(): Boolean

    fun cresoraSetArpeggioReady(value: Boolean)

    fun cresoraGetLastDailyLoginEpochDay(): Long

    fun cresoraSetLastDailyLoginEpochDay(value: Long)

    fun cresoraGetSelectedFeaturedWeaponId(): String?

    fun cresoraSetSelectedFeaturedWeaponId(value: String?)
}
