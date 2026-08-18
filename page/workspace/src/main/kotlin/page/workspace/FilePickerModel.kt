package page.workspace

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

enum class PickMode { OPEN_FILE, OPEN_FOLDER, SAVE_AS, NEW_PROJECT }

data class PickerEntry(
    val path: Path,
    val name: String,
    val isDirectory: Boolean,
    val sizeBytes: Long,
    val isHidden: Boolean = false,
)

sealed class PickerListing {
    data class Ready(val entries: List<PickerEntry>) : PickerListing()
    data class Denied(val reason: String) : PickerListing()
}

data class Crumb(val label: String, val path: Path)

object FilePickerModel {

    fun title(mode: PickMode): String = when (mode) {
        PickMode.OPEN_FILE -> "Open File"
        PickMode.OPEN_FOLDER -> "Open Folder"
        PickMode.SAVE_AS -> "Save As"
        PickMode.NEW_PROJECT -> "New Project"
    }

    fun confirmLabel(mode: PickMode): String = when (mode) {
        PickMode.OPEN_FILE, PickMode.OPEN_FOLDER -> "Open"
        PickMode.SAVE_AS -> "Save"
        PickMode.NEW_PROJECT -> "Create"
    }

    fun modeTag(mode: PickMode): String = when (mode) {
        PickMode.OPEN_FILE, PickMode.SAVE_AS -> "file"
        PickMode.OPEN_FOLDER, PickMode.NEW_PROJECT -> "folder"
    }

    fun picksDirectories(mode: PickMode): Boolean =
        mode == PickMode.OPEN_FOLDER || mode == PickMode.NEW_PROJECT

    fun needsName(mode: PickMode): Boolean =
        mode == PickMode.SAVE_AS || mode == PickMode.NEW_PROJECT

    fun list(dir: Path): PickerListing {
        val stream = runCatching { Files.newDirectoryStream(dir) }
            .getOrElse { return PickerListing.Denied(deniedReason(it)) }
        val entries = mutableListOf<PickerEntry>()
        stream.use {
            for (child in it) {
                val name = child.fileName?.toString() ?: continue
                val dos = runCatching {
                    Files.readAttributes(child, java.nio.file.attribute.DosFileAttributes::class.java)
                }.getOrNull()
                val basic = dos ?: runCatching {
                    Files.readAttributes(child, java.nio.file.attribute.BasicFileAttributes::class.java)
                }.getOrNull()
                val directory = basic?.isDirectory ?: false
                val size = if (directory) 0L else basic?.size() ?: 0L
                entries += PickerEntry(child, name, directory, size, isHidden(name, dos?.isHidden))
            }
        }
        return PickerListing.Ready(sortEntries(entries))
    }

    internal fun deniedReason(error: Throwable): String = when (error) {
        is java.nio.file.AccessDeniedException -> "Access is denied"
        is java.nio.file.NoSuchFileException -> "That folder is no longer there"
        is java.nio.file.NotDirectoryException -> "That is a file, not a folder"
        else -> error.message?.takeIf { it.isNotBlank() } ?: "The folder could not be read"
    }

    internal fun sortEntries(entries: List<PickerEntry>): List<PickerEntry> =
        entries.sortedWith(
            compareByDescending<PickerEntry> { it.isDirectory }
                .thenBy(String.CASE_INSENSITIVE_ORDER) { it.name },
        )

    internal fun isHidden(name: String, dosHidden: Boolean?): Boolean =
        name.startsWith(".") || dosHidden == true

    fun visible(
        entries: List<PickerEntry>,
        query: String,
        mode: PickMode,
        showHidden: Boolean = true,
    ): List<PickerEntry> {
        val trimmed = query.trim()
        var out = entries
        if (!showHidden) out = out.filter { !it.isHidden }
        if (trimmed.isEmpty()) return out
        return out.filter { it.isDirectory || !picksDirectories(mode) }
            .filter { it.name.contains(trimmed, ignoreCase = true) }
    }

    fun hiddenCount(entries: List<PickerEntry>): Int = entries.count { it.isHidden }

    fun crumbs(dir: Path): List<Crumb> {
        val absolute = runCatching { dir.toAbsolutePath().normalize() }.getOrDefault(dir)
        val root = absolute.root
        val out = mutableListOf<Crumb>()
        if (root != null) {
            out += Crumb(root.toString().trimEnd('\\', '/').ifEmpty { root.toString() }, root)
        }
        var walk: Path = root ?: absolute.subpath(0, 0)
        for (part in absolute) {
            walk = walk.resolve(part)
            out += Crumb(part.toString(), walk)
        }
        return out
    }

    fun parentOf(dir: Path): Path? = runCatching { dir.toAbsolutePath().normalize().parent }.getOrNull()

    fun trail(dir: Path): List<Path> {
        val absolute = runCatching { dir.toAbsolutePath().normalize() }.getOrDefault(dir)
        val out = ArrayDeque<Path>()
        var walk: Path? = absolute
        while (walk != null) {
            out.addFirst(walk)
            walk = walk.parent
        }
        return out.toList()
    }

    fun columns(trail: List<Path>, max: Int = 3): List<Path> {
        if (trail.isEmpty()) return emptyList()
        if (trail.size <= max) return trail
        return trail.subList(trail.size - max, trail.size)
    }

    fun childOnTrail(trail: List<Path>, column: Path): Path? {
        val index = trail.indexOf(column)
        if (index < 0 || index == trail.lastIndex) return null
        return trail[index + 1]
    }

    fun columnLabel(dir: Path): String =
        dir.fileName?.toString() ?: dir.toString().trimEnd('\\', '/').ifEmpty { dir.toString() }

    fun roots(): List<Path> = runCatching { java.io.File.listRoots().map { it.toPath() } }.getOrDefault(emptyList())

    fun homeDirectory(): Path = Paths.get(System.getProperty("user.home") ?: ".")

    fun startingDirectory(requested: Path?): Path {
        val candidate = requested ?: homeDirectory()
        val absolute = runCatching { candidate.toAbsolutePath().normalize() }.getOrDefault(candidate)
        var walk: Path? = absolute
        while (walk != null) {
            if (runCatching { Files.isDirectory(walk) }.getOrDefault(false)) return walk
            walk = walk.parent
        }
        return homeDirectory()
    }

    fun canConfirm(mode: PickMode, current: Path, selected: PickerEntry?, name: String): Boolean = when (mode) {
        PickMode.OPEN_FILE -> selected != null && !selected.isDirectory
        PickMode.OPEN_FOLDER -> selected == null || selected.isDirectory
        PickMode.SAVE_AS, PickMode.NEW_PROJECT -> nameError(name) == null && current.toString().isNotEmpty()
    }

    fun target(mode: PickMode, current: Path, selected: PickerEntry?, name: String): Path? = when (mode) {
        PickMode.OPEN_FILE -> selected?.path?.takeIf { !selected.isDirectory }
        PickMode.OPEN_FOLDER -> selected?.path?.takeIf { selected.isDirectory } ?: current
        PickMode.SAVE_AS, PickMode.NEW_PROJECT ->
            if (nameError(name) != null) null else current.resolve(name.trim())
    }

    fun nameError(name: String): String? {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return "Enter a name"
        if (trimmed == "." || trimmed == "..") return "That name is reserved"
        val illegal = trimmed.firstOrNull { it in ILLEGAL_NAME_CHARS }
        if (illegal != null) return "A name cannot contain $illegal"
        return null
    }

    fun overwriteTarget(mode: PickMode, current: Path, name: String): Path? {
        if (mode != PickMode.SAVE_AS) return null
        if (nameError(name) != null) return null
        val candidate = current.resolve(name.trim())
        return candidate.takeIf { runCatching { Files.exists(it) }.getOrDefault(false) }
    }

    fun formatSize(bytes: Long): String = when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> String.format("%.1f KB", bytes / 1024.0)
        bytes < 1024L * 1024 * 1024 -> String.format("%.1f MB", bytes / 1024.0 / 1024.0)
        else -> String.format("%.1f GB", bytes / 1024.0 / 1024.0 / 1024.0)
    }

    private val ILLEGAL_NAME_CHARS = charArrayOf('\\', '/', ':', '*', '?', '"', '<', '>', '|')
}
