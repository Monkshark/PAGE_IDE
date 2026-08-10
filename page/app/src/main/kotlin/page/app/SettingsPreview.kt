package page.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import page.shared.syntax.BracketScan
import page.shared.syntax.KotlinLexer
import page.shared.syntax.SyntaxPreset
import page.shared.syntax.UnusedSymbols
import page.shared.syntax.colorizeCode
import page.ui.Glass

private const val CARET_WORD = "ext"

private val PREVIEW_CODE = """
import java.nio.file.Files

object FileIcons {
    fun resourceFor(path: Path): String? {
        val ext = path.name.substringAfterLast('.')
        return EXT[ext]?.let { "fileicons/${'$'}it.svg" }
    }
}
""".trimIndent()

internal val PREVIEW_HEIGHT = 150.dp

@Composable
internal fun SettingsPreview(editor: EditorOptions, modifier: Modifier = Modifier) {
    val colors = Glass.colors
    val palette = colors.syntax
    val code = PREVIEW_CODE
    val styled = remember(editor, palette) { buildPreview(code, editor, palette, colors.faint, colors.primarySoft) }
    val lines = remember(code) { lineRanges(code) }
    val caretLine = remember(code) { code.take(code.indexOf("val $CARET_WORD")).count { it == '\n' } }
    val fontSize = editor.fontSize.sp
    val lineHeight = (editor.fontSize * 1.6f).sp

    Column(modifier = modifier.background(colors.background)) {
        Row(
            modifier = Modifier.fillMaxWidth().height(24.dp).padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "PREVIEW",
                color = colors.faint,
                fontSize = Glass.type.label,
                fontWeight = FontWeight.Medium,
                letterSpacing = 1.4.sp,
            )
            Box(Modifier.weight(1f))
            Text(
                text = "${editor.syntaxPreset.label} · ${editor.fontSize}sp",
                color = colors.faint,
                fontSize = Glass.type.label,
                fontFamily = FontFamily.Monospace,
            )
        }
        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(colors.separator))
        Row(modifier = Modifier.fillMaxWidth().weight(1f)) {
            Column(modifier = Modifier.weight(1f).padding(vertical = 6.dp)) {
                for ((index, range) in lines.withIndex()) {
                    val onCaretLine = editor.highlightCurrentLine && index == caretLine
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(if (onCaretLine) colors.surfaceL2 else Color.Transparent),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (editor.showLineNumbers) {
                            Text(
                                text = "${index + 1}",
                                color = colors.faint,
                                fontSize = fontSize,
                                lineHeight = lineHeight,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.width(38.dp).padding(end = 10.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.End,
                            )
                        } else {
                            Box(Modifier.width(12.dp))
                        }
                        ScopeGuideBars(
                            depth = indentDepth(code, range, editor.tabSize),
                            editor = editor,
                            fontSize = editor.fontSize,
                        )
                        Text(
                            text = styled.subSequence(range.first, range.last + 1),
                            fontSize = fontSize,
                            lineHeight = lineHeight,
                            fontFamily = FontFamily.Monospace,
                            color = colors.text,
                            maxLines = 1,
                        )
                    }
                }
            }
            if (editor.showMinimap) {
                Box(
                    modifier = Modifier
                        .width(26.dp)
                        .fillMaxHeight()
                        .background(colors.surfaceL1),
                ) {
                    Column(modifier = Modifier.padding(4.dp)) {
                        for (range in lines) {
                            val width = ((range.last - range.first).coerceAtMost(30)) * 0.6f
                            Box(
                                modifier = Modifier
                                    .padding(bottom = 2.dp)
                                    .width(width.dp)
                                    .height(1.dp)
                                    .background(colors.faint),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ScopeGuideBars(depth: Int, editor: EditorOptions, fontSize: Int) {
    if (editor.scopeGuides == ScopeGuideMode.OFF || depth <= 0) return
    val colors = Glass.colors
    val depthColors = colors.syntax.bracketDepths
    val step = (fontSize * 0.6f * editor.tabSize).dp
    Row {
        repeat(depth) { level ->
            val tint = if (editor.rainbowBrackets && depthColors.isNotEmpty()) {
                depthColors[level % depthColors.size].copy(alpha = 0.4f)
            } else {
                colors.outline
            }
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height((fontSize * 1.6f).dp)
                    .background(tint),
            )
            Box(Modifier.width(step - 1.dp))
        }
    }
}

private fun buildPreview(
    code: String,
    editor: EditorOptions,
    palette: page.shared.syntax.SyntaxPalette,
    faint: Color,
    occurrenceTint: Color,
): AnnotatedString {
    val preset = editor.syntaxPreset
    val base = colorizeCode(code, KotlinLexer, palette, preset)
    val tokens = KotlinLexer.tokenize(code)
    val pairs = BracketScan.pairs(code, tokens)
    return buildAnnotatedString {
        append(base)
        if (editor.rainbowBrackets && palette.bracketDepths.isNotEmpty()) {
            for (pair in pairs) {
                val tint = palette.bracketDepths[pair.depth.mod(palette.bracketDepths.size)]
                addStyle(SpanStyle(color = tint), pair.open, pair.open + 1)
                addStyle(SpanStyle(color = tint), pair.close, pair.close + 1)
            }
        }
        if (editor.dimUnusedSymbols) {
            for (range in UnusedSymbols.find(code, tokens, pairs)) {
                addStyle(SpanStyle(color = faint), range.first, (range.last + 1).coerceAtMost(code.length))
            }
        }
        if (editor.highlightIdentifierUnderCaret) {
            for (range in wordOccurrences(code, CARET_WORD)) {
                addStyle(SpanStyle(background = occurrenceTint), range.first, range.last + 1)
            }
        }
    }
}

private fun wordOccurrences(code: String, word: String): List<IntRange> {
    val out = ArrayList<IntRange>()
    var from = 0
    while (true) {
        val at = code.indexOf(word, from)
        if (at < 0) break
        val before = code.getOrNull(at - 1)
        val after = code.getOrNull(at + word.length)
        val bounded = (before == null || !before.isLetterOrDigit() && before != '_') &&
            (after == null || !after.isLetterOrDigit() && after != '_')
        if (bounded) out += at until (at + word.length)
        from = at + word.length
    }
    return out
}

private fun lineRanges(code: String): List<IntRange> {
    val out = ArrayList<IntRange>()
    var start = 0
    for (i in code.indices) {
        if (code[i] == '\n') {
            out += start until i
            start = i + 1
        }
    }
    out += start until code.length
    return out
}

private fun indentDepth(code: String, range: IntRange, tabSize: Int): Int {
    if (range.isEmpty()) return 0
    var spaces = 0
    var i = range.first
    while (i <= range.last && code[i] == ' ') {
        spaces++
        i++
    }
    if (i > range.last) return 0
    return spaces / tabSize.coerceAtLeast(1)
}
