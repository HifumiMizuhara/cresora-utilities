package hifumi.cresora.skill

import hifumi.cresora.WeaponDefinition
import hifumi.cresora.WeaponContentRegistry
import hifumi.cresora.skill.generated.CompiledWeaponSkillRegistry

object WeaponSkillRegistry {
    private val handlers: MutableMap<String, WeaponSkillHandler> = mutableMapOf()
    private val subSkillsByParent: MutableMap<String, MutableList<String>> = mutableMapOf()

    init {
        // All skills are now registered through the compiler-generated registry
        CompiledWeaponSkillRegistry.registerAll(this)
    }

    fun register(effectId: String, handler: WeaponSkillHandler) {
        handlers[effectId] = handler
    }

    fun registerSubSkill(parentEffectId: String, subSkillEffectId: String) {
        subSkillsByParent.getOrPut(parentEffectId) { mutableListOf() } += subSkillEffectId
    }

    fun getHandler(effectId: String): WeaponSkillHandler? {
        return handlers[effectId]
    }

    fun getDefinition(effectId: String): WeaponDefinition? {
        return WeaponContentRegistry.weaponDefinitions().firstOrNull { it.skill.effectId == effectId }
    }

    fun allHandlers(): Map<String, WeaponSkillHandler> {
        return handlers
    }

    fun subSkillEffectIds(parentEffectId: String): List<String> {
        return subSkillsByParent[parentEffectId].orEmpty()
    }
}
