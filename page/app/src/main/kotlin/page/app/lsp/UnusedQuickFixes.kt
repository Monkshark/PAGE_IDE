package page.app.lsp

import page.lsp.CodeActionEntry
import page.lsp.Diagnostic
import page.lsp.DiagnosticPosition
import page.lsp.DiagnosticSeverity
import page.lsp.RenameEdit
import page.lsp.RenameFileChange
import page.lsp.RenameWorkspaceEdit

object UnusedQuickFixes {

    const val IMPORT_CODE = "unused.import"
    const val SYMBOL_CODE = "unused.symbol"
    private const val SOURCE = "page"

    fun diagnostics(text: String, ranges: List<IntRange>): List<Diagnostic> = ranges.mapNotNull { range ->
        val start = range.first.coerceIn(0, text.length)
        val end = (range.last + 1).coerceIn(start, text.length)
        if (start >= end) return@mapNotNull null
        val name = text.substring(start, end)
        val isImport = name.startsWith("import")
        Diagnostic(
            start = positionOf(text, start),
            end = positionOf(text, end),
            severity = DiagnosticSeverity.WARNING,
            message = if (isImport) "Unused import" else "'$name' is never used",
            source = SOURCE,
            code = if (isImport) IMPORT_CODE else SYMBOL_CODE,
            unnecessary = true,
        )
    }

    fun actions(uri: String, text: String, diagnostics: List<Diagnostic>, caretLine: Int): List<CodeActionEntry> {
        val imports = diagnostics.filter { it.code == IMPORT_CODE }
        if (imports.isEmpty()) return emptyList()
        val actions = ArrayList<CodeActionEntry>(2)
        val here = imports.firstOrNull { it.start.line == caretLine }
        if (here != null) {
            actions += entry("Remove unused import", uri, listOf(deleteLine(text, here.start.line)))
        }
        if (imports.size > 1 || here == null) {
            val edits = imports
                .map { it.start.line }
                .distinct()
                .sortedDescending()
                .map { line -> deleteLine(text, line) }
            actions += entry("Remove all unused imports (${imports.size})", uri, edits)
        }
        return actions
    }

    private fun entry(title: String, uri: String, edits: List<RenameEdit>): CodeActionEntry = CodeActionEntry(
        title = title,
        kind = "source.organizeImports",
        isPreferred = true,
        edit = RenameWorkspaceEdit(listOf(RenameFileChange(uri, edits))),
        command = null,
    )

    private fun deleteLine(text: String, line: Int): RenameEdit {
        val lastLine = text.count { it == '\n' }
        val endLine = (line + 1).coerceAtMost(lastLine)
        return if (endLine > line) {
            RenameEdit(line, 0, endLine, 0, "")
        } else {
            RenameEdit(line, 0, line, lineLength(text, line), "")
        }
    }

    private fun lineLength(text: String, line: Int): Int {
        var index = 0
        var current = 0
        while (current < line && index < text.length) {
            if (text[index] == '\n') current++
            index++
        }
        var end = index
        while (end < text.length && text[end] != '\n') end++
        return end - index
    }

    private fun positionOf(text: String, offset: Int): DiagnosticPosition {
        var line = 0
        var lineStart = 0
        var i = 0
        while (i < offset) {
            if (text[i] == '\n') {
                line++
                lineStart = i + 1
            }
            i++
        }
        return DiagnosticPosition(line, offset - lineStart)
    }
}
