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

public object BokuchuMunenSkill : WeaponSkillHandler {
  override fun activate(
    player: ServerPlayerEntity,
    definition: WeaponDefinition,
    `data`: WeaponData,
    access: WeaponSkillAccess,
  ): ActionResult {

    ; run execute@ {
      val world = player.world as? ServerWorld ?: return@execute
      if (!WeaponSkillService.consumeTao(player, 7)) {
                              TanbokuChokuuSkill.munenSeqStates.remove(player.uuid);
                             
              player.sendMessage(Text.translatable("item.cresora.weapon.tanboku_chokuu.tao_insufficient",
              7).formatted(Formatting.RED), true);
                              return@execute;
                          }

                          WeaponSkillService.grantInvulnerability(player, 60L)
      val target = world.getOtherEntities(player, player.boundingBox.expand(10.0)) { it is
          LivingEntity && it.isAlive }
                              .filterIsInstance<LivingEntity>()
                              .maxByOrNull { it.health } ?: run {
                                  WeaponSkillService.addTao(player, 7);
                                  TanbokuChokuuSkill.munenSeqStates.remove(player.uuid);
                              return@execute;
                          }
      val now = WeaponSkillService.currentWorldTime(player)
      val state = TanbokuChokuuSkill.munenSeqStates.getOrPut(player.uuid) {
          TanbokuChokuuSkill.MunenSeqState(0L, 0) }
      state.expireTick = now + 5 * 20L
      state.stacks = 0
      WeaponSkillService.applyMark(target, "munen_target_" + player.uuid, 100L)
      WeaponSkillService.startCooldown(player, "tanboku_chokuu", 600L)
      player.sendMessage(Text.translatable("item.cresora.weapon.tanboku_chokuu.munen_activated").formatted(Formatting.DARK_PURPLE),
          true)
      HotbarOverrideService.restoreHotbar(player)
    }


    return ActionResult.SUCCESS
  }

  override fun onPlayerTick(
    player: ServerPlayerEntity,
    definition: WeaponDefinition,
    `data`: WeaponData,
  ) {

    ; run execute@ {
      val now = WeaponSkillService.currentWorldTime(player)
      val state = TanbokuChokuuSkill.munenSeqStates.get(player.uuid)
      if (state != null && now < state.expireTick) {
                              if (now % 6L == 0L) {
                                  val world = player.world as? ServerWorld ?: return@execute;
                                  val markId = "munen_target_" + player.uuid;
                                  val target = world.getOtherEntities(null,
              player.boundingBox.expand(16.0)) { it is LivingEntity && it.isAlive &&
              WeaponSkillService.hasMark(it, markId) }
                                      .firstOrNull() as? LivingEntity;

                                  if (target == null) {
                                      TanbokuChokuuSkill.munenSeqStates.remove(player.uuid);
                                      return@execute;
                                  }

                                  val hit = state.stacks;
                                  if (hit <= 6) {
                                      val damage = WeaponCombatSupport.attackDamage(definition,
              data).toFloat();
                                      target.damage(world, world.damageSources.playerAttack(player),
              damage);
                                      state.stacks += 1;
                                      world.spawnParticles(ParticleTypes.SWEEP_ATTACK, target.x,
              target.y + 1, target.z, 1, 0.0, 0.0, 0.0, 0.0);
                                  } else if (hit == 7) {
                                      val trueDamage = target.maxHealth * 0.15f;
                                      WeaponSkillService.dealTrueDamage(player, target, trueDamage);
                                     
              player.sendMessage(Text.translatable("item.cresora.weapon.tanboku_chokuu.munen_final",
              WeaponSkillService.formatNumber(trueDamage.toDouble() /
              2.0)).formatted(Formatting.LIGHT_PURPLE), true);

                                      TanbokuChokuuSkill.munenSeqStates.remove(player.uuid);
                                      WeaponSkillService.removeMark(target, markId);
                                      world.spawnParticles(ParticleTypes.EXPLOSION, target.x,
              target.y + 1, target.z, 1, 0.0, 0.0, 0.0, 0.0);
                                  }
                              }
                          }
    }

  }
}
