package hifumi.cresora.story

interface StoryFlagAccess {
    fun cresoraGetStoryFlagsRaw(): String
    fun cresoraSetStoryFlagsRaw(value: String)
}
