package page.app.ui

import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.text.AnnotatedString

/**
 * Wraps the platform clipboard so transient AWT contention
 * ("cannot open system clipboard", thrown when the clipboard is briefly
 * locked by another process or by rapid repeated access) is swallowed
 * instead of crashing the app.
 */
class SafeClipboardManager(private val delegate: ClipboardManager) : ClipboardManager {

    override fun setText(annotatedString: AnnotatedString) {
        repeat(3) {
            try {
                delegate.setText(annotatedString)
                return
            } catch (_: IllegalStateException) {
            } catch (_: java.awt.HeadlessException) {
                return
            }
        }
    }

    override fun getText(): AnnotatedString? {
        repeat(3) {
            try {
                return delegate.getText()
            } catch (_: IllegalStateException) {
            } catch (_: java.awt.HeadlessException) {
                return null
            }
        }
        return null
    }

    override fun hasText(): Boolean = try {
        delegate.hasText()
    } catch (_: IllegalStateException) {
        false
    } catch (_: java.awt.HeadlessException) {
        false
    }
}
