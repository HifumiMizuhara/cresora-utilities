package hifumi.cresora.compiler

class Parser(private val source: String, private val tokens: List<Token>) {
    private var current = 0

    fun parse(): List<ASTNode> {
        val nodes = mutableListOf<ASTNode>()
        while (!isAtEnd()) {
            if (check(TokenType.KEYWORD_WEAPON)) {
                nodes.add(weapon())
            } else if (check(TokenType.KEYWORD_DICTIONARY)) {
                nodes.add(dictionary())
            } else {
                advance() // Skip unknown top-level tokens
            }
        }
        return nodes
    }

    private fun weapon(): WeaponDefNode {
        consume(TokenType.KEYWORD_WEAPON, "Expect 'weapon'")
        val name = consume(TokenType.STRING, "Expect weapon name").lexeme
        consume(TokenType.LEFT_BRACE, "Expect '{' before weapon body")

        var id = ""
        var rarity = ""
        var baseItem = ""
        var damageType = "physical"
        var stats: StatsNode? = null
        var skill: SkillNode? = null
        var translations = mutableMapOf<String, Map<String, String>>()
        val subSkills = mutableListOf<SubSkillNode>()
        var customModelData: Int? = null
        var texture: String? = null

        while (!check(TokenType.RIGHT_BRACE) && !isAtEnd()) {
            val token = advance()
            when (token.lexeme) {
                "id" -> {
                    consume(TokenType.COLON, "Expect ':' after id")
                    id = consume(TokenType.STRING, "Expect id").lexeme
                }
                "rarity" -> {
                    consume(TokenType.COLON, "Expect ':' after rarity")
                    val token = advance()
                    rarity = if (token.type == TokenType.STRING) token.lexeme else token.lexeme
                }
                "base_item" -> {
                    consume(TokenType.COLON, "Expect ':' after base_item")
                    baseItem = consume(TokenType.STRING, "Expect base_item").lexeme
                }
                "damage_type" -> {
                    consume(TokenType.COLON, "Expect ':' after damage_type")
                    damageType = consume(TokenType.IDENTIFIER, "Expect damage_type").lexeme
                }
                "stats" -> {
                    consume(TokenType.LEFT_BRACE, "Expect '{' for stats")
                    stats = stats()
                    consume(TokenType.RIGHT_BRACE, "Expect '}' after stats")
                }
                "skill" -> {
                    val skillName = consume(TokenType.STRING, "Expect skill name").lexeme
                    consume(TokenType.LEFT_BRACE, "Expect '{' for skill")
                    skill = skill(skillName)
                    consume(TokenType.RIGHT_BRACE, "Expect '}' after skill")
                }
                "sub_skill" -> {
                    val subSkillName = consume(TokenType.STRING, "Expect sub_skill name").lexeme
                    consume(TokenType.LEFT_BRACE, "Expect '{' for sub_skill")
                    subSkills.add(subSkill(subSkillName))
                    consume(TokenType.RIGHT_BRACE, "Expect '}' after sub_skill")
                }
                "translations" -> {
                    consume(TokenType.LEFT_BRACE, "Expect '{' for translations")
                    translations = translations()
                    consume(TokenType.RIGHT_BRACE, "Expect '}' after translations")
                }
                "custom_model_data" -> {
                    consume(TokenType.COLON, "Expect ':' after custom_model_data")
                    customModelData = consume(TokenType.NUMBER, "Expect number").lexeme.toInt()
                }
                "texture" -> {
                    consume(TokenType.COLON, "Expect ':' after texture")
                    texture = if (check(TokenType.STRING)) advance().lexeme else consume(TokenType.IDENTIFIER, "Expect string or identifier").lexeme
                }
                else -> throw RuntimeException("Unknown weapon field '${token.lexeme}' at line ${token.line}")
            }
        }

        consume(TokenType.RIGHT_BRACE, "Expect '}' after weapon body")
        val resolvedStats = stats ?: throw RuntimeException("Weapon '$name' is missing required stats block")
        if (id.isBlank()) throw RuntimeException("Weapon '$name' is missing required id")
        if (rarity.isBlank()) throw RuntimeException("Weapon '$name' is missing required rarity")
        if (baseItem.isBlank()) throw RuntimeException("Weapon '$name' is missing required base_item")
        return WeaponDefNode(name, id, rarity, baseItem, damageType, resolvedStats, skill, translations, subSkills, customModelData, texture)
    }

    private fun dictionary(): DictionaryDefNode {
        consume(TokenType.KEYWORD_DICTIONARY, "Expect 'dictionary'")
        val id = consume(TokenType.IDENTIFIER, "Expect dictionary id").lexeme
        consume(TokenType.LEFT_BRACE, "Expect '{' before dictionary body")

        var translations = mutableMapOf<String, Map<String, String>>()

        while (!check(TokenType.RIGHT_BRACE) && !isAtEnd()) {
            val token = advance()
            when (token.lexeme) {
                "translations" -> {
                    consume(TokenType.LEFT_BRACE, "Expect '{' for translations")
                    translations = translations()
                    consume(TokenType.RIGHT_BRACE, "Expect '}' after translations")
                }
            }
        }

        consume(TokenType.RIGHT_BRACE, "Expect '}' after dictionary body")
        return DictionaryDefNode(id, translations)
    }

    private fun stats(): StatsNode {
        var baseAttackDamage = 0.0
        var attackDamagePerLevel = 0.0
        var totalAttackSpeed = 1.6
        var maxBaseLevel = 60
        var maxSkillLevel = 10
        var critRateBonusPercent = 0.0
        var maxAllDamageBonusPercent = 0.0

        while (!check(TokenType.RIGHT_BRACE) && !isAtEnd()) {
            val field = advance().lexeme
            consume(TokenType.COLON, "Expect ':'")
            when (field) {
                "base_damage" -> baseAttackDamage = consume(TokenType.NUMBER, "Expect number").lexeme.toDouble()
                "damage_per_level" -> attackDamagePerLevel = consume(TokenType.NUMBER, "Expect number").lexeme.toDouble()
                "speed" -> totalAttackSpeed = consume(TokenType.NUMBER, "Expect number").lexeme.toDouble()
                "max_base_level" -> maxBaseLevel = consume(TokenType.NUMBER, "Expect number").lexeme.toInt()
                "max_skill_level" -> maxSkillLevel = consume(TokenType.NUMBER, "Expect number").lexeme.toInt()
                "crit_rate_bonus" -> critRateBonusPercent = consume(TokenType.NUMBER, "Expect number").lexeme.toDouble()
                "max_all_damage_bonus" -> maxAllDamageBonusPercent = consume(TokenType.NUMBER, "Expect number").lexeme.toDouble()
                else -> throw RuntimeException("Unknown stats field '$field'")
            }
        }
        return StatsNode(baseAttackDamage, attackDamagePerLevel, totalAttackSpeed, maxBaseLevel, maxSkillLevel, critRateBonusPercent, maxAllDamageBonusPercent)
    }

    private fun skill(name: String): SkillNode {
        var effectId = ""
        var cooldown = 0.0
        var duration = 0.0
        var baseValue = 0.0
        var valuePerLevel = 0.0
        var radius = 0.0
        val handlers = mutableListOf<SkillHandlerNode>()
        val buffs = mutableListOf<BuffNode>()

        while (!check(TokenType.RIGHT_BRACE) && !isAtEnd()) {
            val token = advance()
            if (token.type == TokenType.IDENTIFIER && token.lexeme.startsWith("on_")) {
                consume(TokenType.LEFT_BRACE, "Expect '{' for handler")
                handlers.add(handler(token.lexeme))
                consume(TokenType.RIGHT_BRACE, "Expect '}' after handler")
            } else if (token.type == TokenType.KEYWORD_BUFF) {
                val buffId = consume(TokenType.STRING, "Expect buff id").lexeme
                consume(TokenType.LEFT_BRACE, "Expect '{' for buff")
                buffs.add(buff(buffId))
                consume(TokenType.RIGHT_BRACE, "Expect '}' after buff")
            } else {
                consume(TokenType.COLON, "Expect ':'")
                when (token.lexeme) {
                    "effect_id" -> effectId = consume(TokenType.STRING, "Expect effect_id").lexeme
                    "cooldown" -> cooldown = parseTime()
                    "duration" -> duration = parseTime()
                    "base_value" -> baseValue = consume(TokenType.NUMBER, "Expect number").lexeme.toDouble()
                    "value_per_level" -> valuePerLevel = consume(TokenType.NUMBER, "Expect number").lexeme.toDouble()
                    "radius" -> radius = consume(TokenType.NUMBER, "Expect number").lexeme.toDouble()
                }
            }
        }
        return SkillNode(name, effectId, cooldown, duration, baseValue, valuePerLevel, radius, handlers, buffs)
    }

    private fun subSkill(name: String): SubSkillNode {
        var effectId = ""
        var icon = ""
        val handlers = mutableListOf<SkillHandlerNode>()

        while (!check(TokenType.RIGHT_BRACE) && !isAtEnd()) {
            val field = advance().lexeme
            when (field) {
                "effect_id" -> {
                    consume(TokenType.COLON, "Expect ':'")
                    effectId = consume(TokenType.STRING, "Expect string").lexeme
                }
                "icon" -> {
                    consume(TokenType.COLON, "Expect ':'")
                    icon = consume(TokenType.STRING, "Expect string").lexeme
                }
                else -> {
                    if (field.startsWith("on_")) {
                        consume(TokenType.LEFT_BRACE, "Expect '{' for handler")
                        handlers.add(handler(field))
                        consume(TokenType.RIGHT_BRACE, "Expect '}' after handler")
                    }
                }
            }
        }
        return SubSkillNode(name, effectId, icon, handlers)
    }

    private fun buff(id: String): BuffNode {
        var translationKey: String? = null
        var maxStacks = 1
        var duration = 0.0
        val stats = mutableMapOf<String, Double>()

        while (!check(TokenType.RIGHT_BRACE) && !isAtEnd()) {
            val field = advance().lexeme
            when (field) {
                "translation_key" -> {
                    consume(TokenType.COLON, "Expect ':'")
                    translationKey = consume(TokenType.STRING, "Expect string").lexeme
                }
                "max_stacks" -> {
                    consume(TokenType.COLON, "Expect ':'")
                    maxStacks = consume(TokenType.NUMBER, "Expect number").lexeme.toInt()
                }
                "duration" -> {
                    consume(TokenType.COLON, "Expect ':'")
                    duration = parseTime()
                }
                "stats" -> {
                    consume(TokenType.LEFT_BRACE, "Expect '{' for stats")
                    while (!check(TokenType.RIGHT_BRACE) && !isAtEnd()) {
                        val statName = advance().lexeme
                        consume(TokenType.COLON, "Expect ':'")
                        val statValue = consume(TokenType.NUMBER, "Expect number").lexeme.toDouble()
                        stats[statName] = statValue
                    }
                    consume(TokenType.RIGHT_BRACE, "Expect '}' after stats")
                }
            }
        }
        return BuffNode(id, translationKey, maxStacks, duration, stats)
    }

    private fun translations(): MutableMap<String, Map<String, String>> {
        val locales = mutableMapOf<String, Map<String, String>>()
        while (!check(TokenType.RIGHT_BRACE) && !isAtEnd()) {
            val locale = consume(TokenType.IDENTIFIER, "Expect locale id (e.g. ja_jp)").lexeme
            consume(TokenType.LEFT_BRACE, "Expect '{' for locale")
            val pairs = mutableMapOf<String, String>()
            while (!check(TokenType.RIGHT_BRACE) && !isAtEnd()) {
                val token = advance()
                val key = if (token.type == TokenType.IDENTIFIER || token.type == TokenType.STRING) {
                    token.lexeme
                } else {
                    throw RuntimeException("Expect key at line ${token.line}")
                }
                consume(TokenType.COLON, "Expect ':'")
                val value = consume(TokenType.STRING, "Expect string value").lexeme
                pairs[key] = value
            }
            consume(TokenType.RIGHT_BRACE, "Expect '}' after locale")
            locales[locale] = pairs
        }
        return locales
    }

    private fun handler(eventName: String): SkillHandlerNode {
        val actions = mutableListOf<ActionNode>()
        while (!check(TokenType.RIGHT_BRACE) && !isAtEnd()) {
            val name = consume(TokenType.IDENTIFIER, "Expect action name").lexeme
            if (check(TokenType.LEFT_PAREN)) {
                consume(TokenType.LEFT_PAREN, "Expect '('")
                val args = mutableListOf<String>()
                while (!check(TokenType.RIGHT_PAREN)) {
                    val argToken = advance()
                    if (argToken.type == TokenType.STRING) {
                        args.add("\"${argToken.lexeme}\"")
                    } else {
                        var value = argToken.lexeme
                        if (check(TokenType.IDENTIFIER) && peek().lexeme == "s") {
                            value += advance().lexeme
                        }
                        args.add(value)
                    }
                    if (check(TokenType.COMMA)) advance()
                }
                consume(TokenType.RIGHT_PAREN, "Expect ')'")
                
                if (check(TokenType.LEFT_BRACE)) {
                    consume(TokenType.LEFT_BRACE, "Expect '{'")
                    val blockActions = mutableListOf<ActionNode>()
                    // Basic block parsing - doesn't support nested blocks well for now but enough for these actions
                    while (!check(TokenType.RIGHT_BRACE) && !isAtEnd()) {
                        // For area_of_effect we might want specific sub-actions
                        // For now let's just parse them as regular actions if we can recurse
                        // Actually, let's just use CommandActionNode for internal actions too
                        val subName = consume(TokenType.IDENTIFIER, "Expect action name").lexeme
                        consume(TokenType.LEFT_PAREN, "Expect '('")
                        val subArgs = mutableListOf<String>()
                        while (!check(TokenType.RIGHT_PAREN)) {
                            val subArgToken = advance()
                            if (subArgToken.type == TokenType.STRING) subArgs.add("\"${subArgToken.lexeme}\"")
                            else subArgs.add(subArgToken.lexeme)
                            if (check(TokenType.COMMA)) advance()
                        }
                        consume(TokenType.RIGHT_PAREN, "Expect ')'")
                        blockActions.add(CommandActionNode(subName, subArgs))
                    }
                    consume(TokenType.RIGHT_BRACE, "Expect '}'")
                    
                    when (name) {
                        "area_of_effect" -> {
                            val radius = args.getOrNull(0)?.toDoubleOrNull() ?: 5.0
                            actions.add(AreaOfEffectActionNode(radius, blockActions))
                        }
                        else -> actions.add(CommandActionNode(name, args))
                    }
                } else {
                    when (name) {
                        "open_skill_menu" -> {
                            if (args.size < 2) throw RuntimeException("open_skill_menu requires at least one sub-skill and a duration")
                            val subSkillIds = args.dropLast(1).map { it.removeSurrounding("\"") }
                            val duration = parseTimeFromValue(args.last())
                            actions.add(OpenSkillMenuActionNode(subSkillIds, duration))
                        }
                        "close_skill_menu" -> actions.add(CloseSkillMenuActionNode)
                        "send_localized_message" -> {
                             if (args.size < 2) throw RuntimeException("send_localized_message requires at least a key and a color")
                             val key = args[0].removeSurrounding("\"")
                             val color = args[1].removeSurrounding("\"")
                             val callArgs = args.drop(2)
                             actions.add(SendLocalizedMessageActionNode(key, color, callArgs))
                        }
                        else -> actions.add(CommandActionNode(name, args))
                    }
                }
            } else if (name == "execute" && check(TokenType.LEFT_BRACE)) {
                val openBrace = consume(TokenType.LEFT_BRACE, "Expect '{'")
                val blockStartPos = openBrace.endOffset
                var lastPos = blockStartPos
                
                var braceCount = 1
                while (braceCount > 0 && !isAtEnd()) {
                    if (check(TokenType.LEFT_BRACE)) braceCount++
                    else if (check(TokenType.RIGHT_BRACE)) braceCount--
                    
                    if (braceCount > 0) {
                        lastPos = advance().endOffset
                    }
                }
                
                val closeBrace = consume(TokenType.RIGHT_BRACE, "Expect '}'")
                val content = source.substring(blockStartPos, closeBrace.startOffset)
                actions.add(ExecuteActionNode(content.trim()))
            } else {
                    throw RuntimeException("Unexpected token '$name' in handler '$eventName'")
                }
        }
        return SkillHandlerNode(eventName, actions)
    }

    private fun parseTime(): Double {
        val value = consume(TokenType.NUMBER, "Expect number").lexeme.toDouble()
        if (check(TokenType.IDENTIFIER) && peek().lexeme == "s") {
            advance() // consume 's'
        }
        return value
    }

    private fun parseTimeFromValue(value: String): Double {
        if (value.isBlank()) return 0.0
        return if (value.endsWith("s")) {
            value.dropLast(1).toDoubleOrNull() ?: 0.0
        } else {
            value.toDoubleOrNull() ?: 0.0
        }
    }

    private fun consume(type: TokenType, message: String): Token {
        if (check(type)) return advance()
        val token = peek()
        throw RuntimeException("$message at line ${token.line} (expected $type, got ${token.type} '${token.lexeme}')")
    }

    private fun check(type: TokenType) = if (isAtEnd()) false else peek().type == type
    private fun advance(): Token {
        if (!isAtEnd()) current++
        return previous()
    }

    private fun isAtEnd() = peek().type == TokenType.EOF
    private fun peek() = tokens[current]
    private fun previous() = tokens[current - 1]
}
