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
import kotlin.Boolean
import kotlin.Float
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

public object DarkLuxSkill : WeaponSkillHandler {
  override fun activate(
    player: ServerPlayerEntity,
    definition: WeaponDefinition,
    `data`: WeaponData,
    access: WeaponSkillAccess,
  ): ActionResult {
    WeaponSkillService.startCooldown(player, definition.id, definition.skill.cooldownSeconds * 20L)
    WeaponSkillService.showCooldownBar(player, definition)

    player.world.getNonSpectatingEntities(net.minecraft.entity.LivingEntity::class.java,
        player.boundingBox.expand(5.0.toDouble())).forEach { target ->
        if (target != player) {
            // area_of_effect block
    // Unsupported nested AOE action: InstructionCallNode
        }
    }

    player.sendMessage(Text.translatable("item.cresora.weapon.skill.dark_lux_activated.generic").formatted(Formatting.DARK_PURPLE),
        true)


    return ActionResult.SUCCESS
  }

  override fun onDamageDealt(
    player: ServerPlayerEntity,
    target: LivingEntity,
    amount: Float,
    isTrueDamage: Boolean,
    definition: WeaponDefinition,
    `data`: WeaponData,
  ) {

    ; run execute@ {
      if (hifumi.cresora.WeaponStackSupport.getDefinition(player.mainHandStack)?.id != "dark_lux")
          return@execute
      if (isTrueDamage) return@execute
      val now = hifumi.cresora.WeaponSkillService.currentWorldTime(player)
      if (hifumi.cresora.WeaponSkillService.hasMark(target, "entanglement")) return@execute
      hifumi.cresora.WeaponSkillService.applyMark(target, "dark", 200L)
      if (hifumi.cresora.WeaponSkillService.hasMark(target, "lux") &&
          hifumi.cresora.WeaponSkillService.hasMark(target, "dark")) {
                              val world = player.world as? net.minecraft.server.world.ServerWorld ?:
              return@execute;
                              val roll = world.random.nextDouble();
                              when {
                                  roll < 0.2 -> { // Annihilation
                                     
              player.sendMessage(net.minecraft.text.Text.translatable("message.cresora.weapon.dark_lux.annihilation").formatted(net.minecraft.util.Formatting.DARK_RED),
              true);
                                      hifumi.cresora.WeaponSkillService.removeMark(target, "dark");
                                      hifumi.cresora.WeaponSkillService.removeMark(target, "lux");
                                      val damage = target.health * 0.9f;
                                      target.damage(world, world.damageSources.magic(), damage);
                                      world.getOtherEntities(target, target.boundingBox.expand(5.0))
              { it is net.minecraft.entity.LivingEntity && it.isAlive }
                                          .take(3).forEach { (it as
              net.minecraft.entity.LivingEntity).damage(world, world.damageSources.magic(),
              it.maxHealth * 0.2f) };
                                  }
                                  roll < 0.4 -> { // Entanglement
                                     
              player.sendMessage(net.minecraft.text.Text.translatable("message.cresora.weapon.dark_lux.entanglement").formatted(net.minecraft.util.Formatting.GOLD),
              true);
                                      hifumi.cresora.WeaponSkillService.removeMark(target, "dark");
                                      hifumi.cresora.WeaponSkillService.removeMark(target, "lux");
                                      hifumi.cresora.WeaponSkillService.applyMark(target,
              "entanglement", 200L); // 10s
                                  }
                                  else -> { // Dark Collapse
                                     
              player.sendMessage(net.minecraft.text.Text.translatable("message.cresora.weapon.dark_lux.collapse").formatted(net.minecraft.util.Formatting.DARK_PURPLE),
              true);
                                      player.heal(player.maxHealth * 0.05f);
                                      val hpRatio = player.health / player.maxHealth;
                                      val reductionRatio = (1.0f - hpRatio).coerceIn(0.0f, 1.0f);
                                      val remainingTicks =
              hifumi.cresora.WeaponSkillService.getRemainingCooldownTicks(player, "dark_lux");
                                      if (remainingTicks > 0.0) {
                                          hifumi.cresora.WeaponSkillService.startCooldown(player,
              "dark_lux", (remainingTicks * (1.0f - reductionRatio)).toLong());
                                      }
                                  }
                              }
                          }
    }

  }
}
