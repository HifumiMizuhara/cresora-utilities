package hifumi.cresora.skill.generated

import hifumi.cresora.WeaponData
import hifumi.cresora.WeaponDefinition
import hifumi.cresora.WeaponSkillAccess
import hifumi.cresora.skill.WeaponSkillHandler
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.util.ActionResult

public object ZansoSkill : WeaponSkillHandler {
  override fun activate(
    player: ServerPlayerEntity,
    definition: WeaponDefinition,
    `data`: WeaponData,
    access: WeaponSkillAccess,
  ): ActionResult {

    ; run execute@ {
      if (!hifumi.cresora.WeaponSkillService.consumeTao(player, 3)) {
                          TanbokuChokuuSkill.zansoActiveStates.remove(player.uuid);
                         
          player.sendMessage(net.minecraft.text.Text.translatable("item.cresora.weapon.tanboku_chokuu.tao_insufficient",
          3).formatted(net.minecraft.util.Formatting.RED), true);
                          return@execute;
                      }

                      val now = hifumi.cresora.WeaponSkillService.currentWorldTime(player);
                      val state = TanbokuChokuuSkill.zansoActiveStates.getOrPut(player.uuid) {
          TanbokuChokuuSkill.ZansoActiveState(0L, 0) };
                      state.expireTick = now + 15 * 20L;
                      state.stacks = 1;
                      hifumi.cresora.WeaponSkillService.startCooldown(player, "tanboku_chokuu",
          300L);
                     
          player.sendMessage(net.minecraft.text.Text.translatable("item.cresora.weapon.tanboku_chokuu.zanso_activated").formatted(net.minecraft.util.Formatting.AQUA),
          true);
                      hifumi.cresora.HotbarOverrideService.restoreHotbar(player)
    }


    return ActionResult.SUCCESS
  }

  override fun onPlayerTick(
    player: ServerPlayerEntity,
    definition: WeaponDefinition,
    `data`: WeaponData,
  ) {

    ; run execute@ {
      val now = hifumi.cresora.WeaponSkillService.currentWorldTime(player);
                      val state = TanbokuChokuuSkill.zansoActiveStates.get(player.uuid);
                      if (state != null && now < state.expireTick) {
                          if (now % 30L == 0L) {
                              val world = player.world as? net.minecraft.server.world.ServerWorld ?:
          return@execute;
                              world.getOtherEntities(player, player.boundingBox.expand(3.0)) { it is
          net.minecraft.entity.LivingEntity && it.isAlive }
                                  .forEach { entity ->
                                      val target = entity as net.minecraft.entity.LivingEntity;
                                      val baseDamage =
          hifumi.cresora.WeaponCombatSupport.attackDamage(definition, data).toFloat();
                                      target.damage(world, world.damageSources.playerAttack(player),
          baseDamage * 0.5f);
                                      target.damage(world, world.damageSources.magic(), baseDamage *
          0.5f);

                                      if (target.health / target.maxHealth < 0.10f && !((target as?
          hifumi.cresora.AdventureRankMobAccess)?.cresoraIsEliteMob() ?: false)) {
                                          target.kill(world);
                                         
          player.sendMessage(net.minecraft.text.Text.translatable("item.cresora.weapon.tanboku_chokuu.zanso_instakill").formatted(net.minecraft.util.Formatting.DARK_RED),
          true);
                                      }
                                     
          world.spawnParticles(net.minecraft.particle.ParticleTypes.SNOWFLAKE, target.x, target.y +
          1, target.z, 3, 0.1, 0.1, 0.1, 0.05);
                                  }
                          }
                      }
    }

  }
}
