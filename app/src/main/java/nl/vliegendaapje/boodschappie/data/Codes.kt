package nl.vliegendaapje.boodschappie.data

import java.security.SecureRandom

object Codes {
    private const val ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
    private val random = SecureRandom()

    fun generate(): String = (1..8).map { ALPHABET[random.nextInt(ALPHABET.length)] }.joinToString("")
    fun clean(s: String): String = s.uppercase().filter { it.isLetterOrDigit() }
    fun pretty(code: String): String = code.chunked(4).joinToString("-")
    fun typing(s: String): String = s.uppercase().filter { it.isLetterOrDigit() || it == '-' }.take(9)
}
