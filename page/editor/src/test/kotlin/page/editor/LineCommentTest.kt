package page.editor

import java.nio.file.Paths
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class LineCommentTest {

    @Test
    fun `prefixFor returns slash for kt and java`() {
        assertEquals("// ", LineComment.prefixFor(Paths.get("Foo.kt")))
        assertEquals("// ", LineComment.prefixFor(Paths.get("foo.kts")))
        assertEquals("// ", LineComment.prefixFor(Paths.get("Foo.java")))
        assertNull(LineComment.prefixFor(Paths.get("Foo.json")))
        assertNull(LineComment.prefixFor(Paths.get("Foo.md")))
        assertNull(LineComment.prefixFor(null))
    }

    @Test
    fun `single line collapsed caret toggles to commented`() {
        val src = "val x = 1\nval y = 2"
        val caret = 3
        val r = LineComment.toggle(src, caret, caret, "// ")
        assertEquals("// val x = 1\nval y = 2", r.text)
        assertEquals(6, r.selectionStart)
        assertEquals(6, r.selectionEnd)
    }

    @Test
    fun `single line commented toggles back removing prefix and space`() {
        val src = "// val x = 1\nval y = 2"
        val r = LineComment.toggle(src, 0, 0, "// ")
        assertEquals("val x = 1\nval y = 2", r.text)
    }

    @Test
    fun `multi-line range comments at minimum indent column`() {
        val src = "    val a = 1\n      val b = 2\n    val c = 3"
        val r = LineComment.toggle(src, 0, src.length, "// ")
        assertEquals(
            "    // val a = 1\n    //   val b = 2\n    // val c = 3",
            r.text,
        )
    }

    @Test
    fun `multi-line all commented at same indent toggles off`() {
        val src = "    // val a = 1\n    // val b = 2"
        val r = LineComment.toggle(src, 0, src.length, "// ")
        assertEquals("    val a = 1\n    val b = 2", r.text)
    }

    @Test
    fun `mixed lines comment all if any uncommented`() {
        val src = "// val a = 1\nval b = 2"
        val r = LineComment.toggle(src, 0, src.length, "// ")
        assertEquals("// // val a = 1\n// val b = 2", r.text)
    }

    @Test
    fun `blank lines are skipped during comment`() {
        val src = "val a = 1\n\nval b = 2"
        val r = LineComment.toggle(src, 0, src.length, "// ")
        assertEquals("// val a = 1\n\n// val b = 2", r.text)
    }

    @Test
    fun `selection ending exactly at line start excludes the trailing empty line`() {
        val src = "val a = 1\nval b = 2\n"
        val r = LineComment.toggle(src, 0, 10, "// ")
        assertEquals("// val a = 1\nval b = 2\n", r.text)
    }

    @Test
    fun `uncomment without trailing space still removes prefix`() {
        val src = "//val x = 1"
        val r = LineComment.toggle(src, 0, src.length, "// ")
        assertEquals("val x = 1", r.text)
    }

    @Test
    fun `selection start shifts by inserted prefix on its line`() {
        val src = "val x = 1"
        val r = LineComment.toggle(src, 4, 9, "// ")
        assertEquals("// val x = 1", r.text)
        assertEquals(7, r.selectionStart)
        assertEquals(12, r.selectionEnd)
    }

    @Test
    fun `selection end on commented line shifts back when uncommenting`() {
        val src = "// val x = 1\n// val y = 2"
        val r = LineComment.toggle(src, 0, src.length, "// ")
        assertEquals("val x = 1\nval y = 2", r.text)
        assertEquals(0, r.selectionStart)
        assertEquals(19, r.selectionEnd)
    }

    @Test
    fun `every language page can open has a way to comment a line`() {
        val expected = mapOf(
            "a.swift" to "// ", "a.rs" to "// ", "a.go" to "// ", "a.ts" to "// ",
            "a.tsx" to "// ", "a.js" to "// ", "a.c" to "// ", "a.hpp" to "// ",
            "a.dart" to "// ", "a.php" to "// ", "a.cs" to "// ",
            "a.py" to "# ", "a.rb" to "# ", "a.sh" to "# ", "a.yaml" to "# ", "a.toml" to "# ",
            "a.sql" to "-- ", "a.lua" to "-- ",
            "a.clj" to ";; ",
        )
        for ((name, prefix) in expected) {
            assertEquals(prefix, LineComment.prefixFor(Paths.get(name)), name)
        }
    }

    @Test
    fun `files that go by name alone are known too`() {
        assertEquals("# ", LineComment.prefixFor(Paths.get("Dockerfile")))
        assertEquals("# ", LineComment.prefixFor(Paths.get("Makefile")))
        assertEquals("# ", LineComment.prefixFor(Paths.get(".gitignore")))
        assertEquals("# ", LineComment.prefixFor(Paths.get("Gemfile")))
    }

    @Test
    fun `a language whose comment depends on where you are is left alone`() {
        for (name in listOf("a.html", "a.xml", "a.md", "a.vue", "a.svelte", "a.css", "a.json")) {
            assertNull(LineComment.prefixFor(Paths.get(name)), name)
        }
    }

    @Test
    fun `an unknown file offers nothing rather than guessing`() {
        assertNull(LineComment.prefixFor(Paths.get("notes.whatever")))
        assertNull(LineComment.prefixFor(Paths.get("noextension")))
        assertNull(LineComment.prefixFor(null))
    }

    @Test
    fun `the shell comment round trips on a python file`() {
        val text = "x = 1\ny = 2\n"
        val prefix = LineComment.prefixFor(Paths.get("a.py"))!!
        val commented = LineComment.toggle(text, 0, text.length, prefix)
        assertEquals("# x = 1\n# y = 2\n", commented.text)
        val back = LineComment.toggle(commented.text, 0, commented.text.length, prefix)
        assertEquals(text, back.text)
    }
}
