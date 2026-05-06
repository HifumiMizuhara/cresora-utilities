package hifumi.cresora.skill.generated

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
import net.minecraft.entity.LivingEntity
import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import net.minecraft.util.ActionResult
import net.minecraft.util.Formatting

public object ColdMistCoilingSnowSkill : WeaponSkillHandler {
  public val snowMistStates: MutableMap<UUID, ColdMistCoilingSnowSkill.SnowMistState> =
      mutableMapOf()

  override fun getCritDamageBonus(player: ServerPlayerEntity): Double {
    var total = 0.0
    total += snowMistStates[player.uuid]?.let { it.stacks * 10.0 } ?: 0.0
    return total
  }

  override fun onPlayerTick(
    player: ServerPlayerEntity,
    definition: WeaponDefinition,
    `data`: WeaponData,
  ) {
    val now = WeaponSkillService.currentWorldTime(player)

    ; if (snowMistStates.containsKey(player.uuid) && now >=
        snowMistStates[player.uuid]!!.expireTick) {
      snowMistStates.remove(player.uuid)
      player.sendMessage(Text.translatable("item.cresora.weapon.skill.buff.snow_mist.expired",
          Text.translatable("item.cresora.weapon.skill.buff.snow_mist.name")), true)
    }
    ; 
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
        val state = ColdMistCoilingSnowSkill.snowMistStates.getOrPut(player.uuid) {
        ColdMistCoilingSnowSkill.SnowMistState(0L, 0) }
        state.expireTick = now + 10 * 20L
        state.stacks = (state.stacks + 1).coerceAtMost(5)
        player.sendMessage(Text.translatable("item.cresora.weapon.skill.buff.snow_mist.gained",
        Text.translatable("item.cresora.weapon.skill.buff.snow_mist.name"), state.stacks), true)
    }
    player.sendMessage(Text.translatable("item.cresora.weapon.skill.snow_frost_activated.generic").formatted(Formatting.AQUA),
        true)


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
      // If the player has snow_mist buff, we increment it and apply frost mark to target
                      val now = hifumi.cresora.WeaponSkillService.currentWorldTime(player);
                      val state = ColdMistCoilingSnowSkill.snowMistStates.get(player.uuid);
                      if (state != null && now < state.expireTick) {
                          if (state.stacks < 5) {
                              state.stacks += 1;
                             
          player.sendMessage(net.minecraft.text.Text.translatable("item.cresora.weapon.skill.snow_mist_stack",
          state.stacks, state.stacks * 10.0).formatted(net.minecraft.util.Formatting.AQUA), true);
                          }
                          // Apply frost mark to target (using the new generic mark system)
                          hifumi.cresora.WeaponSkillService.applyMark(target, "frost", 200L);
                         
          target.addStatusEffect(net.minecraft.entity.effect.StatusEffectInstance(net.minecraft.entity.effect.StatusEffects.SLOWNESS,
          200, 1, false, true, true));
                      }
    }

  }

  override fun onTick(
    server: MinecraftServer,
    definition: WeaponDefinition,
    `data`: WeaponData,
  ) {

    ; run execute@ {
      server.worlds.forEach { world ->
                          world.iterateEntities().forEach { entity ->
                              if (entity is net.minecraft.entity.LivingEntity &&
          hifumi.cresora.WeaponSkillService.hasMark(entity, "frost")) {
                                  val now = world.time;
                                  // Simple frost damage slowness and freeze
                                 
          entity.addStatusEffect(net.minecraft.entity.effect.StatusEffectInstance(net.minecraft.entity.effect.StatusEffects.SLOWNESS,
          40, 1, false, true, true));
                                  if (now % 20L == 0L) {
                                       entity.damage(world, world.damageSources.freeze(), 2.0f);
                                  }
                              }
                          }
                      }
    }

  }

  public data class SnowMistState(
    public var expireTick: Long,
    public var stacks: Int,
  )
}
