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

    override fun init() {
        super.init()
        val panelLeft = width / 2 - 150
        val panelTop = height - 124
        continueButton = ButtonWidget.builder(Text.translatable("screen.cresora.story.dialogue.continue")) {
            requestContinue()
        }.dimensions(panelLeft + 196, panelTop + 92, 96, 20).build()
        autoButton = ButtonWidget.builder(autoButtonText()) {
            autoAdvance = !autoAdvance
            if (autoAdvance) {
                scheduleAutoAdvance()
            }
            refreshButtons()
        }.dimensions(panelLeft + 92, panelTop + 92, 96, 20).build()
        skipButton = ButtonWidget.builder(Text.translatable("screen.cresora.story.dialogue.skip")) {
            requestSkip()
        }.dimensions(panelLeft - 4, panelTop + 92, 88, 20).build()
        addDrawableChild(continueButton)
        addDrawableChild(autoButton)
        addDrawableChild(skipButton)
        refreshButtons()
        scheduleAutoAdvance()
    }

    fun applyState(newState: StoryDialogueStatePayload) {
        state = newState
        awaitingServerResponse = false
        if (StoryDialogueViewMode.fromId(newState.modeId) != StoryDialogueViewMode.DIALOGUE) {
            autoAdvance = false
        }
        scheduleAutoAdvance()
        if (this::continueButton.isInitialized) {
            refreshButtons()
        }
    }

    override fun tick() {
        super.tick()
        if (!autoAdvance || awaitingServerResponse) {
            return
        }
        if (StoryDialogueViewMode.fromId(state.modeId) != StoryDialogueViewMode.DIALOGUE || !state.canContinue) {
            return
        }
        if (System.currentTimeMillis() >= nextAutoAdvanceAtMs) {
            requestContinue()
        }
    }

    override fun shouldPause(): Boolean = false

    override fun shouldCloseOnEsc(): Boolean = false

    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        if (StoryDialogueViewMode.fromId(state.modeId) == StoryDialogueViewMode.DIALOGUE && state.canContinue) {
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER || keyCode == GLFW.GLFW_KEY_SPACE) {
                requestContinue()
                return true
            }
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE && state.canSkip) {
            requestSkip()
            return true
        }
        return super.keyPressed(keyCode, scanCode, modifiers)
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        // Avoid Screen.renderBackground here: on 1.21.7 the blur path can be invoked twice
        // in the same frame for this custom full-screen UI and crash the client.
        context.fillGradient(0, 0, width, height, 0xB014101A.toInt(), 0xCC09070A.toInt())

        val chapterWidth = 220
        val chapterX = width / 2 - chapterWidth / 2
        context.fill(chapterX, 18, chapterX + chapterWidth, 42, 0xD037504C.toInt())
        context.drawCenteredTextWithShadow(textRenderer, state.chapterTitle, width / 2, 26, 0xFFF8F2E7.toInt())

        drawObjectivePanel(context)

        when (StoryDialogueViewMode.fromId(state.modeId)) {
            StoryDialogueViewMode.DIALOGUE -> drawDialoguePanel(context)
            StoryDialogueViewMode.COUNTDOWN -> drawCountdownPanel(context)
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
        if (state.hints.isNotEmpty()) {
            context.drawTextWithShadow(
                textRenderer,
                Text.translatable("screen.cresora.story.hints_label"),
                panelX + 10,
                textY,
                0xFF8FBCBB.toInt()
            )
            textY += 14
            for (hint in state.hints) {
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

    private fun refreshButtons() {
        val isDialogue = StoryDialogueViewMode.fromId(state.modeId) == StoryDialogueViewMode.DIALOGUE
        continueButton.visible = isDialogue
        continueButton.active = isDialogue && state.canContinue && !awaitingServerResponse
        skipButton.visible = isDialogue
        skipButton.active = isDialogue && state.canSkip && !awaitingServerResponse
        autoButton.visible = isDialogue
        autoButton.active = isDialogue && state.canContinue && !awaitingServerResponse
        autoButton.message = autoButtonText()
    }

    private fun autoButtonText(): Text {
        return if (autoAdvance) {
            Text.translatable("screen.cresora.story.dialogue.auto_on")
        } else {
            Text.translatable("screen.cresora.story.dialogue.auto_off")
        }
    }

    private fun requestContinue() {
        if (awaitingServerResponse || !state.canContinue) {
            return
        }
        awaitingServerResponse = true
        StoryDialogueClient.sendContinue()
        refreshButtons()
    }

    private fun requestSkip() {
        if (awaitingServerResponse || !state.canSkip) {
            return
        }
        autoAdvance = false
        awaitingServerResponse = true
        StoryDialogueClient.sendSkip()
        refreshButtons()
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
