package hifumi.cresora

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.minecraft.server.network.ServerPlayerEntity

object MasqueradeHooks {
    fun init() {
        ServerTickEvents.END_SERVER_TICK.register(ServerTickEvents.EndTick { server ->
            MasqueradeService.tick(server)
        })

        ServerLivingEntityEvents.AFTER_DEATH.register(ServerLivingEntityEvents.AfterDeath { entity, _ ->
            val player = entity as? ServerPlayerEntity ?: return@AfterDeath
            MasqueradeService.onPlayerDeath(player)
        })
    }
}
