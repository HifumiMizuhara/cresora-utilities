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
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.Boolean
import kotlin.Double
import kotlin.Float
import kotlin.Int
import kotlin.Long
import kotlin.collections.Set
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

public object QianqiuYeluoSkill : WeaponSkillHandler {
  public val passiveStateStates: ConcurrentHashMap<UUID, QianqiuYeluoSkill.PassiveStateState> =
      ConcurrentHashMap()

  public val qiucanStackStates: ConcurrentHashMap<UUID, QianqiuYeluoSkill.QiucanStackState> =
      ConcurrentHashMap()

  public val jingtianStateStates: ConcurrentHashMap<UUID, QianqiuYeluoSkill.JingtianStateState> =
      ConcurrentHashMap()

  public val zansouModeStates: ConcurrentHashMap<UUID, QianqiuYeluoSkill.ZansouModeState> =
      ConcurrentHashMap()

  public val yorakuManchishoActiveStates:
      ConcurrentHashMap<UUID, QianqiuYeluoSkill.YorakuManchishoActiveState> = ConcurrentHashMap()

  public val yorakuPowerStates: ConcurrentHashMap<UUID, QianqiuYeluoSkill.YorakuPowerState> =
      ConcurrentHashMap()

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
      val now = WeaponSkillService.currentWorldTime(player)
      val passive = QianqiuYeluoSkill.passiveStateStates.get(player.uuid)
      if (passive != null) {
                          if (!WeaponSkillService.hasStatus(player, "zansou_mode")) {
                              if (now % 40L == 0L && player.health / player.maxHealth > 0.30f) {
                                  val damage = player.maxHealth * 0.02f
                                  player.damage(player.world, (player.world as? ServerWorld)?.damageSources?.magic() ?: player.damageSources.magic(), damage)
                              }
                          }
                          if (passive.expireTick - now <= 1L) {
                              player.heal(player.maxHealth * 0.30f)
                              player.sendMessage(Text.translatable("item.cresora.weapon.qianqiu_yeluo.passive_healed").formatted(Formatting.GREEN), true)
                          }
                      }
      if (WeaponSkillService.hasStatus(player, "yoraku_manchisho_active")) {
                          val state = QianqiuYeluoSkill.qiucanStackStates.get(player.uuid)
                          if (state == null || state.stacks <= 0) {
                              QianqiuYeluoSkill.yorakuManchishoActiveStates.remove(player.uuid)
                              QianqiuYeluoSkill.yorakuPowerStates.remove(player.uuid)
                              player.sendMessage(Text.translatable("item.cresora.weapon.qianqiu_yeluo.yoraku_ended").formatted(Formatting.GRAY), true)
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
    run {
        val now = WeaponSkillService.currentWorldTime(player)
        val state = QianqiuYeluoSkill.qiucanStackStates.getOrPut(player.uuid) {
        QianqiuYeluoSkill.QiucanStackState(0L, 0) }
        state.expireTick = now + 300 * 20L
        state.stacks = (state.stacks + 1).coerceAtMost(40)
        player.sendMessage(Text.translatable("item.cresora.weapon.skill.buff.qiucan_stack.gained",
        Text.translatable("item.cresora.weapon.skill.buff.qiucan_stack.name"),
        WeaponSkillService.getDisplayStacks(player, "qiucan_stack", state.stacks)), true)
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
    if (WeaponSkillService.hasStatus(player, "passive_state")) {
                        finalAmount *= 0.40f
                    }
    if (WeaponSkillService.hasStatus(player, "yoraku_manchisho_active")) {
                        if (finalAmount >= player.health) {
                            finalAmount = (player.health - 1.0f).coerceAtLeast(0.0f)
                        }
                    }
    return@execute finalAmount
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
      val world = player.world as? ServerWorld ?: return@execute
      if (WeaponSkillService.hasStatus(player, "passive_state")) {
                          val missingHp = (player.maxHealth - player.health).coerceAtLeast(0.0f)
                          val boost = (missingHp / 2.0f * 0.01f).coerceAtMost(0.20f)
                          if (boost > 0) {
                              target.damage(world, world.damageSources.magic(), amount * boost)
                          }
                      }
      if (WeaponSkillService.hasStatus(player, "jingtian_state")) {
                          val zansouBoost = if (WeaponSkillService.hasStatus(player, "zansou_mode")) 1.5f else 1.0f
                          target.damage(world, world.damageSources.magic(), (amount * 0.15f) * zansouBoost)
                          if (world.random.nextDouble() < 0.30) {
                              WeaponSkillService.applyMark(target, "root", 120L)
                              target.addStatusEffect(StatusEffectInstance(StatusEffects.SLOWNESS, 120, 255, false, false, true))
                          }
                      }
      if (WeaponSkillService.hasStatus(player, "zansou_mode")) {
                          WeaponSkillService.applyMark(target, "lux", 200L)
                      }
      if (WeaponSkillService.hasStatus(player, "yoraku_manchisho_active")) {
                          val aoeRange = 4.5
                          val baseDamage = WeaponCombatSupport.attackDamage(definition, data).toFloat()
                          world.getEntitiesByClass(LivingEntity::class.java, player.boundingBox.expand(aoeRange)) { it.isAlive && it != player }
                              .forEach { entity ->
                                   entity.damage(world, world.damageSources.magic(), baseDamage * 1.5f)
                              }

                          val state = QianqiuYeluoSkill.qiucanStackStates.get(player.uuid)
                          if (state != null && state.stacks >= 4) {
                              state.stacks -= 4
                              player.heal(player.maxHealth * 0.04f)
                              val powerState = QianqiuYeluoSkill.yorakuPowerStates.getOrPut(player.uuid) { QianqiuYeluoSkill.YorakuPowerState(0L, 0) }
                              powerState.expireTick = world.time + 300L
                              powerState.stacks = (powerState.stacks + 4).coerceAtMost(100)
                              player.sendMessage(Text.translatable("item.cresora.weapon.qianqiu_yeluo.yoraku_hit", state.stacks, powerState.stacks).formatted(Formatting.GOLD), true)
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
