package page.runtime

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ArchiveUrlTest {

    @Test
    fun `the go archive can be sized before the install starts`() {
        val go = GoSdkInstaller(osKey = "windows", archKey = "amd64", isWindows = true)
        assertEquals(go.downloadUrl("1.23.4"), go.archiveUrl("1.23.4"))
    }

    @Test
    fun `no version means the one the button would install`() {
        val go = GoSdkInstaller(osKey = "linux", archKey = "amd64", isWindows = false, defaultGoVersion = "1.22.0")
        assertEquals(go.downloadUrl("1.22.0"), go.archiveUrl(null))
        assertEquals(go.downloadUrl("1.22.0"), go.archiveUrl("   "))
    }

    @Test
    fun `windows ruby is sized by the bundle it actually downloads`() {
        val ruby = RubyBootstrapInstaller(osKey = "windows", archKey = "amd64", isWindows = true)
        assertEquals(ruby.rubyBundleUrl("3.4.6"), ruby.archiveUrl("3.4.6"))
    }

    @Test
    fun `an install with no single archive reports no size to measure`() {
        val ruby = RubyBootstrapInstaller(osKey = "linux", archKey = "amd64", isWindows = false)
        assertNull(ruby.archiveUrl("3.4.6"))
    }

    @Test
    fun `the jdk asset is asked for over https`() {
        val jdk = JdkInstaller(osKey = "windows", archKey = "amd64", isWindows = true)
        val url = jdk.archiveUrl("21.0.5+11")
        assertTrue(url != null && url.startsWith("https://"), "got $url")
    }
}
