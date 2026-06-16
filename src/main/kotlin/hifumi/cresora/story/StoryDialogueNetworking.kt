package hifumi.cresora.story
import hifumi.cresora.CreSoraUtilities
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.network.RegistryByteBuf
import net.minecraft.network.codec.PacketCodec
import net.minecraft.network.codec.PacketCodecs
import net.minecraft.network.packet.CustomPayload
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import net.minecraft.text.TextCodecs
import net.minecraft.util.Identifier
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

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
    private data class SimpleDialogueSession(
        val title: Text,
        val lines: List<ResolvedStoryLine>,
        val objective: Text?,
        val hints: List<Text>,
        var index: Int = 0
    )

    private val simpleDialogueSessions: MutableMap<UUID, SimpleDialogueSession> = ConcurrentHashMap()

    fun init() {
        PayloadTypeRegistry.playS2C().register(StoryDialogueStatePayload.ID, StoryDialogueStatePayload.CODEC)
        PayloadTypeRegistry.playS2C().register(StoryDialogueClosePayload.ID, StoryDialogueClosePayload.CODEC)
        PayloadTypeRegistry.playC2S().register(StoryDialogueActionPayload.ID, StoryDialogueActionPayload.CODEC)
        ServerPlayConnectionEvents.DISCONNECT.register(ServerPlayConnectionEvents.Disconnect { handler, _ ->
            simpleDialogueSessions.remove(handler.player.uuid)
        })
        ServerPlayNetworking.registerGlobalReceiver(StoryDialogueActionPayload.ID) { payload, context ->
            if (handleSimpleDialogueAction(context.player(), payload.actionId)) {
                return@registerGlobalReceiver
            }
            StoryService.handleDialogueAction(context.player(), payload.actionId)
        }
    }

    fun showSimpleDialogue(
        player: ServerPlayerEntity,
        title: Text,
        lines: List<ResolvedStoryLine>,
        objective: Text? = null,
        hints: List<Text> = emptyList()
    ) {
        val normalizedLines = lines.filter { !it.body.string.isBlank() }
        if (normalizedLines.isEmpty()) {
            close(player)
            return
        }
        val session = SimpleDialogueSession(title, normalizedLines, objective, hints)
        simpleDialogueSessions[player.uuid] = session
        sendSimpleDialogueLine(player, session)
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
        simpleDialogueSessions.remove(player.uuid)
        ServerPlayNetworking.send(player, StoryDialogueClosePayload)
    }

    private fun handleSimpleDialogueAction(player: ServerPlayerEntity, actionId: String): Boolean {
        val session = simpleDialogueSessions[player.uuid] ?: return false
        when (actionId) {
            StoryDialogueActionPayload.ACTION_CONTINUE -> session.index += 1
            StoryDialogueActionPayload.ACTION_SKIP -> session.index = session.lines.size
            else -> return true
        }
        if (session.index >= session.lines.size) {
            close(player)
        } else {
            sendSimpleDialogueLine(player, session)
        }
        return true
    }

    private fun sendSimpleDialogueLine(player: ServerPlayerEntity, session: SimpleDialogueSession) {
        val line = session.lines.getOrNull(session.index)
        if (line == null) {
            close(player)
            return
        }
        showDialogue(player, session.title, line, session.objective, session.hints)
    }
}
