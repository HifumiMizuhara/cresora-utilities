package hifumi.cresora.world

import com.google.gson.JsonParser
import com.mojang.serialization.Codec
import com.mojang.serialization.JsonOps
import com.mojang.serialization.codecs.RecordCodecBuilder
import hifumi.cresora.CreSoraUtilities
import org.slf4j.LoggerFactory
import java.io.InputStreamReader

/**
 * Axis-aligned rectangle in block coords. Bounds are half-open: `min` is inclusive,
 * `max` is exclusive. This lets adjacent regions chain (`max == next.min`) without
 * overlapping on the boundary. Overlap is validated at load time in [RegionContentRegistry.applyBundle].
 */
data class RegionBox(
    val minX: Int,
    val minZ: Int,
    val maxX: Int,
    val maxZ: Int
) {
    fun contains(x: Int, z: Int): Boolean = x >= minX && x < maxX && z >= minZ && z < maxZ

    fun overlaps(other: RegionBox): Boolean =
        minX < other.maxX && other.minX < maxX && minZ < other.maxZ && other.minZ < maxZ

    companion object {
        val CODEC: Codec<RegionBox> = RecordCodecBuilder.create { instance ->
            instance.group(
                Codec.INT.fieldOf("minX").forGetter(RegionBox::minX),
                Codec.INT.fieldOf("minZ").forGetter(RegionBox::minZ),
                Codec.INT.fieldOf("maxX").forGetter(RegionBox::maxX),
                Codec.INT.fieldOf("maxZ").forGetter(RegionBox::maxZ)
            ).apply(instance, ::RegionBox)
        }
    }
}

data class RegionDefinition(
    val id: String,
    val nameKey: String,
    val descriptionKey: String,
    val box: RegionBox,
    val unlockRank: Int,
    val biomeHint: String? = null
) {
    companion object {
        val CODEC: Codec<RegionDefinition> = RecordCodecBuilder.create { instance ->
            instance.group(
                Codec.STRING.fieldOf("id").forGetter(RegionDefinition::id),
                Codec.STRING.fieldOf("nameKey").forGetter(RegionDefinition::nameKey),
                Codec.STRING.fieldOf("descriptionKey").forGetter(RegionDefinition::descriptionKey),
                RegionBox.CODEC.fieldOf("box").forGetter(RegionDefinition::box),
                Codec.INT.optionalFieldOf("unlockRank", 0).forGetter(RegionDefinition::unlockRank),
                Codec.STRING.optionalFieldOf("biomeHint").forGetter { java.util.Optional.ofNullable(it.biomeHint) }
            ).apply(instance) { id, nameKey, descriptionKey, box, unlockRank, biomeHint ->
                RegionDefinition(id, nameKey, descriptionKey, box, unlockRank, biomeHint.orElse(null))
            }
        }
    }
}

data class RegionContentBundle(
    val regions: List<RegionDefinition>
) {
    companion object {
        val CODEC: Codec<RegionContentBundle> = RecordCodecBuilder.create { instance ->
            instance.group(
                RegionDefinition.CODEC.listOf().fieldOf("regions").forGetter(RegionContentBundle::regions)
            ).apply(instance, ::RegionContentBundle)
        }
    }
}

object RegionContentRegistry {
    private const val CONTENT_RESOURCE = "data/${CreSoraUtilities.MOD_ID}/cresora/region_content.json"
    private val logger = LoggerFactory.getLogger("${CreSoraUtilities.MOD_ID}/region-content")

    @Volatile
    private var regions: List<RegionDefinition> = emptyList()

    fun init() {
        applyBundle(defaultBundle())
        val stream = RegionContentRegistry::class.java.classLoader.getResourceAsStream(CONTENT_RESOURCE)
        if (stream != null) {
            runCatching {
                stream.use { s ->
                    InputStreamReader(s).use { reader ->
                        val json = JsonParser.parseReader(reader)
                        RegionContentBundle.CODEC.parse(JsonOps.INSTANCE, json)
                            .getOrThrow { message -> IllegalArgumentException("Invalid region content: $message") }
                    }
                }
            }.onSuccess { bundle ->
                applyBundle(bundle)
                logger.info("Loaded region content from {}", CONTENT_RESOURCE)
            }.onFailure { throwable ->
                logger.error("Failed to load region content from {}. Using built-in defaults.", CONTENT_RESOURCE, throwable)
            }
        } else {
            logger.info("Optional region content resource {} not found; using built-in defaults.", CONTENT_RESOURCE)
        }
    }

    fun regionAt(x: Int, z: Int): RegionDefinition? = regions.firstOrNull { it.box.contains(x, z) }

    fun region(id: String): RegionDefinition? = regions.firstOrNull { it.id == id }

    fun requireRegion(id: String): RegionDefinition =
        region(id) ?: error("Unknown region id referenced by content: $id")

    fun all(): List<RegionDefinition> = regions

    private fun applyBundle(bundle: RegionContentBundle) {
        val seen = HashSet<String>()
        for (region in bundle.regions) {
            require(region.id.isNotBlank()) { "Region id must not be blank" }
            require(seen.add(region.id)) { "Duplicate region id: ${region.id}" }
            require(region.nameKey.isNotBlank()) { "Region '${region.id}' must have a nameKey" }
            require(region.descriptionKey.isNotBlank()) { "Region '${region.id}' must have a descriptionKey" }
            require(region.box.maxX > region.box.minX && region.box.maxZ > region.box.minZ) {
                "Region '${region.id}' has a degenerate or inverted box ${region.box}"
            }
            require(region.unlockRank >= 0) { "Region '${region.id}' unlockRank must be non-negative" }
        }
        for (i in bundle.regions.indices) {
            for (j in i + 1 until bundle.regions.size) {
                val a = bundle.regions[i]
                val b = bundle.regions[j]
                require(!a.box.overlaps(b.box)) {
                    "Regions '${a.id}' ${a.box} and '${b.id}' ${b.box} overlap"
                }
            }
        }
        regions = bundle.regions
    }

    /**
     * Built-in 6-region skeleton. The hub is centered on the SpiritGuide landing site and is
     * unlocked from the start; the four cardinal regions gate on rising adventure ranks, and a
     * far corner rewards deep progression. World lore is injected later by editing region_content.json.
     *
     * Bounds use half-open semantics (max exclusive) so neighbors share the boundary coord cleanly.
     */
    private fun defaultBundle(): RegionContentBundle = RegionContentBundle(
        regions = listOf(
            RegionDefinition(
                id = "region_hub",
                nameKey = "region.cresora.region_hub.name",
                descriptionKey = "region.cresora.region_hub.desc",
                box = RegionBox(minX = -512, minZ = -512, maxX = 512, maxZ = 512),
                unlockRank = 0
            ),
            RegionDefinition(
                id = "region_east",
                nameKey = "region.cresora.region_east.name",
                descriptionKey = "region.cresora.region_east.desc",
                box = RegionBox(minX = 512, minZ = -512, maxX = 2048, maxZ = 512),
                unlockRank = 2
            ),
            RegionDefinition(
                id = "region_north",
                nameKey = "region.cresora.region_north.name",
                descriptionKey = "region.cresora.region_north.desc",
                box = RegionBox(minX = -512, minZ = -2048, maxX = 512, maxZ = -512),
                unlockRank = 4
            ),
            RegionDefinition(
                id = "region_west",
                nameKey = "region.cresora.region_west.name",
                descriptionKey = "region.cresora.region_west.desc",
                box = RegionBox(minX = -2048, minZ = -512, maxX = -512, maxZ = 512),
                unlockRank = 6
            ),
            RegionDefinition(
                id = "region_south",
                nameKey = "region.cresora.region_south.name",
                descriptionKey = "region.cresora.region_south.desc",
                box = RegionBox(minX = -512, minZ = 512, maxX = 512, maxZ = 2048),
                unlockRank = 8
            ),
            RegionDefinition(
                id = "region_far",
                nameKey = "region.cresora.region_far.name",
                descriptionKey = "region.cresora.region_far.desc",
                box = RegionBox(minX = 512, minZ = 512, maxX = 2048, maxZ = 2048),
                unlockRank = 10
            )
        )
    )
}
