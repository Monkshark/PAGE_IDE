package page.editor

import page.shared.syntax.SymbolNames
import page.shared.syntax.SymbolScan
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.isRegularFile

object SymbolUsageScanner {

    private const val MAX_FILE_BYTES = 512L * 1024

    private val EXCLUDED_DIRS = setOf(
        "build", "out", "dist", "target", "bin", "node_modules", "vendor",
        ".git", ".gradle", ".idea", ".kotlin", ".dart_tool", ".venv", "venv", "__pycache__",
    )

    fun symbolsIn(text: String): SymbolScan = SymbolNames.scan(text)

    fun scanFile(path: Path): SymbolScan {
        if (!path.isRegularFile()) return EMPTY
        if (SyntaxLexers.forPath(path) == null) return EMPTY
        val size = try { Files.size(path) } catch (e: Exception) { return EMPTY }
        if (size > MAX_FILE_BYTES) return EMPTY
        val text = try { Files.readString(path) } catch (e: Exception) { return EMPTY }
        return symbolsIn(text)
    }

    fun scanWorkspace(
        root: Path,
        previous: Map<String, FileSymbols> = emptyMap(),
        limit: Int = 4000,
    ): Map<String, FileSymbols> {
        if (!Files.isDirectory(root)) return emptyMap()
        val out = HashMap<String, FileSymbols>()
        Files.walk(root).use { stream ->
            for (path in stream) {
                if (out.size >= limit) break
                if (!path.isRegularFile()) continue
                if (isExcluded(root, path)) continue
                if (SyntaxLexers.forPath(path) == null) continue
                val stamp = stampOf(path)
                val uri = canonicalUsageUri(path.toUri().toString())
                val cached = previous[uri]
                if (cached != null && cached.stamp == stamp && stamp != 0L) {
                    out[uri] = cached
                    continue
                }
                val scan = scanFile(path)
                if (scan.refs.isNotEmpty() || scan.defs.isNotEmpty()) {
                    out[uri] = FileSymbols(scan.refs, scan.defs, stamp)
                }
            }
        }
        return out
    }

    private val EMPTY = SymbolScan(emptySet(), emptyMap())

    private fun stampOf(path: Path): Long = try {
        Files.getLastModifiedTime(path).toMillis()
    } catch (e: Exception) {
        0L
    }

    private fun isExcluded(root: Path, path: Path): Boolean = runCatching {
        root.relativize(path).any { it.toString() in EXCLUDED_DIRS }
    }.getOrDefault(true)
}
