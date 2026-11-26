package nl.vliegendaapje.boodschappie

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import nl.vliegendaapje.boodschappie.data.ALL
import nl.vliegendaapje.boodschappie.data.Categories
import nl.vliegendaapje.boodschappie.data.Qty
import nl.vliegendaapje.boodschappie.data.QtyParser
import nl.vliegendaapje.boodschappie.data.Suggest
import nl.vliegendaapje.boodschappie.ui.BoodschappieTheme
import nl.vliegendaapje.boodschappie.ui.SuggestionRow
import nl.vliegendaapje.boodschappie.ui.ListeningBar
import nl.vliegendaapje.boodschappie.ui.rememberVoiceInput

/** Klein invoervenster over het beginscherm (vanaf de widget). */
class QuickAddActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val voice = intent.getBooleanExtra(EXTRA_VOICE, false)
        val list = intent.getStringExtra(EXTRA_LIST)?.takeIf { it != ALL }
        setContent {
            BoodschappieTheme {
                QuickAddScreen(startVoice = voice, listId = list, onClose = { finish() })
            }
        }
    }

    companion object {
        const val EXTRA_VOICE = "voice"
        const val EXTRA_LIST = "list"
    }
}

fun speechIntent(): Intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
    putExtra(RecognizerIntent.EXTRA_LANGUAGE, "nl-NL")
    putExtra(RecognizerIntent.EXTRA_PROMPT, "Wat moet er op de lijst?")
}

@Composable
private fun QuickAddScreen(startVoice: Boolean, listId: String?, onClose: () -> Unit) {
    val vm: MainViewModel = viewModel()
    val household by vm.household.collectAsStateWithLifecycle()
    val lists by vm.lists.collectAsStateWithLifecycle()
    val history by vm.history.collectAsStateWithLifecycle()
    @Suppress("UNUSED_VARIABLE") val items by vm.items.collectAsStateWithLifecycle() // live houden voor samenvoegen
    val scope = rememberCoroutineScope()
    val keyboard = LocalSoftwareKeyboardController.current
    val focus = remember { FocusRequester() }

    var target by rememberSaveable { mutableStateOf(listId) }
    var text by rememberSaveable { mutableStateOf("") }
    var voiceStarted by rememberSaveable { mutableStateOf(false) }
    val added = remember { mutableStateListOf<String>() }
    var typedSomething by remember { mutableStateOf(false) }

    LaunchedEffect(target) { vm.quickTarget = target }

    val sorted = remember(lists) { lists.sortedBy { it.order } }
    val shownTarget = target?.takeIf { t -> sorted.any { it.id == t } } ?: vm.effectiveTarget()

    val (voice, beginVoice) = rememberVoiceInput { spoken ->
        scope.launch {
            vm.awaitReady()
            added += vm.addSpoken(spoken)
            if (startVoice && !typedSomething) {
                delay(1600); onClose()
            }
        }
    }
    // mislukt of geannuleerd vanaf de widget: dan maar typen
    LaunchedEffect(voice.listening, voice.error) {
        if (startVoice && voiceStarted && !voice.listening) {
            delay(700)
            if (added.isEmpty() && !voice.listening) runCatching { focus.requestFocus(); keyboard?.show() }
        }
    }

    fun launchVoice() {
        keyboard?.hide()
        beginVoice()
    }

    LaunchedEffect(Unit) {
        if (startVoice && !voiceStarted) {
            voiceStarted = true
            launchVoice()
        } else if (!startVoice) {
            delay(150)
            runCatching { focus.requestFocus(); keyboard?.show() }
        }
    }

    fun submit() {
        val t = text.trim()
        if (t.isEmpty()) return
        typedSomething = true
        text = ""
        scope.launch {
            vm.awaitReady()
            vm.addFromText(t)
            added += t
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.35f))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onClose() },
        contentAlignment = Alignment.BottomCenter,
    ) {
        Surface(
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { },
        ) {
            Column(Modifier.navigationBarsPadding().imePadding().padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🛒  Snel toevoegen", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    TextButton(onClick = onClose) { Text("Klaar") }
                }

                if (household == null) {
                    Text("Open eerst Boodschappie en koppel een lijst.", modifier = Modifier.padding(vertical = 12.dp))
                    return@Column
                }

                if (added.isNotEmpty()) {
                    Text(
                        "✓ " + added.joinToString(", "),
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                }
                voice.error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(bottom = 6.dp))
                }

                if (sorted.size > 1) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(bottom = 6.dp)) {
                        items(sorted, key = { it.id }) { l ->
                            FilterChip(selected = l.id == shownTarget, onClick = { target = l.id }, label = { Text(l.name) })
                        }
                    }
                }

                val parsed = remember(text) { QtyParser.parse(text) }
                val suggestions = remember(parsed.name, history) { Suggest.search(parsed.name, history, limit = 5) }
                if (parsed.name.isNotBlank() && suggestions.isNotEmpty()) {
                    Column(Modifier.heightIn(max = 260.dp).verticalScroll(rememberScrollState())) {
                        suggestions.forEach { s ->
                            val t = if (parsed.qty != null && parsed.unit != null) s.copy(qty = parsed.qty, unit = parsed.unit) else s
                            SuggestionRow(
                                emoji = Categories.get(s.category).emoji,
                                name = s.name,
                                hint = Qty.label(t.qty, t.unit),
                                badge = if (s.fromHistory) "vaak" else null,
                                onClick = {
                                    typedSomething = true
                                    text = ""
                                    scope.launch { vm.awaitReady(); vm.addSuggestion(t); added += t.name }
                                },
                            )
                        }
                    }
                }

                if (voice.listening) {
                    ListeningBar(voice, Modifier.padding(top = 6.dp))
                } else Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 6.dp)) {
                    TextField(
                        value = text,
                        onValueChange = { text = it },
                        placeholder = { Text("Wat heb je nodig?") },
                        singleLine = true,
                        shape = RoundedCornerShape(28.dp),
                        colors = TextFieldDefaults.colors(
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                        ),
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { submit() }),
                        modifier = Modifier.weight(1f).focusRequester(focus),
                    )
                    Spacer(Modifier.width(8.dp))
                    FilledTonalIconButton(onClick = { launchVoice() }, modifier = Modifier.size(50.dp)) {
                        Text("🎤", fontSize = 20.sp)
                    }
                    Spacer(Modifier.width(6.dp))
                    FilledIconButton(onClick = { submit() }, enabled = text.isNotBlank(), modifier = Modifier.size(50.dp)) {
                        Icon(Icons.Filled.Add, "Toevoegen")
                    }
                }
            }
        }
    }
}
