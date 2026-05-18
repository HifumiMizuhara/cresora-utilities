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
import java.util.UUID
import kotlin.Boolean
import kotlin.Double
import kotlin.Float
import kotlin.Int
import kotlin.Long
import kotlin.collections.MutableMap
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

public object QianqiuYeluoSkill : WeaponSkillHandler {
  public val passiveStateStates: MutableMap<UUID, QianqiuYeluoSkill.PassiveStateState> =
      mutableMapOf()

  public val qiucanStackStates: MutableMap<UUID, QianqiuYeluoSkill.QiucanStackState> =
      mutableMapOf()

  public val jingtianStateStates: MutableMap<UUID, QianqiuYeluoSkill.JingtianStateState> =
      mutableMapOf()

  public val zansouModeStates: MutableMap<UUID, QianqiuYeluoSkill.ZansouModeState> = mutableMapOf()

  public val yorakuManchishoActiveStates:
      MutableMap<UUID, QianqiuYeluoSkill.YorakuManchishoActiveState> = mutableMapOf()

  public val yorakuPowerStates: MutableMap<UUID, QianqiuYeluoSkill.YorakuPowerState> =
      mutableMapOf()

  override fun clearTransientState(playerId: UUID) {
    passiveStateStates.remove(playerId)
    qiucanStackStates.remove(playerId)
    jingtianStateStates.remove(playerId)
    zansouModeStates.remove(playerId)
    yorakuManchishoActiveStates.remove(playerId)
    yorakuPowerStates.remove(playerId)
  }

  override fun pruneTransientState(activePlayerIds: Set<UUID>) {
    passiveStateStates.keys.removeIf { !activePlayerIds.contains(it) }
    qiucanStackStates.keys.removeIf { !activePlayerIds.contains(it) }
    jingtianStateStates.keys.removeIf { !activePlayerIds.contains(it) }
    zansouModeStates.keys.removeIf { !activePlayerIds.contains(it) }
    yorakuManchishoActiveStates.keys.removeIf { !activePlayerIds.contains(it) }
    yorakuPowerStates.keys.removeIf { !activePlayerIds.contains(it) }
  }

  override fun getAttackDamageScalar(player: ServerPlayerEntity): Double {
    var total = 0.0
    total += yorakuPowerStates[player.uuid]?.let { it.stacks * 2.5 } ?: 0.0
    return total
  }

  override fun getCritDamageBonus(player: ServerPlayerEntity): Double {
    var total = 0.0
    total += yorakuPowerStates[player.uuid]?.let { it.stacks * 1.5 } ?: 0.0
    return total
  }

  override fun onPlayerTick(
    player: ServerPlayerEntity,
    definition: WeaponDefinition,
    `data`: WeaponData,
  ) {
    val now = WeaponSkillService.currentWorldTime(player)

    ; if (passiveStateStates.containsKey(player.uuid) && now >=
        passiveStateStates[player.uuid]!!.expireTick) {
      passiveStateStates.remove(player.uuid)
      player.sendMessage(Text.translatable("item.cresora.weapon.skill.buff.passive_state.expired",
          Text.translatable("item.cresora.weapon.skill.buff.passive_state.name")), true)
    }
    ; 

    ; if (qiucanStackStates.containsKey(player.uuid) && now >=
        qiucanStackStates[player.uuid]!!.expireTick) {
      qiucanStackStates.remove(player.uuid)
      player.sendMessage(Text.translatable("item.cresora.weapon.skill.buff.qiucan_stack.expired",
          Text.translatable("item.cresora.weapon.skill.buff.qiucan_stack.name")), true)
    }
    ; 

    ; if (jingtianStateStates.containsKey(player.uuid) && now >=
        jingtianStateStates[player.uuid]!!.expireTick) {
      jingtianStateStates.remove(player.uuid)
      player.sendMessage(Text.translatable("item.cresora.weapon.skill.buff.jingtian_state.expired",
          Text.translatable("item.cresora.weapon.skill.buff.jingtian_state.name")), true)
    }
    ; 

    ; if (zansouModeStates.containsKey(player.uuid) && now >=
        zansouModeStates[player.uuid]!!.expireTick) {
      zansouModeStates.remove(player.uuid)
      player.sendMessage(Text.translatable("item.cresora.weapon.skill.buff.zansou_mode.expired",
          Text.translatable("item.cresora.weapon.skill.buff.zansou_mode.name")), true)
    }
    ; 

    ; if (yorakuManchishoActiveStates.containsKey(player.uuid) && now >=
        yorakuManchishoActiveStates[player.uuid]!!.expireTick) {
      yorakuManchishoActiveStates.remove(player.uuid)
      player.sendMessage(Text.translatable("item.cresora.weapon.skill.buff.yoraku_manchisho_active.expired",
          Text.translatable("item.cresora.weapon.skill.buff.yoraku_manchisho_active.name")), true)
    }
    ; 

    ; if (yorakuPowerStates.containsKey(player.uuid) && now >=
        yorakuPowerStates[player.uuid]!!.expireTick) {
      yorakuPowerStates.remove(player.uuid)
      player.sendMessage(Text.translatable("item.cresora.weapon.skill.buff.yoraku_power.expired",
          Text.translatable("item.cresora.weapon.skill.buff.yoraku_power.name")), true)
    }
    ; 

    ; run execute@ {
      val now = hifumi.cresora.WeaponSkillService.currentWorldTime(player)
                      val passive = QianqiuYeluoSkill.passiveStateStates.get(player.uuid)
                      if (passive != null) {
                          if (!hifumi.cresora.WeaponSkillService.hasStatus(player, "zansou_mode")) {
                              if (now % 40L == 0L && player.health / player.maxHealth > 0.30f) {
                                  val damage = player.maxHealth * 0.02f
                                  player.damage(player.world, (player.world as?
          net.minecraft.server.world.ServerWorld)?.damageSources?.magic() ?:
          player.damageSources.magic(), damage)
                              }
                          }
                          if (passive.expireTick - now <= 1L) {
                              player.heal(player.maxHealth * 0.30f)
                             
          player.sendMessage(net.minecraft.text.Text.translatable("item.cresora.weapon.qianqiu_yeluo.passive_healed").formatted(net.minecraft.util.Formatting.GREEN),
          true)
                          }
                      }
                      if (hifumi.cresora.WeaponSkillService.hasStatus(player,
          "yoraku_manchisho_active")) {
                          val state = QianqiuYeluoSkill.qiucanStackStates.get(player.uuid)
                          if (state == null || state.stacks <= 0) {
                              QianqiuYeluoSkill.yorakuManchishoActiveStates.remove(player.uuid)
                              QianqiuYeluoSkill.yorakuPowerStates.remove(player.uuid)
                             
          player.sendMessage(net.minecraft.text.Text.translatable("item.cresora.weapon.qianqiu_yeluo.yoraku_ended").formatted(net.minecraft.util.Formatting.GRAY),
          true)
                          }
                      }
    }

  }

  override fun activate(
    player: ServerPlayerEntity,
    definition: WeaponDefinition,
    `data`: WeaponData,
    access: WeaponSkillAccess,
  ): ActionResult {
    ; {
        val now = WeaponSkillService.currentWorldTime(player)
        val state = QianqiuYeluoSkill.qiucanStackStates.getOrPut(player.uuid) {
        QianqiuYeluoSkill.QiucanStackState(0L, 0) }
        state.expireTick = now + 300 * 20L
        state.stacks = (state.stacks + 1).coerceAtMost(40)
        player.sendMessage(Text.translatable("item.cresora.weapon.skill.buff.qiucan_stack.gained",
        Text.translatable("item.cresora.weapon.skill.buff.qiucan_stack.name"), state.stacks), true)
    }
    HotbarOverrideService.overrideHotbar(player, definition.id, listOf("danrai", "zansou",
        "yoraku_manchisho"), 60)


    return ActionResult.SUCCESS
  }

  override fun onDamageTaken(
    player: ServerPlayerEntity,
    amount: Float,
    definition: WeaponDefinition,
    `data`: WeaponData,
  ): Float {

    ; return run execute@ {
    var finalAmount = amount
                    if (hifumi.cresora.WeaponSkillService.hasStatus(player, "passive_state")) {
                        finalAmount *= 0.40f
                    }
                    if (hifumi.cresora.WeaponSkillService.hasStatus(player,
        "yoraku_manchisho_active")) {
                        if (finalAmount >= player.health) {
                            finalAmount = (player.health - 1.0f).coerceAtLeast(0.0f)
                        }
                    }
                    return@execute finalAmount
    amount
    }

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
      if (isTrueDamage) return@execute
                      val world = player.world as? net.minecraft.server.world.ServerWorld ?:
          return@execute

                      if (hifumi.cresora.WeaponSkillService.hasStatus(player, "passive_state")) {
                          val missingHp = (player.maxHealth - player.health).coerceAtLeast(0.0f)
                          val boost = (missingHp / 2.0f * 0.01f).coerceAtMost(0.20f)
                          if (boost > 0) {
                              target.damage(world, world.damageSources.magic(), amount * boost)
                          }
                      }

                      if (hifumi.cresora.WeaponSkillService.hasStatus(player, "jingtian_state")) {
                          val zansouBoost = if (hifumi.cresora.WeaponSkillService.hasStatus(player,
          "zansou_mode")) 1.5f else 1.0f
                          target.damage(world, world.damageSources.magic(), (amount * 0.15f) *
          zansouBoost)
                          if (world.random.nextDouble() < 0.30) {
                              hifumi.cresora.WeaponSkillService.applyMark(target, "root", 120L)
                             
          target.addStatusEffect(net.minecraft.entity.effect.StatusEffectInstance(net.minecraft.entity.effect.StatusEffects.SLOWNESS,
          120, 255, false, false, true))
                          }
                      }

                      if (hifumi.cresora.WeaponSkillService.hasStatus(player, "zansou_mode")) {
                          hifumi.cresora.WeaponSkillService.applyMark(target, "lux", 200L)
                      }

                      if (hifumi.cresora.WeaponSkillService.hasStatus(player,
          "yoraku_manchisho_active")) {
                          val aoeRange = 4.5
                          val baseDamage =
          hifumi.cresora.WeaponCombatSupport.attackDamage(definition, data).toFloat()
                          world.getEntitiesByClass(net.minecraft.entity.LivingEntity::class.java,
          player.boundingBox.expand(aoeRange)) { it.isAlive && it != player }
                              .forEach { entity ->
                                   entity.damage(world, world.damageSources.magic(), baseDamage *
          1.5f)
                              }

                          val state = QianqiuYeluoSkill.qiucanStackStates.get(player.uuid)
                          if (state != null && state.stacks >= 4) {
                              state.stacks -= 4
                              player.heal(player.maxHealth * 0.04f)
                              val powerState =
          QianqiuYeluoSkill.yorakuPowerStates.getOrPut(player.uuid) {
          QianqiuYeluoSkill.YorakuPowerState(0L, 0) }
                              powerState.expireTick = world.time + 300L
                              powerState.stacks = (powerState.stacks + 4).coerceAtMost(100)
                             
          player.sendMessage(net.minecraft.text.Text.translatable("item.cresora.weapon.qianqiu_yeluo.yoraku_hit",
          state.stacks, powerState.stacks).formatted(net.minecraft.util.Formatting.GOLD), true)
                          }
                      }
    }

  }

  public data class PassiveStateState(
    public var expireTick: Long,
    public var stacks: Int,
  )

  public data class QiucanStackState(
    public var expireTick: Long,
    public var stacks: Int,
  )

  public data class JingtianStateState(
    public var expireTick: Long,
    public var stacks: Int,
  )

  public data class ZansouModeState(
    public var expireTick: Long,
    public var stacks: Int,
  )

  public data class YorakuManchishoActiveState(
    public var expireTick: Long,
    public var stacks: Int,
  )

  public data class YorakuPowerState(
    public var expireTick: Long,
    public var stacks: Int,
  )
}
