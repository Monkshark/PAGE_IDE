package page.editor

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private fun refs(vararg names: String, stamp: Long = 0L) = FileSymbols(names.toSet(), emptyMap(), stamp)

class SymbolUsageIndexTest {

    @Test
    fun nameFromAnotherFileCountsAsUsedOutside() {
        val index = SymbolUsageIndex()
        index.setFile("a.kt", refs("todoHeight", "shared"))
        index.setFile("b.kt", refs("shared"))
        assertTrue(index.usedOutside("a.kt", "shared"))
        assertFalse(index.usedOutside("a.kt", "todoHeight"))
    }

    @Test
    fun ownFileDoesNotCountAsOutside() {
        val index = SymbolUsageIndex()
        index.setFile("a.kt", refs("only"))
        assertFalse(index.usedOutside("a.kt", "only"))
        assertTrue(index.usedOutside("b.kt", "only"))
    }

    @Test
    fun aDeclarationInAnotherFileIsNotAUsage() {
        val index = SymbolUsageIndex()
        index.setFile("a.kt", FileSymbols(emptySet(), mapOf("panelTop" to 10)))
        index.setFile("b.kt", FileSymbols(emptySet(), mapOf("panelTop" to 42)))
        assertFalse(index.usedOutside("a.kt", "panelTop"))
        assertEquals(setOf("a.kt", "b.kt"), index.definitionsOf("panelTop").keys)
    }

    @Test
    fun removingAFileDropsItsNames() {
        val index = SymbolUsageIndex()
        index.setFile("a.kt", refs("shared"))
        index.setFile("b.kt", refs("shared"))
        index.removeFile("b.kt")
        assertFalse(index.usedOutside("a.kt", "shared"))
    }

    @Test
    fun replacingAFileReleasesTheNamesItDropped() {
        val index = SymbolUsageIndex()
        index.setFile("a.kt", refs("gone", "kept"))
        index.setFile("b.kt", refs("gone", "kept"))
        index.setFile("b.kt", refs("kept"))
        assertFalse(index.usedOutside("a.kt", "gone"))
        assertTrue(index.usedOutside("a.kt", "kept"))
    }

    @Test
    fun replaceAllRebuildsCounts() {
        val index = SymbolUsageIndex()
        index.setFile("a.kt", refs("old"))
        index.replaceAll(mapOf("c.kt" to refs("fresh")))
        assertFalse(index.usedOutside("a.kt", "old"))
        assertTrue(index.usedOutside("a.kt", "fresh"))
        assertEquals(1, index.fileCount())
    }

    @Test
    fun rescanReusesNamesWhenStampIsUnchanged() {
        val index = SymbolUsageIndex()
        index.replaceAll(mapOf("a.kt" to refs("kept", stamp = 7L)))
        assertEquals(7L, index.entries()["a.kt"]?.stamp)
        index.setFile("a.kt", refs("kept", "typed"))
        assertEquals(7L, index.entries()["a.kt"]?.stamp)
    }

    @Test
    fun replaceAllDropsFilesThatVanished() {
        val index = SymbolUsageIndex()
        index.replaceAll(mapOf("a.kt" to refs("shared"), "b.kt" to refs("shared")))
        index.replaceAll(mapOf("a.kt" to refs("shared")))
        assertFalse(index.usedOutside("a.kt", "shared"))
        assertEquals(1, index.fileCount())
    }

    @Test
    fun uriFormsAndDriveCaseMatchTheSameFile() {
        val index = SymbolUsageIndex()
        index.setFile("file:///C:/proj/a.kt", refs("only"))
        assertTrue(index.knows("file:/c:/proj/a.kt"))
        assertFalse(index.usedOutside("file:/C:/proj/a.kt", "only"))
    }

    @Test
    fun definitionSitesAndReferencesAreQueryable() {
        val index = SymbolUsageIndex()
        index.setFile("a.kt", FileSymbols(setOf("Color"), mapOf("panelTop" to 4)))
        index.setFile("b.kt", FileSymbols(setOf("panelTop"), mapOf("draw" to 9)))
        assertEquals(mapOf("a.kt" to 4), index.definitionsOf("panelTop"))
        assertEquals(setOf("b.kt"), index.referencesOf("panelTop"))
        assertEquals(mapOf("draw" to 9), index.definedIn("b.kt"))
    }

    @Test
    fun listenersFireOnChange() {
        val index = SymbolUsageIndex()
        var calls = 0
        index.addListener { calls++ }
        index.setFile("a.kt", refs("x"))
        index.setFile("a.kt", refs("x"))
        index.setFile("a.kt", refs("y"))
        assertTrue(calls == 2, "expected 2 notifications, got $calls")
    }
}
