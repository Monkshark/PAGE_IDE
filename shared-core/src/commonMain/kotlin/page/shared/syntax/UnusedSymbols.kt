package page.shared.syntax

object UnusedSymbols {

    private val DECLARATION_KEYWORDS = setOf("val", "var", "let", "const")

    private val CODE_KINDS = setOf(
        TokenKind.IDENTIFIER,
        TokenKind.FUNCTION,
        TokenKind.PROPERTY,
        TokenKind.PARAMETER,
        TokenKind.TYPE,
    )

    fun find(
        text: String,
        tokens: List<Token>,
        pairs: List<BracketPair>,
    ): List<IntRange> {
        if (text.isEmpty() || tokens.isEmpty()) return emptyList()
        val importLines = importLines(text)
        val uses = wordUses(text, importLines)
        val ranges = ArrayList<IntRange>()
        ranges += unusedImports(text, importLines, uses)
        ranges += unusedLocals(text, tokens, pairs, uses)
        return ranges
    }

    private fun wordUses(text: String, importLines: List<IntRange>): Map<String, MutableList<Int>> {
        val uses = HashMap<String, MutableList<Int>>()
        var i = 0
        while (i < text.length) {
            if (!isNameStart(text[i])) { i++; continue }
            val start = i
            while (i < text.length && isNamePart(text[i])) i++
            if (importLines.any { start >= it.first && start <= it.last }) continue
            uses.getOrPut(text.substring(start, i)) { ArrayList() }.add(start)
        }
        return uses
    }

    private fun lineRangeAt(text: String, offset: Int): IntRange {
        var start = offset.coerceIn(0, text.length)
        while (start > 0 && text[start - 1] != '\n') start--
        var end = offset.coerceIn(0, text.length)
        while (end < text.length && text[end] != '\n') end++
        return start until end
    }

    private fun isNameStart(c: Char): Boolean = c.isLetter() || c == '_'

    private fun isNamePart(c: Char): Boolean = c.isLetterOrDigit() || c == '_'

    private fun unusedImports(
        text: String,
        importLines: List<IntRange>,
        uses: Map<String, MutableList<Int>>,
    ): List<IntRange> {
        val ranges = ArrayList<IntRange>()
        for (line in importLines) {
            val raw = text.substring(line.first, line.last + 1)
            val symbol = importedSymbol(raw) ?: continue
            if (symbol == "*") continue
            if (uses[symbol]?.isNotEmpty() == true) continue
            val leading = raw.length - raw.trimStart().length
            val start = line.first + leading
            val end = line.first + raw.trimEnd().length
            if (start < end) ranges += start until end
        }
        return ranges
    }

    private fun unusedLocals(
        text: String,
        tokens: List<Token>,
        pairs: List<BracketPair>,
        uses: Map<String, MutableList<Int>>,
    ): List<IntRange> {
        if (pairs.isEmpty()) return emptyList()
        val closers = HashMap<Int, Int>(pairs.size)
        for (pair in pairs) closers[pair.close] = pair.open
        val ranges = ArrayList<IntRange>()
        for (i in tokens.indices) {
            val token = tokens[i]
            if (token.kind != TokenKind.IDENTIFIER) continue
            val previous = tokens.getOrNull(i - 1) ?: continue
            if (previous.kind != TokenKind.KEYWORD) continue
            val keyword = text.substring(
                previous.start.coerceIn(0, text.length),
                previous.endExclusive.coerceIn(0, text.length),
            )
            if (keyword !in DECLARATION_KEYWORDS) continue
            val enclosing = BracketScan.enclosing(pairs, token.start) ?: continue
            if (declaresType(text, enclosing.open, closers)) continue
            val name = nameOf(text, token) ?: continue
            val declarationLine = lineRangeAt(text, token.start)
            val usedElsewhere = uses[name].orEmpty().any { it !in declarationLine }
            if (usedElsewhere) continue
            ranges += token.start until token.endExclusive
        }
        return ranges
    }

    private const val HEADER_LOOKBEHIND = 600

    private val TYPE_KEYWORDS = listOf(
        "class ", "object ", "interface ", "enum ", "companion ", "struct ", "trait ", "record ",
    )

    private fun declaresType(text: String, openOffset: Int, closers: Map<Int, Int>): Boolean {
        val header = StringBuilder()
        var i = openOffset - 1
        var steps = 0
        while (i >= 0 && steps++ < HEADER_LOOKBEHIND) {
            val c = text[i]
            val open = closers[i]
            if (open != null) {
                header.append(' ')
                i = open - 1
                continue
            }
            if (c == '{' || c == '}' || c == ';') break
            header.append(c)
            i--
        }
        val reversed = header.reverse().toString()
        return TYPE_KEYWORDS.any { reversed.contains(it) }
    }

    private fun importLines(text: String): List<IntRange> {
        val lines = ArrayList<IntRange>()
        var start = 0
        while (start <= text.length) {
            val newline = text.indexOf('\n', start)
            val end = if (newline < 0) text.length else newline
            if (end > start) {
                val raw = text.substring(start, end)
                if (raw.trimStart().startsWith("import ")) lines += start until end
            }
            if (newline < 0) break
            start = newline + 1
        }
        return lines
    }

    private fun importedSymbol(line: String): String? {
        val body = line.trim().removePrefix("import ").trim().trimEnd(';')
        if (body.isEmpty()) return null
        val alias = body.substringAfter(" as ", "").trim()
        if (alias.isNotEmpty()) return alias
        val path = body.substringBefore(' ').trim()
        return path.substringAfterLast('.').takeIf { it.isNotEmpty() }
    }

    private fun nameOf(text: String, token: Token): String? {
        val start = token.start.coerceIn(0, text.length)
        val end = token.endExclusive.coerceIn(start, text.length)
        if (start >= end) return null
        return text.substring(start, end).trimStart('$').takeIf { it.isNotEmpty() }
    }
}
