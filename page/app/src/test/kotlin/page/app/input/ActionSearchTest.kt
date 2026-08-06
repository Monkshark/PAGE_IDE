package page.app.input

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ActionSearchTest {

    @Test
    fun `an empty query lists everything grouped`() {
        val all = ActionSearch.rank("")
        assertEquals(ActionCatalog.all.size, all.size)
        val groups = all.map { it.spec.group.ordinal }
        assertEquals(groups.sorted(), groups, "groups must come out in order")
    }

    @Test
    fun `a prefix beats a match in the middle`() {
        val hits = ActionSearch.rank("find")
        assertEquals("Find", hits.first().spec.label)
        assertTrue(hits.any { it.spec.label == "Find in Files" })
    }

    @Test
    fun `initials find an action nobody would type in full`() {
        val hits = ActionSearch.rank("gsp")
        assertTrue(hits.any { it.spec.id == "nav.workspaceSymbol" }, "expected the project symbol search")
    }

    @Test
    fun `matched characters come back for highlighting`() {
        val hit = ActionSearch.rank("save").first()
        assertEquals("file.save", hit.spec.id)
        assertEquals(listOf(0, 1, 2, 3), hit.matchedIndices)
    }

    @Test
    fun `a group name is a valid query`() {
        val hits = ActionSearch.rank("navigate")
        assertTrue(hits.isNotEmpty())
        assertTrue(hits.all { it.spec.group == ActionGroup.Navigate })
    }

    @Test
    fun `nonsense matches nothing`() {
        assertTrue(ActionSearch.rank("zzzqqq").isEmpty())
    }
}
