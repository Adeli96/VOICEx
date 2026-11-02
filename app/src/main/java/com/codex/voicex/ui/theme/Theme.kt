package com.codex.voicex.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

private val DarkPalette = darkColorScheme(
    primary = Color(0xFF9E7CFF),
    onPrimary = Color(0xFF08051A),
    primaryContainer = Color(0xFF2D1E63),
    onPrimaryContainer = Color(0xFFEDE7FF),
    secondary = Color(0xFF00E5FF),
    onSecondary = Color(0xFF001A20),
    tertiary = Color(0xFFFF80AB),
    background = Color(0xFF0B0D1B),
    onBackground = Color(0xFFECEAFB),
    surface = Color(0xFF12162A),
    onSurface = Color(0xFFECEAFB),
    surfaceVariant = Color(0xFF1B2140),
    onSurfaceVariant = Color(0xFFC7C8E4),
    outline = Color(0xFF3B3F63),
    error = Color(0xFFFF6B7A)
)

private val LightPalette = lightColorScheme(
    primary = Color(0xFF6742E5),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE4DAFF),
    onPrimaryContainer = Color(0xFF1E0A6A),
    secondary = Color(0xFF007A99),
    onSecondary = Color.White,
    tertiary = Color(0xFFD62E67),
    background = Color(0xFFF7F5FF),
    onBackground = Color(0xFF1A1B2F),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1A1B2F),
    surfaceVariant = Color(0xFFECE9F9),
    onSurfaceVariant = Color(0xFF4B4C69),
    outline = Color(0xFFBEBDD6),
    error = Color(0xFFB3261E)
)

private val VoiceXTypography = Typography(
    displayLarge = TextStyle(fontSize = 34.sp, letterSpacing = 0.sp),
    headlineMedium = TextStyle(fontSize = 22.sp),
    titleLarge = TextStyle(fontSize = 20.sp),
    titleMedium = TextStyle(fontSize = 16.sp),
    bodyLarge = TextStyle(fontSize = 16.sp),
    bodyMedium = TextStyle(fontSize = 14.sp),
    labelLarge = TextStyle(fontSize = 14.sp)
)

@Composable
fun VoiceXTheme(themePref: String, content: @Composable () -> Unit) {
    val dark = when (themePref) {
        "light" -> false
        "dark" -> true
        else -> isSystemInDarkTheme()
    }
    val colors = if (dark) DarkPalette else LightPalette

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            window.navigationBarColor = android.graphics.Color.TRANSPARENT
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !dark
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !dark
        }
    }

    MaterialTheme(colorScheme = colors, typography = VoiceXTypography, content = content)
}
