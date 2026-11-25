package nl.vliegendaapje.boodschappie.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

enum class JoinResult { OK, NOT_FOUND, OFFLINE }

class Repo {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private fun hh(code: String) = db.collection("households").document(code)
    private fun lists(code: String) = hh(code).collection("lists")
    private fun items(code: String) = hh(code).collection("items")
    private fun history(code: String) = hh(code).collection("history")

    suspend fun ensureSignedIn(): String =
        auth.currentUser?.uid ?: auth.signInAnonymously().await().user!!.uid

    // ---------- huishouden ----------

    suspend fun createHousehold(): String {
        val uid = ensureSignedIn()
        repeat(5) {
            val code = Codes.generate()
            try {
                hh(code).set(mapOf("members" to listOf(uid), "createdAt" to System.currentTimeMillis())).await()
                addList(code, "Supermarkt", 0)
                return code
            } catch (e: FirebaseFirestoreException) {
                // bestaat al (zeer onwaarschijnlijk) -> nieuwe code proberen
                if (e.code != FirebaseFirestoreException.Code.PERMISSION_DENIED) throw e
            }
        }
        error("Kon geen code maken")
    }

    suspend fun joinHousehold(code: String): JoinResult {
        val uid = ensureSignedIn()
        return try {
            hh(code).update("members", FieldValue.arrayUnion(uid)).await()
            JoinResult.OK
        } catch (e: FirebaseFirestoreException) {
            if (e.code == FirebaseFirestoreException.Code.UNAVAILABLE) JoinResult.OFFLINE else JoinResult.NOT_FOUND
        }
    }

    suspend fun leave(code: String) {
        val uid = auth.currentUser?.uid ?: return
        hh(code).update("members", FieldValue.arrayRemove(uid)).await()
    }

    // ---------- live data ----------

    private fun <T> live(query: com.google.firebase.firestore.Query, map: (DocumentSnapshot) -> T): Flow<List<T>> =
        callbackFlow {
            val reg = query.addSnapshotListener { snap, err ->
                if (err != null) {
                    close(err); return@addSnapshotListener
                }
                if (snap != null) trySend(snap.documents.map(map))
            }
            awaitClose { reg.remove() }
        }

    fun listsFlow(code: String): Flow<List<ShopList>> = live(lists(code)) { d ->
        ShopList(d.id, d.getString("name") ?: "Lijst", d.getLong("order") ?: 0)
    }

    fun itemsFlow(code: String): Flow<List<Item>> = live(items(code)) { d ->
        Item(
            id = d.id,
            listId = d.getString("listId") ?: "",
            name = d.getString("name") ?: "",
            category = (d.getLong("category") ?: Categories.OVERIG.toLong()).toInt(),
            qty = d.getLong("qty") ?: 1,
            unit = d.getString("unit") ?: "st",
            checked = d.getBoolean("checked") ?: false,
            checkedAt = d.getLong("checkedAt") ?: 0,
            createdAt = d.getLong("createdAt") ?: 0,
        )
    }

    fun historyFlow(code: String): Flow<List<HistoryEntry>> = live(history(code)) { d ->
        HistoryEntry(
            key = d.id,
            name = d.getString("name") ?: "",
            category = (d.getLong("category") ?: Categories.OVERIG.toLong()).toInt(),
            unit = d.getString("unit") ?: "st",
            qty = d.getLong("qty") ?: 1,
            count = d.getLong("count") ?: 1,
        )
    }

    // ---------- lijsten ----------
    // Schrijfacties wachten niet op de server: Firestore toont ze direct en
    // synchroniseert zodra er verbinding is (handig in de winkel).

    fun addList(code: String, name: String, order: Long): String {
        val ref = lists(code).document()
        ref.set(mapOf("name" to name, "order" to order, "createdAt" to System.currentTimeMillis()))
        return ref.id
    }

    fun renameList(code: String, id: String, name: String) {
        lists(code).document(id).update("name", name)
    }

    fun deleteList(code: String, id: String, itemIds: List<String>) {
        val refs = itemIds.map { items(code).document(it) } + lists(code).document(id)
        refs.chunked(450).forEach { chunk ->
            val batch = db.batch()
            chunk.forEach { batch.delete(it) }
            batch.commit()
        }
    }

    // ---------- producten ----------

    private fun itemMap(i: Item): Map<String, Any> = mapOf(
        "listId" to i.listId, "name" to i.name, "category" to i.category, "qty" to i.qty,
        "unit" to i.unit, "checked" to i.checked, "checkedAt" to i.checkedAt, "createdAt" to i.createdAt,
    )

    fun addItem(code: String, listId: String, name: String, category: Int, qty: Long, unit: String) {
        val ref = items(code).document()
        restoreItem(code, Item(ref.id, listId, name, category, qty, unit, false, 0, System.currentTimeMillis()))
    }

    fun restoreItem(code: String, item: Item) {
        items(code).document(item.id).set(itemMap(item))
    }

    fun updateItem(code: String, id: String, fields: Map<String, Any>) {
        items(code).document(id).update(fields)
    }

    fun deleteItems(code: String, ids: List<String>) {
        ids.chunked(450).forEach { chunk ->
            val batch = db.batch()
            chunk.forEach { batch.delete(items(code).document(it)) }
            batch.commit()
        }
    }

    fun restoreItems(code: String, list: List<Item>) {
        list.chunked(450).forEach { chunk ->
            val batch = db.batch()
            chunk.forEach { batch.set(items(code).document(it.id), itemMap(it)) }
            batch.commit()
        }
    }

    // ---------- geschiedenis (leert jullie favorieten en categorieën) ----------

    fun recordHistory(code: String, name: String, category: Int, unit: String, qty: Long, increment: Boolean) {
        val key = "k_" + Suggest.norm(name).replace('/', ' ').take(120)
        val data = mutableMapOf<String, Any>(
            "name" to name, "category" to category, "unit" to unit, "qty" to qty,
            "lastUsed" to System.currentTimeMillis(),
        )
        if (increment) data["count"] = FieldValue.increment(1)
        history(code).document(key).set(data, SetOptions.merge())
    }
}
