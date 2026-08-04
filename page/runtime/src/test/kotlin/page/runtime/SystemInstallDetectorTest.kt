package page.runtime

import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SystemInstallDetectorTest {

    private fun pathOf(vararg dirs: Path): String =
        dirs.joinToString(File.pathSeparator) { it.toString() }

    @Test
    fun `findOnPath locates a binary in a path entry`(@TempDir dir: Path) {
        val bin = Files.createFile(dir.resolve("gopls"))
        val found = SystemInstallDetector.findOnPath(listOf("gopls"), pathOf(dir))
        assertEquals(bin, found)
    }

    @Test
    fun `findOnPath returns first matching name across candidates`(@TempDir dir: Path) {
        val py = Files.createFile(dir.resolve("python3"))
        val found = SystemInstallDetector.findOnPath(listOf("python", "python3"), pathOf(dir))
        assertEquals(py, found)
    }

    @Test
    fun `findOnPath returns null when absent`(@TempDir dir: Path) {
        assertNull(SystemInstallDetector.findOnPath(listOf("rust-analyzer"), pathOf(dir)))
    }

    @Test
    fun `findOnPath returns null on blank path env`() {
        assertNull(SystemInstallDetector.findOnPath(listOf("node"), ""))
        assertNull(SystemInstallDetector.findOnPath(listOf("node"), null))
    }

    @Test
    fun `findOnPath ignores directories that only match a subdir name`(@TempDir dir: Path) {
        Files.createDirectory(dir.resolve("clangd"))
        assertNull(SystemInstallDetector.findOnPath(listOf("clangd"), pathOf(dir)))
    }

    @Test
    fun `detectOnPath returns the path when found`(@TempDir dir: Path) {
        val bin = Files.createFile(dir.resolve("gopls"))
        val hit = SystemInstallDetector.detectOnPath(listOf("gopls"), pathEnv = pathOf(dir))
        assertTrue(hit != null)
        assertEquals(bin, hit.path)
    }

    @Test
    fun `detectOnPath returns null when not found`(@TempDir dir: Path) {
        assertNull(SystemInstallDetector.detectOnPath(listOf("gopls"), pathEnv = pathOf(dir)))
    }

    @Test
    fun `forRuntime scans path for the runtime command`(@TempDir dir: Path) {
        Files.createFile(dir.resolve("node"))
        val hit = SystemInstallDetector.forRuntime("node", pathOf(dir))
        assertTrue(hit != null)
        assertEquals(dir.resolve("node"), hit.path)
    }

    @Test
    fun `forRuntime is null for unknown id`(@TempDir dir: Path) {
        assertNull(SystemInstallDetector.forRuntime("does-not-exist", pathOf(dir)))
    }
}
