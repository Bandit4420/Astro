package com.bandit.astro.ui

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import com.bandit.astro.data.Preferences

val LocalAppearance = staticCompositionLocalOf { Preferences() }
val themeNames=listOf("Astro","Midnight blue","Sakura","Lavender","Golden hour","AMOLED","Liquid Glass","Cyberpunk","Minimal","Dynamic","Monochrome","Ocean")

@Composable
fun AstroTheme(preferences: Preferences, artworkAccent: String, content: @Composable () -> Unit) {
    val preset = when (preferences.theme) {
        "Sakura" -> Color(0xFFFFB5D0)
        "Cyberpunk" -> Color(0xFFD6A8FF)
        "Ocean", "Midnight blue", "Minimal", "Liquid Glass" -> Color(0xFF98D8FF)
        "Lavender" -> Color(0xFFC8B5FF)
        "Golden hour" -> Color(0xFFF5CE91)
        "Monochrome" -> Color(0xFFE0E3E8)
        else -> Color(0xFFA5E9CE)
    }
    val extracted = if (preferences.dynamic && artworkAccent.matches(Regex("#[0-9a-fA-F]{6}")))
        Color(android.graphics.Color.parseColor(artworkAccent)) else preset
    // Lift saturated/dark cover colours so text and controls stay legible on dark surfaces.
    val lifted = lerp(extracted, Color.White, .3f)
    val accent = if (lifted.luminance() < .32f) lerp(lifted, Color.White, .45f) else lifted
    val background = if (preferences.theme == "AMOLED") Color.Black else lerp(Color(0xFF080C10), accent, .025f)
    val glass = preferences.glass
    fun glassSurface(color: Color) = color.copy(alpha = if(glass) preferences.transparency else 1f)
    CompositionLocalProvider(LocalAppearance provides preferences) {
    MaterialTheme(colorScheme = darkColorScheme(
        surfaceTint = accent,
        surfaceDim = background, surfaceBright = lerp(background, accent, .14f),
        surfaceContainerLowest = glassSurface(background), surfaceContainerLow = glassSurface(lerp(background, accent, .025f)),
        surfaceContainer = glassSurface(lerp(background, accent, .045f)), surfaceContainerHigh = glassSurface(lerp(background, accent, .065f)),
        surfaceContainerHighest = glassSurface(lerp(background, accent, .09f)),
        secondaryContainer = lerp(background, accent, .18f), onSecondaryContainer = accent,
        primary = accent, onPrimary = Color(0xFF102018), secondary = accent,
        background = background, onBackground = Color(0xFFF0F2F4),
        surface = lerp(background, accent, .045f).copy(alpha=if(glass) preferences.transparency else 1f), onSurface = Color(0xFFF0F2F4),
        surfaceVariant = lerp(background, accent, .12f), onSurfaceVariant = Color(0xFFBBC6CB),
        primaryContainer = lerp(background, accent, .18f), onPrimaryContainer = accent,
        outline = lerp(background, accent, .35f)
    ), shapes=Shapes(small=androidx.compose.foundation.shape.RoundedCornerShape(preferences.radius.dp), medium=androidx.compose.foundation.shape.RoundedCornerShape(preferences.radius.dp),large=androidx.compose.foundation.shape.RoundedCornerShape(preferences.radius.dp)), content = content)
    }
}
