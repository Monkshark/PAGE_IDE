package page.editor

import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FileTreeGeneratedTest {

    @Test
    fun `a build directory and everything under it is generated`() {
        assertTrue(FileTree.isGenerated(Path.of("/p/build"), depth = 1))
        assertTrue(FileTree.isGenerated(Path.of("/p/build/classes/Main.class"), depth = 3))
        assertTrue(FileTree.isGenerated(Path.of("/p/app/node_modules/left-pad/index.js"), depth = 4))
    }

    @Test
    fun `source files are left alone`() {
        assertFalse(FileTree.isGenerated(Path.of("/p/src/main/kotlin/Main.kt"), depth = 4))
        assertFalse(FileTree.isGenerated(Path.of("/p/README.md"), depth = 1))
    }

    @Test
    fun `the walk stops at the workspace root`() {
        assertFalse(
            FileTree.isGenerated(Path.of("/tmp/project/src/Main.kt"), depth = 2),
            "a workspace living under a folder named tmp must not dim its own sources",
        )
    }

    @Test
    fun `our own cache directory is generated too`() {
        assertTrue(FileTree.isGenerated(Path.of("/p/.page-ide"), depth = 1))
        assertTrue(FileTree.isGenerated(Path.of("/p/.page-ide/lsp/jdtls/jdtls.bat"), depth = 4))
    }

    @Test
    fun `cache suffixes count too`() {
        assertTrue(FileTree.isGenerated(Path.of("/p/.xwin-cache"), depth = 1))
        assertTrue(FileTree.isGenerated(Path.of("/p/thing.egg-info"), depth = 1))
    }
}
