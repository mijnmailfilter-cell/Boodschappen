package nl.vliegendaapje.boodschappie.data

import java.text.Normalizer

object Suggest {
    private val marks = Regex("\\p{M}+")

    fun norm(s: String): String =
        Normalizer.normalize(s.trim().lowercase(), Normalizer.Form.NFD).replace(marks, "")

    private fun score(name: String, q: String): Int? {
        val n = norm(name)
        return when {
            n == q -> 0
            n.startsWith(q) -> 1
            n.split(' ', '-', '(').any { it.startsWith(q) } -> 2
            n.contains(q) -> 3
            else -> null
        }
    }

    fun search(query: String, history: List<HistoryEntry>, limit: Int = 8): List<Suggestion> {
        val q = norm(query)
        if (q.isEmpty()) return emptyList()
        data class Hit(val s: Suggestion, val score: Int, val count: Long)

        val hits = mutableListOf<Hit>()
        val seen = HashSet<String>()
        for (h in history) {
            val sc = score(h.name, q) ?: continue
            if (seen.add(norm(h.name))) hits += Hit(Suggestion(h.name, h.category, h.unit, h.qty, true), sc, h.count)
        }
        for (p in Catalog.products) {
            val sc = score(p.name, q) ?: continue
            if (seen.add(norm(p.name))) hits += Hit(Suggestion(p.name, p.category, p.unit, p.qty, false), sc, 0)
        }
        return hits
            .sortedWith(compareBy<Hit> { it.score }.thenByDescending { it.count }.thenBy { it.s.name.length })
            .take(limit)
            .map { it.s }
    }

    fun guessCategory(name: String, history: List<HistoryEntry>): Int {
        val n = norm(name)
        history.firstOrNull { norm(it.name) == n }?.let { return it.category }
        Catalog.find(name)?.let { return it.category }
        // langste bekende productnaam die in de getypte tekst zit, bijv. "biologische bananen"
        val best = Catalog.products
            .filter { norm(it.name).length >= 4 && n.contains(norm(it.name)) }
            .maxByOrNull { it.name.length }
        if (best != null) return best.category
        // of andersom: "olijfolie extra" -> begint met bekend product
        val prefix = Catalog.products.firstOrNull { n.length >= 4 && norm(it.name).startsWith(n) }
        return prefix?.category ?: Categories.OVERIG
    }
}
