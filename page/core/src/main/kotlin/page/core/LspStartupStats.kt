package page.core

import java.util.concurrent.ConcurrentHashMap

object LspStartupStats {

    data class Entry(
        val backendId: String,
        val displayName: String,
        val origin: String,
        val resolveMs: Long,
        val spawnMs: Long,
        val initializeMs: Long,
    ) {
        val totalMs: Long get() = resolveMs + spawnMs + initializeMs

        fun line(): String =
            "$displayName ready in ${totalMs}ms " +
                "(resolve ${resolveMs}ms · spawn ${spawnMs}ms · initialize ${initializeMs}ms · $origin)"
    }

    @Volatile
    var onRecord: ((Entry) -> Unit)? = null

    private val entries = ConcurrentHashMap<String, Entry>()

    fun record(entry: Entry) {
        entries[entry.backendId] = entry
        onRecord?.invoke(entry)
    }

    fun snapshot(): List<Entry> = entries.values.sortedByDescending { it.totalMs }

    fun clear() {
        entries.clear()
    }

    fun table(): String {
        val rows = snapshot()
        if (rows.isEmpty()) return "[lsp startup] nothing started yet"
        val nameWidth = rows.maxOf { it.displayName.length }
        val header = "  ${"language".padEnd(nameWidth)}  " +
            "${"total".padStart(8)}${"resolve".padStart(9)}${"spawn".padStart(8)}${"init".padStart(9)}  origin"
        val body = rows.joinToString("\n") { e ->
            "  ${e.displayName.padEnd(nameWidth)}  " +
                "${(e.totalMs.toString() + "ms").padStart(8)}" +
                "${(e.resolveMs.toString() + "ms").padStart(9)}" +
                "${(e.spawnMs.toString() + "ms").padStart(8)}" +
                "${(e.initializeMs.toString() + "ms").padStart(9)}  ${e.origin}"
        }
        val slowest = rows.first()
        return "[lsp startup] ${rows.size} server(s), slowest ${slowest.displayName} ${slowest.totalMs}ms\n" +
            "$header\n$body"
    }
}
