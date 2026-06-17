package hifumi.cresora.story
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.minecraft.client.MinecraftClient

object StoryDialogueClient {
    fun init() {
        ClientPlayNetworking.registerGlobalReceiver(StoryDialogueStatePayload.ID) { payload, context ->
            applyState(context.client(), payload)
        }
        ClientPlayNetworking.registerGlobalReceiver(StoryDialogueClosePayload.ID) { _, context ->
            close(context.client())
        }
    }

    fun sendContinue() {
        ClientPlayNetworking.send(StoryDialogueActionPayload(StoryDialogueActionPayload.ACTION_CONTINUE))
    }

    fun sendSkip() {
        ClientPlayNetworking.send(StoryDialogueActionPayload(StoryDialogueActionPayload.ACTION_SKIP))
    }

    fun sendChoice(actionId: String) {
        ClientPlayNetworking.send(StoryDialogueActionPayload(actionId))
    }

    private fun applyState(client: MinecraftClient, payload: StoryDialogueStatePayload) {
        val existing = client.currentScreen as? StoryDialogueScreen
        if (existing != null) {
            existing.applyState(payload)
        } else {
            client.setScreen(StoryDialogueScreen(payload))
        }
    }

    private fun close(client: MinecraftClient) {
        if (client.currentScreen is StoryDialogueScreen) {
            client.setScreen(null)
        }
    }
}
