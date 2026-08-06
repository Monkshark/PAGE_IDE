package page.shared.syntax

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SymbolNamesTest {

    @Test
    fun declarationsLandInDefsAndUsagesInRefs() {
        val scan = SymbolNames.scan("val panelTop = Color(0xFF0E141D)\nfun draw() { paint(panelTop) }")
        assertTrue("panelTop" in scan.defs)
        assertTrue("draw" in scan.defs)
        assertTrue("Color" in scan.refs)
        assertTrue("paint" in scan.refs)
        assertTrue("panelTop" in scan.refs)
    }

    @Test
    fun aDeclarationThatIsNeverReadStaysOutOfRefs() {
        val scan = SymbolNames.scan("class Colors {\n    val hubTop = 1\n}")
        assertEquals(setOf("Colors", "hubTop"), scan.defs.keys)
        assertFalse("hubTop" in scan.refs)
    }

    @Test
    fun keywordMustSitDirectlyBeforeTheName() {
        val scan = SymbolNames.scan("value.fun_like = other\nmap[fun] = name")
        assertFalse("name" in scan.defs)
        assertTrue("name" in scan.refs)
    }

    @Test
    fun otherLanguageKeywordsDeclareToo() {
        val scan = SymbolNames.scan("def handler():\n    pass\nfunc Serve() {}\nfn build() {}\nfunction go() {}")
        assertEquals(setOf("handler", "Serve", "build", "go"), scan.defs.keys)
    }

    @Test
    fun importLinesAreIgnoredEntirely() {
        val scan = SymbolNames.scan("import page.shared.syntax.Token\nval token = 1")
        assertFalse("Token" in scan.refs)
        assertTrue("token" in scan.defs)
    }

    @Test
    fun defsRecordTheFirstOffset() {
        val text = "val hit = 1\nval hit = 2"
        assertEquals(4, SymbolNames.scan(text).defs["hit"])
    }
}
