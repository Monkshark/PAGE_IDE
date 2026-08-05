package page.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

val fpsOverlayEnabled: Boolean by lazy {
    System.getProperty("page.debug.fps")?.equals("true", ignoreCase = true) == true ||
        System.getenv("PAGE_DEBUG_FPS")?.equals("true", ignoreCase = true) == true
}

@Volatile private var lastTickMs: Long = 0L

private val stallWatchdog: Thread by lazy {
    Thread {
        var reportedAt = 0L
        while (true) {
            Thread.sleep(40)
            val tick = lastTickMs
            if (tick == 0L) continue
            val stalledMs = System.currentTimeMillis() - tick
            if (stalledMs > 200 && tick != reportedAt) {
                reportedAt = tick
                val ui = Thread.getAllStackTraces().entries
                    .firstOrNull { it.key.name.startsWith("AWT-EventQueue") }
                println("[fps] stall ${stalledMs}ms on ${ui?.key?.name}")
                ui?.value?.take(25)?.forEach { println("    at $it") }
            }
        }
    }.apply { isDaemon = true; name = "fps-stall-watchdog" }
}

private fun totalGcMillis(): Long =
    java.lang.management.ManagementFactory.getGarbageCollectorMXBeans()
        .sumOf { it.collectionTime.coerceAtLeast(0L) }

private fun usedHeapMb(): Long {
    val rt = Runtime.getRuntime()
    return (rt.totalMemory() - rt.freeMemory()) / (1024 * 1024)
}

@Composable
fun FpsOverlay(modifier: Modifier = Modifier) {
    if (!fpsOverlayEnabled) return
    var fps by remember { mutableStateOf(0) }
    var frames by remember { mutableStateOf(0) }
    var windowStart by remember { mutableStateOf(0L) }
    var worstMs by remember { mutableStateOf(0f) }
    var lastFrame by remember { mutableStateOf(0L) }
    var gcMs by remember { mutableStateOf(0L) }
    var gcAtWindowStart by remember { mutableStateOf(-1L) }
    LaunchedEffect(Unit) {
        runCatching { stallWatchdog.start() }
        while (true) {
            withFrameNanos { t ->
                lastTickMs = System.currentTimeMillis()
                if (lastFrame != 0L) {
                    val dtMs = (t - lastFrame) / 1_000_000f
                    if (dtMs > worstMs) worstMs = dtMs
                }
                lastFrame = t
                frames++
                if (windowStart == 0L) windowStart = t
                if (gcAtWindowStart < 0L) gcAtWindowStart = totalGcMillis()
                val elapsed = t - windowStart
                if (elapsed >= 500_000_000L) {
                    fps = (frames * 1_000_000_000.0 / elapsed).roundToInt()
                    val gcNow = totalGcMillis()
                    gcMs = gcNow - gcAtWindowStart
                    if (worstMs > 40f) {
                        println(
                            "[fps] dip ${fps}fps worst=${worstMs.roundToInt()}ms gc=${gcMs}ms " +
                                "heap=${usedHeapMb()}MB",
                        )
                    }
                    gcAtWindowStart = gcNow
                    frames = 0
                    windowStart = t
                    worstMs = 0f
                }
            }
        }
    }
    Text(
        text = "$fps fps · ${worstMs.roundToInt()}ms · gc ${gcMs}ms",
        color = Color(0xFF00E5FF),
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = 12.sp,
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xCC000000))
            .padding(horizontal = 8.dp, vertical = 4.dp),
    )
}
