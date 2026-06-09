package hifumi.cresora.equipment.generated

import hifumi.cresora.debuff.CresoraDebuffService
import hifumi.cresora.equipment.ArtifactSkillHandler
import hifumi.cresora.equipment.EquipmentEffectHookService
import hifumi.cresora.weapon.WeaponSkillService
import kotlin.Boolean
import kotlin.Double
import kotlin.Int
import kotlin.String
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

public class ArtifactSkillOsananajimi4pcPassive : ArtifactSkillHandler {
  private fun isActive(player: ServerPlayerEntity): Boolean =
      WeaponSkillService.hasWeaponInInventory(player, "harukanaru_shojo_no_ketsui")

  override fun getCritDamageBonus(player: ServerPlayerEntity): Double {
    if (!isActive(player)) return 0.0
    return 10.0
  }

  override fun getAllDamageBonus(player: ServerPlayerEntity): Double {
    if (!isActive(player)) return 0.0
    return 10.0
  }

  override fun getDisplayStackBonus(player: ServerPlayerEntity, buffId: String): Int {
    if (!isActive(player)) return 0
    return when (buffId) {
      "ketsui" -> 5
      else -> 0
    }
  }
}
