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
    text: CharSequence,
    pairs: List<page.shared.syntax.BracketPair>,
    mapOffset: (Int) -> Int = { it },
): List<ScopeSpan> {
    if (pairs.isEmpty() || text.isEmpty()) return emptyList()
    val lineStarts = ArrayList<Int>()
    lineStarts.add(0)
    for (i in text.indices) if (text[i] == '\n') lineStarts.add(i + 1)

    fun lineOf(offset: Int): Int {
        var lo = 0
        var hi = lineStarts.size - 1
        while (lo < hi) {
            val mid = (lo + hi + 1) ushr 1
            if (lineStarts[mid] <= offset) lo = mid else hi = mid - 1
        }
        return lo
    }

    val spans = LinkedHashMap<Triple<Int, Int, Int>, ScopeSpan>()
    for (pair in pairs) {
        val open = mapOffset(pair.open).coerceIn(0, text.length)
        val close = mapOffset(pair.close).coerceIn(open, text.length)
        val openLine = lineOf(open)
        val closeLine = lineOf(close)
        if (closeLine - openLine < 2) continue
        var column = 0
        var i = lineStarts[openLine]
        while (i < text.length && (text[i] == ' ' || text[i] == '\t')) {
            column++
            i++
        }
        if (column == 0) continue
        if (i >= text.length || text[i] == '\n') continue
        val key = Triple(column, openLine + 1, closeLine - 1)
        val existing = spans[key]
        if (existing == null || pair.depth < existing.depth) {
            spans[key] = ScopeSpan(column, openLine + 1, closeLine - 1, pair.depth)
        }
    }
    return spans.values.toList()
}

internal fun DrawScope.drawScopeGuides(
    guides: ScopeGuides,
    layout: LineLayout,
    pairs: List<page.shared.syntax.BracketPair>,
    activePair: page.shared.syntax.BracketPair?,
    mapOffset: (Int) -> Int,
    firstLine: Int,
    lastLine: Int,
) {
    val spans = layout.scopeSpans(pairs, mapOffset)
    if (spans.isEmpty()) return
    val charWidth = layout.columnWidthPx
    if (charWidth <= 0f) return
    val activeOpenLine = activePair?.let { layout.getLineForOffset(mapOffset(it.open)) }
    for (span in spans) {
        if (span.toLine < firstLine || span.fromLine > lastLine) continue
        val isActive = activeOpenLine != null &&
            activeOpenLine + 1 == span.fromLine &&
            activePair.depth == span.depth
        if (guides.onlyCurrentBlock && !isActive) continue
        val x = span.column * charWidth
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
