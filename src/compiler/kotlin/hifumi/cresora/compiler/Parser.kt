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
            } else if (check(TokenType.KEYWORD_ARTIFACT)) {
                nodes.add(artifact())
            } else {
                val token = peek()
                throw RuntimeException("Unexpected top-level token '${token.lexeme}' at line ${token.line}")
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
                else -> throw RuntimeException("Unknown dictionary field '${token.lexeme}' at line ${token.line}")
            }
        }

        consume(TokenType.RIGHT_BRACE, "Expect '}' after dictionary body")
        return DictionaryDefNode(id, translations)
    }

    private fun artifact(): ArtifactDefNode {
        consume(TokenType.KEYWORD_ARTIFACT, "Expect 'artifact'")
        val name = consume(TokenType.STRING, "Expect artifact name").lexeme
        consume(TokenType.LEFT_BRACE, "Expect '{' before artifact body")

        var id = ""
        val bonuses = mutableListOf<ArtifactBonusNode>()
        var translations = mutableMapOf<String, Map<String, String>>()

        while (!check(TokenType.RIGHT_BRACE) && !isAtEnd()) {
            val token = advance()
            when (token.lexeme) {
                "id" -> {
                    consume(TokenType.COLON, "Expect ':' after id")
                    id = consume(TokenType.STRING, "Expect id").lexeme
                }
                "set" -> {
                    val pieces = consume(TokenType.NUMBER, "Expect number of pieces").lexeme.toInt()
                    consume(TokenType.LEFT_BRACE, "Expect '{' for set bonus")
                    bonuses.add(artifactBonus(pieces))
                    consume(TokenType.RIGHT_BRACE, "Expect '}' after set bonus")
                }
                "translations" -> {
                    consume(TokenType.LEFT_BRACE, "Expect '{' for translations")
                    translations = translations()
                    consume(TokenType.RIGHT_BRACE, "Expect '}' after translations")
                }
                else -> {
                    if (token.type == TokenType.SEMICOLON) {
                        // Skip optional semicolons
                    } else {
                        throw RuntimeException("Unknown artifact field '${token.lexeme}' at line ${token.line}")
                    }
                }
            }
        }

        consume(TokenType.RIGHT_BRACE, "Expect '}' after artifact body")
        if (id.isBlank()) throw RuntimeException("Artifact '$name' is missing required id")
        return ArtifactDefNode(name, id, bonuses, translations)
    }

    private fun artifactBonus(pieces: Int): ArtifactBonusNode {
        val stats = mutableMapOf<String, Double>()
        val handlers = mutableListOf<SkillHandlerNode>()
        val buffs = mutableListOf<BuffNode>()

        while (!check(TokenType.RIGHT_BRACE) && !isAtEnd()) {
            val token = advance()
            if (token.type == TokenType.KEYWORD_STATS) {
                consume(TokenType.LEFT_BRACE, "Expect '{' for stats")
                while (!check(TokenType.RIGHT_BRACE) && !isAtEnd()) {
                    val next = advance()
                    if (next.type == TokenType.SEMICOLON) continue
                    val statName = next.lexeme
                    consume(TokenType.COLON, "Expect ':'")
                    val statValue = consume(TokenType.NUMBER, "Expect number").lexeme.toDouble()
                    stats[statName] = statValue
                }
                consume(TokenType.RIGHT_BRACE, "Expect '}' after stats")
            } else if (token.type == TokenType.KEYWORD_BUFF) {
                val buffId = consume(TokenType.STRING, "Expect buff id").lexeme
                consume(TokenType.LEFT_BRACE, "Expect '{' for buff")
                buffs.add(buff(buffId))
                consume(TokenType.RIGHT_BRACE, "Expect '}' after buff")
            } else if (token.type == TokenType.IDENTIFIER && token.lexeme.startsWith("on_")) {
                consume(TokenType.LEFT_BRACE, "Expect '{' for handler")
                handlers.add(handler(token.lexeme))
                consume(TokenType.RIGHT_BRACE, "Expect '}' after handler")
            } else if (token.type == TokenType.SEMICOLON) {
                // Skip optional semicolons
            } else {
                throw RuntimeException("Unknown artifact bonus field '${token.lexeme}' at line ${token.line}")
            }
        }
        return ArtifactBonusNode(pieces, stats, handlers, buffs)
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
            } else if (token.type == TokenType.SEMICOLON) {
                // Skip optional semicolons
            } else {
                consume(TokenType.COLON, "Expect ':'")
                when (token.lexeme) {
                    "effect_id" -> effectId = consume(TokenType.STRING, "Expect effect_id").lexeme
                    "cooldown" -> cooldown = parseTime()
                    "duration" -> duration = parseTime()
                    "base_value" -> baseValue = consume(TokenType.NUMBER, "Expect number").lexeme.toDouble()
                    "value_per_level" -> valuePerLevel = consume(TokenType.NUMBER, "Expect number").lexeme.toDouble()
                    "radius" -> radius = consume(TokenType.NUMBER, "Expect number").lexeme.toDouble()
                    else -> throw RuntimeException("Unknown skill field '${token.lexeme}' at line ${token.line}")
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
                    } else {
                        throw RuntimeException("Unknown sub_skill field '$field'")
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
            val token = advance()
            if (token.type == TokenType.SEMICOLON) continue
            val field = token.lexeme
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
                        val nextStat = advance()
                        if (nextStat.type == TokenType.SEMICOLON) continue
                        val statName = nextStat.lexeme
                        consume(TokenType.COLON, "Expect ':'")
                        val statValue = consume(TokenType.NUMBER, "Expect number").lexeme.toDouble()
                        stats[statName] = statValue
                    }
                    consume(TokenType.RIGHT_BRACE, "Expect '}' after stats")
                }
                else -> throw RuntimeException("Unknown buff field '$field'")
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
                if (token.type == TokenType.SEMICOLON) continue
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

    private fun action(eventName: String): ActionNode {
        val name = advance().lexeme
        if (name == "execute" && check(TokenType.LEFT_BRACE)) {
            return ExecuteActionNode(executeBlock())
        }

        val args = mutableListOf<String>()
        if (check(TokenType.LEFT_PAREN)) {
            consume(TokenType.LEFT_PAREN, "Expect '('")
            while (!check(TokenType.RIGHT_PAREN) && !isAtEnd()) {
                val argToken = advance()
                if (argToken.type == TokenType.STRING) args.add("\"${argToken.lexeme}\"")
                else {
                    var value = argToken.lexeme
                    if (check(TokenType.IDENTIFIER) && peek().lexeme == "s") {
                        value += advance().lexeme
                    }
                    args.add(value)
                }
                if (check(TokenType.COMMA)) advance()
            }
            consume(TokenType.RIGHT_PAREN, "Expect ')'")
        }

        if (check(TokenType.LEFT_BRACE)) {
            consume(TokenType.LEFT_BRACE, "Expect '{' for block action")
            val blockActions = mutableListOf<ActionNode>()
            while (!check(TokenType.RIGHT_BRACE) && !isAtEnd()) {
                blockActions.add(action(eventName))
                if (check(TokenType.SEMICOLON)) advance()
            }
            consume(TokenType.RIGHT_BRACE, "Expect '}'")
            
            return when (name) {
                "area_of_effect" -> {
                    val radius = args.getOrNull(0)?.toDoubleOrNull() ?: 5.0
                    AreaOfEffectActionNode(radius, blockActions)
                }
                else -> throw RuntimeException("Unsupported block action '$name' in handler '$eventName'")
            }
        } else {
            return when (name) {
                "open_skill_menu" -> {
                    if (args.size < 2) throw RuntimeException("open_skill_menu requires at least one sub-skill and a duration")
                    val subSkillIds = args.dropLast(1).map { it.removeSurrounding("\"") }
                    val duration = parseTimeFromValue(args.last())
                    OpenSkillMenuActionNode(subSkillIds, duration)
                }
                "close_skill_menu" -> CloseSkillMenuActionNode
                "send_localized_message" -> {
                     if (args.size < 2) throw RuntimeException("send_localized_message requires at least a key and a color")
                     val key = args[0].removeSurrounding("\"")
                     val color = args[1].removeSurrounding("\"")
                     val callArgs = args.drop(2)
                     SendLocalizedMessageActionNode(key, color, callArgs)
                }
                else -> {
                    if (InstructionMapping.isKnown(name)) {
                        InstructionCallNode(name, args)
                    } else {
                        CommandActionNode(name, args)
                    }
                }
            }
        }
    }

    private fun handler(eventName: String): SkillHandlerNode {
        val actions = mutableListOf<ActionNode>()
        while (!check(TokenType.RIGHT_BRACE) && !isAtEnd()) {
            actions.add(action(eventName))
            if (check(TokenType.SEMICOLON)) advance()
        }
        return SkillHandlerNode(eventName, actions)
    }

    private fun executeBlock(): List<ActionNode> {
        consume(TokenType.LEFT_BRACE, "Expect '{'")
        val statements = mutableListOf<ActionNode>()
        while (!check(TokenType.RIGHT_BRACE) && !isAtEnd()) {
            val name = peek().lexeme
            // If it's a known instruction or a block action (area_of_effect, etc)
            if (InstructionMapping.isKnown(name) || name == "area_of_effect" || name == "send_localized_message" || name == "close_skill_menu") {
                statements.add(action("execute"))
                if (check(TokenType.SEMICOLON)) advance()
            } else {
                statements.add(statement())
            }
        }
        consume(TokenType.RIGHT_BRACE, "Expect '}'")
        return statements
    }

    private fun statement(): ActionNode {
        val token = peek()
        if (token.type == TokenType.IDENTIFIER && peekNext().type == TokenType.LEFT_PAREN && InstructionMapping.isKnown(token.lexeme)) {
            val name = advance().lexeme
            consume(TokenType.LEFT_PAREN, "Expect '('")
            val args = mutableListOf<String>()
            while (!check(TokenType.RIGHT_PAREN) && !isAtEnd()) {
                val arg = advance()
                if (arg.type == TokenType.STRING) {
                    args.add("\"${arg.lexeme}\"")
                } else {
                    var value = arg.lexeme
                    // Handle time suffixes like 5s
                    if (check(TokenType.IDENTIFIER) && peek().lexeme == "s") {
                        value += advance().lexeme
                    }
                    args.add(value)
                }
                if (check(TokenType.COMMA)) advance()
            }
            consume(TokenType.RIGHT_PAREN, "Expect ')'")
            if (check(TokenType.SEMICOLON)) advance()
            return InstructionCallNode(name, args)
        } else {
            // Fallback to ExpressionNode for raw Kotlin or simple expressions
            val startToken = peek()
            var lastToken = startToken
            var braceCount = 0
            while (!isAtEnd()) {
                if (braceCount == 0 && (check(TokenType.SEMICOLON) || check(TokenType.RIGHT_BRACE))) break
                
                val t = advance()
                if (t.type == TokenType.LEFT_BRACE) braceCount++
                else if (t.type == TokenType.RIGHT_BRACE) braceCount--
                lastToken = t
            }
            val rawContent = source.substring(startToken.startOffset, lastToken.endOffset)
            if (check(TokenType.SEMICOLON)) advance()
            return ExpressionNode(rawContent.trim())
        }
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
    private fun peekNext() = if (current + 1 >= tokens.size) tokens.last() else tokens[current + 1]
    private fun previous() = tokens[current - 1]
}
