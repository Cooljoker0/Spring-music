package com.example.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color

private fun createDarkColorScheme(customTheme: CustomAppTheme) = darkColorScheme(
    primary = customTheme.primaryColor,
    onPrimary = if (customTheme.isGlass) Color.White else SpotifyPitchBlack,
    primaryContainer = customTheme.primaryColor.copy(alpha = 0.25f),
    onPrimaryContainer = customTheme.primaryBright,
    secondary = customTheme.accentColor,
    onSecondary = if (customTheme.isGlass) Color.White else SpotifyPitchBlack,
    secondaryContainer = if (customTheme.isGlass) Color(0x33FFFFFF) else DarkSurfaceElevated,
    onSecondaryContainer = customTheme.accentColor,
    tertiary = SpotifyLikedPurpleStart,
    onTertiary = TextPrimary,
    background = if (customTheme.isGlass) Color(0xFF0F0B18) else DarkBackground,
    onBackground = TextPrimary,
    surface = if (customTheme.isGlass) Color(0x24FFFFFF) else DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = if (customTheme.isGlass) Color(0x38FFFFFF) else DarkSurfaceElevated,
    onSurfaceVariant = TextSecondary,
    outline = if (customTheme.isGlass) Color(0x44FFFFFF) else DarkSurfaceBorder,
    error = AccentRose
)


@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val customTheme by AiThemeManager.currentTheme.collectAsState()
    val colorScheme = createDarkColorScheme(customTheme)
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = false
                    isAppearanceLightNavigationBars = false
                }
            }
        }
    }

    CompositionLocalProvider(LocalAppCustomTheme provides customTheme) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}

