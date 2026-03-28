package hifumi.cresora

import com.google.gson.JsonParser
import com.mojang.serialization.Codec
import com.mojang.serialization.JsonOps
import com.mojang.serialization.codecs.RecordCodecBuilder
import org.slf4j.LoggerFactory
import java.io.InputStreamReader

data class DomainMobPoolEntry(
    val entityTypeId: String,
    val weight: Int
) {
    companion object {
        val CODEC: Codec<DomainMobPoolEntry> = RecordCodecBuilder.create { instance ->
            instance.group(
                Codec.STRING.fieldOf("entityTypeId").forGetter(DomainMobPoolEntry::entityTypeId),
                Codec.INT.fieldOf("weight").forGetter(DomainMobPoolEntry::weight)
            ).apply(instance, ::DomainMobPoolEntry)
        }
    }
}

data class DomainMobPool(
    val id: String,
    val entries: List<DomainMobPoolEntry>
) {
    companion object {
        val CODEC: Codec<DomainMobPool> = RecordCodecBuilder.create { instance ->
            instance.group(
                Codec.STRING.fieldOf("id").forGetter(DomainMobPool::id),
                DomainMobPoolEntry.CODEC.listOf().fieldOf("entries").forGetter(DomainMobPool::entries)
            ).apply(instance, ::DomainMobPool)
        }
    }
}

data class DomainWaveDefinition(
    val count: Int,
    val levelOffset: Int,
    val elite: Boolean,
    val spawnDelayTicks: Int
) {
    companion object {
        val CODEC: Codec<DomainWaveDefinition> = RecordCodecBuilder.create { instance ->
            instance.group(
                Codec.INT.fieldOf("count").forGetter(DomainWaveDefinition::count),
                Codec.INT.optionalFieldOf("levelOffset", 0).forGetter(DomainWaveDefinition::levelOffset),
                Codec.BOOL.optionalFieldOf("elite", false).forGetter(DomainWaveDefinition::elite),
                Codec.INT.optionalFieldOf("spawnDelayTicks", 40).forGetter(DomainWaveDefinition::spawnDelayTicks)
            ).apply(instance, ::DomainWaveDefinition)
        }
    }
}

data class DomainStageDefinition(
    val waves: List<DomainWaveDefinition>
) {
    companion object {
        val CODEC: Codec<DomainStageDefinition> = RecordCodecBuilder.create { instance ->
            instance.group(
                DomainWaveDefinition.CODEC.listOf().fieldOf("waves").forGetter(DomainStageDefinition::waves)
            ).apply(instance, ::DomainStageDefinition)
        }
    }
}

data class DomainDefinition(
    val id: String,
    val nameKey: String,
    val themeId: String,
    val unlockRank: Int,
    val entryCostCsc: Int,
    val rewardProfileId: String,
    val mobPoolId: String,
    val stages: List<DomainStageDefinition>
) {
    companion object {
        val CODEC: Codec<DomainDefinition> = RecordCodecBuilder.create { instance ->
            instance.group(
                Codec.STRING.fieldOf("id").forGetter(DomainDefinition::id),
                Codec.STRING.fieldOf("nameKey").forGetter(DomainDefinition::nameKey),
                Codec.STRING.fieldOf("themeId").forGetter(DomainDefinition::themeId),
                Codec.INT.fieldOf("unlockRank").forGetter(DomainDefinition::unlockRank),
                Codec.INT.fieldOf("entryCostCsc").forGetter(DomainDefinition::entryCostCsc),
                Codec.STRING.fieldOf("rewardProfileId").forGetter(DomainDefinition::rewardProfileId),
                Codec.STRING.fieldOf("mobPoolId").forGetter(DomainDefinition::mobPoolId),
                DomainStageDefinition.CODEC.listOf().fieldOf("stages").forGetter(DomainDefinition::stages)
            ).apply(instance, ::DomainDefinition)
        }
    }

    fun totalWaveCount(): Int = stages.sumOf { it.waves.size }
}

data class DomainContentBundle(
    val mobPools: List<DomainMobPool>,
    val domains: List<DomainDefinition>
) {
    companion object {
        val CODEC: Codec<DomainContentBundle> = RecordCodecBuilder.create { instance ->
            instance.group(
                DomainMobPool.CODEC.listOf().fieldOf("mobPools").forGetter(DomainContentBundle::mobPools),
                DomainDefinition.CODEC.listOf().fieldOf("domains").forGetter(DomainContentBundle::domains)
            ).apply(instance, ::DomainContentBundle)
        }
    }
}

object DomainContentRegistry {
    private const val CONTENT_RESOURCE = "data/cresora-utilities/cresora/domain_content.json"

    private val logger = LoggerFactory.getLogger("${CreSoraUtilities.MOD_ID}/domain-content")

    @Volatile
    private var domains: Map<String, DomainDefinition> = emptyMap()

    @Volatile
    private var mobPools: Map<String, DomainMobPool> = emptyMap()

    fun init() {
        applyBundle(defaultBundle())
        runCatching { loadBundledContent() }
            .onSuccess { bundle ->
                applyBundle(bundle)
                logger.info("Loaded domain content from {}", CONTENT_RESOURCE)
            }
            .onFailure { throwable ->
                logger.error("Failed to load domain content from {}. Using built-in defaults.", CONTENT_RESOURCE, throwable)
            }
    }

    fun domains(): List<DomainDefinition> = domains.values.sortedBy(DomainDefinition::id)

    fun requireDomain(id: String): DomainDefinition {
        return domains[id] ?: error("Unknown domain definition: $id")
    }

    fun requireMobPool(id: String): DomainMobPool {
        return mobPools[id] ?: error("Unknown domain mob pool: $id")
    }

    private fun loadBundledContent(): DomainContentBundle {
        val stream = DomainContentRegistry::class.java.classLoader.getResourceAsStream(CONTENT_RESOURCE)
            ?: error("Missing resource: $CONTENT_RESOURCE")
        InputStreamReader(stream).use { reader ->
            val json = JsonParser.parseReader(reader)
            return DomainContentBundle.CODEC.parse(JsonOps.INSTANCE, json)
                .getOrThrow { message -> IllegalArgumentException("Invalid domain content: $message") }
        }
    }

    private fun applyBundle(bundle: DomainContentBundle) {
        val poolMap = bundle.mobPools.associateBy(DomainMobPool::id)
        val domainMap = bundle.domains.associateBy(DomainDefinition::id)
        require(poolMap.size == bundle.mobPools.size) { "Duplicate domain mob pool ids found in content bundle" }
        require(domainMap.size == bundle.domains.size) { "Duplicate domain ids found in content bundle" }

        for (pool in poolMap.values) {
            require(pool.entries.isNotEmpty()) { "Domain mob pool '${pool.id}' must not be empty" }
            for (entry in pool.entries) {
                require(entry.weight > 0) { "Domain mob pool '${pool.id}' contains non-positive weight" }
            }
        }
        for (domain in domainMap.values) {
            require(poolMap.containsKey(domain.mobPoolId)) { "Unknown mobPool '${domain.mobPoolId}' referenced by domain '${domain.id}'" }
            DomainRewardProfileRegistry.requireProfile(domain.rewardProfileId)
            require(domain.unlockRank in AdventureRankProgression.MIN_RANK..AdventureRankProgression.MAX_RANK) {
                "Invalid unlockRank on domain '${domain.id}'"
            }
            require(domain.entryCostCsc >= 0) { "Domain '${domain.id}' entryCostCsc must be >= 0" }
            require(domain.stages.isNotEmpty()) { "Domain '${domain.id}' must contain at least one stage" }
            require(domain.totalWaveCount() > 0) { "Domain '${domain.id}' must contain at least one wave" }
        }

        mobPools = poolMap
        domains = domainMap
    }

    private fun defaultBundle(): DomainContentBundle {
        return DomainContentBundle(
            mobPools = listOf(
                DomainMobPool(
                    id = "verdant_trial",
                    entries = listOf(
                        DomainMobPoolEntry("minecraft:zombie", 4),
                        DomainMobPoolEntry("minecraft:husk", 2),
                        DomainMobPoolEntry("minecraft:spider", 3)
                    )
                ),
                DomainMobPool(
                    id = "forge_trial",
                    entries = listOf(
                        DomainMobPoolEntry("minecraft:skeleton", 4),
                        DomainMobPoolEntry("minecraft:stray", 2),
                        DomainMobPoolEntry("minecraft:blaze", 3)
                    )
                ),
                DomainMobPool(
                    id = "masquerade_trial",
                    entries = listOf(
                        DomainMobPoolEntry("minecraft:witch", 4),
                        DomainMobPoolEntry("minecraft:cave_spider", 3),
                        DomainMobPoolEntry("minecraft:enderman", 2)
                    )
                ),
                DomainMobPool(
                    id = "credit_trial",
                    entries = listOf(
                        DomainMobPoolEntry("minecraft:witch", 3),
                        DomainMobPoolEntry("minecraft:vindicator", 3),
                        DomainMobPoolEntry("minecraft:enderman", 2),
                        DomainMobPoolEntry("minecraft:piglin_brute", 1)
                    )
                )
            ),
            domains = listOf(
                DomainDefinition(
                    id = "hinagata_archive",
                    nameKey = "screen.cresora.domain.hinagata_archive",
                    themeId = "verdant",
                    unlockRank = 1,
                    entryCostCsc = 1_500,
                    rewardProfileId = "hinagata_hunt",
                    mobPoolId = "verdant_trial",
                    stages = listOf(
                        DomainStageDefinition(
                            waves = listOf(
                                DomainWaveDefinition(4, 0, false, 40),
                                DomainWaveDefinition(5, 1, false, 60),
                                DomainWaveDefinition(2, 3, true, 80)
                            )
                        )
                    )
                ),
                DomainDefinition(
                    id = "rondo_forge",
                    nameKey = "screen.cresora.domain.rondo_forge",
                    themeId = "metal",
                    unlockRank = 10,
                    entryCostCsc = 2_500,
                    rewardProfileId = "rondo_forge",
                    mobPoolId = "forge_trial",
                    stages = listOf(
                        DomainStageDefinition(
                            waves = listOf(
                                DomainWaveDefinition(4, 2, false, 40),
                                DomainWaveDefinition(5, 4, false, 60),
                                DomainWaveDefinition(3, 6, true, 80)
                            )
                        )
                    )
                ),
                DomainDefinition(
                    id = "masquerade_soiree",
                    nameKey = "screen.cresora.domain.masquerade_soiree",
                    themeId = "moonlit",
                    unlockRank = 10,
                    entryCostCsc = 2_500,
                    rewardProfileId = "masquerade_soiree",
                    mobPoolId = "masquerade_trial",
                    stages = listOf(
                        DomainStageDefinition(
                            waves = listOf(
                                DomainWaveDefinition(4, 2, false, 40),
                                DomainWaveDefinition(5, 4, false, 60),
                                DomainWaveDefinition(3, 6, true, 80)
                            )
                        )
                    )
                ),
                DomainDefinition(
                    id = "credit_drill",
                    nameKey = "screen.cresora.domain.credit_drill",
                    themeId = "arcane",
                    unlockRank = 20,
                    entryCostCsc = 4_000,
                    rewardProfileId = "csc_training",
                    mobPoolId = "credit_trial",
                    stages = listOf(
                        DomainStageDefinition(
                            waves = listOf(
                                DomainWaveDefinition(4, 5, false, 40),
                                DomainWaveDefinition(4, 7, false, 60),
                                DomainWaveDefinition(2, 9, true, 80),
                                DomainWaveDefinition(3, 10, true, 100)
                            )
                        )
                    )
                )
            )
        )
    }
}
