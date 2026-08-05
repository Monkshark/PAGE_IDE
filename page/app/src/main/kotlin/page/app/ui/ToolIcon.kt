package page.app.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import page.ui.Glass

fun toolIconResource(id: String, displayName: String): String? {
    val key = "$id $displayName".lowercase()
    return when {
        "kotlin" in key -> "fileicons/kotlin.svg"
        "typescript" in key -> "fileicons/typescript.svg"
        "javascript" in key -> "fileicons/javascript.svg"
        id == "node" || "node" in key -> "fileicons/nodejs.svg"
        "python" in key -> "fileicons/python.svg"
        "rust" in key -> "fileicons/rust.svg"
        id == "cpp" || id == "cpp-toolchain" || id == "mingw-toolchain" ||
            "c++" in key || "clang" in key || "mingw" in key -> "fileicons/cpp.svg"
        id == "c" -> "fileicons/c.svg"
        "java" in key || id == "jdk" -> "fileicons/java.svg"
        "go" in key -> "fileicons/go.svg"
        "flutter" in key -> "fileicons/flutter.svg"
        "dart" in key -> "fileicons/dart.svg"
        "swift" in key -> "fileicons/swift.svg"
        "ruby" in key -> "fileicons/ruby.svg"
        "php" in key -> "fileicons/php.svg"
        "dotnet" in key || "csharp" in key || ".net" in key || "c#" in key -> "fileicons/csharp.svg"
        "vue" in key -> "fileicons/vue.svg"
        "svelte" in key -> "fileicons/svelte.svg"
        "docker" in key -> "fileicons/docker.svg"
        "sql" in key -> "fileicons/database.svg"
        "html" in key -> "fileicons/html.svg"
        "css" in key -> "fileicons/css.svg"
        "json" in key -> "fileicons/json.svg"
        "yaml" in key -> "fileicons/yaml.svg"
        "markdown" in key -> "fileicons/markdown.svg"
        "powershell" in key -> "fileicons/powershell.svg"
        "bash" in key || "shell" in key -> "fileicons/console.svg"
        "toml" in key -> "fileicons/toml.svg"
        else -> null
    }
}

private fun monogram(displayName: String): String {
    val c = displayName.trimStart('.', ' ', '#').firstOrNull { it.isLetterOrDigit() } ?: '?'
    return c.uppercaseChar().toString()
}

@Composable
fun ToolIcon(id: String, displayName: String, size: Dp = 16.dp) {
    val res = toolIconResource(id, displayName)
    if (res != null) {
        Image(
            painter = page.ui.cachedSvgPainter(res),
            contentDescription = null,
            modifier = Modifier.size(size),
        )
        return
    }
    val colors = Glass.colors
    val palette = listOf(
        colors.primary, colors.accent, colors.syntax.type,
        colors.syntax.string, colors.syntax.number, colors.warn, colors.success,
    )
    val tint = palette[(id.hashCode() and 0x7fffffff) % palette.size]
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.28f))
            .background(tint.copy(alpha = 0.16f)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = monogram(displayName),
            color = tint,
            style = LocalTextStyle.current.copy(
                fontSize = (size.value * 0.52f).sp,
                lineHeight = (size.value * 0.52f).sp,
                fontWeight = FontWeight.Bold,
                lineHeightStyle = LineHeightStyle(
                    alignment = LineHeightStyle.Alignment.Center,
                    trim = LineHeightStyle.Trim.Both,
                ),
            ),
        )
    }
}
