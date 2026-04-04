package hifumi.cresora

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.network.RegistryByteBuf
import net.minecraft.network.codec.PacketCodec
import net.minecraft.network.codec.PacketCodecs
import net.minecraft.network.packet.CustomPayload
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import net.minecraft.text.TextCodecs
import net.minecraft.util.Identifier

enum class StoryDialogueViewMode(val id: String) {
    DIALOGUE("dialogue"),
    COUNTDOWN("countdown");

    companion object {
        fun fromId(id: String): StoryDialogueViewMode = entries.firstOrNull { it.id == id } ?: DIALOGUE
    }
}

data class StoryDialogueStatePayload(
    val modeId: String,
    val chapterTitle: Text,
    val speaker: Text,
    val showSpeaker: Boolean,
    val body: Text,
    val objective: Text,
    val showObjective: Boolean,
    val hints: List<Text>,
    val countdownValue: Int,
    val canContinue: Boolean,
    val canSkip: Boolean
) : CustomPayload {
    override fun getId(): CustomPayload.Id<StoryDialogueStatePayload> = ID

    companion object {
        val ID: CustomPayload.Id<StoryDialogueStatePayload> = CustomPayload.Id(Identifier.of(CreSoraUtilities.MOD_ID, "story_dialogue_state"))
        val CODEC: PacketCodec<RegistryByteBuf, StoryDialogueStatePayload> = PacketCodec.tuple(
            PacketCodecs.STRING,
            StoryDialogueStatePayload::modeId,
            TextCodecs.REGISTRY_PACKET_CODEC,
            StoryDialogueStatePayload::chapterTitle,
            TextCodecs.REGISTRY_PACKET_CODEC,
            StoryDialogueStatePayload::speaker,
            PacketCodecs.BOOLEAN,
            StoryDialogueStatePayload::showSpeaker,
            TextCodecs.REGISTRY_PACKET_CODEC,
            StoryDialogueStatePayload::body,
            TextCodecs.REGISTRY_PACKET_CODEC,
            StoryDialogueStatePayload::objective,
            PacketCodecs.BOOLEAN,
            StoryDialogueStatePayload::showObjective,
            TextCodecs.REGISTRY_PACKET_CODEC.collect(PacketCodecs.toList()),
            StoryDialogueStatePayload::hints,
            PacketCodecs.VAR_INT,
            StoryDialogueStatePayload::countdownValue,
            PacketCodecs.BOOLEAN,
            StoryDialogueStatePayload::canContinue,
            PacketCodecs.BOOLEAN,
            StoryDialogueStatePayload::canSkip,
            ::StoryDialogueStatePayload
        )
    }
}

object StoryDialogueClosePayload : CustomPayload {
    override fun getId(): CustomPayload.Id<StoryDialogueClosePayload> = ID

    val ID: CustomPayload.Id<StoryDialogueClosePayload> = CustomPayload.Id(Identifier.of(CreSoraUtilities.MOD_ID, "story_dialogue_close"))
    val CODEC: PacketCodec<RegistryByteBuf, StoryDialogueClosePayload> = PacketCodec.unit(StoryDialogueClosePayload)
}

data class StoryDialogueActionPayload(
    val actionId: String
) : CustomPayload {
    override fun getId(): CustomPayload.Id<StoryDialogueActionPayload> = ID

    companion object {
        const val ACTION_CONTINUE = "continue"
        const val ACTION_SKIP = "skip"

        val ID: CustomPayload.Id<StoryDialogueActionPayload> = CustomPayload.Id(Identifier.of(CreSoraUtilities.MOD_ID, "story_dialogue_action"))
        val CODEC: PacketCodec<RegistryByteBuf, StoryDialogueActionPayload> = PacketCodec.tuple(
            PacketCodecs.STRING,
            StoryDialogueActionPayload::actionId,
            ::StoryDialogueActionPayload
        )
    }
}

object StoryDialogueNetworking {
    fun init() {
        PayloadTypeRegistry.playS2C().register(StoryDialogueStatePayload.ID, StoryDialogueStatePayload.CODEC)
        PayloadTypeRegistry.playS2C().register(StoryDialogueClosePayload.ID, StoryDialogueClosePayload.CODEC)
        PayloadTypeRegistry.playC2S().register(StoryDialogueActionPayload.ID, StoryDialogueActionPayload.CODEC)
        ServerPlayNetworking.registerGlobalReceiver(StoryDialogueActionPayload.ID) { payload, context ->
            StoryService.handleDialogueAction(context.player(), payload.actionId)
        }
    }

    fun showDialogue(
        player: ServerPlayerEntity,
        chapterTitle: Text,
        line: ResolvedStoryLine,
        objective: Text?,
        hints: List<Text>
    ) {
        ServerPlayNetworking.send(
            player,
            StoryDialogueStatePayload(
                modeId = StoryDialogueViewMode.DIALOGUE.id,
                chapterTitle = chapterTitle,
                speaker = line.speaker ?: Text.empty(),
                showSpeaker = line.speaker != null,
                body = line.body,
                objective = objective ?: Text.empty(),
                showObjective = objective != null,
                hints = hints,
                countdownValue = 0,
                canContinue = true,
                canSkip = true
            )
        )
    }

    fun showCountdown(
        player: ServerPlayerEntity,
        chapterTitle: Text,
        objective: Text?,
        hints: List<Text>,
        countdownValue: Int
    ) {
        ServerPlayNetworking.send(
            player,
            StoryDialogueStatePayload(
                modeId = StoryDialogueViewMode.COUNTDOWN.id,
                chapterTitle = chapterTitle,
                speaker = Text.empty(),
                showSpeaker = false,
                body = Text.translatable("screen.cresora.story.dialogue.countdown_label"),
                objective = objective ?: Text.empty(),
                showObjective = objective != null,
                hints = hints,
                countdownValue = countdownValue,
                canContinue = false,
                canSkip = false
            )
        )
    }

    fun close(player: ServerPlayerEntity) {
        ServerPlayNetworking.send(player, StoryDialogueClosePayload)
    }
}
