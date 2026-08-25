package page.runtime

import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class InstallPrerequisiteTest {

    private class Bare(override val languageId: String, private val needs: List<String>) : LspInstaller {
        override val displayName: String = languageId
        override val precheck: LspInstaller.Precheck = LspInstaller.Precheck.Ok
        override fun prerequisites(): List<String> = needs
        override fun isInstalled(): Boolean = false
        override fun executable(): Path? = null
        override fun install(version: String?, onProgress: (LspInstaller.Progress) -> Unit) = Unit
    }

    @Test
    fun `most installers need nothing before them`() {
        assertEquals(emptyList(), Bare("plain", emptyList()).prerequisites())
        assertEquals(emptyList(), NodeInstaller().prerequisites())
    }

    @Test
    fun `swift on windows cannot link without the windows sdk`() {
        assertEquals(listOf("windows-sdk"), SwiftToolchainInstaller(isWindows = true).prerequisites())
    }

    @Test
    fun `swift elsewhere brings its own libc`() {
        assertEquals(emptyList(), SwiftToolchainInstaller(isWindows = false).prerequisites())
    }

    @Test
    fun `clangd on windows needs libc headers, on other systems it does not`() {
        val clangd = LspInstallers.forId("c") as GitHubReleaseInstaller
        assertEquals(listOf("mingw-toolchain"), clangd.descriptor.prerequisites["windows"])
        assertEquals(null, clangd.descriptor.prerequisites["linux"])
        assertEquals(null, clangd.descriptor.prerequisites["macos"])
        assertEquals(clangd.descriptor, (LspInstallers.forId("cpp") as GitHubReleaseInstaller).descriptor)
    }

    @Test
    fun `a prerequisite nobody can build is dropped rather than crashing the install`() {
        assertEquals(emptyList(), LspInstallers.missingPrerequisitesOf(Bare("odd", listOf("no-such-installer"))))
    }

    @Test
    fun `what is already there is not installed twice`() {
        val missing = LspInstallers.missingPrerequisitesOf(Bare("odd", listOf("node", "python-runtime")))
        assertTrue(missing.none { it.isInstalled() }, "an installed prerequisite must not be queued again")
    }

    @Test
    fun `the order asked for is the order installed`() {
        val missing = LspInstallers.missingPrerequisitesOf(Bare("odd", listOf("windows-sdk", "mingw-toolchain")))
        val ids = missing.map { it.languageId }
        assertEquals(ids.sortedBy { listOf("windows-sdk", "mingw-toolchain").indexOf(it) }, ids)
    }
}
