package hifumi.cresora.skill

object WeaponSkillRegistry {
    private val handlers: MutableMap<String, WeaponSkillHandler> = mutableMapOf()

    init {
        register("current_hp_true_damage", CurrentHpBurstSkill)
        register("flame_aura", FlameAuraSkill)
        register("snow_frost", SnowFrostSkill)
        register("healing_aura", HealingAuraSkill)
        register("heal", HealSkill)
        register("dark_lux", DarkLuxSkill)
        register("sunlit_haste", SunlitHasteSkill)
        register("baa_mimic", BaaMimicSkill)
        register("orchid_pavilion_echo", OrchidPavilionEchoSkill)
    }

    fun register(effectId: String, handler: WeaponSkillHandler) {
        handlers[effectId] = handler
    }

    fun getHandler(effectId: String): WeaponSkillHandler? {
        return handlers[effectId]
    }

    fun allHandlers(): Collection<WeaponSkillHandler> {
        return handlers.values
    }
}
