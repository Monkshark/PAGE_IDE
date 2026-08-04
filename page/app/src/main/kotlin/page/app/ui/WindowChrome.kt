package page.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.ui.geometry.Size
import page.ui.Glass
import java.awt.Frame
import java.awt.MouseInfo
import java.awt.Window
import kotlin.math.abs

data class WindowChrome(
    val window: Window,
    val isMaximized: Boolean,
    val onMinimize: () -> Unit,
    val onToggleMaximize: () -> Unit,
    val onClose: () -> Unit,
)

val LocalWindowChrome = compositionLocalOf<WindowChrome?> { null }

@Composable
internal fun WindowControls(chrome: WindowChrome, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxHeight()) {
        WindowButton(onClick = chrome.onMinimize) { color -> drawMinimize(color) }
        WindowButton(onClick = chrome.onToggleMaximize) { color ->
            if (chrome.isMaximized) drawRestore(color) else drawMaximize(color)
        }
        WindowButton(onClick = chrome.onClose, danger = true) { color -> drawClose(color) }
    }
}

@Composable
private fun WindowButton(
    onClick: () -> Unit,
    danger: Boolean = false,
    glyph: androidx.compose.ui.graphics.drawscope.DrawScope.(Color) -> Unit,
) {
    val colors = Glass.colors
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val bg = when {
        hovered && danger -> Color(0xFFE81123)
        hovered -> colors.surfaceL3
        else -> Color.Transparent
    }
    val fg = when {
        hovered && danger -> Color.White
        hovered -> colors.text
        else -> colors.muted
    }
    Box(
        modifier = Modifier
            .width(50.dp)
            .fillMaxHeight()
            .background(bg)
            .hoverable(interaction)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.width(15.dp).height(15.dp)) { glyph(fg) }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawMinimize(color: Color) {
    val y = size.height / 2f
    drawLine(color, Offset(size.width * 0.16f, y), Offset(size.width * 0.84f, y), 1.4f, StrokeCap.Round)
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawMaximize(color: Color) {
    val s = size.minDimension
    val inset = s * 0.16f
    drawRoundRect(
        color = color,
        topLeft = Offset(inset, inset),
        size = Size(s - inset * 2, s - inset * 2),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(s * 0.1f, s * 0.1f),
        style = Stroke(width = 1.4f),
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawRestore(color: Color) {
    val s = size.minDimension
    val sw = 1.4f
    val cr = androidx.compose.ui.geometry.CornerRadius(s * 0.1f, s * 0.1f)
    drawRoundRect(
        color = color,
        topLeft = Offset(s * 0.16f, s * 0.30f),
        size = Size(s * 0.50f, s * 0.50f),
        cornerRadius = cr,
        style = Stroke(width = sw),
    )
    val path = androidx.compose.ui.graphics.Path().apply {
        moveTo(s * 0.32f, s * 0.30f)
        lineTo(s * 0.32f, s * 0.20f)
        lineTo(s * 0.80f, s * 0.20f)
        lineTo(s * 0.80f, s * 0.68f)
        lineTo(s * 0.66f, s * 0.68f)
    }
    drawPath(path, color, style = Stroke(width = sw, cap = StrokeCap.Round))
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawClose(color: Color) {
    val s = size.minDimension
    val a = s * 0.20f
    val b = s * 0.80f
    drawLine(color, Offset(a, a), Offset(b, b), 1.4f, StrokeCap.Round)
    drawLine(color, Offset(b, a), Offset(a, b), 1.4f, StrokeCap.Round)
}

fun Modifier.titleBarDrag(window: Window, onToggleMaximize: () -> Unit): Modifier = composed {
    var lastClickAt by remember { mutableStateOf(0L) }
    pointerInput(window) {
        awaitEachGesture {
            val down = awaitFirstDown()
            var startScreen = MouseInfo.getPointerInfo()?.location
            val frame = window as? Frame
            var maximized = frame != null && (frame.extendedState and Frame.MAXIMIZED_BOTH) != 0
            var baseWin = window.location
            var moved = false
            drag(down.id) { _ ->
                val cur = MouseInfo.getPointerInfo()?.location ?: return@drag
                val s = startScreen ?: run { startScreen = cur; cur }
                if (!moved && (abs(cur.x - s.x) > 3 || abs(cur.y - s.y) > 3)) moved = true
                if (!moved) return@drag
                if (maximized && frame != null) {
                    val fraction = if (window.width > 0) (s.x - window.x).toFloat() / window.width else 0.5f
                    frame.extendedState = Frame.NORMAL
                    val restoredWidth = window.width
                    window.setLocation((cur.x - fraction * restoredWidth).toInt(), cur.y - 12)
                    baseWin = window.location
                    startScreen = cur
                    maximized = false
                } else {
                    window.setLocation(baseWin.x + (cur.x - s.x), baseWin.y + (cur.y - s.y))
                }
            }
            if (!moved) {
                val now = System.currentTimeMillis()
                if (now - lastClickAt < 400L) {
                    onToggleMaximize()
                    lastClickAt = 0L
                } else {
                    lastClickAt = now
                }
            }
        }
    }
}
