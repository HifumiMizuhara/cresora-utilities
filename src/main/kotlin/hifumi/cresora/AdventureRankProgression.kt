package hifumi.cresora

object AdventureRankProgression {
    const val MIN_RANK: Int = 1
    const val MAX_RANK: Int = 10

    private val REQUIRED_XP = intArrayOf(100, 180, 280, 400, 550, 730, 940, 1180, 1450)

    data class Progress(
        val rank: Int,
        val currentXp: Int,
        val requiredXp: Int?
    ) {
        fun isMaxRank(): Boolean = requiredXp == null
    }

    fun sanitizeRank(rank: Int): Int {
        return rank.coerceIn(MIN_RANK, MAX_RANK)
    }

    fun requiredXpForRank(rank: Int): Int? {
        val normalized = sanitizeRank(rank)
        if (normalized >= MAX_RANK) {
            return null
        }
        return REQUIRED_XP[normalized - MIN_RANK]
    }

    fun normalize(rank: Int, xp: Int): Progress {
        var normalizedRank = sanitizeRank(rank)
        var normalizedXp = xp.coerceAtLeast(0)

        while (normalizedRank < MAX_RANK) {
            val required = requiredXpForRank(normalizedRank) ?: break
            if (normalizedXp < required) {
                return Progress(normalizedRank, normalizedXp, required)
            }
            normalizedXp -= required
            normalizedRank++
        }

        return Progress(MAX_RANK, 0, null)
    }
}
