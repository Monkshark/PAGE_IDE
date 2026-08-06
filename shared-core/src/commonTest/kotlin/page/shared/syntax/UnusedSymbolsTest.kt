package page.shared.syntax

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class UnusedSymbolsTest {

    private fun analyze(text: String): List<String> {
        val tokens = SyntaxRoles.refine(text, KotlinLexer.tokenize(text))
        val pairs = BracketScan.pairs(text, tokens)
        return UnusedSymbols.find(text, tokens, pairs).map { text.substring(it.first, it.last + 1) }
    }

    @Test
    fun localDeclaredOnceIsUnused() {
        val text = """
            fun run() {
                val total = 1
                val used = 2
                println(used)
            }
        """.trimIndent()
        val found = analyze(text)
        assertTrue(found.contains("total"), "expected total, got $found")
        assertTrue(!found.contains("used"), "used is referenced, got $found")
    }

    @Test
    fun topLevelDeclarationIsLeftAlone() {
        val text = "val shared = 1\nfun run() { }"
        assertTrue(analyze(text).none { it == "shared" })
    }

    @Test
    fun unusedImportIsFlagged() {
        val text = """
            import java.nio.file.Files
            import java.nio.file.Path

            fun run(p: Path) {
                println(p)
            }
        """.trimIndent()
        val found = analyze(text)
        assertTrue(found.any { it == "import java.nio.file.Files" }, "got $found")
        assertTrue(found.none { it.contains("Path") }, "Path is used, got $found")
    }

    @Test
    fun aliasedImportFollowsItsAlias() {
        val text = """
            import java.nio.file.Path as FsPath

            fun run() {
                val p: FsPath? = null
                println(p)
            }
        """.trimIndent()
        assertTrue(analyze(text).none { it.startsWith("import") })
    }

    @Test
    fun starImportIsNeverFlagged() {
        val text = "import page.runtime.*\n\nfun run() { }"
        assertTrue(analyze(text).isEmpty())
    }

    @Test
    fun nameUsedOnlyInsideAStringStillCountsAsUnused() {
        val text = """
            fun run() {
                val label = 1
                println("label")
            }
        """.trimIndent()
        assertEquals(listOf("label"), analyze(text))
    }
}
