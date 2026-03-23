package hifumi.cresora

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents
import net.minecraft.entity.mob.HostileEntity
import net.minecraft.server.network.ServerPlayerEntity

object AdventureRankHooks {
    fun init() {
        ServerLivingEntityEvents.AFTER_DEATH.register(ServerLivingEntityEvents.AfterDeath { entity, damageSource ->
            val hostile = entity as? HostileEntity ?: return@AfterDeath
            val killer = damageSource.attacker as? ServerPlayerEntity ?: return@AfterDeath
            AdventureRankService.addXp(killer, AdventureRankService.hostileKillXp(hostile))
        })

        ServerEntityEvents.ENTITY_LOAD.register(ServerEntityEvents.Load { entity, world ->
            val hostile = entity as? HostileEntity ?: return@Load
            val rank = AdventureRankService.getOrAssignMobRank(hostile, world)
            AdventureRankService.applyMobScaling(hostile, rank)
        })

        ServerPlayerEvents.COPY_FROM.register(ServerPlayerEvents.CopyFrom { oldPlayer, newPlayer, _ ->
            AdventureRankService.copyTo(oldPlayer, newPlayer)
        })
    }
}
