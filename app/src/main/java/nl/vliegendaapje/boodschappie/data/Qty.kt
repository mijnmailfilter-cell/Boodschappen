package nl.vliegendaapje.boodschappie.data

import java.util.Locale
import kotlin.math.max
import kotlin.math.roundToLong

object Qty {
    private val ladder = listOf<Long>(50, 100, 150, 200, 250, 300, 400, 500, 600, 750, 1000, 1250, 1500, 2000, 2500, 3000, 4000, 5000)
    val quickSt = listOf<Long>(1, 2, 3, 4, 5, 6, 10, 12)
    val quickG = listOf<Long>(100, 200, 250, 500, 750, 1000, 1500, 2000)

    fun default(unit: String): Long = if (unit == "g") 250 else 1

    fun step(q: Long, unit: String, up: Boolean): Long =
        if (unit == "g") {
            if (up) ladder.firstOrNull { it > q } ?: (q + 1000)
            else ladder.lastOrNull { it < q } ?: q
        } else {
            if (up) q + 1 else max(1, q - 1)
        }

    fun canDecrease(q: Long, unit: String) = if (unit == "g") q > ladder.first() else q > 1

    fun label(q: Long, unit: String): String =
        if (unit == "g") {
            if (q >= 1000) {
                val s = if (q % 1000 == 0L) "${q / 1000}"
                else String.format(Locale.forLanguageTag("nl-NL"), "%.2f", q / 1000.0).trimEnd('0').trimEnd(',')
                "$s kg"
            } else "$q g"
        } else "$q"
}

/** Leest "3 bananen", "500g gehakt", "1,5 kg aardappelen", "2x melk". */
object QtyParser {
    data class Parsed(val name: String, val qty: Long?, val unit: String?)

    private val regex = Regex("""^(\d+(?:[.,]\d+)?)\s*(kg|kilo|gram|gr|g|x|stuks|stuk|st)?\.?\s+(.+)$""", RegexOption.IGNORE_CASE)

    fun parse(raw: String): Parsed {
        val s = raw.trim()
        val m = regex.find(s) ?: return Parsed(s, null, null)
        val num = m.groupValues[1].replace(',', '.').toDoubleOrNull() ?: return Parsed(s, null, null)
        val name = m.groupValues[3].trim()
        return when (m.groupValues[2].lowercase()) {
            "kg", "kilo" -> Parsed(name, (num * 1000).roundToLong(), "g")
            "gram", "gr", "g" -> Parsed(name, max(1, num.roundToLong()), "g")
            else -> Parsed(name, max(1, num.roundToLong()), "st")
        }
    }
}
