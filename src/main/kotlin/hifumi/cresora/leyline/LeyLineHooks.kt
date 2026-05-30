package hifumi.cresora.leyline

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents

object LeyLineHooks {
    fun init() {
        ServerTickEvents.END_SERVER_TICK.register(ServerTickEvents.EndTick { server ->
            LeyLineService.tick(server)
        })
    }
}
