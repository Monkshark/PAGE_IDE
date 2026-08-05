package page.app

import page.shared.syntax.Token
import page.shared.syntax.TokenKind

/**
 * Finds the other places the identifier under the caret appears, so the editor can tint them. Only
 * code tokens count — the same word inside a string or a comment is not the same symbol.
 */
object IdentifierOccurrences {

    private val CODE_KINDS = setOf(
        TokenKind.IDENTIFIER,
        TokenKind.FUNCTION,
        TokenKind.PROPERTY,
        TokenKind.PARAMETER,
        TokenKind.TYPE,
    )

    fun find(text: String, tokens: List<Token>, caret: Int): List<IntRange> {
        val anchor = tokenAt(tokens, caret) ?: return emptyList()
        if (anchor.kind !in CODE_KINDS) return emptyList()
        val start = anchor.start.coerceIn(0, text.length)
        val end = anchor.endExclusive.coerceIn(start, text.length)
        val name = text.substring(start, end)
        if (name.length < 2) return emptyList()
        return tokens.asSequence()
            .filter { it.kind in CODE_KINDS }
            .filter { it.endExclusive - it.start == name.length }
            .filter { it.start != anchor.start }
            .filter {
                val s = it.start.coerceIn(0, text.length)
                val e = it.endExclusive.coerceIn(s, text.length)
                text.regionMatches(s, name, 0, name.length) && e - s == name.length
            }
            .map { it.start..(it.endExclusive - 1) }
            .toList()
    }

    private fun tokenAt(tokens: List<Token>, caret: Int): Token? =
        tokens.firstOrNull { caret >= it.start && caret <= it.endExclusive }
}
