package page.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

object InstallState {
    var revision by mutableStateOf(0)
        private set

    fun changed() {
        revision++
    }
}
