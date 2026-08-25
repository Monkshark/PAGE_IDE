package page.lsp

import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ProcessTransportCloseTest {

    object Sleeper {
        @JvmStatic
        fun main(args: Array<String>) {
            Thread.sleep(600_000)
        }
    }

    private fun spawnSleeper(): Process {
        val java = File(System.getProperty("java.home"), "bin/java").absolutePath
        return ProcessBuilder(java, "-cp", System.getProperty("java.class.path"), Sleeper::class.java.name)
            .start()
    }

    @Test
    fun `closing a transport whose output nobody writes does not wait on the pipe`() {
        val process = spawnSleeper()
        val transport = ProcessTransport(process) { }
        val reading = CountDownLatch(1)
        val reader = Thread({
            reading.countDown()
            runCatching { transport.input.read() }
        }, "blocked-reader").apply {
            isDaemon = true
            start()
        }
        assertTrue(reading.await(10, TimeUnit.SECONDS), "the reader never got going")
        Thread.sleep(300)

        val startedAt = System.currentTimeMillis()
        transport.close()
        val tookMs = System.currentTimeMillis() - startedAt

        assertTrue(tookMs < 15_000, "close blocked for ${tookMs}ms with a reader parked on the pipe")
        assertTrue(process.waitFor(10, TimeUnit.SECONDS), "the server was left running")
        assertFalse(process.isAlive)
        reader.interrupt()
    }

    @Test
    fun `the server is gone before the streams are touched`() {
        val process = spawnSleeper()
        val transport = ProcessTransport(process) { }
        assertTrue(process.isAlive)

        transport.close()

        assertTrue(process.waitFor(10, TimeUnit.SECONDS))
        assertFalse(process.isAlive, "close must leave no server behind")
    }

    @Test
    fun `closing twice is not an error`() {
        val process = spawnSleeper()
        val transport = ProcessTransport(process) { }
        transport.close()
        transport.close()
        assertFalse(process.isAlive)
    }
}
