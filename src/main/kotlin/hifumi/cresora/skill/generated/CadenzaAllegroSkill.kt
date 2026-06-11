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

public object CadenzaAllegroSkill : WeaponSkillHandler {
  override fun activate(
    player: ServerPlayerEntity,
    definition: WeaponDefinition,
    `data`: WeaponData,
    access: WeaponSkillAccess,
  ): ActionResult {
    WeaponSkillService.startCooldown(player, definition.id, definition.skill.cooldownSeconds * 20L)
    WeaponSkillService.showCooldownBar(player, definition)


    ; run execute@ {
      val world = player.world as? ServerWorld ?: return@execute
      val radius = definition.skill.radiusMeters
      val n = if (data.baseLevel >= 61) 3 else if (data.baseLevel >= 41) 2 else 1
      val maxPlayerHp = world.players.filter { it.squaredDistanceTo(player.x, player.y, player.z) < 100.0 }.maxOfOrNull { it.maxHealth } ?: 20.0f
      val targets = world.getOtherEntities(player, player.boundingBox.expand(radius)) { entity ->
                          val hostile = entity as? HostileEntity ?: return@getOtherEntities false;
                          if (!hostile.isAlive) return@getOtherEntities false;
                          val mobAccess = hostile as? AdventureRankMobAccess;
                          mobAccess?.cresoraIsEliteMob() != true
                      }.mapNotNull { it as? HostileEntity }.sortedBy { it.squaredDistanceTo(player) }.take(n)
      var transformedCount = 0
      for (target in targets) {
                          val sheep = EntityType.SHEEP.spawn(world, null, target.blockPos, SpawnReason.COMMAND, true, false) ?: continue;
                          sheep!!.refreshPositionAndAngles(target.x, target.y, target.z, target.yaw, target.pitch);
                          BaaMimicService.markTransformedSheep(sheep!!, target);
                          val maxHealthAttr = sheep!!.getAttributeInstance(EntityAttributes.MAX_HEALTH);
                          if (maxHealthAttr != null) {
                              maxHealthAttr.baseValue = maxPlayerHp.toDouble();
                              sheep!!.health = maxPlayerHp;
                          }
                          AdventureRankService.refreshMobDisplay(sheep!!);
                          target.discard();
                          transformedCount++;
                      }
      if (transformedCount > 0) {
                          player.sendMessage(Text.translatable("item.cresora.weapon.skill.baa_mimic_activated", Text.translatable(definition.translationKey()), transformedCount).formatted(Formatting.GREEN), true);
                      } else {
                          player.sendMessage(Text.translatable("item.cresora.weapon.skill.baa_mimic_activated.generic").formatted(Formatting.GRAY), true);
                      }
    }


    return ActionResult.SUCCESS
  }
}
