package hifumi.cresora.adventurerank
import hifumi.cresora.CreSoraUtilities
import hifumi.cresora.bloodmoon.MoonAltarService
import hifumi.cresora.bloodmoon.MoonPhaseService
import hifumi.cresora.combat.CombatMobDisplayService
import hifumi.cresora.combat.FieldMobPackService
import hifumi.cresora.credits.CreditsService
import hifumi.cresora.equipment.ArtifactSpecialUpgradeService
import hifumi.cresora.equipment.EquipmentAttributeService
import hifumi.cresora.equipment.EquipmentEffectHookService
import hifumi.cresora.masquerade.MasqueradeProgressService
import hifumi.cresora.masquerade.MasqueradeService
import hifumi.cresora.resonance.ResonanceCurrencyType
import hifumi.cresora.resonance.ResonanceService
import hifumi.cresora.story.StoryProgressService
import hifumi.cresora.weapon.WeaponDropService
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.minecraft.entity.mob.HostileEntity
import net.minecraft.entity.mob.MobEntity
import net.minecraft.entity.mob.Monster
import net.minecraft.server.network.ServerPlayerEntity
import kotlin.math.roundToInt

object AdventureRankHooks {
    fun init() {
        ServerLivingEntityEvents.AFTER_DEATH.register(ServerLivingEntityEvents.AfterDeath { entity, damageSource ->
            val killer = damageSource.attacker as? ServerPlayerEntity ?: return@AfterDeath
            when (entity) {
                is MobEntity -> {
                    if (entity is Monster || entity is HostileEntity) {
                        val rewardScale = killer.server?.let { MoonPhaseService.killRewardMultiplier(it) } ?: 1.0
                        val bossRewardScale = if ((entity as? AdventureRankMobAccess)?.cresoraIsBossMob() == true) 3.0 else 1.0
                        AdventureRankService.addXp(killer, (AdventureRankService.hostileKillXp(entity) * rewardScale * bossRewardScale).roundToInt())
                        CreditsService.addHostileKillReward(killer, entity, rewardScale * bossRewardScale)
                        WeaponDropService.onHostileKilled(killer, entity)
                        ArtifactSpecialUpgradeService.tryDropSpecialItems(killer, entity)
                        MoonAltarService.tryDropMoonBrick(killer, entity)
                        EquipmentEffectHookService.onKill(killer, entity)
                        hifumi.cresora.guide.GuideService.onKillHostile(killer)
                        if (killer.random.nextDouble() < 0.05) {
                            val element = hifumi.cresora.leyline.LeyLineElement.entries[killer.random.nextInt(hifumi.cresora.leyline.LeyLineElement.entries.size)]
                            val keyItem = CreSoraUtilities.LEY_LINE_KEYS[element]
                            if (keyItem != null) {
                                killer.inventory.offerOrDrop(net.minecraft.item.ItemStack(keyItem))
                                killer.sendMessage(net.minecraft.text.Text.translatable("message.cresora.leyline.key_dropped", net.minecraft.text.Text.translatable(element.translationKeyId)), false)
                            }
                        }
                        if (killer.random.nextDouble() < 0.03) {
                            ResonanceService.addCurrency(killer, ResonanceCurrencyType.CHORD_PROGRESSION, 35)
                            killer.sendMessage(net.minecraft.text.Text.translatable("message.cresora.mob_drop.chord_progression", 35), true)
                        }
                        if (killer.random.nextDouble() < 0.015) {
                            ResonanceService.addCurrency(killer, ResonanceCurrencyType.SUBSTITUTE_CHORD, 25)
                            killer.sendMessage(net.minecraft.text.Text.translatable("message.cresora.mob_drop.substitute_chord", 25), true)
                        }
                        if ((entity as? AdventureRankMobAccess)?.cresoraIsBossMob() == true) {
                            ResonanceService.addCurrency(killer, ResonanceCurrencyType.CHORD_PROGRESSION, 120)
                            killer.sendMessage(net.minecraft.text.Text.translatable("message.cresora.mob_drop.chord_progression", 120), true)
                            if (killer.random.nextDouble() < 0.35) {
                                ResonanceService.addCurrency(killer, ResonanceCurrencyType.SUBSTITUTE_CHORD, 60)
                                killer.sendMessage(net.minecraft.text.Text.translatable("message.cresora.mob_drop.substitute_chord", 60), true)
                            }
                        }
                        if (killer.random.nextDouble() < 0.008) {
                            killer.inventory.offerOrDrop(net.minecraft.item.ItemStack(CreSoraUtilities.RESONANT_LOCATOR_ITEM))
                            killer.sendMessage(net.minecraft.text.Text.translatable("message.cresora.mob_drop.resonant_locator"), true)
                        }
                    } else {
                        CreditsService.addFriendlyKillReward(killer, entity)
                    }
                }
            }
        })

        ServerEntityEvents.ENTITY_LOAD.register(ServerEntityEvents.Load { entity, world ->
            if (CombatMobDisplayService.discardOrphanedIndicator(entity)) {
                return@Load
            }
            val hostile = entity as? MobEntity ?: return@Load
            if (hostile is Monster || hostile is HostileEntity) {
                val rank = AdventureRankService.getOrAssignMobRank(hostile, world)
                FieldMobPackService.ensureClassification(hostile)
                AdventureRankService.applyMobScaling(hostile, rank)
            }
        })

        ServerPlayerEvents.COPY_FROM.register(ServerPlayerEvents.CopyFrom { oldPlayer, newPlayer, _ ->
            AdventureRankService.copyTo(oldPlayer, newPlayer)
            CreditsService.copyTo(oldPlayer, newPlayer)
            ResonanceService.copyTo(oldPlayer, newPlayer)
            StoryProgressService.copyTo(oldPlayer, newPlayer)
            MasqueradeProgressService.copyTo(oldPlayer, newPlayer)
            hifumi.cresora.guide.GuideService.copyTo(oldPlayer, newPlayer)
            MasqueradeService.restoreAfterRespawn(newPlayer)
            EquipmentAttributeService.markForFullHeal(newPlayer)
        })

        ServerTickEvents.END_WORLD_TICK.register(ServerTickEvents.EndWorldTick { world ->
            CombatMobDisplayService.tick(world)
        })
    }
}
