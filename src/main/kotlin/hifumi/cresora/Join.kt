package hifumi.cresora

import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents
import net.minecraft.text.Text

object Join {
    fun init() {
        ServerPlayConnectionEvents.JOIN.register { handler, sender, server ->
            val player = handler.player
            player.sendMessage(
                Text.translatable("cresora.welcome.message", player.displayName)
            )
        }
    }
}
