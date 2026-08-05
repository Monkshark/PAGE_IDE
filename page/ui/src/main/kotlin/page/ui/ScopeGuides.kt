package page.ui

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope

/**
 * Vertical rails that connect a block to its closing brace. Drawn from the indentation of the lines
 * themselves, so it works for any language the editor opens.
 */
@Immutable
data class ScopeGuides(
    val enabled: Boolean,
    val onlyCurrentBlock: Boolean,
    val color: Color,
    val activeColor: Color,
    val indentWidth: Int,
) {
    companion object {
        val None = ScopeGuides(
            enabled = false,
            onlyCurrentBlock = false,
            color = Color.Unspecified,
            activeColor = Color.Unspecified,
            indentWidth = 4,
        )
    }
}

internal data class ScopeSpan(val column: Int, val fromLine: Int, val toLine: Int)

internal fun scopeSpansFor(text: String, indentWidth: Int): List<ScopeSpan> {
    if (indentWidth <= 0) return emptyList()
    val indents = text.split('\n').map { line ->
        if (line.isBlank()) -1
        else {
            var n = 0
            for (c in line) {
                if (c == ' ') n++ else if (c == '\t') n += indentWidth else break
            }
            n
        }
    }
    if (indents.isEmpty()) return emptyList()
    val deepest = indents.max()
    if (deepest < indentWidth) return emptyList()
    val spans = ArrayList<ScopeSpan>()
    var column = indentWidth
    while (column <= deepest) {
        var start = -1
        for (i in 0..indents.size) {
            val indent = if (i < indents.size) indents[i] else Int.MIN_VALUE
            // A blank line inside a block keeps the rail going; one outside never starts it.
            val inside = indent >= column || (indent == -1 && start >= 0)
            if (inside && start < 0) start = i
            if (!inside && start >= 0) {
                var end = i - 1
                while (end > start && indents[end] == -1) end--
                spans += ScopeSpan(column, start, end)
                start = -1
            }
        }
        column += indentWidth
    }
    return spans
}

internal fun DrawScope.drawScopeGuides(
    guides: ScopeGuides,
    layout: LineLayout,
    text: CharSequence,
    caretLine: Int,
    firstLine: Int,
    lastLine: Int,
) {
    val spans = layout.scopeSpans(text.toString(), guides.indentWidth)
    if (spans.isEmpty()) return
    val active = spans.filter { caretLine in it.fromLine..it.toLine }.maxByOrNull { it.column }
    val charWidth = layout.columnWidthPx
    if (charWidth <= 0f) return
    val stroke = density
    for (span in spans) {
        if (span.toLine < firstLine || span.fromLine > lastLine) continue
        val isActive = active != null && span.column == active.column && span.fromLine == active.fromLine
        if (guides.onlyCurrentBlock && !isActive) continue
        val x = span.column * charWidth
        val top = layout.getLineTop(span.fromLine.coerceIn(0, layout.lineCount - 1))
        val bottom = layout.getLineBottom(span.toLine.coerceIn(0, layout.lineCount - 1))
        drawLine(
            color = if (isActive) guides.activeColor else guides.color,
            start = Offset(x, top),
            end = Offset(x, bottom),
            strokeWidth = stroke,
        )
    }
}
