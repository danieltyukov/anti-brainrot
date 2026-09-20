package io.github.danieltyukov.antibrainrot.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

val Sky = Color(0xFF73B8EE)
val LightSky = Color(0xFFB2DCFA)
val PaleSky = Color(0xFFD7EFFF)
val Cream = Color(0xFFECEADE)
val Ink = Color(0xFF1A1A2E)
val Muted = Color(0xFF2C3E50)
val Dark = Color(0xFF0A1220)
val DarkSurface = Color(0xFF16213A)
val Accent = Color(0xFF3D8FD1)

val SkyGradient = Brush.verticalGradient(listOf(Sky, LightSky, PaleSky))

private val LightScheme = lightColorScheme(
    primary = Accent,
    onPrimary = Color.White,
    secondary = Ink,
    background = Color(0xFFE9F4FD),
    onBackground = Ink,
    surface = Color.White,
    onSurface = Ink,
    surfaceVariant = Color(0xFFE3EEF8),
    onSurfaceVariant = Muted,
    outline = Color(0x331A1A2E),
    error = Color(0xFFB3261E),
)

private val DarkScheme = darkColorScheme(
    primary = Sky,
    onPrimary = Dark,
    secondary = Cream,
    background = Dark,
    onBackground = Cream,
    surface = DarkSurface,
    onSurface = Cream,
    surfaceVariant = Color(0xFF1F2B4A),
    onSurfaceVariant = Color(0xFFC7C4B6),
    outline = Color(0x33ECEADE),
    error = Color(0xFFFF8A80),
)

@Composable
fun AntiBrainrotTheme(theme: String = "system", content: @Composable () -> Unit) {
    val dark = when (theme) {
        "light" -> false
        "dark" -> true
        else -> isSystemInDarkTheme()
    }
    MaterialTheme(colorScheme = if (dark) DarkScheme else LightScheme, content = content)
}
