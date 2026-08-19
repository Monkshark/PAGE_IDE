package page.runtime

import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class RubyBuildFileTest {

    private fun project(): Path = Files.createTempDirectory("ruby-buildfile-")

    @Test
    fun `a ruby-version file names the version`() {
        val root = project()
        Files.writeString(root.resolve(".ruby-version"), "  3.4.9  ")
        val found = BuildFileVersionDetector.detectForRuntime(root, "ruby")
        assertEquals("3.4.9", found?.version)
        assertEquals(".ruby-version", found?.source)
    }

    @Test
    fun `the ruby- prefix some tools write is dropped`() {
        val root = project()
        Files.writeString(root.resolve(".ruby-version"), "ruby-3.3.6")
        assertEquals("3.3.6", BuildFileVersionDetector.detectForRuntime(root, "ruby")?.version)
    }

    @Test
    fun `a Gemfile ruby line is read when there is no version file`() {
        val root = project()
        Files.writeString(root.resolve("Gemfile"), "source 'https://rubygems.org'\nruby '3.2.4'\n")
        val found = BuildFileVersionDetector.detectForRuntime(root, "ruby")
        assertEquals("3.2.4", found?.version)
        assertEquals("Gemfile", found?.source)
    }

    @Test
    fun `a Gemfile pessimistic constraint still yields the version`() {
        val root = project()
        Files.writeString(root.resolve("Gemfile"), "ruby \"~> 3.1.0\"\n")
        assertEquals("3.1.0", BuildFileVersionDetector.detectForRuntime(root, "ruby")?.version)
    }

    @Test
    fun `the version file wins over the Gemfile`() {
        val root = project()
        Files.writeString(root.resolve(".ruby-version"), "3.4.9")
        Files.writeString(root.resolve("Gemfile"), "ruby '3.2.4'")
        assertEquals("3.4.9", BuildFileVersionDetector.detectForRuntime(root, "ruby")?.version)
    }

    @Test
    fun `a project with neither says nothing`() {
        assertNull(BuildFileVersionDetector.detectForRuntime(project(), "ruby"))
    }

    @Test
    fun `a ruby project shows up in a full scan`() {
        val root = project()
        Files.writeString(root.resolve(".ruby-version"), "3.4.9")
        assertEquals(
            listOf("ruby" to "3.4.9"),
            BuildFileVersionDetector.detect(root).map { it.runtime to it.version },
        )
    }
}
