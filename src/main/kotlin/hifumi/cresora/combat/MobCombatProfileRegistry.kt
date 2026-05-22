package hifumi.cresora.combat
import hifumi.cresora.CreSoraUtilities
import com.google.gson.JsonParser
import com.mojang.serialization.Codec
import com.mojang.serialization.JsonOps
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.entity.EntityType
import net.minecraft.registry.Registries
import net.minecraft.util.Identifier
import org.slf4j.LoggerFactory
import java.io.InputStreamReader

data class MobCombatProfileDefinition(
    val entityTypeId: String,
    val attackType: CombatDamageType = CombatDamageType.PHYSICAL,
    val physicalResistancePercent: Double = 0.0,
    val arcaneResistancePercent: Double = 0.0
) {
    companion object {
        val CODEC: Codec<MobCombatProfileDefinition> = RecordCodecBuilder.create { instance ->
            instance.group(
                Codec.STRING.fieldOf("entityTypeId").forGetter(MobCombatProfileDefinition::entityTypeId),
                CombatDamageType.CODEC.optionalFieldOf("attackType", CombatDamageType.PHYSICAL).forGetter(MobCombatProfileDefinition::attackType),
                Codec.DOUBLE.optionalFieldOf("physicalResistancePercent", 0.0).forGetter(MobCombatProfileDefinition::physicalResistancePercent),
                Codec.DOUBLE.optionalFieldOf("arcaneResistancePercent", 0.0).forGetter(MobCombatProfileDefinition::arcaneResistancePercent)
            ).apply(instance, ::MobCombatProfileDefinition)
        }
    }
}

data class MobCombatContentBundle(
    val profiles: List<MobCombatProfileDefinition>
) {
    companion object {
        val CODEC: Codec<MobCombatContentBundle> = RecordCodecBuilder.create { instance ->
            instance.group(
                MobCombatProfileDefinition.CODEC.listOf().fieldOf("profiles").forGetter(MobCombatContentBundle::profiles)
            ).apply(instance, ::MobCombatContentBundle)
        }
    }
}

object MobCombatProfileRegistry {
    private const val CONTENT_RESOURCE = "data/cresora-utilities/cresora/mob_combat_content.json"
    private const val MAX_STANDARD_RESISTANCE_PERCENT = 75.0

    private val logger = LoggerFactory.getLogger("${CreSoraUtilities.MOD_ID}/mob-combat-content")

    @Volatile
    private var profiles: Map<EntityType<*>, MobCombatProfileDefinition> = emptyMap()

    fun init() {
        applyBundle(defaultBundle())
        runCatching { loadBundledContent() }
            .onSuccess { bundle ->
                applyBundle(bundle)
                logger.info("Loaded mob combat profiles from {}", CONTENT_RESOURCE)
            }
            .onFailure { throwable ->
                logger.error("Failed to load mob combat profiles from {}. Using built-in defaults.", CONTENT_RESOURCE, throwable)
            }
    }

    fun profile(entityType: EntityType<*>): MobCombatProfileDefinition? = profiles[entityType]

    fun attackType(entityType: EntityType<*>): CombatDamageType {
        return profiles[entityType]?.attackType ?: CombatDamageType.PHYSICAL
    }

    fun resistancePercent(entityType: EntityType<*>, damageType: CombatDamageType): Double {
        val profile = profiles[entityType] ?: return 0.0
        val raw = when (damageType) {
            CombatDamageType.PHYSICAL -> profile.physicalResistancePercent
            CombatDamageType.ARCANE -> profile.arcaneResistancePercent
        }
        return raw.coerceIn(0.0, MAX_STANDARD_RESISTANCE_PERCENT)
    }

    private fun loadBundledContent(): MobCombatContentBundle {
        val stream = MobCombatProfileRegistry::class.java.classLoader.getResourceAsStream(CONTENT_RESOURCE)
            ?: error("Missing resource: $CONTENT_RESOURCE")
        InputStreamReader(stream).use { reader ->
            val json = JsonParser.parseReader(reader)
            return MobCombatContentBundle.CODEC.parse(JsonOps.INSTANCE, json)
                .getOrThrow { message -> IllegalArgumentException("Invalid mob combat content: $message") }
        }
    }

    private fun applyBundle(bundle: MobCombatContentBundle) {
        val mapped = linkedMapOf<EntityType<*>, MobCombatProfileDefinition>()
        for (profile in bundle.profiles) {
            require(profile.entityTypeId.isNotBlank()) { "Mob combat profile entityTypeId must not be blank" }
            require(profile.physicalResistancePercent in 0.0..MAX_STANDARD_RESISTANCE_PERCENT) {
                "Mob combat profile '${profile.entityTypeId}' physicalResistancePercent must be within 0..$MAX_STANDARD_RESISTANCE_PERCENT"
            }
            require(profile.arcaneResistancePercent in 0.0..MAX_STANDARD_RESISTANCE_PERCENT) {
                "Mob combat profile '${profile.entityTypeId}' arcaneResistancePercent must be within 0..$MAX_STANDARD_RESISTANCE_PERCENT"
            }
            val entityTypeId = Identifier.of(profile.entityTypeId)
            val entityType = Registries.ENTITY_TYPE.get(entityTypeId)
            require(Registries.ENTITY_TYPE.getId(entityType) == entityTypeId) {
                "Unknown entity type '${profile.entityTypeId}' in mob combat profile content"
            }
            require(!mapped.containsKey(entityType)) { "Duplicate mob combat profile for '${profile.entityTypeId}'" }
            mapped[entityType] = profile
        }
        profiles = mapped
    }

    private fun defaultBundle(): MobCombatContentBundle {
        fun profile(
            entityType: EntityType<*>,
            attackType: CombatDamageType,
            physicalResistancePercent: Double,
            arcaneResistancePercent: Double
        ): MobCombatProfileDefinition {
            return MobCombatProfileDefinition(
                entityTypeId = Registries.ENTITY_TYPE.getId(entityType).toString(),
                attackType = attackType,
                physicalResistancePercent = physicalResistancePercent,
                arcaneResistancePercent = arcaneResistancePercent
            )
        }

        return MobCombatContentBundle(
            profiles = listOf(
                profile(EntityType.ZOMBIE, CombatDamageType.PHYSICAL, 5.0, 0.0),
                profile(EntityType.DROWNED, CombatDamageType.PHYSICAL, 5.0, 0.0),
                profile(EntityType.HUSK, CombatDamageType.PHYSICAL, 5.0, 0.0),
                profile(EntityType.SPIDER, CombatDamageType.PHYSICAL, 5.0, 0.0),
                profile(EntityType.CAVE_SPIDER, CombatDamageType.PHYSICAL, 5.0, 0.0),
                profile(EntityType.SLIME, CombatDamageType.PHYSICAL, 5.0, 0.0),
                profile(EntityType.SILVERFISH, CombatDamageType.PHYSICAL, 5.0, 0.0),
                profile(EntityType.ENDERMITE, CombatDamageType.PHYSICAL, 5.0, 0.0),
                profile(EntityType.SKELETON, CombatDamageType.PHYSICAL, 8.0, 5.0),
                profile(EntityType.STRAY, CombatDamageType.PHYSICAL, 8.0, 5.0),
                profile(EntityType.CREEPER, CombatDamageType.PHYSICAL, 8.0, 5.0),
                profile(EntityType.PHANTOM, CombatDamageType.PHYSICAL, 8.0, 5.0),
                profile(EntityType.PILLAGER, CombatDamageType.PHYSICAL, 8.0, 5.0),
                profile(EntityType.BREEZE, CombatDamageType.PHYSICAL, 8.0, 5.0),
                profile(EntityType.ZOMBIFIED_PIGLIN, CombatDamageType.PHYSICAL, 8.0, 5.0),
                profile(EntityType.WITCH, CombatDamageType.ARCANE, 0.0, 20.0),
                profile(EntityType.BLAZE, CombatDamageType.ARCANE, 0.0, 20.0),
                profile(EntityType.MAGMA_CUBE, CombatDamageType.ARCANE, 0.0, 20.0),
                profile(EntityType.GHAST, CombatDamageType.ARCANE, 0.0, 20.0),
                profile(EntityType.GUARDIAN, CombatDamageType.ARCANE, 0.0, 20.0),
                profile(EntityType.EVOKER, CombatDamageType.ARCANE, 0.0, 20.0),
                profile(EntityType.VEX, CombatDamageType.ARCANE, 0.0, 20.0),
                profile(EntityType.ENDERMAN, CombatDamageType.PHYSICAL, 15.0, 10.0),
                profile(EntityType.HOGLIN, CombatDamageType.PHYSICAL, 15.0, 10.0),
                profile(EntityType.VINDICATOR, CombatDamageType.PHYSICAL, 15.0, 10.0),
                profile(EntityType.RAVAGER, CombatDamageType.PHYSICAL, 15.0, 10.0),
                profile(EntityType.PIGLIN_BRUTE, CombatDamageType.PHYSICAL, 15.0, 10.0),
                profile(EntityType.ELDER_GUARDIAN, CombatDamageType.ARCANE, 20.0, 20.0),
                profile(EntityType.SHULKER, CombatDamageType.ARCANE, 20.0, 20.0),
                profile(EntityType.WARDEN, CombatDamageType.PHYSICAL, 20.0, 20.0)
            )
        )
    }
}
