package hifumi.cresora.equipment

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents
import net.minecraft.server.network.ServerPlayerEntity
import org.slf4j.LoggerFactory
import java.util.UUID

interface ArtifactSkillHandler {
    fun onEquipChanged(player: ServerPlayerEntity) {}
    fun onAttackDealt(player: ServerPlayerEntity, target: net.minecraft.entity.LivingEntity, damage: Double) {}
    fun onDamageTaken(player: ServerPlayerEntity, source: net.minecraft.entity.damage.DamageSource, damage: Double) {}
    fun onKill(player: ServerPlayerEntity, target: net.minecraft.entity.LivingEntity) {}
    fun onTick(player: ServerPlayerEntity) {}
    fun onTransientStateTick(player: ServerPlayerEntity) {}

    fun clearTransientState(playerId: UUID) {}
    fun pruneTransientState(activePlayerIds: Set<UUID>) {}

    fun getAttackDamageScalar(player: ServerPlayerEntity): Double = 0.0
    fun getArmorScalar(player: ServerPlayerEntity): Double = 0.0
    fun getCritRateBonus(player: ServerPlayerEntity): Double = 0.0
    fun getCritDamageBonus(player: ServerPlayerEntity): Double = 0.0
    fun getAllDamageBonus(player: ServerPlayerEntity): Double = 0.0
    fun getDisplayStackBonus(player: ServerPlayerEntity, buffId: String): Int = 0
}

object ArtifactSkillRegistry {
    private val logger = LoggerFactory.getLogger("cresora-utilities/artifact-skill-registry")
    private val handlers = mutableMapOf<String, ArtifactSkillHandler>()
    private var lifecycleHooksRegistered = false

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
        if (lifecycleHooksRegistered) return
        lifecycleHooksRegistered = true

        ServerPlayConnectionEvents.DISCONNECT.register(ServerPlayConnectionEvents.Disconnect { handler, _ ->
            clearTransientState(handler.player.uuid)
        })
        ServerTickEvents.END_SERVER_TICK.register { server ->
            val onlinePlayers = server.playerManager.playerList
            val activePlayerIds = onlinePlayers.mapTo(linkedSetOf(), ServerPlayerEntity::getUuid)
            handlers.values.toSet().forEach { artifactHandler ->
                artifactHandler.pruneTransientState(activePlayerIds)
                onlinePlayers.forEach(artifactHandler::onTransientStateTick)
            }
        }
    }

    fun register(effectId: String, handler: ArtifactSkillHandler) {
        handlers[effectId] = handler
    }

    fun getHandler(effectId: String): ArtifactSkillHandler? {
        return handlers[effectId]
    }

    fun clearTransientState(playerId: UUID) {
        handlers.values.toSet().forEach { it.clearTransientState(playerId) }
    }
}
