package hifumi.cresora

import hifumi.cresora.resonance.ResonanceCurrencyType
import hifumi.cresora.resonance.ResonanceService
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents
import net.minecraft.text.Text
import java.time.LocalDate

object Join {
    fun init() {
        ServerPlayConnectionEvents.JOIN.register { handler, _, _ ->
            val player = handler.player
            player.sendMessage(
                Text.translatable("cresora.welcome.message", player.displayName)
            )
            val today = LocalDate.now().toEpochDay()
            if (ResonanceService.getLastDailyLoginEpochDay(player) != today) {
                ResonanceService.setLastDailyLoginEpochDay(player, today)
                ResonanceService.addCurrency(player, ResonanceCurrencyType.CHORD_PROGRESSION, 650)
                ResonanceService.addCurrency(player, ResonanceCurrencyType.SUBSTITUTE_CHORD, 325)
                player.sendMessage(Text.translatable("message.cresora.daily_login.reward"), false)
            }
        }
    }
}
