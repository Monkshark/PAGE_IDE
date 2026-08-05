package page.ui

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope

@Immutable
data class ScopeGuides(
    val enabled: Boolean,
    val onlyCurrentBlock: Boolean,
    val rainbow: Boolean,
    val color: Color,
    val activeColor: Color,
    val depthColors: List<Color>,
) {
    fun colorAt(depth: Int, active: Boolean): Color = when {
        rainbow && depthColors.isNotEmpty() ->
            depthColors[depth.mod(depthColors.size)].copy(alpha = if (active) 0.55f else 0.28f)
        active -> activeColor
        else -> color
    }

    companion object {
        val None = ScopeGuides(
            enabled = false,
            onlyCurrentBlock = false,
            rainbow = false,
            color = Color.Unspecified,
            activeColor = Color.Unspecified,
            depthColors = emptyList(),
        )
    }
}

internal data class ScopeSpan(val column: Int, val fromLine: Int, val toLine: Int, val depth: Int)

internal fun scopeSpansFor(
    layout: LineLayout,
    pairs: List<page.shared.syntax.BracketPair>,
): List<ScopeSpan> {
    if (pairs.isEmpty()) return emptyList()
    val spans = ArrayList<ScopeSpan>(pairs.size)
    for (pair in pairs) {
        val openLine = layout.getLineForOffset(pair.open)
        val closeLine = layout.getLineForOffset(pair.close)
        if (closeLine - openLine < 2) continue
        val column = pair.open - layout.getLineStart(openLine)
        spans += ScopeSpan(column, openLine + 1, closeLine - 1, pair.depth)
    }
    return spans
}

internal fun DrawScope.drawScopeGuides(
    guides: ScopeGuides,
    layout: LineLayout,
    pairs: List<page.shared.syntax.BracketPair>,
    activePair: page.shared.syntax.BracketPair?,
    firstLine: Int,
    lastLine: Int,
) {
    val spans = layout.scopeSpans(pairs)
    if (spans.isEmpty()) return
    val charWidth = layout.columnWidthPx
    if (charWidth <= 0f) return
    val activeOpen = activePair?.open
    for (span in spans) {
        if (span.toLine < firstLine || span.fromLine > lastLine) continue
        val isActive = activeOpen != null &&
            layout.getLineForOffset(activeOpen) + 1 == span.fromLine &&
            activePair.depth == span.depth
        if (guides.onlyCurrentBlock && !isActive) continue
        val x = span.column * charWidth + charWidth / 2f
        val top = layout.getLineTop(span.fromLine.coerceIn(0, layout.lineCount - 1))
        val bottom = layout.getLineBottom(span.toLine.coerceIn(0, layout.lineCount - 1))
        drawLine(
            color = guides.colorAt(span.depth, isActive),
            start = Offset(x, top),
            end = Offset(x, bottom),
            strokeWidth = density,
        )
    }
}
