package page.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.loadSvgPainter
import androidx.compose.ui.res.useResource
import androidx.compose.ui.unit.Density
import java.util.concurrent.ConcurrentHashMap

private val painterCache = ConcurrentHashMap<String, Painter>()

fun cachedSvgPainter(resourcePath: String, density: Density): Painter =
    painterCache.getOrPut("$resourcePath@${density.density}") {
        useResource(resourcePath) { loadSvgPainter(it, density) }
    }

@Composable
fun cachedSvgPainter(resourcePath: String): Painter =
    cachedSvgPainter(resourcePath, LocalDensity.current)
