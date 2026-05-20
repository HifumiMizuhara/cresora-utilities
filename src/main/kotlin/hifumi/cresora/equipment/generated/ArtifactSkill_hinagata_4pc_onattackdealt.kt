package hifumi.cresora.equipment.generated

import hifumi.cresora.equipment.ArtifactSkillHandler
import kotlin.Double
import net.minecraft.entity.LivingEntity
import net.minecraft.server.network.ServerPlayerEntity

public class ArtifactSkill_hinagata_4pc_onattackdealt : ArtifactSkillHandler {
  override fun onAttackDealt(
    player: ServerPlayerEntity,
    target: LivingEntity,
    damage: Double,
  ) {
    hifumi.cresora.WeaponSkillService.applyMark(target, "hinagata_curse", 100)
    player.sendMessage(net.minecraft.text.Text.literal("Applied Hinagata curse!"))
  }
}
