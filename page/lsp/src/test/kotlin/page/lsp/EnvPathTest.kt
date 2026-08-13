package page.lsp

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class EnvPathTest {

    @Test
    fun `finds the upper case spelling`() {
        assertEquals("/usr/bin", searchPathOf(mapOf("PATH" to "/usr/bin")))
    }

    @Test
    fun `finds the windows spelling`() {
        assertEquals("C:\\bin", searchPathOf(mapOf("Path" to "C:\\bin")))
    }

    @Test
    fun `finds an all lower case spelling`() {
        assertEquals("/opt/bin", searchPathOf(mapOf("path" to "/opt/bin")))
    }

    @Test
    fun `ignores variables that merely mention path`() {
        val env = mapOf("CLASSPATH" to "a.jar", "JAVA_PATH" to "/jdk", "PATHEXT" to ".EXE")
        assertNull(searchPathOf(env))
    }

    @Test
    fun `an environment without a path has none`() {
        assertNull(searchPathOf(emptyMap()))
    }

    private val sep = System.getProperty("path.separator") ?: ":"

    @Test
    fun `a repeated directory is probed once`() {
        val path = listOf("/opt/git/cmd", "/opt/nvm", "/opt/git/cmd").joinToString(sep)
        assertEquals(listOf("/opt/git/cmd", "/opt/nvm"), searchPathEntries(path))
    }

    @Test
    fun `a trailing separator does not make a second directory`() {
        assertEquals(listOf("/opt/git/cmd"), searchPathEntries(listOf("/opt/git/cmd/", "/opt/git/cmd").joinToString(sep)))
        assertEquals(listOf("\\\\build\\tools\\bin"), searchPathEntries("\\\\build\\tools\\bin\\"))
    }

    @Test
    fun `blank and empty entries are dropped`() {
        val path = listOf("", "  ", "/usr/bin").joinToString(sep)
        assertEquals(listOf("/usr/bin"), searchPathEntries(path))
    }

    @Test
    fun `an absent path yields nothing to probe`() {
        assertEquals(emptyList(), searchPathEntries(null))
        assertEquals(emptyList(), searchPathEntries("   "))
    }
}
