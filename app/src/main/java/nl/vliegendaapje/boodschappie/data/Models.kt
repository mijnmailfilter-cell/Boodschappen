package nl.vliegendaapje.boodschappie.data

const val ALL = "__alles__"

data class ShopList(val id: String, val name: String, val order: Long)

data class Item(
    val id: String,
    val listId: String,
    val name: String,
    val category: Int,
    val qty: Long,
    val unit: String, // "st" of "g"
    val checked: Boolean,
    val checkedAt: Long,
    val createdAt: Long,
)

data class HistoryEntry(
    val key: String,
    val name: String,
    val category: Int,
    val unit: String,
    val qty: Long,
    val count: Long,
)

data class Suggestion(
    val name: String,
    val category: Int,
    val unit: String,
    val qty: Long,
    val fromHistory: Boolean,
)

data class Category(val id: Int, val name: String, val emoji: String)

object Categories {
    const val OVERIG = 17
    val all = listOf(
        Category(1, "Groente & fruit", "🥦"),
        Category(2, "Brood & bakkerij", "🥖"),
        Category(3, "Vlees, vis & vega", "🥩"),
        Category(4, "Kaas & vleeswaren", "🧀"),
        Category(5, "Zuivel & eieren", "🥛"),
        Category(6, "Ontbijt, koffie & thee", "🥣"),
        Category(7, "Pasta, rijst & wereldkeuken", "🍝"),
        Category(8, "Conserven, soep & sauzen", "🥫"),
        Category(9, "Kruiden, olie & bakken", "🧂"),
        Category(10, "Koek, snoep & chocolade", "🍪"),
        Category(11, "Chips & noten", "🥜"),
        Category(12, "Diepvries", "🧊"),
        Category(13, "Dranken", "🥤"),
        Category(14, "Huishouden", "🧻"),
        Category(15, "Verzorging & drogist", "🧴"),
        Category(16, "Baby & huisdier", "🐾"),
        Category(OVERIG, "Overig", "📦"),
    )

    fun get(id: Int): Category = all.firstOrNull { it.id == id } ?: all.last()
    fun order(id: Int): Int = all.indexOfFirst { it.id == id }.let { if (it < 0) 999 else it }
}
