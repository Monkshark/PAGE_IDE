package page.language

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.Snapshot

/**
 * Compose state that a language server thread may write to.
 *
 * Language servers answer on their own reader threads, so every write here goes through a mutable
 * snapshot; writing snapshot state from a thread that holds no snapshot is what makes Compose throw
 * "Reading a state that was created after the snapshot was taken".
 */
internal class OffThreadState<T>(initial: T) : MutableState<T> {

    private val backing = mutableStateOf(initial)

    override var value: T
        get() = backing.value
        set(next) = Snapshot.withMutableSnapshot { backing.value = next }

    override fun component1(): T = value

    override fun component2(): (T) -> Unit = { value = it }
}

/** Runs [block] inside a mutable snapshot so writes from a server thread apply atomically. */
internal inline fun <T> offThread(block: () -> T): T = Snapshot.withMutableSnapshot(block)
