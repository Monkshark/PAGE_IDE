package page.ui

import com.sun.jna.Library
import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.ptr.IntByReference
import java.awt.Window

object WindowCorners {

    private const val DWMWA_WINDOW_CORNER_PREFERENCE = 33
    private const val DWMWCP_ROUND = 2
    private const val DWMWCP_DEFAULT = 0

    private interface Dwmapi : Library {
        fun DwmSetWindowAttribute(hwnd: Pointer, attribute: Int, value: IntByReference, size: Int): Int
    }

    private val library: Dwmapi? by lazy {
        if (!Platform.isWindows) null
        else runCatching { Native.load("dwmapi", Dwmapi::class.java) }.getOrNull()
    }

    fun round(window: Window): Boolean = apply(window, DWMWCP_ROUND)

    fun reset(window: Window): Boolean = apply(window, DWMWCP_DEFAULT)

    internal fun apply(window: Window, preference: Int): Boolean {
        val dwm = library ?: return false
        if (!window.isDisplayable) return false
        val handle = runCatching { Native.getWindowPointer(window) }.getOrNull() ?: return false
        val result = runCatching {
            dwm.DwmSetWindowAttribute(handle, DWMWA_WINDOW_CORNER_PREFERENCE, IntByReference(preference), 4)
        }.getOrNull() ?: return false
        return result == 0
    }
}
