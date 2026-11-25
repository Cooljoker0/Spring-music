package com.example.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private fun createDarkColorScheme(customTheme: CustomAppTheme): androidx.compose.material3.ColorScheme {
    val highSurface = lerp(customTheme.cardBackground, Color.White, 0.05f)
    val highestSurface = lerp(customTheme.cardBackground, Color.White, 0.10f)
    return darkColorScheme(
        primary = customTheme.primaryColor,
        onPrimary = onAccentColor(customTheme.primaryColor, customTheme.canvasBackground),
        primaryContainer = WhiteHighlightSoft,
        onPrimaryContainer = SoftWhite,
        secondary = customTheme.primaryBright,
        onSecondary = onAccentColor(customTheme.primaryBright, customTheme.canvasBackground),
        secondaryContainer = customTheme.cardBackground,
        onSecondaryContainer = SoftWhite,
        tertiary = customTheme.accentColor,
        onTertiary = onAccentColor(customTheme.accentColor, customTheme.canvasBackground),
        background = customTheme.canvasBackground,
        onBackground = SoftWhite,
        surface = customTheme.cardBackground,
        onSurface = SoftWhite,
        surfaceVariant = highSurface,
        onSurfaceVariant = Silver,
        surfaceContainerLowest = customTheme.canvasBackground,
        surfaceContainerLow = customTheme.elevatedBackground,
        surfaceContainer = customTheme.cardBackground,
        surfaceContainerHigh = highSurface,
        surfaceContainerHighest = highestSurface,
        outline = customTheme.glassBorderColor,
        outlineVariant = WhiteHighlightSoft,
        error = DestructiveRose
    )
}

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
