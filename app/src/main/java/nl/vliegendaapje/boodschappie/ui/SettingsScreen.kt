package nl.vliegendaapje.boodschappie.ui

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import nl.vliegendaapje.boodschappie.MainViewModel
import nl.vliegendaapje.boodschappie.data.Codes
import nl.vliegendaapje.boodschappie.data.ShopList

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(vm: MainViewModel, code: String, onBack: () -> Unit, onLeave: () -> Unit) {
    val lists by vm.lists.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val pretty = Codes.pretty(code)

    var newList by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf<ShopList?>(null) }
    var deleting by remember { mutableStateOf<ShopList?>(null) }
    var confirmLeave by remember { mutableStateOf(false) }

    val version = remember {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: ""
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Instellingen") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Terug") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = MaterialTheme.shapes.extraLarge,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(20.dp)) {
                    Text("Jullie koppelcode", style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        pretty,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 34.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 3.sp,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Wie mee wil doen installeert de app, kiest \"Ik heb een code\" en vult deze code in.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(onClick = {
                            val send = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(
                                    Intent.EXTRA_TEXT,
                                    "Doe mee met onze boodschappenlijst in Boodschappie! Kies \"Ik heb een code\" en vul in: $pretty",
                                )
                            }
                            context.startActivity(Intent.createChooser(send, "Code delen"))
                        }) {
                            Icon(Icons.Filled.Share, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Delen")
                        }
                        OutlinedButton(onClick = {
                            clipboard.setText(AnnotatedString(pretty))
                            Toast.makeText(context, "Code gekopieerd", Toast.LENGTH_SHORT).show()
                        }) { Text("Kopiëren") }
                    }
                }
            }

            Spacer(Modifier.height(28.dp))
            Text("Lijsten", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth(),
            ) {
                lists.sortedBy { it.order }.forEach { l ->
                    ListItem(
                        headlineContent = { Text(l.name) },
                        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
                        trailingContent = {
                            Row {
                                IconButton(onClick = { renaming = l }) { Icon(Icons.Filled.Edit, "Hernoemen") }
                                IconButton(onClick = { deleting = l }) { Icon(Icons.Filled.Delete, "Verwijderen") }
                            }
                        },
                    )
                }
                TextButton(onClick = { newList = true }, modifier = Modifier.padding(8.dp)) {
                    Icon(Icons.Filled.Add, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Nieuwe lijst")
                }
            }

            Spacer(Modifier.height(28.dp))
            Text("Handig om te weten", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(6.dp))
            Text(
                "• Typ \"3 bananen\" of \"500g gehakt\" om meteen de hoeveelheid mee te geven.\n" +
                    "• Tik op een product om het af te vinken.\n" +
                    "• Houd een product vast (of tik op het aantal) om het aan te passen of een andere categorie te kiezen. De app onthoudt dat voor de volgende keer.\n" +
                    "• Veeg naar links om te verwijderen.\n" +
                    "• Werkt ook zonder bereik in de winkel; alles synchroniseert zodra je weer verbinding hebt.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(32.dp))
            OutlinedButton(
                onClick = { confirmLeave = true },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Ontkoppelen van deze lijst") }

            Spacer(Modifier.height(16.dp))
            Text(
                "Boodschappie $version",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    if (newList) NameDialog("Nieuwe lijst", "", "Maken", { vm.addList(it); newList = false }, { newList = false })
    renaming?.let { l ->
        NameDialog("Lijst hernoemen", l.name, "Opslaan", { vm.renameList(l.id, it); renaming = null }, { renaming = null })
    }
    deleting?.let { l ->
        ConfirmDialog(
            "\"${l.name}\" verwijderen?",
            "De lijst en alle producten erop worden voor jullie allebei verwijderd.",
            "Verwijderen",
            { vm.deleteList(l.id); deleting = null },
            { deleting = null },
        )
    }
    if (confirmLeave) {
        ConfirmDialog(
            "Ontkoppelen?",
            "Je ziet de gedeelde lijst dan niet meer op deze telefoon. De lijst zelf blijft bestaan; met de code ($pretty) kun je later weer koppelen.",
            "Ontkoppelen",
            { confirmLeave = false; onLeave() },
            { confirmLeave = false },
        )
    }
}
