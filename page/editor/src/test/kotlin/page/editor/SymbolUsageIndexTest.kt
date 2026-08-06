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
        index.replaceAll(mapOf("c.kt" to FileNames(setOf("fresh"))))
        assertFalse(index.usedOutside("a.kt", "old"))
        assertTrue(index.usedOutside("a.kt", "fresh"))
        assertTrue(index.fileCount() == 1)
    }

    @Test
    fun rescanReusesNamesWhenStampIsUnchanged() {
        val index = SymbolUsageIndex()
        index.replaceAll(mapOf("a.kt" to FileNames(setOf("kept"), stamp = 7L)))
        assertTrue(index.entries()["a.kt"]?.stamp == 7L)
        index.setFile("a.kt", setOf("kept", "typed"))
        assertTrue(index.entries()["a.kt"]?.stamp == 7L)
    }

    @Test
    fun replaceAllDropsFilesThatVanished() {
        val index = SymbolUsageIndex()
        index.replaceAll(mapOf("a.kt" to FileNames(setOf("shared")), "b.kt" to FileNames(setOf("shared"))))
        index.replaceAll(mapOf("a.kt" to FileNames(setOf("shared"))))
        assertFalse(index.usedOutside("a.kt", "shared"))
        assertTrue(index.fileCount() == 1)
    }

    @Test
    fun uriFormsAndDriveCaseMatchTheSameFile() {
        val index = SymbolUsageIndex()
        index.setFile("file:///C:/proj/a.kt", setOf("only"))
        assertTrue(index.knows("file:/c:/proj/a.kt"))
        assertFalse(index.usedOutside("file:/C:/proj/a.kt", "only"))
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
