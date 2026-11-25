package nl.vliegendaapje.boodschappie.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import nl.vliegendaapje.boodschappie.MainViewModel
import nl.vliegendaapje.boodschappie.data.ALL
import nl.vliegendaapje.boodschappie.data.Categories
import nl.vliegendaapje.boodschappie.data.Item
import nl.vliegendaapje.boodschappie.data.ShopList

private sealed interface ListDialog {
    data object New : ListDialog
    data class Rename(val list: ShopList) : ListDialog
    data class Delete(val list: ShopList) : ListDialog
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(vm: MainViewModel, onOpenSettings: () -> Unit) {
    val lists by vm.lists.collectAsStateWithLifecycle()
    val items by vm.items.collectAsStateWithLifecycle()
    val history by vm.history.collectAsStateWithLifecycle()
    val selectedRaw by vm.selected.collectAsStateWithLifecycle()
    val addTarget by vm.addTarget.collectAsStateWithLifecycle()
    val accessError by vm.accessError.collectAsStateWithLifecycle()

    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var editing by remember { mutableStateOf<Item?>(null) }
    var dialog by remember { mutableStateOf<ListDialog?>(null) }
    var menuOpen by remember { mutableStateOf(false) }

    val sortedLists = remember(lists) { lists.sortedBy { it.order } }
    val selected = if (selectedRaw == ALL || sortedLists.any { it.id == selectedRaw }) selectedRaw else ALL
    val selectedList = sortedLists.firstOrNull { it.id == selected }
    val listNames = remember(sortedLists) { sortedLists.associate { it.id to it.name } }
    val target = if (selected != ALL) selected
    else addTarget?.takeIf { t -> sortedLists.any { it.id == t } } ?: sortedLists.firstOrNull()?.id

    val visible = if (selected == ALL) items else items.filter { it.listId == selected }
    val groups: List<Pair<Int, List<Item>>> = remember(visible) {
        visible.filter { !it.checked }
            .groupBy { it.category }
            .toList()
            .sortedBy { Categories.order(it.first) }
            .map { (cat, list) -> cat to list.sortedBy { it.name.lowercase() } }
    }
    val checked = remember(visible) { visible.filter { it.checked }.sortedByDescending { it.checkedAt } }
    val showListTag = selected == ALL && sortedLists.size > 1

    fun undoable(text: String, undo: () -> Unit) {
        scope.launch {
            snackbar.currentSnackbarData?.dismiss()
            val r = snackbar.showSnackbar(message = text, actionLabel = "Ongedaan maken", duration = SnackbarDuration.Short)
            if (r == SnackbarResult.ActionPerformed) undo()
        }
    }

    fun deleteWithUndo(item: Item) {
        vm.delete(item)
        undoable("${item.name} verwijderd") { vm.restore(item) }
    }

    fun clearChecked() {
        val gone = checked
        if (gone.isEmpty()) return
        vm.clearChecked(gone)
        undoable("${gone.size} afgevinkt${if (gone.size == 1) " product" else "e producten"} gewist") { vm.restoreAll(gone) }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text("Boodschappie", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                actions = {
                    Box {
                        IconButton(onClick = { menuOpen = true }) { Icon(Icons.Filled.MoreVert, "Menu") }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            DropdownMenuItem(text = { Text("Nieuwe lijst") }, onClick = { menuOpen = false; dialog = ListDialog.New })
                            if (selectedList != null) {
                                DropdownMenuItem(text = { Text("Lijst hernoemen") }, onClick = { menuOpen = false; dialog = ListDialog.Rename(selectedList) })
                                DropdownMenuItem(text = { Text("Lijst verwijderen") }, onClick = { menuOpen = false; dialog = ListDialog.Delete(selectedList) })
                            }
                            DropdownMenuItem(
                                text = { Text("Afgevinkte wissen") },
                                enabled = checked.isNotEmpty(),
                                onClick = { menuOpen = false; clearChecked() },
                            )
                        }
                    }
                    IconButton(onClick = onOpenSettings) { Icon(Icons.Filled.Settings, "Instellingen") }
                },
            )
        },
        bottomBar = {
            AddBar(
                history = history,
                lists = sortedLists,
                selected = selected,
                target = target,
                onAddText = vm::addFromText,
                onAddSuggestion = vm::addSuggestion,
                onTarget = vm::setTarget,
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            // lijst-tabjes
            LazyRow(
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item(key = ALL) {
                    val n = items.count { !it.checked }
                    FilterChip(
                        selected = selected == ALL,
                        onClick = { vm.select(ALL) },
                        label = { Text(if (n > 0) "Alles · $n" else "Alles") },
                    )
                }
                items(sortedLists, key = { it.id }) { l ->
                    val n = items.count { it.listId == l.id && !it.checked }
                    FilterChip(
                        selected = selected == l.id,
                        onClick = { vm.select(l.id) },
                        label = { Text(if (n > 0) "${l.name} · $n" else l.name) },
                    )
                }
                item(key = "__new__") {
                    AssistChip(
                        onClick = { dialog = ListDialog.New },
                        label = { Text("Lijst") },
                        leadingIcon = { Icon(Icons.Filled.Add, null, Modifier.size(18.dp)) },
                    )
                }
            }

            if (accessError) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.padding(12.dp).fillMaxWidth(),
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Geen toegang meer tot deze lijst.", fontWeight = FontWeight.SemiBold)
                        Text("Koppel opnieuw met de code.", style = MaterialTheme.typography.bodyMedium)
                        TextButton(onClick = { vm.forgetHousehold() }) { Text("Opnieuw koppelen") }
                    }
                }
            }

            if (visible.isEmpty()) {
                EmptyState(Modifier.weight(1f))
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 0.dp, bottom = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    groups.forEach { (cat, group) ->
                        item(key = "cat_$cat") {
                            CategoryHeader(cat, group.size, Modifier.animateItem())
                        }
                        itemsIndexed(group, key = { _, it -> it.id }) { i, it ->
                            SwipeItem(
                                item = it,
                                listName = if (showListTag) listNames[it.listId] else null,
                                shape = groupShape(i, group.size),
                                onToggle = { vm.toggle(it) },
                                onStep = { up -> vm.step(it, up) },
                                onEdit = { editing = it },
                                onDelete = { deleteWithUndo(it) },
                                modifier = Modifier.animateItem(),
                            )
                        }
                    }
                    if (checked.isNotEmpty()) {
                        item(key = "checked_header") {
                            Row(
                                Modifier.animateItem().fillMaxWidth().padding(start = 6.dp, top = 22.dp, bottom = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text("✅", fontSize = 16.sp)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "In het mandje (${checked.size})",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.weight(1f),
                                )
                                TextButton(onClick = { clearChecked() }) { Text("Wissen") }
                            }
                        }
                        itemsIndexed(checked, key = { _, it -> it.id }) { i, it ->
                            SwipeItem(
                                item = it,
                                listName = if (showListTag) listNames[it.listId] else null,
                                shape = groupShape(i, checked.size),
                                onToggle = { vm.toggle(it) },
                                onStep = { },
                                onEdit = { editing = it },
                                onDelete = { deleteWithUndo(it) },
                                modifier = Modifier.animateItem(),
                            )
                        }
                    }
                }
            }
        }
    }

    editing?.let { item ->
        EditSheet(
            item = item,
            lists = sortedLists,
            onDismiss = { editing = null },
            onSave = { name, qty, unit, cat, listId ->
                vm.save(item, name, qty, unit, cat, listId)
                editing = null
            },
            onDelete = {
                editing = null
                deleteWithUndo(item)
            },
        )
    }

    when (val d = dialog) {
        ListDialog.New -> NameDialog(
            title = "Nieuwe lijst",
            initial = "",
            confirm = "Maken",
            onConfirm = { vm.addList(it); dialog = null },
            onDismiss = { dialog = null },
        )
        is ListDialog.Rename -> NameDialog(
            title = "Lijst hernoemen",
            initial = d.list.name,
            confirm = "Opslaan",
            onConfirm = { vm.renameList(d.list.id, it); dialog = null },
            onDismiss = { dialog = null },
        )
        is ListDialog.Delete -> ConfirmDialog(
            title = "\"${d.list.name}\" verwijderen?",
            text = "De lijst en alle producten erop worden voor jullie allebei verwijderd.",
            confirm = "Verwijderen",
            onConfirm = { vm.deleteList(d.list.id); dialog = null },
            onDismiss = { dialog = null },
        )
        null -> Unit
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("🧺", fontSize = 64.sp)
        Spacer(Modifier.height(12.dp))
        Text("Nog niks op de lijst", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))
        Text(
            "Typ hieronder wat je nodig hebt. Tik op een product om het af te vinken, veeg naar links om het te verwijderen, en houd vast om het aan te passen.",
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
