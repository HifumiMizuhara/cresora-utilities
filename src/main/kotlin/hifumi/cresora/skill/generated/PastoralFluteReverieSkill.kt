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
      val now = WeaponSkillService.currentWorldTime(player)
      val expireTick = now + definition.skill.durationSeconds.coerceAtLeast(1) * 20L
      val normalPercent = WeaponCombatSupport.skillValuePercent(definition, data).coerceAtLeast(0.0)
      val sunlightPercent = WeaponCombatSupport.secondarySkillValuePercent(definition, data).coerceAtLeast(normalPercent)
      val isSunlit = !player.world.isRaining && player.world.isDay && player.world.isSkyVisible(player.blockPos.up())
      val amplifier = if (isSunlit) sunlightPercent else normalPercent
      val ampInt = (amplifier / 20.0).toInt().coerceAtMost(5)
      player.addStatusEffect(StatusEffectInstance(StatusEffects.SPEED, 50 * 20, ampInt))
      player.sendMessage(
                          Text.translatable(
                              "item.cresora.weapon.skill.sunlit_haste_activated",
                              Text.translatable(definition.translationKey()),
                              definition.skill.durationSeconds,
                              WeaponSkillService.formatNumber(normalPercent),
                              WeaponSkillService.formatNumber(sunlightPercent)
                          ).formatted(Formatting.YELLOW),
                          true
                      )
    }


    return ActionResult.SUCCESS
  }
}
