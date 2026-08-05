package page.ui

import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ScopeGuidesTest {

    private val depthColors = listOf(Color.Red, Color.Green, Color.Blue)

    private fun guides(rainbow: Boolean) = ScopeGuides(
        enabled = true,
        onlyCurrentBlock = false,
        rainbow = rainbow,
        color = Color.Gray,
        activeColor = Color.White,
        depthColors = depthColors,
    )

    @Test
    fun rainbowCyclesThroughDepthColors() {
        val g = guides(rainbow = true)
        assertEquals(Color.Red.copy(alpha = 0.28f), g.colorAt(0, active = false))
        assertEquals(Color.Blue.copy(alpha = 0.28f), g.colorAt(2, active = false))
        assertEquals(Color.Red.copy(alpha = 0.28f), g.colorAt(3, active = false))
    }

    @Test
    fun activeRailIsStronger() {
        val g = guides(rainbow = true)
        val idle = g.colorAt(1, active = false).alpha
        val active = g.colorAt(1, active = true).alpha
        assertTrue(active > idle)
    }

    @Test
    fun plainModeUsesTheSingleColorPair() {
        val g = guides(rainbow = false)
        assertEquals(Color.Gray, g.colorAt(2, active = false))
        assertEquals(Color.White, g.colorAt(2, active = true))
    }

    @Test
    fun rainbowFallsBackWhenNoDepthColors() {
        val g = guides(rainbow = true).copy(depthColors = emptyList())
        assertEquals(Color.Gray, g.colorAt(0, active = false))
    }

    private val source = listOf(
        "fun outer() {",
        "    val a = compute(",
        "        first,",
        "        second,",
        "    )",
        "    if (a) {",
        "        run()",
        "        run()",
        "    }",
        "}",
    ).joinToString("\n")

    private fun pairsOf(text: String) = page.shared.syntax.BracketScan.pairs(text, emptyList())

    @Test
    fun railSitsAtTheIndentOfTheLineThatOpensTheBlock() {
        val spans = scopeSpansFor(source, pairsOf(source))
        val ifSpan = spans.single { it.fromLine == 6 }
        assertEquals(4, ifSpan.column)
        assertEquals(7, ifSpan.toLine)
    }

    @Test
    fun wrappedArgumentsAlignWithTheirOwnLine() {
        val spans = scopeSpansFor(source, pairsOf(source))
        val callSpan = spans.single { it.fromLine == 2 }
        assertEquals(4, callSpan.column)
        assertEquals(3, callSpan.toLine)
    }

    @Test
    fun topLevelBlockGetsNoRail() {
        val spans = scopeSpansFor(source, pairsOf(source))
        assertTrue(spans.none { it.column == 0 })
    }

    @Test
    fun bracketsClosingWithinTwoLinesGetNoRail() {
        val text = "fun a() {\n    b()\n}"
        assertTrue(scopeSpansFor(text, pairsOf(text)).isEmpty())
    }
}
