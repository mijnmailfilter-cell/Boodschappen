package nl.vliegendaapje.boodschappie.ui

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import nl.vliegendaapje.boodschappie.speechIntent
import kotlin.math.abs
import kotlin.math.sin

/** Eigen spraakherkenning zonder het Google-venster. */
class VoiceController(private val context: Context, private val onResult: (String) -> Unit) {
    var listening by mutableStateOf(false)
        private set
    var partial by mutableStateOf("")
        private set
    var level by mutableFloatStateOf(0f)
        private set
    var error by mutableStateOf<String?>(null)

    private var recognizer: SpeechRecognizer? = null

    val available: Boolean get() = SpeechRecognizer.isRecognitionAvailable(context)

    fun start() {
        error = null
        partial = ""
        level = 0f
        recognizer?.destroy()
        val r = SpeechRecognizer.createSpeechRecognizer(context)
        recognizer = r
        r.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {
                level = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
            }
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {
                level = 0f
            }
            override fun onError(code: Int) {
                listening = false
                level = 0f
                val heard = partial
                partial = ""
                if (heard.isNotBlank() && (code == SpeechRecognizer.ERROR_NO_MATCH || code == SpeechRecognizer.ERROR_SPEECH_TIMEOUT)) {
                    onResult(heard); return
                }
                error = when (code) {
                    SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Ik heb niets verstaan"
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Geen toegang tot de microfoon"
                    SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Geen verbinding"
                    SpeechRecognizer.ERROR_CLIENT -> null // zelf geannuleerd
                    else -> "Inspreken lukte niet"
                }
            }
            override fun onResults(results: Bundle?) {
                val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                    ?.takeIf { it.isNotBlank() } ?: partial
                listening = false
                level = 0f
                partial = ""
                if (text.isNotBlank()) onResult(text) else error = "Ik heb niets verstaan"
            }
            override fun onPartialResults(partialResults: Bundle?) {
                partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                    ?.takeIf { it.isNotBlank() }?.let { partial = it }
            }
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "nl-NL")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1800L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1800L)
        }
        listening = true
        runCatching { r.startListening(intent) }.onFailure {
            listening = false
            error = "Inspreken lukte niet"
        }
    }

    /** Klaar met praten: verwerk wat er gezegd is. */
    fun finish() {
        recognizer?.stopListening()
    }

    fun cancel() {
        recognizer?.cancel()
        listening = false
        partial = ""
        level = 0f
    }

    fun release() {
        recognizer?.destroy()
        recognizer = null
    }
}

/**
 * Geeft een controller plus een start-functie die eerst om microfoon-toestemming vraagt.
 * Is er geen spraakdienst, dan valt hij terug op het standaard Google-venster.
 */
@Composable
fun rememberVoiceInput(onResult: (String) -> Unit): Pair<VoiceController, () -> Unit> {
    val context = LocalContext.current
    val callback by rememberUpdatedState(onResult)
    val controller = remember { VoiceController(context) { callback(it) } }
    DisposableEffect(Unit) { onDispose { controller.release() } }

    val fallback = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { res ->
        val spoken = res.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
        if (res.resultCode == Activity.RESULT_OK && !spoken.isNullOrBlank()) callback(spoken)
    }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) controller.start() else controller.error = "Geef de app toegang tot de microfoon om in te spreken"
    }

    val start: () -> Unit = {
        when {
            !controller.available -> runCatching { fallback.launch(speechIntent()) }
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED -> controller.start()
            else -> permission.launch(Manifest.permission.RECORD_AUDIO)
        }
    }
    return controller to start
}

/** Het luister-pilletje dat de invoerbalk tijdelijk vervangt. */
@Composable
fun ListeningBar(controller: VoiceController, modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    Surface(shape = RoundedCornerShape(28.dp), color = cs.primaryContainer, modifier = modifier.fillMaxWidth()) {
        Row(
            Modifier.heightIn(min = 56.dp).padding(start = 18.dp, end = 6.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Waveform(controller.level)
            Spacer(Modifier.width(14.dp))
            Text(
                controller.partial.ifBlank { "Ik luister…" },
                style = MaterialTheme.typography.bodyLarge,
                color = if (controller.partial.isBlank()) cs.onPrimaryContainer.copy(alpha = 0.6f) else cs.onPrimaryContainer,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = { controller.cancel() }) {
                Icon(Icons.Filled.Close, "Annuleren", tint = cs.onPrimaryContainer)
            }
            FilledIconButton(onClick = { controller.finish() }, modifier = Modifier.size(46.dp)) {
                Icon(Icons.Filled.Check, "Klaar")
            }
        }
    }
}

@Composable
private fun Waveform(level: Float) {
    val transition = rememberInfiniteTransition(label = "wave")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing)),
        label = "phase",
    )
    val lvl by animateFloatAsState(level, label = "level")
    val color = MaterialTheme.colorScheme.primary
    Row(horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically, modifier = Modifier.height(28.dp)) {
        repeat(5) { i ->
            val wave = abs(sin(phase + i * 1.1f))
            val h = 5f + (0.25f + lvl * 0.75f) * 23f * (0.35f + 0.65f * wave)
            Box(
                Modifier
                    .width(4.dp)
                    .height(h.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(color)
            )
        }
    }
}
