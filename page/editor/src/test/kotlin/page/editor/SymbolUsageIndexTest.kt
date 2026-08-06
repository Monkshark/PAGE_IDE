package page.editor

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SymbolUsageIndexTest {

    @Test
    fun nameFromAnotherFileCountsAsUsedOutside() {
        val index = SymbolUsageIndex()
        index.setFile("a.kt", setOf("todoHeight", "shared"))
        index.setFile("b.kt", setOf("shared"))
        assertTrue(index.usedOutside("a.kt", "shared"))
        assertFalse(index.usedOutside("a.kt", "todoHeight"))
    }

    @Test
    fun ownFileDoesNotCountAsOutside() {
        val index = SymbolUsageIndex()
        index.setFile("a.kt", setOf("only"))
        assertFalse(index.usedOutside("a.kt", "only"))
        assertTrue(index.usedOutside("b.kt", "only"))
    }

    @Test
    fun removingAFileDropsItsNames() {
        val index = SymbolUsageIndex()
        index.setFile("a.kt", setOf("shared"))
        index.setFile("b.kt", setOf("shared"))
        index.removeFile("b.kt")
        assertFalse(index.usedOutside("a.kt", "shared"))
    }

    @Test
    fun replacingAFileReleasesTheNamesItDropped() {
        val index = SymbolUsageIndex()
        index.setFile("a.kt", setOf("gone", "kept"))
        index.setFile("b.kt", setOf("gone", "kept"))
        index.setFile("b.kt", setOf("kept"))
        assertFalse(index.usedOutside("a.kt", "gone"))
        assertTrue(index.usedOutside("a.kt", "kept"))
    }

    @Test
    fun replaceAllRebuildsCounts() {
        val index = SymbolUsageIndex()
        index.setFile("a.kt", setOf("old"))
        index.replaceAll(mapOf("c.kt" to setOf("fresh")))
        assertFalse(index.usedOutside("a.kt", "old"))
        assertTrue(index.usedOutside("a.kt", "fresh"))
        assertTrue(index.fileCount() == 1)
    }

    @Test
    fun listenersFireOnChange() {
        val index = SymbolUsageIndex()
        var calls = 0
        index.addListener { calls++ }
        index.setFile("a.kt", setOf("x"))
        index.setFile("a.kt", setOf("x"))
        index.setFile("a.kt", setOf("y"))
        assertTrue(calls == 2, "expected 2 notifications, got $calls")
    }
}
