package hifumi.cresora.debuff
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents

object CresoraDebuffHooks {
    fun init() {
        ServerTickEvents.END_SERVER_TICK.register { server ->
            server.playerManager.playerList.forEach { player ->
                CresoraDebuffService.tick(player)
            }
        }
    }
}
