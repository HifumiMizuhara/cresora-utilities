package hifumi.cresora

import net.fabricmc.fabric.api.event.player.AttackBlockCallback
import net.fabricmc.fabric.api.event.player.AttackEntityCallback
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.util.ActionResult

object HotbarOverrideHooks {
    fun init() {
        AttackBlockCallback.EVENT.register { player, world, hand, pos, direction ->
            if (player is ServerPlayerEntity && HotbarOverrideService.isOverridden(player)) {
                ActionResult.FAIL
            } else {
                ActionResult.PASS
            }
        }

        AttackEntityCallback.EVENT.register { player, world, hand, entity, hitResult ->
            if (player is ServerPlayerEntity && HotbarOverrideService.isOverridden(player)) {
                ActionResult.FAIL
            } else {
                ActionResult.PASS
            }
        }
    }
}
