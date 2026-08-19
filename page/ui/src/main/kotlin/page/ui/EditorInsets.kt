package page.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection

internal data class EditorInsets(
    val outer: PaddingValues,
    val trailingSlack: Dp,
    val bottomSlack: Dp,
)

internal fun editorInsets(contentPadding: PaddingValues, direction: LayoutDirection): EditorInsets =
    EditorInsets(
        outer = PaddingValues(
            start = contentPadding.calculateStartPadding(direction),
            top = contentPadding.calculateTopPadding(),
        ),
        trailingSlack = contentPadding.calculateEndPadding(direction),
        bottomSlack = contentPadding.calculateBottomPadding(),
    )

internal fun clickSurfaceWidth(textWidth: Dp, insets: EditorInsets, viewportWidth: Dp, direction: LayoutDirection): Dp =
    maxOf(
        textWidth + insets.trailingSlack,
        viewportWidth - insets.outer.calculateStartPadding(direction),
    )
