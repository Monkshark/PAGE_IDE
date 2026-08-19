package page.editor

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FileDocumentLineEndingTest {

    @AfterTest
    fun tidy() {
        FileDocument.forgetLineEndings()
    }

    private fun write(name: String, content: String): Path {
        val file = Files.createTempDirectory("line-endings-").resolve(name)
        Files.writeString(file, content, StandardCharsets.UTF_8)
        return file
    }

    @Test
    fun `a windows file arrives without the carriage returns`() {
        val file = write("a.rb", "puts 1\r\nputs 2\r\n")
        assertEquals("puts 1\nputs 2\n", FileDocument.load(file))
    }

    @Test
    fun `a unix file is left exactly as it is`() {
        val file = write("a.rb", "puts 1\nputs 2\n")
        assertEquals("puts 1\nputs 2\n", FileDocument.load(file))
    }

    @Test
    fun `an old mac file becomes lines too`() {
        assertEquals("puts 1\nputs 2", FileDocument.normalize("puts 1\rputs 2"))
    }

    @Test
    fun `saving a windows file writes windows endings back`() {
        val file = write("a.rb", "puts 1\r\nputs 2\r\n")
        val text = FileDocument.load(file)

        FileDocument.save(file, text + "puts 3\n")

        val raw = Files.readString(file, StandardCharsets.UTF_8)
        assertEquals("puts 1\r\nputs 2\r\nputs 3\r\n", raw)
    }

    @Test
    fun `saving a unix file does not give it windows endings`() {
        val file = write("a.rb", "puts 1\nputs 2\n")
        val text = FileDocument.load(file)

        FileDocument.save(file, text + "puts 3\n")

        assertEquals("puts 1\nputs 2\nputs 3\n", Files.readString(file, StandardCharsets.UTF_8))
    }

    @Test
    fun `a file with no newline at all keeps its shape`() {
        val file = write("a.rb", "puts 1")
        assertEquals("puts 1", FileDocument.load(file))
        FileDocument.save(file, "puts 1")
        assertEquals("puts 1", Files.readString(file, StandardCharsets.UTF_8))
    }

    @Test
    fun `the ending a file was loaded with is what it is saved with`() {
        assertTrue(FileDocument.usesCrlf("a\r\nb"))
        assertFalse(FileDocument.usesCrlf("a\nb"))
        assertEquals("a\r\nb", FileDocument.denormalize("a\nb", crlf = true))
        assertEquals("a\nb", FileDocument.denormalize("a\nb", crlf = false))
    }

    @Test
    fun `normalising never leaves a stray carriage return behind`() {
        assertFalse(FileDocument.normalize("a\r\nb\rc\nd").contains('\r'))
        assertEquals("a\nb\nc\nd", FileDocument.normalize("a\r\nb\rc\nd"))
    }
}
