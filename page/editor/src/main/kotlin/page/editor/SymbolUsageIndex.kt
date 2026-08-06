package page.editor

import java.util.concurrent.CopyOnWriteArrayList

data class FileNames(val names: Set<String>, val stamp: Long = 0L)

fun canonicalUsageUri(uri: String): String {
    val prefix = "file:///"
    val normalized = if (uri.startsWith("file:/") && !uri.startsWith(prefix)) {
        prefix + uri.removePrefix("file:/").trimStart('/')
    } else {
        uri
    }
    val body = normalized.removePrefix(prefix)
    if (normalized.length == body.length) return normalized
    if (body.length < 2 || body[1] != ':') return normalized
    return prefix + body[0].lowercaseChar() + body.substring(1)
}

class SymbolUsageIndex {

    private val perFile = HashMap<String, FileNames>()
    private val fileCounts = HashMap<String, Int>()
    private val listeners = CopyOnWriteArrayList<(SymbolUsageIndex) -> Unit>()
    private val lock = Any()

    fun addListener(listener: (SymbolUsageIndex) -> Unit) {
        listeners += listener
    }

    fun removeListener(listener: (SymbolUsageIndex) -> Unit) {
        listeners -= listener
    }

    fun setFile(rawUri: String, names: Set<String>) {
        val uri = canonicalUsageUri(rawUri)
        val changed = synchronized(lock) {
            val previous = perFile[uri]
            if (previous?.names == names) return
            previous?.names?.forEach { release(it) }
            if (names.isEmpty()) {
                perFile.remove(uri)
            } else {
                perFile[uri] = FileNames(names, previous?.stamp ?: 0L)
                names.forEach { retain(it) }
            }
            true
        }
        if (changed) notifyListeners()
    }

    fun removeFile(rawUri: String) {
        val uri = canonicalUsageUri(rawUri)
        val changed = synchronized(lock) {
            val previous = perFile.remove(uri) ?: return
            previous.names.forEach { release(it) }
            true
        }
        if (changed) notifyListeners()
    }

    fun replaceAll(byFile: Map<String, FileNames>) {
        val changed = synchronized(lock) {
            val next = HashMap<String, FileNames>(byFile.size)
            for ((rawUri, entry) in byFile) {
                if (entry.names.isEmpty()) continue
                next[canonicalUsageUri(rawUri)] = entry
            }
            if (next == perFile) return
            perFile.clear()
            fileCounts.clear()
            perFile.putAll(next)
            next.values.forEach { entry -> entry.names.forEach { retain(it) } }
            true
        }
        if (changed) notifyListeners()
    }

    fun entries(): Map<String, FileNames> = synchronized(lock) { HashMap(perFile) }

    fun usedOutside(rawUri: String, name: String): Boolean = synchronized(lock) {
        val total = fileCounts[name] ?: return false
        val here = if (perFile[canonicalUsageUri(rawUri)]?.names?.contains(name) == true) 1 else 0
        total - here > 0
    }

    fun knows(rawUri: String): Boolean = synchronized(lock) { perFile.containsKey(canonicalUsageUri(rawUri)) }

    fun fileCount(): Int = synchronized(lock) { perFile.size }

    fun nameCount(): Int = synchronized(lock) { fileCounts.size }

    private fun retain(name: String) {
        fileCounts[name] = (fileCounts[name] ?: 0) + 1
    }

    private fun release(name: String) {
        val next = (fileCounts[name] ?: 0) - 1
        if (next <= 0) fileCounts.remove(name) else fileCounts[name] = next
    }

    private fun notifyListeners() {
        listeners.forEach { it(this) }
    }
}
