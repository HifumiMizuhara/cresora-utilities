package hifumi.cresora.skill.generated

import hifumi.cresora.WeaponData
import hifumi.cresora.WeaponDefinition
import hifumi.cresora.WeaponSkillAccess
import hifumi.cresora.WeaponSkillService
import hifumi.cresora.skill.WeaponSkillHandler
import kotlin.Float
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.util.ActionResult

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
      val value = hifumi.cresora.WeaponCombatSupport.skillValueHp(definition, data);
                      hifumi.cresora.WeaponSkillService.grantShield(player, value,
          definition.skill.durationSeconds * 20L);
                     
          player.sendMessage(net.minecraft.text.Text.translatable("item.cresora.weapon.skill.activated",
          net.minecraft.text.Text.translatable(definition.translationKey()),
          hifumi.cresora.WeaponSkillService.formatNumber(value /
          2.0)).formatted(net.minecraft.util.Formatting.AQUA), true);
    }


    return ActionResult.SUCCESS
  }

  override fun onPlayerTick(
    player: ServerPlayerEntity,
    definition: WeaponDefinition,
    `data`: WeaponData,
  ) {

    ; run execute@ {
      hifumi.cresora.WeaponSkillService.clearExpiredShield(player)
    }

  }

  override fun onDamageAbsorbed(
    player: ServerPlayerEntity,
    amount: Float,
    definition: WeaponDefinition,
    `data`: WeaponData,
  ): Float {

    ; run execute@ {
      hifumi.cresora.WeaponSkillService.clearExpiredShield(player);
                       val remainingShield = (player as
          hifumi.cresora.WeaponSkillAccess).cresoraGetShieldHp();
                       if (remainingShield > 0.0f) {
                           var remainingAmount = amount;
                           if (remainingAmount <= remainingShield) {
                               (player as
          hifumi.cresora.WeaponSkillAccess).cresoraSetShieldHp(remainingShield - remainingAmount);
                               if ((player as hifumi.cresora.WeaponSkillAccess).cresoraGetShieldHp()
          <= 0.0f) hifumi.cresora.WeaponSkillService.clearShield(player);
                              
          player.sendMessage(net.minecraft.text.Text.translatable("item.cresora.weapon.skill.blocked",
          hifumi.cresora.WeaponSkillService.formatNumber(remainingAmount /
          2.0.toDouble())).formatted(net.minecraft.util.Formatting.AQUA), true);
                               return@execute 0.0f;
                           }
                           (player as hifumi.cresora.WeaponSkillAccess).cresoraSetShieldHp(0.0f);
                           hifumi.cresora.WeaponSkillService.clearShield(player);
                          
          player.sendMessage(net.minecraft.text.Text.translatable("item.cresora.weapon.skill.broken").formatted(net.minecraft.util.Formatting.RED),
          true);
                           return@execute remainingAmount - remainingShield;
                       }
                       return@execute amount;
    }

    return amount
  }
}
