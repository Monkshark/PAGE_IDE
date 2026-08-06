package page.shared.syntax

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class UnusedSymbolsTest {

    private fun analyze(text: String, usedElsewhere: (String) -> Boolean = { false }): List<String> {
        val tokens = SyntaxRoles.refine(text, KotlinLexer.tokenize(text))
        val pairs = BracketScan.pairs(text, tokens)
        return UnusedSymbols.find(text, tokens, pairs, usedElsewhere)
            .map { text.substring(it.first, it.last + 1) }
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
    fun nameMentionedAnywhereElseIsLeftAlone() {
        val text = """
            fun run() {
                val label = 1
                println("label")
            }
        """.trimIndent()
        assertTrue(analyze(text).isEmpty(), "a mention anywhere counts as use, got ${analyze(text)}")
    }

    @Test
    fun stringTemplateCountsAsUse() {
        val text = """
            fun run() {
                val started = 1
                println("took ${'$'}{started}ms")
            }
        """.trimIndent()
        assertTrue(analyze(text).isEmpty(), "template use missed, got ${analyze(text)}")
    }

    @Test
    fun delegateRepeatingItsOwnNameIsStillUnused() {
        val text = """
            fun run() {
                var todoHeight by layoutUiState::todoHeight
                var todoOpen by layoutUiState::todoOpen
                println(todoOpen)
            }
        """.trimIndent()
        val found = analyze(text)
        assertTrue(found.contains("todoHeight"), "delegate never read, got ${'$'}found")
        assertTrue(!found.contains("todoOpen"), "todoOpen is read, got ${'$'}found")
    }

    @Test
    fun memberUsedInAnotherFileIsLeftAlone() {
        val text = """
            class Holder {
                val exported: Int = 1
            }
        """.trimIndent()
        assertTrue(analyze(text) { it == "exported" }.isEmpty(), "member used elsewhere was dimmed")
    }

    @Test
    fun memberUsedNowhereIsDimmed() {
        val text = """
            class Holder {
                val orphan: Int = 1
            }
        """.trimIndent()
        assertTrue(analyze(text).contains("orphan"), "member unused project-wide was kept, got ${'$'}{analyze(text)}")
    }

    @Test
    fun classPropertyIsLeftAlone() {
        val text = """
            class Holder(
                private val id: String,
            ) {
                val flow: String = id
            }
        """.trimIndent()
        assertTrue(analyze(text) { true }.none { it == "flow" }, "class property flagged, got ${analyze(text) { true }}")
    }
}
