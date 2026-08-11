package page.app

import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.readText
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CrashLogTest {

    private lateinit var dir: Path
    private var previous: String? = null

    @BeforeTest
    fun setUp() {
        dir = Files.createTempDirectory("page-crashlog-")
        previous = System.getProperty("page.settings.dir")
        System.setProperty("page.settings.dir", dir.toString())
    }

    @AfterTest
    fun tearDown() {
        if (previous != null) System.setProperty("page.settings.dir", previous) else System.clearProperty("page.settings.dir")
        dir.toFile().deleteRecursively()
    }

    @Test
    fun `writes under the settings directory`() {
        assertEquals(dir.resolve("logs").resolve("page.log"), CrashLog.file())
    }

    @Test
    fun `creates the folder on first write`() {
        CrashLog.write("hello")
        assertTrue(Files.exists(CrashLog.file()))
        assertContains(CrashLog.file().readText(), "hello")
    }

    @Test
    fun `keeps every line`() {
        CrashLog.write("first")
        CrashLog.write("second")
        val lines = CrashLog.file().readText().trim().lines()
        assertEquals(2, lines.size)
        assertContains(lines[0], "first")
        assertContains(lines[1], "second")
    }

    @Test
    fun `records a stack trace with its note`() {
        CrashLog.record(IllegalStateException("snapshot boom"), "uncaught on AWT-EventQueue-0")
        val text = CrashLog.file().readText()
        assertContains(text, "uncaught on AWT-EventQueue-0")
        assertContains(text, "IllegalStateException")
        assertContains(text, "snapshot boom")
        assertContains(text, "CrashLogTest")
    }

    @Test
    fun `rolls the file once it grows past the cap`() {
        val path = CrashLog.file()
        Files.createDirectories(path.parent)
        Files.writeString(path, "x".repeat(600 * 1024))
        CrashLog.rollIfLarge(path)
        assertTrue(Files.exists(path.resolveSibling("page.log.1")), "old log kept as page.log.1")
        assertTrue(!Files.exists(path), "current log rotated away")
    }
}
