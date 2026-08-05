package page.lsp

import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class KotlinLspTest {

    @Test
    fun `index storage dir is stable per workspace`() {
        val home = Path.of("/home/dev")
        val workspace = Path.of("/projects/demo")
        val first = KotlinLsp.indexStorageDir(workspace, home)
        val second = KotlinLsp.indexStorageDir(Path.of("/projects", "demo"), home)
        assertEquals(first, second)
        assertTrue(first.toString().contains("demo"), "expected workspace name in $first")
        assertTrue(first!!.startsWith(home.resolve(".page-ide")), "expected page-ide home in $first")
    }

    @Test
    fun `index storage dir differs per workspace`() {
        val home = Path.of("/home/dev")
        val a = KotlinLsp.indexStorageDir(Path.of("/projects/demo"), home)
        val b = KotlinLsp.indexStorageDir(Path.of("/other/demo"), home)
        assertTrue(a != b, "expected distinct dirs, both were $a")
    }

    @Test
    fun `index storage dir is null without a workspace`() {
        assertEquals(null, KotlinLsp.indexStorageDir(null, Path.of("/home/dev")))
    }

    @Test
    fun `system property override resolves first`() {
        val tmp = Files.createTempFile("kls-fake", "")
        val prevOverride = System.getProperty("page.lsp.kotlin.path")
        System.setProperty("page.lsp.kotlin.path", tmp.toString())
        try {
            val r = KotlinLsp.resolveExecutable(emptyMap())
            assertTrue(r is KotlinLsp.Resolution.Found, "expected Found, got $r")
            assertEquals(tmp.toAbsolutePath(), (r as KotlinLsp.Resolution.Found).executable.toAbsolutePath())
        } finally {
            if (prevOverride != null) System.setProperty("page.lsp.kotlin.path", prevOverride)
            else System.clearProperty("page.lsp.kotlin.path")
            Files.deleteIfExists(tmp)
        }
    }

    @Test
    fun `bundled compose resources path resolves when present`() {
        val tmp = Files.createTempDirectory("kls-resources")
        try {
            val isWindows = System.getProperty("os.name").orEmpty().lowercase().contains("win")
            val binName = if (isWindows) "kotlin-language-server.bat" else "kotlin-language-server"
            val bin = tmp.resolve("lsp/server/bin/$binName")
            Files.createDirectories(bin.parent)
            Files.createFile(bin)
            if (!isWindows) bin.toFile().setExecutable(true)

            val prev = System.getProperty("compose.application.resources.dir")
            val prevOverride = System.getProperty("page.lsp.kotlin.path")
            System.clearProperty("page.lsp.kotlin.path")
            System.setProperty("compose.application.resources.dir", tmp.toString())
            try {
                val r = KotlinLsp.resolveExecutable(emptyMap())
                assertTrue(r is KotlinLsp.Resolution.Found, "expected Found, got $r")
                assertEquals(bin.toAbsolutePath(), (r as KotlinLsp.Resolution.Found).executable.toAbsolutePath())
                assertTrue(r.origin.contains("bundled"))
            } finally {
                if (prev != null) System.setProperty("compose.application.resources.dir", prev)
                else System.clearProperty("compose.application.resources.dir")
                if (prevOverride != null) System.setProperty("page.lsp.kotlin.path", prevOverride)
            }
        } finally {
            tmp.toFile().deleteRecursively()
        }
    }

    @Test
    fun `kls gradle opts set perf flags when none set`() {
        val expected = "-Dorg.gradle.configuration-cache=false -Dorg.gradle.configureondemand=true " +
            "-Dorg.gradle.priority=low -Dorg.gradle.workers.max=2"
        assertEquals(expected, KotlinLsp.klsGradleOpts(null))
        assertEquals(expected, KotlinLsp.klsGradleOpts("   "))
    }

    @Test
    fun `kls gradle opts append perf flags preserving existing`() {
        assertEquals(
            "-Xmx512m -Dorg.gradle.configuration-cache=false -Dorg.gradle.configureondemand=true " +
                "-Dorg.gradle.priority=low -Dorg.gradle.workers.max=2",
            KotlinLsp.klsGradleOpts("-Xmx512m"),
        )
    }

    @Test
    fun `not found when nothing on PATH or override or bundled`() {
        val prevOverride = System.getProperty("page.lsp.kotlin.path")
        val prevResources = System.getProperty("compose.application.resources.dir")
        val prevDisableDev = System.getProperty("page.lsp.kotlin.disableDev")
        System.clearProperty("page.lsp.kotlin.path")
        System.clearProperty("compose.application.resources.dir")
        System.setProperty("page.lsp.kotlin.disableDev", "true")
        val isolatedHome = Files.createTempDirectory("kls-home-")
        try {
            val r = KotlinLsp.resolveExecutable(
                env = mapOf("PATH" to "/definitely/does/not/exist"),
                home = isolatedHome,
            )
            assertTrue(r is KotlinLsp.Resolution.NotFound)
        } finally {
            if (prevOverride != null) System.setProperty("page.lsp.kotlin.path", prevOverride)
            if (prevResources != null) System.setProperty("compose.application.resources.dir", prevResources)
            if (prevDisableDev != null) System.setProperty("page.lsp.kotlin.disableDev", prevDisableDev)
            else System.clearProperty("page.lsp.kotlin.disableDev")
            isolatedHome.toFile().deleteRecursively()
        }
    }
}
