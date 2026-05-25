package hifumi.cresora.musicecho
import com.google.gson.JsonParser
import com.mojang.serialization.Codec
import com.mojang.serialization.JsonOps
import com.mojang.serialization.codecs.RecordCodecBuilder
import org.slf4j.LoggerFactory
import java.io.InputStreamReader
import hifumi.cresora.CreSoraUtilities

data class MusicEchoDefinition(
    val id: String,
    val nameKey: String,
    val mobDamageTakenMultiplier: Double = 1.0,
    val enabled: Boolean = true
) {
    companion object {
        val CODEC: Codec<MusicEchoDefinition> = RecordCodecBuilder.create { instance ->
            instance.group(
                Codec.STRING.fieldOf("id").forGetter(MusicEchoDefinition::id),
                Codec.STRING.fieldOf("nameKey").forGetter(MusicEchoDefinition::nameKey),
                Codec.DOUBLE.optionalFieldOf("mobDamageTakenMultiplier", 1.0).forGetter(MusicEchoDefinition::mobDamageTakenMultiplier),
                Codec.BOOL.optionalFieldOf("enabled", true).forGetter(MusicEchoDefinition::enabled)
            ).apply(instance, ::MusicEchoDefinition)
        }
    }
}

data class MusicEchoContentBundle(
    val activeEchoIds: List<String>,
    val echoes: List<MusicEchoDefinition>
) {
    companion object {
        val CODEC: Codec<MusicEchoContentBundle> = RecordCodecBuilder.create { instance ->
            instance.group(
                Codec.STRING.listOf().fieldOf("activeEchoIds").forGetter(MusicEchoContentBundle::activeEchoIds),
                MusicEchoDefinition.CODEC.listOf().fieldOf("echoes").forGetter(MusicEchoContentBundle::echoes)
            ).apply(instance, ::MusicEchoContentBundle)
        }
    }
}

object MusicEchoContentRegistry {
    private const val CONTENT_RESOURCE = "data/cresora-utilities/cresora/music_echo_content.json"

    private val logger = LoggerFactory.getLogger("${CreSoraUtilities.MOD_ID}/music-echo-content")

    @Volatile
    private var echoes: Map<String, MusicEchoDefinition> = emptyMap()

    @Volatile
    private var activeEchoIds: List<String> = emptyList()

    fun init() {
        applyBundle(defaultBundle())
        runCatching { loadBundledContent() }
            .onSuccess { bundle ->
                applyBundle(bundle)
                logger.info("Loaded music echo content from {}", CONTENT_RESOURCE)
            }
            .onFailure { throwable ->
                logger.error("Failed to load music echo content from {}. Using built-in defaults.", CONTENT_RESOURCE, throwable)
            }
    }

    fun activeEchoes(): List<MusicEchoDefinition> = activeEchoIds.mapNotNull(echoes::get)

    fun mobDamageTakenMultiplier(): Double {
        return activeEchoes()
            .asSequence()
            .filter(MusicEchoDefinition::enabled)
            .fold(1.0) { multiplier, echo -> multiplier * echo.mobDamageTakenMultiplier }
    }

    private fun loadBundledContent(): MusicEchoContentBundle {
        val stream = MusicEchoContentRegistry::class.java.classLoader.getResourceAsStream(CONTENT_RESOURCE)
            ?: error("Missing resource: $CONTENT_RESOURCE")
        InputStreamReader(stream).use { reader ->
            val json = JsonParser.parseReader(reader)
            return MusicEchoContentBundle.CODEC.parse(JsonOps.INSTANCE, json)
                .getOrThrow { message -> IllegalArgumentException("Invalid music echo content: $message") }
        }
    }

    private fun applyBundle(bundle: MusicEchoContentBundle) {
        val echoMap = bundle.echoes.associateBy(MusicEchoDefinition::id)
        require(echoMap.size == bundle.echoes.size) { "Duplicate music echo ids found in content bundle" }
        require(bundle.activeEchoIds.isNotEmpty()) { "Music echo content must declare at least one active echo id" }
        for (echoId in bundle.activeEchoIds) {
            require(echoMap.containsKey(echoId)) { "Unknown music echo '$echoId' referenced by activeEchoIds" }
        }
        for (echo in echoMap.values) {
            require(echo.mobDamageTakenMultiplier >= 0.0) {
                "Music echo '${echo.id}' has negative mobDamageTakenMultiplier"
            }
        }
        echoes = echoMap
        activeEchoIds = bundle.activeEchoIds
    }

    private fun defaultBundle(): MusicEchoContentBundle {
        return MusicEchoContentBundle(
            activeEchoIds = listOf("v1_3_2_music_echo"),
            echoes = listOf(
                MusicEchoDefinition(
                    id = "v1_3_2_music_echo",
                    nameKey = "music_echo.cresora.v1_3_2",
                    mobDamageTakenMultiplier = 0.97,
                    enabled = true
                )
            )
        )
    }
}
