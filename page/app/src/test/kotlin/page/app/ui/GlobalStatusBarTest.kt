package page.app.ui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GlobalStatusBarTest {

    @Test
    fun `java ext maps to jdk with matching version`() {
        val info = runtimeInfoFor(
            activeExt = "java",
            runtimeVersions = mapOf("java" to "21.0.2"),
            runtimeSources = mapOf("java" to "managed"),
            runtimeBuildFileVersions = mapOf("java" to "21"),
        )
        assertEquals("JDK 21.0.2", info?.first)
        assertEquals("jdk", info?.second)
        assertEquals("from managed", info?.third)
    }

    @Test
    fun `build-file version mismatch adds warning label and explanatory tooltip`() {
        val info = runtimeInfoFor(
            activeExt = "java",
            runtimeVersions = mapOf("java" to "17.0.1"),
            runtimeSources = mapOf("java" to "system"),
            runtimeBuildFileVersions = mapOf("java" to "21"),
        )
        assertTrue(info!!.first.contains("⚠"), "mismatch should flag the label")
        assertEquals("Project requires 21 (system), using 17.0.1", info.third)
    }

    @Test
    fun `an absent toolchain says so instead of borrowing a version`() {
        val info = runtimeInfoFor(
            activeExt = "rs",
            runtimeVersions = emptyMap(),
            runtimeSources = emptyMap(),
            runtimeBuildFileVersions = emptyMap(),
        )
        assertEquals("Rust (not installed)", info?.first)
        assertEquals("rust-runtime", info?.second)
        assertEquals("Click to install", info?.third)
    }

    @Test
    fun `an absent toolchain still reports what the project wants`() {
        val info = runtimeInfoFor(
            activeExt = "go",
            runtimeVersions = emptyMap(),
            runtimeSources = mapOf("go" to "go.mod"),
            runtimeBuildFileVersions = mapOf("go" to "1.22"),
        )
        assertEquals("Go (not installed)", info?.first)
        assertEquals("Project requires 1.22 (go.mod) — click to install", info?.third)
    }

    @Test
    fun `a newer toolchain than the project floor is not a warning`() {
        val info = runtimeInfoFor(
            activeExt = "go",
            runtimeVersions = mapOf("go" to "1.26.5"),
            runtimeSources = mapOf("go" to "go.mod"),
            runtimeBuildFileVersions = mapOf("go" to "1.22"),
        )
        assertEquals("Go 1.26.5", info?.first)
        assertEquals("from go.mod", info?.third)
    }

    @Test
    fun `version order compares numerically not as text`() {
        assertTrue(isOlderThan("1.9.0", "1.22"), "1.9 is below the 1.22 floor")
        assertFalse(isOlderThan("1.26.5", "1.22"))
        assertFalse(isOlderThan("21.0.2", "21"))
        assertTrue(isOlderThan("17.0.1", "21"))
        assertFalse(isOlderThan("21-ea", "21"))
    }

    @Test
    fun `unrecognized extension yields no runtime info`() {
        val info = runtimeInfoFor(
            activeExt = "txt",
            runtimeVersions = mapOf("java" to "21"),
            runtimeSources = emptyMap(),
            runtimeBuildFileVersions = emptyMap(),
        )
        assertNull(info)
    }
}
