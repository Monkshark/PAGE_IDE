package page.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.future.await
import page.lsp.RenameWorkspaceEdit
import page.ui.Glass
import page.ui.GlassPopup
import java.util.concurrent.CompletableFuture

data class RenameRequestState(
    val line: Int,
    val character: Int,
    val placeholder: String,
)

internal sealed interface RenameCheck {
    object Ready : RenameCheck
    object Empty : RenameCheck
    object Unchanged : RenameCheck
    data class Invalid(val reason: String) : RenameCheck
}

internal object RenameName {
    fun check(raw: String, current: String): RenameCheck {
        val name = raw.trim()
        val first = name.firstOrNull()
        return when {
            name.isEmpty() -> RenameCheck.Empty
            name == current -> RenameCheck.Unchanged
            name.any { it.isWhitespace() } -> RenameCheck.Invalid("No spaces in a name")
            first != null && !first.isLetter() && first != '_' ->
                RenameCheck.Invalid("Start with a letter or _")
            name.any { !it.isLetterOrDigit() && it != '_' } ->
                RenameCheck.Invalid("Letters, digits and _ only")
            else -> RenameCheck.Ready
        }
    }

    fun scopeLabel(edit: RenameWorkspaceEdit): String {
        val edits = edit.totalEditCount
        val files = edit.changes.count { it.edits.isNotEmpty() }
        val editWord = if (edits == 1) "edit" else "edits"
        val fileWord = if (files == 1) "file" else "files"
        return "$edits $editWord · $files $fileWord"
    }
}

private const val SCOPE_DEBOUNCE_MS = 350L

@Composable
internal fun RenamePopup(
    request: RenameRequestState,
    inProgress: Boolean,
    error: String?,
    onComputeEdit: (String) -> CompletableFuture<RenameWorkspaceEdit>,
    onApply: (String, RenameWorkspaceEdit) -> Unit,
    onPreview: (String, RenameWorkspaceEdit) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = Glass.colors
    var value by remember(request) {
        mutableStateOf(TextFieldValue(request.placeholder, TextRange(0, request.placeholder.length)))
    }
    var scope by remember(request) { mutableStateOf<RenameWorkspaceEdit?>(null) }
    var scoping by remember(request) { mutableStateOf(false) }
    val focus = remember(request) { FocusRequester() }
    val submitScope = rememberCoroutineScope()
    LaunchedEffect(request) { runCatching { focus.requestFocus() } }

    val name = value.text.trim()
    val check = RenameName.check(name, request.placeholder)

    LaunchedEffect(request, name) {
        scope = null
        if (check !is RenameCheck.Ready) {
            scoping = false
            return@LaunchedEffect
        }
        delay(SCOPE_DEBOUNCE_MS)
        scoping = true
        val computed = runCatching { onComputeEdit(name).await() }.getOrNull()
        scoping = false
        scope = computed
    }

    val submit: (preview: Boolean) -> Unit = { preview ->
        when {
            check is RenameCheck.Unchanged -> onDismiss()
            check !is RenameCheck.Ready || inProgress -> Unit
            else -> {
                val ready = scope
                if (ready != null) {
                    if (preview) onPreview(name, ready) else onApply(name, ready)
                } else {
                    submitScope.launch {
                        val edit = runCatching { onComputeEdit(name).await() }.getOrNull()
                        if (edit != null) {
                            if (preview) onPreview(name, edit) else onApply(name, edit)
                        }
                    }
                }
            }
        }
    }

    GlassPopup(modifier = Modifier.width(340.dp)) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "Rename symbol",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.muted,
                )
                Box(modifier = Modifier.weight(1f))
                Text(
                    text = request.placeholder,
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.faint,
                    fontFamily = FontFamily.Monospace,
                )
            }
            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(colors.separator))
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                val fieldEdge = if (check is RenameCheck.Invalid) colors.danger else colors.outline
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(30.dp)
                        .clip(RoundedCornerShape(Glass.radius.xs))
                        .background(colors.surface)
                        .border(1.dp, fieldEdge, RoundedCornerShape(Glass.radius.xs))
                        .padding(horizontal = 9.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    BasicTextField(
                        value = value,
                        onValueChange = { if (!inProgress) value = it },
                        singleLine = true,
                        cursorBrush = SolidColor(colors.primary),
                        textStyle = TextStyle(
                            color = colors.text,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focus)
                            .onPreviewKeyEvent { event ->
                                if (event.type != KeyEventType.KeyDown) false
                                else when (event.key) {
                                    Key.Escape -> {
                                        if (!inProgress) onDismiss()
                                        true
                                    }
                                    Key.Enter, Key.NumPadEnter -> {
                                        submit(event.isShiftPressed)
                                        true
                                    }
                                    else -> false
                                }
                            },
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    val status = when {
                        error != null -> error
                        inProgress -> "Applying…"
                        check is RenameCheck.Invalid -> check.reason
                        check is RenameCheck.Empty -> "Type a new name"
                        check is RenameCheck.Unchanged -> "Same name"
                        scoping -> "Checking…"
                        scope != null -> RenameName.scopeLabel(scope!!)
                        else -> " "
                    }
                    val statusColor = when {
                        error != null || check is RenameCheck.Invalid -> colors.danger
                        scope != null -> colors.primary
                        else -> colors.faint
                    }
                    Text(
                        text = status,
                        style = MaterialTheme.typography.labelSmall,
                        color = statusColor,
                    )
                    Box(modifier = Modifier.weight(1f))
                    if (scope != null && !inProgress) {
                        Text(
                            text = "Preview",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.muted,
                            modifier = Modifier
                                .clickable { submit(true) }
                                .padding(horizontal = 4.dp),
                        )
                    }
                    Text(
                        text = "Enter",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.faint,
                    )
                }
            }
        }
    }
}
