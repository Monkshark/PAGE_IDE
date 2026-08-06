package page.shared.syntax

data class SymbolScan(val refs: Set<String>, val defs: Map<String, Int>)

object SymbolNames {

    private val DECL_KEYWORDS = setOf(
        "fun", "val", "var", "class", "object", "interface", "enum", "annotation", "typealias",
        "def", "func", "fn", "function", "type", "struct", "trait", "record", "const", "let",
        "protocol", "extension", "namespace", "module",
    )

    fun distinctIn(text: String): Set<String> {
        if (text.isEmpty()) return emptySet()
        val names = HashSet<String>()
        forEachName(text) { name, _ -> names += name }
        return names
    }

    fun scan(text: String): SymbolScan {
        if (text.isEmpty()) return SymbolScan(emptySet(), emptyMap())
        val refs = HashSet<String>()
        val defs = HashMap<String, Int>()
        var previous = ""
        var previousEnd = -1
        forEachName(text) { name, offset ->
            if (previous in DECL_KEYWORDS && onlyBlankBetween(text, previousEnd, offset)) {
                if (name !in defs) defs[name] = offset
            } else {
                refs += name
            }
            previous = name
            previousEnd = offset + name.length
        }
        return SymbolScan(refs, defs)
    }

    fun occurrences(text: String): Map<String, MutableList<Int>> {
        val uses = HashMap<String, MutableList<Int>>()
        forEachName(text) { name, offset -> uses.getOrPut(name) { ArrayList() }.add(offset) }
        return uses
    }

    private fun onlyBlankBetween(text: String, from: Int, to: Int): Boolean {
        if (from < 0 || from > to) return false
        for (i in from until to) if (!text[i].isWhitespace()) return false
        return true
    }

    private inline fun forEachName(text: String, visit: (String, Int) -> Unit) {
        val skip = importLines(text)
        var i = 0
        var skipIndex = 0
        while (i < text.length) {
            if (!isNameStart(text[i])) { i++; continue }
            val start = i
            while (i < text.length && isNamePart(text[i])) i++
            while (skipIndex < skip.size && skip[skipIndex].last < start) skipIndex++
            val line = skip.getOrNull(skipIndex)
            if (line != null && start >= line.first && start <= line.last) continue
            visit(text.substring(start, i), start)
        }
    }

    fun importLines(text: String): List<IntRange> {
        val lines = ArrayList<IntRange>()
        var start = 0
        while (start <= text.length) {
            val newline = text.indexOf('\n', start)
            val end = if (newline < 0) text.length else newline
            if (end > start && text.substring(start, end).trimStart().startsWith("import ")) {
                lines += start until end
            }
            if (newline < 0) break
            start = newline + 1
        }
        return lines
    }

    fun isNameStart(c: Char): Boolean = c.isLetter() || c == '_'

    fun isNamePart(c: Char): Boolean = c.isLetterOrDigit() || c == '_'
}
