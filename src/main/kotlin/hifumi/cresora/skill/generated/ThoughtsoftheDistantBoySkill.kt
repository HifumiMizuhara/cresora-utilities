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
import kotlin.Double
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
      val hasShojo = WeaponSkillService.hasWeaponInInventory(player, "harukanaru_shojo_no_ketsui")
      if (hasShojo) {
                          if (WeaponSkillService.hasMark(player, "nageki")) {
                              WeaponSkillService.removeMark(player, "nageki");
                              val access = player as? WeaponSkillAccess;
                              if (access != null && access.cresoraGetShieldWeaponId() == definition.id) {
                                  WeaponSkillService.clearShield(player);
                              }
                          }
                          
                          val resolveState = ThoughtsoftheDistantBoySkill.shojoResolveStates.getOrPut(player.uuid) { ThoughtsoftheDistantBoySkill.ShojoResolveState(0L, 0) };
                          resolveState.expireTick = now + 100 * 20L;
                          resolveState.stacks = 1;
                      } else {
                          WeaponSkillService.applyMark(player, "nageki", 10L);
                          ThoughtsoftheDistantBoySkill.shojoResolveStates.remove(player.uuid);

                          if (now % 200L == 0L) {
                              val access = player as? WeaponSkillAccess;
                              if (access != null) {
                                  val currentShield = access.cresoraGetShieldHp();
                                  if (currentShield < 6.0f) {
                                      if (player.random.nextDouble() < 0.5) {
                                          WeaponSkillService.grantShield(player, 6.0f, 99999999L, definition.id);
                                          player.sendMessage(
                                              Text.translatable("item.cresora.weapon.skill.harukanaru_shonen_no_omoi.grief_shield_gained")
                                                  .formatted(Formatting.BLUE),
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
      val isGriefActive = WeaponSkillService.hasMark(player, "nageki")
      if (isGriefActive) {
                          // Pattern A
                          WeaponSkillService.startCooldown(player, definition.id, 200L);
                          WeaponSkillService.showCooldownBar(player, definition);
                          
                          val shieldHearts = 10.0 + (data.baseLevel - 1) * 0.5;
                          WeaponSkillService.grantShield(player, (shieldHearts * 2.0).toFloat(), 99999999L, definition.id);
                          
                          player.sendMessage(
                              Text.translatable(
                                  "item.cresora.weapon.skill.harukanaru_shonen_no_omoi.pattern_a",
                                  WeaponSkillService.formatNumber(shieldHearts)
                              ).formatted(Formatting.AQUA),
                              true
                          );
                      } else {
                          // Pattern B
                          WeaponSkillService.startCooldown(player, definition.id, 700L);
                          WeaponSkillService.showCooldownBar(player, definition);
                          
                          val world = player.world as? ServerWorld ?: return@execute;
                          val range = definition.skill.radiusMeters;
                          val targets = world.getOtherEntities(player, player.boundingBox.expand(range)) {
                              it is LivingEntity &&
                              it.isAlive &&
                              it !is PlayerEntity
                          }
                          .filterIsInstance<LivingEntity>()
                          .sortedBy { it.squaredDistanceTo(player) }
                          .take(5);

                          val playerAtk = player.getAttributeValue(EntityAttributes.ATTACK_DAMAGE);
                          val damageAmount = (playerAtk * (3.0 + data.skillLevel * 0.5)).toFloat();

                          targets.forEach { target ->
                              target.damage(
                                  world,
                                  world.damageSources.indirectMagic(player, player),
                                  damageAmount
                              );
                              world.spawnParticles(
                                  ParticleTypes.CRIT,
                                  target.x, target.y + 1.0, target.z,
                                  5, 0.3, 0.3, 0.3, 0.1
                              );
                          }

                          player.sendMessage(
                              Text.translatable(
                                  "item.cresora.weapon.skill.harukanaru_shonen_no_omoi.pattern_b",
                                  targets.size,
                                  WeaponSkillService.formatNumber(damageAmount.toDouble())
                              ).formatted(Formatting.LIGHT_PURPLE),
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
