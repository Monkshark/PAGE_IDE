package page.language

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SlowAnalysisSignalTest {

    @Test
    fun `an edit that has not been answered yet is still waiting`() {
        val signal = SlowAnalysisSignal()
        assertTrue(signal.stillWaiting(signal.requested()))
    }

    @Test
    fun `diagnostics arriving end the wait`() {
        val signal = SlowAnalysisSignal()
        val token = signal.requested()
        signal.settled()
        assertFalse(signal.stillWaiting(token))
    }

    @Test
    fun `a newer edit supersedes the one before it`() {
        val signal = SlowAnalysisSignal()
        val first = signal.requested()
        val second = signal.requested()
        assertFalse(signal.stillWaiting(first), "the older edit must not announce")
        assertTrue(signal.stillWaiting(second))
    }

    @Test
    fun `nothing is waiting before the first edit`() {
        val signal = SlowAnalysisSignal()
        assertFalse(signal.stillWaiting(0))
        assertFalse(signal.stillWaiting(1))
    }

    @Test
    fun `settling twice is harmless`() {
        val signal = SlowAnalysisSignal()
        val token = signal.requested()
        signal.settled()
        signal.settled()
        assertFalse(signal.stillWaiting(token))
    }
}
