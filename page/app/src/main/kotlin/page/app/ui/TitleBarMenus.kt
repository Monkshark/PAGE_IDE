package page.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import page.app.input.ActionCatalog
import page.app.input.ActionMenus
import page.app.input.ActionSpec
import page.ui.CompactMenuContainer
import page.ui.CompactMenuItem
import page.ui.Glass
import page.ui.Platform

private val COLLAPSE_BELOW = 900.dp

@OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
@Composable
internal fun TitleBarMenus(onRun: (ActionSpec) -> Unit) {
    if (Platform.isMac) return
    val windowWidth = with(LocalDensity.current) { LocalWindowInfo.current.containerSize.width.toDp() }
    var openMenu by remember { mutableStateOf<String?>(null) }

    if (windowWidth < COLLAPSE_BELOW) {
        MenuButton(
            label = "Menu",
            open = openMenu != null,
            onClick = { openMenu = if (openMenu == null) COMPACT else null },
        ) {
            if (openMenu == COMPACT) {
                MenuPopup(onDismiss = { openMenu = null }) {
                    for (menu in ActionMenus.all) {
                        for (action in menu.actions) {
                            CompactMenuItem(
                                label = action.label,
                                icon = ActionIcons.forId(action.id),
                                trailing = ActionCatalog.label(action),
                                onClick = {
                                    openMenu = null
                                    onRun(action)
                                },
                            )
                        }
                    }
                }
            }
        }
        return
    }

    Row(modifier = Modifier.fillMaxHeight(), verticalAlignment = Alignment.CenterVertically) {
        for (menu in ActionMenus.all) {
            MenuButton(
                label = menu.title,
                open = openMenu == menu.title,
                onClick = { openMenu = if (openMenu == menu.title) null else menu.title },
                onHover = { if (openMenu != null && openMenu != menu.title) openMenu = menu.title },
            ) {
                if (openMenu == menu.title) {
                    MenuPopup(onDismiss = { openMenu = null }) {
                        for (action in menu.actions) {
                            CompactMenuItem(
                                label = action.label,
                                icon = ActionIcons.forId(action.id),
                                trailing = ActionCatalog.label(action),
                                onClick = {
                                    openMenu = null
                                    onRun(action)
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MenuButton(
    label: String,
    open: Boolean,
    onClick: () -> Unit,
    onHover: () -> Unit = {},
    popup: @Composable () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    LaunchedEffect(hovered) { if (hovered) onHover() }
    val colors = Glass.colors
    Box(contentAlignment = Alignment.CenterStart) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (open) colors.onPrimary else colors.text,
            modifier = Modifier
                .clip(RoundedCornerShape(Glass.radius.xs))
                .background(
                    when {
                        open -> colors.primary
                        hovered -> colors.highlightEdge
                        else -> Color.Transparent
                    },
                )
                .hoverable(interaction)
                .clickable(onClick = onClick)
                .padding(horizontal = 8.dp, vertical = 3.dp),
        )
        popup()
    }
}

@Composable
private fun MenuPopup(onDismiss: () -> Unit, content: @Composable () -> Unit) {
    Popup(
        offset = IntOffset(0, with(LocalDensity.current) { 32.dp.roundToPx() }),
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true),
    ) {
        CompactMenuContainer(minWidth = 220.dp) { content() }
    }
}

private const val COMPACT = "__compact"
