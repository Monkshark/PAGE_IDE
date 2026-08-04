package page.workspace

import java.nio.file.Path

internal object FileIcons {
    private const val DIR = "fileicons"

    fun resourceFor(path: Path, isDirectory: Boolean): String? {
        val name = path.fileName?.toString()?.lowercase() ?: return null
        if (isDirectory) return "$DIR/${folderIconName(name)}.svg"
        when {
            name.endsWith(".gradle") || name.endsWith(".gradle.kts") -> return "$DIR/gradle.svg"
            name == ".gitignore" || name == ".gitattributes" || name == ".gitmodules" -> return "$DIR/git.svg"
            name == ".editorconfig" -> return "$DIR/editorconfig.svg"
        }
        val ext = name.substringAfterLast('.', "")
        return EXT[ext]?.let { "$DIR/$it.svg" }
    }

    fun documentResource(): String = "$DIR/document.svg"

    private fun folderIconName(name: String): String = when {
        name == ".git" -> "folder-git"
        name == ".gradle" -> "folder-gradle"
        name == "build" || name == "out" || name == "dist" -> "folder-dist"
        name == "target" || name == "bin" -> "folder-target"
        name == "node_modules" -> "folder-node"
        name == ".cache" || name == ".idea" || name == ".kotlin" || name == "tmp" || name == "temp" ||
            name == ".vscode" || name.endsWith("-cache") -> "folder-temp"
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
        name == "src" || name == "main" || name == "kotlin" || name == "java" -> "folder-src"
        else -> "folder-base"
    }

    private val EXT = mapOf(
        "kt" to "kotlin", "kts" to "kotlin",
        "java" to "java",
        "js" to "javascript", "mjs" to "javascript", "cjs" to "javascript", "jsx" to "javascript",
        "ts" to "typescript", "tsx" to "typescript",
        "py" to "python",
        "go" to "go",
        "rs" to "rust",
        "json" to "json",
        "yaml" to "yaml", "yml" to "yaml",
        "md" to "markdown", "markdown" to "markdown",
        "html" to "html", "htm" to "html",
        "css" to "css",
        "xml" to "xml",
        "svg" to "svg",
        "png" to "image", "jpg" to "image", "jpeg" to "image", "gif" to "image", "webp" to "image", "bmp" to "image", "ico" to "image",
        "bat" to "console", "cmd" to "console", "sh" to "console", "bash" to "console", "zsh" to "console",
        "ps1" to "powershell",
        "toml" to "toml",
        "lock" to "lock",
        "iml" to "settings", "properties" to "settings", "ini" to "settings", "cfg" to "settings", "conf" to "settings",
    )
}
