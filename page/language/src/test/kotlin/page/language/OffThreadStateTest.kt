package page.language

import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.snapshots.Snapshot
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class OffThreadStateTest {

    @Test
    fun `holds what was written`() {
        val state = OffThreadState("idle")
        state.value = "ready"
        assertEquals("ready", state.value)
    }

    @Test
    fun `destructuring still works`() {
        val state = OffThreadState(1)
        val (value, setter) = state
        assertEquals(1, value)
        setter(9)
        assertEquals(9, state.value)
    }

    @Test
    fun `a write from another thread is visible and does not throw`() {
        val state = OffThreadState("idle")
        val failure = AtomicReference<Throwable?>(null)
        val pool = Executors.newSingleThreadExecutor()
        try {
            val done = CountDownLatch(1)
            pool.execute {
                runCatching { state.value = "ready" }.onFailure { failure.set(it) }
                done.countDown()
            }
            assertTrue(done.await(5, TimeUnit.SECONDS), "writer finished")
        } finally {
            pool.shutdownNow()
        }
        assertNull(failure.get(), "writing from a server thread must not throw")
        Snapshot.sendApplyNotifications()
        assertEquals("ready", state.value)
    }

    @Test
    fun `a map write from another thread lands`() {
        val map = mutableStateMapOf<String, Int>()
        val failure = AtomicReference<Throwable?>(null)
        val pool = Executors.newSingleThreadExecutor()
        try {
            val done = CountDownLatch(1)
            pool.execute {
                runCatching { offThread { map["diagnostics"] = 3 } }.onFailure { failure.set(it) }
                done.countDown()
            }
            assertTrue(done.await(5, TimeUnit.SECONDS), "writer finished")
        } finally {
            pool.shutdownNow()
        }
        assertNull(failure.get(), "mutating a snapshot map off-thread must not throw")
        Snapshot.sendApplyNotifications()
        assertEquals(3, map["diagnostics"])
    }
}
