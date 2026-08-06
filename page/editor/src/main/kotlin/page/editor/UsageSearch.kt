package page.editor

import page.shared.syntax.SymbolNames
import java.nio.file.Files
import java.nio.file.Path

data class UsageHit(
    val uri: String,
    val line: Int,
    val startCharacter: Int,
    val endCharacter: Int,
)

object UsageSearch {

    private const val MAX_FILES = 200
    private const val MAX_HITS = 500

    fun find(
        name: String,
        uris: Collection<String>,
        textFor: (String) -> String? = { null },
    ): List<UsageHit> {
        if (name.isEmpty()) return emptyList()
        val hits = ArrayList<UsageHit>()
        for (uri in uris.take(MAX_FILES)) {
            val text = textFor(uri) ?: readText(uri) ?: continue
            hits += inText(name, uri, text)
            if (hits.size >= MAX_HITS) break
        }
        return hits.take(MAX_HITS)
    }

    fun inText(name: String, uri: String, text: String): List<UsageHit> {
        val hits = ArrayList<UsageHit>()
        var line = 0
        var lineStart = 0
        var i = 0
        while (i < text.length) {
            val c = text[i]
            if (c == '\n') {
                line++
                lineStart = i + 1
                i++
                continue
            }
            if (!SymbolNames.isNameStart(c) && !SymbolNames.isNamePart(c)) {
                i++
                continue
            }
            val start = i
            while (i < text.length && SymbolNames.isNamePart(text[i])) i++
            if (i - start == name.length && text.regionMatches(start, name, 0, name.length)) {
                hits += UsageHit(uri, line, start - lineStart, i - lineStart)
            }
        }
        return hits
    }

    private fun readText(uri: String): String? = runCatching {
        val path = Path.of(java.net.URI(uri))
        if (Files.size(path) > 512L * 1024) null else Files.readString(path)
    }.getOrNull()
}
