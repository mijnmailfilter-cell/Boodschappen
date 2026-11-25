package nl.vliegendaapje.boodschappie.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Light = lightColorScheme(
    primary = Color(0xFF2E7D5B),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB7EFD2),
    onPrimaryContainer = Color(0xFF00210F),
    secondary = Color(0xFF4E6356),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD7EBDD),
    onSecondaryContainer = Color(0xFF0B1F14),
    tertiary = Color(0xFFD9772B),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFDCC4),
    onTertiaryContainer = Color(0xFF2F1500),
    background = Color(0xFFF4F9F4),
    onBackground = Color(0xFF171D19),
    surface = Color(0xFFF4F9F4),
    onSurface = Color(0xFF171D19),
    surfaceVariant = Color(0xFFDCE5DC),
    onSurfaceVariant = Color(0xFF404943),
    outline = Color(0xFF8FA096),
    outlineVariant = Color(0xFFC6D1C8),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFEFF5EF),
    surfaceContainer = Color(0xFFE9F0EA),
    surfaceContainerHigh = Color(0xFFE3EAE4),
    surfaceContainerHighest = Color(0xFFDDE4DE),
    error = Color(0xFFBA1A1A),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
)

private val Dark = darkColorScheme(
    primary = Color(0xFF8DD9B1),
    onPrimary = Color(0xFF00391F),
    primaryContainer = Color(0xFF0F5134),
    onPrimaryContainer = Color(0xFFA9F5CB),
    secondary = Color(0xFFB5CCBC),
    onSecondary = Color(0xFF213529),
    secondaryContainer = Color(0xFF374B3F),
    onSecondaryContainer = Color(0xFFD0E8D7),
    tertiary = Color(0xFFFFB782),
    onTertiary = Color(0xFF4F2500),
    tertiaryContainer = Color(0xFF703700),
    onTertiaryContainer = Color(0xFFFFDCC4),
    background = Color(0xFF0F1511),
    onBackground = Color(0xFFDFE4DF),
    surface = Color(0xFF0F1511),
    onSurface = Color(0xFFDFE4DF),
    surfaceVariant = Color(0xFF404943),
    onSurfaceVariant = Color(0xFFBFC9C1),
    outline = Color(0xFF89938B),
    outlineVariant = Color(0xFF404943),
    surfaceContainerLowest = Color(0xFF1C231F),
    surfaceContainerLow = Color(0xFF171D19),
    surfaceContainer = Color(0xFF1B211D),
    surfaceContainerHigh = Color(0xFF252B27),
    surfaceContainerHighest = Color(0xFF303632),
    error = Color(0xFFFFB4AB),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
)

@Composable
fun BoodschappieTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) Dark else Light, content = content)
}
