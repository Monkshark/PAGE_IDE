package page.runtime

import java.nio.file.Path

data class LanguageRunTemplate(
    val displayName: String,
    val extensions: Set<String>,
    val command: String,
    val argTemplate: List<String>,
)

object LanguageRunDefaults {

    private const val FILE_TOKEN = "{file}"
    private const val NAME_TOKEN = "{name}"
    private const val DIR_TOKEN = "{dir}"

    val templates: List<LanguageRunTemplate> = listOf(
        LanguageRunTemplate(
            displayName = "Python",
            extensions = setOf("py"),
            command = "python",
            argTemplate = listOf(FILE_TOKEN),
        ),
        LanguageRunTemplate(
            displayName = "Node.js",
            extensions = setOf("js", "mjs", "cjs"),
            command = "node",
            argTemplate = listOf(FILE_TOKEN),
        ),
        LanguageRunTemplate(
            displayName = "TypeScript (ts-node)",
            extensions = setOf("ts"),
            command = "npx",
            argTemplate = listOf("ts-node", FILE_TOKEN),
        ),
        LanguageRunTemplate(
            displayName = "Kotlin (kotlinc)",
            extensions = setOf("kt", "kts"),
            command = "kotlinc",
            argTemplate = listOf("-script", FILE_TOKEN),
        ),
        LanguageRunTemplate(
            displayName = "Java (single-file)",
            extensions = setOf("java"),
            command = "java",
            argTemplate = listOf(FILE_TOKEN),
        ),
        LanguageRunTemplate(
            displayName = "Go (run)",
            extensions = setOf("go"),
            command = "go",
            argTemplate = listOf("run", FILE_TOKEN),
        ),
        LanguageRunTemplate(
            displayName = "C (clang)",
            extensions = setOf("c"),
            command = "clang",
            argTemplate = listOf(FILE_TOKEN, "-o", NAME_TOKEN, "&&", NAME_TOKEN),
        ),
        LanguageRunTemplate(
            displayName = "C++ (clang++)",
            extensions = setOf("cpp", "cc", "cxx"),
            command = "clang++",
            argTemplate = listOf(FILE_TOKEN, "-o", NAME_TOKEN, "&&", NAME_TOKEN),
        ),
        LanguageRunTemplate(
            displayName = "Rust (cargo run)",
            extensions = setOf("rs"),
            command = "cargo",
            argTemplate = listOf("run"),
        ),
        LanguageRunTemplate(
            displayName = "C# (dotnet run)",
            extensions = setOf("cs"),
            command = "dotnet",
            argTemplate = listOf("run"),
        ),
        LanguageRunTemplate(
            displayName = "Bash",
            extensions = setOf("sh", "bash"),
            command = "bash",
            argTemplate = listOf(FILE_TOKEN),
        ),
        LanguageRunTemplate(
            displayName = "Dart",
            extensions = setOf("dart"),
            command = "dart",
            argTemplate = listOf("run", FILE_TOKEN),
        ),
        LanguageRunTemplate(
            displayName = "Swift",
            extensions = setOf("swift"),
            command = "swift",
            argTemplate = listOf(FILE_TOKEN),
        ),
    )

    fun forExtension(ext: String): LanguageRunTemplate? {
        val key = ext.lowercase().trimStart('.')
        return templates.firstOrNull { key in it.extensions }
    }

    private fun buildCppConfig(
        path: Path,
        baseName: String,
        workspaceRoot: Path?,
        template: LanguageRunTemplate,
    ): RunConfig? {
        val win = LspInstaller.isWindows()
        val cxx = "cpp" in template.extensions || "cc" in template.extensions || "cxx" in template.extensions
        val resolved = resolveCppCompiler(cxx)
        if (resolved == null) return cppSetupGuidanceConfig(path, baseName, workspaceRoot)
        val (bin, needsExternalHeaders) = resolved
        if (win && needsExternalHeaders && !windowsCppHeadersAvailable()) {
            return cppSetupGuidanceConfig(path, baseName, workspaceRoot)
        }
        val env = emptyMap<String, String>()
        val outDir = workspaceRoot ?: path.parent ?: path
        val outFile = outDir.resolve(if (win) "$baseName.exe" else baseName).toAbsolutePath().toString()
        val cwd = (workspaceRoot ?: path.parent)?.toString()
        val line = "${shellQuote(bin)} ${shellQuote(path.toString())} -o ${shellQuote(outFile)} && ${shellQuote(outFile)}"
        val shell = if (win) "cmd" else "sh"
        val flag = if (win) "/c" else "-c"
        return RunConfig(
            id = "auto-cpp-$baseName-${System.nanoTime()}",
            name = "${template.displayName} · ${path.fileName}",
            command = shell,
            args = listOf(flag, line),
            workingDir = cwd,
            env = env,
        )
    }

    private fun shellQuote(s: String): String =
        if (s.isEmpty() || s.any { it == ' ' || it == '&' || it == '(' || it == ')' }) "\"$s\"" else s

    /** Resolve a C/C++ compiler; second = needs external headers (clang) vs self-contained (gcc/g++). */
    private fun resolveCppCompiler(cxx: Boolean): Pair<String, Boolean>? {
        val win = LspInstaller.isWindows()
        val clangName = if (cxx) (if (win) "clang++.exe" else "clang++") else (if (win) "clang.exe" else "clang")
        runCatching { CppToolchainInstaller().llvmHome() }.getOrNull()
            ?.resolve("bin")?.resolve(clangName)
            ?.takeIf { java.nio.file.Files.exists(it) }
            ?.let { return it.toAbsolutePath().toString() to true }

        val mingw = MingwInstaller()
        val mv = runCatching { mingw.currentInstalledVersion() }.getOrNull()
        if (mv != null) {
            val gxx = mingw.installRoot(mv).resolve("bin").resolve(if (cxx) "g++.exe" else "gcc.exe")
            if (java.nio.file.Files.exists(gxx)) return gxx.toAbsolutePath().toString() to false
        }

        val clangNames = if (cxx) listOf("clang++", "clang++.exe") else listOf("clang", "clang.exe")
        SystemInstallDetector.findOnPath(clangNames)?.let { return it.toAbsolutePath().toString() to true }

        val gxxNames = if (cxx) listOf("g++", "g++.exe") else listOf("gcc", "gcc.exe")
        SystemInstallDetector.findOnPath(gxxNames)?.let { return it.toAbsolutePath().toString() to false }

        return null
    }

    private fun windowsCppHeadersAvailable(): Boolean {
        if (runCatching { WindowsSdkInstaller().isInstalled() }.getOrNull() == true) return true
        if (runCatching { MingwInstaller().isInstalled() }.getOrNull() == true) return true
        if (MsvcEnv.envVars() != null) return true
        return false
    }

    private fun cppSetupGuidanceConfig(path: Path, baseName: String, workspaceRoot: Path?): RunConfig {
        val cwd = workspaceRoot?.toString() ?: path.parent?.toString()
        val g1 = "No C/C++ toolchain found on this system."
        val g2 = "Install one, then Run again:"
        val g3 = "  1) MinGW-w64 from PAGE Install Manager (lightweight), or"
        val g4 = "  2) Visual Studio Build Tools with the Desktop development with C++ workload."
        val line = listOf(g1, g2, g3, g4).joinToString("& ") { "echo $it" } + "& exit /b 1"
        return RunConfig(
            id = "cpp-setup-$baseName-${System.nanoTime()}",
            name = "C/C++ setup required",
            command = "cmd",
            args = listOf("/c", line),
            workingDir = cwd,
            env = emptyMap(),
        )
    }

    private fun resolveJdkEnv(template: LanguageRunTemplate): Pair<String, Map<String, String>>? {
        if ("java" in template.extensions) {
            val jdk = runCatching { JdkInstaller() }.getOrNull() ?: return null
            val home = jdk.javaHome() ?: return null
            val javaBin = home.resolve("bin").resolve(if (LspInstaller.isWindows()) "java.exe" else "java")
            if (!java.nio.file.Files.exists(javaBin)) return null
            return javaBin.toAbsolutePath().toString() to mapOf("JAVA_HOME" to home.toAbsolutePath().toString())
        }
        if ("js" in template.extensions || "mjs" in template.extensions) {
            val node = runCatching { NodeInstaller() }.getOrNull() ?: return null
            val home = node.nodeHome() ?: return null
            val bin = node.executable() ?: return null
            return bin.toAbsolutePath().toString() to mapOf("NODE_HOME" to home.toAbsolutePath().toString())
        }
        if ("py" in template.extensions) {
            val py = runCatching { PythonInstaller() }.getOrNull() ?: return null
            val bin = py.executable() ?: return null
            return bin.toAbsolutePath().toString() to emptyMap()
        }
        if ("go" in template.extensions) {
            val go = runCatching { GoSdkInstaller() }.getOrNull() ?: return null
            val home = go.goHome() ?: return null
            val bin = go.executable() ?: return null
            return bin.toAbsolutePath().toString() to mapOf("GOROOT" to home.toAbsolutePath().toString())
        }
        if ("c" in template.extensions || "cpp" in template.extensions) {
            val cxx = "cpp" in template.extensions || "cc" in template.extensions || "cxx" in template.extensions
            val win = LspInstaller.isWindows()
            val name = if (cxx) (if (win) "clang++.exe" else "clang++") else (if (win) "clang.exe" else "clang")
            val managed = runCatching { CppToolchainInstaller().llvmHome() }.getOrNull()
                ?.resolve("bin")?.resolve(name)
                ?.takeIf { java.nio.file.Files.exists(it) }
            if (managed != null) return managed.toAbsolutePath().toString() to emptyMap()
            val sysNames = if (cxx) listOf("clang++", "clang++.exe") else listOf("clang", "clang.exe")
            val sys = SystemInstallDetector.findOnPath(sysNames)
            if (sys != null) return sys.toAbsolutePath().toString() to emptyMap()
            return null
        }
        if ("rs" in template.extensions) {
            val rust = runCatching { RustToolchainInstaller() }.getOrNull() ?: return null
            val home = rust.rustHome() ?: return null
            val bin = rust.executable() ?: return null
            return bin.toAbsolutePath().toString() to mapOf("PATH" to home.resolve("bin").toAbsolutePath().toString() + java.io.File.pathSeparator + (System.getenv("PATH") ?: ""))
        }
        if ("cs" in template.extensions) {
            val dotnet = runCatching { DotnetSdkInstaller() }.getOrNull() ?: return null
            val home = dotnet.dotnetHome() ?: return null
            val bin = dotnet.executable() ?: return null
            return bin.toAbsolutePath().toString() to mapOf("DOTNET_ROOT" to home.toAbsolutePath().toString())
        }
        if ("dart" in template.extensions) {
            val dart = runCatching { DartSdkInstaller() }.getOrNull() ?: return null
            val bin = dart.executable() ?: return null
            return bin.toAbsolutePath().toString() to emptyMap()
        }
        if ("swift" in template.extensions) {
            val swift = runCatching { SwiftToolchainInstaller() }.getOrNull() ?: return null
            val bin = swift.swiftExecutable() ?: return null
            return bin.toAbsolutePath().toString() to swiftRunEnv(swift)
        }
        return null
    }

    private fun swiftRunEnv(swift: SwiftToolchainInstaller): Map<String, String> {
        val sep = java.io.File.pathSeparator
        val env = mutableMapOf<String, String>()
        swift.binDir()?.toAbsolutePath()?.toString()?.let { binDir ->
            env["PATH"] = binDir + sep + (System.getenv("PATH") ?: "")
        }
        swift.sdkRoot()?.toAbsolutePath()?.toString()?.let { env["SDKROOT"] = it }
        runCatching { WindowsSdkInstaller().envVars() }.getOrNull()?.let { env.putAll(it) }
        return env
    }

    internal fun swiftWindowsPrelaunch(
        swiftc: String,
        file: String,
        exe: String,
        linkLibs: List<String> = emptyList(),
    ): List<String> = listOf(
        swiftc,
        file,
        "-use-ld=lld",
        "-Xcc", "-Xclang", "-Xcc", "-fbuiltin-headers-in-system-modules",
    ) + linkLibs.flatMap { listOf("-Xlinker", it) } + listOf("-o", exe)

    internal fun buildSwiftWindowsConfig(
        path: Path,
        fileName: String,
        baseName: String,
        workspaceRoot: Path?,
        swiftc: Path,
        env: Map<String, String> = emptyMap(),
        linkLibs: List<String> = emptyList(),
    ): RunConfig? {
        val outDir = workspaceRoot ?: path.toAbsolutePath().parent ?: return null
        val exe = outDir.resolve("$baseName.exe").toAbsolutePath().toString()
        return RunConfig(
            id = "auto-swiftc-$baseName-${System.nanoTime()}",
            name = "Swift · $fileName",
            command = exe,
            args = emptyList(),
            workingDir = outDir.toString(),
            env = env,
            prelaunch = swiftWindowsPrelaunch(swiftc.toAbsolutePath().toString(), path.toString(), exe, linkLibs),
            prelaunchOutput = exe,
            prelaunchInputs = listOf(path.toAbsolutePath().toString()),
        )
    }

    private fun buildSwiftWindowsConfig(
        path: Path,
        fileName: String,
        baseName: String,
        workspaceRoot: Path?,
    ): RunConfig? {
        val swift = runCatching { SwiftToolchainInstaller() }.getOrNull() ?: return null
        val swiftc = swift.swiftcExecutable() ?: return null
        val linkLibs = listOfNotNull(swift.foundationImportLib()?.toAbsolutePath()?.toString())
        return buildSwiftWindowsConfig(path, fileName, baseName, workspaceRoot, swiftc, swiftRunEnv(swift), linkLibs)
    }

    private fun buildFlutterConfig(projectRoot: Path): RunConfig {
        val flutter = runCatching { FlutterSdkInstaller() }.getOrNull()
        val version = flutter?.currentInstalledVersion()
        val command = if (flutter != null && version != null) {
            flutter.flutterCommand(version).toAbsolutePath().toString()
        } else {
            "flutter"
        }
        val projectName = projectRoot.fileName?.toString() ?: "app"
        return RunConfig(
            id = "auto-flutter-$projectName-${System.nanoTime()}",
            name = "Flutter · $projectName",
            command = command,
            args = listOf("run"),
            workingDir = projectRoot.toString(),
        )
    }

    fun forFile(path: Path): LanguageRunTemplate? {
        val name = path.fileName?.toString() ?: return null
        val dot = name.lastIndexOf('.')
        if (dot < 0 || dot == name.lastIndex) return null
        return forExtension(name.substring(dot + 1))
    }

    fun buildConfig(path: Path, workspaceRoot: Path?): RunConfig? {
        val template = forFile(path) ?: return null
        if ("dart" in template.extensions) {
            val flutterRoot = FlutterProjectDetector.flutterRootFor(path, workspaceRoot)
            if (flutterRoot != null) return buildFlutterConfig(flutterRoot)
        }
        val fileName = path.fileName?.toString() ?: return null
        val baseName = fileName.substringBeforeLast('.', fileName)
        if ("swift" in template.extensions && LspInstaller.isWindows()) {
            buildSwiftWindowsConfig(path, fileName, baseName, workspaceRoot)?.let { return it }
        }
        if ("c" in template.extensions || "cpp" in template.extensions) {
            buildCppConfig(path, baseName, workspaceRoot, template)?.let { return it }
        }
        val resolvedArgs = template.argTemplate.map { token ->
            when (token) {
                FILE_TOKEN -> path.toString()
                NAME_TOKEN -> baseName
                DIR_TOKEN -> path.parent?.toString() ?: ""
                else -> token
            }
        }
        val cwd = workspaceRoot?.toString() ?: path.parent?.toString()
        val id = "auto-${template.command}-${baseName}-${System.nanoTime()}"
        val jdkEnv = resolveJdkEnv(template)
        return RunConfig(
            id = id,
            name = "${template.displayName} · $fileName",
            command = jdkEnv?.first ?: template.command,
            args = resolvedArgs,
            workingDir = cwd,
            env = jdkEnv?.second ?: emptyMap(),
        )
    }
}
