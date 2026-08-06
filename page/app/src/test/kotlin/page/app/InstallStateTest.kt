package page.app

import kotlin.test.Test
import kotlin.test.assertEquals

class InstallStateTest {

    @Test
    fun everyChangeAdvancesTheRevision() {
        val before = InstallState.revision
        InstallState.changed()
        InstallState.changed()
        assertEquals(before + 2, InstallState.revision)
    }
}
