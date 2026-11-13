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
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private fun createDarkColorScheme(customTheme: CustomAppTheme) = darkColorScheme(
    primary = customTheme.primaryColor,
    onPrimary = MidnightCanvas,
    primaryContainer = customTheme.primaryColor.copy(alpha = 0.22f),
    onPrimaryContainer = GoldBright,
    secondary = customTheme.accentColor,
    onSecondary = CrispWhite,
    secondaryContainer = FrostedGlassSurface,
    onSecondaryContainer = customTheme.accentColor,
    tertiary = AuroraViolet,
    onTertiary = CrispWhite,
    background = MidnightCanvas,
    onBackground = CrispWhite,
    surface = FrostedGlassSurface,
    onSurface = CrispWhite,
    surfaceVariant = FrostedGlassLight,
    onSurfaceVariant = SlateSecondary,
    surfaceContainerLowest = MidnightCanvas,
    surfaceContainerLow = MidnightElevated,
    surfaceContainer = FrostedGlassSurface,
    surfaceContainerHigh = FrostedGlassLight,
    surfaceContainerHighest = Color(0xFF282B3D),
    outline = FrostedGlassBorder,
    outlineVariant = Color(0x1EEAD5A8),
    error = IosRed
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
