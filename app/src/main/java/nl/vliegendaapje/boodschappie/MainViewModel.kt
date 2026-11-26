package nl.vliegendaapje.boodschappie

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import nl.vliegendaapje.boodschappie.data.VoiceParser
import nl.vliegendaapje.boodschappie.widget.BoodschappieWidget
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import nl.vliegendaapje.boodschappie.data.ALL
import nl.vliegendaapje.boodschappie.data.Codes
import nl.vliegendaapje.boodschappie.data.HistoryEntry
import nl.vliegendaapje.boodschappie.data.Item
import nl.vliegendaapje.boodschappie.data.JoinResult
import nl.vliegendaapje.boodschappie.data.Qty
import nl.vliegendaapje.boodschappie.data.QtyParser
import nl.vliegendaapje.boodschappie.data.Repo
import nl.vliegendaapje.boodschappie.data.ShopList
import nl.vliegendaapje.boodschappie.data.Suggest
import nl.vliegendaapje.boodschappie.data.Suggestion
import nl.vliegendaapje.boodschappie.data.Catalog

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val prefs = app.getSharedPreferences("boodschappie", Context.MODE_PRIVATE)
    private val repo = Repo()

    val household = MutableStateFlow(prefs.getString(KEY_HH, null))
    val busy = MutableStateFlow(false)
    val message = MutableStateFlow<String?>(null)
    val accessError = MutableStateFlow(false)
    val selected = MutableStateFlow(prefs.getString(KEY_SEL, ALL) ?: ALL)
    val addTarget = MutableStateFlow(prefs.getString(KEY_TARGET, null))
    val myName = MutableStateFlow(prefs.getString(KEY_NAME, "") ?: "")
    val notify = MutableStateFlow(prefs.getBoolean(KEY_NOTIFY, true))

    /** Doellijst voor het snelle invoervenster vanaf de widget (null = standaard). */
    var quickTarget: String? = null

    private fun <T> live(source: (String) -> Flow<List<T>>): StateFlow<List<T>> =
        household.flatMapLatest { code ->
            if (code == null) flowOf(emptyList<T>())
            else flow<List<T>> {
                repo.ensureSignedIn()
                emitAll(source(code))
            }.catch { e ->
                if (e is FirebaseFirestoreException && e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                    accessError.value = true
                }
                emit(emptyList<T>())
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val lists: StateFlow<List<ShopList>> = live { repo.listsFlow(it) }
    val items: StateFlow<List<Item>> = live { repo.itemsFlow(it) }
    val history: StateFlow<List<HistoryEntry>> = live { repo.historyFlow(it) }

    init {
        // widget bijwerken zodra de lijst verandert
        viewModelScope.launch {
            items.drop(1).debounce(1200).collect {
                runCatching { BoodschappieWidget().updateAll(getApplication()) }
            }
        }
    }

    fun setName(name: String) {
        val n = name.trim()
        myName.value = n
        prefs.edit().putString(KEY_NAME, n).apply()
    }

    fun setNotify(on: Boolean) {
        notify.value = on
        prefs.edit().putBoolean(KEY_NOTIFY, on).apply()
    }

    // ---------- koppelen ----------

    private fun setHousehold(code: String?) {
        prefs.edit().putString(KEY_HH, code).putString(KEY_SEL, ALL).apply()
        accessError.value = false
        selected.value = ALL
        household.value = code
    }

    fun create() = viewModelScope.launch {
        busy.value = true; message.value = null
        try {
            setHousehold(repo.createHousehold())
        } catch (e: Exception) {
            message.value = "Dat lukte niet. Heb je internet? (${e.message})"
        } finally {
            busy.value = false
        }
    }

    fun join(input: String) = viewModelScope.launch {
        val code = Codes.clean(input)
        if (code.length != 8) {
            message.value = "Een code bestaat uit 8 tekens."; return@launch
        }
        busy.value = true; message.value = null
        try {
            when (repo.joinHousehold(code)) {
                JoinResult.OK -> setHousehold(code)
                JoinResult.NOT_FOUND -> message.value = "Deze code bestaat niet. Controleer hem even."
                JoinResult.OFFLINE -> message.value = "Geen verbinding. Probeer het zo nog eens."
            }
        } catch (e: Exception) {
            message.value = "Dat lukte niet: ${e.message}"
        } finally {
            busy.value = false
        }
    }

    fun leave() = viewModelScope.launch {
        val code = household.value ?: return@launch
        runCatching { repo.leave(code) }
        setHousehold(null)
    }

    fun forgetHousehold() = setHousehold(null)

    // ---------- selectie ----------

    fun select(id: String) {
        selected.value = id
        prefs.edit().putString(KEY_SEL, id).apply()
    }

    fun setTarget(id: String) {
        addTarget.value = id
        prefs.edit().putString(KEY_TARGET, id).apply()
    }

    fun effectiveTarget(): String? {
        val all = lists.value
        quickTarget?.let { q -> if (all.any { it.id == q }) return q }
        val sel = selected.value
        if (sel != ALL && all.any { it.id == sel }) return sel
        return addTarget.value?.takeIf { t -> all.any { it.id == t } } ?: all.minByOrNull { it.order }?.id
    }

    // ---------- toevoegen ----------

    fun addFromText(text: String) {
        val p = QtyParser.parse(text)
        if (p.name.isBlank()) return
        val name = p.name.replaceFirstChar { it.uppercase() }
        val hist = history.value.firstOrNull { Suggest.norm(it.name) == Suggest.norm(name) }
        val product = Catalog.find(name)
        val category = Suggest.guessCategory(name, history.value)
        val unit = p.unit ?: hist?.unit ?: product?.unit ?: "st"
        val qty = p.qty
            ?: hist?.takeIf { it.unit == unit }?.qty
            ?: product?.takeIf { it.unit == unit }?.qty
            ?: Qty.default(unit)
        addItem(hist?.name ?: product?.name ?: name, category, qty, unit)
    }

    fun addSuggestion(s: Suggestion) = addItem(s.name, s.category, s.qty, s.unit)

    /** Ingesproken tekst -> losse producten. Geeft de toegevoegde regels terug. */
    fun addSpoken(spoken: String): List<String> {
        val known = history.value.map { Suggest.norm(it.name) }.toHashSet()
        val parts = VoiceParser.split(spoken) { name -> Catalog.find(name) != null || Suggest.norm(name) in known }
        parts.forEach { addFromText(it) }
        return parts
    }

    /** Wacht (max. 6 s) tot de lijsten geladen zijn, voor het snelle venster. */
    suspend fun awaitReady() {
        withTimeoutOrNull(6000) { lists.first { it.isNotEmpty() } }
    }

    private fun addItem(name: String, category: Int, qty: Long, unit: String) {
        val code = household.value ?: return
        val listId = effectiveTarget() ?: repo.addList(code, "Boodschappen", 0)
        val existing = items.value.firstOrNull { it.listId == listId && Suggest.norm(it.name) == Suggest.norm(name) }
        val now = System.currentTimeMillis()
        val author = mapOf("byUid" to (repo.uid() ?: ""), "byName" to myName.value, "addedAt" to now)
        when {
            existing == null -> repo.addItem(code, listId, name, category, qty, unit, myName.value)
            existing.checked -> repo.updateItem(code, existing.id, mapOf("checked" to false, "qty" to qty, "unit" to unit) + author)
            existing.unit == unit -> repo.updateItem(code, existing.id, mapOf("qty" to existing.qty + qty))
            else -> repo.updateItem(code, existing.id, mapOf("qty" to qty, "unit" to unit))
        }
        repo.recordHistory(code, name, category, unit, qty, increment = true)
    }

    // ---------- bewerken ----------

    fun toggle(item: Item) {
        val code = household.value ?: return
        repo.updateItem(code, item.id, mapOf("checked" to !item.checked, "checkedAt" to System.currentTimeMillis()))
    }

    fun step(item: Item, up: Boolean) {
        val code = household.value ?: return
        val q = Qty.step(item.qty, item.unit, up)
        if (q != item.qty) repo.updateItem(code, item.id, mapOf("qty" to q))
    }

    fun save(item: Item, name: String, qty: Long, unit: String, category: Int, listId: String) {
        val code = household.value ?: return
        val clean = name.trim().ifEmpty { item.name }
        repo.updateItem(
            code, item.id,
            mapOf("name" to clean, "qty" to qty, "unit" to unit, "category" to category, "listId" to listId),
        )
        // onthoud de gekozen categorie/eenheid voor de volgende keer
        repo.recordHistory(code, clean, category, unit, qty, increment = false)
    }

    fun delete(item: Item) {
        val code = household.value ?: return
        repo.deleteItems(code, listOf(item.id))
    }

    fun restore(item: Item) {
        val code = household.value ?: return
        repo.restoreItem(code, item)
    }

    fun clearChecked(list: List<Item>) {
        val code = household.value ?: return
        repo.deleteItems(code, list.map { it.id })
    }

    fun restoreAll(list: List<Item>) {
        val code = household.value ?: return
        repo.restoreItems(code, list)
    }

    // ---------- lijsten beheren ----------

    fun addList(name: String) {
        val code = household.value ?: return
        val order = (lists.value.maxOfOrNull { it.order } ?: -1L) + 1
        val id = repo.addList(code, name.trim().ifEmpty { "Nieuwe lijst" }, order)
        select(id)
    }

    fun renameList(id: String, name: String) {
        val code = household.value ?: return
        if (name.isNotBlank()) repo.renameList(code, id, name.trim())
    }

    fun deleteList(id: String) {
        val code = household.value ?: return
        repo.deleteList(code, id, items.value.filter { it.listId == id }.map { it.id })
        if (selected.value == id) select(ALL)
    }

    companion object {
        private const val KEY_HH = "household"
        private const val KEY_SEL = "selected"
        private const val KEY_TARGET = "addTarget"
        const val KEY_NAME = "name"
        const val KEY_NOTIFY = "notify"
    }
}
