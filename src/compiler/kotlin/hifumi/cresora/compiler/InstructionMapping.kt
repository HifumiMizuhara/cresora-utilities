package hifumi.cresora.compiler

object InstructionMapping {
    private val mappings = mapOf(
        "heal" to "hifumi.cresora.WeaponCombatSupport.healHp(player, %args%)",
        "grant_shield" to "hifumi.cresora.WeaponCombatSupport.grantShield(player, %args%)",
        "start_cooldown" to "hifumi.cresora.WeaponSkillService.startCooldown(player, definition.id, %args%)",
        "apply_mark" to "hifumi.cresora.WeaponSkillService.applyMark(%args%)",
        "log" to "player.sendMessage(net.minecraft.text.Text.literal(%args%))",
        "grant_invulnerability" to "hifumi.cresora.WeaponSkillService.grantInvulnerability(%args%)",
        "send_message" to "player.sendMessage(net.minecraft.text.Text.translatable(%args%), true)",
        "apply_status_effect" to "hifumi.cresora.WeaponCombatSupport.applyStatusEffect(player, %args%)",
        "close_skill_menu" to "hifumi.cresora.HotbarOverrideService.restoreHotbar(player)",
        "spawn_particles" to "hifumi.cresora.WeaponCombatSupport.spawnParticles(player, %args%)"
    )

    fun isKnown(functionName: String): Boolean = mappings.containsKey(functionName)

    fun expand(functionName: String, args: List<String>): String {
        val pattern = mappings[functionName] ?: return "$functionName(${args.joinToString(", ")})"
        
        // Handle special argument conversions if needed
        val processedArgs = args.map { arg ->
            when (arg) {
                "skill_value" -> "definition.skill.baseValue"
                "skill_duration" -> "definition.skill.durationSeconds"
                else -> arg.replace(Regex("(\\d+)s$"), "$1") // Remove 's' from time literals like '5s' but not 'status'
            }
        }
        
        return pattern.replace("%args%", processedArgs.joinToString(", "))
    }
}
