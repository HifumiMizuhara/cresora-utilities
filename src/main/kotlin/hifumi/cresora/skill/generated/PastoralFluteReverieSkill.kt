package hifumi.cresora.skill.generated

import hifumi.cresora.WeaponData
import hifumi.cresora.WeaponDefinition
import hifumi.cresora.WeaponSkillAccess
import hifumi.cresora.WeaponSkillService
import hifumi.cresora.skill.WeaponSkillHandler
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import net.minecraft.util.ActionResult
import net.minecraft.util.Formatting

public object PastoralFluteReverieSkill : WeaponSkillHandler {
  override fun activate(
    player: ServerPlayerEntity,
    definition: WeaponDefinition,
    `data`: WeaponData,
    access: WeaponSkillAccess,
  ): ActionResult {
    WeaponSkillService.startCooldown(player, definition.id, definition.skill.cooldownSeconds * 20L)
    WeaponSkillService.showCooldownBar(player, definition)

    run execute@ {
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
    }

    player.sendMessage(Text.translatable("item.cresora.weapon.skill.sunlit_haste_activated.generic").formatted(Formatting.YELLOW),
        true)


    return ActionResult.SUCCESS
  }
}
