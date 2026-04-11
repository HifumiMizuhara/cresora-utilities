package hifumi.cresora.compiler

enum class TokenType {
    IDENTIFIER, NUMBER, STRING,
    LEFT_BRACE, RIGHT_BRACE, COLON, COMMA, LEFT_PAREN, RIGHT_PAREN,
    KEYWORD_WEAPON, KEYWORD_STATS, KEYWORD_SKILL, KEYWORD_BUFF, KEYWORD_TRANSLATIONS,
    KEYWORD_SUB_SKILL, KEYWORD_ICON,
    EOF
}

data class Token(val type: TokenType, val lexeme: String, val line: Int)

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
        "icon" to TokenType.KEYWORD_ICON
    )

    fun scanTokens(): List<Token> {
        while (!isAtEnd()) {
            start = current
            scanToken()
        }
        tokens.add(Token(TokenType.EOF, "", line))
        return tokens
    }

    private fun scanToken() {
        val c = advance()
        when (c) {
            '{' -> addToken(TokenType.LEFT_BRACE)
            '}' -> addToken(TokenType.RIGHT_BRACE)
            ':' -> addToken(TokenType.COLON)
            ',' -> addToken(TokenType.COMMA)
            '(' -> addToken(TokenType.LEFT_PAREN)
            ')' -> addToken(TokenType.RIGHT_PAREN)
            ' ', '\r', '\t' -> {}
            '\n' -> line++
            '"' -> string()
            else -> {
                if (c.isDigit()) {
                    number()
                } else if (c.isLetter() || c == '_') {
                    identifier()
                }
            }
        }
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
        addToken(TokenType.NUMBER)
    }

    private fun string() {
        while (peek() != '"' && !isAtEnd()) {
            if (peek() == '\n') line++
            advance()
        }
        if (isAtEnd()) return // Unterminated string
        advance() // The closing "
        val value = source.substring(start + 1, current - 1)
        addToken(TokenType.STRING, value)
    }

    private fun isAtEnd() = current >= source.length
    private fun advance() = source[current++]
    private fun peek() = if (isAtEnd()) '\u0000' else source[current]
    private fun peekNext() = if (current + 1 >= source.length) '\u0000' else source[current + 1]

    private fun addToken(type: TokenType) {
        addToken(type, source.substring(start, current))
    }

    private fun addToken(type: TokenType, lexeme: String) {
        tokens.add(Token(type, lexeme, line))
    }
}
