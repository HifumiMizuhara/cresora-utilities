package hifumi.cresora

import com.google.gson.JsonParser
import com.mojang.serialization.Codec
import com.mojang.serialization.JsonOps
import com.mojang.serialization.codecs.RecordCodecBuilder
import org.slf4j.LoggerFactory
import java.io.InputStreamReader

data class WeaponSkillDefinition(
    val effectId: String,
    val durationSeconds: Int,
    val cooldownSeconds: Int,
    val shieldBaseHearts: Double,
    val shieldPerLevelHearts: Double
) {
    companion object {
        val CODEC: Codec<WeaponSkillDefinition> = RecordCodecBuilder.create { instance ->
            instance.group(
                Codec.STRING.fieldOf("effectId").forGetter(WeaponSkillDefinition::effectId),
                Codec.INT.fieldOf("durationSeconds").forGetter(WeaponSkillDefinition::durationSeconds),
                Codec.INT.fieldOf("cooldownSeconds").forGetter(WeaponSkillDefinition::cooldownSeconds),
                Codec.DOUBLE.fieldOf("shieldBaseHearts").forGetter(WeaponSkillDefinition::shieldBaseHearts),
                Codec.DOUBLE.fieldOf("shieldPerLevelHearts").forGetter(WeaponSkillDefinition::shieldPerLevelHearts)
            ).apply(instance, ::WeaponSkillDefinition)
        }
    }
}

data class WeaponUpgradeDefinition(
    val baseCscQuadraticCoefficient: Int,
    val skillCscLinearCoefficient: Int,
    val baseFragmentCost: Int
) {
    companion object {
        val CODEC: Codec<WeaponUpgradeDefinition> = RecordCodecBuilder.create { instance ->
            instance.group(
                Codec.INT.fieldOf("baseCscQuadraticCoefficient").forGetter(WeaponUpgradeDefinition::baseCscQuadraticCoefficient),
                Codec.INT.fieldOf("skillCscLinearCoefficient").forGetter(WeaponUpgradeDefinition::skillCscLinearCoefficient),
                Codec.INT.fieldOf("baseFragmentCost").forGetter(WeaponUpgradeDefinition::baseFragmentCost)
            ).apply(instance, ::WeaponUpgradeDefinition)
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
        }
        weapons = weaponMap
    }

    private fun defaultBundle(): WeaponContentBundle {
        return WeaponContentBundle(
            weaponDefinitions = listOf(
                WeaponDefinition(
                    id = "rondo_melody",
                    baseItemId = "minecraft:wooden_sword",
                    baseAttackDamage = 4.0,
                    attackDamagePerLevel = 0.14,
                    totalAttackSpeed = 1.6,
                    maxBaseLevel = 60,
                    maxSkillLevel = 10,
                    skill = WeaponSkillDefinition(
                        effectId = "shield",
                        durationSeconds = 15,
                        cooldownSeconds = 20,
                        shieldBaseHearts = 3.0,
                        shieldPerLevelHearts = 0.5
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
                    drops = WeaponDropDefinition(
                        fragmentDrop = WeaponFragmentDropDefinition(
                            minMobLevel = 5,
                            chance = 0.08,
                            minCount = 1,
                            maxCount = 1,
                            bonusCountAtLevel70 = 1
                        ),
                        directDropTiers = listOf(
                            WeaponDropTier(WeaponRarity.TWO_STAR, 5, 0.012),
                            WeaponDropTier(WeaponRarity.THREE_STAR, 25, 0.008),
                            WeaponDropTier(WeaponRarity.FOUR_STAR, 45, 0.005),
                            WeaponDropTier(WeaponRarity.FIVE_STAR, 65, 0.0025)
                        ),
                        directDropBaseLevelMultiplier = 0.5,
                        fiveStarChanceMultiplierAtLevel70 = 1.5
                    )
                ),
                WeaponDefinition(
                    id = "masquerade_invitation",
                    baseItemId = "minecraft:wooden_sword",
                    baseAttackDamage = 4.0,
                    attackDamagePerLevel = 0.14,
                    totalAttackSpeed = 1.6,
                    maxBaseLevel = 60,
                    maxSkillLevel = 10,
                    skill = WeaponSkillDefinition(
                        effectId = "heal",
                        durationSeconds = 0,
                        cooldownSeconds = 30,
                        shieldBaseHearts = 2.0,
                        shieldPerLevelHearts = 0.5
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
                    drops = WeaponDropDefinition(
                        fragmentDrop = WeaponFragmentDropDefinition(
                            minMobLevel = 5,
                            chance = 0.08,
                            minCount = 1,
                            maxCount = 1,
                            bonusCountAtLevel70 = 1
                        ),
                        directDropTiers = listOf(
                            WeaponDropTier(
                                rarity = WeaponRarity.TWO_STAR,
                                minMobLevel = 5,
                                chance = 0.012
                            ),
                            WeaponDropTier(
                                rarity = WeaponRarity.THREE_STAR,
                                minMobLevel = 25,
                                chance = 0.008
                            ),
                            WeaponDropTier(
                                rarity = WeaponRarity.FOUR_STAR,
                                minMobLevel = 45,
                                chance = 0.005
                            ),
                            WeaponDropTier(
                                rarity = WeaponRarity.FIVE_STAR,
                                minMobLevel = 65,
                                chance = 0.0025
                            )
                        ),
                        directDropBaseLevelMultiplier = 0.5,
                        fiveStarChanceMultiplierAtLevel70 = 1.5
                    )
                )
            )
        )
    }
}
