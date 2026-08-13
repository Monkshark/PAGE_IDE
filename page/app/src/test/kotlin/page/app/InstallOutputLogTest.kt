package page.app

import kotlin.test.Test
import kotlin.test.assertEquals

class InstallOutputLogTest {

    @Test
    fun `progress overwrites the line before it`() {
        var lines = listOf("> Installing Rust Toolchain 1.96.0", "> GET https://example/rust.tar.gz")
        lines = appendDownloadLine(lines, "> Downloading Rust Toolchain… 18.8 / 187.4 MB (10%)")
        lines = appendDownloadLine(lines, "> Downloading Rust Toolchain… 37.5 / 187.4 MB (20%)")
        lines = appendDownloadLine(lines, "> Downloading Rust Toolchain… 56.4 / 187.4 MB (30%)")

        assertEquals(3, lines.size)
        assertEquals("> Downloading Rust Toolchain… 56.4 / 187.4 MB (30%)", lines.last())
        assertEquals("> GET https://example/rust.tar.gz", lines[1])
    }

    @Test
    fun `a download after other output starts its own line`() {
        var lines = listOf("> Downloading A… 1.0 MB")
        lines = lines + "> Extracting…"
        lines = appendDownloadLine(lines, "> Downloading B… 1.0 MB")

        assertEquals(listOf("> Downloading A… 1.0 MB", "> Extracting…", "> Downloading B… 1.0 MB"), lines)
    }

    @Test
    fun `the first download appends to an empty log`() {
        assertEquals(listOf("> Downloading A… 0.1 MB"), appendDownloadLine(emptyList(), "> Downloading A… 0.1 MB"))
    }

    @Test
    fun `the log stays bounded`() {
        val long = (1..2000).map { "line $it" }
        val trimmed = appendDownloadLine(long, "> Downloading A… 1.0 MB")
        assertEquals(2000, trimmed.size)
        assertEquals("> Downloading A… 1.0 MB", trimmed.last())
    }
}
