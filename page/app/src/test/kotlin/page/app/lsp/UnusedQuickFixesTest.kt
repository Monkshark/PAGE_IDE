package page.app.lsp

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import page.lsp.DiagnosticSeverity

class UnusedQuickFixesTest {

    private val text = "import a.b.C" + System.lineSeparator() +
        "import d.e.F" + System.lineSeparator() +
        "val gone = 1" + System.lineSeparator()

    private fun importRange(line: Int): IntRange {
        val start = text.split(System.lineSeparator()).take(line).sumOf { it.length + System.lineSeparator().length }
        return start until start + "import a.b.C".length
    }

    @Test
    fun `an unused import reads as a warning the problems panel can show`() {
        val diagnostics = UnusedQuickFixes.diagnostics(text, listOf(importRange(0)))
        val one = diagnostics.single()
        assertEquals(DiagnosticSeverity.WARNING, one.severity)
        assertEquals("Unused import", one.message)
        assertEquals(UnusedQuickFixes.IMPORT_CODE, one.code)
        assertTrue(one.unnecessary)
        assertEquals(0, one.start.line)
    }

    @Test
    fun `a dead declaration names itself`() {
        val start = text.indexOf("gone")
        val diagnostics = UnusedQuickFixes.diagnostics(text, listOf(start until start + 4))
        assertEquals("'gone' is never used", diagnostics.single().message)
        assertEquals(UnusedQuickFixes.SYMBOL_CODE, diagnostics.single().code)
    }

    @Test
    fun `the caret line gets its own removal plus a remove-all`() {
        val diagnostics = UnusedQuickFixes.diagnostics(text, listOf(importRange(0), importRange(1)))
        val actions = UnusedQuickFixes.actions("file:///x.kt", text, diagnostics, caretLine = 0)
        assertEquals(listOf("Remove unused import", "Remove all unused imports (2)"), actions.map { it.title })
        val single = actions.first().edit.changes.single().edits.single()
        assertEquals(0, single.startLine)
        assertEquals(1, single.endLine)
        assertEquals("", single.newText)
    }

    @Test
    fun `away from any unused import only the bulk fix is offered`() {
        val diagnostics = UnusedQuickFixes.diagnostics(text, listOf(importRange(0)))
        val actions = UnusedQuickFixes.actions("file:///x.kt", text, diagnostics, caretLine = 2)
        assertEquals(listOf("Remove all unused imports (1)"), actions.map { it.title })
    }

    @Test
    fun `bulk removal deletes from the bottom up so earlier lines stay valid`() {
        val diagnostics = UnusedQuickFixes.diagnostics(text, listOf(importRange(0), importRange(1)))
        val bulk = UnusedQuickFixes.actions("file:///x.kt", text, diagnostics, caretLine = 2).single()
        assertEquals(listOf(1, 0), bulk.edit.changes.single().edits.map { it.startLine })
    }

    @Test
    fun `dead declarations offer no removal`() {
        val start = text.indexOf("gone")
        val diagnostics = UnusedQuickFixes.diagnostics(text, listOf(start until start + 4))
        assertTrue(UnusedQuickFixes.actions("file:///x.kt", text, diagnostics, caretLine = 2).isEmpty())
    }
}
