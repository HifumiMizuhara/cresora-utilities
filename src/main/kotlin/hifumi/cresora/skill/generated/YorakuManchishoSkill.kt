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

public object YorakuManchishoSkill : WeaponSkillHandler {
  override fun activate(
    player: ServerPlayerEntity,
    definition: WeaponDefinition,
    `data`: WeaponData,
    access: WeaponSkillAccess,
  ): ActionResult {

    ; run execute@ {
      val state = QianqiuYeluoSkill.qiucanStackStates.get(player.uuid)
      if (state == null || state.stacks < 20) {
                          player.sendMessage(Text.translatable("item.cresora.weapon.qianqiu_yeluo.insufficient_stacks").formatted(Formatting.RED), true);
                          return@execute;
                      }
      val buffState = QianqiuYeluoSkill.yorakuManchishoActiveStates.getOrPut(player.uuid) {
                          QianqiuYeluoSkill.YorakuManchishoActiveState(0L, 0)
                      }
      buffState.expireTick = WeaponSkillService.currentWorldTime(player) + 60 * 20L
      buffState.stacks = 1
      HotbarOverrideService.restoreHotbar(player)
    }

    player.sendMessage(Text.translatable("item.cresora.weapon.qianqiu_yeluo.yoraku_activated").formatted(Formatting.GOLD),
        true)


    return ActionResult.SUCCESS
  }
}
