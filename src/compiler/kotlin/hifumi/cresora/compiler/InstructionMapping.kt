package hifumi.cresora.compiler

enum class CompilerContext {
    WEAPON,
    ARTIFACT
}

object InstructionMapping {
    private val mappings = mapOf(
        "heal" to "hifumi.cresora.weapon.WeaponCombatSupport.healHp(player, %args%)",
        "grant_shield" to "hifumi.cresora.weapon.WeaponCombatSupport.grantShield(player, %args%)",
        "start_cooldown" to "hifumi.cresora.weapon.WeaponSkillService.startCooldown(player, definition.id, %args%)",
        "apply_mark" to "hifumi.cresora.weapon.WeaponSkillService.applyMark(%args%)",
        "log" to "player.sendMessage(net.minecraft.text.Text.literal(%args%))",
        "grant_invulnerability" to "hifumi.cresora.weapon.WeaponSkillService.grantInvulnerability(%args%)",
        "send_message" to "player.sendMessage(net.minecraft.text.Text.translatable(%args%), true)",
        "apply_status_effect" to "hifumi.cresora.weapon.WeaponCombatSupport.applyStatusEffect(player, %args%)",
        "close_skill_menu" to "hifumi.cresora.weapon.HotbarOverrideService.restoreHotbar(player)",
        "spawn_particles" to "hifumi.cresora.weapon.WeaponCombatSupport.spawnParticles(player, %args%)",
        "deal_true_damage" to "hifumi.cresora.weapon.WeaponSkillService.dealTrueDamage(player, target, %args%.toFloat())",
        "ignite" to "target.setOnFireFor(%args%.toFloat())"
    )

    private val intrinsics = setOf("add_buff")

    fun isKnown(functionName: String): Boolean = mappings.containsKey(functionName) || intrinsics.contains(functionName)

    fun expand(functionName: String, args: List<String>, context: CompilerContext = CompilerContext.WEAPON): String {
        if (context == CompilerContext.ARTIFACT && functionName == "start_cooldown") {
            throw RuntimeException("start_cooldown is not supported in artifacts")
        }

        val pattern = mappings[functionName] ?: return "$functionName(${args.joinToString(", ")})"
        
        // Handle special argument conversions if needed
        val processedArgs = if (args.isEmpty()) {
            when (functionName) {
                "start_cooldown" -> {
                    listOf("definition.skill.cooldownSeconds * 20L")
                }
                "heal" -> {
                    if (context == CompilerContext.ARTIFACT) throw RuntimeException("heal requires an explicit value in artifacts")
                    listOf("definition.skill.baseValue")
                }
                "grant_shield" -> {
                    if (context == CompilerContext.ARTIFACT) throw RuntimeException("grant_shield requires explicit values in artifacts")
                    listOf("definition.skill.baseValue", "definition.skill.durationSeconds * 20L")
                }
                else -> emptyList()
            }
        } else {
            args.map { arg ->
                when (arg) {
                    "skill_value" -> {
                        if (context == CompilerContext.ARTIFACT) throw RuntimeException("skill_value is not supported in artifacts")
                        "definition.skill.baseValue"
                    }
                    "skill_duration" -> {
                        if (context == CompilerContext.ARTIFACT) throw RuntimeException("skill_duration is not supported in artifacts")
                        "definition.skill.durationSeconds"
                    }
                    else -> arg.replace(Regex("(\\d+)s$"), "$1") // Remove 's' from time literals like '5s' but not 'status'
                }
            }
        }
        
        val expanded = pattern.replace("%args%", processedArgs.joinToString(", "))
        
        return expanded
    }

}

