package hifumi.cresora

import com.google.gson.JsonParser
import com.mojang.serialization.Codec
import com.mojang.serialization.JsonOps
import com.mojang.serialization.codecs.RecordCodecBuilder
import org.slf4j.LoggerFactory
import java.io.InputStreamReader

data class EquipmentArtifactLootRule(
    val chance: Float,
    val levelMin: Int,
    val levelMax: Int,
    val forcedRarity: EquipmentRarity,
    val dropProfileId: String,
    val equipmentIds: List<String> = emptyList()
) {
    companion object {
        val CODEC: Codec<EquipmentArtifactLootRule> = RecordCodecBuilder.create { instance ->
            instance.group(
                Codec.FLOAT.fieldOf("chance").forGetter(EquipmentArtifactLootRule::chance),
                Codec.INT.fieldOf("levelMin").forGetter(EquipmentArtifactLootRule::levelMin),
                Codec.INT.fieldOf("levelMax").forGetter(EquipmentArtifactLootRule::levelMax),
                EquipmentRarity.CODEC.fieldOf("forcedRarity").forGetter(EquipmentArtifactLootRule::forcedRarity),
                Codec.STRING.fieldOf("dropProfileId").forGetter(EquipmentArtifactLootRule::dropProfileId),
                Codec.STRING.listOf().optionalFieldOf("equipmentIds", emptyList()).forGetter(EquipmentArtifactLootRule::equipmentIds)
            ).apply(instance, ::EquipmentArtifactLootRule)
        }
    }
}

data class EquipmentUpgradeMaterialLootRule(
    val chance: Float,
    val levelMin: Int,
    val levelMax: Int
) {
    companion object {
        val CODEC: Codec<EquipmentUpgradeMaterialLootRule> = RecordCodecBuilder.create { instance ->
            instance.group(
                Codec.FLOAT.fieldOf("chance").forGetter(EquipmentUpgradeMaterialLootRule::chance),
                Codec.INT.fieldOf("levelMin").forGetter(EquipmentUpgradeMaterialLootRule::levelMin),
                Codec.INT.fieldOf("levelMax").forGetter(EquipmentUpgradeMaterialLootRule::levelMax)
            ).apply(instance, ::EquipmentUpgradeMaterialLootRule)
        }
    }
}

data class EquipmentMobLootDefinition(
    val entityTypeId: String,
    val artifactLoot: EquipmentArtifactLootRule? = null,
    val upgradeMaterialLoot: EquipmentUpgradeMaterialLootRule? = null
) {
    companion object {
        val CODEC: Codec<EquipmentMobLootDefinition> = RecordCodecBuilder.create { instance ->
            instance.group(
                Codec.STRING.fieldOf("entityTypeId").forGetter(EquipmentMobLootDefinition::entityTypeId),
                EquipmentArtifactLootRule.CODEC.optionalFieldOf("artifactLoot")
                    .forGetter { java.util.Optional.ofNullable(it.artifactLoot) },
                EquipmentUpgradeMaterialLootRule.CODEC.optionalFieldOf("upgradeMaterialLoot")
                    .forGetter { java.util.Optional.ofNullable(it.upgradeMaterialLoot) }
            ).apply(instance) { entityTypeId, artifactLoot, upgradeMaterialLoot ->
                EquipmentMobLootDefinition(entityTypeId, artifactLoot.orElse(null), upgradeMaterialLoot.orElse(null))
            }
        }
    }
}

data class EquipmentContentBundle(
    val slots: List<EquipmentSlotType>,
    val sets: List<EquipmentSet>,
    val equipmentDefinitions: List<EquipmentDefinition>,
    val dropProfiles: List<EquipmentDropProfile>,
    val mobLoot: List<EquipmentMobLootDefinition>
) {
    companion object {
        val CODEC: Codec<EquipmentContentBundle> = RecordCodecBuilder.create { instance ->
            instance.group(
                EquipmentSlotType.CODEC.listOf().fieldOf("slots").forGetter(EquipmentContentBundle::slots),
                EquipmentSet.CODEC.listOf().fieldOf("sets").forGetter(EquipmentContentBundle::sets),
                EquipmentDefinition.CODEC.listOf().fieldOf("equipmentDefinitions").forGetter(EquipmentContentBundle::equipmentDefinitions),
                EquipmentDropProfile.CODEC.listOf().fieldOf("dropProfiles").forGetter(EquipmentContentBundle::dropProfiles),
                EquipmentMobLootDefinition.CODEC.listOf().fieldOf("mobLoot").forGetter(EquipmentContentBundle::mobLoot)
            ).apply(instance, ::EquipmentContentBundle)
        }
    }
}

object EquipmentContentRegistry {
    private const val CONTENT_RESOURCE = "data/cresora-utilities/cresora/equipment_content.json"

    private val logger = LoggerFactory.getLogger("${CreSoraUtilities.MOD_ID}/equipment-content")

    @Volatile
    private var slots: Map<String, EquipmentSlotType> = emptyMap()

    @Volatile
    private var sets: Map<String, EquipmentSet> = emptyMap()

    @Volatile
    private var equipmentDefinitions: Map<String, EquipmentDefinition> = emptyMap()

    @Volatile
    private var dropProfiles: Map<String, EquipmentDropProfile> = emptyMap()

    @Volatile
    private var mobLoot: List<EquipmentMobLootDefinition> = emptyList()

    fun init() {
        applyBundle(defaultBundle())
        runCatching { loadBundledContent() }
            .onSuccess { bundle ->
                applyBundle(bundle)
                logger.info("Loaded equipment content from {}", CONTENT_RESOURCE)
            }
            .onFailure { throwable ->
                logger.error("Failed to load equipment content from {}. Using built-in defaults.", CONTENT_RESOURCE, throwable)
            }
    }

    fun requireSlot(id: String): EquipmentSlotType {
        return slots[id] ?: error("Unknown equipment slot type: $id")
    }

    fun requireSet(id: String): EquipmentSet {
        return sets[id] ?: error("Unknown equipment set: $id")
    }

    fun requireEquipment(id: String): EquipmentDefinition {
        return equipmentDefinitions[id] ?: error("Unknown equipment definition: $id")
    }

    fun equipmentDefinitions(): List<EquipmentDefinition> = equipmentDefinitions.values.sortedBy(EquipmentDefinition::id)

    fun requireDropProfile(id: String): EquipmentDropProfile {
        return dropProfiles[id] ?: error("Unknown equipment drop profile: $id")
    }

    fun mobLootRules(): List<EquipmentMobLootDefinition> = mobLoot

    fun equipmentCountForSet(setId: String): Int {
        return equipmentDefinitions.values.count { it.setId == setId }
    }

    private fun loadBundledContent(): EquipmentContentBundle {
        val stream = EquipmentContentRegistry::class.java.classLoader.getResourceAsStream(CONTENT_RESOURCE)
            ?: error("Missing resource: $CONTENT_RESOURCE")
        InputStreamReader(stream).use { reader ->
            val json = JsonParser.parseReader(reader)
            return EquipmentContentBundle.CODEC.parse(JsonOps.INSTANCE, json)
                .getOrThrow { message -> IllegalArgumentException("Invalid equipment content: $message") }
        }
    }

    private fun applyBundle(bundle: EquipmentContentBundle) {
        val slotMap = bundle.slots.associateBy(EquipmentSlotType::id)
        val setMap = bundle.sets.associateBy(EquipmentSet::id)
        val definitionMap = bundle.equipmentDefinitions.associateBy(EquipmentDefinition::id)
        val dropProfileMap = bundle.dropProfiles.associateBy(EquipmentDropProfile::id)

        require(slotMap.size == bundle.slots.size) { "Duplicate equipment slot ids found in content bundle" }
        require(setMap.size == bundle.sets.size) { "Duplicate equipment set ids found in content bundle" }
        require(definitionMap.size == bundle.equipmentDefinitions.size) { "Duplicate equipment definition ids found in content bundle" }
        require(dropProfileMap.size == bundle.dropProfiles.size) { "Duplicate equipment drop profile ids found in content bundle" }

        for (definition in definitionMap.values) {
            require(slotMap.containsKey(definition.slotTypeId)) { "Unknown slot '${definition.slotTypeId}' referenced by equipment '${definition.id}'" }
            require(setMap.containsKey(definition.setId)) { "Unknown set '${definition.setId}' referenced by equipment '${definition.id}'" }
        }
        for (loot in bundle.mobLoot) {
            loot.artifactLoot?.let { artifact ->
                require(dropProfileMap.containsKey(artifact.dropProfileId)) {
                    "Unknown drop profile '${artifact.dropProfileId}' referenced by mob loot '${loot.entityTypeId}'"
                }
                for (equipmentId in artifact.equipmentIds) {
                    require(definitionMap.containsKey(equipmentId)) {
                        "Unknown equipment id '$equipmentId' referenced by mob loot '${loot.entityTypeId}'"
                    }
                }
            }
        }

        slots = slotMap
        sets = setMap
        equipmentDefinitions = definitionMap
        dropProfiles = dropProfileMap
        mobLoot = bundle.mobLoot
    }

    private fun defaultBundle(): EquipmentContentBundle {
        return EquipmentContentBundle(
            slots = listOf(
                EquipmentSlotType(
                    id = "wand",
                    translationKeyId = "item.cresora.equipment.slot.wand",
                    trinketGroup = "chest",
                    trinketSlot = "necklace",
                    mainStatCandidates = listOf(StatType.ATK_FLAT, StatType.ATK_PERCENT, StatType.ALL_DMG_BONUS),
                    mainWeights = mapOf(
                        StatType.ATK_FLAT to 3.2,
                        StatType.ATK_PERCENT to 3.6,
                        StatType.ALL_DMG_BONUS to 3.4
                    ),
                    subWeights = mapOf(
                        StatType.ATK_FLAT to 2.8,
                        StatType.ATK_PERCENT to 3.0,
                        StatType.CRIT_RATE to 2.8,
                        StatType.CRIT_DMG to 3.2,
                        StatType.ALL_DMG_BONUS to 3.0
                    ),
                    defaultMainStatType = StatType.ATK_FLAT
                ),
                EquipmentSlotType(
                    id = "hat",
                    translationKeyId = "item.cresora.equipment.slot.hat",
                    trinketGroup = "head",
                    trinketSlot = "hat",
                    mainStatCandidates = listOf(StatType.HP_FLAT, StatType.HP_PERCENT, StatType.DEF_FLAT, StatType.DEF_PERCENT, StatType.DAMAGE_REDUCTION),
                    mainWeights = mapOf(
                        StatType.HP_FLAT to 2.8,
                        StatType.HP_PERCENT to 2.6,
                        StatType.DEF_FLAT to 3.8,
                        StatType.DEF_PERCENT to 3.6,
                        StatType.DAMAGE_REDUCTION to 2.4
                    ),
                    subWeights = mapOf(
                        StatType.HP_FLAT to 2.6,
                        StatType.HP_PERCENT to 2.4,
                        StatType.DEF_FLAT to 3.2,
                        StatType.DEF_PERCENT to 3.0,
                        StatType.DAMAGE_REDUCTION to 2.4
                    ),
                    defaultMainStatType = StatType.DEF_FLAT
                ),
                EquipmentSlotType(
                    id = "glasses",
                    translationKeyId = "item.cresora.equipment.slot.glasses",
                    trinketGroup = "head",
                    trinketSlot = "face",
                    mainStatCandidates = listOf(StatType.ATK_PERCENT, StatType.CRIT_RATE, StatType.CRIT_DMG, StatType.ALL_DMG_BONUS),
                    mainWeights = mapOf(
                        StatType.ATK_PERCENT to 2.6,
                        StatType.CRIT_RATE to 3.4,
                        StatType.CRIT_DMG to 3.2,
                        StatType.ALL_DMG_BONUS to 3.0
                    ),
                    subWeights = mapOf(
                        StatType.ATK_PERCENT to 2.6,
                        StatType.CRIT_RATE to 3.6,
                        StatType.CRIT_DMG to 3.8,
                        StatType.ALL_DMG_BONUS to 2.8
                    ),
                    defaultMainStatType = StatType.CRIT_RATE
                ),
                EquipmentSlotType(
                    id = "armor",
                    translationKeyId = "item.cresora.equipment.slot.armor",
                    trinketGroup = "chest",
                    trinketSlot = "back",
                    mainStatCandidates = listOf(StatType.HP_FLAT, StatType.HP_PERCENT, StatType.DEF_FLAT, StatType.DEF_PERCENT, StatType.DAMAGE_REDUCTION),
                    mainWeights = mapOf(
                        StatType.HP_FLAT to 3.2,
                        StatType.HP_PERCENT to 3.0,
                        StatType.DEF_FLAT to 3.2,
                        StatType.DEF_PERCENT to 3.0,
                        StatType.DAMAGE_REDUCTION to 2.8
                    ),
                    subWeights = mapOf(
                        StatType.HP_FLAT to 2.8,
                        StatType.HP_PERCENT to 2.6,
                        StatType.DEF_FLAT to 2.8,
                        StatType.DEF_PERCENT to 2.8,
                        StatType.DAMAGE_REDUCTION to 2.8
                    ),
                    defaultMainStatType = StatType.HP_FLAT
                ),
                EquipmentSlotType(
                    id = "boots",
                    translationKeyId = "item.cresora.equipment.slot.boots",
                    trinketGroup = "feet",
                    trinketSlot = "shoes",
                    mainStatCandidates = listOf(StatType.ATK_PERCENT, StatType.HP_PERCENT, StatType.DEF_PERCENT, StatType.CRIT_DMG, StatType.ALL_DMG_BONUS),
                    mainWeights = mapOf(
                        StatType.ATK_PERCENT to 2.8,
                        StatType.HP_PERCENT to 2.2,
                        StatType.DEF_PERCENT to 2.2,
                        StatType.CRIT_DMG to 3.0,
                        StatType.ALL_DMG_BONUS to 2.8
                    ),
                    subWeights = mapOf(
                        StatType.ATK_PERCENT to 2.6,
                        StatType.CRIT_RATE to 2.8,
                        StatType.CRIT_DMG to 3.2,
                        StatType.ALL_DMG_BONUS to 2.6,
                        StatType.DEF_PERCENT to 2.0
                    ),
                    defaultMainStatType = StatType.ATK_PERCENT
                )
            ),
            sets = listOf(
                EquipmentSet(
                    id = "hinagata",
                    translationKeyId = "item.cresora.equipment.set.hinagata",
                    bonuses = listOf(
                        EquipmentSetBonus(
                            requiredPieces = 2,
                            stats = listOf(StatEntry(StatType.ATK_PERCENT, 5.0))
                        ),
                        EquipmentSetBonus(
                            requiredPieces = 4,
                            stats = listOf(
                                StatEntry(StatType.ATK_PERCENT, 10.0),
                                StatEntry(StatType.CRIT_DMG, 10.0)
                            )
                        )
                    )
                )
            ),
            equipmentDefinitions = listOf(
                EquipmentDefinition("hinagata_wand", "wand", "hinagata", mapOf(
                    EquipmentRarity.THREE_STAR to 50.0,
                    EquipmentRarity.FOUR_STAR to 35.0,
                    EquipmentRarity.FIVE_STAR to 15.0
                ), "minecraft:stick", true),
                EquipmentDefinition("hinagata_hat", "hat", "hinagata", mapOf(
                    EquipmentRarity.THREE_STAR to 50.0,
                    EquipmentRarity.FOUR_STAR to 35.0,
                    EquipmentRarity.FIVE_STAR to 15.0
                ), "minecraft:chainmail_helmet"),
                EquipmentDefinition("hinagata_glasses", "glasses", "hinagata", mapOf(
                    EquipmentRarity.THREE_STAR to 50.0,
                    EquipmentRarity.FOUR_STAR to 35.0,
                    EquipmentRarity.FIVE_STAR to 15.0
                ), "minecraft:spyglass"),
                EquipmentDefinition("hinagata_armor", "armor", "hinagata", mapOf(
                    EquipmentRarity.THREE_STAR to 50.0,
                    EquipmentRarity.FOUR_STAR to 35.0,
                    EquipmentRarity.FIVE_STAR to 15.0
                ), "minecraft:iron_chestplate"),
                EquipmentDefinition("hinagata_boots", "boots", "hinagata", mapOf(
                    EquipmentRarity.THREE_STAR to 50.0,
                    EquipmentRarity.FOUR_STAR to 35.0,
                    EquipmentRarity.FIVE_STAR to 15.0
                ), "minecraft:iron_boots")
            ),
            dropProfiles = listOf(
                EquipmentDropProfile(
                    id = "zombie_survivor",
                    mainWeights = mapOf(
                        StatType.HP_FLAT to 4.0,
                        StatType.HP_PERCENT to 3.6,
                        StatType.DEF_FLAT to 3.2,
                        StatType.DEF_PERCENT to 3.2,
                        StatType.DAMAGE_REDUCTION to 2.8
                    ),
                    subWeights = mapOf(
                        StatType.HP_FLAT to 3.4,
                        StatType.HP_PERCENT to 3.2,
                        StatType.DEF_FLAT to 3.0,
                        StatType.DEF_PERCENT to 3.0,
                        StatType.DAMAGE_REDUCTION to 2.8
                    )
                ),
                EquipmentDropProfile(
                    id = "skeleton_assault",
                    mainWeights = mapOf(
                        StatType.ATK_FLAT to 3.4,
                        StatType.ATK_PERCENT to 3.4,
                        StatType.ALL_DMG_BONUS to 3.0
                    ),
                    subWeights = mapOf(
                        StatType.ATK_FLAT to 2.6,
                        StatType.ATK_PERCENT to 2.8,
                        StatType.CRIT_RATE to 3.6,
                        StatType.CRIT_DMG to 3.8,
                        StatType.ALL_DMG_BONUS to 2.8
                    )
                ),
                EquipmentDropProfile(
                    id = "warden_relic",
                    mainWeights = mapOf(
                        StatType.ATK_PERCENT to 3.0,
                        StatType.HP_PERCENT to 2.4,
                        StatType.DEF_PERCENT to 2.4,
                        StatType.ALL_DMG_BONUS to 3.4,
                        StatType.DAMAGE_REDUCTION to 2.8
                    ),
                    subWeights = mapOf(
                        StatType.CRIT_RATE to 3.0,
                        StatType.CRIT_DMG to 3.2,
                        StatType.ALL_DMG_BONUS to 3.4,
                        StatType.DAMAGE_REDUCTION to 3.0,
                        StatType.HP_PERCENT to 2.2,
                        StatType.DEF_PERCENT to 2.2
                    )
                )
            ),
            mobLoot = listOf(
                EquipmentMobLootDefinition(
                    entityTypeId = "minecraft:zombie",
                    artifactLoot = EquipmentArtifactLootRule(0.2f, 0, 2, EquipmentRarity.THREE_STAR, "zombie_survivor"),
                    upgradeMaterialLoot = EquipmentUpgradeMaterialLootRule(0.1f, 1, 2)
                ),
                EquipmentMobLootDefinition(
                    entityTypeId = "minecraft:drowned",
                    artifactLoot = EquipmentArtifactLootRule(0.18f, 1, 3, EquipmentRarity.THREE_STAR, "zombie_survivor"),
                    upgradeMaterialLoot = EquipmentUpgradeMaterialLootRule(0.1f, 1, 2)
                ),
                EquipmentMobLootDefinition(
                    entityTypeId = "minecraft:husk",
                    artifactLoot = EquipmentArtifactLootRule(0.18f, 1, 3, EquipmentRarity.THREE_STAR, "zombie_survivor"),
                    upgradeMaterialLoot = EquipmentUpgradeMaterialLootRule(0.1f, 1, 2)
                ),
                EquipmentMobLootDefinition(
                    entityTypeId = "minecraft:spider",
                    artifactLoot = EquipmentArtifactLootRule(0.16f, 0, 2, EquipmentRarity.THREE_STAR, "zombie_survivor"),
                    upgradeMaterialLoot = EquipmentUpgradeMaterialLootRule(0.08f, 1, 2)
                ),
                EquipmentMobLootDefinition(
                    entityTypeId = "minecraft:cave_spider",
                    artifactLoot = EquipmentArtifactLootRule(0.18f, 1, 3, EquipmentRarity.THREE_STAR, "zombie_survivor"),
                    upgradeMaterialLoot = EquipmentUpgradeMaterialLootRule(0.1f, 1, 2)
                ),
                EquipmentMobLootDefinition(
                    entityTypeId = "minecraft:silverfish",
                    artifactLoot = EquipmentArtifactLootRule(0.1f, 0, 1, EquipmentRarity.THREE_STAR, "zombie_survivor"),
                    upgradeMaterialLoot = EquipmentUpgradeMaterialLootRule(0.06f, 1, 1)
                ),
                EquipmentMobLootDefinition(
                    entityTypeId = "minecraft:endermite",
                    artifactLoot = EquipmentArtifactLootRule(0.1f, 0, 1, EquipmentRarity.THREE_STAR, "zombie_survivor"),
                    upgradeMaterialLoot = EquipmentUpgradeMaterialLootRule(0.06f, 1, 1)
                ),
                EquipmentMobLootDefinition(
                    entityTypeId = "minecraft:slime",
                    artifactLoot = EquipmentArtifactLootRule(0.14f, 0, 2, EquipmentRarity.THREE_STAR, "zombie_survivor"),
                    upgradeMaterialLoot = EquipmentUpgradeMaterialLootRule(0.08f, 1, 2)
                ),
                EquipmentMobLootDefinition(
                    entityTypeId = "minecraft:skeleton",
                    artifactLoot = EquipmentArtifactLootRule(0.2f, 2, 5, EquipmentRarity.FOUR_STAR, "skeleton_assault"),
                    upgradeMaterialLoot = EquipmentUpgradeMaterialLootRule(0.12f, 2, 3)
                ),
                EquipmentMobLootDefinition(
                    entityTypeId = "minecraft:stray",
                    artifactLoot = EquipmentArtifactLootRule(0.2f, 2, 5, EquipmentRarity.FOUR_STAR, "skeleton_assault"),
                    upgradeMaterialLoot = EquipmentUpgradeMaterialLootRule(0.12f, 2, 3)
                ),
                EquipmentMobLootDefinition(
                    entityTypeId = "minecraft:creeper",
                    artifactLoot = EquipmentArtifactLootRule(0.18f, 2, 4, EquipmentRarity.FOUR_STAR, "skeleton_assault"),
                    upgradeMaterialLoot = EquipmentUpgradeMaterialLootRule(0.12f, 2, 3)
                ),
                EquipmentMobLootDefinition(
                    entityTypeId = "minecraft:phantom",
                    artifactLoot = EquipmentArtifactLootRule(0.18f, 2, 4, EquipmentRarity.FOUR_STAR, "skeleton_assault"),
                    upgradeMaterialLoot = EquipmentUpgradeMaterialLootRule(0.1f, 2, 3)
                ),
                EquipmentMobLootDefinition(
                    entityTypeId = "minecraft:pillager",
                    artifactLoot = EquipmentArtifactLootRule(0.2f, 3, 5, EquipmentRarity.FOUR_STAR, "skeleton_assault"),
                    upgradeMaterialLoot = EquipmentUpgradeMaterialLootRule(0.12f, 2, 3)
                ),
                EquipmentMobLootDefinition(
                    entityTypeId = "minecraft:breeze",
                    artifactLoot = EquipmentArtifactLootRule(0.22f, 3, 6, EquipmentRarity.FOUR_STAR, "skeleton_assault"),
                    upgradeMaterialLoot = EquipmentUpgradeMaterialLootRule(0.14f, 2, 4)
                ),
                EquipmentMobLootDefinition(
                    entityTypeId = "minecraft:witch",
                    artifactLoot = EquipmentArtifactLootRule(0.2f, 3, 6, EquipmentRarity.FOUR_STAR, "skeleton_assault"),
                    upgradeMaterialLoot = EquipmentUpgradeMaterialLootRule(0.14f, 2, 4)
                ),
                EquipmentMobLootDefinition(
                    entityTypeId = "minecraft:blaze",
                    artifactLoot = EquipmentArtifactLootRule(0.22f, 4, 7, EquipmentRarity.FOUR_STAR, "skeleton_assault"),
                    upgradeMaterialLoot = EquipmentUpgradeMaterialLootRule(0.14f, 3, 4)
                ),
                EquipmentMobLootDefinition(
                    entityTypeId = "minecraft:magma_cube",
                    artifactLoot = EquipmentArtifactLootRule(0.18f, 3, 5, EquipmentRarity.FOUR_STAR, "skeleton_assault"),
                    upgradeMaterialLoot = EquipmentUpgradeMaterialLootRule(0.12f, 2, 4)
                ),
                EquipmentMobLootDefinition(
                    entityTypeId = "minecraft:ghast",
                    artifactLoot = EquipmentArtifactLootRule(0.2f, 4, 7, EquipmentRarity.FOUR_STAR, "skeleton_assault"),
                    upgradeMaterialLoot = EquipmentUpgradeMaterialLootRule(0.14f, 3, 4)
                ),
                EquipmentMobLootDefinition(
                    entityTypeId = "minecraft:guardian",
                    artifactLoot = EquipmentArtifactLootRule(0.22f, 4, 7, EquipmentRarity.FOUR_STAR, "skeleton_assault"),
                    upgradeMaterialLoot = EquipmentUpgradeMaterialLootRule(0.14f, 3, 4)
                ),
                EquipmentMobLootDefinition(
                    entityTypeId = "minecraft:zombified_piglin",
                    artifactLoot = EquipmentArtifactLootRule(0.18f, 4, 7, EquipmentRarity.FOUR_STAR, "skeleton_assault"),
                    upgradeMaterialLoot = EquipmentUpgradeMaterialLootRule(0.12f, 2, 4)
                ),
                EquipmentMobLootDefinition(
                    entityTypeId = "minecraft:enderman",
                    artifactLoot = EquipmentArtifactLootRule(0.22f, 5, 8, EquipmentRarity.FOUR_STAR, "warden_relic"),
                    upgradeMaterialLoot = EquipmentUpgradeMaterialLootRule(0.15f, 3, 5)
                ),
                EquipmentMobLootDefinition(
                    entityTypeId = "minecraft:hoglin",
                    artifactLoot = EquipmentArtifactLootRule(0.22f, 5, 8, EquipmentRarity.FOUR_STAR, "warden_relic"),
                    upgradeMaterialLoot = EquipmentUpgradeMaterialLootRule(0.15f, 3, 5)
                ),
                EquipmentMobLootDefinition(
                    entityTypeId = "minecraft:vindicator",
                    artifactLoot = EquipmentArtifactLootRule(0.24f, 5, 8, EquipmentRarity.FOUR_STAR, "warden_relic"),
                    upgradeMaterialLoot = EquipmentUpgradeMaterialLootRule(0.16f, 3, 5)
                ),
                EquipmentMobLootDefinition(
                    entityTypeId = "minecraft:evoker",
                    artifactLoot = EquipmentArtifactLootRule(0.28f, 6, 9, EquipmentRarity.FOUR_STAR, "warden_relic"),
                    upgradeMaterialLoot = EquipmentUpgradeMaterialLootRule(0.18f, 4, 5)
                ),
                EquipmentMobLootDefinition(
                    entityTypeId = "minecraft:ravager",
                    artifactLoot = EquipmentArtifactLootRule(0.35f, 7, 10, EquipmentRarity.FIVE_STAR, "warden_relic"),
                    upgradeMaterialLoot = EquipmentUpgradeMaterialLootRule(0.22f, 4, 6)
                ),
                EquipmentMobLootDefinition(
                    entityTypeId = "minecraft:piglin_brute",
                    artifactLoot = EquipmentArtifactLootRule(0.28f, 6, 9, EquipmentRarity.FOUR_STAR, "warden_relic"),
                    upgradeMaterialLoot = EquipmentUpgradeMaterialLootRule(0.18f, 4, 5)
                ),
                EquipmentMobLootDefinition(
                    entityTypeId = "minecraft:elder_guardian",
                    artifactLoot = EquipmentArtifactLootRule(0.55f, 7, 10, EquipmentRarity.FIVE_STAR, "warden_relic"),
                    upgradeMaterialLoot = EquipmentUpgradeMaterialLootRule(0.25f, 4, 6)
                ),
                EquipmentMobLootDefinition(
                    entityTypeId = "minecraft:shulker",
                    artifactLoot = EquipmentArtifactLootRule(0.26f, 6, 9, EquipmentRarity.FOUR_STAR, "warden_relic"),
                    upgradeMaterialLoot = EquipmentUpgradeMaterialLootRule(0.16f, 3, 5)
                ),
                EquipmentMobLootDefinition(
                    entityTypeId = "minecraft:warden",
                    artifactLoot = EquipmentArtifactLootRule(1.0f, 8, 12, EquipmentRarity.FIVE_STAR, "warden_relic")
                )
            )
        )
    }
}
