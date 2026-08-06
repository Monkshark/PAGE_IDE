package page.shared.syntax

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BracketScanTest {

    @Test
    fun nestedPairsCarryTheirDepth() {
        val text = "fun a() { if (b) { c() } }"
        val pairs = BracketScan.pairs(text, emptyList())
        val braces = pairs.filter { text[it.open] == '{' }.sortedBy { it.open }
        assertEquals(0, braces[0].depth)
        assertEquals(1, braces[1].depth)
    }

    @Test
    fun bracketsInsideStringsAreIgnored() {
        val text = "val s = \"{ not code }\""
        val stringStart = text.indexOf('"')
        val tokens = listOf(Token(TokenKind.STRING, stringStart..(text.length - 1)))
        assertTrue(BracketScan.pairs(text, tokens).isEmpty())
    }

    @Test
    fun bracketsInsideCommentsAreIgnored() {
        val text = "// fun a() {\nval b = 1"
        val tokens = listOf(Token(TokenKind.COMMENT, 0..11))
        assertTrue(BracketScan.pairs(text, tokens).isEmpty())
    }

    @Test
    fun unbalancedClosersDoNotPair() {
        val text = "a) b) c)"
        assertTrue(BracketScan.pairs(text, emptyList()).isEmpty())
    }

    @Test
    fun mismatchedKindsDoNotPair() {
        val text = "( ]"
        assertTrue(BracketScan.pairs(text, emptyList()).isEmpty())
    }

    @Test
    fun enclosingReturnsInnermostBlock() {
        val text = "fun a() { if (b) { c() } }"
        val pairs = BracketScan.pairs(text, emptyList())
        val inner = text.indexOf("c()")
        val found = BracketScan.enclosing(pairs, inner)
        assertEquals(1, found?.depth)
    }

    @Test
    fun enclosingIsNullOutsideEveryBlock() {
        val text = "val a = 1\nfun b() { }"
        val pairs = BracketScan.pairs(text, emptyList())
        assertNull(BracketScan.enclosing(pairs, 2))
    }
}
