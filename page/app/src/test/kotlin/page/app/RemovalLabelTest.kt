package page.app

import kotlin.test.Test
import kotlin.test.assertEquals

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
}
