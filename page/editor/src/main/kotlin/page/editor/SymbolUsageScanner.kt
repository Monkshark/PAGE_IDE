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

    fun scanWorkspace(root: Path, limit: Int = 4000): Map<String, Set<String>> {
        if (!Files.isDirectory(root)) return emptyMap()
        val out = HashMap<String, Set<String>>()
        Files.walk(root).use { stream ->
            for (path in stream) {
                if (out.size >= limit) break
                if (!path.isRegularFile()) continue
                if (isExcluded(root, path)) continue
                val names = scanFile(path)
                if (names.isNotEmpty()) out[path.toUri().toString()] = names
            }
        }
        return out
    }

    private fun isExcluded(root: Path, path: Path): Boolean = runCatching {
        root.relativize(path).any { it.toString() in EXCLUDED_DIRS }
    }.getOrDefault(true)
}
