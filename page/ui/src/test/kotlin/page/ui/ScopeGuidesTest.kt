package page.ui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ScopeGuidesTest {

    private val source = listOf(
        "fun start() {",          // 0
        "    val a = 1",          // 1
        "    if (a > 0) {",       // 2
        "        run()",          // 3
        "        run()",          // 4
        "    }",                  // 5
        "    return a",           // 6
        "}",                      // 7
    ).joinToString("\n")

    @Test
    fun outerBlockSpansItsBody() {
        val spans = scopeSpansFor(source, indentWidth = 4)
        val outer = spans.single { it.column == 4 }
        assertEquals(1, outer.fromLine)
        assertEquals(6, outer.toLine)
    }

    @Test
    fun nestedBlockGetsItsOwnSpan() {
        val spans = scopeSpansFor(source, indentWidth = 4)
        val inner = spans.single { it.column == 8 }
        assertEquals(3, inner.fromLine)
        assertEquals(4, inner.toLine)
    }

    @Test
    fun blankLinesDoNotBreakASpan() {
        val withBlank = source.replace("        run()\n        run()", "        run()\n\n        run()")
        val spans = scopeSpansFor(withBlank, indentWidth = 4)
        assertTrue(spans.any { it.column == 8 && it.toLine - it.fromLine >= 2 })
    }

    @Test
    fun flatFileHasNoSpans() {
        assertTrue(scopeSpansFor("val a = 1\nval b = 2", indentWidth = 4).isEmpty())
    }

    @Test
    fun tabsCountAsOneIndentStep() {
        val tabbed = "fun start() {\n\tval a = 1\n}"
        assertTrue(scopeSpansFor(tabbed, indentWidth = 4).any { it.column == 4 })
    }
}
