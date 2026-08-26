package page.language

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.runtime.snapshots.SnapshotStateMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import page.lsp.Diagnostic
import page.lsp.LanguageBackend
import page.lsp.LanguageRegistry
import page.lsp.LspBackends
import page.lsp.RenameWorkspaceEdit
import page.runtime.PageRuntimeEnv
import java.nio.file.Path

class LspRouter(
    private val workspaceRoot: Path?,
    private val parentScope: CoroutineScope,
) {
    private val controllers = java.util.concurrent.ConcurrentHashMap<String, LspController>()
    private val deleting = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()

    @Volatile
    var applyEditHandler: ((RenameWorkspaceEdit) -> Boolean)? = null
        set(value) {
            field = value
            synchronized(this) { controllers.values.forEach { it.applyEditHandler = value } }
        }

    @Synchronized
    fun controllerFor(path: Path): LspController? {
        val backend = LspBackends.forFile(path, workspaceRoot) ?: return null
        if (backend.id in deleting) return controllers[backend.id]
        return controllers.getOrPut(backend.id) { newController(backend) }
    }

    /**
     * A controller owns Compose state, and prewarming builds one on an IO thread. State objects born
     * outside a mutable snapshot are invisible to the composition that later reads them, so the
     * construction runs inside one.
     */
    private fun newController(backend: LanguageBackend): LspController = Snapshot.withMutableSnapshot {
        val scope = CoroutineScope(SupervisorJob(parentScope.coroutineContext[Job]) + Dispatchers.Default)
        LspController(workspaceRoot, scope).also {
            it.applyEditHandler = applyEditHandler
            it.ensureStarted(backend)
        }
    }

    fun existingControllerFor(path: Path): LspController? {
        val backend = LspBackends.forFile(path, workspaceRoot) ?: return null
        return controllers[backend.id]
    }

    fun prewarm(backendId: String): Boolean {
        if (backendId in deleting) return false
        if (controllers.containsKey(backendId)) return true
        val backend = LspBackends.byId(backendId) ?: return false
        val env = HashMap(System.getenv())
        PageRuntimeEnv.applyTo(env)
        if (backend.resolveExecutable(env) !is LanguageBackend.Resolution.Found) return false
        synchronized(this) {
            if (backendId in deleting) return false
            controllers.getOrPut(backend.id) { newController(backend) }
        }
        return true
    }

    fun prewarmWorkspace() {
        val root = workspaceRoot ?: return
        parentScope.launch(Dispatchers.IO) {
            val dominant = runCatching { dominantBackendId(root) }.getOrNull() ?: return@launch
            if (prewarm(dominant)) println("[lsp] prewarming $dominant for $root")
        }
    }

    internal fun dominantBackendId(root: Path): String? {
        val counts = HashMap<String, Int>()
        java.nio.file.Files.walk(root, PREWARM_SCAN_DEPTH).use { stream ->
            stream
                .filter { java.nio.file.Files.isRegularFile(it) }
                .filter { path ->
                    root.relativize(path).none { it.toString() in PREWARM_EXCLUDES }
                }
                .limit(PREWARM_SCAN_LIMIT)
                .forEach { path ->
                    val id = LspBackends.forFile(path, root)?.id ?: return@forEach
                    counts[id] = (counts[id] ?: 0) + 1
                }
        }
        return counts.maxByOrNull { it.value }?.key
    }

    fun backendFor(path: Path): LanguageBackend? = LspBackends.forFile(path, workspaceRoot)

    fun languageIdFor(path: Path): String? = backendFor(path)?.lspLanguageId

    @Synchronized
    fun controllerById(id: String): LspController? = controllers[id]

    fun beginLanguageDelete(id: String) {
        val ctrl = synchronized(this) {
            deleting += id
            controllers.remove(id)
        }
        ctrl?.shutdownNow()
    }

    fun endLanguageDelete(id: String) {
        synchronized(this) { deleting -= id }
    }

    fun refreshForExtensions(extensions: List<String>, reason: String) {
        val cold = mutableListOf<String>()
        for (id in backendIdsForExtensions(extensions)) {
            val running = controllers[id]
            if (running != null) running.restart(reason) else cold += id
        }
        if (cold.isEmpty()) return
        parentScope.launch(Dispatchers.IO) {
            for (id in cold) if (prewarm(id)) println("[lsp] prewarming $id after $reason")
        }
    }

    val allDiagnosticsByUri: Map<String, List<Diagnostic>>
        get() = controllers.values
            .flatMap { it.diagnosticsByUri.entries }
            .associate { it.key to it.value }

    fun controllerForUri(uri: String): LspController? {
        if (!uri.startsWith("file:")) return null
        val path = runCatching { java.nio.file.Paths.get(java.net.URI(uri)) }.getOrNull() ?: return null
        return controllerFor(path)
    }

    val startingActivities: List<LspController.Activity>
        get() = controllers.entries
            .filter { it.value.status.value == LspController.Status.STARTING }
            .map { (id, ctrl) ->
                LspController.Activity(
                    kind = "startup",
                    label = id,
                    startedAtMs = ctrl.startedAtMs,
                )
            }

    fun applyExternalChange(uri: String, newText: String) {
        controllerForUri(uri)?.applyExternalChange(uri, newText)
    }

    fun notifyFilesRenamed(moves: List<Pair<Path, Path>>) {
        val affected = mutableSetOf<String>()
        for ((old, new) in moves) {
            LspBackends.forFile(old, workspaceRoot)?.let { affected += it.id }
            LspBackends.forFile(new, workspaceRoot)?.let { affected += it.id }
        }
        for (id in affected) {
            controllerById(id)?.notifyFilesRenamed(moves)
        }
    }

    @Synchronized
    fun shutdown() {
        controllers.values.forEach { it.shutdown() }
        controllers.clear()
    }

    companion object {
        private const val PREWARM_SCAN_DEPTH = 12
        private const val PREWARM_SCAN_LIMIT = 3000L
        private val PREWARM_EXCLUDES = setOf(
            "build", "out", "dist", "target", "bin", "node_modules", "vendor",
            ".git", ".gradle", ".idea", ".kotlin", ".dart_tool", ".venv", "venv",
        )

        fun backendIdsForExtensions(extensions: List<String>): Set<String> =
            extensions.flatMap { ext -> LspBackends.allForExtension(ext).map { it.id } }.toSet()
    }
}

@Composable
fun rememberLspRouter(workspaceRoot: Path?): LspRouter {
    val router = remember(workspaceRoot) {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        LspRouter(workspaceRoot, scope)
    }
    DisposableEffect(router) {
        onDispose { router.shutdown() }
    }
    return router
}
