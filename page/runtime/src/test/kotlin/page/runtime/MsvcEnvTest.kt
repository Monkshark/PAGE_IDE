package page.runtime

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MsvcEnvTest {

    @Test
    fun `parseSetOutput extracts requested keys case-insensitively`() {
        val text = """
            Path=C:\a;C:\b
            INCLUDE=C:\vc\include;C:\sdk\include
            LIB=C:\vc\lib
            RANDOM=ignore-me
        """.trimIndent()
        val r = MsvcEnv.parseSetOutput(text, listOf("INCLUDE", "LIB", "PATH"))
        assertEquals("C:\\vc\\include;C:\\sdk\\include", r["INCLUDE"])
        assertEquals("C:\\vc\\lib", r["LIB"])
        assertEquals("C:\\a;C:\\b", r["PATH"])
        assertNull(r["RANDOM"])
    }

    @Test
    fun `parseSetOutput ignores lines without an equals sign`() {
        val r = MsvcEnv.parseSetOutput("no-equals-line\nINCLUDE=x", listOf("INCLUDE"))
        assertEquals(mapOf("INCLUDE" to "x"), r)
    }

    @Test
    fun `parseSetOutput preserves values that contain equals signs`() {
        val r = MsvcEnv.parseSetOutput("LIB=a=b;c", listOf("LIB"))
        assertEquals("a=b;c", r["LIB"])
    }

    @Test
    fun `parseSetOutput returns empty when no requested keys present`() {
        val r = MsvcEnv.parseSetOutput("FOO=1\nBAR=2", listOf("INCLUDE"))
        assertEquals(emptyMap(), r)
    }
}
