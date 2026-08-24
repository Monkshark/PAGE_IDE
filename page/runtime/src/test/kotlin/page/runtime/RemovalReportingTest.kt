package page.runtime

import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RemovalReportingTest {

    private val id = "removal-reporting"

    @AfterTest
    fun tidy() {
        InstallProgressRegistry.removals.keys.toList().forEach { key ->
            val parts = key.split("/", limit = 2)
            if (parts.size == 2) InstallProgressRegistry.finishRemoval(parts[0], parts[1])
        }
    }

    @Test
    fun `a removal knows when it began so the status bar can count up from it`() {
        val before = System.currentTimeMillis()
        InstallProgressRegistry.startRemoval(id, "1.0.0")
        val started = InstallProgressRegistry.removalOf(id, "1.0.0")?.startedAtMs
        assertTrue(started != null && started >= before, "got $started")
    }

    @Test
    fun `the phase says what the removal is waiting on`() {
        InstallProgressRegistry.startRemoval(id, "1.0.0")
        assertEquals("Removing", InstallProgressRegistry.removalOf(id, "1.0.0")?.phase)

        InstallProgressRegistry.removalPhase(id, "1.0.0", "Stopping")
        assertEquals("Stopping", InstallProgressRegistry.removalOf(id, "1.0.0")?.phase)
    }

    @Test
    fun `a phase for a removal that never started is ignored`() {
        InstallProgressRegistry.removalPhase(id, "9.9.9", "Stopping")
        assertTrue(InstallProgressRegistry.removalOf(id, "9.9.9") == null)
    }

    @Test
    fun `a tree that comes away cleanly reports no failures`() {
        val root = tree()
        val seen = mutableListOf<Pair<Int, Int>>()

        val failed = ArchiveExtractors.deleteRecursively(root) { removed, total -> seen.add(removed to total) }

        assertEquals(0, failed)
        assertFalse(Files.exists(root))
        assertTrue(seen.isNotEmpty())
        assertEquals(seen.last().second, seen.last().first, "the last report reaches the total")
    }

    @Test
    fun `removing something that is not there is not a failure`() {
        val gone = Files.createTempDirectory("removal-").resolve("never-existed")
        assertEquals(0, ArchiveExtractors.deleteRecursively(gone) { _, _ -> })
    }

    @Test
    fun `what cannot be deleted is counted, not swallowed`() {
        val root = tree()
        val failed = ArchiveExtractors.deleteRecursively(root, seedOnFirstReport(root))

        assertTrue(failed > 0, "the directory that would not go away should be counted")
        assertTrue(Files.exists(root))
        ArchiveExtractors.deleteRecursively(root)
    }

    @Test
    fun `an install directory that survives the sweep is reported to the caller`() {
        val root = tree()
        val installer = FixedDirInstaller(root)

        assertFailsWith<IOException> { installer.uninstall("1.0.0", seedOnFirstReport(root)) }

        assertTrue(Files.exists(root), "the caller is told precisely because the files are still there")
        ArchiveExtractors.deleteRecursively(root)
    }

    private fun seedOnFirstReport(root: Path): (Int, Int) -> Unit {
        var seeded = false
        return { _, _ ->
            if (!seeded) {
                seeded = true
                Files.writeString(root.resolve("appeared-mid-sweep.txt"), "not in the snapshot")
            }
        }
    }

    @Test
    fun `an install directory that really goes away raises nothing`() {
        val installer = FixedDirInstaller(tree())
        installer.uninstall("1.0.0")
    }

    private fun tree(): Path {
        val root = Files.createTempDirectory("removal-tree-")
        Files.writeString(root.resolve("a.txt"), "a")
        val nested = Files.createDirectories(root.resolve("nested"))
        Files.writeString(nested.resolve("b.txt"), "b")
        Files.writeString(nested.resolve("c.txt"), "c")
        return root
    }

    private class FixedDirInstaller(private val dir: Path) : LspInstaller {
        override val languageId: String = "fixed"
        override val displayName: String = "Fixed"
        override val precheck: LspInstaller.Precheck = LspInstaller.Precheck.Ok
        override fun isInstalled(): Boolean = Files.exists(dir)
        override fun executable(): Path? = null
        override fun install(version: String?, onProgress: (LspInstaller.Progress) -> Unit) = Unit
        override fun installDir(version: String?): Path = dir
    }
}
