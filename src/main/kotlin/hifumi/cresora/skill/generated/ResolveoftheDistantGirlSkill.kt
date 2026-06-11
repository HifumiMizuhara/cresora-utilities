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
import kotlin.collections.MutableList
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
          player.sendMessage(Text.translatable("item.cresora.weapon.skill.buff.ketsui.decreased",
              Text.translatable("item.cresora.weapon.skill.buff.ketsui.name"),
              WeaponSkillService.getDisplayStacks(player, "ketsui", state.stacks)), true)
        }
      }
    }
    ; 

    ; run execute@ {
      val now = WeaponSkillService.currentWorldTime(player)
      if (now % 20L == 0L) {
                          if (!WeaponSkillService.hasMark(player, "ketsui_active")) return@execute;
                          val world = player.world as? ServerWorld ?: return@execute;
                          val markId = "kyundeath_" + player.uuid;
                          world.getOtherEntities(
                              player,
                              player.boundingBox.expand(16.0)
                          ) { entity ->
                              entity is LivingEntity &&
                              entity.isAlive &&
                              WeaponSkillService.hasMark(entity, markId)
                          }
                          .forEach { entity ->
                              val target = entity as LivingEntity;
                              val hits = player.random.nextBetween(1, 3);
                              for (i in 0 until hits) {
                                  target.damage(
                                      world,
                                      world.damageSources.indirectMagic(player, player),
                                      1.0f
                                  );
                              }
                              world.spawnParticles(
                                  ParticleTypes.HEART,
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
      WeaponSkillService.applyMark(player, "ketsui_active", 100L)
      val world = player.world as? ServerWorld ?: return@execute
      val range = 8.0
      val targets = world.getOtherEntities(player, player.boundingBox.expand(range)) {
                          it is LivingEntity &&
                          it.isAlive &&
                          it !is PlayerEntity
                      }
                      .filterIsInstance<LivingEntity>()
                      .sortedBy { it.squaredDistanceTo(player) }
                      .take(4)
      targets.forEach { target ->
                          WeaponSkillService.applyMark(target, "kyundeath", 100L);
                          WeaponSkillService.applyMark(target, "kyundeath_" + player.uuid, 100L);
                          world.spawnParticles(
                              ParticleTypes.HEART,
                              target.x, target.y + 1.0, target.z,
                              5, 0.3, 0.3, 0.3, 0.0
                          );
                      }
      player.sendMessage(
                          Text.translatable(
                              "item.cresora.weapon.skill.harukanaru_shojo_no_ketsui.activated",
                              targets.size
                          ).formatted(Formatting.LIGHT_PURPLE),
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
      if (CombatFeedbackService.hasPendingCrit(player)) {
                          val now = WeaponSkillService.currentWorldTime(player);
                          val state = ResolveoftheDistantGirlSkill.ketsuiStates.getOrPut(player.uuid) { ResolveoftheDistantGirlSkill.KetsuiState() };
                          if (state.expireTicks.size < 100) {
                              state.expireTicks.add(now + 40 * 20L);
                          }
                          val displayStacks = WeaponSkillService.getDisplayStacks(player, "ketsui", state.stacks);
                          player.sendMessage(
                              Text.translatable(
                                  "item.cresora.weapon.skill.buff.ketsui.gained",
                                  Text.translatable("item.cresora.weapon.skill.buff.ketsui.name"),
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
