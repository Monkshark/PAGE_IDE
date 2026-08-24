package page.runtime

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DownloadSizeTest {

    @AfterTest
    fun tidy() {
        DownloadSize.forget()
    }

    @Test
    fun `a big download reads in whole megabytes`() {
        assertEquals("350 MB", DownloadSize.format(367_515_503))
    }

    @Test
    fun `a small download keeps one decimal so it is not just zero`() {
        assertEquals("32.6 MB", DownloadSize.format(34_188_182))
        assertEquals("78.1 MB", DownloadSize.format(81_942_945))
    }

    @Test
    fun `past a gigabyte the unit changes`() {
        assertEquals("1.50 GB", DownloadSize.format(1_610_612_736))
        assertEquals("2.00 GB", DownloadSize.format(2_147_483_648))
    }

    @Test
    fun `a host that cannot be reached gives no number at all`() {
        assertNull(DownloadSize.of("https://page-ide-nonexistent.invalid/asset.zip"))
    }

    @Test
    fun `a url that is not a url is not an error`() {
        assertNull(DownloadSize.of("not a url"))
    }
}
