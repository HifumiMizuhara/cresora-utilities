package hifumi.cresora.skill.generated

import hifumi.cresora.adventurerank.AdventureRankMobAccess
import hifumi.cresora.adventurerank.AdventureRankService
import hifumi.cresora.combat.BaaMimicService
import hifumi.cresora.combat.CombatFeedbackService
import hifumi.cresora.credits.CreditsService
import hifumi.cresora.debuff.CresoraDebuffService
import hifumi.cresora.skill.WeaponSkillHandler
import hifumi.cresora.story.StoryService
import hifumi.cresora.weapon.HotbarOverrideService
import hifumi.cresora.weapon.WeaponCombatSupport
import hifumi.cresora.weapon.WeaponData
import hifumi.cresora.weapon.WeaponDefinition
import hifumi.cresora.weapon.WeaponSkillAccess
import hifumi.cresora.weapon.WeaponSkillService
import hifumi.cresora.weapon.WeaponStackSupport
import net.minecraft.entity.EntityType
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.SpawnReason
import net.minecraft.entity.attribute.EntityAttributes
import net.minecraft.entity.effect.StatusEffectInstance
import net.minecraft.entity.effect.StatusEffects
import net.minecraft.entity.mob.HostileEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.particle.ParticleTypes
import net.minecraft.registry.Registries
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.server.world.ServerWorld
import net.minecraft.text.Text
import net.minecraft.util.ActionResult
import net.minecraft.util.Formatting
import net.minecraft.util.Identifier

public object HighMountainsFlowingWaterSkill : WeaponSkillHandler {
  override fun activate(
    player: ServerPlayerEntity,
    definition: WeaponDefinition,
    `data`: WeaponData,
    access: WeaponSkillAccess,
  ): ActionResult {
    WeaponSkillService.startCooldown(player, definition.id, definition.skill.cooldownSeconds * 20L)
    WeaponSkillService.showCooldownBar(player, definition)


    ; run execute@ {
      val immediateHealHp = WeaponCombatSupport.skillValueHp(definition, data)
      val pulseHealHp = WeaponCombatSupport.secondarySkillValueHp(definition, data).coerceAtLeast(0.0f)
      val expireTick = WeaponSkillService.currentWorldTime(player) + definition.skill.durationSeconds * 20L
      val count = WeaponSkillService.healNearbyAllies(player, definition.skill.radiusMeters, immediateHealHp)
      val results = WeaponSkillService.restoreHealthOrGuard(player, immediateHealHp, expireTick)
      player.sendMessage(
                          Text.translatable(
                              "item.cresora.weapon.skill.healing_aura_activated",
                              Text.translatable(definition.translationKey()),
                              WeaponSkillService.formatNumber(results.first / 2.0),
                              WeaponSkillService.formatNumber(results.second / 2.0),
                              count,
                              WeaponSkillService.formatNumber(pulseHealHp / 2.0),
                              definition.skill.durationSeconds
                          ).formatted(Formatting.GREEN),
                          true
                      )
    }


    return ActionResult.SUCCESS
  }

  override fun onPlayerTick(
    player: ServerPlayerEntity,
    definition: WeaponDefinition,
    `data`: WeaponData,
  ) {

    ; run execute@ {
      val now = WeaponSkillService.currentWorldTime(player)
      if (WeaponSkillService.isCoolingDown(player, "gaoshan_liushui")) {
                           val remaining = WeaponSkillService.getRemainingCooldownTicks(player, "gaoshan_liushui");
                           val activeTicks = (20L * 20L - remaining).toLong();
                           // Periodic healing and knockback
                           if (activeTicks > 0 && activeTicks <= 8 * 20 && activeTicks % 40 == 0L) {
                               val pulseHealHp = WeaponCombatSupport.secondarySkillValueHp(definition, data).coerceAtLeast(0.0f);
                               WeaponSkillService.restoreHealthOrGuard(player, pulseHealHp, now + 200L);
                               // Knockback
                               val radius = definition.skill.radiusMeters;
                               player.world.getOtherEntities(player, player.boundingBox.expand(radius)) { it is HostileEntity && it.isAlive }
                                   .forEach { (it as HostileEntity).takeKnockback(1.15, player.x - it.x, player.z - it.z) }
                           }
                      }
    }

  }
}
