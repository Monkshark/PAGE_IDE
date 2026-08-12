package page.language

import androidx.compose.runtime.snapshots.Snapshot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LspRouterThreadTest {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val root = createTempDirectory("page-router-")

    @AfterTest
    fun tearDown() {
        root.toFile().deleteRecursively()
    }

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
        val router = LspRouter(root, scope)
        val file = root.resolve("Main.kt").also { it.toFile().writeText("fun main() {}\n") }

        val built = AtomicReference<LspController?>(null)
        val failure = offThread { built.set(router.controllerFor(file)) }
        assertNull(failure, "building a controller off-thread must not throw")

        val controller = built.get()
        if (controller != null) {
            Snapshot.sendApplyNotifications()
            assertEquals(controller, router.existingControllerFor(file), "the router keeps the controller")
            controller.status.value
            controller.diagnosticsByUri.size
        }
    }

    @Test
    fun `asking twice hands back the same controller`() {
        val router = LspRouter(root, scope)
        val file = root.resolve("Main.kt").also { it.toFile().writeText("fun main() {}\n") }
        val first = router.controllerFor(file)
        val second = router.controllerFor(file)
        assertEquals(first, second)
    }
}
