package nl.vliegendaapje.boodschappie.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import nl.vliegendaapje.boodschappie.data.ALL
import nl.vliegendaapje.boodschappie.data.Categories
import nl.vliegendaapje.boodschappie.data.HistoryEntry
import nl.vliegendaapje.boodschappie.data.Item
import nl.vliegendaapje.boodschappie.data.Qty
import nl.vliegendaapje.boodschappie.data.QtyParser
import nl.vliegendaapje.boodschappie.data.ShopList
import nl.vliegendaapje.boodschappie.data.Suggest
import nl.vliegendaapje.boodschappie.data.Suggestion

// ---------------------------------------------------------------- product-rij

fun groupShape(index: Int, size: Int): Shape {
    val big = 18.dp
    val small = 4.dp
    val top = if (index == 0) big else small
    val bottom = if (index == size - 1) big else small
    return RoundedCornerShape(topStart = top, topEnd = top, bottomStart = bottom, bottomEnd = bottom)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeItem(
    item: Item,
    listName: String?,
    shape: Shape,
    onToggle: () -> Unit,
    onStep: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentDelete by rememberUpdatedState(onDelete)
    val state = rememberSwipeToDismissBoxState(
        confirmValueChange = { v ->
            if (v == SwipeToDismissBoxValue.EndToStart) {
                currentDelete(); true
            } else false
        },
    )
    SwipeToDismissBox(
        state = state,
        modifier = modifier,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            Box(
                Modifier
                    .fillMaxSize()
                    .clip(shape)
                    .background(MaterialTheme.colorScheme.errorContainer)
                    .padding(horizontal = 22.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Icon(Icons.Filled.Delete, "Verwijderen", tint = MaterialTheme.colorScheme.onErrorContainer)
            }
        },
    ) {
        ItemRow(item, listName, shape, onToggle, onStep, onEdit)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ItemRow(
    item: Item,
    listName: String?,
    shape: Shape,
    onToggle: () -> Unit,
    onStep: (Boolean) -> Unit,
    onEdit: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    Surface(shape = shape, color = cs.surfaceContainerLowest, modifier = Modifier.fillMaxWidth()) {
        Row(
            Modifier
                .combinedClickable(onClick = onToggle, onLongClick = onEdit)
                .heightIn(min = 58.dp)
                .padding(start = 14.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CheckDot(item.checked)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    item.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (item.checked) FontWeight.Normal else FontWeight.Medium,
                    textDecoration = if (item.checked) TextDecoration.LineThrough else null,
                    color = if (item.checked) cs.onSurface.copy(alpha = 0.45f) else cs.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (listName != null) {
                    Text(
                        listName,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (item.checked) cs.primary.copy(alpha = 0.45f) else cs.primary,
                    )
                }
            }
            if (item.checked) {
                Text(
                    Qty.label(item.qty, item.unit),
                    style = MaterialTheme.typography.bodyMedium,
                    color = cs.onSurface.copy(alpha = 0.45f),
                    modifier = Modifier.padding(end = 12.dp),
                )
            } else {
                Stepper(item.qty, item.unit, onStep, onEdit)
            }
        }
    }
}

@Composable
fun CheckDot(checked: Boolean) {
    val cs = MaterialTheme.colorScheme
    Box(
        Modifier
            .size(26.dp)
            .clip(CircleShape)
            .then(
                if (checked) Modifier.background(cs.primary)
                else Modifier.border(2.dp, cs.outline, CircleShape)
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (checked) Icon(Icons.Filled.Check, null, tint = cs.onPrimary, modifier = Modifier.size(18.dp))
    }
}

@Composable
fun Stepper(qty: Long, unit: String, onStep: (Boolean) -> Unit, onTapQty: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        StepButton("−", Qty.canDecrease(qty, unit), MaterialTheme.colorScheme.secondaryContainer) { onStep(false) }
        Text(
            Qty.label(qty, unit),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            maxLines = 1,
            modifier = Modifier
                .widthIn(min = 46.dp)
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onTapQty)
                .padding(horizontal = 4.dp, vertical = 6.dp),
        )
        StepButton("+", true, MaterialTheme.colorScheme.primaryContainer) { onStep(true) }
    }
}

@Composable
fun StepButton(symbol: String, enabled: Boolean, color: Color, size: Int = 36, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        color = if (enabled) color else color.copy(alpha = 0.35f),
        modifier = Modifier.size(size.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                symbol,
                fontSize = (size * 0.55).sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (enabled) 1f else 0.4f),
            )
        }
    }
}

@Composable
fun CategoryHeader(category: Int, count: Int, modifier: Modifier = Modifier) {
    val c = Categories.get(category)
    Row(
        modifier.fillMaxWidth().padding(start = 6.dp, end = 10.dp, top = 16.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(c.emoji, fontSize = 18.sp)
        Spacer(Modifier.width(8.dp))
        Text(
            c.name,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f),
        )
        Text("$count", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// ---------------------------------------------------------------- invoerbalk

@Composable
fun AddBar(
    history: List<HistoryEntry>,
    lists: List<ShopList>,
    selected: String,
    target: String?,
    onAddText: (String) -> Unit,
    onAddSuggestion: (Suggestion) -> Unit,
    onTarget: (String) -> Unit,
) {
    var text by rememberSaveable { mutableStateOf("") }
    var focused by remember { mutableStateOf(false) }
    val parsed = remember(text) { QtyParser.parse(text) }
    val suggestions = remember(parsed.name, history) { Suggest.search(parsed.name, history) }
    val frequent = remember(history) { history.sortedByDescending { it.count }.take(15) }
    val exact = suggestions.any { Suggest.norm(it.name) == Suggest.norm(parsed.name) }

    fun withTyped(s: Suggestion): Suggestion =
        if (parsed.qty != null && parsed.unit != null) s.copy(qty = parsed.qty, unit = parsed.unit) else s

    fun submit() {
        if (text.isBlank()) return
        onAddText(text)
        text = ""
    }

    Surface(color = MaterialTheme.colorScheme.surfaceContainer, shadowElevation = 12.dp) {
        Column(Modifier.navigationBarsPadding().imePadding()) {
            if (focused && parsed.name.isNotBlank()) {
                Column(Modifier.heightIn(max = 320.dp).verticalScroll(rememberScrollState())) {
                    if (!exact) {
                        SuggestionRow(
                            emoji = "＋",
                            name = "\"${parsed.name}\" toevoegen",
                            hint = parsed.qty?.let { Qty.label(it, parsed.unit ?: "st") },
                            badge = null,
                            onClick = { submit() },
                        )
                    }
                    suggestions.forEach { s ->
                        val t = withTyped(s)
                        SuggestionRow(
                            emoji = Categories.get(s.category).emoji,
                            name = s.name,
                            hint = Qty.label(t.qty, t.unit),
                            badge = if (s.fromHistory) "vaak" else null,
                            onClick = { onAddSuggestion(t); text = "" },
                        )
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            } else if (focused && frequent.isNotEmpty()) {
                Text(
                    "Vaak gekocht",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 16.dp, top = 10.dp),
                )
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(frequent, key = { it.key }) { h ->
                        SuggestionChip(
                            onClick = { onAddSuggestion(Suggestion(h.name, h.category, h.unit, h.qty, true)) },
                            label = { Text("${Categories.get(h.category).emoji} ${h.name}") },
                        )
                    }
                }
            }

            if (selected == ALL && lists.size > 1) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 16.dp, top = 6.dp)) {
                    Text("Naar:", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        items(lists, key = { it.id }) { l ->
                            FilterChip(
                                selected = l.id == target,
                                onClick = { onTarget(l.id) },
                                label = { Text(l.name, style = MaterialTheme.typography.labelMedium) },
                                modifier = Modifier.height(30.dp),
                            )
                        }
                    }
                }
            }

            Row(
                Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextField(
                    value = text,
                    onValueChange = { text = it },
                    placeholder = { Text("Wat heb je nodig?  (bv. 2 melk)") },
                    singleLine = true,
                    shape = RoundedCornerShape(28.dp),
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent,
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                    ),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(onDone = { submit() }),
                    modifier = Modifier.weight(1f).onFocusChanged { focused = it.isFocused },
                )
                Spacer(Modifier.width(10.dp))
                FilledIconButton(
                    onClick = { submit() },
                    enabled = text.isNotBlank(),
                    modifier = Modifier.size(54.dp),
                ) { Icon(Icons.Filled.Add, "Toevoegen") }
            }
        }
    }
}

@Composable
fun SuggestionRow(emoji: String, name: String, hint: String?, badge: String?, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(emoji, fontSize = 18.sp, modifier = Modifier.width(30.dp))
        Text(name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (badge != null) {
            Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(8.dp)) {
                Text(badge, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
            }
            Spacer(Modifier.width(8.dp))
        }
        if (hint != null) {
            Text(hint, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// ---------------------------------------------------------------- bewerk-paneel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditSheet(
    item: Item,
    lists: List<ShopList>,
    onDismiss: () -> Unit,
    onSave: (name: String, qty: Long, unit: String, category: Int, listId: String) -> Unit,
    onDelete: () -> Unit,
) {
    var name by remember(item.id) { mutableStateOf(item.name) }
    var unit by remember(item.id) { mutableStateOf(item.unit) }
    var qty by remember(item.id) { mutableStateOf(item.qty) }
    var category by remember(item.id) { mutableStateOf(item.category) }
    var listId by remember(item.id) { mutableStateOf(item.listId) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Product") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth(),
            )

            SectionTitle("Hoeveelheid")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = unit == "st", onClick = { if (unit != "st") { unit = "st"; qty = 1 } }, label = { Text("Aantal") })
                FilterChip(selected = unit == "g", onClick = { if (unit != "g") { unit = "g"; qty = 250 } }, label = { Text("Gram") })
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                StepButton("−", Qty.canDecrease(qty, unit), MaterialTheme.colorScheme.secondaryContainer, size = 52) { qty = Qty.step(qty, unit, false) }
                Text(
                    Qty.label(qty, unit),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.widthIn(min = 130.dp),
                )
                StepButton("+", true, MaterialTheme.colorScheme.primaryContainer, size = 52) { qty = Qty.step(qty, unit, true) }
            }
            Spacer(Modifier.height(10.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                (if (unit == "g") Qty.quickG else Qty.quickSt).forEach { v ->
                    FilterChip(selected = qty == v, onClick = { qty = v }, label = { Text(Qty.label(v, unit)) })
                }
            }

            SectionTitle("Categorie")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Categories.all.forEach { c ->
                    FilterChip(selected = category == c.id, onClick = { category = c.id }, label = { Text("${c.emoji} ${c.name}") })
                }
            }

            if (lists.size > 1) {
                SectionTitle("Lijst")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    lists.forEach { l ->
                        FilterChip(selected = listId == l.id, onClick = { listId = l.id }, label = { Text(l.name) })
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(
                    onClick = onDelete,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) {
                    Icon(Icons.Filled.Delete, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Verwijderen")
                }
                Spacer(Modifier.weight(1f))
                Button(onClick = { onSave(name, qty, unit, category, listId) }, modifier = Modifier.height(48.dp)) {
                    Text("Opslaan")
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = 20.dp, bottom = 8.dp),
    )
}

// ---------------------------------------------------------------- dialogen

@Composable
fun NameDialog(title: String, initial: String, confirm: String, onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var value by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                singleLine = true,
                placeholder = { Text("Bijv. Drogist") },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { if (value.isNotBlank()) onConfirm(value) }),
            )
        },
        confirmButton = { TextButton(onClick = { onConfirm(value) }, enabled = value.isNotBlank()) { Text(confirm) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuleren") } },
    )
}

@Composable
fun ConfirmDialog(title: String, text: String, confirm: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = {
            TextButton(onClick = onConfirm, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                Text(confirm)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuleren") } },
    )
}
