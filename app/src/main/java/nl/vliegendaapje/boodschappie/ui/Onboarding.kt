package nl.vliegendaapje.boodschappie.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import nl.vliegendaapje.boodschappie.MainViewModel
import nl.vliegendaapje.boodschappie.data.Codes

@Composable
fun OnboardingScreen(vm: MainViewModel) {
    val busy by vm.busy.collectAsStateWithLifecycle()
    val message by vm.message.collectAsStateWithLifecycle()
    var joining by rememberSaveable { mutableStateOf(false) }
    var code by rememberSaveable { mutableStateOf("") }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text("🛒", fontSize = 72.sp)
            Spacer(Modifier.height(12.dp))
            Text(
                "Boodschappie",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Samen één boodschappenlijst. Wat de een toevoegt, ziet de ander meteen.",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(40.dp))

            if (!joining) {
                Button(
                    onClick = { vm.create() },
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                ) { Text("Nieuwe lijst beginnen", style = MaterialTheme.typography.titleMedium) }
                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    onClick = { joining = true; vm.message.value = null },
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                ) { Text("Ik heb een code", style = MaterialTheme.typography.titleMedium) }
            } else {
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = Codes.typing(it) },
                    label = { Text("Koppelcode") },
                    placeholder = { Text("ABCD-EFGH") },
                    singleLine = true,
                    textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 24.sp, letterSpacing = 2.sp),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Characters,
                        keyboardType = KeyboardType.Ascii,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(onDone = { vm.join(code) }),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = { vm.join(code) },
                    enabled = !busy && Codes.clean(code).length == 8,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                ) { Text("Koppelen", style = MaterialTheme.typography.titleMedium) }
                TextButton(onClick = { joining = false; vm.message.value = null }) { Text("Terug") }
            }

            if (busy) {
                Spacer(Modifier.height(24.dp))
                CircularProgressIndicator()
            }
            message?.let {
                Spacer(Modifier.height(16.dp))
                Text(it, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
            }
        }
    }
}
