package hifumi.cresora.guide

import net.minecraft.component.DataComponentTypes
import net.minecraft.component.type.LoreComponent
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.text.Text

object GuideDisplayStackFactory {
    const val CLAIMABLE_STATUS_KEY = "guide.cresora.status.claimable"

    fun fillerDisplay(): ItemStack {
        return ItemStack.EMPTY
    }

    fun backDisplay(): ItemStack {
        return ItemStack(Items.ARROW).apply {
            set(DataComponentTypes.CUSTOM_NAME, Text.translatable("screen.cresora.guide.back"))
        }
    }

    fun chapterIndicatorDisplay(chapterIndex: Int, currentChapter: Int, isClaimed: Boolean): ItemStack {
        val name = Text.translatable("guide.cresora.chapter_indicator", chapterIndex)
        return when {
            chapterIndex < currentChapter -> {
                // Completed
                ItemStack(Items.BOOK).apply {
                    set(DataComponentTypes.CUSTOM_NAME, name)
                    set(
                        DataComponentTypes.LORE,
                        LoreComponent(listOf(Text.translatable("guide.cresora.chapter_status.completed")))
                    )
                }
            }
            chapterIndex == currentChapter -> {
                // Active
                ItemStack(Items.WRITTEN_BOOK).apply {
                    set(DataComponentTypes.CUSTOM_NAME, name)
                    set(
                        DataComponentTypes.LORE,
                        LoreComponent(listOf(Text.translatable("guide.cresora.chapter_status.active")))
                    )
                }
            }
            else -> {
                // Locked
                ItemStack(Items.BARRIER).apply {
                    set(DataComponentTypes.CUSTOM_NAME, name)
                    set(
                        DataComponentTypes.LORE,
                        LoreComponent(listOf(Text.translatable("guide.cresora.chapter_status.locked")))
                    )
                }
            }
        }
    }

    fun chapterRewardDisplay(chapter: GuideChapter, allTasksClaimed: Boolean, isClaimed: Boolean): ItemStack {
        val name = Text.translatable("guide.cresora.chapter_reward_title", chapter.index)
        return when {
            isClaimed -> {
                ItemStack(Items.MINECART).apply {
                    set(DataComponentTypes.CUSTOM_NAME, name)
                    set(
                        DataComponentTypes.LORE,
                        LoreComponent(listOf(Text.translatable("guide.cresora.status.claimed")))
                    )
                }
            }
            allTasksClaimed -> {
                // Glow if claimable (we can use ender chest)
                ItemStack(Items.ENDER_CHEST).apply {
                    set(DataComponentTypes.CUSTOM_NAME, name)
                    set(
                        DataComponentTypes.LORE,
                        LoreComponent(
                            listOf(
                                Text.translatable("guide.cresora.reward_line", chapter.cscReward, chapter.chordReward),
                                Text.translatable(CLAIMABLE_STATUS_KEY)
                            )
                        )
                    )
                }
            }
            else -> {
                ItemStack(Items.CHEST).apply {
                    set(DataComponentTypes.CUSTOM_NAME, name)
                    set(
                        DataComponentTypes.LORE,
                        LoreComponent(
                            listOf(
                                Text.translatable("guide.cresora.reward_line", chapter.cscReward, chapter.chordReward),
                                Text.translatable("guide.cresora.status.incomplete")
                            )
                        )
                    )
                }
            }
        }
    }

    fun taskDisplay(task: GuideTask, progress: Int, isClaimed: Boolean): ItemStack {
        if (isClaimed) {
            return ItemStack(Items.GREEN_STAINED_GLASS_PANE).apply {
                set(DataComponentTypes.CUSTOM_NAME, Text.translatable(task.translationKey))
                set(
                    DataComponentTypes.LORE,
                    LoreComponent(listOf(Text.translatable("guide.cresora.status.claimed")))
                )
            }
        }

        val item = when (task.type) {
            GuideTaskType.KILL_HOSTILE -> Items.IRON_SWORD
            GuideTaskType.OBTAIN_CSC -> Items.SUNFLOWER
            GuideTaskType.RESONANCE_PULL -> Items.NOTE_BLOCK
            GuideTaskType.UPGRADE_WEAPON -> Items.ANVIL
            GuideTaskType.REACH_RANK -> Items.EXPERIENCE_BOTTLE
            GuideTaskType.CLEAR_STORY_STAGE -> Items.WRITABLE_BOOK
        }

        val isComplete = progress >= task.targetValue
        val progressText = if (task.type == GuideTaskType.CLEAR_STORY_STAGE) {
            if (isComplete) "1 / 1" else "0 / 1"
        } else {
            "${progress.coerceAtMost(task.targetValue)} / ${task.targetValue}"
        }

        return ItemStack(item).apply {
            set(DataComponentTypes.CUSTOM_NAME, Text.translatable(task.translationKey))
            set(
                DataComponentTypes.LORE,
                LoreComponent(
                    listOf(
                        Text.translatable("guide.cresora.progress", progressText),
                        Text.translatable("guide.cresora.reward_line", task.cscReward, task.chordReward),
                        if (isComplete) {
                            Text.translatable(CLAIMABLE_STATUS_KEY)
                        } else {
                            Text.translatable("guide.cresora.status.incomplete")
                        }
                    )
                )
            )
        }
    }
}
