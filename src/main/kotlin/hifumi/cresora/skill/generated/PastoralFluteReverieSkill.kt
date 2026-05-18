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

public object PastoralFluteReverieSkill : WeaponSkillHandler {
  override fun activate(
    player: ServerPlayerEntity,
    definition: WeaponDefinition,
    `data`: WeaponData,
    access: WeaponSkillAccess,
  ): ActionResult {
    WeaponSkillService.startCooldown(player, definition.id, definition.skill.cooldownSeconds * 20L)
    WeaponSkillService.showCooldownBar(player, definition)


    ; run execute@ {
      val now = hifumi.cresora.WeaponSkillService.currentWorldTime(player);
                      val expireTick = now + definition.skill.durationSeconds.coerceAtLeast(1) *
          20L;
                      val normalPercent =
          hifumi.cresora.WeaponCombatSupport.skillValuePercent(definition, data).coerceAtLeast(0.0);
                      val sunlightPercent =
          hifumi.cresora.WeaponCombatSupport.secondarySkillValuePercent(definition,
          data).coerceAtLeast(normalPercent);

                      /* We'll use a simple status effect for now as a placeholder for the complex
          state */
                      val isSunlit = !player.world.isRaining && player.world.isDay &&
          player.world.isSkyVisible(player.blockPos.up());
                      val amplifier = if (isSunlit) sunlightPercent else normalPercent;
                      val ampInt = (amplifier / 20.0).toInt().coerceAtMost(5);

                     
          player.addStatusEffect(net.minecraft.entity.effect.StatusEffectInstance(net.minecraft.entity.effect.StatusEffects.SPEED,
          50 * 20, ampInt));

                      player.sendMessage(
                          net.minecraft.text.Text.translatable(
                              "item.cresora.weapon.skill.sunlit_haste_activated",
                              net.minecraft.text.Text.translatable(definition.translationKey()),
                              definition.skill.durationSeconds,
                              hifumi.cresora.WeaponSkillService.formatNumber(normalPercent),
                              hifumi.cresora.WeaponSkillService.formatNumber(sunlightPercent)
                          ).formatted(net.minecraft.util.Formatting.YELLOW),
                          true
                      );
    }


    return ActionResult.SUCCESS
  }
}
