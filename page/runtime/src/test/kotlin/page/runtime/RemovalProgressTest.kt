package page.runtime

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RemovalProgressTest {

    private val id = "removal-test"

    @AfterTest
    fun tidy() {
        InstallProgressRegistry.removals.keys.toList().forEach { key ->
            val parts = key.split("/", limit = 2)
            if (parts.size == 2) InstallProgressRegistry.finishRemoval(parts[0], parts[1])
        }
    }

    @Test
    fun `nothing is being removed to begin with`() {
        assertNull(InstallProgressRegistry.removalOf(id, "1.0.0"))
        assertFalse(InstallProgressRegistry.isRemoving(id))
    }

    @Test
    fun `a removal is visible to anyone who asks, not just the panel that started it`() {
        InstallProgressRegistry.startRemoval(id, "1.0.0")
        InstallProgressRegistry.updateRemoval(id, "1.0.0", 0.42f)

        assertEquals(0.42f, InstallProgressRegistry.removalOf(id, "1.0.0")?.fraction)
        assertTrue(InstallProgressRegistry.isRemoving(id))
        assertEquals(1, InstallProgressRegistry.removals.size)
    }

    @Test
    fun `two versions of one tool are tracked apart`() {
        InstallProgressRegistry.startRemoval(id, "1.0.0")
        InstallProgressRegistry.startRemoval(id, "2.0.0")
        InstallProgressRegistry.updateRemoval(id, "2.0.0", 0.9f)

        assertEquals(0f, InstallProgressRegistry.removalOf(id, "1.0.0")?.fraction)
        assertEquals(0.9f, InstallProgressRegistry.removalOf(id, "2.0.0")?.fraction)

        InstallProgressRegistry.finishRemoval(id, "2.0.0")
        assertNull(InstallProgressRegistry.removalOf(id, "2.0.0"))
        assertTrue(InstallProgressRegistry.isRemoving(id), "the other one is still going")
    }

    @Test
    fun `progress is kept inside its range`() {
        InstallProgressRegistry.startRemoval(id, "1.0.0")
        InstallProgressRegistry.updateRemoval(id, "1.0.0", 4f)
        assertEquals(1f, InstallProgressRegistry.removalOf(id, "1.0.0")?.fraction)
        InstallProgressRegistry.updateRemoval(id, "1.0.0", -2f)
        assertEquals(0f, InstallProgressRegistry.removalOf(id, "1.0.0")?.fraction)
    }

    @Test
    fun `an update for a removal that never started is ignored`() {
        InstallProgressRegistry.updateRemoval(id, "9.9.9", 0.5f)
        assertNull(InstallProgressRegistry.removalOf(id, "9.9.9"))
    }
}
