package hifumi.cresora

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.text.Text
import java.util.Collections
import java.util.WeakHashMap
import net.minecraft.server.network.ServerPlayerEntity
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

object Join {
    private val authenticatedPlayers = Collections.newSetFromMap(WeakHashMap< ServerPlayerEntity, Boolean>())
    fun init() {
        ServerPlayConnectionEvents.JOIN.register { handler, sender, server ->
            // handlerからプレイヤーのインスタンスを取得
            val player = handler.player
            player.sendMessage(
                Text.translatable("cresora.welcome.message", player.displayName)
            )
        }
    }
}