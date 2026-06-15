package hifumi.cresora.guide

import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.client.gui.widget.ButtonWidget
import net.minecraft.component.DataComponentTypes
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.screen.slot.SlotActionType
import net.minecraft.text.Text

import hifumi.cresora.equipment.ArtifactBookScreenBase

class GuideScreen(
    handler: GuideScreenHandler,
    inventory: PlayerInventory,
    title: Text
) : ArtifactBookScreenBase<GuideScreenHandler>(handler, inventory, title) {

    private lateinit var prevButton: ButtonWidget
    private lateinit var nextButton: ButtonWidget
    private lateinit var claimChapterRewardButton: ButtonWidget
    private val taskClaimButtons = arrayOfNulls<ButtonWidget>(3)

    override fun getActiveTab(): Int = 1

    override fun init() {
        super.init()

        // 1. Position slots
        for (i in 0 until 18) {
            val slot = handler.slots[i]
            slot.x = -2000
            slot.y = -2000
        }

        // CHAPTER_REWARD_SLOT = 4 (Left Page Center)
        handler.slots[4].x = 88
        handler.slots[4].y = 135

        // TASK_1_SLOT = 11 (Right Page Card 1)
        handler.slots[11].x = 206
        handler.slots[11].y = 49

        // TASK_2_SLOT = 13 (Right Page Card 2)
        handler.slots[13].x = 206
        handler.slots[13].y = 104

        // TASK_3_SLOT = 15 (Right Page Card 3)
        handler.slots[15].x = 206
        handler.slots[15].y = 159

        // 2. Initialize Navigation and Action Buttons
        val mc = client

        prevButton = ButtonWidget.builder(Text.literal("<")) {
            val prevCh = handler.selectedChapter() - 1
            if (prevCh in 1..3 && mc != null) {
                mc.interactionManager?.clickSlot(handler.syncId, prevCh - 1, 0, SlotActionType.PICKUP, mc.player)
            }
        }.dimensions(x + 205, y + 13, 16, 16).build()

        nextButton = ButtonWidget.builder(Text.literal(">")) {
            val nextCh = handler.selectedChapter() + 1
            if (nextCh in 1..3 && mc != null) {
                mc.interactionManager?.clickSlot(handler.syncId, nextCh - 1, 0, SlotActionType.PICKUP, mc.player)
            }
        }.dimensions(x + 349, y + 13, 16, 16).build()

        claimChapterRewardButton = ButtonWidget.builder(Text.translatable("guide.cresora.status.incomplete")) {
            if (mc != null) {
                mc.interactionManager?.clickSlot(handler.syncId, 4, 0, SlotActionType.PICKUP, mc.player)
            }
        }.dimensions(x + 40, y + 172, 114, 20).build()

        for (i in 0 until 3) {
            val idx = i
            val btn = ButtonWidget.builder(Text.translatable("guide.cresora.status.claim_button")) {
                if (mc != null) {
                    mc.interactionManager?.clickSlot(handler.syncId, 11 + idx * 2, 0, SlotActionType.PICKUP, mc.player)
                }
            }.dimensions(x + 316, y + 35 + idx * 55 + 16, 42, 18).build()
            taskClaimButtons[idx] = btn
            addDrawableChild(btn)
        }

        addDrawableChild(prevButton)
        addDrawableChild(nextButton)
        addDrawableChild(claimChapterRewardButton)

        updateButtonStates()
    }

    private fun isTaskClaimable(stack: ItemStack): Boolean {
        if (stack.isEmpty || stack.item == Items.GREEN_STAINED_GLASS_PANE) return false
        val lore = stack.get(DataComponentTypes.LORE) ?: return false
        for (line in lore.lines) {
            val content = line.content
            if (content is net.minecraft.text.TranslatableTextContent && content.key == "guide.cresora.status.claimable") {
                return true
            }
            val str = line.string.lowercase()
            if (str.contains("claim") || str.contains("领取") || str.contains("可") || str.contains("click")) {
                return true
            }
        }
        return false
    }

    private fun updateButtonStates() {
        val currentCh = handler.currentChapter()
        val selectedCh = handler.selectedChapter()

        prevButton.active = (selectedCh > 1)
        nextButton.active = (selectedCh < currentCh && selectedCh < 3)

        // Update Chapter Reward Button
        if (handler.isChapterClaimed()) {
            claimChapterRewardButton.active = false
            claimChapterRewardButton.message = Text.translatable("guide.cresora.status.claimed")
        } else if (handler.allTasksClaimed()) {
            claimChapterRewardButton.active = true
            claimChapterRewardButton.message = Text.translatable("guide.cresora.status.claimable")
        } else {
            claimChapterRewardButton.active = false
            claimChapterRewardButton.message = Text.translatable("guide.cresora.status.incomplete")
        }

        // Update Task Buttons
        val chapter = GuideContentRegistry.getChapter(selectedCh)
        for (i in 0 until 3) {
            val btn = taskClaimButtons[i] ?: continue
            val slotIndex = 11 + i * 2
            if (chapter != null && i < chapter.tasks.size) {
                val stack = handler.slots[slotIndex].stack
                val claimable = isTaskClaimable(stack)
                btn.visible = claimable
                btn.active = claimable
            } else {
                btn.visible = false
                btn.active = false
            }
        }
    }

    override fun handledScreenTick() {
        super.handledScreenTick()
        updateButtonStates()
    }

    private fun drawCircle(context: DrawContext, centerX: Int, centerY: Int, radius: Int, color: Int) {
        var x = radius
        var y = 0
        var err = 0

        while (x >= y) {
            context.fill(centerX + x, centerY + y, centerX + x + 1, centerY + y + 1, color)
            context.fill(centerX + y, centerY + x, centerX + y + 1, centerY + x + 1, color)
            context.fill(centerX - y, centerY + x, centerX - y + 1, centerY + x + 1, color)
            context.fill(centerX - x, centerY + y, centerX - x + 1, centerY + y + 1, color)
            context.fill(centerX - x, centerY - y, centerX - x + 1, centerY - y + 1, color)
            context.fill(centerX - y, centerY - x, centerX - y + 1, centerY - x + 1, color)
            context.fill(centerX + y, centerY - x, centerX + y + 1, centerY - x + 1, color)
            context.fill(centerX + x, centerY - y, centerX + x + 1, centerY - y + 1, color)

            y += 1
            if (err <= 0) {
                err += 2 * y + 1
            }
            if (err > 0) {
                x -= 1
                err -= 2 * x + 1
            }
        }
    }

    override fun drawBackground(context: DrawContext, delta: Float, mouseX: Int, mouseY: Int) {
        super.drawBackground(context, delta, mouseX, mouseY)

        // --- Left Page Progress Rendering ---
        val cx = x + 97
        val cy = y + 66
        // Medallion Ring
        drawCircle(context, cx, cy, 24, 0xFFC0A678.toInt())
        drawCircle(context, cx, cy, 22, 0xFF423427.toInt())
        drawCircle(context, cx, cy, 21, 0xFF423427.toInt())

        // Calculate progress tasks count
        var completedTasks = 0
        val totalTasks = 3
        for (i in 0 until 3) {
            val slotIndex = 11 + i * 2
            val stack = handler.slots[slotIndex].stack
            if (!stack.isEmpty) {
                if (stack.item == Items.GREEN_STAINED_GLASS_PANE || isTaskClaimable(stack)) {
                    completedTasks++
                }
            }
        }

        // Draw Segmented Progress Bar (3 segments)
        for (i in 0 until 3) {
            val segX = x + 35 + i * 43
            context.fill(segX, y + 98, segX + 38, y + 104, 0xFFDDD6CD.toInt())
            if (i < completedTasks) {
                context.fill(segX, y + 98, segX + 38, y + 104, 0xFF6EAF3C.toInt())
            }
            context.fill(segX, y + 98, segX + 38, y + 99, 0x40FFFFFF)
        }

        // Reward Slot Frame (Center Bottom)
        drawSlotFrame(context, x + 88, y + 135)

        // --- Right Page Cards ---
        val selectedCh = handler.selectedChapter()
        val chapter = GuideContentRegistry.getChapter(selectedCh)
        if (chapter != null) {
            for (i in 0 until chapter.tasks.size.coerceAtMost(3)) {
                val cardY = y + 35 + i * 55
                context.fill(x + 200, cardY, x + 365, cardY + 50, 0xFFEADFCE.toInt())
                context.fill(x + 200, cardY, x + 365, cardY + 1, 0xFFD2C7B8.toInt())
                context.fill(x + 200, cardY + 49, x + 365, cardY + 50, 0xFFD2C7B8.toInt())
                context.fill(x + 200, cardY, x + 201, cardY + 50, 0xFFD2C7B8.toInt())
                context.fill(x + 364, cardY, x + 365, cardY + 50, 0xFFD2C7B8.toInt())

                drawSlotFrame(context, x + 206, cardY + 16)
            }
        }
    }


    private fun drawCenteredText(context: DrawContext, text: Text, centerX: Int, y: Int, color: Int) {
        val width = textRenderer.getWidth(text)
        context.drawText(textRenderer, text, centerX - width / 2, y, color, false)
    }

    override fun drawForeground(context: DrawContext, mouseX: Int, mouseY: Int) {
        super.drawForeground(context, mouseX, mouseY)

        // Left Page Titles & Ratios
        drawCenteredText(context, Text.translatable("guide.cresora.progress_title"), 97, 20, 0xFF3C3024.toInt())

        var completedTasks = 0
        val totalTasks = 3
        for (i in 0 until 3) {
            val slotIndex = 11 + i * 2
            val stack = handler.slots[slotIndex].stack
            if (!stack.isEmpty) {
                if (stack.item == Items.GREEN_STAINED_GLASS_PANE || isTaskClaimable(stack)) {
                    completedTasks++
                }
            }
        }
        drawCenteredText(context, Text.literal("$completedTasks / $totalTasks"), 97, 62, 0xFF3C3024.toInt())

        drawCenteredText(context, Text.translatable("guide.cresora.reward_text"), 97, 120, 0xFF5F503D.toInt())

        val selectedCh = handler.selectedChapter()
        val chapterTitle = Text.translatable("guide.cresora.chapter_indicator", selectedCh)
        drawCenteredText(context, chapterTitle, 282, 17, 0xFF3C3024.toInt())

        val chapter = GuideContentRegistry.getChapter(selectedCh)
        if (chapter != null) {
            for (i in 0 until chapter.tasks.size.coerceAtMost(3)) {
                val slotIndex = 11 + i * 2
                val stack = handler.slots[slotIndex].stack
                if (!stack.isEmpty) {
                    val cardY = 35 + i * 55
                    context.drawText(textRenderer, stack.name, 230, cardY + 5, 0xFF3C3024.toInt(), false)

                    val loreLines = stack.get(DataComponentTypes.LORE)?.lines ?: emptyList()
                    if (loreLines.isNotEmpty()) {
                        context.drawText(textRenderer, loreLines[0], 230, cardY + 16, 0xFF5F503D.toInt(), false)
                    }
                    if (loreLines.size >= 2) {
                        context.drawText(textRenderer, loreLines[1], 230, cardY + 26, 0xFF83715D.toInt(), false)
                    }

                    val isClaimed = stack.item == Items.GREEN_STAINED_GLASS_PANE
                    val isClaimable = isTaskClaimable(stack)
                    if (isClaimed) {
                        val claimedText = Text.literal("✓ ").append(Text.translatable("guide.cresora.status.claimed"))
                        context.drawText(textRenderer, claimedText, 316, cardY + 20, 0xFF5C9E32.toInt(), false)
                    } else if (!isClaimable) {
                        val incompleteText = Text.translatable("guide.cresora.chapter_status.active")
                        context.drawText(textRenderer, incompleteText, 320, cardY + 20, 0xFFD68F23.toInt(), false)
                    }
                }
            }
        }
    }

    override fun drawMouseoverTooltip(context: DrawContext, x: Int, y: Int) {
        // Task and reward details are rendered inline; item tooltips obscure the book layout.
    }
}
