package hifumi.cresora.equipment
import hifumi.cresora.CreSoraUtilities
import com.google.gson.JsonParser
import com.mojang.serialization.Codec
import com.mojang.serialization.JsonOps
import com.mojang.serialization.codecs.RecordCodecBuilder
import org.slf4j.LoggerFactory
import java.io.InputStreamReader

data class ArtifactSpecialMobDrop(
    val rankMin: Int,
    val rankMax: Int,
    val maxChance: Double
) {
    companion object {
        val CODEC: Codec<ArtifactSpecialMobDrop> = RecordCodecBuilder.create { instance ->
            instance.group(
                Codec.INT.fieldOf("rankMin").forGetter(ArtifactSpecialMobDrop::rankMin),
                Codec.INT.fieldOf("rankMax").forGetter(ArtifactSpecialMobDrop::rankMax),
                Codec.DOUBLE.fieldOf("maxChance").forGetter(ArtifactSpecialMobDrop::maxChance)
            ).apply(instance, ::ArtifactSpecialMobDrop)
        }
    }
}

data class ArtifactSpecialItemDefinition(
    val id: String,
    val kind: ArtifactSpecialItemKind,
    val translationKeyId: String,
    val baseItemId: String,
    val shopPrice: Int,
    val shopVisible: Boolean = true,
    val mobDrop: ArtifactSpecialMobDrop? = null
) {
    companion object {
        val CODEC: Codec<ArtifactSpecialItemDefinition> = RecordCodecBuilder.create { instance ->
            instance.group(
                Codec.STRING.fieldOf("id").forGetter(ArtifactSpecialItemDefinition::id),
                ArtifactSpecialItemKind.CODEC.fieldOf("kind").forGetter(ArtifactSpecialItemDefinition::kind),
                Codec.STRING.fieldOf("translationKey").forGetter(ArtifactSpecialItemDefinition::translationKeyId),
                Codec.STRING.fieldOf("baseItemId").forGetter(ArtifactSpecialItemDefinition::baseItemId),
                Codec.INT.fieldOf("shopPrice").forGetter(ArtifactSpecialItemDefinition::shopPrice),
                Codec.BOOL.optionalFieldOf("shopVisible", true).forGetter(ArtifactSpecialItemDefinition::shopVisible),
                ArtifactSpecialMobDrop.CODEC.optionalFieldOf("mobDrop")
                    .forGetter { java.util.Optional.ofNullable(it.mobDrop) }
            ).apply(instance) { id, kind, translationKeyId, baseItemId, shopPrice, shopVisible, mobDrop ->
                ArtifactSpecialItemDefinition(id, kind, translationKeyId, baseItemId, shopPrice, shopVisible, mobDrop.orElse(null))
            }
        }
    }
}

data class ArtifactSpecialItemBundle(
    val definitions: List<ArtifactSpecialItemDefinition>
) {
    companion object {
        val CODEC: Codec<ArtifactSpecialItemBundle> = RecordCodecBuilder.create { instance ->
            instance.group(
                ArtifactSpecialItemDefinition.CODEC.listOf().fieldOf("definitions").forGetter(ArtifactSpecialItemBundle::definitions)
            ).apply(instance, ::ArtifactSpecialItemBundle)
        }
    }
}

object ArtifactSpecialItemRegistry {
    private const val CONTENT_RESOURCE = "data/cresora-utilities/cresora/artifact_special_items.json"

    private val logger = LoggerFactory.getLogger("${CreSoraUtilities.MOD_ID}/artifact-special-items")

    @Volatile
    private var definitions: Map<String, ArtifactSpecialItemDefinition> = emptyMap()

    fun init() {
        applyBundle(defaultBundle())
        runCatching { loadBundledContent() }
            .onSuccess { bundle ->
                applyBundle(bundle)
                logger.info("Loaded artifact special item content from {}", CONTENT_RESOURCE)
            }
            .onFailure { throwable ->
                logger.error("Failed to load artifact special item content from {}. Using built-in defaults.", CONTENT_RESOURCE, throwable)
            }
    }

    fun definition(id: String): ArtifactSpecialItemDefinition {
        return definitions[id] ?: error("Unknown artifact special item definition: $id")
    }

    fun definitions(): List<ArtifactSpecialItemDefinition> = definitions.values.sortedBy(ArtifactSpecialItemDefinition::id)

    fun shopDefinitions(): List<ArtifactSpecialItemDefinition> {
        return definitions.values.filter(ArtifactSpecialItemDefinition::shopVisible).sortedBy(ArtifactSpecialItemDefinition::shopPrice)
    }

    fun definitionsWithMobDrop(): List<ArtifactSpecialItemDefinition> {
        return definitions.values.filter { it.mobDrop != null }.sortedBy(ArtifactSpecialItemDefinition::id)
    }

    fun definitionForKind(kind: ArtifactSpecialItemKind): ArtifactSpecialItemDefinition? {
        return definitions.values.firstOrNull { it.kind == kind }
    }

    private fun loadBundledContent(): ArtifactSpecialItemBundle {
        val stream = ArtifactSpecialItemRegistry::class.java.classLoader.getResourceAsStream(CONTENT_RESOURCE)
            ?: error("Missing resource: $CONTENT_RESOURCE")
        InputStreamReader(stream).use { reader ->
            val json = JsonParser.parseReader(reader)
            return ArtifactSpecialItemBundle.CODEC.parse(JsonOps.INSTANCE, json)
                .getOrThrow { message -> IllegalArgumentException("Invalid artifact special item content: $message") }
        }
    }

    private fun applyBundle(bundle: ArtifactSpecialItemBundle) {
        val definitionMap = bundle.definitions.associateBy(ArtifactSpecialItemDefinition::id)
        require(definitionMap.size == bundle.definitions.size) { "Duplicate artifact special item ids found in content bundle" }
        for (definition in definitionMap.values) {
            require(definition.shopPrice >= 0) { "Artifact special item '${definition.id}' has negative shop price" }
            definition.mobDrop?.let { mobDrop ->
                require(mobDrop.rankMax >= mobDrop.rankMin) { "Artifact special item '${definition.id}' has invalid mob-drop rank range" }
                require(mobDrop.maxChance >= 0.0) { "Artifact special item '${definition.id}' has negative mob-drop chance" }
            }
        }
        definitions = definitionMap
    }

    private fun defaultBundle(): ArtifactSpecialItemBundle {
        return ArtifactSpecialItemBundle(
            definitions = listOf(
                ArtifactSpecialItemDefinition(
                    id = "zankyo_kanata_alpha",
                    kind = ArtifactSpecialItemKind.ALPHA,
                    translationKeyId = "item.cresora-utilities.zankyo_kanata_alpha",
                    baseItemId = "minecraft:echo_shard",
                    shopPrice = 50_000
                ),
                ArtifactSpecialItemDefinition(
                    id = "zankyo_kanata_beta",
                    kind = ArtifactSpecialItemKind.BETA,
                    translationKeyId = "item.cresora-utilities.zankyo_kanata_beta",
                    baseItemId = "minecraft:nether_star",
                    shopPrice = 2_000_000,
                    mobDrop = ArtifactSpecialMobDrop(
                        rankMin = 45,
                        rankMax = 70,
                        maxChance = 0.10
                    )
                ),
                ArtifactSpecialItemDefinition(
                    id = "blood_note",
                    kind = ArtifactSpecialItemKind.NOTE,
                    translationKeyId = "item.cresora-utilities.blood_note",
                    baseItemId = "minecraft:amethyst_shard",
                    shopPrice = 0,
                    shopVisible = false,
                    mobDrop = ArtifactSpecialMobDrop(
                        rankMin = 1,
                        rankMax = 1,
                        maxChance = 0.005
                    )
                )
            )
        )
    }
}
