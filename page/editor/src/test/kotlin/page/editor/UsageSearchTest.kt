package page.editor

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class UsageSearchTest {

    @Test
    fun findsWholeWordsOnly() {
        val text = "val panelTop = 1\nval panelTopEdge = 2\nuse(panelTop)"
        val hits = UsageSearch.inText("panelTop", "a.kt", text)
        assertEquals(2, hits.size)
        assertEquals(0, hits[0].line)
        assertEquals(2, hits[1].line)
    }

    @Test
    fun columnsAreRelativeToTheLine() {
        val hits = UsageSearch.inText("name", "a.kt", "fun f() {\n    val name = 1\n}")
        assertEquals(1, hits.single().line)
        assertEquals(8, hits.single().startCharacter)
        assertEquals(12, hits.single().endCharacter)
    }

    @Test
    fun aNameInsideAnotherWordIsNotAHit() {
        assertTrue(UsageSearch.inText("set", "a.kt", "toSet() + setting + offset").isEmpty())
    }

    @Test
    fun openBuffersWinOverDisk() {
        val hits = UsageSearch.find("draw", listOf("file:///nowhere.kt")) { "fun draw() = draw()" }
        assertEquals(2, hits.size)
        assertTrue(hits.all { it.uri == "file:///nowhere.kt" })
    }

    @Test
    fun missingFilesAreSkipped() {
        assertTrue(UsageSearch.find("draw", listOf("file:///C:/definitely/missing.kt")).isEmpty())
    }
}
