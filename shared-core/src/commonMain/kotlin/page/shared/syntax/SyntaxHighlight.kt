package page.shared.syntax

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle

fun colorizeCode(
    code: String,
    lexer: SyntaxLexer,
    palette: SyntaxPalette,
    preset: SyntaxPreset = SyntaxPreset.CALM,
): AnnotatedString {
    val tokens = SyntaxRoles.refine(code, lexer.tokenize(code))
    return buildAnnotatedString {
        var cursor = 0
        for (t in tokens) {
            val start = t.start.coerceIn(0, code.length)
            val end = t.endExclusive.coerceIn(0, code.length)
            if (start < cursor || end <= start) continue
            if (start > cursor) append(code.substring(cursor, start))
            val span = SyntaxStyles.spanFor(t.kind, palette, preset)
            if (span != null) withStyle(span) { append(code.substring(start, end)) }
            else append(code.substring(start, end))
            cursor = end
        }
        if (cursor < code.length) append(code.substring(cursor))
    }
}

private fun colorFor(kind: TokenKind, palette: SyntaxPalette): Color? =
    SyntaxStyles.colorFor(kind, palette, SyntaxPreset.CALM)
