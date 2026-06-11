package hifumi.cresora.compiler

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class LexerTest {

    @Test
    fun `unterminated string throws`() {
        val ex = assertThrows<RuntimeException> { lex("weapon \"unclosed") }
        assertTrue(ex.message!!.contains("Unterminated string"))
    }

    @Test
    fun `unexpected character throws with line number`() {
        val ex = assertThrows<RuntimeException> { lex("id: \"x\"\n#") }
        assertTrue(ex.message!!.contains("Unexpected character '#'"))
        assertTrue(ex.message!!.contains("line 2"))
    }

    @Test
    fun `time literal 30s lexes as number followed by identifier`() {
        val tokens = lex("30s")
        assertEquals(TokenType.NUMBER, tokens[0].type)
        assertEquals("30", tokens[0].lexeme)
        assertEquals(TokenType.IDENTIFIER, tokens[1].type)
        assertEquals("s", tokens[1].lexeme)
        assertEquals(TokenType.EOF, tokens[2].type)
    }

    @Test
    fun `numeric suffixes stay part of the number token`() {
        assertEquals("20L", lex("20L")[0].lexeme)
        assertEquals("1.5f", lex("1.5f")[0].lexeme)
        assertEquals("2d", lex("2d")[0].lexeme)
        assertEquals("5.25", lex("5.25")[0].lexeme)
    }

    @Test
    fun `line comments are skipped`() {
        val tokens = lex("// comment line\nfoo")
        assertEquals(TokenType.IDENTIFIER, tokens[0].type)
        assertEquals("foo", tokens[0].lexeme)
        assertEquals(2, tokens[0].line)
    }

    @Test
    fun `block comments are skipped and line count survives them`() {
        val tokens = lex("a\n/* one\ntwo */\nb")
        assertEquals("a", tokens[0].lexeme)
        assertEquals(1, tokens[0].line)
        assertEquals("b", tokens[1].lexeme)
        assertEquals(4, tokens[1].line)
    }

    @Test
    fun `token offsets round-trip through source substring`() {
        val source = "weapon \"Name\" { id: \"x\" }"
        for (token in lex(source)) {
            if (token.type == TokenType.EOF) continue
            val slice = source.substring(token.startOffset, token.endOffset)
            if (token.type == TokenType.STRING) {
                assertEquals("\"${token.lexeme}\"", slice)
            } else {
                assertEquals(token.lexeme, slice)
            }
        }
    }

    @Test
    fun `keywords are distinguished from identifiers`() {
        val tokens = lex("weapon stats skill buff custom_word")
        assertEquals(TokenType.KEYWORD_WEAPON, tokens[0].type)
        assertEquals(TokenType.KEYWORD_STATS, tokens[1].type)
        assertEquals(TokenType.KEYWORD_SKILL, tokens[2].type)
        assertEquals(TokenType.KEYWORD_BUFF, tokens[3].type)
        assertEquals(TokenType.IDENTIFIER, tokens[4].type)
    }

    @Test
    fun `multi-character operators lex as single tokens`() {
        val expected = listOf("->", "::", "?:", "?.", "==", "!=", "&&", "||", "<=", ">=", "!!")
        val tokens = lex(expected.joinToString(" "))
        assertEquals(expected, tokens.dropLast(1).map { it.lexeme })
        assertTrue(tokens.dropLast(1).all { it.type == TokenType.OPERATOR })
    }
}
