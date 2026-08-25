package page.editor

import java.nio.file.Path
import java.util.Locale

object LineComment {

    data class Result(val text: String, val selectionStart: Int, val selectionEnd: Int)

    fun prefixFor(path: Path?): String? {
        val name = path?.fileName?.toString()?.lowercase(Locale.ROOT) ?: return null
        BY_NAME[name]?.let { return it }
        return BY_EXTENSION[name.substringAfterLast('.', missingDelimiterValue = "")]
    }

    private val BY_EXTENSION: Map<String, String> = buildMap {
        for (ext in listOf(
            "kt", "kts", "java", "js", "jsx", "mjs", "cjs", "ts", "tsx",
            "c", "h", "cc", "cpp", "cxx", "hh", "hpp", "hxx",
            "cs", "csx", "go", "rs", "swift", "dart", "scala", "groovy", "php", "jsonc",
        )) put(ext, "// ")
        for (ext in listOf(
            "py", "pyi", "pyw", "rb", "rake", "gemspec", "sh", "bash", "zsh", "fish",
            "yaml", "yml", "toml", "ini", "cfg", "conf", "properties", "r", "pl", "pm",
            "ex", "exs", "nim", "jl", "tf", "gradle",
        )) put(ext, "# ")
        for (ext in listOf("sql", "lua", "hs", "elm", "adb", "ads")) put(ext, "-- ")
        for (ext in listOf("clj", "cljs", "cljc", "edn", "lisp", "el", "scm")) put(ext, ";; ")
        for (ext in listOf("tex", "erl", "m")) put(ext, "% ")
        put("vim", '"' + " ")
        put("bat", "REM ")
        put("cmd", "REM ")
        put("ps1", "# ")
    }

    private val BY_NAME: Map<String, String> = buildMap {
        for (name in listOf(
            "dockerfile", "makefile", "gnumakefile", "gemfile", "rakefile", "vagrantfile",
            "brewfile", "podfile", "fastfile", "appfile", "justfile", "procfile",
            ".gitignore", ".gitattributes", ".dockerignore", ".npmrc", ".nvmrc", ".env",
            ".editorconfig", ".bashrc", ".zshrc", ".profile",
        )) put(name, "# ")
    }

    fun toggle(text: String, selectionStart: Int, selectionEnd: Int, prefix: String): Result {
        val a = selectionStart.coerceIn(0, text.length)
        val b = selectionEnd.coerceIn(0, text.length)
        val lo = minOf(a, b)
        val hi = maxOf(a, b)

        val lineStarts = computeLineStarts(text)
        val firstLine = lineIndexOf(lineStarts, lo)
        val lastLineRaw = lineIndexOf(lineStarts, hi)
        val lastLine = if (hi > lo && hi == lineStarts.getOrNull(lastLineRaw)) {
            lastLineRaw - 1
        } else {
            lastLineRaw
        }.coerceAtLeast(firstLine)

        val trimmedPrefix = prefix.trimEnd()
        val nonBlank = (firstLine..lastLine).filter { idx ->
            val (s, e) = lineRange(text, lineStarts, idx)
            (s until e).any { !text[it].isWhitespace() }
        }
        if (nonBlank.isEmpty()) return Result(text, selectionStart, selectionEnd)

        val minIndent = nonBlank.minOf { idx ->
            val (s, e) = lineRange(text, lineStarts, idx)
            var k = s
            while (k < e && text[k].isWhitespace()) k++
            k - s
        }

        val allCommented = nonBlank.all { idx ->
            val (s, e) = lineRange(text, lineStarts, idx)
            val pos = s + minIndent
            pos + trimmedPrefix.length <= e && text.regionMatches(pos, trimmedPrefix, 0, trimmedPrefix.length)
        }

        val edits = mutableListOf<Edit>()
        if (allCommented) {
            for (idx in nonBlank) {
                val (s, e) = lineRange(text, lineStarts, idx)
                val pos = s + minIndent
                var removeLen = trimmedPrefix.length
                if (pos + removeLen < e && text[pos + removeLen] == ' ') removeLen++
                edits += Edit(pos, removeLen, "")
            }
        } else {
            for (idx in nonBlank) {
                val (s, _) = lineRange(text, lineStarts, idx)
                edits += Edit(s + minIndent, 0, prefix)
            }
        }

        val out = StringBuilder()
        var cursor = 0
        for (e in edits) {
            out.append(text, cursor, e.at)
            out.append(e.insert)
            cursor = e.at + e.removeLen
        }
        out.append(text, cursor, text.length)

        val newStart = shift(a, edits)
        val newEnd = shift(b, edits)
        return Result(out.toString(), newStart, newEnd)
    }

    private data class Edit(val at: Int, val removeLen: Int, val insert: String)

    private fun shift(pos: Int, edits: List<Edit>): Int {
        var p = pos
        for (e in edits) {
            if (e.at >= pos) break
            p += e.insert.length - e.removeLen
            if (e.removeLen > 0 && pos in e.at until e.at + e.removeLen) {
                p = e.at + e.insert.length
            }
        }
        return p.coerceAtLeast(0)
    }

    private fun computeLineStarts(text: String): IntArray {
        val starts = mutableListOf(0)
        for (i in text.indices) {
            if (text[i] == '\n') starts += i + 1
        }
        return starts.toIntArray()
    }

    private fun lineIndexOf(lineStarts: IntArray, offset: Int): Int {
        var lo = 0
        var hi = lineStarts.size - 1
        while (lo < hi) {
            val mid = (lo + hi + 1) ushr 1
            if (lineStarts[mid] <= offset) lo = mid else hi = mid - 1
        }
        return lo
    }

    private fun lineRange(text: String, lineStarts: IntArray, idx: Int): Pair<Int, Int> {
        val s = lineStarts[idx]
        val e = if (idx + 1 < lineStarts.size) lineStarts[idx + 1] - 1 else text.length
        return s to e
    }
}
