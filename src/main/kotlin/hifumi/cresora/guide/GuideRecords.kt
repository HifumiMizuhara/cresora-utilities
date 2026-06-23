package hifumi.cresora.guide

/**
 * Read-only record views the [GuideService] hub exposes beyond the beginner task chapters.
 * Each record is computed on demand by aggregating already-persisted player data (story flags,
 * spirit bond points, story clears), so none of them introduce new persistence.
 */

/** A cresora-world region and whether the player has discovered (visited) it. */
data class GuideRegionRecord(
    val regionId: String,
    val nameKey: String,
    val descriptionKey: String,
    val unlockRank: Int,
    val discovered: Boolean
)

/** A spirit (weapon-bound companion) and the player's bond progression with it. */
data class GuideSpiritBondRecord(
    val weaponId: String,
    val nameKey: String,
    /** Whether the player has ever obtained this spirit (persisted) or accrued bond points. */
    val encountered: Boolean,
    /** Current bond stage, 1-indexed. Unmet spirits report the floor stage of 1. */
    val stage: Int,
    val maxStage: Int,
    val points: Int,
    /** Bond points required to reach the next stage, or `null` when already at [maxStage]. */
    val pointsForNextStage: Int?,
    /** Title/story localization keys for the current stage, if the spirit defines that stage. */
    val currentStageTitleKey: String?,
    val currentStageStoryKey: String?
) {
    val atMaxStage: Boolean get() = stage >= maxStage
}

/** Aggregate completion snapshot across guide chapters, story, discovery, and spirit bonds. */
data class GuideAchievementSummary(
    val currentChapter: Int,
    val guideChaptersCompleted: Int,
    val guideTasksClaimed: Int,
    val storyStagesCleared: Int,
    val regionsDiscovered: Int,
    val regionsTotal: Int,
    val spiritsEncountered: Int,
    val spiritsTotal: Int,
    val spiritsMaxBonded: Int
)
