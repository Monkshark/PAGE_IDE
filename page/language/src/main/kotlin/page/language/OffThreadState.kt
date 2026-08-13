package page.language

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.Snapshot

internal class OffThreadState<T>(initial: T) : MutableState<T> {

    private val backing = mutableStateOf(initial)

    override var value: T
        get() = backing.value
        set(next) = Snapshot.withMutableSnapshot { backing.value = next }

    override fun component1(): T = value

    override fun component2(): (T) -> Unit = { value = it }
}

internal inline fun <T> offThread(block: () -> T): T = Snapshot.withMutableSnapshot(block)
