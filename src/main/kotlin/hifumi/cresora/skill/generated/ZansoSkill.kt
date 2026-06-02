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

public object ZansoSkill : WeaponSkillHandler {
  override fun activate(
    player: ServerPlayerEntity,
    definition: WeaponDefinition,
    `data`: WeaponData,
    access: WeaponSkillAccess,
  ): ActionResult {

    ; run execute@ {
      if (!WeaponSkillService.consumeTao(player, 3)) {
                          TanbokuChokuuSkill.zansoActiveStates.remove(player.uuid);
                          player.sendMessage(Text.translatable("item.cresora.weapon.tanboku_chokuu.tao_insufficient", 3).formatted(Formatting.RED), true);
                          return@execute;
                      }
      val now = WeaponSkillService.currentWorldTime(player)
      val state = TanbokuChokuuSkill.zansoActiveStates.getOrPut(player.uuid) { TanbokuChokuuSkill.ZansoActiveState(0L, 0) }
      state.expireTick = now + 15 * 20L
      state.stacks = 1
      WeaponSkillService.startCooldown(player, "tanboku_chokuu", 300L)
      player.sendMessage(Text.translatable("item.cresora.weapon.tanboku_chokuu.zanso_activated").formatted(Formatting.AQUA), true)
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
      val state = TanbokuChokuuSkill.zansoActiveStates.get(player.uuid)
      if (state != null && now < state.expireTick) {
                          if (now % 30L == 0L) {
                              val world = player.world as? ServerWorld ?: return@execute;
                              world.getOtherEntities(player, player.boundingBox.expand(3.0)) { it is LivingEntity && it.isAlive }
                                  .forEach { entity ->
                                      val target = entity as LivingEntity;
                                      val baseDamage = WeaponCombatSupport.attackDamage(definition, data).toFloat();
                                      target.damage(world, world.damageSources.playerAttack(player), baseDamage * 0.5f);
                                      target.damage(world, world.damageSources.magic(), baseDamage * 0.5f);

                                      if (target.health / target.maxHealth < 0.10f && !((target as? AdventureRankMobAccess)?.cresoraIsEliteMob() ?: false)) {
                                          target.kill(world);
                                          player.sendMessage(Text.translatable("item.cresora.weapon.tanboku_chokuu.zanso_instakill").formatted(Formatting.DARK_RED), true);
                                      }
                                      world.spawnParticles(ParticleTypes.SNOWFLAKE, target.x, target.y + 1, target.z, 3, 0.1, 0.1, 0.1, 0.05);
                                  }
                          }
                      }
    }

  }
}
