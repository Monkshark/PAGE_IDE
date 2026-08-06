package page.lsp

enum class DiagnosticSeverity {
    ERROR, WARNING, INFO, HINT;

    companion object {
        fun fromLsp(value: org.eclipse.lsp4j.DiagnosticSeverity?): DiagnosticSeverity = when (value) {
            org.eclipse.lsp4j.DiagnosticSeverity.Error -> ERROR
            org.eclipse.lsp4j.DiagnosticSeverity.Warning -> WARNING
            org.eclipse.lsp4j.DiagnosticSeverity.Information -> INFO
            org.eclipse.lsp4j.DiagnosticSeverity.Hint -> HINT
            null -> ERROR
        }
    }
}

data class DiagnosticPosition(val line: Int, val character: Int)

data class Diagnostic(
    val start: DiagnosticPosition,
    val end: DiagnosticPosition,
    val severity: DiagnosticSeverity,
    val message: String,
    val source: String? = null,
    val code: String? = null,
    val unnecessary: Boolean = false,
    val deprecated: Boolean = false,
) {
    companion object {
        fun fromLsp(d: org.eclipse.lsp4j.Diagnostic): Diagnostic = Diagnostic(
            start = DiagnosticPosition(d.range.start.line, d.range.start.character),
            end = DiagnosticPosition(d.range.end.line, d.range.end.character),
            severity = DiagnosticSeverity.fromLsp(d.severity),
            message = d.message ?: "",
            source = d.source,
            code = d.code?.let { e ->
                when {
                    e.isLeft -> e.left
                    e.isRight -> e.right?.toString()
                    else -> null
                }
            },
            unnecessary = d.tags?.contains(org.eclipse.lsp4j.DiagnosticTag.Unnecessary) == true ||
                readsAsUnused(d.message),
            deprecated = d.tags?.contains(org.eclipse.lsp4j.DiagnosticTag.Deprecated) == true,
        )

        private val UNUSED_PHRASES = listOf(
            "is never used",
            "are never used",
            "never used",
            "is unused",
            "unused variable",
            "unused import",
            "unused parameter",
            "unused local",
            "declared but its value is never read",
            "declared but never used",
            "declared and not used",
            "imported but unused",
            "assigned but never used",
            "value is never used",
        )

        fun readsAsUnused(message: String?): Boolean {
            val text = message?.lowercase() ?: return false
            return UNUSED_PHRASES.any { text.contains(it) }
        }
    }
}
