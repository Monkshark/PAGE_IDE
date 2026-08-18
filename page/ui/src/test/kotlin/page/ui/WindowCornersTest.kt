package page.ui

import java.awt.GraphicsEnvironment
import javax.swing.JWindow
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WindowCornersTest {

    @Test
    fun `a window that was never shown is left alone`() {
        if (GraphicsEnvironment.isHeadless()) return
        val window = JWindow()
        assertFalse(window.isDisplayable, "a fresh window has no peer yet")
        assertFalse(WindowCorners.round(window), "nothing to round before the peer exists")
    }

    @Test
    fun `rounding a shown window succeeds on windows and is refused elsewhere`() {
        if (GraphicsEnvironment.isHeadless()) return
        val window = JWindow()
        window.pack()
        try {
            val rounded = WindowCorners.round(window)
            if (Platform.isWindows) {
                assertTrue(rounded, "Windows 11 should accept the corner preference")
                assertTrue(WindowCorners.reset(window), "resetting should be accepted too")
            } else {
                assertFalse(rounded, "no dwmapi outside Windows")
            }
        } finally {
            window.dispose()
        }
    }
}
