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
import kotlin.Double
import kotlin.Float
import kotlin.Int
import kotlin.Long
import kotlin.collections.MutableList
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

public object ResolveoftheDistantGirlSkill : WeaponSkillHandler {
  public val ketsuiStates: ConcurrentHashMap<UUID, ResolveoftheDistantGirlSkill.KetsuiState> =
      ConcurrentHashMap()

  override fun clearTransientState(playerId: UUID) {
    ketsuiStates.remove(playerId)
  }

  override fun pruneTransientState(activePlayerIds: Set<UUID>) {
    ketsuiStates.keys.removeIf { !activePlayerIds.contains(it) }
  }

  override fun getCritDamageBonus(player: ServerPlayerEntity): Double {
    var total = 0.0
    total += ketsuiStates[player.uuid]?.let { it.stacks * 2.0 } ?: 0.0
    return total
  }

  override fun getAllDamageBonus(player: ServerPlayerEntity): Double {
    var total = 0.0
    total += ketsuiStates[player.uuid]?.let { it.stacks * 2.0 } ?: 0.0
    return total
  }

  override fun onPlayerTick(
    player: ServerPlayerEntity,
    definition: WeaponDefinition,
    `data`: WeaponData,
  ) {
    val now = WeaponSkillService.currentWorldTime(player)

    ; run {
      val state = ketsuiStates[player.uuid]
      if (state != null) {
        val removed = state.expireTicks.removeIf { now >= it }
        if (removed && state.expireTicks.isEmpty()) {
          ketsuiStates.remove(player.uuid)
          player.sendMessage(Text.translatable("item.cresora.weapon.skill.buff.ketsui.expired",
              Text.translatable("item.cresora.weapon.skill.buff.ketsui.name")), true)
        } else if (removed) {
          player.sendMessage(Text.translatable("item.cresora.weapon.skill.buff.ketsui.gained",
              Text.translatable("item.cresora.weapon.skill.buff.ketsui.name"),
              WeaponSkillService.getDisplayStacks(player, "ketsui", state.stacks)), true)
        }
      }
    }
    ; 

    ; run execute@ {
      val now = hifumi.cresora.weapon.WeaponSkillService.currentWorldTime(player)
      if (now % 20L == 0L) {
                          if (!hifumi.cresora.weapon.WeaponSkillService.hasMark(player, "ketsui_active")) return@execute;
                          val world = player.world as? net.minecraft.server.world.ServerWorld ?: return@execute;
                          val markId = "kyundeath_" + player.uuid;
                          world.getOtherEntities(
                              player,
                              player.boundingBox.expand(16.0)
                          ) { entity ->
                              entity is net.minecraft.entity.LivingEntity &&
                              entity.isAlive &&
                              hifumi.cresora.weapon.WeaponSkillService.hasMark(entity, markId)
                          }
                          .forEach { entity ->
                              val target = entity as net.minecraft.entity.LivingEntity;
                              val hits = player.random.nextBetween(1, 3);
                              for (i in 0 until hits) {
                                  target.damage(
                                      world,
                                      world.damageSources.indirectMagic(player, player),
                                      1.0f
                                  );
                              }
                              world.spawnParticles(
                                  net.minecraft.particle.ParticleTypes.HEART,
                                  target.x, target.y + 1.0, target.z,
                                  3, 0.2, 0.2, 0.2, 0.0
                              );
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


    ; run execute@ {
      hifumi.cresora.weapon.WeaponSkillService.applyMark(player, "ketsui_active", 100L)
      val world = player.world as? net.minecraft.server.world.ServerWorld ?: return@execute
      val range = 8.0
      val targets = world.getOtherEntities(player, player.boundingBox.expand(range)) {
                          it is net.minecraft.entity.LivingEntity &&
                          it.isAlive &&
                          it !is net.minecraft.entity.player.PlayerEntity
                      }
                      .filterIsInstance<net.minecraft.entity.LivingEntity>()
                      .sortedBy { it.squaredDistanceTo(player) }
                      .take(4)
      targets.forEach { target ->
                          hifumi.cresora.weapon.WeaponSkillService.applyMark(target, "kyundeath", 100L);
                          hifumi.cresora.weapon.WeaponSkillService.applyMark(target, "kyundeath_" + player.uuid, 100L);
                          world.spawnParticles(
                              net.minecraft.particle.ParticleTypes.HEART,
                              target.x, target.y + 1.0, target.z,
                              5, 0.3, 0.3, 0.3, 0.0
                          );
                      }
      player.sendMessage(
                          net.minecraft.text.Text.translatable(
                              "item.cresora.weapon.skill.harukanaru_shojo_no_ketsui.activated",
                              targets.size
                          ).formatted(net.minecraft.util.Formatting.LIGHT_PURPLE),
                          true
                      )
    }


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
      if (hifumi.cresora.combat.CombatFeedbackService.hasPendingCrit(player)) {
                          val now = hifumi.cresora.weapon.WeaponSkillService.currentWorldTime(player);
                          val state = ResolveoftheDistantGirlSkill.ketsuiStates.getOrPut(player.uuid) { ResolveoftheDistantGirlSkill.KetsuiState() };
                          if (state.expireTicks.size < 100) {
                              state.expireTicks.add(now + 40 * 20L);
                          }
                          val displayStacks = hifumi.cresora.weapon.WeaponSkillService.getDisplayStacks(player, "ketsui", state.stacks);
                          player.sendMessage(
                              net.minecraft.text.Text.translatable(
                                  "item.cresora.weapon.skill.buff.ketsui.gained",
                                  net.minecraft.text.Text.translatable("item.cresora.weapon.skill.buff.ketsui.name"),
                                  displayStacks
                              ),
                              true
                          );
                      }
    }

  }

  public data class KetsuiState(
    public val expireTicks: MutableList<Long> = mutableListOf(),
  ) {
    public val stacks: Int
      get() = expireTicks.size
  }
}
