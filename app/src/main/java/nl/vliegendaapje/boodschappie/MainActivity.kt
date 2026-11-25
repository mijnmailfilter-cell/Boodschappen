package nl.vliegendaapje.boodschappie

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import nl.vliegendaapje.boodschappie.ui.BoodschappieTheme
import nl.vliegendaapje.boodschappie.ui.MainScreen
import nl.vliegendaapje.boodschappie.ui.OnboardingScreen
import nl.vliegendaapje.boodschappie.ui.SettingsScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { BoodschappieTheme { App() } }
    }
}

@Composable
fun App(vm: MainViewModel = viewModel()) {
    val household by vm.household.collectAsStateWithLifecycle()
    var showSettings by rememberSaveable { mutableStateOf(false) }
    val code = household

    when {
        code == null -> OnboardingScreen(vm)
        showSettings -> {
            BackHandler { showSettings = false }
            SettingsScreen(
                vm = vm,
                code = code,
                onBack = { showSettings = false },
                onLeave = { showSettings = false; vm.leave() },
            )
        }
        else -> MainScreen(vm, onOpenSettings = { showSettings = true })
    }
}
