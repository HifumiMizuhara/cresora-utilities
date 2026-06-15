package hifumi.cresora.guide

enum class GuideTaskType(val id: String) {
    KILL_HOSTILE("kill_hostile"),
    OBTAIN_CSC("obtain_csc"),
    RESONANCE_PULL("resonance_pull"),
    UPGRADE_WEAPON("upgrade_weapon"),
    REACH_RANK("reach_rank"),
    CLEAR_STORY_STAGE("clear_story_stage")
}

data class GuideTask(
    val id: String,
    val translationKey: String,
    val type: GuideTaskType,
    val targetValue: Int,
    val cscReward: Int,
    val chordReward: Int,
    val detailParam: String = ""
)

data class GuideChapter(
    val index: Int,
    val translationKey: String,
    val tasks: List<GuideTask>,
    val cscReward: Int,
    val chordReward: Int
)

object GuideContentRegistry {
    val chapters = listOf(
        GuideChapter(
            index = 1,
            translationKey = "guide.cresora.chapter.1",
            tasks = listOf(
                GuideTask(
                    id = "ch1_kill",
                    translationKey = "guide.cresora.task.ch1_kill",
                    type = GuideTaskType.KILL_HOSTILE,
                    targetValue = 5,
                    cscReward = 500,
                    chordReward = 325
                ),
                GuideTask(
                    id = "ch1_obtain_csc",
                    translationKey = "guide.cresora.task.ch1_obtain_csc",
                    type = GuideTaskType.OBTAIN_CSC,
                    targetValue = 1000,
                    cscReward = 500,
                    chordReward = 325
                ),
                GuideTask(
                    id = "ch1_resonance_pull",
                    translationKey = "guide.cresora.task.ch1_resonance_pull",
                    type = GuideTaskType.RESONANCE_PULL,
                    targetValue = 1,
                    cscReward = 1000,
                    chordReward = 325
                )
            ),
            cscReward = 1500,
            chordReward = 650
        ),
        GuideChapter(
            index = 2,
            translationKey = "guide.cresora.chapter.2",
            tasks = listOf(
                GuideTask(
                    id = "ch2_upgrade_weapon",
                    translationKey = "guide.cresora.task.ch2_upgrade_weapon",
                    type = GuideTaskType.UPGRADE_WEAPON,
                    targetValue = 1,
                    cscReward = 1000,
                    chordReward = 325
                ),
                GuideTask(
                    id = "ch2_kill",
                    translationKey = "guide.cresora.task.ch2_kill",
                    type = GuideTaskType.KILL_HOSTILE,
                    targetValue = 15,
                    cscReward = 1000,
                    chordReward = 325
                ),
                GuideTask(
                    id = "ch2_reach_rank",
                    translationKey = "guide.cresora.task.ch2_reach_rank",
                    type = GuideTaskType.REACH_RANK,
                    targetValue = 2,
                    cscReward = 1000,
                    chordReward = 325
                )
            ),
            cscReward = 3000,
            chordReward = 975
        ),
        GuideChapter(
            index = 3,
            translationKey = "guide.cresora.chapter.3",
            tasks = listOf(
                GuideTask(
                    id = "ch3_clear_story",
                    translationKey = "guide.cresora.task.ch3_clear_story",
                    type = GuideTaskType.CLEAR_STORY_STAGE,
                    targetValue = 1,
                    cscReward = 2000,
                    chordReward = 650,
                    detailParam = "0-0"
                ),
                GuideTask(
                    id = "ch3_upgrade_weapon_10",
                    translationKey = "guide.cresora.task.ch3_upgrade_weapon_10",
                    type = GuideTaskType.UPGRADE_WEAPON,
                    targetValue = 10,
                    cscReward = 2000,
                    chordReward = 650
                ),
                GuideTask(
                    id = "ch3_pulls",
                    translationKey = "guide.cresora.task.ch3_pulls",
                    type = GuideTaskType.RESONANCE_PULL,
                    targetValue = 5,
                    cscReward = 2000,
                    chordReward = 650
                )
            ),
            cscReward = 5000,
            chordReward = 1625
        )
    )

    fun getChapter(index: Int): GuideChapter? {
        return chapters.firstOrNull { it.index == index }
    }

    fun getTask(id: String): GuideTask? {
        return chapters.flatMap { it.tasks }.firstOrNull { it.id == id }
    }
}
