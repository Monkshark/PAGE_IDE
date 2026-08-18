package page.runtime

import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DeleteProgressTest {

    private fun tree(fileCount: Int): Path {
        val root = Files.createTempDirectory("delete-progress-")
        val nested = root.resolve("a").resolve("b")
        Files.createDirectories(nested)
        repeat(fileCount) { Files.writeString(nested.resolve("f$it.txt"), "x") }
        return root
    }

    @Test
    fun `progress starts at zero and finishes at the total`() {
        val root = tree(200)
        val reports = mutableListOf<Pair<Int, Int>>()

        ArchiveExtractors.deleteRecursively(root) { removed, total -> reports += removed to total }

        assertFalse(Files.exists(root), "the tree should be gone")
        assertTrue(reports.size >= 2, "expected a first and a last report, got ${reports.size}")
        assertEquals(0, reports.first().first)
        val total = reports.first().second
        assertEquals(203, total, "3 directories plus 200 files")
        assertEquals(total to total, reports.last())
    }

    @Test
    fun `progress never goes backwards`() {
        val root = tree(300)
        val seen = mutableListOf<Int>()

        ArchiveExtractors.deleteRecursively(root) { removed, _ -> seen += removed }

        assertEquals(seen.sorted(), seen, "reports arrived out of order: $seen")
    }

    @Test
    fun `deleting nothing reports nothing`() {
        val missing = Files.createTempDirectory("delete-progress-").resolve("gone")
        var called = false

        ArchiveExtractors.deleteRecursively(missing) { _, _ -> called = true }

        assertFalse(called, "an absent directory should not report progress")
    }

    @Test
    fun `a single file tree still reaches the end`() {
        val root = Files.createTempDirectory("delete-progress-")
        Files.writeString(root.resolve("only.txt"), "x")
        val reports = mutableListOf<Pair<Int, Int>>()

        ArchiveExtractors.deleteRecursively(root) { removed, total -> reports += removed to total }

        assertEquals(2 to 2, reports.last())
        assertFalse(Files.exists(root))
    }
}
