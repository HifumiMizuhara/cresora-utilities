package hifumi.cresora

import net.minecraft.server.network.ServerPlayerEntity
import java.util.UUID

object CombatFeedbackService {
    private data class PendingCrit(
        val critMultiplier: Double,
        val expireTick: Long
    )

    private val pendingCrits: MutableMap<UUID, PendingCrit> = mutableMapOf()

    fun recordCrit(player: ServerPlayerEntity, critMultiplier: Double) {
        pendingCrits[player.uuid] = PendingCrit(
            critMultiplier = critMultiplier,
            expireTick = player.world.time + 4L
        )
    }

    fun consumeCrit(player: ServerPlayerEntity): Double? {
        val pending = pendingCrits[player.uuid] ?: return null
        pendingCrits.remove(player.uuid)
        return if (player.world.time <= pending.expireTick) {
            pending.critMultiplier
        } else {
            null
        }
    }
}
