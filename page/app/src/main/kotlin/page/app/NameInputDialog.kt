package page.app

import page.runtime.*
import page.workspace.*

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DriveFileRenameOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogWindow
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberDialogState
import page.ui.Glass
import page.ui.GlassTheme
import java.nio.file.Path

internal fun initialNameFieldValue(text: String): TextFieldValue {
    val lastDot = text.lastIndexOf('.')
    val selEnd = if (lastDot > 0) lastDot else text.length
    return TextFieldValue(text, TextRange(0, selEnd))
}

@Composable
private fun NameInputChip(
    label: String,
    primary: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val colors = Glass.colors
    val src = remember { MutableInteractionSource() }
    val hovered by src.collectIsHoveredAsState()
    val active = hovered && enabled
    val shape = RoundedCornerShape(Glass.radius.xs)
    val bg = when {
        !enabled -> colors.surface.copy(alpha = 0.4f)
        primary && active -> colors.primary
        primary -> colors.primary.copy(alpha = 0.8f)
        active -> colors.primarySoft
        else -> colors.surface.copy(alpha = 0.6f)
    }
    Row(
        modifier = Modifier
            .height(28.dp)
            .clip(shape)
            .background(bg)
            .border(1.dp, if (primary && enabled) colors.primary else colors.outline, shape)
            .hoverable(src, enabled = enabled)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            color = when {
                !enabled -> colors.faint
                primary -> colors.onPrimary
                else -> colors.text
            },
            style = LocalTextStyle.current.copy(
                fontSize = 11.sp,
                lineHeight = 11.sp,
                lineHeightStyle = androidx.compose.ui.text.style.LineHeightStyle(
                    alignment = androidx.compose.ui.text.style.LineHeightStyle.Alignment.Center,
                    trim = androidx.compose.ui.text.style.LineHeightStyle.Trim.Both,
                ),
            ),
        )
    }
}

@Composable
internal fun NameInputDialog(
    title: String,
    label: String,
    initial: String = "",
    error: String?,
    impact: ImpactScanState? = null,
    rootDir: Path? = null,
    onJumpToHit: ((ReferenceHit) -> Unit)? = null,
    onSkip: (() -> Unit)? = null,
    onSkipRemaining: (() -> Unit)? = null,
    onOverwrite: ((String) -> Unit)? = null,
    onOverwriteAll: ((String) -> Unit)? = null,
    onSubmit: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val valueState = remember { mutableStateOf(initialNameFieldValue(initial)) }
    var value by valueState
    val focus = remember { FocusRequester() }
    LaunchedEffect(initial) {
        valueState.value = initialNameFieldValue(initial)
        focus.requestFocus()
    }

    val hasHits = (impact as? ImpactScanState.Done)?.hits?.isNotEmpty() == true
    val showHits = hasHits && onJumpToHit != null
    val extraChips = onSkip != null || onSkipRemaining != null
    val baseWidth = if (showHits) 520.dp else 460.dp
    val targetWidth = if (extraChips) baseWidth + 120.dp else baseWidth
    val errorLineCount = error?.let { msg ->
        val charsPerLine = (targetWidth.value.toInt() - 36) / 6
        val approx = (msg.length + charsPerLine - 1) / charsPerLine.coerceAtLeast(1)
        approx.coerceIn(1, 6)
    } ?: 1
    val errorExtra = ((errorLineCount - 1) * 14).dp
    val canOverwrite = error?.contains("already exists") == true &&
        (onOverwrite != null || onOverwriteAll != null)
    val chromeExtra = 76.dp
    val targetHeight = when {
        showHits -> 340.dp + errorExtra + chromeExtra
        impact == null || impact is ImpactScanState.Idle -> 140.dp + errorExtra + chromeExtra
        else -> 168.dp + errorExtra + chromeExtra
    }
    val state = rememberDialogState(
        position = WindowPosition.Aligned(Alignment.Center),
        width = targetWidth,
        height = targetHeight,
    )
    LaunchedEffect(targetWidth, targetHeight) {
        state.size = DpSize(targetWidth, targetHeight)
    }

    DialogWindow(
        onCloseRequest = onDismiss,
        state = state,
        title = title,
        resizable = false,
        undecorated = true,
        alwaysOnTop = true,
        onPreviewKeyEvent = { event ->
            if (event.type != KeyEventType.KeyDown) false
            else when (event.key) {
                Key.Escape -> { onDismiss(); true }
                Key.Enter, Key.NumPadEnter -> {
                    val name = value.text.trim()
                    if (FileName.check(name) is NameCheck.Ready) onSubmit(name)
                    true
                }
                else -> false
            }
        },
    ) {
        GlassTheme {
            val colors = Glass.colors
            val check = FileName.check(value.text)
            Surface(
                modifier = Modifier.fillMaxSize().border(1.dp, colors.outline),
                color = colors.background,
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 14.dp, end = 14.dp, top = 11.dp, bottom = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.DriveFileRenameOutline,
                            contentDescription = null,
                            tint = colors.muted,
                            modifier = Modifier.size(14.dp),
                        )
                        Text(
                            text = title,
                            style = MaterialTheme.typography.labelMedium,
                            color = colors.text,
                        )
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.faint,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Box(Modifier.fillMaxWidth().height(1.dp).background(colors.separator))
                    Column(modifier = Modifier.fillMaxSize().padding(14.dp)) {
                        val fieldEdge = when {
                            error != null || check is NameCheck.Invalid -> colors.danger
                            else -> colors.outline
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(32.dp)
                                .clip(RoundedCornerShape(Glass.radius.xs))
                                .background(colors.surface)
                                .border(1.dp, fieldEdge, RoundedCornerShape(Glass.radius.xs))
                                .padding(horizontal = 10.dp),
                            contentAlignment = Alignment.CenterStart,
                        ) {
                            BasicTextField(
                                value = value,
                                onValueChange = { value = it },
                                singleLine = true,
                                cursorBrush = SolidColor(colors.primary),
                                textStyle = TextStyle(
                                    color = colors.text,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 13.sp,
                                ),
                                modifier = Modifier.fillMaxWidth().focusRequester(focus),
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        val status = when {
                            error != null -> error
                            check is NameCheck.Invalid -> check.reason
                            check is NameCheck.Empty -> "Type a name"
                            showHits -> "Click a row to jump to it"
                            else -> "Esc to cancel"
                        }
                        val statusColor = when {
                            error != null || check is NameCheck.Invalid -> colors.danger
                            else -> colors.muted
                        }
                        Text(
                            text = status,
                            color = statusColor,
                            style = LocalTextStyle.current.copy(fontSize = 11.sp),
                            softWrap = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        if (impact != null && impact !is ImpactScanState.Idle) {
                            Spacer(Modifier.height(4.dp))
                            ImpactStatusLine(impact)
                        }
                        if (showHits) {
                            Spacer(Modifier.height(6.dp))
                            val hits = (impact as ImpactScanState.Done).hits
                            ReferenceHitsList(
                                hits = hits,
                                rootDir = rootDir,
                                onJumpToHit = onJumpToHit!!,
                                modifier = Modifier.weight(1f),
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (canOverwrite && onOverwrite != null) {
                                NameInputChip("Overwrite") { onOverwrite(value.text.trim()) }
                                Spacer(Modifier.width(8.dp))
                            }
                            if (canOverwrite && onOverwriteAll != null) {
                                NameInputChip("Overwrite all") { onOverwriteAll(value.text.trim()) }
                                Spacer(Modifier.width(8.dp))
                            }
                            if (onSkip != null) NameInputChip("Skip this", onClick = onSkip)
                            if (onSkip != null && onSkipRemaining != null) Spacer(Modifier.width(8.dp))
                            if (onSkipRemaining != null) NameInputChip("Skip all", onClick = onSkipRemaining)
                            Spacer(Modifier.weight(1f))
                            NameInputChip("Cancel", onClick = onDismiss)
                            Spacer(Modifier.width(8.dp))
                            NameInputChip(
                                label = title,
                                primary = true,
                                enabled = check is NameCheck.Ready,
                            ) { onSubmit(value.text.trim()) }
                        }
                    }
                }
            }
        }
    }
}

internal sealed interface NameCheck {
    object Ready : NameCheck
    object Empty : NameCheck
    data class Invalid(val reason: String) : NameCheck
}

internal object FileName {
    private val ILLEGAL = charArrayOf('\\', '/', ':', '*', '?', '"', '<', '>', '|')
    private val RESERVED = setOf("con", "prn", "aux", "nul") +
        (1..9).flatMap { listOf("com$it", "lpt$it") }

    fun check(raw: String): NameCheck {
        val name = raw.trim()
        return when {
            name.isEmpty() -> NameCheck.Empty
            name == "." || name == ".." -> NameCheck.Invalid("Pick a real name")
            name.any { it in ILLEGAL } -> NameCheck.Invalid("A name cannot contain \\ / : * ? \" < > |")
            name.endsWith(".") -> NameCheck.Invalid("A name cannot end with a dot")
            name.substringBefore('.').lowercase() in RESERVED ->
                NameCheck.Invalid("$name is reserved by Windows")
            else -> NameCheck.Ready
        }
    }
}
