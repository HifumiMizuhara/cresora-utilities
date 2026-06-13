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
            args.mapIndexed { index, arg ->
                when {
                    isDurationArgument(functionName, index) -> durationToTicks(functionName, arg)
                    arg == "skill_value" -> {
                        if (context == CompilerContext.ARTIFACT) throw RuntimeException("skill_value is not supported in artifacts")
                        "definition.skill.baseValue"
                    }
                    arg == "skill_duration" -> {
                        if (context == CompilerContext.ARTIFACT) throw RuntimeException("skill_duration is not supported in artifacts")
                        "definition.skill.durationSeconds"
                    }
                    else -> arg
                }
            }
        }
        
        val expanded = pattern.replace("%args%", processedArgs.joinToString(", "))
        
        return expanded
    }

    private fun isDurationArgument(functionName: String, index: Int): Boolean {
        return when (functionName) {
            "grant_shield" -> index == 1
            "start_cooldown" -> index == 0
            "apply_mark" -> index == 2
            "grant_invulnerability" -> index == 1
            "apply_status_effect" -> index == 1
            else -> false
        }
    }

    private fun durationToTicks(functionName: String, arg: String): String {
        if (arg == "skill_duration") {
            return if (functionName == "apply_status_effect") {
                "definition.skill.durationSeconds * 20"
            } else {
                "definition.skill.durationSeconds * 20L"
            }
        }
        if (arg.endsWith("s")) {
            val seconds = arg.dropLast(1).toDoubleOrNull()
                ?: throw RuntimeException("$functionName duration must be a number of seconds, got '$arg'")
            val ticks = seconds * 20.0
            return if (functionName == "apply_status_effect") {
                if (ticks % 1.0 == 0.0) ticks.toInt().toString() else "($seconds * 20).toInt()"
            } else {
                if (ticks % 1.0 == 0.0) "${ticks.toLong()}L" else "($seconds * 20).toLong()"
            }
        }
        return when (arg) {
            "skill_value" -> throw RuntimeException("skill_value is not a valid duration for $functionName")
            else -> arg
        }
    }

    fun expandAll(content: String, context: CompilerContext): String {
        val sb = java.lang.StringBuilder()
        var i = 0
        val len = content.length
        while (i < len) {
            if (content[i].isLetter() || content[i] == '_') {
                val start = i
                while (i < len && (content[i].isLetterOrDigit() || content[i] == '_')) {
                    i++
                }
                val id = content.substring(start, i)
                var isDotPrefixed = false
                var p = start - 1
                while (p >= 0 && content[p].isWhitespace()) {
                    p--
                }
                if (p >= 0 && content[p] == '.') {
                    isDotPrefixed = true
                }
                var temp = i
                while (temp < len && content[temp].isWhitespace()) {
                    temp++
                }
                if (temp < len && content[temp] == '(' && isKnown(id) && !isDotPrefixed) {
                    i = temp + 1
                    var depth = 1
                    val argStart = i
                    while (i < len && depth > 0) {
                        val c = content[i]
                        if (c == '(') depth++
                        else if (c == ')') depth--
                        i++
                    }
                    val argEnd = i - 1
                    val argsStr = content.substring(argStart, argEnd)
                    val args = if (argsStr.trim().isEmpty()) {
                        emptyList()
                    } else {
                        splitArguments(argsStr)
                    }
                    val expandedArgs = args.map { expandAll(it, context) }
                    val expandedInstruction = expand(id, expandedArgs, context)
                    sb.append(expandedInstruction)
                } else {
                    sb.append(id)
                }
            } else {
                sb.append(content[i])
                i++
            }
        }
        return sb.toString()
    }

    private fun splitArguments(argsStr: String): List<String> {
        val result = mutableListOf<String>()
        val current = java.lang.StringBuilder()
        var depth = 0
        var inString = false
        var escape = false
        for (c in argsStr) {
            if (escape) {
                current.append(c)
                escape = false
                continue
            }
            if (c == '\\') {
                current.append(c)
                escape = true
                continue
            }
            if (c == '"') {
                inString = !inString
                current.append(c)
                continue
            }
            if (inString) {
                current.append(c)
                continue
            }
            if (c == '(' || c == '[' || c == '{') {
                depth++
                current.append(c)
            } else if (c == ')' || c == ']' || c == '}') {
                depth--
                current.append(c)
            } else if (c == ',' && depth == 0) {
                result.add(current.toString().trim())
                current.setLength(0)
            } else {
                current.append(c)
            }
        }
        if (current.isNotEmpty() || result.isNotEmpty()) {
            result.add(current.toString().trim())
        }
        return result
    }

}
