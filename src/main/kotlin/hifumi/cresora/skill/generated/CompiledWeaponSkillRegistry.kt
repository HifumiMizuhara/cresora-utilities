package hifumi.cresora.skill.generated

import hifumi.cresora.skill.WeaponSkillRegistry

public object CompiledWeaponSkillRegistry {
  public fun registerAll(registry: WeaponSkillRegistry) {
    registry.register("shield", RondoMelodySkill)
    registry.register("sunlit_haste", PastoralFluteReverieSkill)
    registry.register("baa_mimic", CadenzaAllegroSkill)
    registry.register("heal", MasqueradeInvitationSkill)
    registry.register("flame_aura", RequiemTowardDawnSkill)
  }
}
