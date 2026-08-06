package page.shared.syntax

data class BracketPair(val open: Int, val close: Int, val depth: Int)

object BracketScan {

    private const val OPENERS = "([{"
    private const val CLOSERS = ")]}"

    fun pairs(text: String, tokens: List<Token>): List<BracketPair> {
        if (text.isEmpty()) return emptyList()
        val skip = skipRanges(tokens)
        val pairs = ArrayList<BracketPair>()
        val stack = ArrayList<Int>()
        var skipIndex = 0
        var i = 0
        while (i < text.length) {
            while (skipIndex < skip.size && skip[skipIndex].last < i) skipIndex++
            val range = skip.getOrNull(skipIndex)
            if (range != null && i >= range.first && i <= range.last) {
                i = range.last + 1
                continue
            }
            val c = text[i]
            val openIndex = OPENERS.indexOf(c)
            if (openIndex >= 0) {
                stack.add(i)
            } else {
                val closeIndex = CLOSERS.indexOf(c)
                if (closeIndex >= 0 && stack.isNotEmpty()) {
                    val open = stack.removeAt(stack.size - 1)
                    if (OPENERS.indexOf(text[open]) == closeIndex) {
                        pairs.add(BracketPair(open, i, stack.size))
                    }
                }
            }
            i++
        }
        return pairs
    }

    fun enclosing(pairs: List<BracketPair>, offset: Int): BracketPair? =
        pairs.filter { offset > it.open && offset <= it.close }.maxByOrNull { it.depth }

    private fun skipRanges(tokens: List<Token>): List<IntRange> = tokens
        .asSequence()
        .filter {
            it.kind == TokenKind.STRING ||
                it.kind == TokenKind.COMMENT ||
                it.kind == TokenKind.DOC_COMMENT ||
                it.kind == TokenKind.TODO_TAG
        }
        .map { it.range }
        .sortedBy { it.first }
        .toList()
}
