package page.lsp

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DiagnosticUnusedTest {

    @Test
    fun kotlinUnusedVariableReadsAsUnused() {
        assertTrue(Diagnostic.readsAsUnused("Variable 'windowState' is never used"))
    }

    @Test
    fun javaUnusedImportReadsAsUnused() {
        assertTrue(Diagnostic.readsAsUnused("The import java.util.List is never used"))
    }

    @Test
    fun typescriptUnusedLocalReadsAsUnused() {
        assertTrue(Diagnostic.readsAsUnused("'count' is declared but its value is never read."))
    }

    @Test
    fun goUnusedVariableReadsAsUnused() {
        assertTrue(Diagnostic.readsAsUnused("declared and not used: err"))
    }

    @Test
    fun pythonUnusedImportReadsAsUnused() {
        assertTrue(Diagnostic.readsAsUnused("'os' imported but unused"))
    }

    @Test
    fun rustUnusedVariableReadsAsUnused() {
        assertTrue(Diagnostic.readsAsUnused("unused variable: `total`"))
    }

    @Test
    fun ordinaryWarningsStayColored() {
        assertFalse(Diagnostic.readsAsUnused("Type mismatch: inferred type is String but Int was expected"))
        assertFalse(Diagnostic.readsAsUnused("Condition is always true"))
        assertFalse(Diagnostic.readsAsUnused(null))
    }
}
