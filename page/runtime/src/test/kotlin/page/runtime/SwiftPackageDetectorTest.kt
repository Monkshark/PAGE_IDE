package page.runtime

import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SwiftPackageDetectorTest {

    private fun packageAt(manifest: String): Path {
        val root = Files.createTempDirectory("swiftpm-")
        Files.writeString(root.resolve("Package.swift"), manifest)
        Files.createDirectories(root.resolve("Sources").resolve("bench"))
        Files.writeString(root.resolve("Sources").resolve("bench").resolve("main.swift"), "print(1)")
        return root
    }

    private val executable = """
        // swift-tools-version:5.9
        import PackageDescription

        let package = Package(
            name: "bench-swift",
            targets: [.executableTarget(name: "bench", path: "Sources/bench")]
        )
    """.trimIndent()

    private val library = """
        // swift-tools-version:5.9
        import PackageDescription

        let package = Package(
            name: "lib-only",
            products: [.library(name: "Lib", targets: ["Lib"])],
            targets: [.target(name: "Lib")]
        )
    """.trimIndent()

    @Test
    fun `a source file inside a package finds the package it belongs to`() {
        val root = packageAt(executable)
        val source = root.resolve("Sources").resolve("bench").resolve("main.swift")
        assertEquals(root.toRealPath(), SwiftPackageDetector.packageRootFor(source, null)?.toRealPath())
    }

    @Test
    fun `a loose swift file belongs to no package`() {
        val loose = Files.createTempDirectory("loose-").resolve("scratch.swift")
        Files.writeString(loose, "print(1)")
        assertNull(SwiftPackageDetector.packageRootFor(loose, loose.parent))
    }

    @Test
    fun `the search stops at the workspace it was given`() {
        val root = packageAt(executable)
        val nested = Files.createDirectories(root.resolve("Sources").resolve("deep"))
        val source = nested.resolve("a.swift")
        Files.writeString(source, "print(1)")

        assertNull(SwiftPackageDetector.packageRootFor(source, nested))
        assertEquals(root.toRealPath(), SwiftPackageDetector.packageRootFor(source, root)?.toRealPath())
    }

    @Test
    fun `a package that builds a program is told apart from one that only builds a library`() {
        assertTrue(SwiftPackageDetector.declaresExecutable(executable))
        assertFalse(SwiftPackageDetector.declaresExecutable(library))
    }

    @Test
    fun `an executable product counts even without an executable target`() {
        val manifest = """
            let package = Package(
                name: "p",
                products: [.executable(name: "tool", targets: ["Tool"])],
                targets: [.target(name: "Tool")]
            )
        """.trimIndent()
        assertTrue(SwiftPackageDetector.declaresExecutable(manifest))
    }

    @Test
    fun `spacing before the parenthesis does not hide the executable`() {
        assertTrue(SwiftPackageDetector.declaresExecutable(".executableTarget (name: \"x\")"))
        assertFalse(SwiftPackageDetector.declaresExecutable(".testTarget(name: \"x\")"))
    }
}
