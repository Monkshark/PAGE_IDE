package page.app

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SettingsCatalogTest {

    @Test
    fun `groups stay contiguous in the sidebar`() {
        val groups = SettingsCategory.entries.map { it.group }
        val collapsed = groups.fold(mutableListOf<String>()) { acc, g ->
            if (acc.lastOrNull() != g) acc += g
            acc
        }
        assertEquals(collapsed, collapsed.distinct(), "a group label would be drawn twice: $collapsed")
    }

    @Test
    fun `every category is reachable from search`() {
        val covered = SETTINGS_INDEX.map { it.category }.toSet()
        val missing = SettingsCategory.entries.toSet() - covered
        assertTrue(missing.isEmpty(), "no search entry points at: ${missing.map { it.name }}")
    }

    @Test
    fun `search matches labels and category names`() {
        assertEquals(
            listOf(SettingsCategory.CODE_STYLE),
            searchSettings("rainbow").map { it.category },
        )
        assertTrue(searchSettings("MINIMAP").any { it.label == "Minimap" })
        assertTrue(searchSettings("auto save").all { it.category == SettingsCategory.AUTO_SAVE })
    }

    @Test
    fun `blank search returns nothing`() {
        assertEquals(emptyList(), searchSettings(""))
        assertEquals(emptyList(), searchSettings("   "))
    }

    @Test
    fun `only the code-shaped categories show the preview`() {
        val withPreview = SettingsCategory.entries.filter { it.showsCode }.toSet()
        assertEquals(
            setOf(SettingsCategory.EDITING, SettingsCategory.THEME, SettingsCategory.CODE_STYLE),
            withPreview,
        )
    }
}
