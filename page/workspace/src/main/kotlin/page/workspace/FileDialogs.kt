package page.workspace

import page.runtime.*

import java.awt.EventQueue
import java.awt.Frame
import java.io.File
import java.nio.file.Path
import javax.swing.JFileChooser
import javax.swing.filechooser.FileSystemView

object FileDialogs {

    /**
     * The first [JFileChooser] costs a fifth of a second to build — Swing installs its look and feel,
     * the Windows places bar and a text field before anything shows, and the places bar reaches into
     * the shell over COM. It has to happen on the event thread, so only its timing can be chosen: one
     * chooser is kept and reused, and [warmUp] buys it once the opening rush has died down rather than
     * during it, where it showed up as a dropped-frame dip.
     */
    @Volatile
    private var shared: JFileChooser? = null

    private const val EDT_WARMUP_DELAY_MS = 4_000L

    private fun chooser(): JFileChooser {
        shared?.let { return it }
        synchronized(this) {
            shared?.let { return it }
            return JFileChooser().also { shared = it }
        }
    }

    /**
     * Most of the chooser's price is the shortcut panel asking the Windows shell, over COM, for the
     * folders and icons it puts down its left side. That answer is cached process-wide and can be
     * fetched from any thread, so it is bought here rather than on the event thread. What is left
     * still has to be built on the event thread, and is, once the opening rush is over — but a user
     * who reaches for Open first now waits for the smaller half.
     */
    fun warmUp() {
        if (shared != null) return
        Thread({
            runCatching {
                val view = FileSystemView.getFileSystemView()
                view.chooserShortcutPanelFiles
                view.roots
                view.homeDirectory
            }
            runCatching { Thread.sleep(EDT_WARMUP_DELAY_MS) }
            EventQueue.invokeLater { runCatching { chooser() } }
        }, "file-dialog-warmup").apply { isDaemon = true }.start()
    }

    fun open(parent: Frame): Path? {
        val chooser = chooser().apply {
            dialogTitle = "Open"
            fileSelectionMode = JFileChooser.FILES_ONLY
            selectedFile = null
        }
        return if (chooser.showOpenDialog(parent) == JFileChooser.APPROVE_OPTION) {
            chooser.selectedFile.toPath()
        } else null
    }

    fun saveAs(parent: Frame, suggested: String? = null): Path? {
        val chooser = chooser().apply {
            dialogTitle = "Save As"
            fileSelectionMode = JFileChooser.FILES_ONLY
            selectedFile = suggested?.let { File(it) }
        }
        return if (chooser.showSaveDialog(parent) == JFileChooser.APPROVE_OPTION) {
            chooser.selectedFile.toPath()
        } else null
    }

    fun openDirectory(parent: Frame): Path? {
        val chooser = chooser().apply {
            dialogTitle = "Open Folder"
            fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
            selectedFile = null
        }
        return if (chooser.showOpenDialog(parent) == JFileChooser.APPROVE_OPTION) {
            chooser.selectedFile.toPath()
        } else null
    }

    fun newProjectDirectory(parent: Frame): Path? {
        val chooser = chooser().apply {
            dialogTitle = "New Project"
            fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
            selectedFile = null
        }
        return if (chooser.showDialog(parent, "Create") == JFileChooser.APPROVE_OPTION) {
            chooser.selectedFile.toPath()
        } else null
    }
}
