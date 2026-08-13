package page.workspace

import page.runtime.*

import java.awt.EventQueue
import java.awt.Frame
import java.io.File
import java.nio.file.Path
import javax.swing.JFileChooser

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

    private fun chooser(): JFileChooser {
        shared?.let { return it }
        synchronized(this) {
            shared?.let { return it }
            return JFileChooser().also { shared = it }
        }
    }

    fun warmUp() {
        if (shared != null) return
        EventQueue.invokeLater { runCatching { chooser() } }
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
