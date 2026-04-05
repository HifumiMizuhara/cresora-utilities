package hifumi.cresora

import com.google.gson.JsonParser
import com.mojang.serialization.Codec
import com.mojang.serialization.JsonOps
import com.mojang.serialization.codecs.RecordCodecBuilder
import org.slf4j.LoggerFactory
import java.io.InputStreamReader

data class WeaponAttackCurvePoint(
    val level: Int,
    val attackDamage: Double
) {
    companion object {
        val CODEC: Codec<WeaponAttackCurvePoint> = RecordCodecBuilder.create { instance ->
            instance.group(
                Codec.INT.fieldOf("level").forGetter(WeaponAttackCurvePoint::level),
                Codec.DOUBLE.fieldOf("attackDamage").forGetter(WeaponAttackCurvePoint::attackDamage)
            ).apply(instance, ::WeaponAttackCurvePoint)
        }
    }
}

data class WeaponSkillDefinition(
    val effectId: String,
    val durationSeconds: Int,
    val cooldownSeconds: Int,
    val baseValue: Double,
    val valuePerLevel: Double,
    val radiusMeters: Double = 0.0,
    val secondaryBaseValue: Double = 0.0,
    val secondaryValuePerLevel: Double = 0.0,
    val tickIntervalSeconds: Double = 1.0
) {
    companion object {
        val CODEC: Codec<WeaponSkillDefinition> = RecordCodecBuilder.create { instance ->
            instance.group(
                Codec.STRING.fieldOf("effectId").forGetter(WeaponSkillDefinition::effectId),
                Codec.INT.fieldOf("durationSeconds").forGetter(WeaponSkillDefinition::durationSeconds),
                Codec.INT.fieldOf("cooldownSeconds").forGetter(WeaponSkillDefinition::cooldownSeconds),
                Codec.DOUBLE.fieldOf("baseValue").forGetter(WeaponSkillDefinition::baseValue),
                Codec.DOUBLE.fieldOf("valuePerLevel").forGetter(WeaponSkillDefinition::valuePerLevel),
                Codec.DOUBLE.optionalFieldOf("radiusMeters", 0.0).forGetter(WeaponSkillDefinition::radiusMeters),
                Codec.DOUBLE.optionalFieldOf("secondaryBaseValue", 0.0).forGetter(WeaponSkillDefinition::secondaryBaseValue),
                Codec.DOUBLE.optionalFieldOf("secondaryValuePerLevel", 0.0).forGetter(WeaponSkillDefinition::secondaryValuePerLevel),
                Codec.DOUBLE.optionalFieldOf("tickIntervalSeconds", 1.0).forGetter(WeaponSkillDefinition::tickIntervalSeconds)
            ).apply(instance, ::WeaponSkillDefinition)
        }
    }
}

data class WeaponUpgradeDefinition(
    val baseCscQuadraticCoefficient: Int,
    val skillCscLinearCoefficient: Int,
    val skillCscQuadraticCoefficient: Int = 0,
    val baseFragmentCost: Int,
    val skillArtifactMinRarity: EquipmentRarity? = null,
    val skillArtifactCountPerLevel: Int = 0
) {
    companion object {
        val CODEC: Codec<WeaponUpgradeDefinition> = RecordCodecBuilder.create { instance ->
            instance.group(
                Codec.INT.fieldOf("baseCscQuadraticCoefficient").forGetter(WeaponUpgradeDefinition::baseCscQuadraticCoefficient),
                Codec.INT.fieldOf("skillCscLinearCoefficient").forGetter(WeaponUpgradeDefinition::skillCscLinearCoefficient),
                Codec.INT.optionalFieldOf("skillCscQuadraticCoefficient", 0).forGetter(WeaponUpgradeDefinition::skillCscQuadraticCoefficient),
                Codec.INT.fieldOf("baseFragmentCost").forGetter(WeaponUpgradeDefinition::baseFragmentCost),
                EquipmentRarity.CODEC.optionalFieldOf("skillArtifactMinRarity")
                    .forGetter { java.util.Optional.ofNullable(it.skillArtifactMinRarity) },
                Codec.INT.optionalFieldOf("skillArtifactCountPerLevel", 0).forGetter(WeaponUpgradeDefinition::skillArtifactCountPerLevel)
            ).apply(instance) { baseCoeff, skillLinear, skillQuadratic, fragmentCost, artifactRarity, artifactCount ->
                WeaponUpgradeDefinition(
                    baseCscQuadraticCoefficient = baseCoeff,
                    skillCscLinearCoefficient = skillLinear,
                    skillCscQuadraticCoefficient = skillQuadratic,
                    baseFragmentCost = fragmentCost,
                    skillArtifactMinRarity = artifactRarity.orElse(null),
                    skillArtifactCountPerLevel = artifactCount
                )
            }
        }
    }
}

data class WeaponCraftDefinition(
    val fragmentItemId: String,
    val fragmentBaseItemId: String,
    val fragmentsRequired: Int,
    val craftedRarity: WeaponRarity,
    val craftedBaseLevel: Int,
    val craftedSkillLevel: Int
) {
    companion object {
        val CODEC: Codec<WeaponCraftDefinition> = RecordCodecBuilder.create { instance ->
            instance.group(
                Codec.STRING.fieldOf("fragmentItemId").forGetter(WeaponCraftDefinition::fragmentItemId),
                Codec.STRING.fieldOf("fragmentBaseItemId").forGetter(WeaponCraftDefinition::fragmentBaseItemId),
                Codec.INT.fieldOf("fragmentsRequired").forGetter(WeaponCraftDefinition::fragmentsRequired),
                WeaponRarity.CODEC.fieldOf("craftedRarity").forGetter(WeaponCraftDefinition::craftedRarity),
                Codec.INT.fieldOf("craftedBaseLevel").forGetter(WeaponCraftDefinition::craftedBaseLevel),
                Codec.INT.fieldOf("craftedSkillLevel").forGetter(WeaponCraftDefinition::craftedSkillLevel)
            ).apply(instance, ::WeaponCraftDefinition)
        }
    }
}

data class WeaponDropTier(
    val rarity: WeaponRarity,
    val minMobLevel: Int,
    val chance: Double
) {
    companion object {
        val CODEC: Codec<WeaponDropTier> = RecordCodecBuilder.create { instance ->
            instance.group(
                WeaponRarity.CODEC.fieldOf("rarity").forGetter(WeaponDropTier::rarity),
                Codec.INT.fieldOf("minMobLevel").forGetter(WeaponDropTier::minMobLevel),
                Codec.DOUBLE.fieldOf("chance").forGetter(WeaponDropTier::chance)
            ).apply(instance, ::WeaponDropTier)
        }
    }
}

data class WeaponFragmentDropDefinition(
    val minMobLevel: Int,
    val chance: Double,
    val minCount: Int,
    val maxCount: Int,
    val bonusCountAtLevel70: Int
) {
    companion object {
        val CODEC: Codec<WeaponFragmentDropDefinition> = RecordCodecBuilder.create { instance ->
            instance.group(
                Codec.INT.fieldOf("minMobLevel").forGetter(WeaponFragmentDropDefinition::minMobLevel),
                Codec.DOUBLE.fieldOf("chance").forGetter(WeaponFragmentDropDefinition::chance),
                Codec.INT.fieldOf("minCount").forGetter(WeaponFragmentDropDefinition::minCount),
                Codec.INT.fieldOf("maxCount").forGetter(WeaponFragmentDropDefinition::maxCount),
                Codec.INT.fieldOf("bonusCountAtLevel70").forGetter(WeaponFragmentDropDefinition::bonusCountAtLevel70)
            ).apply(instance, ::WeaponFragmentDropDefinition)
        }
    }
}

data class WeaponDropDefinition(
    val fragmentDrop: WeaponFragmentDropDefinition,
    val directDropTiers: List<WeaponDropTier>,
    val directDropBaseLevelMultiplier: Double,
    val fiveStarChanceMultiplierAtLevel70: Double
) {
    companion object {
        val CODEC: Codec<WeaponDropDefinition> = RecordCodecBuilder.create { instance ->
            instance.group(
                WeaponFragmentDropDefinition.CODEC.fieldOf("fragmentDrop").forGetter(WeaponDropDefinition::fragmentDrop),
                WeaponDropTier.CODEC.listOf().fieldOf("directDropTiers").forGetter(WeaponDropDefinition::directDropTiers),
                Codec.DOUBLE.fieldOf("directDropBaseLevelMultiplier").forGetter(WeaponDropDefinition::directDropBaseLevelMultiplier),
                Codec.DOUBLE.fieldOf("fiveStarChanceMultiplierAtLevel70").forGetter(WeaponDropDefinition::fiveStarChanceMultiplierAtLevel70)
            ).apply(instance, ::WeaponDropDefinition)
        }
    }
}

data class WeaponDefinition(
    val id: String,
    val baseItemId: String,
    val baseAttackDamage: Double,
    val attackDamagePerLevel: Double,
    val totalAttackSpeed: Double,
    val maxBaseLevel: Int,
    val maxSkillLevel: Int,
    val critRateBonusPercent: Double = 0.0,
    val maxAllDamageBonusPercent: Double = 0.0,
    val damageType: CombatDamageType = CombatDamageType.PHYSICAL,
    val attackCurve: List<WeaponAttackCurvePoint> = emptyList(),
    val skill: WeaponSkillDefinition,
    val upgrades: WeaponUpgradeDefinition,
    val craft: WeaponCraftDefinition,
    val drops: WeaponDropDefinition
) {
    fun translationKey(): String = "item.cresora-utilities.$id"

    fun fragmentTranslationKey(): String = "item.cresora-utilities.${craft.fragmentItemId}"
}

data class WeaponDefinitionRef(
    val id: String
) {
    fun resolve(): WeaponDefinition = WeaponContentRegistry.requireWeapon(id)
}

data class WeaponContentBundle(
    val weaponDefinitions: List<WeaponDefinition>
) {
    companion object {
        val CODEC: Codec<WeaponContentBundle> = RecordCodecBuilder.create { instance ->
            instance.group(
                Codec.list(WeaponContentRegistry.WEAPON_DEFINITION_CODEC).fieldOf("weaponDefinitions")
                    .forGetter(WeaponContentBundle::weaponDefinitions)
            ).apply(instance, ::WeaponContentBundle)
        }
    }
}

object WeaponContentRegistry {
    private const val CONTENT_RESOURCE = "data/cresora-utilities/cresora/weapon_content.json"

    internal val WEAPON_DEFINITION_CODEC: Codec<WeaponDefinition> = RecordCodecBuilder.create { instance ->
        instance.group(
            Codec.STRING.fieldOf("id").forGetter(WeaponDefinition::id),
            Codec.STRING.fieldOf("baseItemId").forGetter(WeaponDefinition::baseItemId),
            Codec.DOUBLE.fieldOf("baseAttackDamage").forGetter(WeaponDefinition::baseAttackDamage),
            Codec.DOUBLE.fieldOf("attackDamagePerLevel").forGetter(WeaponDefinition::attackDamagePerLevel),
            Codec.DOUBLE.fieldOf("totalAttackSpeed").forGetter(WeaponDefinition::totalAttackSpeed),
            Codec.INT.fieldOf("maxBaseLevel").forGetter(WeaponDefinition::maxBaseLevel),
            Codec.INT.fieldOf("maxSkillLevel").forGetter(WeaponDefinition::maxSkillLevel),
            Codec.DOUBLE.optionalFieldOf("critRateBonusPercent", 0.0).forGetter(WeaponDefinition::critRateBonusPercent),
            Codec.DOUBLE.optionalFieldOf("maxAllDamageBonusPercent", 0.0).forGetter(WeaponDefinition::maxAllDamageBonusPercent),
            CombatDamageType.CODEC.optionalFieldOf("damageType", CombatDamageType.PHYSICAL).forGetter(WeaponDefinition::damageType),
            WeaponAttackCurvePoint.CODEC.listOf().optionalFieldOf("attackCurve", emptyList()).forGetter(WeaponDefinition::attackCurve),
            WeaponSkillDefinition.CODEC.fieldOf("skill").forGetter(WeaponDefinition::skill),
            WeaponUpgradeDefinition.CODEC.fieldOf("upgrades").forGetter(WeaponDefinition::upgrades),
            WeaponCraftDefinition.CODEC.fieldOf("craft").forGetter(WeaponDefinition::craft),
            WeaponDropDefinition.CODEC.fieldOf("drops").forGetter(WeaponDefinition::drops)
        ).apply(instance, ::WeaponDefinition)
    }

    private val logger = LoggerFactory.getLogger("${CreSoraUtilities.MOD_ID}/weapon-content")

    @Volatile
    private var weapons: Map<String, WeaponDefinition> = emptyMap()

    fun init() {
        applyBundle(defaultBundle())
        runCatching { loadBundledContent() }
            .onSuccess { bundle ->
                applyBundle(bundle)
                logger.info("Loaded weapon content from {}", CONTENT_RESOURCE)
            }
            .onFailure { throwable ->
                logger.error("Failed to load weapon content from {}. Using built-in defaults.", CONTENT_RESOURCE, throwable)
            }
    }

    fun weaponDefinitions(): List<WeaponDefinition> = weapons.values.sortedBy(WeaponDefinition::id)

    fun requireWeapon(id: String): WeaponDefinition {
        return weapons[id] ?: error("Unknown weapon definition: $id")
    }

    fun definitionByFragmentId(fragmentItemId: String): WeaponDefinition? {
        return weapons.values.firstOrNull { it.craft.fragmentItemId == fragmentItemId }
    }

    private fun loadBundledContent(): WeaponContentBundle {
        val stream = WeaponContentRegistry::class.java.classLoader.getResourceAsStream(CONTENT_RESOURCE)
            ?: error("Missing resource: $CONTENT_RESOURCE")
        InputStreamReader(stream).use { reader ->
            val json = JsonParser.parseReader(reader)
            return WeaponContentBundle.CODEC.parse(JsonOps.INSTANCE, json)
                .getOrThrow { message -> IllegalArgumentException("Invalid weapon content: $message") }
        }
    }

    private fun applyBundle(bundle: WeaponContentBundle) {
        val weaponMap = bundle.weaponDefinitions.associateBy(WeaponDefinition::id)
        require(weaponMap.size == bundle.weaponDefinitions.size) { "Duplicate weapon ids found in content bundle" }
        for (definition in bundle.weaponDefinitions) {
            require(definition.maxBaseLevel >= 1) { "Weapon '${definition.id}' maxBaseLevel must be >= 1" }
            require(definition.maxSkillLevel >= 1) { "Weapon '${definition.id}' maxSkillLevel must be >= 1" }
            require(definition.craft.fragmentsRequired >= 0) { "Weapon '${definition.id}' has negative fragmentsRequired" }
            require(definition.upgrades.baseFragmentCost >= 0) { "Weapon '${definition.id}' has negative baseFragmentCost" }
            require(definition.upgrades.skillArtifactCountPerLevel >= 0) { "Weapon '${definition.id}' has negative skillArtifactCountPerLevel" }
            if (definition.attackCurve.isNotEmpty()) {
                val sorted = definition.attackCurve.sortedBy(WeaponAttackCurvePoint::level)
                require(sorted == definition.attackCurve) { "Weapon '${definition.id}' attackCurve must be sorted by level" }
                require(sorted.first().level == 1) { "Weapon '${definition.id}' attackCurve must start at level 1" }
                require(sorted.last().level == definition.maxBaseLevel) {
                    "Weapon '${definition.id}' attackCurve must end at maxBaseLevel ${definition.maxBaseLevel}"
                }
            }
        }
        weapons = weaponMap
    }

    private fun defaultBundle(): WeaponContentBundle {
        return WeaponContentBundle(
            weaponDefinitions = listOf(
                WeaponDefinition(
                    id = "rondo_melody",
                    baseItemId = "minecraft:wooden_sword",
                    baseAttackDamage = 7.0,
                    attackDamagePerLevel = 0.18,
                    totalAttackSpeed = 1.6,
                    maxBaseLevel = 60,
                    maxSkillLevel = 10,
                    skill = WeaponSkillDefinition(
                        effectId = "shield",
                        durationSeconds = 15,
                        cooldownSeconds = 20,
                        baseValue = 3.0,
                        valuePerLevel = 0.5
                    ),
                    upgrades = WeaponUpgradeDefinition(
                        baseCscQuadraticCoefficient = 100,
                        skillCscLinearCoefficient = 10_000,
                        baseFragmentCost = 1
                    ),
                    craft = WeaponCraftDefinition(
                        fragmentItemId = "rondo_melody_fragment",
                        fragmentBaseItemId = "minecraft:prismarine_shard",
                        fragmentsRequired = 8,
                        craftedRarity = WeaponRarity.TWO_STAR,
                        craftedBaseLevel = 1,
                        craftedSkillLevel = 1
                    ),
                    drops = fragmentFieldDrops()
                ),
                WeaponDefinition(
                    id = "masquerade_invitation",
                    baseItemId = "minecraft:wooden_sword",
                    baseAttackDamage = 7.0,
                    attackDamagePerLevel = 0.18,
                    totalAttackSpeed = 1.6,
                    maxBaseLevel = 60,
                    maxSkillLevel = 10,
                    skill = WeaponSkillDefinition(
                        effectId = "heal",
                        durationSeconds = 0,
                        cooldownSeconds = 30,
                        baseValue = 2.0,
                        valuePerLevel = 0.5
                    ),
                    upgrades = WeaponUpgradeDefinition(
                        baseCscQuadraticCoefficient = 100,
                        skillCscLinearCoefficient = 10_000,
                        baseFragmentCost = 1
                    ),
                    craft = WeaponCraftDefinition(
                        fragmentItemId = "masquerade_invitation_fragment",
                        fragmentBaseItemId = "minecraft:amethyst_shard",
                        fragmentsRequired = 8,
                        craftedRarity = WeaponRarity.TWO_STAR,
                        craftedBaseLevel = 1,
                        craftedSkillLevel = 1
                    ),
                    drops = fragmentFieldDrops()
                ),
                WeaponDefinition(
                    id = "lakeside_stride",
                    baseItemId = "minecraft:diamond_sword",
                    baseAttackDamage = 7.0,
                    attackDamagePerLevel = 0.0,
                    totalAttackSpeed = 1.6,
                    maxBaseLevel = 60,
                    maxSkillLevel = 10,
                    critRateBonusPercent = 10.0,
                    attackCurve = listOf(
                        WeaponAttackCurvePoint(1, 7.0),
                        WeaponAttackCurvePoint(5, 8.0),
                        WeaponAttackCurvePoint(10, 9.5),
                        WeaponAttackCurvePoint(30, 13.5),
                        WeaponAttackCurvePoint(50, 18.0),
                        WeaponAttackCurvePoint(60, 22.0)
                    ),
                    skill = WeaponSkillDefinition(
                        effectId = "current_hp_true_damage",
                        durationSeconds = 0,
                        cooldownSeconds = 50,
                        baseValue = 0.0,
                        valuePerLevel = 5.0,
                        radiusMeters = 5.0
                    ),
                    upgrades = WeaponUpgradeDefinition(
                        baseCscQuadraticCoefficient = 100,
                        skillCscLinearCoefficient = 0,
                        skillCscQuadraticCoefficient = 10_000,
                        baseFragmentCost = 1,
                        skillArtifactMinRarity = EquipmentRarity.FOUR_STAR,
                        skillArtifactCountPerLevel = 1
                    ),
                    craft = WeaponCraftDefinition(
                        fragmentItemId = "lakeside_stride_fragment",
                        fragmentBaseItemId = "minecraft:prismarine_crystals",
                        fragmentsRequired = 8,
                        craftedRarity = WeaponRarity.FIVE_STAR,
                        craftedBaseLevel = 1,
                        craftedSkillLevel = 1
                    ),
                    drops = resonanceOnlyDrops()
                ),
                WeaponDefinition(
                    id = "hanwu_juanxue",
                    baseItemId = "minecraft:diamond_sword",
                    baseAttackDamage = 8.0,
                    attackDamagePerLevel = 0.0,
                    totalAttackSpeed = 1.6,
                    maxBaseLevel = 60,
                    maxSkillLevel = 10,
                    damageType = CombatDamageType.ARCANE,
                    attackCurve = listOf(
                        WeaponAttackCurvePoint(1, 8.0),
                        WeaponAttackCurvePoint(5, 9.5),
                        WeaponAttackCurvePoint(10, 11.0),
                        WeaponAttackCurvePoint(30, 15.5),
                        WeaponAttackCurvePoint(50, 21.5),
                        WeaponAttackCurvePoint(60, 26.0)
                    ),
                    skill = WeaponSkillDefinition(
                        effectId = "snow_frost",
                        durationSeconds = 10,
                        cooldownSeconds = 25,
                        baseValue = 1.0,
                        valuePerLevel = 0.3
                    ),
                    upgrades = WeaponUpgradeDefinition(
                        baseCscQuadraticCoefficient = 100,
                        skillCscLinearCoefficient = 0,
                        skillCscQuadraticCoefficient = 10_000,
                        baseFragmentCost = 1,
                        skillArtifactMinRarity = EquipmentRarity.FOUR_STAR,
                        skillArtifactCountPerLevel = 1
                    ),
                    craft = WeaponCraftDefinition(
                        fragmentItemId = "hanwu_juanxue_fragment",
                        fragmentBaseItemId = "minecraft:snowball",
                        fragmentsRequired = 8,
                        craftedRarity = WeaponRarity.FIVE_STAR,
                        craftedBaseLevel = 1,
                        craftedSkillLevel = 1
                    ),
                    drops = resonanceOnlyDrops()
                ),
                WeaponDefinition(
                    id = "kyokusui_no_ryusho",
                    baseItemId = "minecraft:diamond_sword",
                    baseAttackDamage = 8.0,
                    attackDamagePerLevel = 0.0,
                    totalAttackSpeed = 1.6,
                    maxBaseLevel = 60,
                    maxSkillLevel = 1,
                    attackCurve = listOf(
                        WeaponAttackCurvePoint(1, 8.0),
                        WeaponAttackCurvePoint(5, 9.0),
                        WeaponAttackCurvePoint(10, 10.5),
                        WeaponAttackCurvePoint(30, 14.5),
                        WeaponAttackCurvePoint(50, 20.0),
                        WeaponAttackCurvePoint(60, 24.0)
                    ),
                    skill = WeaponSkillDefinition(
                        effectId = "orchid_pavilion_echo",
                        durationSeconds = 16,
                        cooldownSeconds = 25,
                        baseValue = 0.0,
                        valuePerLevel = 0.0,
                        radiusMeters = 5.0,
                        tickIntervalSeconds = 2.0
                    ),
                    upgrades = WeaponUpgradeDefinition(
                        baseCscQuadraticCoefficient = 100,
                        skillCscLinearCoefficient = 0,
                        baseFragmentCost = 1
                    ),
                    craft = WeaponCraftDefinition(
                        fragmentItemId = "kyokusui_no_ryusho_fragment",
                        fragmentBaseItemId = "minecraft:book",
                        fragmentsRequired = 8,
                        craftedRarity = WeaponRarity.FIVE_STAR,
                        craftedBaseLevel = 1,
                        craftedSkillLevel = 1
                    ),
                    drops = resonanceOnlyDrops()
                ),
                WeaponDefinition(
                    id = "requiem_toward_dawn",
                    baseItemId = "minecraft:iron_sword",
                    baseAttackDamage = 8.5,
                    attackDamagePerLevel = 0.20,
                    totalAttackSpeed = 1.6,
                    maxBaseLevel = 60,
                    maxSkillLevel = 10,
                    maxAllDamageBonusPercent = 10.0,
                    damageType = CombatDamageType.ARCANE,
                    skill = WeaponSkillDefinition(
                        effectId = "flame_aura",
                        durationSeconds = 10,
                        cooldownSeconds = 25,
                        baseValue = 3.0,
                        valuePerLevel = 1.0,
                        radiusMeters = 4.0
                    ),
                    upgrades = WeaponUpgradeDefinition(
                        baseCscQuadraticCoefficient = 100,
                        skillCscLinearCoefficient = 10_000,
                        baseFragmentCost = 1
                    ),
                    craft = WeaponCraftDefinition(
                        fragmentItemId = "requiem_toward_dawn_fragment",
                        fragmentBaseItemId = "minecraft:iron_ingot",
                        fragmentsRequired = 8,
                        craftedRarity = WeaponRarity.FOUR_STAR,
                        craftedBaseLevel = 1,
                        craftedSkillLevel = 1
                    ),
                    drops = resonanceOnlyDrops()
                ),
                WeaponDefinition(
                    id = "gaoshan_liushui",
                    baseItemId = "minecraft:iron_sword",
                    baseAttackDamage = 8.5,
                    attackDamagePerLevel = 0.20,
                    totalAttackSpeed = 1.6,
                    maxBaseLevel = 60,
                    maxSkillLevel = 10,
                    skill = WeaponSkillDefinition(
                        effectId = "healing_aura",
                        durationSeconds = 8,
                        cooldownSeconds = 20,
                        baseValue = 2.0,
                        valuePerLevel = 0.5,
                        radiusMeters = 5.0,
                        secondaryBaseValue = 0.75,
                        secondaryValuePerLevel = 0.25,
                        tickIntervalSeconds = 2.0
                    ),
                    upgrades = WeaponUpgradeDefinition(
                        baseCscQuadraticCoefficient = 100,
                        skillCscLinearCoefficient = 10_000,
                        baseFragmentCost = 1
                    ),
                    craft = WeaponCraftDefinition(
                        fragmentItemId = "gaoshan_liushui_fragment",
                        fragmentBaseItemId = "minecraft:lapis_lazuli",
                        fragmentsRequired = 8,
                        craftedRarity = WeaponRarity.FOUR_STAR,
                        craftedBaseLevel = 1,
                        craftedSkillLevel = 1
                    ),
                    drops = resonanceOnlyDrops()
                ),
                WeaponDefinition(
                    id = "dark_lux",
                    baseItemId = "minecraft:netherite_sword",
                    baseAttackDamage = 8.5,
                    attackDamagePerLevel = 0.20,
                    totalAttackSpeed = 1.6,
                    maxBaseLevel = 60,
                    maxSkillLevel = 10,
                    damageType = CombatDamageType.PHYSICAL,
                    skill = WeaponSkillDefinition(
                        effectId = "dark_lux",
                        durationSeconds = 30,
                        cooldownSeconds = 30,
                        baseValue = 0.0,
                        valuePerLevel = 0.0
                    ),
                    upgrades = WeaponUpgradeDefinition(
                        baseCscQuadraticCoefficient = 100,
                        skillCscLinearCoefficient = 0,
                        skillCscQuadraticCoefficient = 10_000,
                        baseFragmentCost = 1,
                        skillArtifactMinRarity = EquipmentRarity.FOUR_STAR,
                        skillArtifactCountPerLevel = 1
                    ),
                    craft = WeaponCraftDefinition(
                        fragmentItemId = "dark_lux_fragment",
                        fragmentBaseItemId = "minecraft:echo_shard",
                        fragmentsRequired = 8,
                        craftedRarity = WeaponRarity.FIVE_STAR,
                        craftedBaseLevel = 1,
                        craftedSkillLevel = 1
                    ),
                    drops = resonanceOnlyDrops()
                ),
                WeaponDefinition(
                    id = "pastoral_flute_reverie",
                    baseItemId = "minecraft:wooden_sword",
                    baseAttackDamage = 7.0,
                    attackDamagePerLevel = 0.18,
                    totalAttackSpeed = 1.6,
                    maxBaseLevel = 60,
                    maxSkillLevel = 10,
                    damageType = CombatDamageType.PHYSICAL,
                    skill = WeaponSkillDefinition(
                        effectId = "sunlit_haste",
                        durationSeconds = 50,
                        cooldownSeconds = 50,
                        baseValue = 40.0,
                        valuePerLevel = 0.0,
                        secondaryBaseValue = 60.0,
                        secondaryValuePerLevel = 0.0
                    ),
                    upgrades = WeaponUpgradeDefinition(
                        baseCscQuadraticCoefficient = 100,
                        skillCscLinearCoefficient = 10_000,
                        baseFragmentCost = 1
                    ),
                    craft = WeaponCraftDefinition(
                        fragmentItemId = "pastoral_flute_reverie_fragment",
                        fragmentBaseItemId = "minecraft:bamboo",
                        fragmentsRequired = 8,
                        craftedRarity = WeaponRarity.THREE_STAR,
                        craftedBaseLevel = 1,
                        craftedSkillLevel = 1
                    ),
                    drops = resonanceOnlyDrops()
                ),
                WeaponDefinition(
                    id = "cadenza_allegro",
                    baseItemId = "minecraft:wooden_sword",
                    baseAttackDamage = 8.5,
                    attackDamagePerLevel = 0.20,
                    totalAttackSpeed = 1.6,
                    maxBaseLevel = 80,
                    maxSkillLevel = 1,
                    skill = WeaponSkillDefinition(
                        effectId = "baa_mimic",
                        durationSeconds = 0,
                        cooldownSeconds = 15,
                        baseValue = 1.0,
                        valuePerLevel = 0.0,
                        radiusMeters = 10.0
                    ),
                    upgrades = WeaponUpgradeDefinition(
                        baseCscQuadraticCoefficient = 100,
                        skillCscLinearCoefficient = 0,
                        baseFragmentCost = 1
                    ),
                    craft = WeaponCraftDefinition(
                        fragmentItemId = "cadenza_allegro_fragment",
                        fragmentBaseItemId = "minecraft:white_wool",
                        fragmentsRequired = 8,
                        craftedRarity = WeaponRarity.FOUR_STAR,
                        craftedBaseLevel = 1,
                        craftedSkillLevel = 1
                    ),
                    drops = resonanceOnlyDrops()
                ),
                WeaponDefinition(
                    id = "dummy_four_star_b",
                    baseItemId = "minecraft:golden_sword",
                    baseAttackDamage = 8.5,
                    attackDamagePerLevel = 0.20,
                    totalAttackSpeed = 1.6,
                    maxBaseLevel = 60,
                    maxSkillLevel = 1,
                    skill = WeaponSkillDefinition(
                        effectId = "none",
                        durationSeconds = 0,
                        cooldownSeconds = 0,
                        baseValue = 0.0,
                        valuePerLevel = 0.0
                    ),
                    upgrades = WeaponUpgradeDefinition(
                        baseCscQuadraticCoefficient = 100,
                        skillCscLinearCoefficient = 0,
                        baseFragmentCost = 1
                    ),
                    craft = WeaponCraftDefinition(
                        fragmentItemId = "dummy_four_star_b_fragment",
                        fragmentBaseItemId = "minecraft:gold_ingot",
                        fragmentsRequired = 8,
                        craftedRarity = WeaponRarity.FOUR_STAR,
                        craftedBaseLevel = 1,
                        craftedSkillLevel = 1
                    ),
                    drops = resonanceOnlyDrops()
                )
            )
        )
    }

    private fun fragmentFieldDrops(): WeaponDropDefinition {
        return WeaponDropDefinition(
            fragmentDrop = WeaponFragmentDropDefinition(
                minMobLevel = 5,
                chance = 0.08,
                minCount = 1,
                maxCount = 1,
                bonusCountAtLevel70 = 1
            ),
            directDropTiers = emptyList(),
            directDropBaseLevelMultiplier = 0.0,
            fiveStarChanceMultiplierAtLevel70 = 1.0
        )
    }

    private fun resonanceOnlyDrops(): WeaponDropDefinition {
        return WeaponDropDefinition(
            fragmentDrop = WeaponFragmentDropDefinition(
                minMobLevel = 999,
                chance = 0.0,
                minCount = 0,
                maxCount = 0,
                bonusCountAtLevel70 = 0
            ),
            directDropTiers = emptyList(),
            directDropBaseLevelMultiplier = 0.0,
            fiveStarChanceMultiplierAtLevel70 = 1.0
        )
    }
}
