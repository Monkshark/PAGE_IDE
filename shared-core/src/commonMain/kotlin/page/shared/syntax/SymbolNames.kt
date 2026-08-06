package page.shared.syntax

object SymbolNames {

    fun distinctIn(text: String): Set<String> {
        if (text.isEmpty()) return emptySet()
        val names = HashSet<String>()
        forEachName(text) { name, _ -> names += name }
        return names
    }

    fun occurrences(text: String): Map<String, MutableList<Int>> {
        val uses = HashMap<String, MutableList<Int>>()
        forEachName(text) { name, offset -> uses.getOrPut(name) { ArrayList() }.add(offset) }
        return uses
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
