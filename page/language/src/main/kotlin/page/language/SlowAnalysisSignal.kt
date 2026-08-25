package page.language

import java.util.concurrent.atomic.AtomicLong

internal class SlowAnalysisSignal {

    private val issued = AtomicLong(0)
    private val waiting = AtomicLong(0)

    fun requested(): Long {
        val token = issued.incrementAndGet()
        waiting.set(token)
        return token
    }

    fun settled() {
        waiting.set(0)
    }

    fun stillWaiting(token: Long): Boolean = token != 0L && waiting.get() == token
}
