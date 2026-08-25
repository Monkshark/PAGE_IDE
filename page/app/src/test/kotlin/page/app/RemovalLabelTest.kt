package page.app

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RemovalLabelTest {

    @Test
    fun `before any file is gone the label says what it is busy with`() {
        assertEquals("Stopping…", removalLabel("Stopping", 0f))
        assertEquals("Removing…", removalLabel("Removing", 0f))
    }

    @Test
    fun `once files start going the label carries the count`() {
        assertEquals("Removing… 42%", removalLabel("Removing", 0.42f))
        assertEquals("Removing… 100%", removalLabel("Removing", 1f))
    }

    @Test
    fun `a phase that was never set still reads as a removal`() {
        assertEquals("Removing…", removalLabel(null, 0f))
    }

    @Test
    fun `a fraction outside its range does not print a nonsense percentage`() {
        assertEquals("Removing… 100%", removalLabel("Removing", 4f))
        assertEquals("Removing…", removalLabel("Removing", -1f))
    }

    @Test
    fun `waiting on the server moves the bar instead of sitting at zero`() {
        assertEquals(0f, stopFraction(0, 8_000))
        assertEquals(STOP_SHARE / 2, stopFraction(4_000, 8_000))
        assertEquals(STOP_SHARE, stopFraction(8_000, 8_000))
    }

    @Test
    fun `waiting past the deadline never eats into the sweep`() {
        assertEquals(STOP_SHARE, stopFraction(60_000, 8_000))
        assertEquals(STOP_SHARE, stopFraction(1, 0))
    }

    @Test
    fun `the sweep picks up where the wait left off`() {
        assertEquals(STOP_SHARE, sweepFraction(0, 100, afterStop = true))
        assertEquals(1f, sweepFraction(100, 100, afterStop = true))
        assertTrue(sweepFraction(50, 100, afterStop = true) > STOP_SHARE)
    }

    @Test
    fun `a removal with no server to stop uses the whole bar`() {
        assertEquals(0f, sweepFraction(0, 100, afterStop = false))
        assertEquals(0.5f, sweepFraction(50, 100, afterStop = false))
        assertEquals(1f, sweepFraction(100, 100, afterStop = false))
    }

    @Test
    fun `a sweep that cannot count its files does not jump ahead`() {
        assertEquals(STOP_SHARE, sweepFraction(0, 0, afterStop = true))
        assertEquals(0f, sweepFraction(0, 0, afterStop = false))
    }

    @Test
    fun `the bar only ever moves forward across both phases`() {
        val readings = listOf(
            stopFraction(0, 8_000),
            stopFraction(4_000, 8_000),
            stopFraction(8_000, 8_000),
            sweepFraction(0, 100, afterStop = true),
            sweepFraction(50, 100, afterStop = true),
            sweepFraction(100, 100, afterStop = true),
        )
        assertEquals(readings.sorted(), readings)
    }
}
