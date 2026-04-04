package hifumi.cresora

interface ResonanceAccess {
    fun cresoraGetChordProgression(): Int

    fun cresoraSetChordProgression(value: Int)

    fun cresoraGetSubstituteChord(): Int

    fun cresoraSetSubstituteChord(value: Int)

    fun cresoraGetLimitedPityPulls(): Int

    fun cresoraSetLimitedPityPulls(value: Int)

    fun cresoraGetStandardPulls(): Int

    fun cresoraSetStandardPulls(value: Int)

    fun cresoraGetDeepPityStreak(): Int

    fun cresoraSetDeepPityStreak(value: Int)

    fun cresoraGetArpeggioReady(): Boolean

    fun cresoraSetArpeggioReady(value: Boolean)
}
