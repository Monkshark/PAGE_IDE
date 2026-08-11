package page.app

import java.io.PrintWriter
import java.io.StringWriter
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object CrashLog {

    private const val DIR_NAME = ".page-ide"
    private const val LOGS_DIR = "logs"
    private const val FILE_NAME = "page.log"
    private const val MAX_BYTES = 512L * 1024L

    private val stamp = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    fun file(): Path {
        val home = System.getProperty("page.settings.dir")?.takeIf { it.isNotBlank() }?.let(Path::of)
            ?: System.getProperty("user.home")?.let(Path::of)?.resolve(DIR_NAME)
            ?: Path.of(".").resolve(DIR_NAME)
        return home.resolve(LOGS_DIR).resolve(FILE_NAME)
    }

    fun install() {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            record(error, "uncaught on ${thread.name}")
            previous?.uncaughtException(thread, error)
        }
        write("started")
    }

    fun record(error: Throwable, note: String? = null) {
        val trace = StringWriter().also { error.printStackTrace(PrintWriter(it)) }.toString()
        write(buildString {
            append(note ?: "error")
            append('\n')
            append(trace.trimEnd())
        })
    }

    fun write(message: String) {
        runCatching {
            val path = file()
            Files.createDirectories(path.parent)
            rollIfLarge(path)
            Files.writeString(
                path,
                "${LocalDateTime.now().format(stamp)}  $message\n",
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND,
            )
        }
    }

    internal fun rollIfLarge(path: Path) {
        if (!Files.exists(path)) return
        if (Files.size(path) < MAX_BYTES) return
        val rolled = path.resolveSibling(path.fileName.toString() + ".1")
        runCatching { Files.deleteIfExists(rolled) }
        runCatching { Files.move(path, rolled) }
    }
}
