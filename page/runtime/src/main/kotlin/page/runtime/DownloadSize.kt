package page.runtime

import java.net.HttpURLConnection
import java.net.URI
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

object DownloadSize {

    private val cache = ConcurrentHashMap<String, Long>()

    fun of(url: String): Long? {
        cache[url]?.let { return it }
        val bytes = runCatching { head(url) }.getOrNull() ?: return null
        if (bytes <= 0L) return null
        cache[url] = bytes
        return bytes
    }

    fun format(bytes: Long): String {
        val mb = bytes.toDouble() / (1024 * 1024)
        return when {
            mb >= 1024 -> String.format(Locale.ROOT, "%.2f GB", mb / 1024)
            mb >= 100 -> String.format(Locale.ROOT, "%.0f MB", mb)
            else -> String.format(Locale.ROOT, "%.1f MB", mb)
        }
    }

    internal fun forget() {
        cache.clear()
    }

    private fun head(url: String): Long {
        val conn = URI(url).toURL().openConnection() as HttpURLConnection
        conn.requestMethod = "HEAD"
        conn.instanceFollowRedirects = true
        conn.connectTimeout = 8_000
        conn.readTimeout = 8_000
        conn.setRequestProperty("User-Agent", "PAGE-IDE/0.1 DownloadSize")
        try {
            if (conn.responseCode !in 200..299) return -1L
            return conn.contentLengthLong
        } finally {
            conn.disconnect()
        }
    }
}
