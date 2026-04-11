package hifumi.cresora.compiler

class Parser(private val tokens: List<Token>) {
    private var current = 0

    fun parse(): List<WeaponDefNode> {
        val weapons = mutableListOf<WeaponDefNode>()
        while (!isAtEnd()) {
            weapons.add(weapon())
        }
        return weapons
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

        while (!check(TokenType.RIGHT_BRACE) && !isAtEnd()) {
            val token = advance()
            when (token.lexeme) {
                "id" -> {
                    consume(TokenType.COLON, "Expect ':' after id")
                    id = consume(TokenType.STRING, "Expect id").lexeme
                }
                "rarity" -> {
                    consume(TokenType.COLON, "Expect ':' after rarity")
                    rarity = consume(TokenType.IDENTIFIER, "Expect rarity").lexeme
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
            }
        }

        consume(TokenType.RIGHT_BRACE, "Expect '}' after weapon body")
        return WeaponDefNode(name, id, rarity, baseItem, damageType, stats!!, skill, translations, subSkills)
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
                val key = consume(TokenType.IDENTIFIER, "Expect key").lexeme
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
                
                when (name) {
                    "open_skill_menu" -> {
                        if (args.size < 2) throw RuntimeException("open_skill_menu requires at least one sub-skill and a duration")
                        val subSkillIds = args.dropLast(1).map { it.removeSurrounding("\"") }
                        val duration = parseTimeFromValue(args.last())
                        actions.add(OpenSkillMenuActionNode(subSkillIds, duration))
                    }
                    "close_skill_menu" -> actions.add(CloseSkillMenuActionNode)
                    else -> actions.add(CommandActionNode(name, args))
                }
            } else {
                // Expression or other logic
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
        throw RuntimeException("$message at line ${peek().line}")
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
