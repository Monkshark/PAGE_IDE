package page.shared.syntax

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration

/**
 * How much the editor separates token roles. The palette always carries a color per role; the preset
 * decides how many of those colors are used and whether emphasis is layered on top.
 */
enum class SyntaxPreset(val label: String) {
    /** Colors as they were before roles existed: calls and members read as plain identifiers. */
    CALM("Calm"),

    /** One color set, roles told apart by italic, underline and a dimmer parameter tone. */
    BALANCED("Balanced"),

    /** A color per role. */
    VIVID("Vivid"),

    /** A color per role, plus the emphasis from Balanced. */
    EXPRESSIVE("Expressive");

    val usesRoleColors: Boolean get() = this == VIVID || this == EXPRESSIVE
    val usesEmphasis: Boolean get() = this == BALANCED || this == EXPRESSIVE
}

object SyntaxStyles {

    fun colorFor(kind: TokenKind, palette: SyntaxPalette, preset: SyntaxPreset): Color? = when (kind) {
        TokenKind.KEYWORD -> palette.keyword
        TokenKind.STRING -> palette.string
        TokenKind.NUMBER -> palette.number
        TokenKind.COMMENT -> palette.comment
        TokenKind.DOC_COMMENT -> palette.docComment
        TokenKind.TODO_TAG -> palette.todoTag
        TokenKind.ANNOTATION -> palette.annotation
        TokenKind.TYPE -> palette.type
        TokenKind.IDENTIFIER -> palette.identifier
        TokenKind.FUNCTION -> if (preset.usesRoleColors) palette.function else palette.identifier
        TokenKind.PROPERTY -> if (preset.usesRoleColors) palette.property else palette.identifier
        TokenKind.PARAMETER -> when {
            preset.usesRoleColors -> palette.parameter
            preset.usesEmphasis -> palette.parameter
            else -> palette.identifier
        }
        TokenKind.TEMPLATE -> if (preset.usesRoleColors) palette.template else palette.identifier
        TokenKind.PUNCT -> null
    }

    fun spanFor(kind: TokenKind, palette: SyntaxPalette, preset: SyntaxPreset): SpanStyle? {
        val color = colorFor(kind, palette, preset)
        if (color == null) return null
        if (!preset.usesEmphasis) return SpanStyle(color = color)
        return when (kind) {
            TokenKind.FUNCTION -> SpanStyle(color = color, fontStyle = FontStyle.Italic)
            TokenKind.PROPERTY -> SpanStyle(color = color, textDecoration = TextDecoration.Underline)
            TokenKind.TYPE -> SpanStyle(color = color, fontWeight = FontWeight.Medium)
            else -> SpanStyle(color = color)
        }
    }
}
