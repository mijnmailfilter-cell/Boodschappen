package nl.vliegendaapje.boodschappie.data

/**
 * Maakt van ingesproken tekst losse regels.
 *  "2 flessen wijn 3 stukken kaas en 5 fietsen" -> ["2 wijn", "3 kaas", "5 fietsen"]
 *  "melk kaas brood"                           -> ["melk", "kaas", "brood"]
 *  "een pond gehakt en twee ons ham"           -> ["500 g gehakt", "200 g ham"]
 */
object VoiceParser {
    private val numberWords = mapOf(
        "twee" to 2, "drie" to 3, "vier" to 4, "vijf" to 5, "zes" to 6, "zeven" to 7, "acht" to 8,
        "negen" to 9, "tien" to 10, "elf" to 11, "twaalf" to 12, "dertien" to 13, "veertien" to 14,
        "vijftien" to 15, "twintig" to 20, "dertig" to 30, "veertig" to 40, "vijftig" to 50,
    )
    private val numberRegex = Regex("""\b(${numberWords.keys.joinToString("|")})\b""")
    private val separators = Regex("""\s*(?:,|;|\.(?!\d)|\ben\b|\bplus\b|\book nog\b|\bnog\b|\bverder\b)\s*""")
    private val beforeNumber = Regex("""\s+(?=\d)""")
    private val onlyQty = Regex("""^\d+(?:[.,]\d+)?\s*(?:g|gr|gram|kg|kilo|x|st|stuks)?$""")
    private val qtyThenRest = Regex("""^(\d+(?:[.,]\d+)?\s*(?:g|gr|gram|kg|kilo)?)\s+(.+)$""")
    private val containers = Regex(
        """^(?:flessen|fles|flesjes|flesje|stukken|stuks|stuk|stukjes|stukje|pakken|pakjes|pakje|pak|""" +
            """zakken|zakjes|zakje|zak|blikken|blikjes|blikje|blik|potten|potjes|potje|pot|dozen|doosjes|doosje|doos|""" +
            """bossen|bosjes|bosje|bos|kratten|krat|rollen|rol|tubes|tube|bakjes|bakje|bakken|bak|netjes|netje|keer|x)""" +
            """\s+(?:van\s+)?(?:de\s+|het\s+)?""",
    )
    private val protect = listOf("doperwten en wortelen")

    fun split(spoken: String, isKnown: (String) -> Boolean = { Catalog.find(it) != null }): List<String> {
        var s = " " + spoken.lowercase().trim() + " "
        s = s.replace(Regex("""^\s*(zet|doe|graag|ik wil|ik heb nodig|we hebben nodig|ik moet)\s+"""), " ")
            .replace(Regex("""\s+(op de lijst|op het lijstje|op de boodschappenlijst|erbij|kopen|halen)\s*$"""), " ")
        protect.forEach { s = s.replace(it, it.replace(" ", "_")) }

        // getallen in woorden -> cijfers (behalve "een", zie onder)
        s = s.replace(numberRegex) { numberWords.getValue(it.value).toString() }
        // gewichten
        s = s.replace(Regex("""\b(\d+)\s+pond\b""")) { "${it.groupValues[1].toInt() * 500} g" }
            .replace(Regex("""\b(\d+)\s+ons\b""")) { "${it.groupValues[1].toInt() * 100} g" }
            .replace(Regex("""\b(?:een\s+)?halve\s+kilo\b"""), "500 g")
            .replace(Regex("""\banderhalve\s+kilo\b"""), "1500 g")
            .replace(Regex("""\b(?:een\s+)?pond\b"""), "500 g")
            .replace(Regex("""\b(?:een\s+)?ons\b"""), "100 g")
            .replace(Regex("""\b(?:één|een)\b"""), "1")

        val out = mutableListOf<String>()
        for (chunk in s.split(separators)) {
            val pieces = chunk.trim().split(beforeNumber).map { it.trim() }.filter { it.isNotEmpty() }
            for (piece in pieces) {
                // "kaas 500 g" -> hoort bij het vorige product
                if (onlyQty.matches(piece)) {
                    if (out.isNotEmpty() && !out.last().first().isDigit()) out[out.size - 1] = "$piece ${out.last()}"
                    continue
                }
                val m = qtyThenRest.find(piece)
                if (m != null) {
                    val qty = m.groupValues[1]
                    val name = m.groupValues[2].replace(containers, "").trim()
                    if (name.isEmpty()) continue
                    val parts = segment(name, isKnown)
                    out += "$qty ${parts.first()}"
                    out += parts.drop(1)
                } else {
                    out += segment(piece, isKnown)
                }
            }
        }
        return out.map { it.replace("_", " ").trim() }.filter { it.isNotBlank() }
    }

    /** "melk kaas brood" -> 3 producten, maar alleen als elk stuk een bekend product is. */
    private fun segment(text: String, isKnown: (String) -> Boolean): List<String> {
        val words = text.split(Regex("\\s+")).filter { it.isNotEmpty() }
        if (words.size < 2 || isKnown(text.replace("_", " "))) return listOf(text)
        val segs = mutableListOf<String>()
        var i = 0
        while (i < words.size) {
            var found = false
            for (j in words.size downTo i + 1) {
                val cand = words.subList(i, j).joinToString(" ")
                if (isKnown(cand.replace("_", " "))) {
                    segs += cand; i = j; found = true; break
                }
            }
            if (!found) return listOf(text)
        }
        return segs
    }
}
