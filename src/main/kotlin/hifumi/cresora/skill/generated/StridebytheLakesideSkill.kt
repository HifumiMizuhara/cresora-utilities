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

public object StridebytheLakesideSkill : WeaponSkillHandler {
  override fun activate(
    player: ServerPlayerEntity,
    definition: WeaponDefinition,
    `data`: WeaponData,
    access: WeaponSkillAccess,
  ): ActionResult {
    WeaponSkillService.startCooldown(player, definition.id, definition.skill.cooldownSeconds * 20L)
    WeaponSkillService.showCooldownBar(player, definition)


    ; run execute@ {
      val radius = definition.skill.radiusMeters.coerceAtLeast(0.0)
      val ratio = WeaponCombatSupport.currentHpTrueDamageRatio(definition, data)
      val world = player.world as? ServerWorld ?: return@execute
      val targets = world.getOtherEntities(player, player.boundingBox.expand(radius)) { entity ->
                          entity is HostileEntity && entity.isAlive
                      }.mapNotNull { it as? HostileEntity }
      var hitCount = 0
      var totalDamage = 0.0
      for (target in targets) {
                          if (!StoryService.allowsTrueDamage(target)) continue;
                          val damage = (target.health.toDouble() * ratio).coerceAtLeast(0.0);
                          if (damage <= 0.0) continue;
                          target.health = (target.health.toDouble() - damage).coerceAtLeast(0.001).toFloat();
                          AdventureRankService.showMobTrueDamage(target, player, damage.toFloat());
                          AdventureRankService.refreshMobDisplay(target);
                          hitCount++;
                          totalDamage += damage;
                      }
      player.sendMessage(
                          Text.translatable(
                              "item.cresora.weapon.skill.current_hp_true_damage_activated",
                              Text.translatable(definition.translationKey()),
                              hitCount,
                              WeaponSkillService.formatNumber(totalDamage / 2.0)
                          ).formatted(Formatting.AQUA),
                          true
                      )
    }


    return ActionResult.SUCCESS
  }
}
