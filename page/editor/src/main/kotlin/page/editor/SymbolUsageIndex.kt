package page.editor

import java.util.concurrent.CopyOnWriteArrayList

data class FileSymbols(
    val refs: Set<String>,
    val defs: Map<String, Int> = emptyMap(),
    val stamp: Long = 0L,
)

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

    private val perFile = HashMap<String, FileSymbols>()
    private val referers = HashMap<String, HashSet<String>>()
    private val definers = HashMap<String, HashMap<String, Int>>()
    private val listeners = CopyOnWriteArrayList<(SymbolUsageIndex) -> Unit>()
    private val lock = Any()

    fun addListener(listener: (SymbolUsageIndex) -> Unit) {
        listeners += listener
    }

    fun removeListener(listener: (SymbolUsageIndex) -> Unit) {
        listeners -= listener
    }

    fun setFile(rawUri: String, symbols: FileSymbols) {
        val uri = canonicalUsageUri(rawUri)
        val changed = synchronized(lock) {
            val previous = perFile[uri]
            if (previous?.refs == symbols.refs && previous.defs == symbols.defs) return
            if (previous != null) detach(uri, previous)
            if (symbols.refs.isEmpty() && symbols.defs.isEmpty()) {
                perFile.remove(uri)
            } else {
                val kept = if (previous != null) symbols.copy(stamp = previous.stamp) else symbols
                perFile[uri] = kept
                attach(uri, kept)
            }
            true
        }
        if (changed) notifyListeners()
    }

    fun removeFile(rawUri: String) {
        val uri = canonicalUsageUri(rawUri)
        val changed = synchronized(lock) {
            val previous = perFile.remove(uri) ?: return
            detach(uri, previous)
            true
        }
        if (changed) notifyListeners()
    }

    fun replaceAll(byFile: Map<String, FileSymbols>) {
        val changed = synchronized(lock) {
            val next = HashMap<String, FileSymbols>(byFile.size)
            for ((rawUri, symbols) in byFile) {
                if (symbols.refs.isEmpty() && symbols.defs.isEmpty()) continue
                next[canonicalUsageUri(rawUri)] = symbols
            }
            if (next == perFile) return
            perFile.clear()
            referers.clear()
            definers.clear()
            perFile.putAll(next)
            next.forEach { (uri, symbols) -> attach(uri, symbols) }
            true
        }
        if (changed) notifyListeners()
    }

    fun entries(): Map<String, FileSymbols> = synchronized(lock) { HashMap(perFile) }

    fun usedOutside(rawUri: String, name: String): Boolean = synchronized(lock) {
        val files = referers[name] ?: return false
        when (files.size) {
            0 -> false
            1 -> canonicalUsageUri(rawUri) !in files
            else -> true
        }
    }

    fun referencesOf(name: String): Set<String> = synchronized(lock) {
        referers[name]?.toSet() ?: emptySet()
    }

    fun definitionsOf(name: String): Map<String, Int> = synchronized(lock) {
        definers[name]?.toMap() ?: emptyMap()
    }

    fun definedIn(rawUri: String): Map<String, Int> = synchronized(lock) {
        perFile[canonicalUsageUri(rawUri)]?.defs ?: emptyMap()
    }

    fun knows(rawUri: String): Boolean = synchronized(lock) { perFile.containsKey(canonicalUsageUri(rawUri)) }

    fun fileCount(): Int = synchronized(lock) { perFile.size }

    fun nameCount(): Int = synchronized(lock) { referers.size + definers.keys.count { it !in referers } }

    private fun attach(uri: String, symbols: FileSymbols) {
        symbols.refs.forEach { referers.getOrPut(it) { HashSet() } += uri }
        symbols.defs.forEach { (name, offset) -> definers.getOrPut(name) { HashMap() }[uri] = offset }
    }

    private fun detach(uri: String, symbols: FileSymbols) {
        symbols.refs.forEach { name ->
            val files = referers[name] ?: return@forEach
            files -= uri
            if (files.isEmpty()) referers.remove(name)
        }
        symbols.defs.keys.forEach { name ->
            val sites = definers[name] ?: return@forEach
            sites -= uri
            if (sites.isEmpty()) definers.remove(name)
        }
    }

    private fun notifyListeners() {
        listeners.forEach { it(this) }
    }
}
