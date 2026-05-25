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

public object ZansouSkill : WeaponSkillHandler {
  override fun activate(
    player: ServerPlayerEntity,
    definition: WeaponDefinition,
    `data`: WeaponData,
    access: WeaponSkillAccess,
  ): ActionResult {

    ; run execute@ {
      val cost = player.maxHealth * 0.20f
      player.damage(player.world, player.damageSources.magic(), cost)
    }

    WeaponSkillService.startCooldown(player, definition.id, 600L)
    WeaponSkillService.showCooldownBar(player, definition)

    run {
        val now = WeaponSkillService.currentWorldTime(player)
        val state = QianqiuYeluoSkill.passiveStateStates.getOrPut(player.uuid) {
        QianqiuYeluoSkill.PassiveStateState(0L, 0) }
        state.expireTick = now + 40 * 20L
        state.stacks = (state.stacks + 1).coerceAtMost(1)
        player.sendMessage(Text.translatable("item.cresora.weapon.skill.buff.passive_state.gained",
        Text.translatable("item.cresora.weapon.skill.buff.passive_state.name"), state.stacks), true)
    }

    run {
        val now = WeaponSkillService.currentWorldTime(player)
        val state = QianqiuYeluoSkill.zansouModeStates.getOrPut(player.uuid) {
        QianqiuYeluoSkill.ZansouModeState(0L, 0) }
        state.expireTick = now + 50 * 20L
        state.stacks = (state.stacks + 1).coerceAtMost(1)
        player.sendMessage(Text.translatable("item.cresora.weapon.skill.buff.zansou_mode.gained",
        Text.translatable("item.cresora.weapon.skill.buff.zansou_mode.name"), state.stacks), true)
    }


    ; run execute@ {
      val jt = QianqiuYeluoSkill.jingtianStateStates.get(player.uuid)
      if (jt != null) {
                              jt.expireTick =
              hifumi.cresora.weapon.WeaponSkillService.currentWorldTime(player) + 40 * 20L;
                          }
    }

    HotbarOverrideService.restoreHotbar(player)

    player.sendMessage(Text.translatable("item.cresora.weapon.qianqiu_yeluo.zansou_activated").formatted(Formatting.AQUA),
        true)


    return ActionResult.SUCCESS
  }
}
