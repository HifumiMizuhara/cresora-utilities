package hifumi.cresora.skill.generated

import hifumi.cresora.adventurerank.AdventureRankMobAccess
import hifumi.cresora.adventurerank.AdventureRankService
import hifumi.cresora.credits.CreditsService
import hifumi.cresora.debuff.CresoraDebuffService
import hifumi.cresora.skill.WeaponSkillHandler
import hifumi.cresora.weapon.HotbarOverrideService
import hifumi.cresora.weapon.WeaponCombatSupport
import hifumi.cresora.weapon.WeaponData
import hifumi.cresora.weapon.WeaponDefinition
import hifumi.cresora.weapon.WeaponSkillAccess
import hifumi.cresora.weapon.WeaponSkillService
import kotlin.Float
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

public object RondoMelodySkill : WeaponSkillHandler {
  override fun activate(
    player: ServerPlayerEntity,
    definition: WeaponDefinition,
    `data`: WeaponData,
    access: WeaponSkillAccess,
  ): ActionResult {
    WeaponSkillService.startCooldown(player, definition.id, definition.skill.cooldownSeconds * 20L)
    WeaponSkillService.showCooldownBar(player, definition)


    ; run execute@ {
      val value = hifumi.cresora.weapon.WeaponCombatSupport.skillValueHp(definition, data)
      hifumi.cresora.weapon.WeaponSkillService.grantShield(player, value, definition.skill.durationSeconds * 20L)
      player.sendMessage(net.minecraft.text.Text.translatable("item.cresora.weapon.skill.activated", net.minecraft.text.Text.translatable(definition.translationKey()), hifumi.cresora.weapon.WeaponSkillService.formatNumber(value / 2.0)).formatted(net.minecraft.util.Formatting.AQUA), true)
    }


    return ActionResult.SUCCESS
  }

  override fun onPlayerTick(
    player: ServerPlayerEntity,
    definition: WeaponDefinition,
    `data`: WeaponData,
  ) {

    ; run execute@ {
      hifumi.cresora.weapon.WeaponSkillService.clearExpiredShield(player)
    }

  }

  override fun onDamageAbsorbed(
    player: ServerPlayerEntity,
    amount: Float,
    definition: WeaponDefinition,
    `data`: WeaponData,
  ): Float {

    ; return run execute@ {
    hifumi.cresora.weapon.WeaponSkillService.clearExpiredShield(player)
    val remainingShield = (player as hifumi.cresora.weapon.WeaponSkillAccess).cresoraGetShieldHp()
    if (remainingShield > 0.0f) {
                         var remainingAmount = amount;
                         if (remainingAmount <= remainingShield) {
                             (player as hifumi.cresora.weapon.WeaponSkillAccess).cresoraSetShieldHp(remainingShield - remainingAmount);
                             if ((player as hifumi.cresora.weapon.WeaponSkillAccess).cresoraGetShieldHp() <= 0.0f) hifumi.cresora.weapon.WeaponSkillService.clearShield(player);
                             player.sendMessage(net.minecraft.text.Text.translatable("item.cresora.weapon.skill.blocked", hifumi.cresora.weapon.WeaponSkillService.formatNumber(remainingAmount / 2.0.toDouble())).formatted(net.minecraft.util.Formatting.AQUA), true);
                             return@execute 0.0f;
                         }
                         (player as hifumi.cresora.weapon.WeaponSkillAccess).cresoraSetShieldHp(0.0f);
                         hifumi.cresora.weapon.WeaponSkillService.clearShield(player);
                         player.sendMessage(net.minecraft.text.Text.translatable("item.cresora.weapon.skill.broken").formatted(net.minecraft.util.Formatting.RED), true);
                         return@execute remainingAmount - remainingShield;
                     }
    return@execute amount
    }

  }
}
