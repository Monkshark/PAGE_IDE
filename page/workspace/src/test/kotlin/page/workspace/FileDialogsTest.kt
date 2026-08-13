package page.workspace

import java.awt.GraphicsEnvironment
import kotlin.test.Test
import kotlin.test.assertSame
import kotlin.test.assertTrue

class FileDialogsTest {

    private fun chooser(): Any? {
        val method = FileDialogs::class.java.getDeclaredMethod("chooser")
        method.isAccessible = true
        return method.invoke(FileDialogs)
    }

    @Test
    fun `the chooser is built once and reused`() {
        if (GraphicsEnvironment.isHeadless()) return
        val first = chooser()
        val second = chooser()
        assertSame(first, second, "a second dialog must not pay the construction cost again")
    }

    @Test
    fun `warming up leaves a chooser ready`() {
        if (GraphicsEnvironment.isHeadless()) return
        FileDialogs.warmUp()
        assertTrue(chooser() is javax.swing.JFileChooser)
    }
}
