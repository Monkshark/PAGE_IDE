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
}
