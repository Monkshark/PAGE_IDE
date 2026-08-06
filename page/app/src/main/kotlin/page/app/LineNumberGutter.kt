package page.app

import page.runtime.*
import page.workspace.*

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import page.lsp.DiagnosticSeverity
import page.ui.CompactDropdown
import page.ui.CompactMenuSlot
import page.ui.Glass
import kotlin.math.ceil
import kotlin.math.floor

internal data class MultiKeywordChoice(
    val commentRange: IntRange,
    val chosenKeyword: String,
    val keywords: List<String>,
    val chosenColor: Color,
    val keywordColors: Map<String, Color>,
)

internal data class GutterLine(
    val originalLine: Int,
    val foldable: Boolean,
    val folded: Boolean,
    val severity: DiagnosticSeverity? = null,
    val multiKeyword: MultiKeywordChoice? = null,
)

private val TopPadding = 16.dp
private val FoldColumnWidth = 20.dp
private val DotColumnWidth = 8.dp
private val KeywordColumnWidth = 14.dp
private val NumberEndPadding = 12.dp

@Composable
internal fun LineNumberGutter(
    lines: List<GutterLine>,
    currentOriginalLine: Int,
    onToggleFold: (Int) -> Unit,
    onPickKeyword: (commentRange: IntRange, oldKeyword: String, newKeyword: String) -> Unit,
    textStyle: TextStyle,
    viewportHeightProvider: () -> Float,
    scrollOffsetProvider: () -> Float,
    modifier: Modifier = Modifier,
) {
    val mutedColor = MaterialTheme.colorScheme.onSurfaceVariant
    val activeColor = MaterialTheme.colorScheme.onBackground
    val toggleColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
    val surfaceColor = MaterialTheme.colorScheme.surface
    val infoColor = MaterialTheme.colorScheme.primary
    val hintColor = MaterialTheme.colorScheme.tertiary
    val errorColor = Glass.colors.error
    val warnColor = Glass.colors.warn
    val measurer = rememberTextMeasurer(cacheSize = 512)
    val density = LocalDensity.current

    val digits = remember(lines.size) {
        (lines.lastOrNull()?.originalLine?.plus(1) ?: 1).toString().length
    }
    val numberColumnWidth = remember(digits, textStyle, density.density) {
        with(density) {
            measurer.measure(AnnotatedString("9".repeat(digits)), style = textStyle).size.width.toDp()
        }
    }
    val lineHeightPx = remember(textStyle, density.density, density.fontScale) {
        measurer.measure(AnnotatedString("0"), style = textStyle, softWrap = false).getLineBottom(0)
    }
    val numberMutedStyle = remember(textStyle, mutedColor) { textStyle.copy(color = mutedColor) }
    val numberActiveStyle = remember(textStyle, activeColor) { textStyle.copy(color = activeColor) }
    val foldOpenStyle = numberMutedStyle
    val foldClosedStyle = remember(textStyle, toggleColor) { textStyle.copy(color = toggleColor) }
    val glyphCache = remember(textStyle, density.density, mutedColor, activeColor, toggleColor) {
        HashMap<String, androidx.compose.ui.text.TextLayoutResult>()
    }
    val numberCache = remember(textStyle, density.density, mutedColor, activeColor) {
        HashMap<Int, androidx.compose.ui.text.TextLayoutResult>()
    }
    val gutterWidth = FoldColumnWidth + DotColumnWidth + KeywordColumnWidth +
        numberColumnWidth + NumberEndPadding

    val latestLines by rememberUpdatedState(lines)
    val latestToggleFold by rememberUpdatedState(onToggleFold)
    var keywordMenu by remember { mutableStateOf<GutterLine?>(null) }

    val contentHeight = with(density) {
        (TopPadding.toPx() * 2 + lineHeightPx * lines.size).toDp()
    }
    Box(
        modifier = modifier
            .background(surfaceColor)
            .width(gutterWidth)
            .height(contentHeight),
    ) {
        Canvas(
            modifier = Modifier
                .width(gutterWidth)
                .height(contentHeight)
                .pointerInput(Unit) {
                    detectTapGestures { pos ->
                        val lineH = lineHeightPx
                        if (lineH <= 0f) return@detectTapGestures
                        val index = floor((pos.y - TopPadding.toPx()) / lineH).toInt()
                        val entry = latestLines.getOrNull(index) ?: return@detectTapGestures
                        val foldEnd = FoldColumnWidth.toPx()
                        val keywordStart = foldEnd + DotColumnWidth.toPx()
                        val keywordEnd = keywordStart + KeywordColumnWidth.toPx()
                        when {
                            pos.x <= foldEnd && entry.foldable -> latestToggleFold(entry.originalLine)
                            pos.x in keywordStart..keywordEnd && entry.multiKeyword != null ->
                                keywordMenu = entry
                        }
                    }
                },
        ) {
            val lineH = lineHeightPx
            if (lineH <= 0f || lines.isEmpty()) return@Canvas
            val topPad = TopPadding.toPx()
            val scrollY = scrollOffsetProvider()
            val viewH = viewportHeightProvider().takeIf { it > 0f } ?: size.height
            val first = (floor((scrollY - topPad) / lineH).toInt() - 2).coerceAtLeast(0)
            val last = (ceil((scrollY - topPad + viewH) / lineH).toInt() + 2)
                .coerceAtMost(lines.size - 1)
            if (first > last) return@Canvas

            val foldEnd = FoldColumnWidth.toPx()
            val dotCenterX = foldEnd + DotColumnWidth.toPx() / 2f
            val keywordX = foldEnd + DotColumnWidth.toPx()
            val numberRight = size.width - NumberEndPadding.toPx()

            for (i in first..last) {
                val entry = lines[i]
                val top = topPad + i * lineH
                if (entry.foldable) {
                    val symbol = if (entry.folded) "▸" else "▾"
                    val measured = glyphCache.getOrPut("f:${entry.folded}") {
                        measurer.measure(
                            AnnotatedString(symbol),
                            style = if (entry.folded) foldClosedStyle else foldOpenStyle,
                        )
                    }
                    drawText(
                        measured,
                        topLeft = Offset(
                            (foldEnd - measured.size.width) / 2f,
                            top + (lineH - measured.size.height) / 2f,
                        ),
                    )
                }
                val severityColor = when (entry.severity) {
                    DiagnosticSeverity.ERROR -> errorColor
                    DiagnosticSeverity.WARNING -> warnColor
                    DiagnosticSeverity.INFO -> infoColor
                    DiagnosticSeverity.HINT -> hintColor
                    null -> null
                }
                if (severityColor != null) {
                    drawCircle(
                        color = severityColor,
                        radius = 3.dp.toPx(),
                        center = Offset(dotCenterX, top + lineH / 2f),
                    )
                }
                val keyword = entry.multiKeyword
                if (keyword != null) {
                    val letter = keyword.chosenKeyword.firstOrNull()?.uppercase() ?: "?"
                    val measured = glyphCache.getOrPut("k:$letter:${keyword.chosenColor.value}") {
                        measurer.measure(
                            AnnotatedString(letter),
                            style = textStyle.copy(color = keyword.chosenColor, fontWeight = FontWeight.Bold),
                        )
                    }
                    drawText(
                        measured,
                        topLeft = Offset(keywordX, top + (lineH - measured.size.height) / 2f),
                    )
                }
                val isCurrent = entry.originalLine == currentOriginalLine
                val cacheKey = if (isCurrent) -(entry.originalLine + 1) else entry.originalLine + 1
                val measured = numberCache.getOrPut(cacheKey) {
                    val label = (entry.originalLine + 1).toString()
                    measurer.measure(
                        AnnotatedString(label),
                        style = if (isCurrent) numberActiveStyle else numberMutedStyle,
                    )
                }
                drawText(
                    measured,
                    topLeft = Offset(
                        numberRight - measured.size.width,
                        top + (lineH - measured.size.height) / 2f,
                    ),
                )
            }
        }
        val menuEntry = keywordMenu
        val menuChoice = menuEntry?.multiKeyword
        if (menuEntry != null && menuChoice != null) {
            val index = lines.indexOfFirst { it.originalLine == menuEntry.originalLine }
            val yDp = with(density) {
                (TopPadding.toPx() + lineHeightPx * index.coerceAtLeast(0) - scrollOffsetProvider()).toDp()
            }
            Box(modifier = Modifier.offset(x = FoldColumnWidth + DotColumnWidth, y = yDp)) {
                CompactDropdown(
                    expanded = true,
                    onDismissRequest = { keywordMenu = null },
                    minWidth = 120.dp,
                ) {
                    for (kw in menuChoice.keywords) {
                        val swatch = menuChoice.keywordColors[kw] ?: mutedColor
                        val isActive = kw == menuChoice.chosenKeyword
                        CompactMenuSlot(
                            onClick = {
                                keywordMenu = null
                                onPickKeyword(menuChoice.commentRange, menuChoice.chosenKeyword, kw)
                            },
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Text(
                                    text = kw.first().uppercase(),
                                    color = swatch,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.width(10.dp),
                                )
                                Text(
                                    text = kw,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (isActive) swatch else MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
