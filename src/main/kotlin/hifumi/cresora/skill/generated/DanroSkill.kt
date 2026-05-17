package hifumi.cresora.skill.generated

import hifumi.cresora.HotbarOverrideService
import hifumi.cresora.WeaponData
import hifumi.cresora.WeaponDefinition
import hifumi.cresora.WeaponSkillAccess
import hifumi.cresora.skill.WeaponSkillHandler
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import net.minecraft.util.ActionResult
import net.minecraft.util.Formatting

public object DanroSkill : WeaponSkillHandler {
  override fun activate(
    player: ServerPlayerEntity,
    definition: WeaponDefinition,
    `data`: WeaponData,
    access: WeaponSkillAccess,
  ): ActionResult {

    ; run execute@ {
      val world = player.world as? net.minecraft.server.world.ServerWorld ?: return@execute;
                      val dir = player.rotationVecClient.multiply(1.0, 0.0, 1.0).normalize();

                      val hitBox = player.boundingBox.stretch(dir.multiply(5.0)).expand(1.0);
                      world.getOtherEntities(player, hitBox) { it is
          net.minecraft.entity.LivingEntity && it.isAlive }
                          .forEach { entity ->
                              val target = entity as net.minecraft.entity.LivingEntity;
                              val damage =
          hifumi.cresora.WeaponCombatSupport.attackDamage(definition, data).toFloat();
                              target.damage(world, world.damageSources.playerAttack(player),
          damage);
                              hifumi.cresora.WeaponSkillService.applySoulBreak(target, 1, 200L);
                              world.spawnParticles(net.minecraft.particle.ParticleTypes.CRIT,
          target.x, target.y + 1, target.z, 5, 0.2, 0.2, 0.2, 0.1);
                          }

                      hifumi.cresora.WeaponSkillService.addTao(player, 1);
                      hifumi.cresora.WeaponSkillService.startCooldown(player, "tanboku_chokuu",
          60L);
    }

    player.sendMessage(Text.translatable("item.cresora.weapon.tanboku_chokuu.danro_activated").formatted(Formatting.WHITE),
        true)

    HotbarOverrideService.restoreHotbar(player)


    return ActionResult.SUCCESS
  }
}
