package hifumi.cresora.adventurerank
interface AdventureRankAccess {
    fun cresoraGetAdventureRank(): Int
    fun cresoraSetAdventureRank(rank: Int)
    fun cresoraGetAdventureRankXp(): Int
    fun cresoraSetAdventureRankXp(xp: Int)
}
