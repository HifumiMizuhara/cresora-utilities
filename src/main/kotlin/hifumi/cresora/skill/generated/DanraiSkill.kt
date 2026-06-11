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

public object DanraiSkill : WeaponSkillHandler {
  override fun activate(
    player: ServerPlayerEntity,
    definition: WeaponDefinition,
    `data`: WeaponData,
    access: WeaponSkillAccess,
  ): ActionResult {
    WeaponSkillService.startCooldown(player, definition.id, 680L)
    WeaponSkillService.showCooldownBar(player, definition)

    run {
        val now = WeaponSkillService.currentWorldTime(player)
        val state = QianqiuYeluoSkill.passiveStateStates.getOrPut(player.uuid) {
        QianqiuYeluoSkill.PassiveStateState(0L, 0) }
        state.expireTick = now + 40 * 20L
        state.stacks = (state.stacks + 1).coerceAtMost(1)
        player.sendMessage(Text.translatable("item.cresora.weapon.skill.buff.passive_state.gained",
        Text.translatable("item.cresora.weapon.skill.buff.passive_state.name"),
        WeaponSkillService.getDisplayStacks(player, "passive_state", state.stacks)), true)
    }

    run {
        val now = WeaponSkillService.currentWorldTime(player)
        val state = QianqiuYeluoSkill.jingtianStateStates.getOrPut(player.uuid) {
        QianqiuYeluoSkill.JingtianStateState(0L, 0) }
        state.expireTick = now + 40 * 20L
        state.stacks = (state.stacks + 1).coerceAtMost(1)
        player.sendMessage(Text.translatable("item.cresora.weapon.skill.buff.jingtian_state.gained",
        Text.translatable("item.cresora.weapon.skill.buff.jingtian_state.name"),
        WeaponSkillService.getDisplayStacks(player, "jingtian_state", state.stacks)), true)
    }

    HotbarOverrideService.restoreHotbar(player)

    player.sendMessage(Text.translatable("item.cresora.weapon.qianqiu_yeluo.danrai_activated").formatted(Formatting.YELLOW),
        true)


    return ActionResult.SUCCESS
  }
}
