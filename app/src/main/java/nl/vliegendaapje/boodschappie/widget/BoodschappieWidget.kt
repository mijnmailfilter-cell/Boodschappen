package nl.vliegendaapje.boodschappie.widget

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.glance.ColorFilter
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.appwidget.GlanceAppWidgetManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import nl.vliegendaapje.boodschappie.R
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextDecoration
import androidx.glance.text.TextStyle
import com.google.firebase.auth.FirebaseAuth
import nl.vliegendaapje.boodschappie.MainActivity
import nl.vliegendaapje.boodschappie.QuickAddActivity
import nl.vliegendaapje.boodschappie.data.ALL
import nl.vliegendaapje.boodschappie.data.Categories
import nl.vliegendaapje.boodschappie.data.Item
import nl.vliegendaapje.boodschappie.data.Qty
import nl.vliegendaapje.boodschappie.data.Repo
import nl.vliegendaapje.boodschappie.data.ShopList

private val KEY_LIST = stringPreferencesKey("widget_list")
private val KEY_REV = longPreferencesKey("widget_rev")
private val P_ITEM = ActionParameters.Key<String>("item")
private val P_CHECKED = ActionParameters.Key<Boolean>("checked")
private val P_LIST = ActionParameters.Key<String>("list")

// kleuren (dag / nacht)
private val cBg = ColorProvider(day = Color(0xFFF4F9F4), night = Color(0xFF151B17))
private val cCard = ColorProvider(day = Color(0xFFFFFFFF), night = Color(0xFF222A25))
private val cPrimary = ColorProvider(day = Color(0xFF2E7D5B), night = Color(0xFF8DD9B1))
private val cText = ColorProvider(day = Color(0xFF171D19), night = Color(0xFFDFE4DF))
private val cDim = ColorProvider(day = Color(0xFF6B776F), night = Color(0xFF9AA59D))
private val cChip = ColorProvider(day = Color(0xFFB7EFD2), night = Color(0xFF0F5134))
private val cWhite = ColorProvider(day = Color.White, night = Color.White)

private val initialColors = listOf(
    Color(0xFF2E7D5B), Color(0xFFD9772B), Color(0xFF5B6CC9), Color(0xFFB8467A), Color(0xFF2F8FA3),
)

fun initialColor(name: String): Color = initialColors[(name.lowercase().hashCode() and 0x7fffffff) % initialColors.size]

data class WidgetSnapshot(val linked: Boolean, val lists: List<ShopList>, val items: List<Item>)

object WidgetData {
    suspend fun load(context: Context): WidgetSnapshot {
        val code = context.getSharedPreferences("boodschappie", Context.MODE_PRIVATE).getString("household", null)
        if (code == null || FirebaseAuth.getInstance().currentUser == null) return WidgetSnapshot(false, emptyList(), emptyList())
        val repo = Repo()
        return WidgetSnapshot(true, repo.fetchLists(code).sortedBy { it.order }, repo.fetchItems(code))
    }
}

class BoodschappieWidget : GlanceAppWidget() {
    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val first = WidgetData.load(context)
        provideContent {
            // bij elke "rev"-ophoging opnieuw ophalen, zodat de widget echt ververst
            val rev = currentState<Preferences>()[KEY_REV] ?: 0L
            val snap by produceState(first, rev) { if (rev != 0L) value = WidgetData.load(context) }
            WidgetContent(context, snap)
        }
    }
}

/** Ververs alle Boodschappie-widgets (haalt de lijst opnieuw op). */
object WidgetRefresher {
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    suspend fun refreshAll(context: Context) {
        val manager = GlanceAppWidgetManager(context)
        val widget = BoodschappieWidget()
        runCatching {
            manager.getGlanceIds(BoodschappieWidget::class.java).forEach { id ->
                updateAppWidgetState(context, id) { it[KEY_REV] = System.currentTimeMillis() }
                widget.update(context, id)
            }
        }
    }

    /** Ook als het scherm/venster al dicht is (los van de levensduur van een Activity). */
    fun refreshSoon(context: Context, delayMs: Long = 600) {
        val app = context.applicationContext
        appScope.launch {
            delay(delayMs)
            refreshAll(app)
        }
    }
}

class BoodschappieWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = BoodschappieWidget()
}

private fun quickIntent(context: Context, voice: Boolean, listId: String): Intent =
    Intent(context, QuickAddActivity::class.java).apply {
        data = Uri.parse("boodschappie://quick/${if (voice) "voice" else "text"}/$listId")
        putExtra(QuickAddActivity.EXTRA_VOICE, voice)
        putExtra(QuickAddActivity.EXTRA_LIST, listId)
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
    }

@Composable
private fun WidgetContent(context: Context, snap: WidgetSnapshot) {
    val state = currentState<Preferences>()
    val listId = state[KEY_LIST]?.takeIf { id -> id == ALL || snap.lists.any { it.id == id } } ?: ALL
    val order = listOf(ALL) + snap.lists.map { it.id }
    val next = order[(order.indexOf(listId) + 1) % order.size]
    val title = if (listId == ALL) "Alles" else snap.lists.firstOrNull { it.id == listId }?.name ?: "Alles"
    val visible = if (listId == ALL) snap.items else snap.items.filter { it.listId == listId }
    val groups = visible.filter { !it.checked }
        .groupBy { it.category }
        .toList()
        .sortedBy { Categories.order(it.first) }
        .map { (c, l) -> c to l.sortedBy { it.name.lowercase() } }
    val checked = visible.filter { it.checked }.sortedByDescending { it.checkedAt }
    val showListTag = listId == ALL && snap.lists.size > 1
    val names = snap.lists.associate { it.id to it.name }

    Column(GlanceModifier.fillMaxSize().background(cBg).cornerRadius(24.dp).padding(10.dp)) {
        // kop
        Row(GlanceModifier.fillMaxWidth().padding(bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "🛒",
                style = TextStyle(fontSize = 18.sp),
                modifier = GlanceModifier.padding(end = 6.dp).clickable(actionStartActivity<MainActivity>()),
            )
            Text(
                if (snap.lists.isEmpty()) title else "$title ▾",
                style = TextStyle(color = cPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold),
                maxLines = 1,
                modifier = GlanceModifier.defaultWeight()
                    .clickable(actionRunCallback<CycleListAction>(actionParametersOf(P_LIST to next))),
            )
            if (snap.linked) {
                RoundButton("🎤", actionStartActivity(quickIntent(context, true, listId)))
                Spacer(GlanceModifier.width(6.dp))
                RoundButton("＋", actionStartActivity(quickIntent(context, false, listId)))
            }
            Spacer(GlanceModifier.width(4.dp))
            Box(
                GlanceModifier.size(30.dp).cornerRadius(15.dp).clickable(actionStartActivity<MainActivity>()),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    provider = ImageProvider(R.drawable.ic_open_app),
                    contentDescription = "App openen",
                    colorFilter = ColorFilter.tint(cDim),
                    modifier = GlanceModifier.size(18.dp),
                )
            }
        }

        when {
            !snap.linked -> Message("Open Boodschappie om te koppelen", actionStartActivity<MainActivity>())
            visible.isEmpty() -> Message("Niks op de lijst 🎉\nTik op ＋ of 🎤 om iets toe te voegen.", null)
            else -> LazyColumn(GlanceModifier.fillMaxWidth().defaultWeight()) {
                groups.forEach { (cat, list) ->
                    item(itemId = -1000L - cat) {
                        val c = Categories.get(cat)
                        Text(
                            "${c.emoji}  ${c.name}",
                            style = TextStyle(color = cDim, fontSize = 12.sp, fontWeight = FontWeight.Medium),
                            modifier = GlanceModifier.padding(start = 4.dp, top = 6.dp, bottom = 3.dp),
                        )
                    }
                    items(list, itemId = { it.id.hashCode().toLong() }) { item ->
                        WidgetRow(item, if (showListTag) names[item.listId] else null)
                    }
                }
                if (checked.isNotEmpty()) {
                    item(itemId = -1L) {
                        Row(
                            GlanceModifier.fillMaxWidth().padding(start = 4.dp, top = 8.dp, bottom = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                "✓  In het mandje (${checked.size})",
                                style = TextStyle(color = cDim, fontSize = 12.sp, fontWeight = FontWeight.Medium),
                                modifier = GlanceModifier.defaultWeight(),
                            )
                            Box(
                                GlanceModifier.size(32.dp).cornerRadius(16.dp)
                                    .clickable(actionRunCallback<ClearCheckedAction>(actionParametersOf(P_LIST to listId))),
                                contentAlignment = Alignment.Center,
                            ) {
                                Image(
                                    provider = ImageProvider(R.drawable.ic_trash),
                                    contentDescription = "Afgevinkte wissen",
                                    colorFilter = ColorFilter.tint(cDim),
                                    modifier = GlanceModifier.size(18.dp),
                                )
                            }
                        }
                    }
                    items(checked, itemId = { it.id.hashCode().toLong() }) { item ->
                        WidgetRow(item, null)
                    }
                }
            }
        }
    }
}

@Composable
private fun RoundButton(label: String, action: androidx.glance.action.Action) {
    Box(
        GlanceModifier.size(38.dp).background(cChip).cornerRadius(19.dp).clickable(action),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = TextStyle(color = cText, fontSize = 17.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center))
    }
}

@Composable
private fun Message(text: String, action: androidx.glance.action.Action?) {
    val base = GlanceModifier.fillMaxSize().padding(12.dp)
    Box(if (action != null) base.clickable(action) else base, contentAlignment = Alignment.Center) {
        Text(text, style = TextStyle(color = cDim, fontSize = 14.sp, textAlign = TextAlign.Center))
    }
}

@Composable
private fun WidgetRow(item: Item, listName: String?) {
    val toggle = actionRunCallback<ToggleItemAction>(
        actionParametersOf(P_ITEM to item.id, P_CHECKED to !item.checked),
    )
    Box(GlanceModifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Row(
            GlanceModifier.fillMaxWidth().background(cCard).cornerRadius(12.dp)
                .padding(horizontal = 10.dp, vertical = 8.dp).clickable(toggle),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                if (item.checked) "✓" else "○",
                style = TextStyle(color = if (item.checked) cPrimary else cDim, fontSize = 16.sp, fontWeight = FontWeight.Bold),
            )
            Spacer(GlanceModifier.width(8.dp))
            Column(GlanceModifier.defaultWeight()) {
                Text(
                    item.name,
                    maxLines = 1,
                    style = TextStyle(
                        color = if (item.checked) cDim else cText,
                        fontSize = 14.sp,
                        fontWeight = if (item.checked) FontWeight.Normal else FontWeight.Medium,
                        textDecoration = if (item.checked) TextDecoration.LineThrough else TextDecoration.None,
                    ),
                )
                if (listName != null) {
                    Text(listName, maxLines = 1, style = TextStyle(color = cPrimary, fontSize = 10.sp))
                }
            }
            if (item.initial.isNotEmpty()) {
                val col = initialColor(item.byName)
                Box(
                    GlanceModifier.size(20.dp).background(ColorProvider(day = col, night = col)).cornerRadius(10.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(item.initial, style = TextStyle(color = cWhite, fontSize = 10.sp, fontWeight = FontWeight.Bold))
                }
                Spacer(GlanceModifier.width(6.dp))
            }
            Text(
                Qty.label(item.qty, item.unit),
                style = TextStyle(color = cDim, fontSize = 13.sp),
            )
        }
    }
}

class ToggleItemAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val id = parameters[P_ITEM] ?: return
        val checked = parameters[P_CHECKED] ?: return
        val code = context.getSharedPreferences("boodschappie", Context.MODE_PRIVATE).getString("household", null) ?: return
        Repo().updateItemAwait(code, id, mapOf("checked" to checked, "checkedAt" to System.currentTimeMillis()))
        WidgetRefresher.refreshAll(context)
    }
}

class ClearCheckedAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val listId = parameters[P_LIST] ?: ALL
        val code = context.getSharedPreferences("boodschappie", Context.MODE_PRIVATE).getString("household", null) ?: return
        val repo = Repo()
        val ids = repo.fetchItems(code)
            .filter { it.checked && (listId == ALL || it.listId == listId) }
            .map { it.id }
        if (ids.isNotEmpty()) repo.deleteItemsAwait(code, ids)
        WidgetRefresher.refreshAll(context)
    }
}

class CycleListAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val next = parameters[P_LIST] ?: ALL
        updateAppWidgetState(context, glanceId) {
            it[KEY_LIST] = next
            it[KEY_REV] = System.currentTimeMillis()
        }
        BoodschappieWidget().update(context, glanceId)
    }
}
