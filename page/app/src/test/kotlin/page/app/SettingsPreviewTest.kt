package page.app

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SettingsPreviewTest {

    @Test
    fun `indent follows the tab size`() {
        for (tabSize in listOf(1, 2, 4, 8)) {
            val (code, lines) = previewSource(tabSize)
            val body = lines[4]
            val leading = code.substring(body.start, body.end).takeWhile { it == ' ' }
            assertEquals(tabSize * 2, leading.length, "tab size $tabSize")
            assertEquals(2, body.depth, "tab size $tabSize")
        }
    }

    @Test
    fun `line ranges land on real line boundaries`() {
        val (code, lines) = previewSource(4)
        val split = code.split("\n")
        assertEquals(split.size, lines.size)
        for ((index, line) in lines.withIndex()) {
            assertEquals(split[index], code.substring(line.start, line.end), "line ${index + 1}")
        }
    }

    @Test
    fun `blank lines carry no depth`() {
        val (code, lines) = previewSource(4)
        val blank = lines[1]
        assertEquals(0, blank.depth)
        assertEquals("", code.substring(blank.start, blank.end))
    }

    @Test
    fun `the sample still holds what the preview advertises`() {
        val (code, _) = previewSource(4)
        assertTrue(code.startsWith("import "), "needs an unused import to dim")
        assertTrue(code.contains("val ext"), "needs the caret word declared")
        assertEquals(2, Regex("\\bext\\b").findAll(code).count(), "needs two occurrences to highlight")
        assertTrue(code.contains("{") && code.contains("["), "needs nested brackets for the rainbow")
    }
}
