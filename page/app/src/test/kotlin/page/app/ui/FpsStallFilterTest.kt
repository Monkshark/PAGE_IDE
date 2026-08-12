package page.app.ui

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FpsStallFilterTest {

    private fun frame(cls: String, method: String) = StackTraceElement(cls, method, null, -1)

    @Test
    fun `an event thread parked for input is not a stall`() {
        val stack = arrayOf(
            frame("jdk.internal.misc.Unsafe", "park"),
            frame("java.util.concurrent.locks.LockSupport", "park"),
            frame("java.awt.EventQueue", "getNextEvent"),
            frame("java.awt.EventDispatchThread", "pumpOneEventForFilters"),
        )
        assertTrue(isWaitingForEvents(stack))
    }

    @Test
    fun `work on the event thread is a stall`() {
        val stack = arrayOf(
            frame("java.lang.Thread", "sleep"),
            frame("page.language.LspController", "runWithClientDown"),
            frame("page.app.ui.LayoutKt", "IdeMainLayout"),
            frame("java.awt.EventDispatchThread", "run"),
        )
        assertFalse(isWaitingForEvents(stack))
    }

    @Test
    fun `a deep unrelated getNextEvent does not excuse the stall`() {
        val deep = Array(12) { frame("page.app.Busy", "work$it") } +
            arrayOf(frame("java.awt.EventQueue", "getNextEvent"))
        assertFalse(isWaitingForEvents(deep))
    }

    @Test
    fun `an empty stack is not a stall claim`() {
        assertFalse(isWaitingForEvents(emptyArray()))
    }
}
