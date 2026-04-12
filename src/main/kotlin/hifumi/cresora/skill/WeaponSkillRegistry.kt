package hifumi.cresora.skill

import hifumi.cresora.skill.generated.CompiledWeaponSkillRegistry

object WeaponSkillRegistry {
    private val handlers: MutableMap<String, WeaponSkillHandler> = mutableMapOf()

    init {
        register("current_hp_true_damage", CurrentHpBurstSkill)
        register("snow_frost", SnowFrostSkill)
        register("healing_aura", HealingAuraSkill)
        register("dark_lux", DarkLuxSkill)
        register("orchid_pavilion_echo", OrchidPavilionEchoSkill)
        
        // Compiled skills
        CompiledWeaponSkillRegistry.registerAll(this)
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
