package hifumi.cresora.equipment.generated

import hifumi.cresora.equipment.ArtifactSkillHandler
import hifumi.cresora.weapon.WeaponSkillService
import net.minecraft.server.network.ServerPlayerEntity

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
