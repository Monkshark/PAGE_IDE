package page.runtime

import java.nio.file.Files

object MsvcEnv {

    private val CAPTURE_KEYS = listOf(
        "INCLUDE", "LIB", "LIBPATH", "PATH",
        "WindowsSdkDir", "WindowsSDKVersion", "UCRTVersion", "VCToolsInstallDir",
    )

    @Volatile private var probed = false
    @Volatile private var cached: Map<String, String>? = null

    fun envVars(): Map<String, String>? {
        if (!LspInstaller.isWindows()) return null
        if (probed) return cached
        synchronized(this) {
            if (!probed) {
                cached = runCatching { capture() }.getOrNull()
                probed = true
            }
        }
        return cached
    }

    internal fun parseSetOutput(text: String, keys: List<String> = CAPTURE_KEYS): Map<String, String> {
        val all = LinkedHashMap<String, String>()
        for (line in text.lineSequence()) {
            val i = line.indexOf('=')
            if (i <= 0) continue
            all[line.substring(0, i).trim()] = line.substring(i + 1)
        }
        val result = LinkedHashMap<String, String>()
        for (k in keys) {
            all.entries.firstOrNull { it.key.equals(k, ignoreCase = true) }?.let { result[k] = it.value }
        }
        return result
    }

    private fun capture(): Map<String, String>? {
        val vcvars = findVcvars() ?: return null
        val p = ProcessBuilder("cmd", "/c", "\"$vcvars\" >nul 2>&1 && set")
            .redirectErrorStream(true).start()
        val text = p.inputStream.bufferedReader().use { it.readText() }
        p.waitFor()
        return parseSetOutput(text).ifEmpty { null }
    }

    private fun findVcvars(): String? {
        val vs = SystemToolchainDetector.detect().firstOrNull { it.vendor == "msvc" }?.path ?: return null
        val vcvars = vs.resolve("VC").resolve("Auxiliary").resolve("Build").resolve("vcvars64.bat")
        return if (Files.exists(vcvars)) vcvars.toAbsolutePath().toString() else null
    }
}
