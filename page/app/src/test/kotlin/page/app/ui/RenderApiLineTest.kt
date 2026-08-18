package page.app.ui

import java.awt.GraphicsEnvironment
import javax.swing.JFrame
import javax.swing.JPanel
import kotlin.test.Test
import kotlin.test.assertTrue

class RenderApiLineTest {

    private class FakeLayer(private val api: String) : JPanel() {
        @Suppress("unused")
        fun getRenderApi(): String = api
    }

    private fun withProperty(value: String?, block: () -> Unit) {
        val previous = System.getProperty("skiko.renderApi")
        if (value == null) System.clearProperty("skiko.renderApi") else System.setProperty("skiko.renderApi", value)
        try {
            block()
        } finally {
            if (previous == null) System.clearProperty("skiko.renderApi") else System.setProperty("skiko.renderApi", previous)
        }
    }

    @Test
    fun `a backend that matches the request reads as one value`() {
        if (GraphicsEnvironment.isHeadless()) return
        withProperty("DIRECT3D") {
            val frame = JFrame()
            frame.contentPane.add(FakeLayer("DIRECT3D"))
            val line = renderApiLine(frame)
            frame.dispose()
            assertTrue(line.contains("DIRECT3D"), line)
            assertTrue(!line.contains("asked for"), "no fallback happened, so nothing to explain: $line")
        }
    }

    @Test
    fun `a fallback names both what runs and what was asked for`() {
        if (GraphicsEnvironment.isHeadless()) return
        withProperty("DIRECT3D") {
            val frame = JFrame()
            frame.contentPane.add(FakeLayer("SOFTWARE_FAST"))
            val line = renderApiLine(frame)
            frame.dispose()
            assertTrue(line.contains("SOFTWARE_FAST"), line)
            assertTrue(line.contains("asked for DIRECT3D"), line)
        }
    }

    @Test
    fun `a layer nested deeper is still found`() {
        if (GraphicsEnvironment.isHeadless()) return
        withProperty("OPENGL") {
            val frame = JFrame()
            val outer = JPanel()
            val inner = JPanel()
            inner.add(FakeLayer("OPENGL"))
            outer.add(inner)
            frame.contentPane.add(outer)
            val line = renderApiLine(frame)
            frame.dispose()
            assertTrue(line.contains("OPENGL"), line)
        }
    }

    @Test
    fun `no window means no claim about the backend`() {
        withProperty(null) {
            assertTrue(renderApiLine(null).contains("unknown"))
        }
    }
}
