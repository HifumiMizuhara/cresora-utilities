package hifumi.cresora.guide

interface GuideProgressAccess {
    fun cresoraGetGuideCurrentChapter(): Int
    fun cresoraSetGuideCurrentChapter(value: Int)

    fun cresoraGetGuideClaimedTasks(): String
    fun cresoraSetGuideClaimedTasks(value: String)

    fun cresoraGetGuideClaimedChapters(): String
    fun cresoraSetGuideClaimedChapters(value: String)

    fun cresoraGetGuideProgressMap(): String
    fun cresoraSetGuideProgressMap(value: String)
}
