package hifumi.cresora

interface AdventureRankAccess {
    fun cresoraGetAdventureRank(): Int
    fun cresoraSetAdventureRank(rank: Int)
    fun cresoraGetAdventureRankXp(): Int
    fun cresoraSetAdventureRankXp(xp: Int)
}
