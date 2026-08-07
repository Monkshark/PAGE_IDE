package page.app

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import page.lsp.RenameEdit
import page.lsp.RenameFileChange
import page.lsp.RenameWorkspaceEdit

class CodeActionReviewTest {

    private val edit = RenameWorkspaceEdit(
        listOf(
            RenameFileChange("file:///a.kt", listOf(RenameEdit(1, 0, 1, 4, "x"))),
            RenameFileChange("file:///b.kt", listOf(RenameEdit(2, 0, 2, 4, "x"), RenameEdit(9, 0, 9, 4, "x"))),
        ),
    )

    @Test
    fun `nothing excluded leaves the edit untouched`() {
        assertTrue(filterEdit(edit, emptySet()) === edit)
    }

    @Test
    fun `an excluded file drops out with all of its edits`() {
        val filtered = filterEdit(edit, setOf("file:///b.kt"))
        assertEquals(listOf("file:///a.kt"), filtered.changes.map { it.uri })
        assertEquals(1, filtered.totalEditCount)
    }

    @Test
    fun `excluding everything yields an edit that applies nothing`() {
        val filtered = filterEdit(edit, setOf("file:///a.kt", "file:///b.kt"))
        assertTrue(filtered.isEmpty)
    }
}
