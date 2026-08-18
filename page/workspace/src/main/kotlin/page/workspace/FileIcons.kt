package page.workspace

import java.nio.file.Path

object FileIcons {
    private const val DIR = "fileicons"

    fun resourceFor(path: Path, isDirectory: Boolean): String? {
        val name = path.fileName?.toString()?.lowercase() ?: return null
        if (isDirectory) {
            if (isUserHome(path)) return "$DIR/folder-home.svg"
            return "$DIR/${folderIconName(name)}.svg"
        }
        fileIconByName(name)?.let { return "$DIR/$it.svg" }
        val ext = name.substringAfterLast('.', "")
        return EXT[ext]?.let { "$DIR/$it.svg" }
    }

    fun documentResource(): String = "$DIR/document.svg"

    internal fun isUserHome(path: Path, home: String? = System.getProperty("user.home")): Boolean {
        if (home.isNullOrBlank()) return false
        return runCatching {
            path.toAbsolutePath().normalize() == java.nio.file.Path.of(home).toAbsolutePath().normalize()
        }.getOrDefault(false)
    }

    private fun fileIconByName(name: String): String? = when {
        name.endsWith(".gradle") || name.endsWith(".gradle.kts") || name == "gradle.properties" -> "gradle"
        name == ".gitignore" || name == ".gitattributes" || name == ".gitmodules" -> "git"
        name == ".editorconfig" -> "editorconfig"
        name == "dockerfile" || name.startsWith("dockerfile.") || name.endsWith(".dockerfile") ||
            name == ".dockerignore" || name.startsWith("docker-compose.") || name.startsWith("compose.y") -> "docker"
        name == "package.json" || name == "package-lock.json" || name == ".npmrc" || name == ".nvmrc" -> "nodejs"
        name == "pubspec.yaml" || name == "pubspec.lock" -> "flutter"
        name == "go.mod" || name == "go.sum" || name == "go.work" -> "go"
        name == "cargo.toml" || name == "cargo.lock" -> "rust"
        name == "pom.xml" -> "maven"
        name == "cmakelists.txt" -> "cmake"
        name == "makefile" || name == "gnumakefile" || name.startsWith("makefile.") -> "makefile"
        name == "license" || name == "licence" || name == "copying" ||
            name.startsWith("license.") || name.startsWith("licence.") -> "certificate"
        name == "gemfile" || name == "gemfile.lock" || name == ".ruby-version" -> "ruby"
        name.startsWith("tsconfig.") -> "typescript"
        name.startsWith("vite.config.") -> "vite"
        name.startsWith("webpack.config.") -> "webpack"
        name.startsWith("tailwind.config.") -> "tailwindcss"
        name.startsWith(".eslintrc") || name.startsWith("eslint.config.") -> "eslint"
        name.startsWith(".prettierrc") || name.startsWith("prettier.config.") -> "prettier"
        name == ".env" || name.startsWith(".env.") -> "tune"
        name == "requirements.txt" || name == "pyproject.toml" || name == "pipfile" -> "python"
        else -> null
    }

    private fun folderIconName(name: String): String = when {
        name == ".page-ide" -> "folder-page"
        name == ".git" -> "folder-git"
        name == ".gradle" -> "folder-gradle"
        name == "build" || name == "out" || name == "dist" -> "folder-dist"
        name == "target" || name == "bin" -> "folder-target"
        name == "node_modules" -> "folder-node"
        name == ".cache" || name == ".idea" || name == ".kotlin" || name == "tmp" || name == "temp" ||
            name.endsWith("-cache") -> "folder-temp"
        name == "test" || name == "tests" || name == "__tests__" -> "folder-test"
        name == "docs" || name == "doc" -> "folder-docs"
        name == "resources" || name == "res" || name == "assets" || name == "asset" -> "folder-resource"
        name == "images" || name == "img" || name == "icons" -> "folder-images"
        name == "config" || name == "conf" || name == "cfg" || name == "settings" -> "folder-config"
        name == "scripts" || name == "script" -> "folder-scripts"
        name == "public" -> "folder-public"
        name == "lib" || name == "libs" -> "folder-lib"
        name == ".github" -> "folder-github"
        name == "app" -> "folder-app"
        name == "components" || name == "component" -> "folder-components"
        name == "db" || name == "database" -> "folder-database"
        name == "i18n" || name == "locale" || name == "locales" || name == "lang" || name == "translations" -> "folder-i18n"
        name == "plugin" || name == "plugins" -> "folder-plugin"
        name == "server" -> "folder-server"
        name == "client" -> "folder-client"
        name == "css" || name == "styles" || name == "style" -> "folder-css"
        name == "api" -> "folder-api"
        name == "core" || name == "common" -> "folder-core"
        name == "utils" || name == "util" || name == "helpers" || name == "helper" -> "folder-utils"
        name == "types" || name == "interfaces" || name == "model" || name == "models" ||
            name == "entity" || name == "entities" -> "folder-interface"
        name == "venv" || name == ".venv" || name == "__pycache__" || name == ".pytest_cache" -> "folder-python"
        name == "android" -> "folder-android"
        name == "ios" -> "folder-ios"
        name == "packages" || name == "apps" || name == "modules" -> "folder-packages"
        name == "tools" || name == "tooling" -> "folder-tools"
        name == "examples" || name == "example" || name == "samples" || name == "sample" ||
            name == "demo" || name == "demos" -> "folder-examples"
        name == "fonts" || name == "font" -> "folder-font"
        name == "logs" || name == "log" -> "folder-log"
        name == "coverage" -> "folder-coverage"
        name == "mock" || name == "mocks" || name == "__mocks__" || name == "fixtures" -> "folder-mock"
        name == "vendor" || name == "vendors" || name == "third_party" || name == "shared" -> "folder-shared"
        name == "include" || name == "includes" || name == "headers" -> "folder-include"
        name == "keys" || name == "secrets" || name == ".ssh" || name == "cert" || name == "certs" -> "folder-keys"
        name == "ci" || name == ".ci" || name == ".circleci" -> "folder-ci"
        name == "hooks" || name == "hook" -> "folder-hook"
        name == "store" || name == "stores" || name == "redux" || name == "state" -> "folder-store"
        name == "routes" || name == "router" || name == "routing" -> "folder-routes"
        name == "middleware" || name == "middlewares" -> "folder-middleware"
        name == "controllers" || name == "controller" || name == "handlers" -> "folder-controller"
        name == "views" || name == "view" || name == "screens" || name == "pages" || name == "layouts" -> "folder-views"
        name == "theme" || name == "themes" -> "folder-theme"
        name == "video" || name == "videos" || name == "movies" -> "folder-video"
        name == "desktop" -> "folder-desktop"
        name == "downloads" || name == "download" -> "folder-download"
        name == "documents" || name == "my documents" -> "folder-docs"
        name == "pictures" || name == "photos" || name == "my pictures" -> "folder-images"
        name == "music" || name == "my music" -> "folder-audio"
        name == "users" || name == "home" -> "folder-home"
        name == "onedrive" || name == "dropbox" || name == "google drive" -> "folder-shared"
        name == "favorites" || name == "links" || name == "searches" -> "folder-secure"
        name == "saved games" || name == "3d objects" || name == "contacts" -> "folder-archive"
        name == "java" -> "folder-java"
        name == "kotlin" -> "folder-kotlin"
        name == "javascript" || name == "js" -> "folder-javascript"
        name == "typescript" || name == "ts" -> "folder-typescript"
        name == "rust" -> "folder-rust"
        name == "go" || name == "golang" -> "folder-go"
        name == "php" -> "folder-php"
        name == "dart" -> "folder-dart"
        name == "scala" -> "folder-scala"
        name == "lua" -> "folder-lua"
        name == "flutter" -> "folder-flutter"
        name == "vue" -> "folder-vue"
        name == "svelte" -> "folder-svelte"
        name == "angular" -> "folder-angular"
        name == "next" || name == ".next" -> "folder-next"
        name == "docker" || name == ".docker" -> "folder-docker"
        name == "kubernetes" || name == "k8s" -> "folder-kubernetes"
        name == "terraform" || name == ".terraform" -> "folder-terraform"
        name == "aws" || name == ".aws" -> "folder-aws"
        name == ".gitlab" -> "folder-gitlab"
        name == ".vscode" -> "folder-vscode"
        name == "graphql" || name == "gql" -> "folder-graphql"
        name == "proto" || name == "protos" -> "folder-proto"
        name == "webpack" -> "folder-webpack"
        name == "cypress" -> "folder-cypress"
        name == "storybook" || name == ".storybook" -> "folder-storybook"
        name == ".husky" -> "folder-husky"
        name == "benchmark" || name == "benchmarks" || name == "bench" -> "folder-benchmark"
        name == "sass" || name == "scss" -> "folder-sass"
        name == "less" -> "folder-less"
        name == "audio" || name == "sounds" || name == "sound" -> "folder-audio"
        name == "svg" || name == "svgs" -> "folder-svg"
        name == "errors" || name == "error" -> "folder-error"
        name == "events" || name == "event" -> "folder-event"
        name == "jobs" || name == "job" -> "folder-job"
        name == "queues" || name == "queue" -> "folder-queue"
        name == "rules" -> "folder-rules"
        name == "src" || name == "main" -> "folder-src"
        else -> "folder-base"
    }

    private val EXT = mapOf(
        "kt" to "kotlin", "kts" to "kotlin",
        "java" to "java",
        "js" to "javascript", "mjs" to "javascript", "cjs" to "javascript", "jsx" to "javascript",
        "ts" to "typescript", "tsx" to "typescript",
        "py" to "python", "pyi" to "python", "pyw" to "python",
        "go" to "go",
        "rs" to "rust",
        "c" to "c", "h" to "c",
        "cc" to "cpp", "cpp" to "cpp", "cxx" to "cpp", "hh" to "cpp", "hpp" to "cpp", "hxx" to "cpp",
        "cs" to "csharp", "csx" to "csharp", "csproj" to "csharp", "sln" to "csharp",
        "dart" to "dart",
        "swift" to "swift",
        "rb" to "ruby", "rake" to "ruby", "gemspec" to "ruby", "erb" to "ruby",
        "php" to "php", "phtml" to "php",
        "vue" to "vue",
        "svelte" to "svelte",
        "lua" to "lua",
        "scala" to "scala", "sbt" to "scala", "sc" to "scala",
        "sql" to "database", "db" to "database", "sqlite" to "database", "sqlite3" to "database",
        "graphql" to "graphql", "gql" to "graphql",
        "proto" to "proto",
        "tf" to "terraform", "tfvars" to "terraform", "tfstate" to "terraform", "hcl" to "terraform",
        "ipynb" to "jupyter",
        "json" to "json", "jsonc" to "json", "json5" to "json",
        "yaml" to "yaml", "yml" to "yaml",
        "md" to "markdown", "markdown" to "markdown",
        "html" to "html", "htm" to "html",
        "css" to "css",
        "scss" to "sass", "sass" to "sass",
        "less" to "less",
        "xml" to "xml",
        "svg" to "svg",
        "png" to "image", "jpg" to "image", "jpeg" to "image", "gif" to "image", "webp" to "image",
        "bmp" to "image", "ico" to "image", "avif" to "image", "tiff" to "image",
        "pdf" to "pdf",
        "zip" to "zip", "tar" to "zip", "gz" to "zip", "tgz" to "zip", "bz2" to "zip", "xz" to "zip",
        "7z" to "zip", "rar" to "zip", "jar" to "zip", "war" to "zip",
        "ttf" to "font", "otf" to "font", "woff" to "font", "woff2" to "font", "eot" to "font",
        "mp4" to "video", "webm" to "video", "mov" to "video", "mkv" to "video", "avi" to "video", "m4v" to "video",
        "mp3" to "audio", "wav" to "audio", "ogg" to "audio", "flac" to "audio", "m4a" to "audio", "aac" to "audio",
        "log" to "log",
        "txt" to "document", "text" to "document", "rtf" to "document",
        "csv" to "table", "tsv" to "table", "xls" to "table", "xlsx" to "table",
        "bat" to "console", "cmd" to "console", "sh" to "console", "bash" to "console", "zsh" to "console",
        "ps1" to "powershell",
        "toml" to "toml",
        "lock" to "lock",
        "iml" to "settings", "properties" to "settings", "ini" to "settings", "cfg" to "settings", "conf" to "settings",
    )
}
