package page.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals

class EditorInsetsTest {

    private val padding = PaddingValues(start = 8.dp, end = 20.dp, top = 16.dp, bottom = 16.dp)
    private val ltr = LayoutDirection.Ltr

    @Test
    fun `the room after the longest line belongs to the click surface`() {
        val insets = editorInsets(padding, ltr)
        assertEquals(20.dp, insets.trailingSlack)
        assertEquals(0.dp, insets.outer.calculateEndPadding(ltr))
    }

    @Test
    fun `the room under the last line belongs to the click surface`() {
        val insets = editorInsets(padding, ltr)
        assertEquals(16.dp, insets.bottomSlack)
        assertEquals(0.dp, insets.outer.calculateBottomPadding())
    }

    @Test
    fun `the leading gutter stays outside so text does not touch the edge`() {
        val insets = editorInsets(padding, ltr)
        assertEquals(8.dp, insets.outer.calculateStartPadding(ltr))
        assertEquals(16.dp, insets.outer.calculateTopPadding())
    }

    @Test
    fun `the editor is the same size as before, only the surface reaches further`() {
        val insets = editorInsets(padding, ltr)
        assertEquals(
            padding.calculateStartPadding(ltr) + padding.calculateEndPadding(ltr),
            insets.outer.calculateStartPadding(ltr) + insets.trailingSlack,
        )
        assertEquals(
            padding.calculateTopPadding() + padding.calculateBottomPadding(),
            insets.outer.calculateTopPadding() + insets.bottomSlack,
        )
    }

    @Test
    fun `an editor with no padding asks for no slack`() {
        val insets = editorInsets(PaddingValues(0.dp), ltr)
        assertEquals(0.dp, insets.trailingSlack)
        assertEquals(0.dp, insets.bottomSlack)
    }

    @Test
    fun `a click far right of the longest line still lands on the surface`() {
        val insets = editorInsets(padding, ltr)
        assertEquals(592.dp, clickSurfaceWidth(300.dp, insets, viewportWidth = 600.dp, direction = ltr))
    }

    @Test
    fun `text wider than the window keeps its own width so nothing is cut off`() {
        val insets = editorInsets(padding, ltr)
        assertEquals(920.dp, clickSurfaceWidth(900.dp, insets, viewportWidth = 600.dp, direction = ltr))
    }

    @Test
    fun `an editor that has not been measured yet falls back to the text width`() {
        val insets = editorInsets(padding, ltr)
        assertEquals(320.dp, clickSurfaceWidth(300.dp, insets, viewportWidth = 0.dp, direction = ltr))
    }
}
