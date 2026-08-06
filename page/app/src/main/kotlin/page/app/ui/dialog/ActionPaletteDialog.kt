package page.app.ui.dialog

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import page.app.input.ActionCatalog
import page.app.input.ActionMatch
import page.app.input.ActionSearch
import page.app.input.ActionSpec
import page.ui.Glass
import page.ui.GlassSurface
import page.ui.GlassSurfaceLevel

@Composable
fun ActionPaletteDialog(
    onRun: (ActionSpec) -> Unit,
    onDismiss: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf(0) }
    val results = remember(query) { ActionSearch.rank(query) }
    val listState = rememberLazyListState()
    val queryFocus = remember { FocusRequester() }
    val colors = Glass.colors

    LaunchedEffect(Unit) { queryFocus.requestFocus() }
    LaunchedEffect(results) {
        if (selected >= results.size) selected = if (results.isEmpty()) 0 else results.size - 1
    }
    LaunchedEffect(selected) {
        if (selected in results.indices) listState.animateScrollToItem(selected)
    }

    fun runSelected() {
        val hit = results.getOrNull(selected) ?: return
        onDismiss()
        onRun(hit.spec)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) false
                else when (event.key) {
                    Key.Escape -> { onDismiss(); true }
                    Key.DirectionDown -> {
                        if (results.isNotEmpty()) selected = (selected + 1).coerceAtMost(results.size - 1)
                        true
                    }
                    Key.DirectionUp -> {
                        if (results.isNotEmpty()) selected = (selected - 1).coerceAtLeast(0)
                        true
                    }
                    Key.Enter, Key.NumPadEnter -> { runSelected(); true }
                    else -> false
                }
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss,
            ),
        contentAlignment = Alignment.TopCenter,
    ) {
        GlassSurface(
            level = GlassSurfaceLevel.Overlay,
            shape = RoundedCornerShape(Glass.radius.lg),
            modifier = Modifier
                .padding(top = 88.dp)
                .width(560.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                ),
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = ">",
                        style = MaterialTheme.typography.bodyMedium,
                        fontFamily = FontFamily.Monospace,
                        color = colors.primary,
                    )
                    BasicTextField(
                        value = query,
                        onValueChange = { query = it; selected = 0 },
                        singleLine = true,
                        cursorBrush = SolidColor(colors.primary),
                        textStyle = TextStyle(color = colors.text, fontSize = 14.sp),
                        modifier = Modifier.weight(1f).focusRequester(queryFocus),
                    )
                    Text(
                        text = "${results.size} actions",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.faint,
                    )
                }
                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(colors.separator))
                if (results.isEmpty()) {
                    Text(
                        text = "No action matches \"$query\"",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.muted,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    )
                } else {
                    LazyColumn(state = listState, modifier = Modifier.heightIn(max = 380.dp)) {
                        items(results) { hit ->
                            ActionRow(
                                hit = hit,
                                selected = results.getOrNull(selected) === hit,
                                onClick = {
                                    onDismiss()
                                    onRun(hit.spec)
                                },
                            )
                        }
                    }
                }
                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(colors.separator))
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    for (hint in listOf("↑↓ move", "Enter run", "Esc close")) {
                        Text(text = hint, style = MaterialTheme.typography.labelSmall, color = colors.faint)
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionRow(hit: ActionMatch, selected: Boolean, onClick: () -> Unit) {
    val colors = Glass.colors
    val label = buildAnnotatedString {
        val marked = hit.matchedIndices.toSet()
        hit.spec.label.forEachIndexed { index, ch ->
            if (index in marked) {
                pushStyle(SpanStyle(color = colors.primary, fontWeight = FontWeight.SemiBold))
                append(ch)
                pop()
            } else {
                append(ch)
            }
        }
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (selected) colors.primarySoft else Color.Transparent)
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = colors.text)
        Text(
            text = hit.spec.group.name,
            style = MaterialTheme.typography.labelSmall,
            color = colors.faint,
        )
        Box(modifier = Modifier.weight(1f))
        val shortcut = ActionCatalog.label(hit.spec)
        if (shortcut != null) {
            Text(
                text = shortcut,
                style = MaterialTheme.typography.labelSmall,
                fontFamily = FontFamily.Monospace,
                color = colors.muted,
            )
        }
    }
}
