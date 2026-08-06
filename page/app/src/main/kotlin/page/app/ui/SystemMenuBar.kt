package page.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyShortcut
import androidx.compose.ui.window.FrameWindowScope
import androidx.compose.ui.window.MenuBar
import page.app.input.ActionMenus
import page.app.input.ActionSpec
import page.ui.Binding
import page.ui.Platform

@Composable
fun FrameWindowScope.SystemMenuBar(onRun: (ActionSpec) -> Unit) {
    if (!Platform.isMac) return
    MenuBar {
        for (menu in ActionMenus.all) {
            Menu(text = menu.title) {
                for (action in menu.actions) {
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
