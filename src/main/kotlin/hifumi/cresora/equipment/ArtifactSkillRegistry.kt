package hifumi.cresora.equipment

import net.minecraft.server.network.ServerPlayerEntity
import org.slf4j.LoggerFactory

interface ArtifactSkillHandler {
    fun onEquipChanged(player: ServerPlayerEntity) {}
    fun onAttackDealt(player: ServerPlayerEntity, target: net.minecraft.entity.LivingEntity, damage: Double) {}
    fun onDamageTaken(player: ServerPlayerEntity, source: net.minecraft.entity.damage.DamageSource, damage: Double) {}
    fun onKill(player: ServerPlayerEntity, target: net.minecraft.entity.LivingEntity) {}
    fun onTick(player: ServerPlayerEntity) {}
    
    // For transient state management if needed in the future
    fun clearTransientState(playerId: java.util.UUID) {}
    fun pruneTransientState(activePlayerIds: Set<java.util.UUID>) {}

    fun getAttackDamageScalar(player: ServerPlayerEntity): Double = 0.0
    fun getArmorScalar(player: ServerPlayerEntity): Double = 0.0
    fun getCritRateBonus(player: ServerPlayerEntity): Double = 0.0
    fun getCritDamageBonus(player: ServerPlayerEntity): Double = 0.0
}

object ArtifactSkillRegistry {
    private val logger = LoggerFactory.getLogger("cresora-utilities/artifact-skill-registry")
    private val handlers = mutableMapOf<String, ArtifactSkillHandler>()

    init {
        // All artifacts are now registered through the compiler-generated registry
        runCatching {
            val clazz = Class.forName("hifumi.cresora.equipment.generated.CompiledArtifactRegistry")
            val method = clazz.getMethod("registerAll", ArtifactSkillRegistry::class.java)
            method.invoke(null, this)
        }.onFailure { throwable ->
            logger.error("Failed to initialize CompiledArtifactRegistry", throwable)
        }
    }

    fun init() {
        // Just to trigger the init block
    }

    fun register(effectId: String, handler: ArtifactSkillHandler) {
        handlers[effectId] = handler
    }

    fun getHandler(effectId: String): ArtifactSkillHandler? {
        return handlers[effectId]
    }
}

