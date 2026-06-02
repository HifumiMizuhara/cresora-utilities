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
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.Boolean
import kotlin.Float
import kotlin.Int
import kotlin.Long
import kotlin.collections.Set
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

public object TanbokuChokuuSkill : WeaponSkillHandler {
  public val zansoActiveStates: ConcurrentHashMap<UUID, TanbokuChokuuSkill.ZansoActiveState> =
      ConcurrentHashMap()

  public val munenSeqStates: ConcurrentHashMap<UUID, TanbokuChokuuSkill.MunenSeqState> =
      ConcurrentHashMap()

  override fun clearTransientState(playerId: UUID) {
    zansoActiveStates.remove(playerId)
    munenSeqStates.remove(playerId)
  }

  override fun pruneTransientState(activePlayerIds: Set<UUID>) {
    zansoActiveStates.keys.removeIf { !activePlayerIds.contains(it) }
    munenSeqStates.keys.removeIf { !activePlayerIds.contains(it) }
  }

  override fun onPlayerTick(
    player: ServerPlayerEntity,
    definition: WeaponDefinition,
    `data`: WeaponData,
  ) {
    val now = WeaponSkillService.currentWorldTime(player)

    ; if (zansoActiveStates.containsKey(player.uuid) && now >=
        zansoActiveStates[player.uuid]!!.expireTick) {
      zansoActiveStates.remove(player.uuid)
      player.sendMessage(Text.translatable("item.cresora.weapon.skill.buff.zanso_active.expired",
          Text.translatable("item.cresora.weapon.skill.buff.zanso_active.name")), true)
    }
    ; 

    ; if (munenSeqStates.containsKey(player.uuid) && now >=
        munenSeqStates[player.uuid]!!.expireTick) {
      munenSeqStates.remove(player.uuid)
      player.sendMessage(Text.translatable("item.cresora.weapon.skill.buff.munen_seq.expired",
          Text.translatable("item.cresora.weapon.skill.buff.munen_seq.name")), true)
    }
    ; 
  }

  override fun activate(
    player: ServerPlayerEntity,
    definition: WeaponDefinition,
    `data`: WeaponData,
    access: WeaponSkillAccess,
  ): ActionResult {

    ; run execute@ {
      WeaponSkillService.addTao(player, 1)
      player.sendMessage(Text.translatable("item.cresora.weapon.tanboku_chokuu.tao_gained", WeaponSkillService.getTao(player)).formatted(Formatting.GOLD), true)
    }

    HotbarOverrideService.overrideHotbar(player, definition.id, listOf("danro", "zanso",
        "bokuchu_munen"), 60)


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
      val stacks = WeaponSkillService.getSoulBreakStacks(target)
      if (stacks > 0 && !isTrueDamage) {
                          val boost = amount * (stacks * 0.04f);
                          target.damage(player.world, player.world.damageSources.magic(), boost);
                          if (player.world.time % 20L == 0L) {
                               player.sendMessage(Text.translatable("item.cresora.weapon.tanboku_chokuu.soul_break", stacks, stacks * 5, stacks * 4).formatted(Formatting.GRAY), true);
                          }
                      }
    }

  }

  public data class ZansoActiveState(
    public var expireTick: Long,
    public var stacks: Int,
  )

  public data class MunenSeqState(
    public var expireTick: Long,
    public var stacks: Int,
  )
}
