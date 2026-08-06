package page.ui

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.pointer.PointerKeyboardModifiers
import androidx.compose.ui.input.pointer.isCtrlPressed as pointerCtrlPressed
import androidx.compose.ui.input.pointer.isMetaPressed as pointerMetaPressed

data class Binding(
    val key: Key,
    val primary: Boolean = false,
    val control: Boolean = false,
    val alt: Boolean = false,
    val shift: Boolean = false,
) {
    fun matches(key: Key, primary: Boolean, control: Boolean, alt: Boolean, shift: Boolean): Boolean =
        this.key == key &&
            this.primary == primary &&
            this.control == control &&
            this.alt == alt &&
            this.shift == shift
}

object Platform {
    val isMac: Boolean = (System.getProperty("os.name") ?: "").lowercase().contains("mac")
}

fun KeyEvent.isPrimaryPressed(mac: Boolean = Platform.isMac): Boolean =
    if (mac) isMetaPressed else isCtrlPressed

fun KeyEvent.isSecondaryControlPressed(mac: Boolean = Platform.isMac): Boolean =
    if (mac) isCtrlPressed else false

fun PointerKeyboardModifiers.isPrimaryPressed(mac: Boolean = Platform.isMac): Boolean =
    if (mac) pointerMetaPressed else pointerCtrlPressed

object ShortcutLabels {

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
