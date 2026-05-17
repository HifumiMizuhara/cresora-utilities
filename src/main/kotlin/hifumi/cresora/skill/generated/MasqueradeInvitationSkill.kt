package hifumi.cresora.skill.generated

import hifumi.cresora.WeaponData
import hifumi.cresora.WeaponDefinition
import hifumi.cresora.WeaponSkillAccess
import hifumi.cresora.WeaponSkillService
import hifumi.cresora.skill.WeaponSkillHandler
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.util.ActionResult

public object MasqueradeInvitationSkill : WeaponSkillHandler {
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
                      player.heal(value);
                     
          player.sendMessage(net.minecraft.text.Text.translatable("item.cresora.weapon.skill.heal_activated",
          net.minecraft.text.Text.translatable(definition.translationKey()),
          hifumi.cresora.WeaponSkillService.formatNumber(value /
          2.0)).formatted(net.minecraft.util.Formatting.GREEN), true);
    }


    return ActionResult.SUCCESS
  }
}
