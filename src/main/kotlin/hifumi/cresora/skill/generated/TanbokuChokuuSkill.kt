package hifumi.cresora.skill.generated

import hifumi.cresora.HotbarOverrideService
import hifumi.cresora.WeaponData
import hifumi.cresora.WeaponDefinition
import hifumi.cresora.WeaponSkillAccess
import hifumi.cresora.WeaponSkillService
import hifumi.cresora.skill.WeaponSkillHandler
import java.util.UUID
import kotlin.Boolean
import kotlin.Float
import kotlin.Int
import kotlin.Long
import kotlin.collections.MutableMap
import net.minecraft.entity.LivingEntity
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import net.minecraft.util.ActionResult

public object TanbokuChokuuSkill : WeaponSkillHandler {
  public val zansoActiveStates: MutableMap<UUID, TanbokuChokuuSkill.ZansoActiveState> =
      mutableMapOf()

  public val munenSeqStates: MutableMap<UUID, TanbokuChokuuSkill.MunenSeqState> = mutableMapOf()

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
      hifumi.cresora.WeaponSkillService.addTao(player, 1);
                     
          player.sendMessage(net.minecraft.text.Text.translatable("item.cresora.weapon.tanboku_chokuu.tao_gained",
          hifumi.cresora.WeaponSkillService.getTao(player)).formatted(net.minecraft.util.Formatting.GOLD),
          true);
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
      val stacks = hifumi.cresora.WeaponSkillService.getSoulBreakStacks(target);
                      if (stacks > 0 && !isTrueDamage) {
                          val boost = amount * (stacks * 0.04f);
                          target.damage(player.world, player.world.damageSources.magic(), boost);
                          if (player.world.time % 20L == 0L) {
                              
          player.sendMessage(net.minecraft.text.Text.translatable("item.cresora.weapon.tanboku_chokuu.soul_break",
          stacks, stacks * 5, stacks * 4).formatted(net.minecraft.util.Formatting.GRAY), true);
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
