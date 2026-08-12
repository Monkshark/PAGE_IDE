package page.language

import androidx.compose.runtime.snapshots.Snapshot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The router builds controllers on whatever thread asked for one — prewarming does it on IO. A
 * controller owns Compose state, so it has to be built inside a snapshot for the composition to see
 * it. These build controllers directly: starting one would spawn a real language server.
 */
class LspRouterThreadTest {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private fun offThread(block: () -> Unit): Throwable? {
        val failure = AtomicReference<Throwable?>(null)
        val pool = Executors.newSingleThreadExecutor()
        try {
            val done = CountDownLatch(1)
            pool.execute {
                runCatching(block).onFailure { failure.set(it) }
                done.countDown()
            }
            assertTrue(done.await(10, TimeUnit.SECONDS), "worker finished")
        } finally {
            pool.shutdownNow()
        }
        return failure.get()
    }

    @Test
    fun `a controller built off the ui thread is readable afterwards`() {
        val built = AtomicReference<LspController?>(null)
        val failure = offThread {
            built.set(Snapshot.withMutableSnapshot { LspController(workspaceRoot = null, scope = scope) })
        }
        assertNull(failure, "building a controller off-thread must not throw")

        Snapshot.sendApplyNotifications()
        val controller = assertNotNull(built.get())
        assertEquals(LspController.Status.IDLE, controller.status.value)
        assertEquals(0, controller.diagnosticsByUri.size)
        assertEquals(0, controller.activities.size)
    }

    @Test
    fun `state written off-thread reaches a later reader`() {
        val controller = LspController(workspaceRoot = null, scope = scope)
        val failure = offThread {
            controller.startActivity("startup", "Starting…")
            controller.statusDetail.value = "warming up"
        }
        assertNull(failure, "writing controller state off-thread must not throw")

        Snapshot.sendApplyNotifications()
        assertEquals("warming up", controller.statusDetail.value)
        assertEquals("Starting…", controller.activities["startup"]?.label)
    }
}
