package page.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HoverPositionProviderTest {

    private val window = IntSize(1440, 860)
    private val editor = IntRect(left = 300, top = 60, right = 1440, bottom = 860)
    private val card = IntSize(520, 300)

    private fun place(anchor: Offset, size: IntSize = card) =
        hoverPopupOffset(anchor, editor, window, size)

    @Test
    fun `sits below and right of the pointer when there is room`() {
        val at = place(Offset(100f, 100f))
        assertEquals(300 + 100 + 12, at.x)
        assertEquals(60 + 100 + 18, at.y)
    }

    @Test
    fun `flips to the left edge of the pointer when it would overflow`() {
        val at = place(Offset(1000f, 100f))
        assertEquals(300 + 1000 - 12 - card.width, at.x)
        assertTrue(at.x + card.width <= window.width)
    }

    @Test
    fun `flips above the pointer when it would overflow the bottom`() {
        val at = place(Offset(100f, 700f))
        assertEquals(60 + 700 - 10 - card.height, at.y)
        assertTrue(at.y + card.height <= window.height)
    }

    @Test
    fun `never leaves the window when the card is larger than the gap`() {
        val huge = IntSize(1400, 840)
        val at = place(Offset(1100f, 780f), huge)
        assertTrue(at.x >= 0, "x=${at.x}")
        assertTrue(at.y >= 0, "y=${at.y}")
    }
}
