package page.app.input

import androidx.compose.ui.input.key.Key
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ActionCatalogTest {

    private fun resolve(
        key: Key,
        primary: Boolean = false,
        control: Boolean = false,
        alt: Boolean = false,
        shift: Boolean = false,
        hasSearch: Boolean = false,
        mac: Boolean = false,
    ) = ActionCatalog.resolve(key, primary, control, alt, shift, hasSearch, mac)?.id

    @Test
    fun `primary alt T is the theme cycle, primary T is the symbol search`() {
        assertEquals("page.cycleTheme", resolve(Key.T, primary = true, alt = true))
        assertEquals("nav.workspaceSymbol", resolve(Key.T, primary = true))
    }

    @Test
    fun `alt separates settings from save`() {
        assertEquals("file.save", resolve(Key.S, primary = true))
        assertEquals("file.settings", resolve(Key.S, primary = true, alt = true))
    }

    @Test
    fun `shift picks the wider variant of open and find`() {
        assertEquals("file.openFile", resolve(Key.O, primary = true))
        assertEquals("file.openFolder", resolve(Key.O, primary = true, shift = true))
        assertEquals("edit.find", resolve(Key.F, primary = true))
        assertEquals("nav.findInFiles", resolve(Key.F, primary = true, shift = true))
    }

    @Test
    fun `undo and redo step aside while a search field is focused`() {
        assertEquals("edit.undo", resolve(Key.Z, primary = true))
        assertEquals("edit.redo", resolve(Key.Z, primary = true, shift = true))
        assertEquals("edit.redoAlternate", resolve(Key.Y, primary = true))
        assertNull(resolve(Key.Z, primary = true, hasSearch = true))
        assertNull(resolve(Key.Y, primary = true, hasSearch = true))
    }

    @Test
    fun `escape only closes a search that is open`() {
        assertNull(resolve(Key.Escape))
        assertEquals("edit.closeSearch", resolve(Key.Escape, hasSearch = true))
    }

    @Test
    fun `tab navigation and context actions yield to an open search`() {
        assertEquals("nav.prevTab", resolve(Key.DirectionLeft, alt = true))
        assertNull(resolve(Key.DirectionLeft, alt = true, hasSearch = true))
        assertEquals("code.action", resolve(Key.Enter, alt = true))
        assertNull(resolve(Key.Enter, alt = true, hasSearch = true))
        assertEquals("code.format", resolve(Key.Enter, alt = true, shift = true, hasSearch = true))
    }

    @Test
    fun `problem jumps split on shift`() {
        assertEquals("nav.nextProblem", resolve(Key.F8))
        assertEquals("nav.prevProblem", resolve(Key.F8, shift = true))
    }

    @Test
    fun `on macOS the primary modifier is cmd and settings moves to cmd comma`() {
        assertEquals("file.save", resolve(Key.S, primary = true, mac = true))
        assertEquals("file.settings", resolve(Key.Comma, primary = true, mac = true))
        assertNull(resolve(Key.S, primary = true, alt = true, mac = true))
    }

    @Test
    fun `theme cycle keeps a literal control chord on macOS`() {
        assertEquals("page.cycleTheme", resolve(Key.T, control = true, alt = true, mac = true))
        assertNull(resolve(Key.T, primary = true, alt = true, mac = true))
    }

    @Test
    fun `only global actions with a body are dispatched`() {
        assertNull(resolve(Key.F2), "editor rename is owned by the editor, not the dispatcher")
        assertNull(resolve(Key.Delete), "tree delete is owned by the file tree")
        assertEquals("file.save", resolve(Key.S, primary = true))
    }

    @Test
    fun `documented editor and tree keys still carry labels`() {
        val rename = ActionCatalog.all.first { it.id == "editor.rename" }
        assertEquals(ActionContext.Editor, rename.context)
        assertEquals("F2", ActionCatalog.label(rename, mac = false))
        val cut = ActionCatalog.all.first { it.id == "tree.cut" }
        assertEquals(ActionContext.FileTree, cut.context)
        assertEquals("Ctrl+X", ActionCatalog.label(cut, mac = false))
        assertEquals("⌘X", ActionCatalog.label(cut, mac = true))
    }

    @Test
    fun `every action carries a binding on both platforms`() {
        for (spec in ActionCatalog.all) {
            assertNotNull(spec.bindingFor(mac = false), "${spec.id} has no Windows binding")
            assertNotNull(spec.bindingFor(mac = true), "${spec.id} has no macOS binding")
        }
    }

    @Test
    fun `no two actions share a chord on the same platform`() {
        for (mac in listOf(false, true)) {
            val seen = HashMap<String, String>()
            for (spec in ActionCatalog.all.filter { it.context == ActionContext.Global }) {
                val binding = spec.bindingFor(mac) ?: continue
                val chord = "$binding|${spec.searchMode}"
                val previous = seen.put(chord, spec.id)
                assertNull(previous, "${spec.id} collides with $previous on ${if (mac) "macOS" else "Windows"}")
            }
        }
    }

    @Test
    fun `labels read as the platform writes them`() {
        val save = ActionCatalog.all.first { it.id == "file.save" }
        assertEquals("Ctrl+S", ActionCatalog.label(save, mac = false))
        assertEquals("⌘S", ActionCatalog.label(save, mac = true))
        val findInFiles = ActionCatalog.all.first { it.id == "nav.findInFiles" }
        assertEquals("Ctrl+Shift+F", ActionCatalog.label(findInFiles, mac = false))
        assertEquals("⇧⌘F", ActionCatalog.label(findInFiles, mac = true))
    }

    @Test
    fun `ids are unique`() {
        val ids = ActionCatalog.all.map { it.id }
        assertTrue(ids.size == ids.toSet().size, "duplicate action id in the catalog")
    }
}
