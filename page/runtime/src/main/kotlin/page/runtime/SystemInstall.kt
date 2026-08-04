package page.runtime

import java.io.File
import java.nio.file.Path

data class SystemInstall(val path: Path, val version: String?)

object SystemInstallDetector {

    fun findOnPath(names: List<String>, pathEnv: String? = System.getenv("PATH")): Path? {
        if (pathEnv.isNullOrBlank()) return null
        for (dir in pathEnv.split(File.pathSeparator)) {
            if (dir.isBlank()) continue
            for (name in names) {
                val f = File(dir, name)
                if (f.isFile) return f.toPath()
            }
        }
        return null
    }

    fun probeVersion(executable: Path, args: List<String>): String? = runCatching {
        val p = ProcessBuilder(listOf(executable.toString()) + args)
            .redirectErrorStream(true).start()
        val text = p.inputStream.bufferedReader().use { it.readText() }
        p.waitFor()
        Regex("(\\d+\\.\\d+(?:\\.\\d+)?)").find(text)?.value
    }.getOrNull()

    fun detectOnPath(
        names: List<String>,
        versionArgs: List<String> = listOf("--version"),
        pathEnv: String? = System.getenv("PATH"),
    ): SystemInstall? {
        val path = findOnPath(names, pathEnv) ?: return null
        return SystemInstall(path, probeVersion(path, versionArgs))
    }

    fun forLsp(binaries: List<String>, pathEnv: String? = System.getenv("PATH")): SystemInstall? =
        detectOnPath(binaries, listOf("--version"), pathEnv)

    fun runtimeNames(id: String): List<String> = when (id) {
        "jdk" -> listOf("java", "java.exe")
        "node" -> listOf("node", "node.exe")
        "python-runtime" -> listOf("python3", "python", "python.exe")
        "go-sdk" -> listOf("go", "go.exe")
        "rust-runtime" -> listOf("rustc", "rustc.exe")
        "dotnet-runtime" -> listOf("dotnet", "dotnet.exe")
        "cpp-toolchain" -> listOf("clang", "clang.exe")
        "mingw-toolchain" -> listOf("gcc", "gcc.exe")
        else -> emptyList()
    }

    fun runtimeVersionArgs(id: String): List<String> = when (id) {
        "jdk" -> listOf("-version")
        "go-sdk" -> listOf("version")
        else -> listOf("--version")
    }

    fun runtimePresent(id: String, pathEnv: String? = System.getenv("PATH")): Boolean {
        val names = runtimeNames(id)
        if (names.isEmpty()) return false
        return findOnPath(names, pathEnv) != null
    }

    fun forRuntime(id: String, pathEnv: String? = System.getenv("PATH")): SystemInstall? = when (id) {
        "cpp-toolchain" -> toolchainOf("clang")
        "mingw-toolchain" -> toolchainOf("gcc")
        "windows-sdk" -> toolchainOf("msvc")
        else -> {
            val names = runtimeNames(id)
            if (names.isEmpty()) null else detectOnPath(names, runtimeVersionArgs(id), pathEnv)
        }
    }

    private fun toolchainOf(vendorPrefix: String): SystemInstall? =
        SystemToolchainDetector.detect()
            .firstOrNull { it.vendor.startsWith(vendorPrefix) }
            ?.let { SystemInstall(it.compilerPath, it.version) }
}
