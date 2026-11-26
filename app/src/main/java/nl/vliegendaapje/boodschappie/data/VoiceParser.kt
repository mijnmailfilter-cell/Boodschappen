package nl.vliegendaapje.boodschappie.data

/** Maakt van "melk, drie bananen en een pond gehakt" losse regels: ["melk", "3 bananen", "500 g gehakt"]. */
object VoiceParser {
    private val numbers = mapOf(
        "een" to 1, "één" to 1, "twee" to 2, "drie" to 3, "vier" to 4, "vijf" to 5, "zes" to 6,
        "zeven" to 7, "acht" to 8, "negen" to 9, "tien" to 10, "elf" to 11, "twaalf" to 12,
    )
    private val splitter = Regex("""\s*(?:,|;|\.|\ben\b|\bplus\b|\book nog\b|\bnog\b)\s*""", RegexOption.IGNORE_CASE)
    private val protect = listOf("doperwten en wortelen")

    fun split(spoken: String): List<String> {
        var s = " " + spoken.lowercase().trim() + " "
        s = s.replace(Regex("""^\s*(zet|doe|graag|ik wil|ik heb nodig|we hebben nodig)\s+"""), " ")
            .replace(Regex("""\s+(op de lijst|op het lijstje|erbij)\s*$"""), " ")
        protect.forEach { s = s.replace(it, it.replace(" en ", "_EN_")) }
        return s.split(splitter)
            .map { it.replace("_EN_", " en ").trim() }
            .filter { it.isNotBlank() }
            .map(::normalize)
    }

    private fun normalize(part: String): String {
        var p = part
            .replace(Regex("""^(een\s+)?halve\s+kilo\b"""), "500 g")
            .replace(Regex("""^anderhalve\s+kilo\b"""), "1500 g")
            .replace(Regex("""^(een\s+)?pond\b"""), "500 g")
            .replace(Regex("""^(een\s+)?ons\b"""), "100 g")
            .replace(Regex("""^(\d+)\s+ons\b""")) { "${it.groupValues[1].toInt() * 100} g" }
            .replace(Regex("""^(\d+)\s+pond\b""")) { "${it.groupValues[1].toInt() * 500} g" }
        val first = p.substringBefore(' ')
        val n = numbers[first]
        if (n != null && p.contains(' ')) p = "$n " + p.substringAfter(' ')
        // "3 pond" etc. na vervanging van het getal
        p = p.replace(Regex("""^(\d+)\s+ons\b""")) { "${it.groupValues[1].toInt() * 100} g" }
            .replace(Regex("""^(\d+)\s+pond\b""")) { "${it.groupValues[1].toInt() * 500} g" }
        return p
    }
}
