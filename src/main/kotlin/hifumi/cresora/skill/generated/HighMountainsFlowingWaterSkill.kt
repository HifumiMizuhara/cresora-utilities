package hifumi.cresora.skill.generated

import hifumi.cresora.AdventureRankMobAccess
import hifumi.cresora.AdventureRankService
import hifumi.cresora.CreditsService
import hifumi.cresora.CresoraDebuffService
import hifumi.cresora.HotbarOverrideService
import hifumi.cresora.WeaponCombatSupport
import hifumi.cresora.WeaponData
import hifumi.cresora.WeaponDefinition
import hifumi.cresora.WeaponSkillAccess
import hifumi.cresora.WeaponSkillService
import hifumi.cresora.skill.WeaponSkillHandler
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.effect.StatusEffectInstance
import net.minecraft.entity.effect.StatusEffects
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
      val immediateHealHp = hifumi.cresora.WeaponCombatSupport.skillValueHp(definition, data)
      val pulseHealHp = hifumi.cresora.WeaponCombatSupport.secondarySkillValueHp(definition,
          data).coerceAtLeast(0.0f)
      val expireTick = hifumi.cresora.WeaponSkillService.currentWorldTime(player) +
          definition.skill.durationSeconds * 20L
      val count = hifumi.cresora.WeaponSkillService.healNearbyAllies(player,
          definition.skill.radiusMeters, immediateHealHp)
      val results = hifumi.cresora.WeaponSkillService.restoreHealthOrGuard(player, immediateHealHp,
          expireTick)
      player.sendMessage(
                              net.minecraft.text.Text.translatable(
                                  "item.cresora.weapon.skill.healing_aura_activated",
                                  net.minecraft.text.Text.translatable(definition.translationKey()),
                                  hifumi.cresora.WeaponSkillService.formatNumber(results.first /
              2.0),
                                  hifumi.cresora.WeaponSkillService.formatNumber(results.second /
              2.0),
                                  count,
                                  hifumi.cresora.WeaponSkillService.formatNumber(pulseHealHp / 2.0),
                                  definition.skill.durationSeconds
                              ).formatted(net.minecraft.util.Formatting.GREEN),
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
      val now = hifumi.cresora.WeaponSkillService.currentWorldTime(player)
      if (hifumi.cresora.WeaponSkillService.isCoolingDown(player, "gaoshan_liushui")) {
                               val remaining =
              hifumi.cresora.WeaponSkillService.getRemainingCooldownTicks(player,
              "gaoshan_liushui");
                               val activeTicks = (20L * 20L - remaining).toLong();
                               // Periodic healing and knockback
                               if (activeTicks > 0 && activeTicks <= 8 * 20 && activeTicks % 40 ==
              0L) {
                                   val pulseHealHp =
              hifumi.cresora.WeaponCombatSupport.secondarySkillValueHp(definition,
              data).coerceAtLeast(0.0f);
                                   hifumi.cresora.WeaponSkillService.restoreHealthOrGuard(player,
              pulseHealHp, now + 200L);
                                   // Knockback
                                   val radius = definition.skill.radiusMeters;
                                   player.world.getOtherEntities(player,
              player.boundingBox.expand(radius)) { it is net.minecraft.entity.mob.HostileEntity &&
              it.isAlive }
                                       .forEach { (it as
              net.minecraft.entity.mob.HostileEntity).takeKnockback(1.15, player.x - it.x,
              player.z - it.z) }
                               }
                          }
    }

  }
}
