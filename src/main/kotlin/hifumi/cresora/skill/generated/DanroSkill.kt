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

public object DanroSkill : WeaponSkillHandler {
  override fun activate(
    player: ServerPlayerEntity,
    definition: WeaponDefinition,
    `data`: WeaponData,
    access: WeaponSkillAccess,
  ): ActionResult {

    ; run execute@ {
      val world = player.world as? ServerWorld ?: return@execute
      val dir = player.rotationVecClient.multiply(1.0, 0.0, 1.0).normalize()
      val hitBox = player.boundingBox.stretch(dir.multiply(5.0)).expand(1.0)
      world.getOtherEntities(player, hitBox) { it is LivingEntity && it.isAlive }
                          .forEach { entity ->
                              val target = entity as LivingEntity;
                              val damage = WeaponCombatSupport.attackDamage(definition, data).toFloat();
                              target.damage(world, world.damageSources.playerAttack(player), damage);
                              WeaponSkillService.applySoulBreak(target, 1, 200L);
                              world.spawnParticles(ParticleTypes.CRIT, target.x, target.y + 1, target.z, 5, 0.2, 0.2, 0.2, 0.1);
                          }
      WeaponSkillService.addTao(player, 1)
      WeaponSkillService.startCooldown(player, "tanboku_chokuu", 60L)
    }

    player.sendMessage(Text.translatable("item.cresora.weapon.tanboku_chokuu.danro_activated").formatted(Formatting.WHITE),
        true)

    HotbarOverrideService.restoreHotbar(player)


    return ActionResult.SUCCESS
  }
}
