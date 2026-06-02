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
import kotlin.Double
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

public object ThoughtsoftheDistantBoySkill : WeaponSkillHandler {
  public val passiveStatsStates:
      ConcurrentHashMap<UUID, ThoughtsoftheDistantBoySkill.PassiveStatsState> = ConcurrentHashMap()

  public val shojoResolveStates:
      ConcurrentHashMap<UUID, ThoughtsoftheDistantBoySkill.ShojoResolveState> = ConcurrentHashMap()

  override fun clearTransientState(playerId: UUID) {
    passiveStatsStates.remove(playerId)
    shojoResolveStates.remove(playerId)
  }

  override fun pruneTransientState(activePlayerIds: Set<UUID>) {
    passiveStatsStates.keys.removeIf { !activePlayerIds.contains(it) }
    shojoResolveStates.keys.removeIf { !activePlayerIds.contains(it) }
  }

  override fun getCritRateBonus(player: ServerPlayerEntity): Double {
    var total = 0.0
    total += shojoResolveStates[player.uuid]?.let { it.stacks * 5.0 } ?: 0.0
    return total
  }

  override fun getCritDamageBonus(player: ServerPlayerEntity): Double {
    var total = 0.0
    total += passiveStatsStates[player.uuid]?.let { it.stacks * 10.0 } ?: 0.0
    total += shojoResolveStates[player.uuid]?.let { it.stacks * 60.0 } ?: 0.0
    return total
  }

  override fun getAllDamageBonus(player: ServerPlayerEntity): Double {
    var total = 0.0
    total += passiveStatsStates[player.uuid]?.let { it.stacks * 10.0 } ?: 0.0
    total += shojoResolveStates[player.uuid]?.let { it.stacks * 25.0 } ?: 0.0
    return total
  }

  override fun onPlayerTick(
    player: ServerPlayerEntity,
    definition: WeaponDefinition,
    `data`: WeaponData,
  ) {
    val now = WeaponSkillService.currentWorldTime(player)

    ; if (passiveStatsStates.containsKey(player.uuid) && now >=
        passiveStatsStates[player.uuid]!!.expireTick) {
      passiveStatsStates.remove(player.uuid)
      player.sendMessage(Text.translatable("item.cresora.weapon.skill.buff.passive_stats.expired",
          Text.translatable("item.cresora.weapon.skill.buff.passive_stats.name")), true)
    }
    ; 

    ; if (shojoResolveStates.containsKey(player.uuid) && now >=
        shojoResolveStates[player.uuid]!!.expireTick) {
      shojoResolveStates.remove(player.uuid)
      player.sendMessage(Text.translatable("item.cresora.weapon.skill.buff.shojo_resolve.expired",
          Text.translatable("item.cresora.weapon.skill.buff.shojo_resolve.name")), true)
    }
    ; 

    ; run execute@ {
      val passiveState = ThoughtsoftheDistantBoySkill.passiveStatsStates.getOrPut(player.uuid) { ThoughtsoftheDistantBoySkill.PassiveStatsState(0L, 0) }
      passiveState.expireTick = now + 100 * 20L
      passiveState.stacks = 1
      val hasShojo = hifumi.cresora.weapon.WeaponSkillService.hasWeaponInInventory(player, "harukanaru_shojo_no_ketsui")
      if (hasShojo) {
                          if (hifumi.cresora.weapon.WeaponSkillService.hasMark(player, "nageki")) {
                              hifumi.cresora.weapon.WeaponSkillService.removeMark(player, "nageki");
                              val access = player as? hifumi.cresora.weapon.WeaponSkillAccess;
                              if (access != null && access.cresoraGetShieldWeaponId() == definition.id) {
                                  hifumi.cresora.weapon.WeaponSkillService.clearShield(player);
                              }
                          }
                          
                          val resolveState = ThoughtsoftheDistantBoySkill.shojoResolveStates.getOrPut(player.uuid) { ThoughtsoftheDistantBoySkill.ShojoResolveState(0L, 0) };
                          resolveState.expireTick = now + 100 * 20L;
                          resolveState.stacks = 1;
                      } else {
                          hifumi.cresora.weapon.WeaponSkillService.applyMark(player, "nageki", 10L);
                          ThoughtsoftheDistantBoySkill.shojoResolveStates.remove(player.uuid);

                          if (now % 200L == 0L) {
                              val access = player as? hifumi.cresora.weapon.WeaponSkillAccess;
                              if (access != null) {
                                  val currentShield = access.cresoraGetShieldHp();
                                  if (currentShield < 6.0f) {
                                      if (player.random.nextDouble() < 0.5) {
                                          hifumi.cresora.weapon.WeaponSkillService.grantShield(player, 6.0f, 99999999L, definition.id);
                                          player.sendMessage(
                                              net.minecraft.text.Text.translatable("item.cresora.weapon.skill.harukanaru_shonen_no_omoi.grief_shield_gained")
                                                  .formatted(net.minecraft.util.Formatting.BLUE),
                                              true
                                          );
                                      }
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

    ; run execute@ {
      val isGriefActive = hifumi.cresora.weapon.WeaponSkillService.hasMark(player, "nageki")
      if (isGriefActive) {
                          // Pattern A
                          hifumi.cresora.weapon.WeaponSkillService.startCooldown(player, definition.id, 200L);
                          hifumi.cresora.weapon.WeaponSkillService.showCooldownBar(player, definition);
                          
                          val shieldHearts = 10.0 + (data.baseLevel - 1) * 0.5;
                          hifumi.cresora.weapon.WeaponSkillService.grantShield(player, (shieldHearts * 2.0).toFloat(), 99999999L, definition.id);
                          
                          player.sendMessage(
                              net.minecraft.text.Text.translatable(
                                  "item.cresora.weapon.skill.harukanaru_shonen_no_omoi.pattern_a",
                                  hifumi.cresora.weapon.WeaponSkillService.formatNumber(shieldHearts)
                              ).formatted(net.minecraft.util.Formatting.AQUA),
                              true
                          );
                      } else {
                          // Pattern B
                          hifumi.cresora.weapon.WeaponSkillService.startCooldown(player, definition.id, 700L);
                          hifumi.cresora.weapon.WeaponSkillService.showCooldownBar(player, definition);
                          
                          val world = player.world as? net.minecraft.server.world.ServerWorld ?: return@execute;
                          val range = definition.skill.radiusMeters;
                          val targets = world.getOtherEntities(player, player.boundingBox.expand(range)) {
                              it is net.minecraft.entity.LivingEntity &&
                              it.isAlive &&
                              it !is net.minecraft.entity.player.PlayerEntity
                          }
                          .filterIsInstance<net.minecraft.entity.LivingEntity>()
                          .sortedBy { it.squaredDistanceTo(player) }
                          .take(5);

                          val playerAtk = player.getAttributeValue(net.minecraft.entity.attribute.EntityAttributes.ATTACK_DAMAGE);
                          val damageAmount = (playerAtk * (3.0 + data.skillLevel * 0.5)).toFloat();

                          targets.forEach { target ->
                              target.damage(
                                  world,
                                  world.damageSources.indirectMagic(player, player),
                                  damageAmount
                              );
                              world.spawnParticles(
                                  net.minecraft.particle.ParticleTypes.CRIT,
                                  target.x, target.y + 1.0, target.z,
                                  5, 0.3, 0.3, 0.3, 0.1
                              );
                          }

                          player.sendMessage(
                              net.minecraft.text.Text.translatable(
                                  "item.cresora.weapon.skill.harukanaru_shonen_no_omoi.pattern_b",
                                  targets.size,
                                  hifumi.cresora.weapon.WeaponSkillService.formatNumber(damageAmount.toDouble())
                              ).formatted(net.minecraft.util.Formatting.LIGHT_PURPLE),
                              true
                          );
                      }
    }


    return ActionResult.SUCCESS
  }

  public data class PassiveStatsState(
    public var expireTick: Long,
    public var stacks: Int,
  )

  public data class ShojoResolveState(
    public var expireTick: Long,
    public var stacks: Int,
  )
}
