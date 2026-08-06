package page.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyShortcut
import androidx.compose.ui.window.FrameWindowScope
import androidx.compose.ui.window.MenuBar
import page.app.input.ActionCatalog
import page.app.input.ActionContext
import page.app.input.ActionGroup
import page.app.input.ActionSpec
import page.ui.Binding
import page.ui.Platform

private val MENU_ORDER = listOf(
    ActionGroup.File,
    ActionGroup.Edit,
    ActionGroup.Navigate,
    ActionGroup.Code,
    ActionGroup.View,
    ActionGroup.Page,
)

@Composable
fun FrameWindowScope.SystemMenuBar(onRun: (ActionSpec) -> Unit) {
    if (!Platform.isMac) return
    MenuBar {
        for (group in MENU_ORDER) {
            val actions = ActionCatalog.all.filter {
                it.group == group && it.context == ActionContext.Global && it.run != null
            }
            if (actions.isEmpty()) continue
            Menu(text = menuTitle(group)) {
                for (action in actions) {
                    Item(
                        text = action.label,
                        shortcut = action.bindingFor(mac = true)?.let(::toShortcut),
                        onClick = { onRun(action) },
                    )
                }
            }
        }
    }
}

private fun menuTitle(group: ActionGroup): String = when (group) {
    ActionGroup.Page -> "PAGE"
    else -> group.name
}

private fun toShortcut(binding: Binding): KeyShortcut? {
    if (binding.key == Key.Unknown) return null
    return KeyShortcut(
        key = binding.key,
        ctrl = binding.control,
        meta = binding.primary,
        alt = binding.alt,
        shift = binding.shift,
    )
}
