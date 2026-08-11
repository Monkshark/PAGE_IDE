package page.core

import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LspStartupStatsTest {

    private fun entry(id: String, name: String, resolve: Long, spawn: Long, init: Long) =
        LspStartupStats.Entry(
            backendId = id,
            displayName = name,
            origin = "managed",
            resolveMs = resolve,
            spawnMs = spawn,
            initializeMs = init,
        )

    @BeforeTest
    fun setUp() {
        LspStartupStats.clear()
        LspStartupStats.onRecord = null
    }

    @AfterTest
    fun tearDown() {
        LspStartupStats.clear()
        LspStartupStats.onRecord = null
    }

    @Test
    fun `total adds the three phases`() {
        assertEquals(1400, entry("kotlin", "Kotlin", 120, 280, 1000).totalMs)
    }

    @Test
    fun `slowest server comes first`() {
        LspStartupStats.record(entry("go", "Go", 10, 40, 300))
        LspStartupStats.record(entry("kotlin", "Kotlin", 120, 280, 9000))
        LspStartupStats.record(entry("json", "JSON", 5, 30, 120))
        assertEquals(listOf("Kotlin", "Go", "JSON"), LspStartupStats.snapshot().map { it.displayName })
    }

    @Test
    fun `a restart replaces the earlier run`() {
        LspStartupStats.record(entry("kotlin", "Kotlin", 100, 100, 9000))
        LspStartupStats.record(entry("kotlin", "Kotlin", 10, 20, 400))
        val rows = LspStartupStats.snapshot()
        assertEquals(1, rows.size)
        assertEquals(430, rows.first().totalMs)
    }

    @Test
    fun `every recorded server reaches the listener`() {
        val seen = mutableListOf<String>()
        LspStartupStats.onRecord = { seen += it.displayName }
        LspStartupStats.record(entry("go", "Go", 10, 40, 300))
        LspStartupStats.record(entry("rust", "Rust", 10, 40, 300))
        assertEquals(listOf("Go", "Rust"), seen)
    }

    @Test
    fun `the table names the slowest server`() {
        LspStartupStats.record(entry("go", "Go", 10, 40, 300))
        LspStartupStats.record(entry("kotlin", "Kotlin", 120, 280, 9000))
        val table = LspStartupStats.table()
        assertContains(table, "2 server(s)")
        assertContains(table, "slowest Kotlin 9400ms")
        assertTrue(table.indexOf("Kotlin") < table.indexOf("Go"), "slowest row is listed first")
    }

    @Test
    fun `an empty table says so`() {
        assertContains(LspStartupStats.table(), "nothing started yet")
    }

    @Test
    fun `the one-line form carries every phase`() {
        val line = entry("kotlin", "Kotlin", 120, 280, 9000).line()
        assertContains(line, "Kotlin ready in 9400ms")
        assertContains(line, "resolve 120ms")
        assertContains(line, "spawn 280ms")
        assertContains(line, "initialize 9000ms")
        assertContains(line, "managed")
    }
}
