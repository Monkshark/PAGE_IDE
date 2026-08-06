package page.shared.syntax

import androidx.compose.ui.graphics.Color

data class SyntaxPalette(
    val keyword: Color,
    val string: Color,
    val number: Color,
    val comment: Color,
    val docComment: Color,
    val todoTag: Color,
    val annotation: Color,
    val type: Color,
    val identifier: Color,
    val function: Color = identifier,
    val property: Color = identifier,
    val parameter: Color = identifier,
    val template: Color = identifier,
) {
    val bracketDepths: List<Color> = listOf(function, property, keyword, template, type, annotation)

    fun bracketAt(depth: Int): Color = bracketDepths[depth.mod(bracketDepths.size)]
}
