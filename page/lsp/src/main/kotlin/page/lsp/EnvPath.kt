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
