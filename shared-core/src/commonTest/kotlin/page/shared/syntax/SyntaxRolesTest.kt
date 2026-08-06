package page.shared.syntax

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SyntaxRolesTest {

    private fun ident(text: String, vararg names: String): List<Token> =
        names.map { name ->
            val start = text.indexOf(name)
            Token(TokenKind.IDENTIFIER, start..(start + name.length - 1))
        }

    private fun kindOf(text: String, tokens: List<Token>, name: String): TokenKind {
        val start = text.indexOf(name)
        return SyntaxRoles.refine(text, tokens).first { it.start == start }.kind
    }

    @Test
    fun nameBeforeParenIsFunction() {
        val text = "val ms = currentTimeMillis()"
        assertEquals(TokenKind.FUNCTION, kindOf(text, ident(text, "currentTimeMillis"), "currentTimeMillis"))
    }

    @Test
    fun nameAfterDotIsProperty() {
        val text = "status.value = 1"
        assertEquals(TokenKind.PROPERTY, kindOf(text, ident(text, "value"), "value"))
    }

    @Test
    fun callWinsOverMemberAccess() {
        val text = "backend.spawn(env)"
        assertEquals(TokenKind.FUNCTION, kindOf(text, ident(text, "spawn"), "spawn"))
    }

    @Test
    fun declaredArgumentIsParameter() {
        val text = "fun start(backend: LanguageBackend) {}"
        assertEquals(TokenKind.PARAMETER, kindOf(text, ident(text, "backend"), "backend"))
    }

    @Test
    fun lambdaHeaderIsParameter() {
        val text = "list.forEach { item -> use(item) }"
        assertEquals(TokenKind.PARAMETER, kindOf(text, ident(text, "item"), "item"))
    }

    @Test
    fun plainNameStaysIdentifier() {
        val text = "val total = count + 1"
        assertEquals(TokenKind.IDENTIFIER, kindOf(text, ident(text, "count"), "count"))
    }

    @Test
    fun templateExpressionSplitsOutOfString() {
        val text = "\"[lsp] \${backend.id} ready\""
        val tokens = listOf(Token(TokenKind.STRING, 0..(text.length - 1)))
        val refined = SyntaxRoles.refine(text, tokens)
        val template = refined.single { it.kind == TokenKind.TEMPLATE }
        assertEquals("\${backend.id}", text.substring(template.start, template.endExclusive))
        assertTrue(refined.count { it.kind == TokenKind.STRING } == 2, "string keeps its two halves")
    }

    @Test
    fun stringWithoutTemplateIsUntouched() {
        val text = "\"plain text\""
        val tokens = listOf(Token(TokenKind.STRING, 0..(text.length - 1)))
        assertEquals(tokens, SyntaxRoles.refine(text, tokens))
    }

    @Test
    fun escapedDollarIsNotATemplate() {
        val text = "\"cost: \\\${amount}\""
        val tokens = listOf(Token(TokenKind.STRING, 0..(text.length - 1)))
        assertTrue(SyntaxRoles.refine(text, tokens).none { it.kind == TokenKind.TEMPLATE })
    }
}
