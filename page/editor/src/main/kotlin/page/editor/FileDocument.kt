package page.editor

import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap

object FileDocument {

    private val crlfFiles = ConcurrentHashMap.newKeySet<String>()

    fun load(path: Path): String {
        val raw = Files.readString(path, StandardCharsets.UTF_8)
        if (usesCrlf(raw)) crlfFiles.add(key(path)) else crlfFiles.remove(key(path))
        return normalize(raw)
    }

    fun loadOrNull(path: Path): String? = try {
        load(path)
    } catch (_: IOException) {
        null
    }

    fun save(path: Path, text: String) {
        Files.writeString(path, denormalize(text, crlfFiles.contains(key(path))), StandardCharsets.UTF_8)
    }

    internal fun usesCrlf(raw: String): Boolean = raw.contains("\r\n")

    internal fun normalize(raw: String): String =
        if (raw.indexOf('\r') < 0) raw else raw.replace("\r\n", "\n").replace('\r', '\n')

    internal fun denormalize(text: String, crlf: Boolean): String =
        if (!crlf) text else text.replace("\n", "\r\n")

    internal fun forgetLineEndings() {
        crlfFiles.clear()
    }

    private fun key(path: Path): String =
        runCatching { path.toAbsolutePath().normalize().toString() }.getOrDefault(path.toString())
}
