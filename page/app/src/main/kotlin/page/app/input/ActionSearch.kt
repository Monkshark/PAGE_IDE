package page.app.input

data class ActionMatch(val spec: ActionSpec, val score: Int, val matchedIndices: List<Int>)

object ActionSearch {

    fun rank(query: String, actions: List<ActionSpec> = ActionCatalog.all): List<ActionMatch> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) {
            return actions
                .sortedWith(compareBy({ it.group.ordinal }, { it.label }))
                .map { ActionMatch(it, 0, emptyList()) }
        }
        return actions
            .mapNotNull { spec -> match(trimmed, spec) }
            .sortedWith(compareByDescending<ActionMatch> { it.score }.thenBy { it.spec.label })
    }

    private fun match(query: String, spec: ActionSpec): ActionMatch? {
        val haystack = spec.label
        val direct = haystack.indexOf(query, ignoreCase = true)
        if (direct >= 0) {
            val bonus = if (direct == 0) 200 else 100
            return ActionMatch(spec, bonus - direct, (direct until direct + query.length).toList())
        }
        val subsequence = subsequenceIndices(query, haystack)
        if (subsequence != null) {
            val spread = subsequence.last() - subsequence.first()
            return ActionMatch(spec, 60 - spread, subsequence)
        }
        if (spec.group.name.contains(query, ignoreCase = true)) {
            return ActionMatch(spec, 10, emptyList())
        }
        return null
    }

    private fun subsequenceIndices(query: String, target: String): List<Int>? {
        val hits = ArrayList<Int>(query.length)
        var at = 0
        for (c in query) {
            if (c == ' ') continue
            var found = -1
            var i = at
            while (i < target.length) {
                if (target[i].equals(c, ignoreCase = true)) {
                    found = i
                    break
                }
                i++
            }
            if (found < 0) return null
            hits += found
            at = found + 1
        }
        return if (hits.isEmpty()) null else hits
    }
}
