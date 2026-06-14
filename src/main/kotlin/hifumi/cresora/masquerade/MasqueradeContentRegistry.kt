package hifumi.cresora.masquerade
import hifumi.cresora.CreSoraUtilities
import hifumi.cresora.StatType
import hifumi.cresora.adventurerank.AdventureRankProgression
import hifumi.cresora.equipment.EquipmentRarity
import com.google.gson.JsonParser
import com.mojang.serialization.Codec
import com.mojang.serialization.JsonOps
import com.mojang.serialization.codecs.RecordCodecBuilder
import org.slf4j.LoggerFactory
import java.io.InputStreamReader
import java.util.Optional

data class MasqueradeEnemyModifierDefinition(
    val healthScalar: Double = 1.0,
    val damageScalar: Double = 1.0,
    val defenseScalar: Double = 1.0,
    val toughnessScalar: Double = 1.0,
    val damageReductionPercent: Double = 0.0
) {
    companion object {
        val CODEC: Codec<MasqueradeEnemyModifierDefinition> = RecordCodecBuilder.create { instance ->
            instance.group(
                Codec.DOUBLE.optionalFieldOf("healthScalar", 1.0).forGetter(MasqueradeEnemyModifierDefinition::healthScalar),
                Codec.DOUBLE.optionalFieldOf("damageScalar", 1.0).forGetter(MasqueradeEnemyModifierDefinition::damageScalar),
                Codec.DOUBLE.optionalFieldOf("defenseScalar", 1.0).forGetter(MasqueradeEnemyModifierDefinition::defenseScalar),
                Codec.DOUBLE.optionalFieldOf("toughnessScalar", 1.0).forGetter(MasqueradeEnemyModifierDefinition::toughnessScalar),
                Codec.DOUBLE.optionalFieldOf("damageReductionPercent", 0.0).forGetter(MasqueradeEnemyModifierDefinition::damageReductionPercent)
            ).apply(instance, ::MasqueradeEnemyModifierDefinition)
        }
    }
}

data class MasqueradeSpawnDefinition(
    val entityTypeId: String,
    val rank: Int,
    val count: Int,
    val elite: Boolean = false,
    val modifiers: MasqueradeEnemyModifierDefinition = MasqueradeEnemyModifierDefinition(),
    val boss: Boolean = false
) {
    companion object {
        val CODEC: Codec<MasqueradeSpawnDefinition> = RecordCodecBuilder.create { instance ->
            instance.group(
                Codec.STRING.fieldOf("entityTypeId").forGetter(MasqueradeSpawnDefinition::entityTypeId),
                Codec.INT.fieldOf("rank").forGetter(MasqueradeSpawnDefinition::rank),
                Codec.INT.fieldOf("count").forGetter(MasqueradeSpawnDefinition::count),
                Codec.BOOL.optionalFieldOf("elite", false).forGetter(MasqueradeSpawnDefinition::elite),
                MasqueradeEnemyModifierDefinition.CODEC.optionalFieldOf("modifiers", MasqueradeEnemyModifierDefinition())
                    .forGetter(MasqueradeSpawnDefinition::modifiers),
                Codec.BOOL.optionalFieldOf("boss", false).forGetter(MasqueradeSpawnDefinition::boss)
            ).apply(instance, ::MasqueradeSpawnDefinition)
        }
    }
}

data class MasqueradeWaveDefinition(
    val waveNumber: Int,
    val timeLimitSeconds: Int,
    val spawns: List<MasqueradeSpawnDefinition>
) {
    companion object {
        val CODEC: Codec<MasqueradeWaveDefinition> = RecordCodecBuilder.create { instance ->
            instance.group(
                Codec.INT.fieldOf("waveNumber").forGetter(MasqueradeWaveDefinition::waveNumber),
                Codec.INT.fieldOf("timeLimitSeconds").forGetter(MasqueradeWaveDefinition::timeLimitSeconds),
                MasqueradeSpawnDefinition.CODEC.listOf().fieldOf("spawns").forGetter(MasqueradeWaveDefinition::spawns)
            ).apply(instance, ::MasqueradeWaveDefinition)
        }
    }
}

data class MasqueradeSupportEligibilityDefinition(
    val minWave: Int = 1,
    val maxWave: Int = 999
) {
    companion object {
        val CODEC: Codec<MasqueradeSupportEligibilityDefinition> = RecordCodecBuilder.create { instance ->
            instance.group(
                Codec.INT.optionalFieldOf("minWave", 1).forGetter(MasqueradeSupportEligibilityDefinition::minWave),
                Codec.INT.optionalFieldOf("maxWave", 999).forGetter(MasqueradeSupportEligibilityDefinition::maxWave)
            ).apply(instance, ::MasqueradeSupportEligibilityDefinition)
        }
    }
}

data class MasqueradeSupportBuffDefinition(
    val id: String,
    val rarity: EquipmentRarity,
    val nameKey: String,
    val descriptionKey: String,
    val effectId: String,
    val statType: StatType? = null,
    val statValue: Double = 0.0,
    val weight: Double = 1.0,
    val procChance: Double = 0.0,
    val cooldownSeconds: Int = 0,
    val triggerWaveMultiple: Int = 0,
    val eligibility: MasqueradeSupportEligibilityDefinition = MasqueradeSupportEligibilityDefinition()
) {
    companion object {
        val CODEC: Codec<MasqueradeSupportBuffDefinition> = RecordCodecBuilder.create { instance ->
            instance.group(
                Codec.STRING.fieldOf("id").forGetter(MasqueradeSupportBuffDefinition::id),
                EquipmentRarity.CODEC.fieldOf("rarity").forGetter(MasqueradeSupportBuffDefinition::rarity),
                Codec.STRING.fieldOf("nameKey").forGetter(MasqueradeSupportBuffDefinition::nameKey),
                Codec.STRING.fieldOf("descriptionKey").forGetter(MasqueradeSupportBuffDefinition::descriptionKey),
                Codec.STRING.fieldOf("effectId").forGetter(MasqueradeSupportBuffDefinition::effectId),
                StatType.CODEC.optionalFieldOf("statType").forGetter { java.util.Optional.ofNullable(it.statType) },
                Codec.DOUBLE.optionalFieldOf("statValue", 0.0).forGetter(MasqueradeSupportBuffDefinition::statValue),
                Codec.DOUBLE.optionalFieldOf("weight", 1.0).forGetter(MasqueradeSupportBuffDefinition::weight),
                Codec.DOUBLE.optionalFieldOf("procChance", 0.0).forGetter(MasqueradeSupportBuffDefinition::procChance),
                Codec.INT.optionalFieldOf("cooldownSeconds", 0).forGetter(MasqueradeSupportBuffDefinition::cooldownSeconds),
                Codec.INT.optionalFieldOf("triggerWaveMultiple", 0).forGetter(MasqueradeSupportBuffDefinition::triggerWaveMultiple),
                MasqueradeSupportEligibilityDefinition.CODEC.optionalFieldOf("eligibility", MasqueradeSupportEligibilityDefinition())
                    .forGetter(MasqueradeSupportBuffDefinition::eligibility)
            ).apply(instance) { id, rarity, nameKey, descriptionKey, effectId, statType, statValue, weight, procChance, cooldownSeconds, triggerWaveMultiple, eligibility ->
                MasqueradeSupportBuffDefinition(
                    id = id,
                    rarity = rarity,
                    nameKey = nameKey,
                    descriptionKey = descriptionKey,
                    effectId = effectId,
                    statType = statType.orElse(null),
                    statValue = statValue,
                    weight = weight,
                    procChance = procChance,
                    cooldownSeconds = cooldownSeconds,
                    triggerWaveMultiple = triggerWaveMultiple,
                    eligibility = eligibility
                )
            }
        }
    }
}

data class MasqueradeRewardDefinition(
    val creditsPerWave: Int,
    val chordProgressionPerWave: Int
) {
    companion object {
        val CODEC: Codec<MasqueradeRewardDefinition> = RecordCodecBuilder.create { instance ->
            instance.group(
                Codec.INT.fieldOf("creditsPerWave").forGetter(MasqueradeRewardDefinition::creditsPerWave),
                Codec.INT.fieldOf("chordProgressionPerWave").forGetter(MasqueradeRewardDefinition::chordProgressionPerWave)
            ).apply(instance, ::MasqueradeRewardDefinition)
        }
    }
}

data class MasqueradeDefinition(
    val seasonId: String,
    val unlockRank: Int,
    val maxWaveCount: Int,
    val defaultTimeLimitSeconds: Int,
    val rewardPerClearedWave: MasqueradeRewardDefinition,
    val waves: List<MasqueradeWaveDefinition>,
    val supportBuffs: List<MasqueradeSupportBuffDefinition>
) {
    companion object {
        val CODEC: Codec<MasqueradeDefinition> = RecordCodecBuilder.create { instance ->
            instance.group(
                Codec.STRING.fieldOf("seasonId").forGetter(MasqueradeDefinition::seasonId),
                Codec.INT.fieldOf("unlockRank").forGetter(MasqueradeDefinition::unlockRank),
                Codec.INT.fieldOf("maxWaveCount").forGetter(MasqueradeDefinition::maxWaveCount),
                Codec.INT.fieldOf("defaultTimeLimitSeconds").forGetter(MasqueradeDefinition::defaultTimeLimitSeconds),
                MasqueradeRewardDefinition.CODEC.fieldOf("rewardPerClearedWave").forGetter(MasqueradeDefinition::rewardPerClearedWave),
                MasqueradeWaveDefinition.CODEC.listOf().fieldOf("waves").forGetter(MasqueradeDefinition::waves),
                MasqueradeSupportBuffDefinition.CODEC.listOf().fieldOf("supportBuffs").forGetter(MasqueradeDefinition::supportBuffs)
            ).apply(instance, ::MasqueradeDefinition)
        }
    }
}

object MasqueradeContentRegistry {
    private const val CONTENT_RESOURCE = "data/cresora-utilities/cresora/masquerade_content.json"

    private val logger = LoggerFactory.getLogger("${CreSoraUtilities.MOD_ID}/masquerade-content")

    @Volatile
    private var definition: MasqueradeDefinition = defaultDefinition()
    private var supportBuffsById: Map<String, MasqueradeSupportBuffDefinition> = definition.supportBuffs.associateBy(MasqueradeSupportBuffDefinition::id)

    fun init() {
        applyDefinition(defaultDefinition())
        runCatching { loadBundledContent() }
            .onSuccess { loaded ->
                applyDefinition(loaded)
                logger.info("Loaded masquerade content from {}", CONTENT_RESOURCE)
            }
            .onFailure { throwable ->
                logger.error("Failed to load masquerade content from {}. Using built-in defaults.", CONTENT_RESOURCE, throwable)
            }
    }

    fun definition(): MasqueradeDefinition = definition

    fun wave(waveNumber: Int): MasqueradeWaveDefinition {
        return definition.waves.firstOrNull { it.waveNumber == waveNumber } ?: error("Unknown masquerade wave: $waveNumber")
    }

    fun supportBuff(id: String): MasqueradeSupportBuffDefinition {
        return supportBuffsById[id] ?: error("Unknown masquerade support buff: $id")
    }

    fun supportBuffs(): List<MasqueradeSupportBuffDefinition> = definition.supportBuffs

    private fun loadBundledContent(): MasqueradeDefinition {
        val stream = MasqueradeContentRegistry::class.java.classLoader.getResourceAsStream(CONTENT_RESOURCE)
            ?: error("Missing resource: $CONTENT_RESOURCE")
        InputStreamReader(stream).use { reader ->
            val json = JsonParser.parseReader(reader)
            return MasqueradeDefinition.CODEC.parse(JsonOps.INSTANCE, json)
                .getOrThrow { message -> IllegalArgumentException("Invalid masquerade content: $message") }
        }
    }

    private fun applyDefinition(loaded: MasqueradeDefinition) {
        require(loaded.seasonId.matches(Regex("[A-Za-z0-9_.-]+"))) {
            "Masquerade seasonId must match [A-Za-z0-9_.-]+"
        }
        require(loaded.unlockRank >= 1) { "Masquerade unlockRank must be >= 1" }
        require(loaded.maxWaveCount >= 1) { "Masquerade maxWaveCount must be >= 1" }
        require(loaded.defaultTimeLimitSeconds >= 30) { "Masquerade defaultTimeLimitSeconds must be >= 30" }
        require(loaded.rewardPerClearedWave.creditsPerWave >= 0) { "Masquerade creditsPerWave must be >= 0" }
        require(loaded.rewardPerClearedWave.chordProgressionPerWave >= 0) { "Masquerade chordProgressionPerWave must be >= 0" }
        require(loaded.waves.size == loaded.maxWaveCount) { "Masquerade waves size must match maxWaveCount" }
        require(loaded.waves.map(MasqueradeWaveDefinition::waveNumber).sorted() == (1..loaded.maxWaveCount).toList()) {
            "Masquerade wave numbers must be unique and contiguous from 1 to maxWaveCount"
        }
        for (wave in loaded.waves) {
            require(wave.timeLimitSeconds >= 30) { "Masquerade wave ${wave.waveNumber} timeLimitSeconds must be >= 30" }
            require(wave.spawns.isNotEmpty()) { "Masquerade wave ${wave.waveNumber} must contain at least one spawn" }
            for (spawn in wave.spawns) {
                require(spawn.rank in AdventureRankProgression.MIN_RANK..AdventureRankProgression.MAX_RANK) {
                    "Masquerade wave ${wave.waveNumber} spawn '${spawn.entityTypeId}' has invalid rank ${spawn.rank}"
                }
                require(spawn.count >= 1) { "Masquerade wave ${wave.waveNumber} spawn '${spawn.entityTypeId}' count must be >= 1" }
                require(spawn.modifiers.healthScalar > 0.0) { "Masquerade wave ${wave.waveNumber} healthScalar must be > 0" }
                require(spawn.modifiers.damageScalar > 0.0) { "Masquerade wave ${wave.waveNumber} damageScalar must be > 0" }
                require(spawn.modifiers.defenseScalar > 0.0) { "Masquerade wave ${wave.waveNumber} defenseScalar must be > 0" }
                require(spawn.modifiers.toughnessScalar > 0.0) { "Masquerade wave ${wave.waveNumber} toughnessScalar must be > 0" }
                require(spawn.modifiers.damageReductionPercent in 0.0..95.0) {
                    "Masquerade wave ${wave.waveNumber} damageReductionPercent must be between 0 and 95"
                }
            }
        }
        val buffMap = loaded.supportBuffs.associateBy(MasqueradeSupportBuffDefinition::id)
        require(buffMap.size == loaded.supportBuffs.size) { "Masquerade support buff ids must be unique" }
        for (buff in loaded.supportBuffs) {
            require(buff.weight > 0.0) { "Masquerade support buff '${buff.id}' weight must be > 0" }
            require(buff.eligibility.minWave >= 1) { "Masquerade support buff '${buff.id}' minWave must be >= 1" }
            require(buff.eligibility.maxWave >= buff.eligibility.minWave) {
                "Masquerade support buff '${buff.id}' maxWave must be >= minWave"
            }
            when (buff.effectId) {
                "stat_bonus" -> {
                    require(buff.statType != null) { "Masquerade support buff '${buff.id}' requires statType" }
                    require(buff.statValue > 0.0) { "Masquerade support buff '${buff.id}' statValue must be > 0" }
                }
                "skip_multiple_of_four" -> Unit
                "guard_every_five_wave" -> {
                    require(buff.procChance in 0.0..1.0) { "Masquerade support buff '${buff.id}' procChance must be in [0, 1]" }
                    require(buff.cooldownSeconds >= 1) { "Masquerade support buff '${buff.id}' cooldownSeconds must be >= 1" }
                    require(buff.triggerWaveMultiple >= 1) { "Masquerade support buff '${buff.id}' triggerWaveMultiple must be >= 1" }
                }
                else -> error("Unknown masquerade support effectId: ${buff.effectId}")
            }
        }
        definition = loaded
        supportBuffsById = buffMap
    }

    private fun defaultDefinition(): MasqueradeDefinition {
        fun modifiers(
            health: Double = 1.0,
            damage: Double = 1.0,
            defense: Double = 1.0,
            toughness: Double = 1.0,
            reduction: Double = 0.0
        ): MasqueradeEnemyModifierDefinition {
            return MasqueradeEnemyModifierDefinition(
                healthScalar = health,
                damageScalar = damage,
                defenseScalar = defense,
                toughnessScalar = toughness,
                damageReductionPercent = reduction
            )
        }

        fun spawn(
            id: String,
            rank: Int,
            count: Int,
            elite: Boolean = false,
            modifiers: MasqueradeEnemyModifierDefinition = MasqueradeEnemyModifierDefinition(),
            boss: Boolean = false
        ): MasqueradeSpawnDefinition {
            return MasqueradeSpawnDefinition(id, rank, count, elite, modifiers, boss)
        }

        val waves = listOf(
            MasqueradeWaveDefinition(1, 180, listOf(spawn("minecraft:zombie", 5, 1, modifiers = modifiers(reduction = 20.0)))),
            MasqueradeWaveDefinition(2, 180, listOf(spawn("minecraft:zombie", 6, 2, modifiers = modifiers(health = 1.10, reduction = 10.0)))),
            MasqueradeWaveDefinition(3, 180, listOf(spawn("minecraft:spider", 7, 3, modifiers = modifiers(health = 1.10, damage = 1.05)))),
            MasqueradeWaveDefinition(4, 180, listOf(spawn("minecraft:skeleton", 8, 3, modifiers = modifiers(health = 1.15, damage = 1.10, reduction = 12.0)))),
            MasqueradeWaveDefinition(5, 180, listOf(spawn("minecraft:husk", 10, 3, modifiers = modifiers(health = 1.20, damage = 1.10, defense = 1.05)))),
            MasqueradeWaveDefinition(6, 180, listOf(spawn("minecraft:zombie", 11, 4, modifiers = modifiers(health = 1.25, damage = 1.12, reduction = 15.0)))),
            MasqueradeWaveDefinition(7, 180, listOf(spawn("minecraft:drowned", 12, 4, modifiers = modifiers(health = 1.25, damage = 1.15)))),
            MasqueradeWaveDefinition(8, 180, listOf(spawn("minecraft:witch", 14, 2, elite = true, modifiers = modifiers(health = 1.40, damage = 1.20, reduction = 18.0)))),
            MasqueradeWaveDefinition(9, 180, listOf(spawn("minecraft:enderman", 15, 2, modifiers = modifiers(health = 1.45, damage = 1.20, defense = 1.08)))),
            MasqueradeWaveDefinition(10, 180, listOf(spawn("minecraft:creeper", 16, 4, modifiers = modifiers(health = 1.35, damage = 1.18, reduction = 20.0)))),
            MasqueradeWaveDefinition(11, 180, listOf(spawn("minecraft:stray", 18, 4, modifiers = modifiers(health = 1.40, damage = 1.22)))),
            MasqueradeWaveDefinition(12, 180, listOf(spawn("minecraft:blaze", 20, 3, elite = true, modifiers = modifiers(health = 1.55, damage = 1.25, reduction = 22.0)))),
            MasqueradeWaveDefinition(13, 180, listOf(spawn("minecraft:cave_spider", 21, 5, modifiers = modifiers(health = 1.45, damage = 1.24)))),
            MasqueradeWaveDefinition(14, 180, listOf(spawn("minecraft:wither_skeleton", 23, 4, modifiers = modifiers(health = 1.55, damage = 1.28, defense = 1.10, reduction = 24.0)))),
            MasqueradeWaveDefinition(15, 180, listOf(spawn("minecraft:vindicator", 25, 3, elite = true, modifiers = modifiers(health = 1.70, damage = 1.32, defense = 1.12)))),
            MasqueradeWaveDefinition(16, 180, listOf(spawn("minecraft:pillager", 27, 5, modifiers = modifiers(health = 1.60, damage = 1.30, reduction = 25.0)))),
            MasqueradeWaveDefinition(17, 180, listOf(spawn("minecraft:enderman", 29, 3, elite = true, modifiers = modifiers(health = 1.80, damage = 1.35, defense = 1.12)))),
            MasqueradeWaveDefinition(18, 180, listOf(spawn("minecraft:piglin_brute", 31, 3, elite = true, modifiers = modifiers(health = 1.95, damage = 1.38, defense = 1.16, reduction = 28.0)))),
            MasqueradeWaveDefinition(19, 180, listOf(spawn("minecraft:witch", 33, 4, elite = true, modifiers = modifiers(health = 1.85, damage = 1.35, reduction = 26.0)))),
            MasqueradeWaveDefinition(20, 180, listOf(spawn("minecraft:ravager", 35, 1, elite = true, modifiers = modifiers(health = 2.20, damage = 1.42, defense = 1.20, toughness = 1.20, reduction = 30.0)))),
            MasqueradeWaveDefinition(21, 180, listOf(spawn("minecraft:shulker", 38, 3, elite = true, modifiers = modifiers(health = 2.00, damage = 1.42, reduction = 30.0)))),
            MasqueradeWaveDefinition(22, 180, listOf(spawn("minecraft:guardian", 40, 4, elite = true, modifiers = modifiers(health = 2.10, damage = 1.45, defense = 1.18)))),
            MasqueradeWaveDefinition(23, 180, listOf(spawn("minecraft:elder_guardian", 43, 1, elite = true, modifiers = modifiers(health = 2.35, damage = 1.48, defense = 1.22, toughness = 1.25, reduction = 32.0)))),
            MasqueradeWaveDefinition(24, 180, listOf(spawn("minecraft:evoker", 46, 3, elite = true, modifiers = modifiers(health = 2.20, damage = 1.50, reduction = 32.0)))),
            MasqueradeWaveDefinition(25, 180, listOf(spawn("minecraft:blaze", 49, 5, elite = true, modifiers = modifiers(health = 2.10, damage = 1.55, reduction = 34.0)))),
            MasqueradeWaveDefinition(26, 180, listOf(spawn("minecraft:piglin_brute", 52, 4, elite = true, modifiers = modifiers(health = 2.35, damage = 1.60, defense = 1.22, reduction = 35.0)))),
            MasqueradeWaveDefinition(27, 180, listOf(spawn("minecraft:shulker", 56, 4, elite = true, modifiers = modifiers(health = 2.45, damage = 1.62, reduction = 36.0)))),
            MasqueradeWaveDefinition(28, 180, listOf(spawn("minecraft:ravager", 60, 2, elite = true, modifiers = modifiers(health = 2.70, damage = 1.68, defense = 1.25, toughness = 1.30, reduction = 38.0)))),
            MasqueradeWaveDefinition(29, 180, listOf(spawn("minecraft:evoker", 64, 4, elite = true, modifiers = modifiers(health = 2.60, damage = 1.72, reduction = 40.0)))),
            MasqueradeWaveDefinition(30, 180, listOf(spawn("minecraft:warden", 70, 1, elite = true, modifiers = modifiers(health = 3.00, damage = 1.85, defense = 1.30, toughness = 1.35, reduction = 42.0))))
        )

        val supportBuffs = listOf(
            MasqueradeSupportBuffDefinition("atk_boost_minor", EquipmentRarity.THREE_STAR, "screen.cresora.masquerade.support.atk_minor", "screen.cresora.masquerade.support.atk_minor.desc", "stat_bonus", StatType.ATK_PERCENT, 10.0, 1.2),
            MasqueradeSupportBuffDefinition("hp_boost_minor", EquipmentRarity.THREE_STAR, "screen.cresora.masquerade.support.hp_minor", "screen.cresora.masquerade.support.hp_minor.desc", "stat_bonus", StatType.HP_PERCENT, 10.0, 1.0),
            MasqueradeSupportBuffDefinition("def_boost_minor", EquipmentRarity.THREE_STAR, "screen.cresora.masquerade.support.def_minor", "screen.cresora.masquerade.support.def_minor.desc", "stat_bonus", StatType.DEF_PERCENT, 10.0, 1.0),
            MasqueradeSupportBuffDefinition("crit_rate_minor", EquipmentRarity.THREE_STAR, "screen.cresora.masquerade.support.crit_rate_minor", "screen.cresora.masquerade.support.crit_rate_minor.desc", "stat_bonus", StatType.CRIT_RATE, 5.0, 0.8),
            MasqueradeSupportBuffDefinition("all_dmg_minor", EquipmentRarity.THREE_STAR, "screen.cresora.masquerade.support.all_dmg_minor", "screen.cresora.masquerade.support.all_dmg_minor.desc", "stat_bonus", StatType.ALL_DMG_BONUS, 8.0, 0.9),
            MasqueradeSupportBuffDefinition("atk_boost_major", EquipmentRarity.FOUR_STAR, "screen.cresora.masquerade.support.atk_major", "screen.cresora.masquerade.support.atk_major.desc", "stat_bonus", StatType.ATK_PERCENT, 30.0, 0.7),
            MasqueradeSupportBuffDefinition("hp_boost_major", EquipmentRarity.FOUR_STAR, "screen.cresora.masquerade.support.hp_major", "screen.cresora.masquerade.support.hp_major.desc", "stat_bonus", StatType.HP_PERCENT, 30.0, 0.6),
            MasqueradeSupportBuffDefinition("def_boost_major", EquipmentRarity.FOUR_STAR, "screen.cresora.masquerade.support.def_major", "screen.cresora.masquerade.support.def_major.desc", "stat_bonus", StatType.DEF_PERCENT, 25.0, 0.6),
            MasqueradeSupportBuffDefinition("crit_dmg_major", EquipmentRarity.FOUR_STAR, "screen.cresora.masquerade.support.crit_dmg_major", "screen.cresora.masquerade.support.crit_dmg_major.desc", "stat_bonus", StatType.CRIT_DMG, 25.0, 0.5),
            MasqueradeSupportBuffDefinition("damage_reduction_major", EquipmentRarity.FOUR_STAR, "screen.cresora.masquerade.support.damage_reduction_major", "screen.cresora.masquerade.support.damage_reduction_major.desc", "stat_bonus", StatType.DAMAGE_REDUCTION, 12.0, 0.5),
            MasqueradeSupportBuffDefinition("skip_multiple_of_four", EquipmentRarity.FIVE_STAR, "screen.cresora.masquerade.support.skip_four", "screen.cresora.masquerade.support.skip_four.desc", "skip_multiple_of_four", weight = 0.22, eligibility = MasqueradeSupportEligibilityDefinition(2, 24)),
            MasqueradeSupportBuffDefinition("guard_every_five_wave", EquipmentRarity.FIVE_STAR, "screen.cresora.masquerade.support.guard_five", "screen.cresora.masquerade.support.guard_five.desc", "guard_every_five_wave", weight = 0.18, procChance = 0.5, cooldownSeconds = 5, triggerWaveMultiple = 5, eligibility = MasqueradeSupportEligibilityDefinition(4, 30))
        )

        return MasqueradeDefinition(
            seasonId = "season_1",
            unlockRank = 3,
            maxWaveCount = waves.size,
            defaultTimeLimitSeconds = 180,
            rewardPerClearedWave = MasqueradeRewardDefinition(
                creditsPerWave = 10_000,
                chordProgressionPerWave = 3_250
            ),
            waves = waves,
            supportBuffs = supportBuffs
        )
    }
}
