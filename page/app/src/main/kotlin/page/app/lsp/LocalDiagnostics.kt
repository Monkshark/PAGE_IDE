package page.app.lsp

import androidx.compose.runtime.mutableStateMapOf
import page.lsp.Diagnostic

object LocalDiagnostics {

    private val byUri = mutableStateMapOf<String, List<Diagnostic>>()

    fun set(uri: String, diagnostics: List<Diagnostic>) {
        if (diagnostics.isEmpty()) byUri.remove(uri) else byUri[uri] = diagnostics
    }

    fun remove(uri: String) {
        byUri.remove(uri)
    }

    fun forUri(uri: String): List<Diagnostic> = byUri[uri].orEmpty()

    fun merged(lsp: Map<String, List<Diagnostic>>): Map<String, List<Diagnostic>> {
        if (byUri.isEmpty()) return lsp
        val out = LinkedHashMap<String, List<Diagnostic>>(lsp)
        for ((uri, local) in byUri) {
            out[uri] = out[uri].orEmpty() + local
        }
        return out
    }
}
