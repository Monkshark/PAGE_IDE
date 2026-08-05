package page.runtime

import kotlin.test.Test
import kotlin.test.assertEquals

class PageRuntimeEnvMsvcTest {

    @Test
    fun `mergePathPrepend prepends only entries not already present`() {
        val r = PageRuntimeEnv.mergePathPrepend("C:\\msvc\\bin;C:\\old", "C:\\old;C:\\x", ";")
        assertEquals("C:\\msvc\\bin;C:\\old;C:\\x", r)
    }

    @Test
    fun `mergePathPrepend dedups case-insensitively against current`() {
        val r = PageRuntimeEnv.mergePathPrepend("C:\\OLD", "c:\\old", ";")
        assertEquals("c:\\old", r)
    }

    @Test
    fun `mergePathPrepend drops blank segments`() {
        val r = PageRuntimeEnv.mergePathPrepend("C:\\a;;", ";C:\\b;", ";")
        assertEquals("C:\\a;C:\\b", r)
    }

    @Test
    fun `mergePathPrepend keeps all new entries when current is empty`() {
        val r = PageRuntimeEnv.mergePathPrepend("C:\\a;C:\\b", "", ";")
        assertEquals("C:\\a;C:\\b", r)
    }
}
