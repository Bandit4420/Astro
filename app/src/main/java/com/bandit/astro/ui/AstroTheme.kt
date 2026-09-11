package com.bandit.astro.ui

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import com.bandit.astro.data.Preferences

@Composable
fun AstroTheme(preferences: Preferences, artworkAccent: String, content: @Composable () -> Unit) {
    val preset = when (preferences.theme) {
        "Sakura" -> Color(0xFFFFB5D0)
        "Cyberpunk" -> Color(0xFFD6A8FF)
        "Ocean" -> Color(0xFF98D8FF)
        else -> Color(0xFFA5E9CE)
    }
    val extracted = if (preferences.dynamic && artworkAccent.matches(Regex("#[0-9a-fA-F]{6}")))
        Color(android.graphics.Color.parseColor(artworkAccent)) else preset
    // Lift saturated/dark cover colours so text and controls stay legible on dark surfaces.
    val accent = lerp(extracted, Color.White, .3f)
    val background = if (preferences.theme == "AMOLED") Color.Black else lerp(Color(0xFF080C10), accent, .025f)
    MaterialTheme(colorScheme = darkColorScheme(
        primary = accent, onPrimary = Color(0xFF102018), secondary = accent,
        background = background, onBackground = Color(0xFFF0F2F4),
        surface = lerp(background, accent, .045f), onSurface = Color(0xFFF0F2F4),
        surfaceVariant = lerp(background, accent, .12f), onSurfaceVariant = Color(0xFFBBC6CB),
        primaryContainer = lerp(background, accent, .18f), onPrimaryContainer = accent,
        outline = lerp(background, accent, .35f)
    ), content = content)
}
