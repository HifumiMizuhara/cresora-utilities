package hifumi.cresora

import com.google.gson.JsonParser
import com.mojang.serialization.Codec
import com.mojang.serialization.JsonOps
import com.mojang.serialization.codecs.RecordCodecBuilder
import org.slf4j.LoggerFactory
import java.io.InputStreamReader

data class DomainRarityWeights(
    val threeStar: Double,
    val fourStar: Double,
    val fiveStar: Double
) {
    companion object {
        val CODEC: Codec<DomainRarityWeights> = RecordCodecBuilder.create { instance ->
            instance.group(
                Codec.DOUBLE.fieldOf("threeStar").forGetter(DomainRarityWeights::threeStar),
                Codec.DOUBLE.fieldOf("fourStar").forGetter(DomainRarityWeights::fourStar),
                Codec.DOUBLE.fieldOf("fiveStar").forGetter(DomainRarityWeights::fiveStar)
            ).apply(instance, ::DomainRarityWeights)
        }
    }

    fun roll(random: net.minecraft.util.math.random.Random): EquipmentRarity {
        val total = (threeStar + fourStar + fiveStar).coerceAtLeast(0.0)
        if (total <= 0.0) {
            return EquipmentRarity.THREE_STAR
        }
        var roll = random.nextDouble() * total
        roll -= threeStar.coerceAtLeast(0.0)
        if (roll <= 0.0) {
            return EquipmentRarity.THREE_STAR
        }
        roll -= fourStar.coerceAtLeast(0.0)
        if (roll <= 0.0) {
            return EquipmentRarity.FOUR_STAR
        }
        return EquipmentRarity.FIVE_STAR
    }
}

data class DomainArtifactRewardDefinition(
    val setId: String,
    val minCount: Int,
    val maxCount: Int,
    val minLevel: Int,
    val maxLevel: Int,
    val rarityWeights: DomainRarityWeights
) {
    companion object {
        val CODEC: Codec<DomainArtifactRewardDefinition> = RecordCodecBuilder.create { instance ->
            instance.group(
                Codec.STRING.fieldOf("setId").forGetter(DomainArtifactRewardDefinition::setId),
                Codec.INT.fieldOf("minCount").forGetter(DomainArtifactRewardDefinition::minCount),
                Codec.INT.fieldOf("maxCount").forGetter(DomainArtifactRewardDefinition::maxCount),
                Codec.INT.fieldOf("minLevel").forGetter(DomainArtifactRewardDefinition::minLevel),
                Codec.INT.fieldOf("maxLevel").forGetter(DomainArtifactRewardDefinition::maxLevel),
                DomainRarityWeights.CODEC.fieldOf("rarityWeights").forGetter(DomainArtifactRewardDefinition::rarityWeights)
            ).apply(instance, ::DomainArtifactRewardDefinition)
        }
    }
}

data class DomainWeaponFragmentRewardDefinition(
    val weaponId: String,
    val minCount: Int,
    val maxCount: Int
) {
    companion object {
        val CODEC: Codec<DomainWeaponFragmentRewardDefinition> = RecordCodecBuilder.create { instance ->
            instance.group(
                Codec.STRING.fieldOf("weaponId").forGetter(DomainWeaponFragmentRewardDefinition::weaponId),
                Codec.INT.fieldOf("minCount").forGetter(DomainWeaponFragmentRewardDefinition::minCount),
                Codec.INT.fieldOf("maxCount").forGetter(DomainWeaponFragmentRewardDefinition::maxCount)
            ).apply(instance, ::DomainWeaponFragmentRewardDefinition)
        }
    }
}

data class DomainCurrencyRewardDefinition(
    val creditsBase: Int,
    val creditsPerRank: Int,
    val rankXpBase: Int,
    val rankXpPerRank: Int
) {
    companion object {
        val CODEC: Codec<DomainCurrencyRewardDefinition> = RecordCodecBuilder.create { instance ->
            instance.group(
                Codec.INT.fieldOf("creditsBase").forGetter(DomainCurrencyRewardDefinition::creditsBase),
                Codec.INT.fieldOf("creditsPerRank").forGetter(DomainCurrencyRewardDefinition::creditsPerRank),
                Codec.INT.fieldOf("rankXpBase").forGetter(DomainCurrencyRewardDefinition::rankXpBase),
                Codec.INT.fieldOf("rankXpPerRank").forGetter(DomainCurrencyRewardDefinition::rankXpPerRank)
            ).apply(instance, ::DomainCurrencyRewardDefinition)
        }
    }
}

data class DomainRewardProfile(
    val id: String,
    val artifactReward: DomainArtifactRewardDefinition? = null,
    val weaponFragmentReward: DomainWeaponFragmentRewardDefinition? = null,
    val currencyReward: DomainCurrencyRewardDefinition
) {
    companion object {
        val CODEC: Codec<DomainRewardProfile> = RecordCodecBuilder.create { instance ->
            instance.group(
                Codec.STRING.fieldOf("id").forGetter(DomainRewardProfile::id),
                DomainArtifactRewardDefinition.CODEC.optionalFieldOf("artifactReward")
                    .forGetter { java.util.Optional.ofNullable(it.artifactReward) },
                DomainWeaponFragmentRewardDefinition.CODEC.optionalFieldOf("weaponFragmentReward")
                    .forGetter { java.util.Optional.ofNullable(it.weaponFragmentReward) },
                DomainCurrencyRewardDefinition.CODEC.fieldOf("currencyReward").forGetter(DomainRewardProfile::currencyReward)
            ).apply(instance) { id, artifactReward, weaponReward, currencyReward ->
                DomainRewardProfile(id, artifactReward.orElse(null), weaponReward.orElse(null), currencyReward)
            }
        }
    }
}

data class DomainRewardContentBundle(
    val rewardProfiles: List<DomainRewardProfile>
) {
    companion object {
        val CODEC: Codec<DomainRewardContentBundle> = RecordCodecBuilder.create { instance ->
            instance.group(
                DomainRewardProfile.CODEC.listOf().fieldOf("rewardProfiles").forGetter(DomainRewardContentBundle::rewardProfiles)
            ).apply(instance, ::DomainRewardContentBundle)
        }
    }
}

object DomainRewardProfileRegistry {
    private const val CONTENT_RESOURCE = "data/cresora-utilities/cresora/domain_reward_profiles.json"

    private val logger = LoggerFactory.getLogger("${CreSoraUtilities.MOD_ID}/domain-reward-content")

    @Volatile
    private var profiles: Map<String, DomainRewardProfile> = emptyMap()

    fun init() {
        applyBundle(defaultBundle())
        runCatching { loadBundledContent() }
            .onSuccess { bundle ->
                applyBundle(bundle)
                logger.info("Loaded domain reward profiles from {}", CONTENT_RESOURCE)
            }
            .onFailure { throwable ->
                logger.error("Failed to load domain reward profiles from {}. Using built-in defaults.", CONTENT_RESOURCE, throwable)
            }
    }

    fun profiles(): List<DomainRewardProfile> = profiles.values.sortedBy(DomainRewardProfile::id)

    fun requireProfile(id: String): DomainRewardProfile {
        return profiles[id] ?: error("Unknown domain reward profile: $id")
    }

    private fun loadBundledContent(): DomainRewardContentBundle {
        val stream = DomainRewardProfileRegistry::class.java.classLoader.getResourceAsStream(CONTENT_RESOURCE)
            ?: error("Missing resource: $CONTENT_RESOURCE")
        InputStreamReader(stream).use { reader ->
            val json = JsonParser.parseReader(reader)
            return DomainRewardContentBundle.CODEC.parse(JsonOps.INSTANCE, json)
                .getOrThrow { message -> IllegalArgumentException("Invalid domain reward content: $message") }
        }
    }

    private fun applyBundle(bundle: DomainRewardContentBundle) {
        val profileMap = bundle.rewardProfiles.associateBy(DomainRewardProfile::id)
        require(profileMap.size == bundle.rewardProfiles.size) { "Duplicate domain reward profile ids found in content bundle" }
        for (profile in profileMap.values) {
            profile.artifactReward?.let { artifact ->
                require(artifact.minCount >= 0 && artifact.maxCount >= artifact.minCount) {
                    "Invalid artifact count range in domain reward profile '${profile.id}'"
                }
                require(artifact.minLevel >= 0 && artifact.maxLevel >= artifact.minLevel) {
                    "Invalid artifact level range in domain reward profile '${profile.id}'"
                }
            }
            profile.weaponFragmentReward?.let { weapon ->
                require(weapon.minCount >= 0 && weapon.maxCount >= weapon.minCount) {
                    "Invalid weapon fragment count range in domain reward profile '${profile.id}'"
                }
            }
        }
        profiles = profileMap
    }

    private fun defaultBundle(): DomainRewardContentBundle {
        return DomainRewardContentBundle(
            rewardProfiles = listOf(
                DomainRewardProfile(
                    id = "hinagata_hunt",
                    artifactReward = DomainArtifactRewardDefinition(
                        setId = "hinagata",
                        minCount = 1,
                        maxCount = 2,
                        minLevel = 0,
                        maxLevel = 16,
                        rarityWeights = DomainRarityWeights(0.40, 0.42, 0.18)
                    ),
                    currencyReward = DomainCurrencyRewardDefinition(
                        creditsBase = 6_000,
                        creditsPerRank = 160,
                        rankXpBase = 120,
                        rankXpPerRank = 5
                    )
                ),
                DomainRewardProfile(
                    id = "rondo_forge",
                    weaponFragmentReward = DomainWeaponFragmentRewardDefinition(
                        weaponId = "rondo_melody",
                        minCount = 2,
                        maxCount = 8
                    ),
                    currencyReward = DomainCurrencyRewardDefinition(
                        creditsBase = 7_500,
                        creditsPerRank = 190,
                        rankXpBase = 150,
                        rankXpPerRank = 6
                    )
                ),
                DomainRewardProfile(
                    id = "masquerade_soiree",
                    weaponFragmentReward = DomainWeaponFragmentRewardDefinition(
                        weaponId = "masquerade_invitation",
                        minCount = 2,
                        maxCount = 8
                    ),
                    currencyReward = DomainCurrencyRewardDefinition(
                        creditsBase = 7_500,
                        creditsPerRank = 190,
                        rankXpBase = 150,
                        rankXpPerRank = 6
                    )
                ),
                DomainRewardProfile(
                    id = "csc_training",
                    currencyReward = DomainCurrencyRewardDefinition(
                        creditsBase = 11_000,
                        creditsPerRank = 250,
                        rankXpBase = 220,
                        rankXpPerRank = 8
                    )
                )
            )
        )
    }
}
