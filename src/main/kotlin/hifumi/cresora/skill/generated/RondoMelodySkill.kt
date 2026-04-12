package hifumi.cresora.skill.generated

import hifumi.cresora.WeaponCombatSupport
import hifumi.cresora.WeaponData
import hifumi.cresora.WeaponDefinition
import hifumi.cresora.WeaponSkillAccess
import hifumi.cresora.WeaponSkillService
import hifumi.cresora.skill.WeaponSkillHandler
import kotlin.Float
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import net.minecraft.util.ActionResult
import net.minecraft.util.Formatting

public object RondoMelodySkill : WeaponSkillHandler {
  override fun activate(
    player: ServerPlayerEntity,
    definition: WeaponDefinition,
    `data`: WeaponData,
    access: WeaponSkillAccess,
  ): ActionResult {
    WeaponSkillService.startCooldown(player, definition.id, definition.skill.cooldownSeconds * 20L)
    WeaponSkillService.showCooldownBar(player, definition)

    (player as WeaponSkillAccess).cresoraSetShieldHp(WeaponCombatSupport.shieldHp(definition, data))
    (player as
        WeaponSkillAccess).cresoraSetShieldExpireTick(WeaponSkillService.currentWorldTime(player) +
        definition.skill.durationSeconds.toLong() * 20L)

    player.sendMessage(Text.translatable("item.cresora.weapon.skill.activated.generic").formatted(Formatting.AQUA),
        true)


    return ActionResult.SUCCESS
  }

  override fun onPlayerTick(player: ServerPlayerEntity) {
    run execute@ {
      hifumi.cresora.WeaponSkillService.clearExpiredShield(player)
    }

  }

  override fun onDamageAbsorbed(player: ServerPlayerEntity, amount: Float): Float {
    run execute@ {
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
                               return 0.0f;
                           }
                           (player as hifumi.cresora.WeaponSkillAccess).cresoraSetShieldHp(0.0f);
                           hifumi.cresora.WeaponSkillService.clearShield(player);
                          
          player.sendMessage(net.minecraft.text.Text.translatable("item.cresora.weapon.skill.broken").formatted(net.minecraft.util.Formatting.RED),
          true);
                           return remainingAmount - remainingShield;
                       }
    }

    return amount
  }
}
