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

public object RequiemTowardDawnSkill : WeaponSkillHandler {
  override fun activate(
    player: ServerPlayerEntity,
    definition: WeaponDefinition,
    `data`: WeaponData,
    access: WeaponSkillAccess,
  ): ActionResult {
    WeaponSkillService.startCooldown(player, definition.id, definition.skill.cooldownSeconds * 20L)
    WeaponSkillService.showCooldownBar(player, definition)


    ; run execute@ {
      val radius = definition.skill.radiusMeters
      val duration = definition.skill.durationSeconds * 20L
      player.addStatusEffect(net.minecraft.entity.effect.StatusEffectInstance(net.minecraft.entity.effect.StatusEffects.FIRE_RESISTANCE,
          duration.toInt(), 0))
      val world = player.world as? net.minecraft.server.world.ServerWorld ?: return@execute
      val targets = world.getOtherEntities(player, player.boundingBox.expand(radius)) { it is
          net.minecraft.entity.LivingEntity && it.isAlive }
      for (target in targets) {
                              target.setOnFireFor(5.0f);
                          }

                          player.sendMessage(
                              net.minecraft.text.Text.translatable(
                                  "item.cresora.weapon.skill.flame_aura_activated",
                                  net.minecraft.text.Text.translatable(definition.translationKey()),
                                  targets.size,
                                  5,
                                  definition.skill.durationSeconds
                              ).formatted(net.minecraft.util.Formatting.GOLD),
                              true
                          )
    }


    return ActionResult.SUCCESS
  }
}
