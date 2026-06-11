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
    WeaponSkillService.applyMark(target, "lux", 600L)
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
      if (hifumi.cresora.weapon.WeaponStackSupport.getDefinition(player.mainHandStack)?.id != "dark_lux") return@execute
      if (isTrueDamage) return@execute
      val now = hifumi.cresora.weapon.WeaponSkillService.currentWorldTime(player)
      if (hifumi.cresora.weapon.WeaponSkillService.hasMark(target, "entanglement")) return@execute
      hifumi.cresora.weapon.WeaponSkillService.applyMark(target, "dark", 200L)
      if (hifumi.cresora.weapon.WeaponSkillService.hasMark(target, "lux") && hifumi.cresora.weapon.WeaponSkillService.hasMark(target, "dark")) {
                          val world = player.world as? net.minecraft.server.world.ServerWorld ?: return@execute;
                          val roll = world.random.nextDouble();
                          when {
                              roll < 0.2 -> { // Annihilation
                                  player.sendMessage(net.minecraft.text.Text.translatable("message.cresora.weapon.dark_lux.annihilation").formatted(net.minecraft.util.Formatting.DARK_RED), true);
                                  hifumi.cresora.weapon.WeaponSkillService.removeMark(target, "dark");
                                  hifumi.cresora.weapon.WeaponSkillService.removeMark(target, "lux");
                                  val damage = target.health * 0.9f;
                                  target.damage(world, world.damageSources.magic(), damage);
                                  world.getOtherEntities(target, target.boundingBox.expand(5.0)) { it is net.minecraft.entity.LivingEntity && it.isAlive }
                                      .take(3).forEach { (it as net.minecraft.entity.LivingEntity).damage(world, world.damageSources.magic(), it.maxHealth * 0.2f) };
                              }
                              roll < 0.4 -> { // Entanglement
                                  player.sendMessage(net.minecraft.text.Text.translatable("message.cresora.weapon.dark_lux.entanglement").formatted(net.minecraft.util.Formatting.GOLD), true);
                                  hifumi.cresora.weapon.WeaponSkillService.removeMark(target, "dark");
                                  hifumi.cresora.weapon.WeaponSkillService.removeMark(target, "lux");
                                  hifumi.cresora.weapon.WeaponSkillService.applyMark(target, "entanglement", 200L); // 10s
                              }
                              else -> { // Dark Collapse
                                  player.sendMessage(net.minecraft.text.Text.translatable("message.cresora.weapon.dark_lux.collapse").formatted(net.minecraft.util.Formatting.DARK_PURPLE), true);
                                  player.heal(player.maxHealth * 0.05f);
                                  val hpRatio = player.health / player.maxHealth;
                                  val reductionRatio = (1.0f - hpRatio).coerceIn(0.0f, 1.0f);
                                  val remainingTicks = hifumi.cresora.weapon.WeaponSkillService.getRemainingCooldownTicks(player, "dark_lux");
                                  if (remainingTicks > 0.0) {
                                      hifumi.cresora.weapon.WeaponSkillService.startCooldown(player, "dark_lux", (remainingTicks * (1.0f - reductionRatio)).toLong());
                                  }
                              }
                          }
                      }
    }

  }
}
