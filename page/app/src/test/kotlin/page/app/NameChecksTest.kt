package page.app

import page.lsp.RenameEdit
import page.lsp.RenameFileChange
import page.lsp.RenameWorkspaceEdit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class NameChecksTest {

    @Test
    fun `symbol name must change`() {
        assertIs<RenameCheck.Unchanged>(RenameName.check("FileIcons", "FileIcons"))
        assertIs<RenameCheck.Ready>(RenameName.check("IconTable", "FileIcons"))
    }

    @Test
    fun `symbol name rejects what an identifier cannot hold`() {
        assertIs<RenameCheck.Empty>(RenameName.check("   ", "FileIcons"))
        assertIs<RenameCheck.Invalid>(RenameName.check("Icon Table", "FileIcons"))
        assertIs<RenameCheck.Invalid>(RenameName.check("2Icons", "FileIcons"))
        assertIs<RenameCheck.Invalid>(RenameName.check("Icon-Table", "FileIcons"))
        assertIs<RenameCheck.Ready>(RenameName.check("_iconTable2", "FileIcons"))
    }

    @Test
    fun `scope label counts edits and files`() {
        val edit = RenameWorkspaceEdit(
            listOf(
                RenameFileChange("file:///a.kt", listOf(renameEdit(1), renameEdit(2))),
                RenameFileChange("file:///b.kt", listOf(renameEdit(3))),
            ),
        )
        assertEquals("3 edits · 2 files", RenameName.scopeLabel(edit))
    }

    @Test
    fun `scope label stays singular for one edit`() {
        val edit = RenameWorkspaceEdit(listOf(RenameFileChange("file:///a.kt", listOf(renameEdit(1)))))
        assertEquals("1 edit · 1 file", RenameName.scopeLabel(edit))
    }

    @Test
    fun `file name rejects what the filesystem cannot hold`() {
        assertIs<NameCheck.Empty>(FileName.check(" "))
        assertIs<NameCheck.Invalid>(FileName.check("a/b.kt"))
        assertIs<NameCheck.Invalid>(FileName.check("what?.kt"))
        assertIs<NameCheck.Invalid>(FileName.check("trailing."))
        assertIs<NameCheck.Invalid>(FileName.check(".."))
        assertIs<NameCheck.Invalid>(FileName.check("con.txt"))
        assertIs<NameCheck.Ready>(FileName.check("FileIcons.kt"))
        assertIs<NameCheck.Ready>(FileName.check(".gitignore"))
    }

    private fun renameEdit(line: Int) = RenameEdit(line, 0, line, 4, "x")
}
