package hifumi.cresora

import com.google.gson.JsonParser
import com.mojang.serialization.Codec
import com.mojang.serialization.JsonOps
import com.mojang.serialization.codecs.RecordCodecBuilder
import org.slf4j.LoggerFactory
import java.io.InputStreamReader

enum class ResonanceBannerType(
    val id: String
) {
    LIMITED("limited"),
    STANDARD("standard");

    companion object {
        val CODEC: Codec<ResonanceBannerType> = Codec.STRING.xmap(
            { value -> entries.firstOrNull { it.id == value } ?: error("Unknown resonance banner type: $value") },
            ResonanceBannerType::id
        )
    }
}

data class ResonanceRateTable(
    val fiveStarChance: Double,
    val fourStarChance: Double,
    val threeStarChance: Double,
    val twoStarChance: Double
) {
    companion object {
        val CODEC: Codec<ResonanceRateTable> = RecordCodecBuilder.create { instance ->
            instance.group(
                Codec.DOUBLE.fieldOf("fiveStarChance").forGetter(ResonanceRateTable::fiveStarChance),
                Codec.DOUBLE.fieldOf("fourStarChance").forGetter(ResonanceRateTable::fourStarChance),
                Codec.DOUBLE.fieldOf("threeStarChance").forGetter(ResonanceRateTable::threeStarChance),
                Codec.DOUBLE.fieldOf("twoStarChance").forGetter(ResonanceRateTable::twoStarChance)
            ).apply(instance, ::ResonanceRateTable)
        }
    }
}

data class ResonanceWeaponEntry(
    val weaponId: String,
    val rarity: WeaponRarity,
    val weight: Double = 1.0
) {
    companion object {
        val CODEC: Codec<ResonanceWeaponEntry> = RecordCodecBuilder.create { instance ->
            instance.group(
                Codec.STRING.fieldOf("weaponId").forGetter(ResonanceWeaponEntry::weaponId),
                WeaponRarity.CODEC.fieldOf("rarity").forGetter(ResonanceWeaponEntry::rarity),
                Codec.DOUBLE.optionalFieldOf("weight", 1.0).forGetter(ResonanceWeaponEntry::weight)
            ).apply(instance, ::ResonanceWeaponEntry)
        }
    }
}

data class ResonanceBannerDefinition(
    val id: String,
    val type: ResonanceBannerType,
    val familyId: String,
    val translationKey: String,
    val currencyItemId: String,
    val cost: Int,
    val featuredFiveStarWeaponId: String? = null,
    val rates: ResonanceRateTable,
    val fiveStarPool: List<ResonanceWeaponEntry>,
    val fourStarPool: List<ResonanceWeaponEntry>,
    val threeStarPool: List<ResonanceWeaponEntry>,
    val twoStarPool: List<ResonanceWeaponEntry>
) {
    fun currencyType(): ResonanceCurrencyType = ResonanceCurrencyType.byId(currencyItemId)

    companion object {
        val CODEC: Codec<ResonanceBannerDefinition> = RecordCodecBuilder.create { instance ->
            instance.group(
                Codec.STRING.fieldOf("id").forGetter(ResonanceBannerDefinition::id),
                ResonanceBannerType.CODEC.fieldOf("type").forGetter(ResonanceBannerDefinition::type),
                Codec.STRING.fieldOf("familyId").forGetter(ResonanceBannerDefinition::familyId),
                Codec.STRING.fieldOf("translationKey").forGetter(ResonanceBannerDefinition::translationKey),
                Codec.STRING.fieldOf("currencyItemId").forGetter(ResonanceBannerDefinition::currencyItemId),
                Codec.INT.fieldOf("cost").forGetter(ResonanceBannerDefinition::cost),
                Codec.STRING.optionalFieldOf("featuredFiveStarWeaponId")
                    .forGetter { java.util.Optional.ofNullable(it.featuredFiveStarWeaponId) },
                ResonanceRateTable.CODEC.fieldOf("rates").forGetter(ResonanceBannerDefinition::rates),
                ResonanceWeaponEntry.CODEC.listOf().fieldOf("fiveStarPool").forGetter(ResonanceBannerDefinition::fiveStarPool),
                ResonanceWeaponEntry.CODEC.listOf().fieldOf("fourStarPool").forGetter(ResonanceBannerDefinition::fourStarPool),
                ResonanceWeaponEntry.CODEC.listOf().fieldOf("threeStarPool").forGetter(ResonanceBannerDefinition::threeStarPool),
                ResonanceWeaponEntry.CODEC.listOf().fieldOf("twoStarPool").forGetter(ResonanceBannerDefinition::twoStarPool)
            ).apply(instance) { id, type, familyId, translationKey, currencyItemId, cost, featured, rates, five, four, three, two ->
                ResonanceBannerDefinition(
                    id = id,
                    type = type,
                    familyId = familyId,
                    translationKey = translationKey,
                    currencyItemId = currencyItemId,
                    cost = cost,
                    featuredFiveStarWeaponId = featured.orElse(null),
                    rates = rates,
                    fiveStarPool = five,
                    fourStarPool = four,
                    threeStarPool = three,
                    twoStarPool = two
                )
            }
        }
    }
}

data class ResonanceContentBundle(
    val banners: List<ResonanceBannerDefinition>
) {
    companion object {
        val CODEC: Codec<ResonanceContentBundle> = RecordCodecBuilder.create { instance ->
            instance.group(
                ResonanceBannerDefinition.CODEC.listOf().fieldOf("banners").forGetter(ResonanceContentBundle::banners)
            ).apply(instance, ::ResonanceContentBundle)
        }
    }
}

object ResonanceContentRegistry {
    private const val CONTENT_RESOURCE = "data/cresora-utilities/cresora/resonance_content.json"

    private val logger = LoggerFactory.getLogger("${CreSoraUtilities.MOD_ID}/resonance-content")

    @Volatile
    private var banners: Map<String, ResonanceBannerDefinition> = emptyMap()

    fun init() {
        applyBundle(defaultBundle())
        runCatching { loadBundledContent() }
            .onSuccess { bundle ->
                applyBundle(bundle)
                logger.info("Loaded resonance content from {}", CONTENT_RESOURCE)
            }
            .onFailure { throwable ->
                logger.error("Failed to load resonance content from {}. Using built-in defaults.", CONTENT_RESOURCE, throwable)
            }
    }

    fun banners(): List<ResonanceBannerDefinition> = banners.values.sortedBy(ResonanceBannerDefinition::id)

    fun banner(id: String): ResonanceBannerDefinition = banners[id] ?: error("Unknown resonance banner: $id")

    fun limitedBanners(): List<ResonanceBannerDefinition> = banners().filter { it.type == ResonanceBannerType.LIMITED }

    private fun loadBundledContent(): ResonanceContentBundle {
        val stream = ResonanceContentRegistry::class.java.classLoader.getResourceAsStream(CONTENT_RESOURCE)
            ?: error("Missing resource: $CONTENT_RESOURCE")
        InputStreamReader(stream).use { reader ->
            val json = JsonParser.parseReader(reader)
            return ResonanceContentBundle.CODEC.parse(JsonOps.INSTANCE, json)
                .getOrThrow { message -> IllegalArgumentException("Invalid resonance content: $message") }
        }
    }

    private fun applyBundle(bundle: ResonanceContentBundle) {
        val bannerMap = bundle.banners.associateBy(ResonanceBannerDefinition::id)
        require(bannerMap.size == bundle.banners.size) { "Duplicate resonance banner ids found in content bundle" }
        for (banner in bannerMap.values) {
            require(banner.cost > 0) { "Resonance banner '${banner.id}' must have positive cost" }
            require(banner.rates.fiveStarChance >= 0.0 && banner.rates.fourStarChance >= 0.0) {
                "Resonance banner '${banner.id}' has negative rates"
            }
            require(banner.rates.threeStarChance >= 0.0 && banner.rates.twoStarChance >= 0.0) {
                "Resonance banner '${banner.id}' has negative rates"
            }
            require(banner.fiveStarPool.isNotEmpty()) { "Resonance banner '${banner.id}' needs a five-star pool" }
            require(banner.fourStarPool.isNotEmpty()) { "Resonance banner '${banner.id}' needs a four-star pool" }
            require(banner.threeStarPool.isNotEmpty()) { "Resonance banner '${banner.id}' needs a three-star pool" }
            require(banner.twoStarPool.isNotEmpty()) { "Resonance banner '${banner.id}' needs a two-star pool" }
        }
        banners = bannerMap
    }

    private fun defaultBundle(): ResonanceContentBundle {
        return ResonanceContentBundle(
            banners = listOf(
                ResonanceBannerDefinition(
                    id = "limited_lakeside",
                    type = ResonanceBannerType.LIMITED,
                    familyId = "minor_1_3",
                    translationKey = "screen.cresora.resonance.banner.limited_lakeside",
                    currencyItemId = "chord_progression",
                    cost = 325,
                    featuredFiveStarWeaponId = "lakeside_stride",
                    rates = defaultRates(),
                    fiveStarPool = listOf(
                        ResonanceWeaponEntry("lakeside_stride", WeaponRarity.FIVE_STAR, 1.0)
                    ),
                    fourStarPool = standardFourStarPool(),
                    threeStarPool = standardThreeStarPool(),
                    twoStarPool = standardTwoStarPool()
                ),
                ResonanceBannerDefinition(
                    id = "standard_ensemble",
                    type = ResonanceBannerType.STANDARD,
                    familyId = "standard",
                    translationKey = "screen.cresora.resonance.banner.standard_ensemble",
                    currencyItemId = "substitute_chord",
                    cost = 325,
                    rates = defaultRates(),
                    fiveStarPool = listOf(
                        ResonanceWeaponEntry("rondo_melody", WeaponRarity.FIVE_STAR, 1.0),
                        ResonanceWeaponEntry("masquerade_invitation", WeaponRarity.FIVE_STAR, 1.0),
                        ResonanceWeaponEntry("hanwu_juanxue", WeaponRarity.FIVE_STAR, 1.0),
                        ResonanceWeaponEntry("kyokusui_no_ryusho", WeaponRarity.FIVE_STAR, 1.0)
                    ),
                    fourStarPool = standardFourStarPool(),
                    threeStarPool = standardThreeStarPool(),
                    twoStarPool = standardTwoStarPool()
                )
            )
        )
    }

    private fun defaultRates(): ResonanceRateTable {
        return ResonanceRateTable(
            fiveStarChance = 0.0100,
            fourStarChance = 0.0325,
            threeStarChance = 0.0799,
            twoStarChance = 0.8776
        )
    }

    private fun standardFourStarPool(): List<ResonanceWeaponEntry> {
        return listOf(
            ResonanceWeaponEntry("requiem_toward_dawn", WeaponRarity.FOUR_STAR, 1.0),
            ResonanceWeaponEntry("gaoshan_liushui", WeaponRarity.FOUR_STAR, 1.0),
            ResonanceWeaponEntry("cadenza_allegro", WeaponRarity.FOUR_STAR, 1.0)
        )
    }

    private fun standardThreeStarPool(): List<ResonanceWeaponEntry> {
        return listOf(
            ResonanceWeaponEntry("rondo_melody", WeaponRarity.THREE_STAR, 1.0),
            ResonanceWeaponEntry("masquerade_invitation", WeaponRarity.THREE_STAR, 1.0),
            ResonanceWeaponEntry("pastoral_flute_reverie", WeaponRarity.THREE_STAR, 1.0)
        )
    }

    private fun standardTwoStarPool(): List<ResonanceWeaponEntry> {
        return listOf(
            ResonanceWeaponEntry("rondo_melody", WeaponRarity.TWO_STAR, 1.0),
            ResonanceWeaponEntry("masquerade_invitation", WeaponRarity.TWO_STAR, 1.0)
        )
    }
}
