package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

@Immutable
data class WatchPilotAccentColors(
    val isDark: Boolean,
    val goldAccent: Color,
    val goldSolidBg: Color,
    val onGoldSolid: Color,
    val goldChipContainer: Color,
    val goldChipBorder: Color,
    val cyanAccent: Color,
    val cyanSolidBg: Color,
    val onCyanSolid: Color,
    val cyanChipContainer: Color,
    val cyanChipBorder: Color,
    val greenAccent: Color,
    val greenSolidBg: Color,
    val onGreenSolid: Color,
    val greenChipContainer: Color,
    val orangeAccent: Color,
    val redAccent: Color
)

private val DarkAccentColors = WatchPilotAccentColors(
    isDark = true,
    goldAccent = CinemaGold,
    goldSolidBg = CinemaGold,
    onGoldSolid = DarkBackground,
    goldChipContainer = CinemaGold.copy(alpha = 0.20f),
    goldChipBorder = CinemaGold.copy(alpha = 0.48f),
    cyanAccent = CinemaCyan,
    cyanSolidBg = CinemaCyan,
    onCyanSolid = DarkBackground,
    cyanChipContainer = CinemaCyan.copy(alpha = 0.20f),
    cyanChipBorder = CinemaCyan.copy(alpha = 0.48f),
    greenAccent = CinemaGreen,
    greenSolidBg = CinemaGreen,
    onGreenSolid = DarkBackground,
    greenChipContainer = CinemaGreen.copy(alpha = 0.22f),
    orangeAccent = Color(0xFFF97316),
    redAccent = CinemaRed
)

private val LightAccentColors = WatchPilotAccentColors(
    isDark = false,
    goldAccent = CinemaGoldOnLight,
    goldSolidBg = CinemaGoldSolidLight,
    onGoldSolid = CinemaGoldInkLight,
    goldChipContainer = CinemaGoldTintLight,
    goldChipBorder = CinemaGoldBorderLight.copy(alpha = 0.65f),
    cyanAccent = CinemaCyanOnLight,
    cyanSolidBg = CinemaCyanSolidLight,
    onCyanSolid = LightSurface,
    cyanChipContainer = CinemaCyanTintLight,
    cyanChipBorder = CinemaCyanBorderLight.copy(alpha = 0.75f),
    greenAccent = CinemaGreenOnLight,
    greenSolidBg = CinemaGreenSolidLight,
    onGreenSolid = LightSurface,
    greenChipContainer = CinemaGreenTintLight,
    orangeAccent = CinemaAmberOnLight,
    redAccent = CinemaRedOnLight
)

val LocalWatchPilotColors = staticCompositionLocalOf { DarkAccentColors }

object WatchPilotColors {
    val current: WatchPilotAccentColors
        @Composable get() = LocalWatchPilotColors.current
}

private val DarkColorScheme = darkColorScheme(
    primary = CinemaGold,
    onPrimary = DarkBackground,
    primaryContainer = CinemaGoldDark,
    onPrimaryContainer = CinemaGoldLight,
    secondary = CinemaCyan,
    onSecondary = DarkBackground,
    secondaryContainer = DarkSurfaceVariant,
    onSecondaryContainer = CinemaCyan,
    tertiary = CinemaRed,
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    surfaceContainer = DarkSurfaceVariant,
    surfaceContainerHigh = DarkSurfaceHighlight
)

private val LightColorScheme = lightColorScheme(
    primary = CinemaGoldOnLight,
    onPrimary = LightSurface,
    primaryContainer = CinemaGoldTintLight,
    onPrimaryContainer = CinemaGoldOnLight,
    secondary = CinemaCyanSolidLight,
    onSecondary = LightSurface,
    secondaryContainer = CinemaCyanTintLight,
    onSecondaryContainer = CinemaCyanOnLight,
    tertiary = CinemaRedOnLight,
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    surfaceContainer = Color(0xFFF1F5F9),
    surfaceContainerHigh = Color(0xFFE2E8F0),
    outline = Color(0xFF94A3B8),
    outlineVariant = Color(0xFFCBD5E1)
)

@Composable
fun MovieverseTheme(
    darkTheme: Boolean = true, // Default to sleek cinematic dark theme for immersion
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val accentColors = if (darkTheme) DarkAccentColors else LightAccentColors
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = colorScheme.background.toArgb()
                window.navigationBarColor = colorScheme.background.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
                WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    CompositionLocalProvider(LocalWatchPilotColors provides accentColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}

@Composable
fun CineTrackTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) = MovieverseTheme(darkTheme = darkTheme, content = content)

@Composable
fun WatchPilotTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) = MovieverseTheme(darkTheme = darkTheme, content = content)

