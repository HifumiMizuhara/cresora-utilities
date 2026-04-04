package hifumi.cresora

interface MasqueradeProgressAccess {
    fun cresoraGetMasqueradeCurrentSeasonId(): String
    fun cresoraSetMasqueradeCurrentSeasonId(value: String)
    fun cresoraGetMasqueradeBestWave(): Int
    fun cresoraSetMasqueradeBestWave(value: Int)
    fun cresoraGetMasqueradeAttemptCount(): Int
    fun cresoraSetMasqueradeAttemptCount(value: Int)
    fun cresoraGetMasqueradeTotalClearedWaves(): Int
    fun cresoraSetMasqueradeTotalClearedWaves(value: Int)
    fun cresoraGetMasqueradeArchiveRaw(): String
    fun cresoraSetMasqueradeArchiveRaw(value: String)
}
