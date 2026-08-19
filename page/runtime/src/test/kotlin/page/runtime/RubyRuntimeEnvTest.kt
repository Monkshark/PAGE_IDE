package page.runtime

import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RubyRuntimeEnvTest {

    private fun withHome(block: (Path) -> Unit) {
        val saved = System.getProperty("user.home")
        val home = Files.createTempDirectory("ruby-env-")
        System.setProperty("user.home", home.toString())
        try {
            block(home)
        } finally {
            System.setProperty("user.home", saved)
            runCatching { home.toFile().deleteRecursively() }
        }
    }

    private fun install(version: String) {
        Files.createDirectories(RubyBootstrapInstaller().gemHomeFor(version).resolve("bin"))
        Files.createDirectories(RubyBootstrapInstaller().rubyRoot(version).resolve("bin"))
    }

    @Test
    fun `the server is told where its gems live`() {
        withHome {
            install("3.4.9")
            val env = mutableMapOf("PATH" to "/opt/existing")
            RubyBootstrapInstaller().applyRuntimeEnv(env, "3.4.9")

            val gemHome = RubyBootstrapInstaller().gemHomeFor("3.4.9").toString()
            assertEquals(gemHome, env["GEM_HOME"])
            assertEquals(gemHome, env["GEM_PATH"])
        }
    }

    @Test
    fun `ruby and its gem binaries come before whatever was on the path`() {
        withHome {
            install("3.4.9")
            val env = mutableMapOf("PATH" to "/opt/existing")
            RubyBootstrapInstaller().applyRuntimeEnv(env, "3.4.9")

            val path = env["PATH"]!!
            assertTrue(path.endsWith("/opt/existing"), "the caller path should survive at the end: $path")
            assertTrue(
                path.indexOf("gemhome") < path.indexOf("/opt/existing"),
                "our gem bin has to win: $path",
            )
        }
    }

    @Test
    fun `an existing path spelt Path is extended, not duplicated`() {
        withHome {
            install("3.4.9")
            val env = mutableMapOf("Path" to "/opt/existing")
            RubyBootstrapInstaller().applyRuntimeEnv(env, "3.4.9")

            assertTrue(env["Path"]!!.endsWith("/opt/existing"))
            assertEquals(null, env["PATH"], "windows spells it Path — do not add a second one")
        }
    }

    @Test
    fun `nothing is claimed when ruby was never installed`() {
        withHome {
            val env = mutableMapOf("PATH" to "/opt/existing")
            RubyBootstrapInstaller().applyRuntimeEnv(env, "3.4.9")

            assertEquals(null, env["GEM_HOME"])
            assertEquals("/opt/existing", env["PATH"])
        }
    }
}
