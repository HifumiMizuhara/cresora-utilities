package hifumi.cresora.story

import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.widget.ButtonWidget
import net.minecraft.text.Text
import org.lwjgl.glfw.GLFW

class StoryDialogueScreen(
    initialState: StoryDialogueStatePayload
) : Screen(initialState.chapterTitle) {
    private var state: StoryDialogueStatePayload = initialState
    private var autoAdvance: Boolean = false
    private var nextAutoAdvanceAtMs: Long = 0L
    private var awaitingServerResponse: Boolean = false

    private lateinit var continueButton: ButtonWidget
    private lateinit var autoButton: ButtonWidget
    private lateinit var skipButton: ButtonWidget
    private val choiceButtons = mutableListOf<ButtonWidget>()

    override fun init() {
        super.init()
        rebuildControls()
        scheduleAutoAdvance()
    }

    fun applyState(newState: StoryDialogueStatePayload) {
        val previousMode = StoryDialogueViewMode.fromId(state.modeId)
        state = newState
        awaitingServerResponse = false
        val mode = StoryDialogueViewMode.fromId(newState.modeId)
        if (mode != StoryDialogueViewMode.DIALOGUE) {
            autoAdvance = false
        }

        if (previousMode != mode) {
            rebuildControls()
        } else if (mode == StoryDialogueViewMode.NPC_DIALOGUE) {
            rebuildChoiceButtons()
        }

        scheduleAutoAdvance()
        refreshButtons()
    }

    private fun rebuildControls() {
        clearChildren()
        choiceButtons.clear()

        val panelLeft = width / 2 - 150
        val panelTop = height - 124

        when (StoryDialogueViewMode.fromId(state.modeId)) {
            StoryDialogueViewMode.DIALOGUE -> {
                continueButton = ButtonWidget.builder(Text.translatable("screen.cresora.story.dialogue.continue")) {
                    requestContinue()
                }.dimensions(panelLeft + 196, panelTop + 92, 96, 20).build()
                autoButton = ButtonWidget.builder(autoButtonText()) {
                    autoAdvance = !autoAdvance
                    if (autoAdvance) scheduleAutoAdvance()
                    refreshButtons()
                }.dimensions(panelLeft + 92, panelTop + 92, 96, 20).build()
                skipButton = ButtonWidget.builder(Text.translatable("screen.cresora.story.dialogue.skip")) {
                    requestSkip()
                }.dimensions(panelLeft - 4, panelTop + 92, 88, 20).build()
                addDrawableChild(continueButton)
                addDrawableChild(autoButton)
                addDrawableChild(skipButton)
            }
            StoryDialogueViewMode.NPC_DIALOGUE -> {
                rebuildChoiceButtons()
            }
            StoryDialogueViewMode.COUNTDOWN -> {}
        }
        refreshButtons()
    }

    override fun tick() {
        super.tick()
        if (!autoAdvance || awaitingServerResponse) return
        if (StoryDialogueViewMode.fromId(state.modeId) != StoryDialogueViewMode.DIALOGUE || !state.canContinue) return
        if (System.currentTimeMillis() >= nextAutoAdvanceAtMs) {
            requestContinue()
        }
    }

    override fun shouldPause(): Boolean = false

    override fun shouldCloseOnEsc(): Boolean = false

    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        val mode = StoryDialogueViewMode.fromId(state.modeId)
        if ((mode == StoryDialogueViewMode.DIALOGUE || mode == StoryDialogueViewMode.NPC_DIALOGUE) && state.canContinue) {
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER || keyCode == GLFW.GLFW_KEY_SPACE) {
                requestContinue()
                return true
            }
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE && state.canSkip) {
            requestSkip()
            return true
        }
        if (mode == StoryDialogueViewMode.NPC_DIALOGUE && state.extras.choices.isNotEmpty()) {
            val index = keyCode - GLFW.GLFW_KEY_1
            if (index in 0 until state.extras.choices.size) {
                val choice = state.extras.choices[index]
                if (choice.enabled) {
                    requestChoice(choice.actionId)
                    return true
                }
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers)
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        context.fillGradient(0, 0, width, height, 0xB014101A.toInt(), 0xCC09070A.toInt())

        val mode = StoryDialogueViewMode.fromId(state.modeId)
        when (mode) {
            StoryDialogueViewMode.DIALOGUE -> {
                val chapterWidth = 220
                val chapterX = width / 2 - chapterWidth / 2
                context.fill(chapterX, 18, chapterX + chapterWidth, 42, 0xD037504C.toInt())
                context.drawCenteredTextWithShadow(textRenderer, state.chapterTitle, width / 2, 26, 0xFFF8F2E7.toInt())
                drawObjectivePanel(context)
                drawDialoguePanel(context)
            }
            StoryDialogueViewMode.NPC_DIALOGUE -> {
                drawNpcDialoguePanel(context)
            }
            StoryDialogueViewMode.COUNTDOWN -> {
                drawCountdownPanel(context)
            }
        }

        super.render(context, mouseX, mouseY, delta)
    }

    private fun drawObjectivePanel(context: DrawContext) {
        val panelX = 18
        val panelY = 18
        val panelWidth = 156
        val panelBottom = height - 18
        context.fill(panelX, panelY, panelX + panelWidth, panelBottom, 0xAF1F1820.toInt())
        context.drawTextWithShadow(
            textRenderer,
            Text.translatable("screen.cresora.story.objective_label"),
            panelX + 10,
            panelY + 10,
            0xFFEBCB8B.toInt()
        )
        var textY = panelY + 24
        if (state.showObjective) {
            context.drawWrappedText(textRenderer, state.objective, panelX + 10, textY, panelWidth - 20, 0xFFF4EFE5.toInt(), false)
            textY += textRenderer.getWrappedLinesHeight(state.objective, panelWidth - 20) + 10
        }
        if (state.extras.hints.isNotEmpty()) {
            context.drawTextWithShadow(
                textRenderer,
                Text.translatable("screen.cresora.story.hints_label"),
                panelX + 10,
                textY,
                0xFF8FBCBB.toInt()
            )
            textY += 14
            for (hint in state.extras.hints) {
                context.drawWrappedText(textRenderer, hint, panelX + 10, textY, panelWidth - 20, 0xFFD8DEE9.toInt(), false)
                textY += textRenderer.getWrappedLinesHeight(hint, panelWidth - 20) + 6
            }
        }
    }

    private fun drawDialoguePanel(context: DrawContext) {
        val panelWidth = width - 220
        val panelLeft = width / 2 - panelWidth / 2 + 34
        val panelTop = height - 124
        context.fill(panelLeft, panelTop, panelLeft + panelWidth, panelTop + 112, 0xD62A211B.toInt())
        context.fill(panelLeft + 2, panelTop + 2, panelLeft + panelWidth - 2, panelTop + 110, 0xEAEADFC9.toInt())
        if (state.showSpeaker) {
            context.fill(panelLeft + 12, panelTop - 18, panelLeft + 118, panelTop + 2, 0xE6486C67.toInt())
            context.drawTextWithShadow(textRenderer, state.speaker, panelLeft + 20, panelTop - 12, 0xFFFDF6EA.toInt())
        }
        context.drawWrappedText(textRenderer, state.body, panelLeft + 16, panelTop + 18, panelWidth - 32, 0xFF231A14.toInt(), false)
    }

    private fun drawNpcDialoguePanel(context: DrawContext) {
        val panelWidth = width - 80
        val panelLeft = width / 2 - panelWidth / 2
        val dialogueHeight = 80 + state.extras.choices.size * 26
        val panelTop = height - dialogueHeight - 24

        context.fill(panelLeft, panelTop, panelLeft + panelWidth, panelTop + dialogueHeight, 0xD62A211B.toInt())
        context.fill(panelLeft + 2, panelTop + 2, panelLeft + panelWidth - 2, panelTop + dialogueHeight - 2, 0xEAEADFC9.toInt())

        if (state.showSpeaker) {
            context.fill(panelLeft + 12, panelTop - 18, panelLeft + 118, panelTop + 2, 0xE6486C67.toInt())
            context.drawTextWithShadow(textRenderer, state.speaker, panelLeft + 20, panelTop - 12, 0xFFFDF6EA.toInt())
        }
        context.drawWrappedText(textRenderer, state.body, panelLeft + 16, panelTop + 18, panelWidth - 32, 0xFF231A14.toInt(), false)

        if (state.canContinue) {
            val continueWidth = 80
            val cx = panelLeft + panelWidth - continueWidth - 12
            val cy = panelTop + dialogueHeight - 28
            context.fill(cx, cy, cx + continueWidth, cy + 18, 0xD0486C67.toInt())
            context.drawCenteredTextWithShadow(textRenderer, Text.literal("▼"), cx + continueWidth / 2, cy + 4, 0xFFFDF6EA.toInt())
        }
    }

    private fun drawCountdownPanel(context: DrawContext) {
        val boxWidth = 260
        val boxHeight = 140
        val boxLeft = width / 2 - boxWidth / 2 + 48
        val boxTop = height / 2 - boxHeight / 2
        context.fill(boxLeft, boxTop, boxLeft + boxWidth, boxTop + boxHeight, 0xC0282028.toInt())
        context.fill(boxLeft + 2, boxTop + 2, boxLeft + boxWidth - 2, boxTop + boxHeight - 2, 0xE9F1E2C9.toInt())
        context.drawCenteredTextWithShadow(
            textRenderer,
            Text.translatable("screen.cresora.story.dialogue.countdown_label"),
            boxLeft + boxWidth / 2,
            boxTop + 20,
            0xFF3C2A21.toInt()
        )
        context.drawCenteredTextWithShadow(
            textRenderer,
            Text.literal(state.countdownValue.toString()),
            boxLeft + boxWidth / 2,
            boxTop + 72,
            0xFFB84A3A.toInt()
        )
    }

    private fun clearChoiceButtons() {
        choiceButtons.forEach { remove(it) }
        choiceButtons.clear()
    }

    private fun rebuildChoiceButtons() {
        clearChoiceButtons()
        val panelWidth = width - 80
        val panelLeft = width / 2 - panelWidth / 2
        val dialogueHeight = 80 + state.extras.choices.size * 26
        val panelTop = height - dialogueHeight - 24
        val buttonWidth = panelWidth - 32
        val buttonX = panelLeft + 16

        state.extras.choices.forEachIndexed { index, choice ->
            val buttonY = panelTop + 54 + index * 26
            val label = if (choice.enabled) {
                Text.literal("${index + 1}. ").copy().append(choice.label)
            } else {
                Text.literal("${index + 1}. ").copy().append(choice.label).formatted(net.minecraft.util.Formatting.GRAY, net.minecraft.util.Formatting.STRIKETHROUGH)
            }
            val btn = ButtonWidget.builder(label) {
                if (choice.enabled && !awaitingServerResponse) {
                    requestChoice(choice.actionId)
                }
            }.dimensions(buttonX, buttonY, buttonWidth, 20).build()
            btn.active = choice.enabled && !awaitingServerResponse
            choiceButtons.add(btn)
            addDrawableChild(btn)
        }

        if (state.canContinue) {
            val continueWidth = 80
            val cx = panelLeft + panelWidth - continueWidth - 12
            val cy = panelTop + dialogueHeight - 28
            choiceButtons.add(ButtonWidget.builder(Text.literal("▼")) {
                if (!awaitingServerResponse) requestContinue()
            }.dimensions(cx, cy, continueWidth, 18).build().also { addDrawableChild(it) })
        }
    }

    private fun refreshButtons() {
        val mode = StoryDialogueViewMode.fromId(state.modeId)
        if (mode == StoryDialogueViewMode.DIALOGUE) {
            continueButton.visible = true
            continueButton.active = state.canContinue && !awaitingServerResponse
            skipButton.visible = true
            skipButton.active = state.canSkip && !awaitingServerResponse
            autoButton.visible = true
            autoButton.active = state.canContinue && !awaitingServerResponse
            autoButton.message = autoButtonText()
        }
    }

    private fun autoButtonText(): Text {
        return if (autoAdvance) {
            Text.translatable("screen.cresora.story.dialogue.auto_on")
        } else {
            Text.translatable("screen.cresora.story.dialogue.auto_off")
        }
    }

    private fun requestContinue() {
        if (awaitingServerResponse || !state.canContinue) return
        awaitingServerResponse = true
        StoryDialogueClient.sendContinue()
        refreshButtons()
    }

    private fun requestSkip() {
        if (awaitingServerResponse || !state.canSkip) return
        autoAdvance = false
        awaitingServerResponse = true
        StoryDialogueClient.sendSkip()
        refreshButtons()
    }

    private fun requestChoice(actionId: String) {
        if (awaitingServerResponse) return
        awaitingServerResponse = true
        StoryDialogueClient.sendChoice(actionId)
    }

    private fun scheduleAutoAdvance() {
        if (!autoAdvance || StoryDialogueViewMode.fromId(state.modeId) != StoryDialogueViewMode.DIALOGUE) {
            nextAutoAdvanceAtMs = Long.MAX_VALUE
            return
        }
        val baseDelay = 1700L
        val textDelay = state.body.string.length.toLong() * 55L
        nextAutoAdvanceAtMs = System.currentTimeMillis() + maxOf(baseDelay, textDelay)
    }
}
