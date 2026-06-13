package hifumi.cresora.adventurerank
object AdventureRankProgression {
    const val MIN_RANK: Int = 1
    const val MAX_RANK: Int = 70

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
        val step = normalized - MIN_RANK
        return when {
            step < 10 -> 300 + step * 75
            step < 20 -> 1_050 + (step - 10) * 130
            step < 35 -> 2_350 + (step - 20) * 220
            step < 50 -> 5_650 + (step - 35) * 360
            else -> 11_050 + (step - 50) * 520
        }
    }

    fun normalizedProgress(rank: Int): Double {
        if (MAX_RANK <= MIN_RANK) {
            return 0.0
        }
        return (sanitizeRank(rank) - MIN_RANK).toDouble() / (MAX_RANK - MIN_RANK).toDouble()
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
