package page.runtime

import java.nio.file.Files
import java.nio.file.Path

object SwiftPackageDetector {

    private val executableTarget = Regex("""\.executableTarget\s*\(""")
    private val executableProduct = Regex("""\.executable\s*\(""")

    fun declaresExecutable(manifest: String): Boolean =
        executableTarget.containsMatchIn(manifest) || executableProduct.containsMatchIn(manifest)

    fun findManifest(start: Path, ceiling: Path? = null): Path? {
        val stop = ceiling?.toAbsolutePath()?.normalize()
        var dir: Path? = (if (Files.isDirectory(start)) start else start.parent)
            ?.toAbsolutePath()?.normalize()
        while (dir != null) {
            val candidate = dir.resolve("Package.swift")
            if (Files.isRegularFile(candidate)) return candidate
            if (stop != null && dir == stop) break
            dir = dir.parent
        }
        return null
    }

    fun packageRootFor(file: Path, workspaceRoot: Path?): Path? =
        findManifest(file, workspaceRoot)?.parent
}
