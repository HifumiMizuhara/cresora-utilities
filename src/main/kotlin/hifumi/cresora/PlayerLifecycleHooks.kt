package hifumi.cresora

import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents

object PlayerLifecycleHooks {
    fun init() {
        ServerPlayConnectionEvents.DISCONNECT.register(ServerPlayConnectionEvents.Disconnect { handler, _ ->
            val player = handler.player
            HotbarOverrideService.clearSession(player)
            WeaponSkillService.clearTransientState(player)
            EquipmentAttributeService.clearTransientState(player)
            CombatFeedbackService.clearTransientState(player)
            CresoraDebuffService.clearTransientState(player)
            TreasureChestService.clearTransientState(player)
            BloodMoonService.clearTransientState(player)
            MasqueradeService.onPlayerDisconnect(player)
            MasqueradeService.clearTransientState(player)
        })
    }
}
