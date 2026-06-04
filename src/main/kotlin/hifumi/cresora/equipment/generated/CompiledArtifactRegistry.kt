package hifumi.cresora.equipment.generated

import hifumi.cresora.equipment.ArtifactSkillRegistry
import kotlin.jvm.JvmStatic

public object CompiledArtifactRegistry {
  @JvmStatic
  public fun registerAll(registry: ArtifactSkillRegistry) {
    registry.register("hinagata_4pc_on_attack_dealt", ArtifactSkillHinagata4pcOnAttackDealt())
    registry.register("osananajimi_4pc_passive", ArtifactSkillOsananajimi4pcPassive())
  }
}
