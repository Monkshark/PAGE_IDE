package page.app

import page.lsp.LanguageBackend
import page.lsp.LspBackends
import page.runtime.LspInstaller
import page.runtime.LspInstallers
import page.runtime.PageRuntimeEnv
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.TimeUnit
import kotlin.test.Test

class LspStartupBench {

    private data class Row(
        val backend: String,
        val origin: String,
        val resolveMs: Long,
        val spawnMs: Long,
        val initMs: Long,
        val note: String,
    ) {
        companion object {
            fun blank(note: String) = Row("-", "-", -1, -1, -1, note)
        }
    }

    @Test
    fun bench() {
        val root = benchRoot()
        if (root == null) {
            println(USAGE)
            return
        }
        registerAllBackends()
        installRequested()

        val only = listProp(ONLY)
        val projects = Files.list(root).use { stream ->
            stream.filter { Files.isDirectory(it) }.sorted().toList()
        }.filter { only.isEmpty() || it.fileName.toString() in only }
        if (projects.isEmpty()) {
            println("[bench] $root holds no project directories")
            return
        }

        val runs = intProp(RUNS, 1).coerceIn(1, 10)
        val samples = LinkedHashMap<Path, MutableList<Row>>()
        projects.forEach { samples[it] = mutableListOf() }
        repeat(runs) { run ->
            println("[bench] run ${run + 1} of $runs")
            for (project in projects) {
                samples.getValue(project).add(measure(project))
                writeReport(root, samples, runs)
            }
        }
        println(report(samples, runs))
    }

    private fun installRequested() {
        for (id in listProp(INSTALL)) {
            val installer = LspInstallers.forId(id)
            if (installer == null) {
                println("[bench] no installer named $id")
                continue
            }
            if (installer.isInstalled()) continue
            println("[bench] installing $id …")
            val startedAt = System.currentTimeMillis()
            val failure = runCatching {
                installer.install(null) { progress ->
                    if (progress is LspInstaller.Progress.Failed) throw progress.error
                }
            }.exceptionOrNull()
            val tookMs = System.currentTimeMillis() - startedAt
            val outcome = if (failure == null) "installed" else "FAILED: ${failure.message}"
            println("[bench] $id $outcome in ${tookMs}ms")
        }
    }

    private fun measure(project: Path): Row {
        val file = sourceFile(project) ?: return Row.blank("no source file with a backend")
        val backend = LspBackends.forFile(file, project) ?: return Row.blank("no backend for ${file.fileName}")

        val env = HashMap(System.getenv())
        PageRuntimeEnv.applyTo(env)

        val resolveStartedAt = System.currentTimeMillis()
        val resolution = backend.resolveExecutable(env)
        val resolveMs = System.currentTimeMillis() - resolveStartedAt
        if (resolution !is LanguageBackend.Resolution.Found) {
            return Row(backend.id, "-", resolveMs, -1, -1, "not installed")
        }

        val spawnStartedAt = System.currentTimeMillis()
        val client = runCatching { backend.spawn(resolution.executable, project, { }, env) }
            .getOrElse { return Row(backend.id, resolution.origin, resolveMs, -1, -1, "spawn failed: ${it.message}") }
        val spawnMs = System.currentTimeMillis() - spawnStartedAt

        val initStartedAt = System.currentTimeMillis()
        val note = runCatching {
            client.start().get(intProp(TIMEOUT, 180).toLong(), TimeUnit.SECONDS)
            ""
        }.getOrElse { "initialize failed: ${it.message?.take(60)}" }
        val initMs = System.currentTimeMillis() - initStartedAt
        runCatching { client.forceClose() }
        return Row(backend.id, resolution.origin, resolveMs, spawnMs, if (note.isEmpty()) initMs else -1, note)
    }

    private fun sourceFile(project: Path): Path? {
        val candidates = Files.walk(project, WALK_DEPTH).use { stream ->
            stream.filter { Files.isRegularFile(it) }
                .filter { path -> path.none { it.toString() in SKIP } }
                .filter { LspBackends.forFile(it, project) != null }
                .sorted()
                .toList()
        }
        if (candidates.isEmpty()) return null
        val wanted = ALIAS[project.fileName.toString()] ?: project.fileName.toString()
        candidates.firstOrNull { LspBackends.forFile(it, project)?.id == wanted }?.let { return it }
        val pool = candidates.filterNot { LspBackends.forFile(it, project)!!.id in CONFIG }.ifEmpty { candidates }
        val dominant = pool.groupingBy { LspBackends.forFile(it, project)!!.id }.eachCount().maxBy { it.value }.key
        return pool.first { LspBackends.forFile(it, project)!!.id == dominant }
    }

    private fun writeReport(root: Path, samples: Map<Path, List<Row>>, runs: Int) {
        val target = prop(OUT)?.let { Paths.get(it) } ?: root.resolve("lsp-startup.txt")
        runCatching { Files.writeString(target, report(samples, runs)) }
    }

    private fun report(samples: Map<Path, List<Row>>, runs: Int): String {
        val runHeader = (1..runs).joinToString("") { "%9s".format("init $it") }
        val out = StringBuilder()
        out.appendLine("%-12s %-22s %-16s %8s %8s%s  %s".format("project", "backend", "origin", "resolve", "spawn", runHeader, "note"))
        out.appendLine("-".repeat(76 + runs * 9))
        for ((project, rows) in samples) {
            if (rows.isEmpty()) continue
            val last = rows.last()
            val inits = (0 until runs).joinToString("") { i -> "%9s".format(rows.getOrNull(i)?.let { ms(it.initMs) } ?: "") }
            out.appendLine(
                "%-12s %-22s %-16s %8s %8s%s  %s".format(
                    project.fileName, last.backend, last.origin,
                    ms(last.resolveMs), ms(last.spawnMs), inits, last.note,
                ),
            )
        }
        return out.toString()
    }

    private fun ms(value: Long): String = if (value < 0) "-" else "${value}ms"

    private fun benchRoot(): Path? = prop(ROOT)?.let { Paths.get(it) }?.takeIf { Files.isDirectory(it) }

    private fun prop(key: String): String? =
        (System.getProperty(key) ?: System.getenv(key.replace('.', '_').uppercase()))
            ?.trim()?.takeIf { it.isNotEmpty() }

    private fun listProp(key: String): List<String> =
        prop(key)?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() }.orEmpty()

    private fun intProp(key: String, fallback: Int): Int = prop(key)?.toIntOrNull() ?: fallback

    private companion object {
        const val ROOT = "page.lsp.bench"
        const val ONLY = "page.lsp.bench.only"
        const val INSTALL = "page.lsp.bench.install"
        const val OUT = "page.lsp.bench.out"
        const val RUNS = "page.lsp.bench.runs"
        const val TIMEOUT = "page.lsp.bench.timeout"
        const val WALK_DEPTH = 8

        val SKIP = setOf("node_modules", "build", "target", ".git", ".dart_tool", "vendor", ".gradle", ".page-ide", ".settings")
        val ALIAS = mapOf("web" to "html")
        val CONFIG = setOf("json", "yaml", "markdown", "sql", "docker", "toml", "xml", "ini", "properties")

        val USAGE = """
            [bench] skipped — point $ROOT at a directory holding one project per language:

              ./gradlew :page:app:test --tests 'page.app.LspStartupBench' --rerun-tasks -D$ROOT=/path/to/lsp-bench -D$RUNS=2

            Each subdirectory is one project; its language is taken from the directory name,
            falling back to whichever backend most of its source files use.

              -D$ONLY=rust,go        measure only these projects
              -D$INSTALL=dart,swift  install these servers first if they are missing
              -D$RUNS=2              repeat, so cold and warm starts are both visible
              -D$TIMEOUT=180         seconds to wait for initialize
              -D$OUT=report.txt      where the report lands (default <root>/lsp-startup.txt)
        """.trimIndent()
    }
}
