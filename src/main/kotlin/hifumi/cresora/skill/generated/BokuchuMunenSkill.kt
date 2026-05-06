package hifumi.cresora.skill.generated

import hifumi.cresora.WeaponData
import hifumi.cresora.WeaponDefinition
import hifumi.cresora.WeaponSkillAccess
import hifumi.cresora.skill.WeaponSkillHandler
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.util.ActionResult

public object BokuchuMunenSkill : WeaponSkillHandler {
  override fun activate(
    player: ServerPlayerEntity,
    definition: WeaponDefinition,
    `data`: WeaponData,
    access: WeaponSkillAccess,
  ): ActionResult {

    ; run execute@ {
      val world = player.world as? net.minecraft.server.world.ServerWorld ?: return@execute;
                      if (!hifumi.cresora.WeaponSkillService.consumeTao(player, 7)) {
                          TanbokuChokuuSkill.munenSeqStates.remove(player.uuid);
                         
          player.sendMessage(net.minecraft.text.Text.translatable("item.cresora.weapon.tanboku_chokuu.tao_insufficient",
          7).formatted(net.minecraft.util.Formatting.RED), true);
                          return@execute;
                      }

                      hifumi.cresora.WeaponSkillService.grantInvulnerability(player, 60L);
                      val target = world.getOtherEntities(player, player.boundingBox.expand(10.0)) {
          it is net.minecraft.entity.LivingEntity && it.isAlive }
                          .filterIsInstance<net.minecraft.entity.LivingEntity>()
                          .maxByOrNull { it.health } ?: run {
                              hifumi.cresora.WeaponSkillService.addTao(player, 7);
                              TanbokuChokuuSkill.munenSeqStates.remove(player.uuid);
                          return@execute;
                      };

                      val now = hifumi.cresora.WeaponSkillService.currentWorldTime(player);
                      val state = TanbokuChokuuSkill.munenSeqStates.getOrPut(player.uuid) {
          TanbokuChokuuSkill.MunenSeqState(0L, 0) };
                      state.expireTick = now + 5 * 20L;
                      state.stacks = 0;
                      hifumi.cresora.WeaponSkillService.applyMark(target, "munen_target_" +
          player.uuid, 100L);
                      hifumi.cresora.WeaponSkillService.startCooldown(player, "tanboku_chokuu",
          600L);
                     
          player.sendMessage(net.minecraft.text.Text.translatable("item.cresora.weapon.tanboku_chokuu.munen_activated").formatted(net.minecraft.util.Formatting.DARK_PURPLE),
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
                      val state = TanbokuChokuuSkill.munenSeqStates.get(player.uuid);
                      if (state != null && now < state.expireTick) {
                          if (now % 6L == 0L) {
                              val world = player.world as? net.minecraft.server.world.ServerWorld ?:
          return@execute;
                              val markId = "munen_target_" + player.uuid;
                              val target = world.getOtherEntities(null,
          player.boundingBox.expand(16.0)) { it is net.minecraft.entity.LivingEntity && it.isAlive
          && hifumi.cresora.WeaponSkillService.hasMark(it, markId) }
                                  .firstOrNull() as? net.minecraft.entity.LivingEntity;

                              if (target == null) {
                                  TanbokuChokuuSkill.munenSeqStates.remove(player.uuid);
                                  return@execute;
                              }

                              val hit = state.stacks;
                              if (hit <= 6) {
                                  val damage =
          hifumi.cresora.WeaponCombatSupport.attackDamage(definition, data).toFloat();
                                  target.damage(world, world.damageSources.playerAttack(player),
          damage);
                                  state.stacks += 1;
                                 
          world.spawnParticles(net.minecraft.particle.ParticleTypes.SWEEP_ATTACK, target.x,
          target.y + 1, target.z, 1, 0.0, 0.0, 0.0, 0.0);
                              } else if (hit == 7) {
                                  val trueDamage = target.maxHealth * 0.15f;
                                  target.health = (target.health -
          trueDamage).coerceAtLeast(0.001f);
                                  hifumi.cresora.AdventureRankService.showMobTrueDamage(target,
          player, trueDamage);
                                 
          player.sendMessage(net.minecraft.text.Text.translatable("item.cresora.weapon.tanboku_chokuu.munen_final",
          hifumi.cresora.WeaponSkillService.formatNumber(trueDamage.toDouble() /
          2.0)).formatted(net.minecraft.util.Formatting.LIGHT_PURPLE), true);

                                  TanbokuChokuuSkill.munenSeqStates.remove(player.uuid);
                                  hifumi.cresora.WeaponSkillService.removeMark(target, markId);
                                 
          world.spawnParticles(net.minecraft.particle.ParticleTypes.EXPLOSION, target.x, target.y +
          1, target.z, 1, 0.0, 0.0, 0.0, 0.0);
                              }
                          }
                      }
    }

  }
}
