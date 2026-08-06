package page.editor

import java.util.concurrent.CopyOnWriteArrayList

class SymbolUsageIndex {

    private val perFile = HashMap<String, Set<String>>()
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
        val uri = canonical(rawUri)
        val changed = synchronized(lock) {
            val previous = perFile[uri]
            if (previous == names) return
            previous?.forEach { release(it) }
            if (names.isEmpty()) perFile.remove(uri) else perFile[uri] = names
            names.forEach { retain(it) }
            true
        }
        if (changed) notifyListeners()
    }

    fun removeFile(rawUri: String) {
        val uri = canonical(rawUri)
        val changed = synchronized(lock) {
            val previous = perFile.remove(uri) ?: return
            previous.forEach { release(it) }
            true
        }
        if (changed) notifyListeners()
    }

    fun replaceAll(byFile: Map<String, Set<String>>) {
        synchronized(lock) {
            perFile.clear()
            fileCounts.clear()
            for ((rawUri, names) in byFile) {
                if (names.isEmpty()) continue
                perFile[canonical(rawUri)] = names
                names.forEach { retain(it) }
            }
        }
        notifyListeners()
    }

    fun usedOutside(rawUri: String, name: String): Boolean = synchronized(lock) {
        val total = fileCounts[name] ?: return false
        val here = if (perFile[canonical(rawUri)]?.contains(name) == true) 1 else 0
        total - here > 0
    }

    fun knows(rawUri: String): Boolean = synchronized(lock) { perFile.containsKey(canonical(rawUri)) }

    private fun canonical(uri: String): String {
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
