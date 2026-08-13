package page.lsp

/**
 * Reads the search path out of an environment map.
 *
 * Windows spells the variable `Path`, and copying `System.getenv()` into a plain map drops the
 * case-insensitive lookup the JDK gives it. Asking for `"PATH"` there finds nothing, which silently
 * turns off every PATH-based server lookup.
 */
fun searchPathOf(env: Map<String, String>): String? =
    env.entries.firstOrNull { it.key.equals("PATH", ignoreCase = true) }?.value

/**
 * Splits a search path into the directories worth probing.
 *
 * A real Windows PATH repeats itself — this machine lists Git, the nvm shim and the Python scripts
 * directory twice each. Probing a directory a second time can only produce the answer it already
 * gave, and every repeat shows up as another line in the "not found" list a reader has to scan.
 */
fun searchPathEntries(pathEnv: String?): List<String> {
    if (pathEnv.isNullOrBlank()) return emptyList()
    val separator = System.getProperty("path.separator") ?: ":"
    val seen = LinkedHashSet<String>()
    for (entry in pathEnv.split(separator)) {
        val trimmed = entry.trim().trimEnd('\\', '/')
        if (trimmed.isEmpty()) continue
        seen += trimmed
    }
    return seen.toList()
}
