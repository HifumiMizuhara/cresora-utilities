package hifumi.cresora.npc

import com.mojang.serialization.JsonOps
import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import com.google.gson.JsonParser
import hifumi.cresora.CreSoraUtilities
import net.minecraft.text.Text
import net.minecraft.text.TextCodecs
import org.slf4j.LoggerFactory

data class NpcDialogueChoice(
    val actionId: String,
    val label: Text,
    val requiredFlags: Map<String, Boolean>? = null,
    val nextNodeId: String? = null
) {
    companion object {
        private val FLAG_MAP_CODEC: Codec<Map<String, Boolean>> = Codec.unboundedMap(Codec.STRING, Codec.BOOL)
        val CODEC: Codec<NpcDialogueChoice> = RecordCodecBuilder.create { instance ->
            instance.group(
                Codec.STRING.fieldOf("actionId").forGetter(NpcDialogueChoice::actionId),
                TextCodecs.CODEC.fieldOf("label").forGetter(NpcDialogueChoice::label),
                FLAG_MAP_CODEC.optionalFieldOf("requiredFlags").forGetter { java.util.Optional.ofNullable(it.requiredFlags) },
                Codec.STRING.optionalFieldOf("nextNodeId").forGetter { java.util.Optional.ofNullable(it.nextNodeId) }
            ).apply(instance) { actionId, label, requiredFlags, nextNodeId ->
                NpcDialogueChoice(actionId, label, requiredFlags.orElse(null), nextNodeId.orElse(null))
            }
        }
    }
}

data class NpcDialogueNode(
    val id: String,
    val speaker: Text?,
    val body: Text,
    val choices: List<NpcDialogueChoice>?,
    val flagsToSet: List<String>?,
    val conditionFlags: Map<String, Boolean>?,
    val nextNodeId: String?
) {
    companion object {
        private val FLAG_MAP_CODEC: Codec<Map<String, Boolean>> = Codec.unboundedMap(Codec.STRING, Codec.BOOL)
        val CODEC: Codec<NpcDialogueNode> = RecordCodecBuilder.create { instance ->
            instance.group(
                Codec.STRING.fieldOf("id").forGetter(NpcDialogueNode::id),
                TextCodecs.CODEC.optionalFieldOf("speaker").forGetter { java.util.Optional.ofNullable(it.speaker) },
                TextCodecs.CODEC.fieldOf("body").forGetter(NpcDialogueNode::body),
                NpcDialogueChoice.CODEC.listOf().optionalFieldOf("choices").forGetter { java.util.Optional.ofNullable(it.choices) },
                Codec.STRING.listOf().optionalFieldOf("flagsToSet").forGetter { java.util.Optional.ofNullable(it.flagsToSet) },
                FLAG_MAP_CODEC.optionalFieldOf("conditionFlags").forGetter { java.util.Optional.ofNullable(it.conditionFlags) },
                Codec.STRING.optionalFieldOf("nextNodeId").forGetter { java.util.Optional.ofNullable(it.nextNodeId) }
            ).apply(instance) { id, speaker, body, choices, flagsToSet, conditionFlags, nextNodeId ->
                NpcDialogueNode(id, speaker.orElse(null), body, choices.orElse(null), flagsToSet.orElse(null), conditionFlags.orElse(null), nextNodeId.orElse(null))
            }
        }
    }
}

data class NpcDialogueTree(
    val id: String,
    val rootNodeId: String,
    val nodes: List<NpcDialogueNode>
) {
    companion object {
        val CODEC: Codec<NpcDialogueTree> = RecordCodecBuilder.create { instance ->
            instance.group(
                Codec.STRING.fieldOf("id").forGetter(NpcDialogueTree::id),
                Codec.STRING.fieldOf("rootNodeId").forGetter(NpcDialogueTree::rootNodeId),
                NpcDialogueNode.CODEC.listOf().fieldOf("nodes").forGetter(NpcDialogueTree::nodes)
            ).apply(instance, ::NpcDialogueTree)
        }
    }
}

data class NpcDialogueBundle(
    val trees: List<NpcDialogueTree>
) {
    companion object {
        val CODEC: Codec<NpcDialogueBundle> = RecordCodecBuilder.create { instance ->
            instance.group(
                NpcDialogueTree.CODEC.listOf().fieldOf("trees").forGetter(NpcDialogueBundle::trees)
            ).apply(instance, ::NpcDialogueBundle)
        }
    }
}

object NpcDialogueContentRegistry {
    private val logger = LoggerFactory.getLogger(CreSoraUtilities.MOD_ID)
    private val trees = mutableMapOf<String, NpcDialogueTree>()
    private val nodeIndex = mutableMapOf<String, NpcDialogueNode>()

    fun init() {
        trees.clear()
        nodeIndex.clear()
        val path = "data/${CreSoraUtilities.MOD_ID}/cresora/npc_dialogue_content.json"
        val resource = NpcDialogueContentRegistry::class.java.classLoader.getResourceAsStream(path)
        if (resource != null) {
            val json = JsonParser.parseReader(java.io.InputStreamReader(resource))
            val parsed = NpcDialogueBundle.CODEC.parse(JsonOps.INSTANCE, json)
            val bundle = parsed.getOrThrow { error("Failed to parse $path: $it") }
            for (tree in bundle.trees) {
                trees[tree.id] = tree
                for (node in tree.nodes) {
                    nodeIndex[tree.id + ":" + node.id] = node
                }
            }
        } else {
            registerFallback()
        }
        logger.info("Loaded {} NPC dialogue trees", trees.size)
    }

    fun getTree(treeId: String): NpcDialogueTree? = trees[treeId]

    fun getNode(treeId: String, nodeId: String): NpcDialogueNode? = nodeIndex[treeId + ":" + nodeId]

    fun getRootNode(treeId: String): NpcDialogueNode? {
        val tree = trees[treeId] ?: return null
        return nodeIndex[treeId + ":" + tree.rootNodeId]
    }

    private fun registerFallback() {
        val tree = NpcDialogueTree(
            id = "spirit_guide_intro",
            rootNodeId = "root",
            nodes = listOf(
                NpcDialogueNode(
                    id = "root",
                    speaker = Text.translatable("npc.cresora.spirit_guide.name"),
                    body = Text.translatable("npc.cresora.demo.dialogue.line_1"),
                    choices = listOf(
                        NpcDialogueChoice("choice_accept", Text.translatable("npc.cresora.demo.dialogue.choice_accept"), null, "after_accept"),
                        NpcDialogueChoice("choice_decline", Text.translatable("npc.cresora.demo.dialogue.choice_decline"), null, "after_decline")
                    ),
                    flagsToSet = null,
                    conditionFlags = null,
                    nextNodeId = null
                ),
                NpcDialogueNode(
                    id = "after_accept",
                    speaker = Text.translatable("npc.cresora.spirit_guide.name"),
                    body = Text.translatable("npc.cresora.demo.dialogue.line_accept"),
                    choices = null,
                    flagsToSet = listOf("met_guide"),
                    conditionFlags = null,
                    nextNodeId = "farewell"
                ),
                NpcDialogueNode(
                    id = "after_decline",
                    speaker = Text.translatable("npc.cresora.spirit_guide.name"),
                    body = Text.translatable("npc.cresora.demo.dialogue.line_decline"),
                    choices = null,
                    flagsToSet = null,
                    conditionFlags = null,
                    nextNodeId = "farewell"
                ),
                NpcDialogueNode(
                    id = "farewell",
                    speaker = null,
                    body = Text.translatable("npc.cresora.demo.dialogue.line_farewell"),
                    choices = null,
                    flagsToSet = null,
                    conditionFlags = null,
                    nextNodeId = null
                )
            )
        )
        trees[tree.id] = tree
        for (node in tree.nodes) {
            nodeIndex[tree.id + ":" + node.id] = node
        }
    }
}
