package page.app.input

import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type

internal object ActionDispatcher {

    fun handle(event: KeyEvent, host: ActionHost, mac: Boolean = Platform.isMac): Boolean {
        if (event.type != KeyEventType.KeyDown) return false
        val spec = ActionCatalog.resolve(
            key = normalizeKey(event.key),
            primary = event.isPrimaryPressed(mac),
            control = event.isSecondaryControlPressed(mac),
            alt = event.isAltPressed,
            shift = event.isShiftPressed,
            hasSearch = host.hasSearch,
            mac = mac,
        ) ?: return false
        spec.run(host)
        return true
    }

    private fun normalizeKey(key: androidx.compose.ui.input.key.Key) =
        if (key == androidx.compose.ui.input.key.Key.NumPadEnter) androidx.compose.ui.input.key.Key.Enter else key
}
