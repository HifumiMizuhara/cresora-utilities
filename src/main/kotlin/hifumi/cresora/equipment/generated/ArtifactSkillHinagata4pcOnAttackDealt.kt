package hifumi.cresora.equipment.generated

import hifumi.cresora.debuff.CresoraDebuffService
import hifumi.cresora.equipment.ArtifactSkillHandler
import hifumi.cresora.equipment.EquipmentEffectHookService
import hifumi.cresora.weapon.WeaponSkillService
import java.util.UUID
import kotlin.Double
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

public class ArtifactSkillHinagata4pcOnAttackDealt : ArtifactSkillHandler {
  public val hinagataStacksStates:
      MutableMap<UUID, ArtifactSkillHinagata4pcOnAttackDealt.HinagataStacksState> = mutableMapOf()

  override fun clearTransientState(playerId: UUID) {
    hinagataStacksStates.remove(playerId)
  }

  override fun pruneTransientState(activePlayerIds: Set<UUID>) {
    hinagataStacksStates.keys.removeIf { !activePlayerIds.contains(it) }
  }

  override fun getAttackDamageScalar(player: ServerPlayerEntity): Double {
    var total = 0.0
    total += hinagataStacksStates[player.uuid]?.let { it.stacks * 2.0 } ?: 0.0
    return total
  }

  override fun onTick(player: ServerPlayerEntity) {
    val now = WeaponSkillService.currentWorldTime(player)
    if (hinagataStacksStates.containsKey(player.uuid) && now >=
        hinagataStacksStates[player.uuid]!!.expireTick) {
      hinagataStacksStates.remove(player.uuid)
      player.sendMessage(Text.translatable("item.cresora.artifact.skill.buff.hinagata_stacks.expired",
          Text.translatable("item.cresora.artifact.skill.buff.hinagata_stacks.name")), true)
    }
  }

  override fun onAttackDealt(
    player: ServerPlayerEntity,
    target: LivingEntity,
    damage: Double,
  ) {
    hifumi.cresora.weapon.WeaponSkillService.applyMark(target, "hinagata_curse", 100)
    run {
        val now = WeaponSkillService.currentWorldTime(player)
        val state = this.hinagataStacksStates.getOrPut(player.uuid) { HinagataStacksState(0L, 0) }
        state.expireTick = now + 5 * 20L
        state.stacks = (state.stacks + 1).coerceAtMost(10)
       
        player.sendMessage(Text.translatable("item.cresora.artifact.skill.buff.hinagata_stacks.gained",
        Text.translatable("item.cresora.artifact.skill.buff.hinagata_stacks.name"),
        EquipmentEffectHookService.getDisplayStacks(player, "hinagata_stacks", state.stacks)), true)
    }
    player.sendMessage(Text.translatable("message.cresora.hinagata.curse").formatted(Formatting.GOLD),
        true)
  }

  public data class HinagataStacksState(
    public var expireTick: Long,
    public var stacks: Int,
  )
}
