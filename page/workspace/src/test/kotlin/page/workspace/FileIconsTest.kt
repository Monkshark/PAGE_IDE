package page.workspace

import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class FileIconsTest {
    private val fileProbes = listOf(
        "a.mp3" to "audio",
        "a.c" to "c",
        "LICENSE" to "certificate",
        "CMakeLists.txt" to "cmake",
        "run.sh" to "console",
        "a.cpp" to "cpp",
        "a.cs" to "csharp",
        "a.css" to "css",
        "a.dart" to "dart",
        "a.sql" to "database",
        "Dockerfile" to "docker",
        ".editorconfig" to "editorconfig",
        ".eslintrc.json" to "eslint",
        "pubspec.yaml" to "flutter",
        "a.ttf" to "font",
        ".gitignore" to "git",
        "go.mod" to "go",
        "build.gradle.kts" to "gradle",
        "a.graphql" to "graphql",
        "a.html" to "html",
        "a.png" to "image",
        "A.java" to "java",
        "a.js" to "javascript",
        "a.json" to "json",
        "a.ipynb" to "jupyter",
        "A.kt" to "kotlin",
        "a.less" to "less",
        "yarn.lock" to "lock",
        "a.log" to "log",
        "notes.txt" to "document",
        "data.csv" to "table",
        "a.lua" to "lua",
        "Makefile" to "makefile",
        "a.md" to "markdown",
        "pom.xml" to "maven",
        "package.json" to "nodejs",
        "a.pdf" to "pdf",
        "a.php" to "php",
        "a.ps1" to "powershell",
        ".prettierrc" to "prettier",
        "a.proto" to "proto",
        "a.py" to "python",
        "a.rb" to "ruby",
        "Cargo.toml" to "rust",
        "a.scss" to "sass",
        "a.scala" to "scala",
        "a.properties" to "settings",
        "a.svelte" to "svelte",
        "a.svg" to "svg",
        "a.swift" to "swift",
        "tailwind.config.js" to "tailwindcss",
        "a.tf" to "terraform",
        "a.toml" to "toml",
        ".env" to "tune",
        "a.ts" to "typescript",
        "a.mp4" to "video",
        "vite.config.ts" to "vite",
        "a.vue" to "vue",
        "webpack.config.js" to "webpack",
        "a.xml" to "xml",
        "a.yaml" to "yaml",
        "a.zip" to "zip",
    )

    private val folderProbes = listOf(
        "android" to "folder-android",
        "api" to "folder-api",
        "app" to "folder-app",
        "whatever" to "folder-base",
        "ci" to "folder-ci",
        "client" to "folder-client",
        "components" to "folder-components",
        "config" to "folder-config",
        "controllers" to "folder-controller",
        "core" to "folder-core",
        "coverage" to "folder-coverage",
        "styles" to "folder-css",
        "db" to "folder-database",
        "build" to "folder-dist",
        "docs" to "folder-docs",
        "examples" to "folder-examples",
        "fonts" to "folder-font",
        ".git" to "folder-git",
        ".github" to "folder-github",
        ".gradle" to "folder-gradle",
        "hooks" to "folder-hook",
        "i18n" to "folder-i18n",
        "images" to "folder-images",
        "include" to "folder-include",
        "types" to "folder-interface",
        "ios" to "folder-ios",
        "keys" to "folder-keys",
        "lib" to "folder-lib",
        "logs" to "folder-log",
        "middleware" to "folder-middleware",
        "mocks" to "folder-mock",
        "node_modules" to "folder-node",
        ".page-ide" to "folder-page",
        "packages" to "folder-packages",
        "plugins" to "folder-plugin",
        "public" to "folder-public",
        "venv" to "folder-python",
        "resources" to "folder-resource",
        "routes" to "folder-routes",
        "scripts" to "folder-scripts",
        "server" to "folder-server",
        "vendor" to "folder-shared",
        "src" to "folder-src",
        "store" to "folder-store",
        "target" to "folder-target",
        "tmp" to "folder-temp",
        "test" to "folder-test",
        "themes" to "folder-theme",
        "tools" to "folder-tools",
        "utils" to "folder-utils",
        "videos" to "folder-video",
        "views" to "folder-views",
    )

    @Test
    fun `maps files to their icon`() {
        for ((name, icon) in fileProbes) {
            assertEquals("fileicons/$icon.svg", FileIcons.resourceFor(Paths.get(name), isDirectory = false), name)
        }
    }

    @Test
    fun `maps folders to their icon`() {
        for ((name, icon) in folderProbes) {
            assertEquals("fileicons/$icon.svg", FileIcons.resourceFor(Paths.get(name), isDirectory = true), name)
        }
    }

    @Test
    fun `unknown extension has no icon`() {
        assertEquals(null, FileIcons.resourceFor(Paths.get("a.qqq"), isDirectory = false))
    }

    @Test
    fun `every mapped icon exists as a resource`() {
        val loader = FileIcons::class.java.classLoader
        val mapped = (fileProbes.map { it.second } + folderProbes.map { it.second }).toSet()
        for (icon in mapped) {
            assertNotNull(loader.getResource("fileicons/$icon.svg"), icon)
        }
        assertNotNull(loader.getResource(FileIcons.documentResource()))
    }

    @Test
    fun `every bundled icon is reachable`() {
        val shipped = iconDirectory().listDirectoryEntries("*.svg").map { it.name.removeSuffix(".svg") }.toSet()
        val reachable = (fileProbes.map { it.second } + folderProbes.map { it.second } + "document").toSet()
        val orphans = shipped - reachable
        assertTrue(orphans.isEmpty(), "icons no filename maps to: ${orphans.sorted()}")
    }

    private fun iconDirectory(): Path {
        val marker = FileIcons::class.java.classLoader.getResource("fileicons/kotlin.svg")
        assertNotNull(marker, "fileicons resources missing")
        return Paths.get(marker.toURI()).parent
    }
}
