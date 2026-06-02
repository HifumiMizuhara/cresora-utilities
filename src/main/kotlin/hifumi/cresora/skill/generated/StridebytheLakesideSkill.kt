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
      val ratio = hifumi.cresora.weapon.WeaponCombatSupport.currentHpTrueDamageRatio(definition, data)
      val world = player.world as? net.minecraft.server.world.ServerWorld ?: return@execute
      val targets = world.getOtherEntities(player, player.boundingBox.expand(radius)) { entity ->
                          entity is net.minecraft.entity.mob.HostileEntity && entity.isAlive
                      }.mapNotNull { it as? net.minecraft.entity.mob.HostileEntity }
      var hitCount = 0
      var totalDamage = 0.0
      for (target in targets) {
                          if (!hifumi.cresora.story.StoryService.allowsTrueDamage(target)) continue;
                          val damage = (target.health.toDouble() * ratio).coerceAtLeast(0.0);
                          if (damage <= 0.0) continue;
                          target.health = (target.health.toDouble() - damage).coerceAtLeast(0.001).toFloat();
                          hifumi.cresora.adventurerank.AdventureRankService.showMobTrueDamage(target, player, damage.toFloat());
                          hifumi.cresora.adventurerank.AdventureRankService.refreshMobDisplay(target);
                          hitCount++;
                          totalDamage += damage;
                      }
      player.sendMessage(
                          net.minecraft.text.Text.translatable(
                              "item.cresora.weapon.skill.current_hp_true_damage_activated",
                              net.minecraft.text.Text.translatable(definition.translationKey()),
                              hitCount,
                              hifumi.cresora.weapon.WeaponSkillService.formatNumber(totalDamage / 2.0)
                          ).formatted(net.minecraft.util.Formatting.AQUA),
                          true
                      )
    }


    return ActionResult.SUCCESS
  }
}
