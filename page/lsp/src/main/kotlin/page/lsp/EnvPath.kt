package page.lsp

fun searchPathOf(env: Map<String, String>): String? =
    env.entries.firstOrNull { it.key.equals("PATH", ignoreCase = true) }?.value

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
