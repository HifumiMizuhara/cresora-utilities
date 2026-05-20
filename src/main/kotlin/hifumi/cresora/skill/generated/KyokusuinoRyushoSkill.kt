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

public object KyokusuinoRyushoSkill : WeaponSkillHandler {
  public val raiseACupStates: MutableMap<UUID, KyokusuinoRyushoSkill.RaiseACupState> =
      mutableMapOf()

  public val recitePoetryStates: MutableMap<UUID, KyokusuinoRyushoSkill.RecitePoetryState> =
      mutableMapOf()

  public val zhiPierceStates: MutableMap<UUID, KyokusuinoRyushoSkill.ZhiPierceState> =
      mutableMapOf()

  public val pavilionActiveStates: MutableMap<UUID, KyokusuinoRyushoSkill.PavilionActiveState> =
      mutableMapOf()

  override fun clearTransientState(playerId: UUID) {
    raiseACupStates.remove(playerId)
    recitePoetryStates.remove(playerId)
    zhiPierceStates.remove(playerId)
    pavilionActiveStates.remove(playerId)
  }

  override fun pruneTransientState(activePlayerIds: Set<UUID>) {
    raiseACupStates.keys.removeIf { !activePlayerIds.contains(it) }
    recitePoetryStates.keys.removeIf { !activePlayerIds.contains(it) }
    zhiPierceStates.keys.removeIf { !activePlayerIds.contains(it) }
    pavilionActiveStates.keys.removeIf { !activePlayerIds.contains(it) }
  }

  override fun getAttackDamageScalar(player: ServerPlayerEntity): Double {
    var total = 0.0
    total += raiseACupStates[player.uuid]?.let { it.stacks * 0.1 } ?: 0.0
    return total
  }

  override fun getArmorScalar(player: ServerPlayerEntity): Double {
    var total = 0.0
    total += recitePoetryStates[player.uuid]?.let { it.stacks * 0.2 } ?: 0.0
    return total
  }

  override fun getCritDamageBonus(player: ServerPlayerEntity): Double {
    var total = 0.0
    total += raiseACupStates[player.uuid]?.let { it.stacks * 15.0 } ?: 0.0
    return total
  }

  override fun onPlayerTick(
    player: ServerPlayerEntity,
    definition: WeaponDefinition,
    `data`: WeaponData,
  ) {
    val now = WeaponSkillService.currentWorldTime(player)

    ; if (raiseACupStates.containsKey(player.uuid) && now >=
        raiseACupStates[player.uuid]!!.expireTick) {
      raiseACupStates.remove(player.uuid)
      player.sendMessage(Text.translatable("item.cresora.weapon.skill.buff.raise_a_cup.expired",
          Text.translatable("item.cresora.weapon.skill.buff.raise_a_cup.name")), true)
    }
    ; 

    ; if (recitePoetryStates.containsKey(player.uuid) && now >=
        recitePoetryStates[player.uuid]!!.expireTick) {
      recitePoetryStates.remove(player.uuid)
      player.sendMessage(Text.translatable("item.cresora.weapon.skill.buff.recite_poetry.expired",
          Text.translatable("item.cresora.weapon.skill.buff.recite_poetry.name")), true)
    }
    ; 

    ; if (zhiPierceStates.containsKey(player.uuid) && now >=
        zhiPierceStates[player.uuid]!!.expireTick) {
      zhiPierceStates.remove(player.uuid)
      player.sendMessage(Text.translatable("item.cresora.weapon.skill.buff.zhi_pierce.expired",
          Text.translatable("item.cresora.weapon.skill.buff.zhi_pierce.name")), true)
    }
    ; 

    ; if (pavilionActiveStates.containsKey(player.uuid) && now >=
        pavilionActiveStates[player.uuid]!!.expireTick) {
      pavilionActiveStates.remove(player.uuid)
      player.sendMessage(Text.translatable("item.cresora.weapon.skill.buff.pavilion_active.expired",
          Text.translatable("item.cresora.weapon.skill.buff.pavilion_active.name")), true)
    }
    ; 

    ; run execute@ {
      val now = hifumi.cresora.WeaponSkillService.currentWorldTime(player)
      val state = KyokusuinoRyushoSkill.pavilionActiveStates.get(player.uuid)
      if (state != null && now < state.expireTick) {
                              // Pulse every 2 seconds (40 ticks)
                              if (now % 40L == 0L) {
                                  val world = player.world;
                                  val roll = world.random.nextInt(4);
                                  when (roll) {
                                      0 -> { // Raise a Cup
                                          // Add raise_a_cup and zhi_pierce
                                          val rState =
              KyokusuinoRyushoSkill.raiseACupStates.getOrPut(player.uuid) {
              KyokusuinoRyushoSkill.RaiseACupState(0L, 0) };
                                          rState.expireTick = now + 16 * 20L;
                                          rState.stacks = (rState.stacks + 1).coerceAtMost(20);

                                          val zState =
              KyokusuinoRyushoSkill.zhiPierceStates.getOrPut(player.uuid) {
              KyokusuinoRyushoSkill.ZhiPierceState(0L, 0) };
                                          zState.expireTick = now + 16 * 20L;
                                          zState.stacks = (zState.stacks + 1).coerceAtMost(20);

                                         
              player.sendMessage(net.minecraft.text.Text.translatable("item.cresora.weapon.skill.orchid_pavilion_echo.raise_a_cup",
              rState.stacks, hifumi.cresora.WeaponSkillService.formatNumber(rState.stacks * 10.0),
              hifumi.cresora.WeaponSkillService.formatNumber(rState.stacks * 15.0), zState.stacks,
              hifumi.cresora.WeaponSkillService.formatNumber(zState.stacks *
              1.0)).formatted(net.minecraft.util.Formatting.RED), true);
                                      }
                                      1 -> { // Recite Poetry
                                          val pState =
              KyokusuinoRyushoSkill.recitePoetryStates.getOrPut(player.uuid) {
              KyokusuinoRyushoSkill.RecitePoetryState(0L, 0) };
                                          pState.expireTick = now + 16 * 20L;
                                          pState.stacks = (pState.stacks + 1).coerceAtMost(20);
                                         
              player.sendMessage(net.minecraft.text.Text.translatable("item.cresora.weapon.skill.orchid_pavilion_echo.recite_poetry",
              pState.stacks, hifumi.cresora.WeaponSkillService.formatNumber(pState.stacks * 20.0),
              pState.stacks * 2).formatted(net.minecraft.util.Formatting.BLUE), true);
                                      }
                                      2 -> { // Place a Stone (Shield)
                                          val amount = 7.0f;
                                          val duration = 16L;
                                          (player as
              hifumi.cresora.WeaponSkillAccess).cresoraSetShieldHp((player as
              hifumi.cresora.WeaponSkillAccess).cresoraGetShieldHp() + amount);
                                          (player as
              hifumi.cresora.WeaponSkillAccess).cresoraSetShieldExpireTick(now + duration * 20L);
                                         
              player.sendMessage(net.minecraft.text.Text.translatable("item.cresora.weapon.skill.orchid_pavilion_echo.place_a_stone",
              1, hifumi.cresora.WeaponSkillService.formatNumber(amount /
              2.0)).formatted(net.minecraft.util.Formatting.AQUA), true);
                                      }
                                      3 -> { // Ink Brush (Invulnerability + Heal)
                                         
              hifumi.cresora.WeaponSkillService.grantInvulnerability(player, 40L); // 2s
                                          hifumi.cresora.WeaponSkillService.healNearbyAllies(player,
              5.0, 8.0f);
                                         
              player.sendMessage(net.minecraft.text.Text.translatable("item.cresora.weapon.skill.orchid_pavilion_echo.ink_brush",
              1, hifumi.cresora.WeaponSkillService.formatNumber(4.0),
              hifumi.cresora.WeaponSkillService.formatNumber(2.0)).formatted(net.minecraft.util.Formatting.LIGHT_PURPLE),
              true);
                                      }
                                  }
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
    WeaponSkillService.startCooldown(player, definition.id, definition.skill.cooldownSeconds * 20L)
    WeaponSkillService.showCooldownBar(player, definition)

    ; {
        val now = WeaponSkillService.currentWorldTime(player)
        val state = KyokusuinoRyushoSkill.pavilionActiveStates.getOrPut(player.uuid) {
        KyokusuinoRyushoSkill.PavilionActiveState(0L, 0) }
        state.expireTick = now + 16 * 20L
        state.stacks = (state.stacks + 1).coerceAtMost(1)
       
        player.sendMessage(Text.translatable("item.cresora.weapon.skill.buff.pavilion_active.gained",
        Text.translatable("item.cresora.weapon.skill.buff.pavilion_active.name"), state.stacks),
        true)
    }
    player.sendMessage(Text.translatable("item.cresora.weapon.skill.orchid_pavilion_echo_activated.generic").formatted(Formatting.LIGHT_PURPLE),
        true)


    return ActionResult.SUCCESS
  }

  override fun onDamageAbsorbed(
    player: ServerPlayerEntity,
    amount: Float,
    definition: WeaponDefinition,
    `data`: WeaponData,
  ): Float {

    ; return run execute@ {
    if (hifumi.cresora.WeaponSkillService.isInvulnerable(player)) {
                           
            player.sendMessage(net.minecraft.text.Text.translatable("item.cresora.weapon.skill.orchid_pavilion_echo.invulnerable").formatted(net.minecraft.util.Formatting.LIGHT_PURPLE),
            true);
                            return@execute 0.0f;
                        }
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
      val zState = KyokusuinoRyushoSkill.zhiPierceStates.get(player.uuid)
      if (zState != null && zState.stacks > 0) {
                              val damage = zState.stacks * 2.0f;
                              if (target is net.minecraft.entity.mob.HostileEntity) {
                                  target.health = (target.health - damage).coerceAtLeast(0.001f);
                                  hifumi.cresora.AdventureRankService.showMobTrueDamage(target,
              player, damage);
                                  hifumi.cresora.AdventureRankService.refreshMobDisplay(target);
                              }
                          }
    }

  }

  public data class RaiseACupState(
    public var expireTick: Long,
    public var stacks: Int,
  )

  public data class RecitePoetryState(
    public var expireTick: Long,
    public var stacks: Int,
  )

  public data class ZhiPierceState(
    public var expireTick: Long,
    public var stacks: Int,
  )

  public data class PavilionActiveState(
    public var expireTick: Long,
    public var stacks: Int,
  )
}
