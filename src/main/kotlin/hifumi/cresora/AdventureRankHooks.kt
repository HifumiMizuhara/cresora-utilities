package hifumi.cresora

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.minecraft.entity.mob.HostileEntity
import net.minecraft.entity.mob.MobEntity
import net.minecraft.server.network.ServerPlayerEntity

object AdventureRankHooks {
    fun init() {
        ServerLivingEntityEvents.AFTER_DEATH.register(ServerLivingEntityEvents.AfterDeath { entity, damageSource ->
            val killer = damageSource.attacker as? ServerPlayerEntity ?: return@AfterDeath
            when (entity) {
                is HostileEntity -> {
                    AdventureRankService.addXp(killer, AdventureRankService.hostileKillXp(entity))
                    CreditsService.addHostileKillReward(killer, entity)
                    WeaponDropService.onHostileKilled(killer, entity)
                    ArtifactSpecialUpgradeService.tryDropSpecialItems(killer, entity)
                    MoonAltarService.tryDropMoonBrick(killer, entity)
                    EquipmentEffectHookService.onKill(killer, entity)
                }
                is MobEntity -> {
                    CreditsService.addFriendlyKillReward(killer, entity)
                }
            }
        })

        ServerEntityEvents.ENTITY_LOAD.register(ServerEntityEvents.Load { entity, world ->
            val hostile = entity as? HostileEntity ?: return@Load
            val rank = AdventureRankService.getOrAssignMobRank(hostile, world)
            FieldMobPackService.ensureClassification(hostile)
            AdventureRankService.applyMobScaling(hostile, rank)
        })

        ServerPlayerEvents.COPY_FROM.register(ServerPlayerEvents.CopyFrom { oldPlayer, newPlayer, _ ->
            AdventureRankService.copyTo(oldPlayer, newPlayer)
            CreditsService.copyTo(oldPlayer, newPlayer)
            ResonanceService.copyTo(oldPlayer, newPlayer)
            StoryProgressService.copyTo(oldPlayer, newPlayer)
            MasqueradeProgressService.copyTo(oldPlayer, newPlayer)
            MasqueradeService.restoreAfterRespawn(newPlayer)
            EquipmentAttributeService.markForFullHeal(newPlayer)
        })

        ServerTickEvents.END_WORLD_TICK.register(ServerTickEvents.EndWorldTick { world ->
            CombatMobDisplayService.tick(world)
        })
    }
}
