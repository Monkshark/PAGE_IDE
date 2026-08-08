package page.editor

import java.nio.file.Files
import java.nio.file.Path

data class TreeNode(
    val path: Path,
    val depth: Int,
    val isDirectory: Boolean,
)

object FileTree {

    private val HEAVY_DIR_NAMES = setOf(
        "build", "out", "dist", "target", "bin",
        "node_modules", "vendor", "bower_components", "jspm_packages",
        ".git", ".svn", ".hg",
        ".gradle", ".idea", ".kotlin", ".dart_tool", ".m2",
        ".cache", ".xwin-cache", ".ccache", "caches",
        ".venv", "venv", "__pycache__", ".mypy_cache", ".pytest_cache", ".tox",
        ".next", ".nuxt", ".svelte-kit", ".angular", ".parcel-cache", ".turbo",
        "tmp", "temp",
    )

    private const val MAX_RECURSIVE_DIRS = 20_000

    fun isGenerated(path: Path, depth: Int): Boolean {
        var current: Path? = path
        var steps = depth
        while (current != null && steps >= 0) {
            if (isHeavyDir(current)) return true
            current = current.parent
            steps--
        }
        return false
    }

    fun isHeavyDir(path: Path): Boolean {
        val name = path.fileName?.toString()?.lowercase() ?: return false
        return name in HEAVY_DIR_NAMES || name.endsWith("-cache") || name.endsWith(".egg-info")
    }

    fun listTree(root: Path, expanded: Set<Path>): List<TreeNode> {
        val result = mutableListOf<TreeNode>()
        val rootIsDir = isDirectorySafe(root)
        result += TreeNode(root, depth = 0, isDirectory = rootIsDir)
        if (rootIsDir && root in expanded) {
            appendChildren(root, depth = 1, expanded, result)
        }
        return result
    }

    fun descendantDirs(dir: Path): Set<Path> {
        if (!isDirectorySafe(dir) || isHeavyDir(dir)) return emptySet()
        val out = LinkedHashSet<Path>()
        val stack = ArrayDeque<Path>()
        stack.addLast(dir)
        while (stack.isNotEmpty() && out.size < MAX_RECURSIVE_DIRS) {
            val cur = stack.removeLast()
            val children = listChildrenSorted(cur) ?: continue
            for (child in children) {
                if (isDirectorySafe(child) && !isHeavyDir(child) && out.add(child)) {
                    stack.addLast(child)
                }
            }
        }
        return out
    }

    fun singleChildChain(dir: Path): Set<Path> {
        if (!isDirectorySafe(dir)) return emptySet()
        val out = mutableSetOf<Path>()
        var cur = dir
        while (true) {
            val children = listChildrenSorted(cur) ?: break
            if (children.size != 1) break
            val only = children[0]
            if (!isDirectorySafe(only)) break
            if (!out.add(only)) break
            cur = only
        }
        return out
    }

    private fun appendChildren(
        dir: Path,
        depth: Int,
        expanded: Set<Path>,
        out: MutableList<TreeNode>,
    ) {
        val children = listChildrenSorted(dir) ?: return
        for (child in children) {
            val isDir = isDirectorySafe(child)
            out += TreeNode(child, depth, isDir)
            if (isDir && child in expanded) {
                appendChildren(child, depth + 1, expanded, out)
            }
        }
    }

    private fun listChildrenSorted(dir: Path): List<Path>? {
        val stream = try {
            Files.list(dir)
        } catch (_: Exception) {
            return null
        }
        return try {
            stream.use { s ->
                s.sorted(compareBy(
                    { !isDirectorySafe(it) },
                    { it.fileName?.toString()?.lowercase() ?: "" },
                )).toList()
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun isDirectorySafe(path: Path): Boolean = try {
        Files.isDirectory(path)
    } catch (_: Exception) {
        false
    }
}
