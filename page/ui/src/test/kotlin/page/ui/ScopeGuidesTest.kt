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
}
