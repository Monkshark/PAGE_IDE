package page.workspace

import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
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
        "Desktop" to "folder-desktop",
        "Downloads" to "folder-download",
        "Documents" to "folder-docs",
        "Pictures" to "folder-images",
        "Music" to "folder-audio",
        "Users" to "folder-home",
        "OneDrive" to "folder-shared",
        "Favorites" to "folder-secure",
        "3D Objects" to "folder-archive",
        "views" to "folder-views",
        "java" to "folder-java",
        "kotlin" to "folder-kotlin",
        "javascript" to "folder-javascript",
        "typescript" to "folder-typescript",
        "rust" to "folder-rust",
        "go" to "folder-go",
        "php" to "folder-php",
        "dart" to "folder-dart",
        "scala" to "folder-scala",
        "lua" to "folder-lua",
        "flutter" to "folder-flutter",
        "vue" to "folder-vue",
        "svelte" to "folder-svelte",
        "angular" to "folder-angular",
        ".next" to "folder-next",
        "docker" to "folder-docker",
        "k8s" to "folder-kubernetes",
        "terraform" to "folder-terraform",
        ".aws" to "folder-aws",
        ".gitlab" to "folder-gitlab",
        ".vscode" to "folder-vscode",
        "graphql" to "folder-graphql",
        "proto" to "folder-proto",
        "webpack" to "folder-webpack",
        "cypress" to "folder-cypress",
        ".storybook" to "folder-storybook",
        ".husky" to "folder-husky",
        "benchmarks" to "folder-benchmark",
        "scss" to "folder-sass",
        "less" to "folder-less",
        "sounds" to "folder-audio",
        "svg" to "folder-svg",
        "errors" to "folder-error",
        "events" to "folder-event",
        "jobs" to "folder-job",
        "queues" to "folder-queue",
        "rules" to "folder-rules",
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
    fun `the profile folder is a home no matter what it is called`() {
        val home = java.nio.file.Path.of(System.getProperty("user.home"))
        assertTrue(FileIcons.isUserHome(home))
        assertFalse(FileIcons.isUserHome(home.resolve("Desktop")))
        assertEquals("fileicons/folder-home.svg", FileIcons.resourceFor(home, isDirectory = true))
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
