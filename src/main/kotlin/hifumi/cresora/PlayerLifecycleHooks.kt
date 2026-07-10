package hifumi.cresora

import hifumi.cresora.bloodmoon.BloodMoonService
import hifumi.cresora.combat.CombatFeedbackService
import hifumi.cresora.equipment.EquipmentAttributeService
import hifumi.cresora.treasure.TreasureChestService
import hifumi.cresora.weapon.HotbarOverrideService
import hifumi.cresora.weapon.WeaponSkillService
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents

object PlayerLifecycleHooks {
    fun init() {
        ServerPlayConnectionEvents.DISCONNECT.register(ServerPlayConnectionEvents.Disconnect { handler, _ ->
            val player = handler.player
            HotbarOverrideService.clearSession(player)
            WeaponSkillService.clearTransientState(player)
            EquipmentAttributeService.clearTransientState(player)
            CombatFeedbackService.clearTransientState(player)
            TreasureChestService.clearTransientState(player)
            BloodMoonService.clearTransientState(player)
        })
    }
}
