package page.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.unit.IntSize
import kotlin.math.ceil

internal class LineMetrics private constructor(
    private val starts: IntArray,
    private val ends: IntArray,
) {
    val lineCount: Int get() = starts.size

    fun lineStart(line: Int): Int = starts[line.coerceIn(0, starts.size - 1)]

    fun lineEnd(line: Int): Int = ends[line.coerceIn(0, ends.size - 1)]

    fun lineLength(line: Int): Int {
        val i = line.coerceIn(0, starts.size - 1)
        return ends[i] - starts[i]
    }

    fun lineForOffset(offset: Int): Int {
        var lo = 0
        var hi = starts.size - 1
        while (lo < hi) {
            val mid = (lo + hi + 1) ushr 1
            if (starts[mid] <= offset) lo = mid else hi = mid - 1
        }
        return lo
    }

    fun columnIn(line: Int, offset: Int): Int =
        (offset - lineStart(line)).coerceIn(0, lineLength(line))

    companion object {
        fun of(text: CharSequence): LineMetrics {
            val starts = ArrayList<Int>()
            val ends = ArrayList<Int>()
            var lineStart = 0
            var i = 0
            val n = text.length
            while (i < n) {
                if (text[i] == '\n') {
                    starts.add(lineStart)
                    ends.add(i)
                    lineStart = i + 1
                }
                i++
            }
            starts.add(lineStart)
            ends.add(n)
            return LineMetrics(starts.toIntArray(), ends.toIntArray())
        }
    }
}

internal class LineLayout(
    val text: AnnotatedString,
    private val metrics: LineMetrics,
    private val measurer: TextMeasurer,
    private val style: TextStyle,
    private val cache: MutableMap<AnnotatedString, TextLayoutResult>,
    val lineHeightPx: Float,
    private val estimatedWidthPx: Int,
) {
    private var measuredWidthPx: Int = 0

    private val byLine = HashMap<Int, TextLayoutResult>()

    private val lines = object {
        operator fun get(line: Int): TextLayoutResult {
            byLine[line]?.let { return it }
            val slice = text.subSequence(metrics.lineStart(line), metrics.lineEnd(line))
            val measured = cache.getOrPut(slice) {
                measurer.measure(text = slice, style = style, softWrap = false)
            }
            byLine[line] = measured
            if (measured.size.width > measuredWidthPx) measuredWidthPx = measured.size.width
            return measured
        }
    }

    val lineCount: Int get() = metrics.lineCount

    val columnWidthPx: Float by lazy {
        measurer.measure(AnnotatedString("0"), style = style, softWrap = false).size.width.toFloat()
    }

    private var scopeSpanCache: Pair<List<page.shared.syntax.BracketPair>, List<ScopeSpan>>? = null

    internal fun scopeSpans(
        pairs: List<page.shared.syntax.BracketPair>,
        mapOffset: (Int) -> Int,
    ): List<ScopeSpan> {
        scopeSpanCache?.let { (cached, spans) -> if (cached === pairs) return spans }
        val spans = scopeSpansFor(text, pairs, mapOffset)
        scopeSpanCache = pairs to spans
        return spans
    }

    val size: IntSize
        get() = IntSize(
            maxOf(estimatedWidthPx, measuredWidthPx),
            ceil(lineHeightPx * lineCount).toInt(),
        )

    fun getLineForOffset(offset: Int): Int = metrics.lineForOffset(offset)

    fun getLineStart(line: Int): Int = metrics.lineStart(line)

    fun getLineEnd(line: Int, visibleEnd: Boolean = false): Int = metrics.lineEnd(line)

    fun getLineTop(line: Int): Float = line.coerceIn(0, lineCount - 1) * lineHeightPx

    fun getLineBottom(line: Int): Float = (line.coerceIn(0, lineCount - 1) + 1) * lineHeightPx

    fun getCursorRect(offset: Int): Rect {
        val line = metrics.lineForOffset(offset)
        val col = metrics.columnIn(line, offset)
        return lines[line].getCursorRect(col).translate(0f, line * lineHeightPx)
    }

    fun getOffsetForPosition(position: Offset): Int {
        if (lineCount == 0) return 0
        val line = (position.y / lineHeightPx).toInt().coerceIn(0, lineCount - 1)
        val localY = (position.y - line * lineHeightPx).coerceIn(0f, lineHeightPx - 1f)
        val col = lines[line].getOffsetForPosition(Offset(position.x, localY))
        return metrics.lineStart(line) + col
    }

    fun getSelectionPath(start: Int, end: Int): Path {
        val path = Path()
        if (start >= end) return path
        val startLine = metrics.lineForOffset(start)
        val endLine = metrics.lineForOffset(end)
        val sliver = lineHeightPx * 0.4f
        for (line in startLine..endLine) {
            val tlr = lines[line]
            val lineRight = tlr.size.width.toFloat()
            val left = if (line == startLine) {
                tlr.getCursorRect(metrics.columnIn(line, start)).left
            } else {
                0f
            }
            var right = if (line == endLine) {
                tlr.getCursorRect(metrics.columnIn(line, end)).left
            } else {
                lineRight
            }
            if (line != endLine && right <= left) right = left + sliver
            val top = line * lineHeightPx
            if (right > left) {
                path.addRect(Rect(left, top, right, top + lineHeightPx))
            }
        }
        return path
    }

    fun draw(scope: DrawScope, firstLine: Int = 0, lastLine: Int = lineCount - 1) {
        if (lineCount == 0) return
        val lo = firstLine.coerceIn(0, lineCount - 1)
        val hi = lastLine.coerceIn(lo, lineCount - 1)
        for (line in lo..hi) {
            scope.drawText(lines[line], topLeft = Offset(0f, line * lineHeightPx))
        }
    }
}

internal class LineLayoutCache(private val measurer: TextMeasurer) {
    private var cache: HashMap<AnnotatedString, TextLayoutResult> = HashMap()
    private var lastStyle: TextStyle? = null

    fun layout(text: AnnotatedString, style: TextStyle): LineLayout {
        if (style != lastStyle) {
            cache = HashMap()
            lastStyle = style
        }
        if (cache.size > MAX_CACHED_LINES) cache = HashMap()
        val metrics = LineMetrics.of(text)
        val probe = measurer.measure(AnnotatedString("0"), style = style, softWrap = false)
        val advance = probe.size.width.toFloat()
        val estimatedWidth = ceil(advance * widestLineUnits(text, metrics)).toInt()
        return LineLayout(
            text = text,
            metrics = metrics,
            measurer = measurer,
            style = style,
            cache = cache,
            lineHeightPx = probe.getLineBottom(0),
            estimatedWidthPx = estimatedWidth,
        )
    }

    private fun widestLineUnits(text: CharSequence, metrics: LineMetrics): Int {
        var widest = 0
        for (line in 0 until metrics.lineCount) {
            val start = metrics.lineStart(line)
            val end = metrics.lineEnd(line)
            var units = 0
            for (i in start until end) units += if (isWideChar(text[i])) 2 else 1
            if (units > widest) widest = units
        }
        return widest
    }

    private fun isWideChar(c: Char): Boolean = c.code >= 0x1100 && (
        c.code <= 0x115F ||
            c.code in 0x2E80..0xA4CF ||
            c.code in 0xAC00..0xD7A3 ||
            c.code in 0xF900..0xFAFF ||
            c.code in 0xFE30..0xFE6F ||
            c.code in 0xFF00..0xFF60 ||
            c.code in 0xFFE0..0xFFE6
        )

    private companion object {
        const val MAX_CACHED_LINES = 20_000
    }
}
