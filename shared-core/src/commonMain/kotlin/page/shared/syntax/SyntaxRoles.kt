package page.shared.syntax

/**
 * Upgrades plain identifier and string tokens into finer roles so themes can style calls, members,
 * parameters and string templates apart. Runs on the token list, which keeps it independent of the
 * lexer backend (hand written or tree-sitter) and of the language in front of it.
 */
object SyntaxRoles {

    fun refine(text: String, tokens: List<Token>): List<Token> {
        if (tokens.isEmpty()) return tokens
        val out = ArrayList<Token>(tokens.size + 8)
        for (i in tokens.indices) {
            val token = tokens[i]
            when (token.kind) {
                TokenKind.IDENTIFIER -> out += token.copy(kind = identifierRole(text, tokens, i))
                TokenKind.STRING -> splitTemplate(text, token, out)
                else -> out += token
            }
        }
        return out
    }

    private fun identifierRole(text: String, tokens: List<Token>, index: Int): TokenKind {
        val token = tokens[index]
        if (nextNonSpace(text, token.endExclusive) == '(') return TokenKind.FUNCTION
        if (prevNonSpace(text, token.start) == '.') return TokenKind.PROPERTY
        if (isParameter(text, tokens, index)) return TokenKind.PARAMETER
        return TokenKind.IDENTIFIER
    }

    /**
     * A parameter is an identifier that is being introduced rather than used: it sits right after an
     * opening paren or a comma inside a signature, or ahead of the `->` of a lambda header.
     */
    private fun isParameter(text: String, tokens: List<Token>, index: Int): Boolean {
        val token = tokens[index]
        val before = prevNonSpace(text, token.start)
        val after = nextNonSpace(text, token.endExclusive)
        if (after == '-' && charAt(text, nextNonSpaceIndex(text, token.endExclusive) + 1) == '>') return true
        if (before != '(' && before != ',') return false
        return after == ':' || after == ',' || after == ')' || after == '='
    }

    private fun splitTemplate(text: String, token: Token, out: MutableList<Token>) {
        val start = token.start
        val end = token.endExclusive.coerceAtMost(text.length)
        if (start >= end) { out += token; return }
        var cursor = start
        var i = start
        while (i < end - 1) {
            if (text[i] == '$' && text[i + 1] == '{' && !isEscaped(text, i)) {
                val close = matchingBrace(text, i + 1, end) ?: break
                if (cursor < i) out += Token(TokenKind.STRING, cursor until i)
                out += Token(TokenKind.TEMPLATE, i..close)
                cursor = close + 1
                i = close + 1
                continue
            }
            i++
        }
        if (cursor < end) out += Token(TokenKind.STRING, cursor until end)
        else if (cursor == start) out += token
    }

    private fun matchingBrace(text: String, openIndex: Int, limit: Int): Int? {
        var depth = 0
        var i = openIndex
        while (i < limit) {
            when (text[i]) {
                '{' -> depth++
                '}' -> {
                    depth--
                    if (depth == 0) return i
                }
            }
            i++
        }
        return null
    }

    private fun isEscaped(text: String, index: Int): Boolean {
        var backslashes = 0
        var i = index - 1
        while (i >= 0 && text[i] == '\\') { backslashes++; i-- }
        return backslashes % 2 == 1
    }

    private fun charAt(text: String, index: Int): Char? = text.getOrNull(index)

    private fun nextNonSpaceIndex(text: String, from: Int): Int {
        var i = from
        while (i < text.length && (text[i] == ' ' || text[i] == '\t')) i++
        return i
    }

    private fun nextNonSpace(text: String, from: Int): Char? = text.getOrNull(nextNonSpaceIndex(text, from))

    private fun prevNonSpace(text: String, from: Int): Char? {
        var i = from - 1
        while (i >= 0 && (text[i] == ' ' || text[i] == '\t')) i--
        return text.getOrNull(i)
    }
}
