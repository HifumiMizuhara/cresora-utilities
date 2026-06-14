package hifumi.cresora.adventurerank
interface AdventureRankMobAccess {
    fun cresoraGetMobAdventureRank(): Int
    fun cresoraSetMobAdventureRank(rank: Int)
    fun cresoraIsEliteMob(): Boolean
    fun cresoraSetEliteMob(elite: Boolean)
    fun cresoraIsBossMob(): Boolean
    fun cresoraSetBossMob(boss: Boolean)
    fun cresoraGetMobPackId(): String
    fun cresoraSetMobPackId(packId: String)
}
