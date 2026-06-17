package hifumi.cresora.story
import hifumi.cresora.CreSoraUtilities
import hifumi.cresora.npc.NpcDialogueChoice
import hifumi.cresora.npc.NpcDialogueNode
import hifumi.cresora.story.StoryDialogueViewMode
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
    COUNTDOWN("countdown"),
    NPC_DIALOGUE("npc_dialogue");

    companion object {
        fun fromId(id: String): StoryDialogueViewMode = entries.firstOrNull { it.id == id } ?: DIALOGUE
    }
}

data class DialogueChoicePayload(
    val actionId: String,
    val label: Text,
    val enabled: Boolean
) {
    companion object {
        val CODEC: PacketCodec<RegistryByteBuf, DialogueChoicePayload> = PacketCodec.tuple(
            PacketCodecs.STRING,
            DialogueChoicePayload::actionId,
            TextCodecs.REGISTRY_PACKET_CODEC,
            DialogueChoicePayload::label,
            PacketCodecs.BOOLEAN,
            DialogueChoicePayload::enabled,
            ::DialogueChoicePayload
        )
    }
}

data class DialogueExtraLists(
    val hints: List<Text>,
    val choices: List<DialogueChoicePayload>
) {
    companion object {
        val CODEC: PacketCodec<RegistryByteBuf, DialogueExtraLists> = PacketCodec.tuple(
            TextCodecs.REGISTRY_PACKET_CODEC.collect(PacketCodecs.toList()),
            DialogueExtraLists::hints,
            DialogueChoicePayload.CODEC.collect(PacketCodecs.toList()),
            DialogueExtraLists::choices,
            ::DialogueExtraLists
        )
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
    val countdownValue: Int,
    val canContinue: Boolean,
    val canSkip: Boolean,
    val extras: DialogueExtraLists
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
            PacketCodecs.VAR_INT,
            StoryDialogueStatePayload::countdownValue,
            PacketCodecs.BOOLEAN,
            StoryDialogueStatePayload::canContinue,
            PacketCodecs.BOOLEAN,
            StoryDialogueStatePayload::canSkip,
            DialogueExtraLists.CODEC,
            StoryDialogueStatePayload::extras,
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

data class NpcDialogueSession(
    val treeId: String,
    val playerUuid: UUID,
    var currentNodeId: String
)

object StoryDialogueNetworking {
    private data class SimpleDialogueSession(
        val title: Text,
        val lines: List<ResolvedStoryLine>,
        val objective: Text?,
        val hints: List<Text>,
        var index: Int = 0
    )

    private val simpleDialogueSessions: MutableMap<UUID, SimpleDialogueSession> = ConcurrentHashMap()
    private val npcDialogueSessions: MutableMap<UUID, NpcDialogueSession> = ConcurrentHashMap()

    fun init() {
        PayloadTypeRegistry.playS2C().register(StoryDialogueStatePayload.ID, StoryDialogueStatePayload.CODEC)
        PayloadTypeRegistry.playS2C().register(StoryDialogueClosePayload.ID, StoryDialogueClosePayload.CODEC)
        PayloadTypeRegistry.playC2S().register(StoryDialogueActionPayload.ID, StoryDialogueActionPayload.CODEC)
        ServerPlayConnectionEvents.DISCONNECT.register(ServerPlayConnectionEvents.Disconnect { handler, _ ->
            simpleDialogueSessions.remove(handler.player.uuid)
            npcDialogueSessions.remove(handler.player.uuid)
        })
        ServerPlayNetworking.registerGlobalReceiver(StoryDialogueActionPayload.ID) { payload, context ->
            if (handleSimpleDialogueAction(context.player(), payload.actionId)) {
                return@registerGlobalReceiver
            }
            if (handleNpcDialogueAction(context.player(), payload.actionId)) {
                return@registerGlobalReceiver
            }
            StoryService.handleDialogueAction(context.player(), payload.actionId)
        }
    }

    fun startNpcDialogue(player: ServerPlayerEntity, treeId: String) {
        val node = hifumi.cresora.npc.NpcDialogueContentRegistry.getRootNode(treeId)
        if (node == null) {
            return
        }
        npcDialogueSessions[player.uuid] = NpcDialogueSession(treeId, player.uuid, node.id)
        sendNpcDialogueNode(player, node)
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
                countdownValue = 0,
                canContinue = true,
                canSkip = true,
                extras = DialogueExtraLists(hints = hints, choices = emptyList())
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
                countdownValue = countdownValue,
                canContinue = false,
                canSkip = false,
                extras = DialogueExtraLists(hints = hints, choices = emptyList())
            )
        )
    }

    fun close(player: ServerPlayerEntity) {
        simpleDialogueSessions.remove(player.uuid)
        npcDialogueSessions.remove(player.uuid)
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

    private fun handleNpcDialogueAction(player: ServerPlayerEntity, actionId: String): Boolean {
        val session = npcDialogueSessions[player.uuid] ?: return false
        val currentNode = hifumi.cresora.npc.NpcDialogueContentRegistry.getNode(session.treeId, session.currentNodeId)
            ?: return run { close(player); true }

        val choice = currentNode.choices?.find { it.actionId == actionId }
        if (choice != null) {
            if (choice.requiredFlags != null && !hifumi.cresora.story.StoryFlagService.checkFlags(player, choice.requiredFlags)) {
                return true
            }
            resolveNpcChoice(player, session, choice)
            return true
        }

        if (actionId == StoryDialogueActionPayload.ACTION_CONTINUE && currentNode.choices.isNullOrEmpty()) {
            advanceNpcNode(player, session, currentNode.nextNodeId)
            return true
        }

        return false
    }

    private fun resolveNpcChoice(player: ServerPlayerEntity, session: NpcDialogueSession, choice: NpcDialogueChoice) {
        val currentNode = hifumi.cresora.npc.NpcDialogueContentRegistry.getNode(session.treeId, session.currentNodeId)
        val nextNodeId = choice.nextNodeId ?: currentNode?.nextNodeId ?: choice.actionId
        advanceNpcNode(player, session, nextNodeId)
    }

    private fun advanceNpcNode(player: ServerPlayerEntity, session: NpcDialogueSession, targetNodeId: String?) {
        if (targetNodeId == null) {
            close(player)
            return
        }
        val currentNode = hifumi.cresora.npc.NpcDialogueContentRegistry.getNode(session.treeId, session.currentNodeId)
        currentNode?.flagsToSet?.forEach { flag ->
            hifumi.cresora.story.StoryFlagService.setFlag(player, flag)
        }
        val nextNode = hifumi.cresora.npc.NpcDialogueContentRegistry.getNode(session.treeId, targetNodeId)
        if (nextNode != null && isNodeAccessible(player, nextNode)) {
            session.currentNodeId = nextNode.id
            sendNpcDialogueNode(player, nextNode)
        } else {
            close(player)
        }
    }

    private fun isNodeAccessible(player: ServerPlayerEntity, node: NpcDialogueNode): Boolean {
        if (node.conditionFlags == null) return true
        return hifumi.cresora.story.StoryFlagService.checkFlags(player, node.conditionFlags)
    }

    private fun sendNpcDialogueNode(player: ServerPlayerEntity, node: NpcDialogueNode) {
        val choicePayloads = node.choices?.mapNotNull { choice ->
            val enabled = choice.requiredFlags?.let { hifumi.cresora.story.StoryFlagService.checkFlags(player, it) } ?: true
            DialogueChoicePayload(choice.actionId, choice.label, enabled)
        } ?: emptyList()

        val hasChoices = choicePayloads.isNotEmpty()
        ServerPlayNetworking.send(
            player,
            StoryDialogueStatePayload(
                modeId = StoryDialogueViewMode.NPC_DIALOGUE.id,
                chapterTitle = Text.empty(),
                speaker = node.speaker ?: Text.empty(),
                showSpeaker = node.speaker != null,
                body = node.body,
                objective = Text.empty(),
                showObjective = false,
                countdownValue = 0,
                canContinue = !hasChoices,
                canSkip = false,
                extras = DialogueExtraLists(hints = emptyList(), choices = choicePayloads)
            )
        )
    }
}
