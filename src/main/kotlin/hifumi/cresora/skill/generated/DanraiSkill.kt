package hifumi.cresora.skill.generated

import hifumi.cresora.HotbarOverrideService
import hifumi.cresora.WeaponData
import hifumi.cresora.WeaponDefinition
import hifumi.cresora.WeaponSkillAccess
import hifumi.cresora.WeaponSkillService
import hifumi.cresora.skill.WeaponSkillHandler
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import net.minecraft.util.ActionResult
import net.minecraft.util.Formatting

public object DanraiSkill : WeaponSkillHandler {
  override fun activate(
    player: ServerPlayerEntity,
    definition: WeaponDefinition,
    `data`: WeaponData,
    access: WeaponSkillAccess,
  ): ActionResult {
    WeaponSkillService.startCooldown(player, definition.id, definition.skill.cooldownSeconds * 20L)
    WeaponSkillService.showCooldownBar(player, definition)

    ; {
        val now = WeaponSkillService.currentWorldTime(player)
        val state = QianqiuYeluoSkill.passiveStateStates.getOrPut(player.uuid) {
        QianqiuYeluoSkill.PassiveStateState(0L, 0) }
        state.expireTick = now + 40 * 20L
        state.stacks = (state.stacks + 1).coerceAtMost(1)
        player.sendMessage(Text.translatable("item.cresora.weapon.skill.buff.passive_state.gained",
        Text.translatable("item.cresora.weapon.skill.buff.passive_state.name"), state.stacks), true)
    }
    ; {
        val now = WeaponSkillService.currentWorldTime(player)
        val state = QianqiuYeluoSkill.jingtianStateStates.getOrPut(player.uuid) {
        QianqiuYeluoSkill.JingtianStateState(0L, 0) }
        state.expireTick = now + 40 * 20L
        state.stacks = (state.stacks + 1).coerceAtMost(1)
        player.sendMessage(Text.translatable("item.cresora.weapon.skill.buff.jingtian_state.gained",
        Text.translatable("item.cresora.weapon.skill.buff.jingtian_state.name"), state.stacks),
        true)
    }
    HotbarOverrideService.restoreHotbar(player)

    player.sendMessage(Text.translatable("item.cresora.weapon.qianqiu_yeluo.danrai_activated").formatted(Formatting.YELLOW),
        true)


    return ActionResult.SUCCESS
  }
}
