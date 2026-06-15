package hifumi.cresora.guide

import hifumi.cresora.CreSoraUtilities
import hifumi.cresora.ScreenSyncSupport
import hifumi.cresora.equipment.ArtifactUiFlow
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.inventory.Inventory
import net.minecraft.inventory.SimpleInventory
import net.minecraft.item.ItemStack
import net.minecraft.screen.ArrayPropertyDelegate
import net.minecraft.screen.PropertyDelegate
import net.minecraft.screen.ScreenHandler
import net.minecraft.screen.slot.Slot
import net.minecraft.screen.slot.SlotActionType
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.util.Identifier

class GuideScreenHandler(
    syncId: Int,
    private val playerInventory: PlayerInventory
) : ScreenHandler(CreSoraUtilities.GUIDE_SCREEN_HANDLER, syncId) {

    companion object {
        private const val ROWS = 2
        private const val SLOT_COUNT = 18

        private const val CHAPTER_1_SLOT = 0
        private const val CHAPTER_2_SLOT = 1
        private const val CHAPTER_3_SLOT = 2
        private const val STORY_TAB_SLOT = 3
        private const val CHAPTER_REWARD_SLOT = 4
        private const val CONQUEST_TAB_SLOT = 5
        private const val RESONANCE_TAB_SLOT = 6
        private const val SHOP_TAB_SLOT = 7
        private const val BACK_SLOT = 8

        private const val TASK_1_SLOT = 11
        private const val TASK_2_SLOT = 13
        private const val TASK_3_SLOT = 15

        private const val PROPERTY_CURRENT_CHAPTER = 0
        private const val PROPERTY_SELECTED_CHAPTER = 1
        private const val PROPERTY_ALL_TASKS_CLAIMED = 2
        private const val PROPERTY_CHAPTER_CLAIMED = 3
    }

    private val displayInventory: Inventory = object : SimpleInventory(SLOT_COUNT) {}
    private val properties: PropertyDelegate = ArrayPropertyDelegate(4)

    private var clientSelectedChapter = 1

    init {
        addProperties(properties)
        if (playerInventory.player is ServerPlayerEntity) {
            val serverPlayer = playerInventory.player as ServerPlayerEntity
            val currentCh = GuideService.getPlayerChapter(serverPlayer)
            properties.set(PROPERTY_CURRENT_CHAPTER, currentCh)
            properties.set(PROPERTY_SELECTED_CHAPTER, currentCh)
        } else {
            properties.set(PROPERTY_CURRENT_CHAPTER, 1)
            properties.set(PROPERTY_SELECTED_CHAPTER, 1)
        }

        refreshProperties()
        refreshEntries()

        for (index in 0 until SLOT_COUNT) {
            addSlot(object : Slot(displayInventory, index, 8 + (index % 9) * 18, 18 + (index / 9) * 18) {
                override fun canInsert(stack: ItemStack): Boolean = false
                override fun canTakeItems(playerEntity: PlayerEntity): Boolean = false
                override fun getBackgroundSprite(): Identifier? = null
            })
        }
    }

    override fun canUse(player: PlayerEntity): Boolean = true

    override fun sendContentUpdates() {
        refreshProperties()
        refreshEntries()
        super.sendContentUpdates()
    }

    override fun quickMove(player: PlayerEntity, slotIndex: Int): ItemStack = ItemStack.EMPTY

    override fun onSlotClick(slotIndex: Int, button: Int, actionType: SlotActionType, player: PlayerEntity) {
        if (slotIndex in 0 until SLOT_COUNT) {
            if (actionType == SlotActionType.PICKUP || actionType == SlotActionType.QUICK_MOVE) {
                val serverPlayer = player as? ServerPlayerEntity ?: return
                val currentChapter = GuideService.getPlayerChapter(serverPlayer)
                val selectedChapter = properties.get(PROPERTY_SELECTED_CHAPTER)

                when (slotIndex) {
                    CHAPTER_1_SLOT -> {
                        if (1 <= currentChapter) {
                            properties.set(PROPERTY_SELECTED_CHAPTER, 1)
                        }
                    }
                    CHAPTER_2_SLOT -> {
                        if (2 <= currentChapter) {
                            properties.set(PROPERTY_SELECTED_CHAPTER, 2)
                        }
                    }
                    CHAPTER_3_SLOT -> {
                        if (3 <= currentChapter) {
                            properties.set(PROPERTY_SELECTED_CHAPTER, 3)
                        }
                    }
                    STORY_TAB_SLOT -> {
                        ArtifactUiFlow.openStoryChapterSelection(serverPlayer)
                    }
                    CONQUEST_TAB_SLOT -> {
                        ArtifactUiFlow.openDomainSelection(serverPlayer)
                    }
                    RESONANCE_TAB_SLOT -> {
                        ArtifactUiFlow.openResonance(serverPlayer)
                    }
                    SHOP_TAB_SLOT -> {
                        ArtifactUiFlow.openShop(serverPlayer)
                    }
                    BACK_SLOT -> {
                        serverPlayer.closeHandledScreen()
                    }
                    CHAPTER_REWARD_SLOT -> {
                        GuideService.claimChapterReward(serverPlayer, selectedChapter)
                    }
                    TASK_1_SLOT -> {
                        val chapter = GuideContentRegistry.getChapter(selectedChapter)
                        if (chapter != null && chapter.tasks.size > 0) {
                            GuideService.claimTaskReward(serverPlayer, chapter.tasks[0].id)
                        }
                    }
                    TASK_2_SLOT -> {
                        val chapter = GuideContentRegistry.getChapter(selectedChapter)
                        if (chapter != null && chapter.tasks.size > 1) {
                            GuideService.claimTaskReward(serverPlayer, chapter.tasks[1].id)
                        }
                    }
                    TASK_3_SLOT -> {
                        val chapter = GuideContentRegistry.getChapter(selectedChapter)
                        if (chapter != null && chapter.tasks.size > 2) {
                            GuideService.claimTaskReward(serverPlayer, chapter.tasks[2].id)
                        }
                    }
                }
                refreshProperties()
                refreshEntries()
            }
            return
        }
        super.onSlotClick(slotIndex, button, actionType, player)
    }

    fun currentChapter(): Int = properties.get(PROPERTY_CURRENT_CHAPTER)

    fun selectedChapter(): Int = properties.get(PROPERTY_SELECTED_CHAPTER)

    fun allTasksClaimed(): Boolean = properties.get(PROPERTY_ALL_TASKS_CLAIMED) > 0

    fun isChapterClaimed(): Boolean = properties.get(PROPERTY_CHAPTER_CLAIMED) > 0

    private fun refreshProperties() {
        val player = playerInventory.player as? ServerPlayerEntity ?: return
        val currentCh = GuideService.getPlayerChapter(player)
        val selectedCh = properties.get(PROPERTY_SELECTED_CHAPTER)

        properties.set(PROPERTY_CURRENT_CHAPTER, currentCh)

        val chapter = GuideContentRegistry.getChapter(selectedCh)
        if (chapter != null) {
            val claimedTasks = GuideService.getClaimedTasks(player)
            val allTasksClaimed = chapter.tasks.all { claimedTasks.contains(it.id) }
            val chapterClaimed = GuideService.getClaimedChapters(player).contains(selectedCh)

            properties.set(PROPERTY_ALL_TASKS_CLAIMED, if (allTasksClaimed) 1 else 0)
            properties.set(PROPERTY_CHAPTER_CLAIMED, if (chapterClaimed) 1 else 0)
        }
    }

    private fun refreshEntries() {
        val player = playerInventory.player
        val currentCh = if (player is ServerPlayerEntity) {
            GuideService.getPlayerChapter(player)
        } else {
            properties.get(PROPERTY_CURRENT_CHAPTER)
        }

        val selectedCh = properties.get(PROPERTY_SELECTED_CHAPTER)
        val chapter = GuideContentRegistry.getChapter(selectedCh)

        val claimedTasks = if (player is ServerPlayerEntity) {
            GuideService.getClaimedTasks(player)
        } else emptySet()

        val claimedChapters = if (player is ServerPlayerEntity) {
            GuideService.getClaimedChapters(player)
        } else emptySet()

        // 1. Fill default background
        for (index in 0 until SLOT_COUNT) {
            displayInventory.setStack(index, GuideDisplayStackFactory.fillerDisplay())
        }

        // 2. Set indicators for chapters 1, 2, 3
        displayInventory.setStack(
            CHAPTER_1_SLOT,
            GuideDisplayStackFactory.chapterIndicatorDisplay(1, currentCh, claimedChapters.contains(1))
        )
        displayInventory.setStack(
            CHAPTER_2_SLOT,
            GuideDisplayStackFactory.chapterIndicatorDisplay(2, currentCh, claimedChapters.contains(2))
        )
        displayInventory.setStack(
            CHAPTER_3_SLOT,
            GuideDisplayStackFactory.chapterIndicatorDisplay(3, currentCh, claimedChapters.contains(3))
        )

        // 3. Set Chapter Reward
        if (chapter != null) {
            val allTasksClaimed = if (player is ServerPlayerEntity) {
                chapter.tasks.all { claimedTasks.contains(it.id) }
            } else {
                properties.get(PROPERTY_ALL_TASKS_CLAIMED) > 0
            }
            val isChapterClaimed = if (player is ServerPlayerEntity) {
                claimedChapters.contains(selectedCh)
            } else {
                properties.get(PROPERTY_CHAPTER_CLAIMED) > 0
            }

            displayInventory.setStack(
                CHAPTER_REWARD_SLOT,
                GuideDisplayStackFactory.chapterRewardDisplay(chapter, allTasksClaimed, isChapterClaimed)
            )

            // 4. Set tasks
            if (chapter.tasks.size > 0) {
                val task = chapter.tasks[0]
                val prog = if (player is ServerPlayerEntity) GuideService.getTaskProgress(player, task) else 0
                displayInventory.setStack(
                    TASK_1_SLOT,
                    GuideDisplayStackFactory.taskDisplay(task, prog, claimedTasks.contains(task.id))
                )
            }
            if (chapter.tasks.size > 1) {
                val task = chapter.tasks[1]
                val prog = if (player is ServerPlayerEntity) GuideService.getTaskProgress(player, task) else 0
                displayInventory.setStack(
                    TASK_2_SLOT,
                    GuideDisplayStackFactory.taskDisplay(task, prog, claimedTasks.contains(task.id))
                )
            }
            if (chapter.tasks.size > 2) {
                val task = chapter.tasks[2]
                val prog = if (player is ServerPlayerEntity) GuideService.getTaskProgress(player, task) else 0
                displayInventory.setStack(
                    TASK_3_SLOT,
                    GuideDisplayStackFactory.taskDisplay(task, prog, claimedTasks.contains(task.id))
                )
            }
        }

        // 5. Back Button
        displayInventory.setStack(BACK_SLOT, GuideDisplayStackFactory.backDisplay())
    }
}
