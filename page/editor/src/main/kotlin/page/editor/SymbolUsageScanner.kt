package page.editor

import page.shared.syntax.SymbolNames
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.isRegularFile

object SymbolUsageScanner {

    private const val MAX_FILE_BYTES = 512L * 1024

    private val EXCLUDED_DIRS = setOf(
        "build", "out", "dist", "target", "bin", "node_modules", "vendor",
        ".git", ".gradle", ".idea", ".kotlin", ".dart_tool", ".venv", "venv", "__pycache__",
    )

    fun namesIn(text: String): Set<String> = SymbolNames.distinctIn(text)

    fun scanFile(path: Path): Set<String> {
        if (!path.isRegularFile()) return emptySet()
        if (SyntaxLexers.forPath(path) == null) return emptySet()
        val size = try { Files.size(path) } catch (e: Exception) { return emptySet() }
        if (size > MAX_FILE_BYTES) return emptySet()
        val text = try { Files.readString(path) } catch (e: Exception) { return emptySet() }
        return namesIn(text)
    }

    fun scanWorkspace(
        root: Path,
        previous: Map<String, FileNames> = emptyMap(),
        limit: Int = 4000,
    ): Map<String, FileNames> {
        if (!Files.isDirectory(root)) return emptyMap()
        val out = HashMap<String, FileNames>()
        Files.walk(root).use { stream ->
            for (path in stream) {
                if (out.size >= limit) break
                if (!path.isRegularFile()) continue
                if (isExcluded(root, path)) continue
                if (SyntaxLexers.forPath(path) == null) continue
                val stamp = stampOf(path)
                val uri = canonicalUsageUri(path.toUri().toString())
                val cached = previous[uri]
                val names = if (cached != null && cached.stamp == stamp && stamp != 0L) {
                    cached.names
                } else {
                    scanFile(path)
                }
                if (names.isNotEmpty()) out[uri] = FileNames(names, stamp)
            }
        }
        return out
    }

    private fun stampOf(path: Path): Long = try {
        Files.getLastModifiedTime(path).toMillis()
    } catch (e: Exception) {
        0L
    }

    private fun isExcluded(root: Path, path: Path): Boolean = runCatching {
        root.relativize(path).any { it.toString() in EXCLUDED_DIRS }
    }.getOrDefault(true)
}
