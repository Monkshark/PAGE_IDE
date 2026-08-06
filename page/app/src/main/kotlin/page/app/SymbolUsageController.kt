package page.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import page.editor.SymbolUsageIndex
import page.editor.SymbolUsageScanner
import java.nio.file.Path

class SymbolUsageController(
    val workspaceRoot: Path?,
    private val scope: CoroutineScope,
) {
    val index = SymbolUsageIndex()
    private val revisionState = mutableStateOf(0)
    val revision: State<Int> get() = revisionState
    private val listener: (SymbolUsageIndex) -> Unit = { revisionState.value = revisionState.value + 1 }
    private val perFileDebounce = HashMap<String, Job>()
    private val scanLock = Mutex()
    private var refreshJob: Job? = null
    private var sweepJob: Job? = null

    init {
        index.addListener(listener)
    }

    fun scanWorkspaceAsync() {
        val root = workspaceRoot ?: return
        scope.launch(Dispatchers.IO) {
            delay(SCAN_START_DELAY_MS)
            rescan(root)
            startSweep(root)
        }
    }

    fun refreshWorkspaceAsync() {
        val root = workspaceRoot ?: return
        refreshJob?.cancel()
        refreshJob = scope.launch(Dispatchers.IO) {
            delay(REFRESH_DEBOUNCE_MS)
            rescan(root)
        }
    }

    fun updateFile(path: Path, text: String) {
        val uri = path.toUri().toString()
        perFileDebounce[uri]?.cancel()
        perFileDebounce[uri] = scope.launch(Dispatchers.Default) {
            delay(UPDATE_DEBOUNCE_MS)
            index.setFile(uri, SymbolUsageScanner.namesIn(text))
        }
    }

    fun removeFile(path: Path) {
        val uri = path.toUri().toString()
        perFileDebounce.remove(uri)?.cancel()
        index.removeFile(uri)
    }

    fun usedElsewhere(path: Path?): (String) -> Boolean {
        val uri = path?.toUri()?.toString() ?: return { true }
        if (!index.knows(uri)) return { true }
        return { name -> index.usedOutside(uri, name) }
    }

    fun shutdown() {
        index.removeListener(listener)
        scope.cancel()
    }

    private suspend fun rescan(root: Path) = scanLock.withLock {
        index.replaceAll(SymbolUsageScanner.scanWorkspace(root, index.entries()))
    }

    private fun startSweep(root: Path) {
        if (sweepJob?.isActive == true) return
        sweepJob = scope.launch(Dispatchers.IO) {
            while (isActive) {
                delay(SWEEP_INTERVAL_MS)
                rescan(root)
            }
        }
    }

    private companion object {
        const val SCAN_START_DELAY_MS = 2_500L
        const val UPDATE_DEBOUNCE_MS = 250L
        const val REFRESH_DEBOUNCE_MS = 800L
        const val SWEEP_INTERVAL_MS = 120_000L
    }
}

@Composable
fun rememberSymbolUsageController(workspaceRoot: Path?): SymbolUsageController {
    val controller = remember(workspaceRoot) {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        SymbolUsageController(workspaceRoot, scope)
    }
    DisposableEffect(controller) {
        onDispose { controller.shutdown() }
    }
    return controller
}
