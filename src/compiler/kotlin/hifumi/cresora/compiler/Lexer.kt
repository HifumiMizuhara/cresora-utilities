package hifumi.cresora.compiler

enum class TokenType {
    IDENTIFIER, NUMBER, STRING,
    LEFT_BRACE, RIGHT_BRACE, COLON, COMMA, LEFT_PAREN, RIGHT_PAREN,
    LEFT_BRACKET, RIGHT_BRACKET,
    DOT, OPERATOR, SEMICOLON,
    KEYWORD_WEAPON, KEYWORD_STATS, KEYWORD_SKILL, KEYWORD_BUFF, KEYWORD_TRANSLATIONS,
    KEYWORD_SUB_SKILL, KEYWORD_ICON, KEYWORD_DICTIONARY, KEYWORD_TEXTURE,
    KEYWORD_ARTIFACT, KEYWORD_SET,
    KEYWORD_MOVEMENT, KEYWORD_PHASE, KEYWORD_PRE_BATTLE, KEYWORD_BATTLE, KEYWORD_POST_BATTLE,
    KEYWORD_DIALOGUE, KEYWORD_COMBAT_HINTS, KEYWORD_GRANTED_WEAPONS, KEYWORD_BATTLE_OBJECTIVE,
    KEYWORD_RESONANT_CHORD_TUTORIAL, KEYWORD_STEP,
    KEYWORD_WAVE, KEYWORD_SPAWNS, KEYWORD_SPAWN, KEYWORD_MODIFIERS, KEYWORD_REWARDS,
    KEYWORD_RESONANCE_CURRENCIES,
    EOF
}

data class Token(val type: TokenType, val lexeme: String, val line: Int, val startOffset: Int, val endOffset: Int)

class Lexer(private val source: String) {
    private val tokens = mutableListOf<Token>()
    private var start = 0
    private var current = 0
    private var line = 1

    private val keywords = mapOf(
        "weapon" to TokenType.KEYWORD_WEAPON,
        "stats" to TokenType.KEYWORD_STATS,
        "skill" to TokenType.KEYWORD_SKILL,
        "buff" to TokenType.KEYWORD_BUFF,
        "translations" to TokenType.KEYWORD_TRANSLATIONS,
        "sub_skill" to TokenType.KEYWORD_SUB_SKILL,
        "icon" to TokenType.KEYWORD_ICON,
        "dictionary" to TokenType.KEYWORD_DICTIONARY,
        "texture" to TokenType.KEYWORD_TEXTURE,
        "artifact" to TokenType.KEYWORD_ARTIFACT,
        "set" to TokenType.KEYWORD_SET,
        "movement" to TokenType.KEYWORD_MOVEMENT,
        "phase" to TokenType.KEYWORD_PHASE,
        "pre_battle" to TokenType.KEYWORD_PRE_BATTLE,
        "battle" to TokenType.KEYWORD_BATTLE,
        "post_battle" to TokenType.KEYWORD_POST_BATTLE,
        "dialogue" to TokenType.KEYWORD_DIALOGUE,
        "combat_hints" to TokenType.KEYWORD_COMBAT_HINTS,
        "granted_weapons" to TokenType.KEYWORD_GRANTED_WEAPONS,
        "battle_objective" to TokenType.KEYWORD_BATTLE_OBJECTIVE,
        "resonant_chord_tutorial" to TokenType.KEYWORD_RESONANT_CHORD_TUTORIAL,
        "step" to TokenType.KEYWORD_STEP,
        "wave" to TokenType.KEYWORD_WAVE,
        "spawns" to TokenType.KEYWORD_SPAWNS,
        "spawn" to TokenType.KEYWORD_SPAWN,
        "modifiers" to TokenType.KEYWORD_MODIFIERS,
        "rewards" to TokenType.KEYWORD_REWARDS,
        "resonance_currencies" to TokenType.KEYWORD_RESONANCE_CURRENCIES
    )

    fun scanTokens(): List<Token> {
        while (!isAtEnd()) {
            start = current
            scanToken()
        }
        tokens.add(Token(TokenType.EOF, "", line, current, current))
        return tokens
    }

    private fun scanToken() {
        val c = advance()
        when (c) {
            '{' -> addToken(TokenType.LEFT_BRACE)
            '}' -> addToken(TokenType.RIGHT_BRACE)
            ',' -> addToken(TokenType.COMMA)
            '(' -> addToken(TokenType.LEFT_PAREN)
            ')' -> addToken(TokenType.RIGHT_PAREN)
            '[' -> addToken(TokenType.LEFT_BRACKET)
            ']' -> addToken(TokenType.RIGHT_BRACKET)
            '.' -> addToken(TokenType.DOT)
            ';' -> addToken(TokenType.SEMICOLON)
            '?' -> if (match(':')) addToken(TokenType.OPERATOR, "?:") else if (match('.')) addToken(TokenType.OPERATOR, "?.") else addToken(TokenType.OPERATOR)
            '-' -> if (match('>')) addToken(TokenType.OPERATOR, "->") else addToken(TokenType.OPERATOR)
            ':' -> if (match(':')) addToken(TokenType.OPERATOR, "::") else addToken(TokenType.COLON)
            '!' -> if (match('=')) addToken(TokenType.OPERATOR, "!=") else if (match('!')) addToken(TokenType.OPERATOR, "!!") else addToken(TokenType.OPERATOR)
            '@' -> addToken(TokenType.OPERATOR)
            '<' -> if (match('=')) addToken(TokenType.OPERATOR, "<=") else addToken(TokenType.OPERATOR)
            '>' -> if (match('=')) addToken(TokenType.OPERATOR, ">=") else addToken(TokenType.OPERATOR)
            '=' -> if (match('=')) addToken(TokenType.OPERATOR, "==") else addToken(TokenType.OPERATOR)
            '&' -> if (match('&')) addToken(TokenType.OPERATOR, "&&") else addToken(TokenType.OPERATOR)
            '|' -> if (match('|')) addToken(TokenType.OPERATOR, "||") else addToken(TokenType.OPERATOR)
            '/' -> {
                when {
                    match('/') -> while (peek() != '\n' && !isAtEnd()) advance()
                    match('*') -> scanBlockComment()
                    else -> addToken(TokenType.OPERATOR)
                }
            }
            '+', '*', '%' -> addToken(TokenType.OPERATOR)
            ' ', '\r', '\t' -> {}
            '\n' -> line++
            '"' -> string()
            else -> {
                if (c.isDigit()) {
                    number()
                } else if (c.isLetter() || c == '_') {
                    identifier()
                } else {
                    throw RuntimeException("Unexpected character '$c' at line $line")
                }
            }
        }
    }

    private fun match(expected: Char): Boolean {
        if (isAtEnd()) return false
        if (source[current] != expected) return false
        current++
        return true
    }

    private fun identifier() {
        while (peek().isLetterOrDigit() || peek() == '_') advance()
        val text = source.substring(start, current)
        val type = keywords[text] ?: TokenType.IDENTIFIER
        addToken(type)
    }

    private fun number() {
        while (peek().isDigit()) advance()
        if (peek() == '.' && peekNext().isDigit()) {
            advance()
            while (peek().isDigit()) advance()
        }
        if (peek() == 'L' || peek() == 'f' || peek() == 'd' || peek() == 'F' || peek() == 'D') {
            advance()
        }
        addToken(TokenType.NUMBER)
    }

    private fun string() {
        while (peek() != '"' && !isAtEnd()) {
            if (peek() == '\n') line++
            advance()
        }
        if (isAtEnd()) {
            throw RuntimeException("Unterminated string at line $line")
        }
        advance() // The closing "
        val value = source.substring(start + 1, current - 1)
        addToken(TokenType.STRING, value)
    }

    private fun scanBlockComment() {
        while (!isAtEnd()) {
            if (peek() == '*' && peekNext() == '/') {
                advance()
                advance()
                return
            }
            if (peek() == '\n') line++
            advance()
        }
    }

    private fun isAtEnd() = current >= source.length
    private fun advance() = source[current++]
    private fun peek() = if (isAtEnd()) '\u0000' else source[current]
    private fun peekNext() = if (current + 1 >= source.length) '\u0000' else source[current + 1]

    private fun addToken(type: TokenType) {
        addToken(type, source.substring(start, current))
    }

    private fun addToken(type: TokenType, lexeme: String) {
        tokens.add(Token(type, lexeme, line, start, current))
    }
}
