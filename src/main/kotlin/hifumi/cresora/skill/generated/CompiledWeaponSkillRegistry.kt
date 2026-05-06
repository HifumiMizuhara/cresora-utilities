package hifumi.cresora.skill.generated

import hifumi.cresora.skill.WeaponSkillRegistry

public object CompiledWeaponSkillRegistry {
  public fun registerAll(registry: WeaponSkillRegistry) {
    registry.register("qianqiu_yeluo", QianqiuYeluoSkill)
    registry.registerSubSkill("qianqiu_yeluo", "danrai")
    registry.registerSubSkill("qianqiu_yeluo", "zansou")
    registry.registerSubSkill("qianqiu_yeluo", "yoraku_manchisho")
    registry.register("danrai", DanraiSkill)
    registry.register("zansou", ZansouSkill)
    registry.register("yoraku_manchisho", YorakuManchishoSkill)
    registry.register("dark_lux", DarkLuxSkill)
    registry.register("shield", RondoMelodySkill)
    registry.register("orchid_pavilion_echo", KyokusuinoRyushoSkill)
    registry.register("sunlit_haste", PastoralFluteReverieSkill)
    registry.register("baa_mimic", CadenzaAllegroSkill)
    registry.register("heal", MasqueradeInvitationSkill)
    registry.register("healing_aura", HighMountainsFlowingWaterSkill)
    registry.register("rougan_kenpo", TanbokuChokuuSkill)
    registry.registerSubSkill("rougan_kenpo", "danro")
    registry.registerSubSkill("rougan_kenpo", "zanso")
    registry.registerSubSkill("rougan_kenpo", "bokuchu_munen")
    registry.register("danro", DanroSkill)
    registry.register("zanso", ZansoSkill)
    registry.register("bokuchu_munen", BokuchuMunenSkill)
    registry.register("flame_aura", RequiemTowardDawnSkill)
    registry.register("snow_frost", ColdMistCoilingSnowSkill)
    registry.register("current_hp_true_damage", StridebytheLakesideSkill)
  }
}
