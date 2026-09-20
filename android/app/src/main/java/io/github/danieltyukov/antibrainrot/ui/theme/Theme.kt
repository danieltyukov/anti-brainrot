package io.github.danieltyukov.antibrainrot.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// The site's palette: sky, cream and navy ink.
val Sky = Color(0xFF73B8EE)
val LightSky = Color(0xFFB2DCFA)
val PaleSky = Color(0xFFD7EFFF)
val Cream = Color(0xFFECEADE)
val Ink = Color(0xFF1A1A2E)
val Muted = Color(0xFF2C3E50)
val Dark = Color(0xFF0A1220)
val DarkSurface = Color(0xFF16213A)
val Accent = Color(0xFF3D8FD1)
val DeepAccent = Color(0xFF2B6FB0)
val Leaf = Color(0xFF3FB58A)
val Amber = Color(0xFFE8A33D)

val SkyGradient = Brush.verticalGradient(listOf(Sky, LightSky, PaleSky))
// The hero card while the filter is on.
val OnGradient = Brush.linearGradient(listOf(DeepAccent, Accent, Sky))

// Colours that are not part of the Material scheme.
data class Extra(val pageGradient: Brush, val track: Color, val chartSecondary: Color, val tileBackground: Color)

private val LightExtra = Extra(Brush.verticalGradient(listOf(PaleSky, Color(0xFFEEF6FD))), Color(0x1A1A1A2E), Muted.copy(alpha = 0.55f), Color(0xFFF3F8FD))

val LocalExtra = staticCompositionLocalOf { LightExtra }

private val LightScheme = lightColorScheme(
    primary = Accent,
    onPrimary = Color.White,
    primaryContainer = PaleSky,
    onPrimaryContainer = Ink,
    secondary = Ink,
    secondaryContainer = Color(0xFFE3EEF8),
    onSecondaryContainer = Ink,
    background = Color(0xFFEEF6FD),
    onBackground = Ink,
    surface = Color.White,
    onSurface = Ink,
    surfaceVariant = Color(0xFFE3EEF8),
    onSurfaceVariant = Muted,
    surfaceContainer = Color(0xFFF7FAFE),
    outline = Color(0x331A1A2E),
    outlineVariant = Color(0x1A1A1A2E),
    error = Color(0xFFB3261E),
)

private val DarkScheme = darkColorScheme(
    primary = Sky,
    onPrimary = Dark,
    primaryContainer = Color(0xFF1F3A5C),
    onPrimaryContainer = Cream,
    secondary = Cream,
    secondaryContainer = Color(0xFF243352),
    onSecondaryContainer = Cream,
    background = Dark,
    onBackground = Cream,
    surface = DarkSurface,
    onSurface = Cream,
    surfaceVariant = Color(0xFF1F2B4A),
    onSurfaceVariant = Color(0xFFC7C4B6),
    surfaceContainer = Color(0xFF111C33),
    outline = Color(0x33ECEADE),
    outlineVariant = Color(0x1AECEADE),
    error = Color(0xFFFF8A80),
)

private val DarkExtra = Extra(Brush.verticalGradient(listOf(Color(0xFF14213D), Dark)), Color(0x26ECEADE), Cream.copy(alpha = 0.5f), Color(0xFF1B2744))

val AppTypography = Typography(
    displayLarge = TextStyle(fontSize = 56.sp, lineHeight = 60.sp, fontWeight = FontWeight.Bold, letterSpacing = (-1.5).sp),
    displayMedium = TextStyle(fontSize = 40.sp, lineHeight = 44.sp, fontWeight = FontWeight.Bold, letterSpacing = (-1).sp),
    headlineMedium = TextStyle(fontSize = 26.sp, lineHeight = 32.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.5).sp),
    headlineSmall = TextStyle(fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.3).sp),
    titleLarge = TextStyle(fontSize = 21.sp, lineHeight = 26.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.4).sp),
    titleMedium = TextStyle(fontSize = 17.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.1).sp),
    titleSmall = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 23.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontSize = 12.5.sp, lineHeight = 17.sp),
    labelLarge = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.1.sp),
    labelMedium = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.3.sp),
    labelSmall = TextStyle(fontSize = 11.sp, lineHeight = 14.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.3.sp),
)

val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun AntiBrainrotTheme(theme: String = "system", content: @Composable () -> Unit) {
    val dark = when (theme) {
        "light" -> false
        "dark" -> true
        else -> isSystemInDarkTheme()
    }
    CompositionLocalProvider(LocalExtra provides if (dark) DarkExtra else LightExtra) {
        MaterialTheme(
            colorScheme = if (dark) DarkScheme else LightScheme,
            typography = AppTypography,
            shapes = AppShapes,
            content = content,
        )
    }
}

// Convenience so screens do not need the CompositionLocal name.
object Theme {
    val extra: Extra
        @Composable get() = LocalExtra.current
}
