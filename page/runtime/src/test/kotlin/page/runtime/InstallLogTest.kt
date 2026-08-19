package page.runtime

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class InstallLogTest {

    private val id = "install-log-test"

    @AfterTest
    fun tidy() {
        InstallProgressRegistry.clearLog(id)
    }

    @Test
    fun `a log with nothing written to it reads empty`() {
        assertEquals(emptyList(), InstallProgressRegistry.log(id))
    }

    @Test
    fun `the log outlives the dialog that started it`() {
        InstallProgressRegistry.startLog(id, "> Installing Rust Toolchain 1.96.0")
        InstallProgressRegistry.appendLog(id, "> GET https://example/rust.tar.gz")
        InstallProgressRegistry.appendLog(id, "> Downloading… 18.8 MB")

        assertEquals(
            listOf(
                "> Installing Rust Toolchain 1.96.0",
                "> GET https://example/rust.tar.gz",
                "> Downloading… 18.8 MB",
            ),
            InstallProgressRegistry.log(id),
        )
    }

    @Test
    fun `starting again drops what the last run wrote`() {
        InstallProgressRegistry.startLog(id, "> first run")
        InstallProgressRegistry.appendLog(id, "> noise")
        InstallProgressRegistry.startLog(id, "> second run")

        assertEquals(listOf("> second run"), InstallProgressRegistry.log(id))
    }

    @Test
    fun `progress overwrites the line before it`() {
        InstallProgressRegistry.startLog(id, "> Installing")
        InstallProgressRegistry.appendLog(id, "> Downloading… 10%")
        InstallProgressRegistry.replaceLastLog(id, "> Downloading… 20%")
        InstallProgressRegistry.replaceLastLog(id, "> Downloading… 30%")

        assertEquals(listOf("> Installing", "> Downloading… 30%"), InstallProgressRegistry.log(id))
    }

    @Test
    fun `replacing into an empty log just writes the line`() {
        InstallProgressRegistry.replaceLastLog(id, "> only line")
        assertEquals(listOf("> only line"), InstallProgressRegistry.log(id))
    }

    @Test
    fun `the log stays bounded`() {
        InstallProgressRegistry.startLog(id, "> head")
        repeat(2100) { InstallProgressRegistry.appendLog(id, "line $it") }

        val log = InstallProgressRegistry.log(id)
        assertEquals(2000, log.size)
        assertEquals("line 2099", log.last())
        assertTrue(log.none { it == "> head" }, "the oldest lines should have gone")
    }

    @Test
    fun `a whole log can be swapped in at once`() {
        InstallProgressRegistry.setLog(id, listOf("a", "b", "c"))
        assertEquals(listOf("a", "b", "c"), InstallProgressRegistry.log(id))
    }

    @Test
    fun `finishing an install keeps the log for a reopened dialog`() {
        InstallProgressRegistry.start(id, "Test Installer")
        InstallProgressRegistry.startLog(id, "> Installing")
        InstallProgressRegistry.appendLog(id, "> Installed at C:\\somewhere")
        InstallProgressRegistry.finish(id)

        assertEquals(null, InstallProgressRegistry.get(id), "the in-flight entry goes")
        assertEquals(2, InstallProgressRegistry.log(id).size, "its log does not")
    }
}
