package page.app.input

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed

internal object Platform {
    val isMac: Boolean = (System.getProperty("os.name") ?: "").lowercase().contains("mac")
}

internal fun KeyEvent.isPrimaryPressed(mac: Boolean = Platform.isMac): Boolean =
    if (mac) isMetaPressed else isCtrlPressed

internal fun KeyEvent.isSecondaryControlPressed(mac: Boolean = Platform.isMac): Boolean =
    if (mac) isCtrlPressed else false

internal object ShortcutLabels {

    fun of(binding: Binding, mac: Boolean = Platform.isMac): String {
        val parts = ArrayList<String>(4)
        if (mac) {
            if (binding.control) parts += "⌃"
            if (binding.alt) parts += "⌥"
            if (binding.shift) parts += "⇧"
            if (binding.primary) parts += "⌘"
            return parts.joinToString("") + keyLabel(binding.key, mac)
        }
        if (binding.primary) parts += "Ctrl"
        if (binding.control) parts += "Ctrl"
        if (binding.alt) parts += "Alt"
        if (binding.shift) parts += "Shift"
        parts += keyLabel(binding.key, mac)
        return parts.joinToString("+")
    }

    private fun keyLabel(key: Key, mac: Boolean): String = when (key) {
        Key.Enter, Key.NumPadEnter -> if (mac) "↩" else "Enter"
        Key.Escape -> "Esc"
        Key.DirectionLeft -> if (mac) "←" else "Left"
        Key.DirectionRight -> if (mac) "→" else "Right"
        Key.DirectionUp -> if (mac) "↑" else "Up"
        Key.DirectionDown -> if (mac) "↓" else "Down"
        Key.Backslash -> "\\"
        Key.Comma -> ","
        Key.Spacebar -> "Space"
        Key.Six -> "6"
        else -> key.toString().removePrefix("Key: ")
    }
}
