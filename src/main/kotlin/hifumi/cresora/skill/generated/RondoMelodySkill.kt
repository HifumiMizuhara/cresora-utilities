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
import kotlin.Float
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

public object RondoMelodySkill : WeaponSkillHandler {
  override fun activate(
    player: ServerPlayerEntity,
    definition: WeaponDefinition,
    `data`: WeaponData,
    access: WeaponSkillAccess,
  ): ActionResult {
    WeaponSkillService.startCooldown(player, definition.id, definition.skill.cooldownSeconds * 20L)
    WeaponSkillService.showCooldownBar(player, definition)


    ; run execute@ {
      val value = WeaponCombatSupport.skillValueHp(definition, data)
      WeaponSkillService.grantShield(player, value, definition.skill.durationSeconds * 20L)
      player.sendMessage(Text.translatable("item.cresora.weapon.skill.activated", Text.translatable(definition.translationKey()), WeaponSkillService.formatNumber(value / 2.0)).formatted(Formatting.AQUA), true)
    }


    return ActionResult.SUCCESS
  }

  override fun onPlayerTick(
    player: ServerPlayerEntity,
    definition: WeaponDefinition,
    `data`: WeaponData,
  ) {

    ; run execute@ {
      WeaponSkillService.clearExpiredShield(player)
    }

  }

  override fun onDamageAbsorbed(
    player: ServerPlayerEntity,
    amount: Float,
    definition: WeaponDefinition,
    `data`: WeaponData,
  ): Float {

    ; return run execute@ {
    WeaponSkillService.clearExpiredShield(player)
    val remainingShield = (player as WeaponSkillAccess).cresoraGetShieldHp()
    if (remainingShield > 0.0f) {
                         var remainingAmount = amount;
                         if (remainingAmount <= remainingShield) {
                             (player as WeaponSkillAccess).cresoraSetShieldHp(remainingShield - remainingAmount);
                             if ((player as WeaponSkillAccess).cresoraGetShieldHp() <= 0.0f) WeaponSkillService.clearShield(player);
                             player.sendMessage(Text.translatable("item.cresora.weapon.skill.blocked", WeaponSkillService.formatNumber(remainingAmount / 2.0.toDouble())).formatted(Formatting.AQUA), true);
                             return@execute 0.0f;
                         }
                         (player as WeaponSkillAccess).cresoraSetShieldHp(0.0f);
                         WeaponSkillService.clearShield(player);
                         player.sendMessage(Text.translatable("item.cresora.weapon.skill.broken").formatted(Formatting.RED), true);
                         return@execute remainingAmount - remainingShield;
                     }
    return@execute amount
    }

  }
}
