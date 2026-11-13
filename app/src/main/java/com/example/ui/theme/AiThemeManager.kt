package com.example.ui.theme

import android.content.Context
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

enum class AppThemePreset(val id: String, val title: String, val subtitle: String) {
    SPOTIFY_DARK("spotify_dark", "Obsidian Gold", "Luxury smoked obsidian glass with champagne gold & amethyst"),
    APPLE_LIQUID_GLASS("apple_liquid_glass", "Apple Liquid Glass", "Translucent frosted glass with neon crimson rose"),
    CUSTOM_AI("custom_ai", "Custom AI Prompt", "Dynamic palette generated from your aesthetic prompt")
}

data class CustomAppTheme(
    val id: String = "spotify_dark",
    val name: String = "Obsidian Gold",
    val prompt: String = "Luxury Obsidian Champagne Gold & Velvet Amethyst",
    val primaryColor: Color = AuroraCyan,
    val primaryBright: Color = AuroraViolet,
    val accentColor: Color = AuroraMagenta,
    val cardBackground: Color = FrostedGlassSurface,
    val isGlass: Boolean = true,
    val glassBorderColor: Color = FrostedGlassBorder,
    val isCustom: Boolean = false
)

val LocalAppCustomTheme = compositionLocalOf { CustomAppTheme() }

object AiThemeManager {
    private const val PREFS_NAME = "spring_theme_prefs"
    private const val KEY_PRESET_ID = "theme_preset_id"
    private const val KEY_THEME_PROMPT = "theme_prompt"
    private const val KEY_PRIMARY_HEX = "theme_primary_hex"
    private const val KEY_ACCENT_HEX = "theme_accent_hex"
    private const val KEY_NAME = "theme_name"

    val SpotifyDarkTheme = CustomAppTheme(
        id = "spotify_dark",
        name = "Obsidian Gold",
        prompt = "Luxury Obsidian Champagne Gold & Velvet Amethyst",
        primaryColor = AuroraCyan,
        primaryBright = AuroraViolet,
        accentColor = AuroraMagenta,
        cardBackground = FrostedGlassSurface,
        isGlass = true,
        glassBorderColor = FrostedGlassBorder,
        isCustom = false
    )

    val AppleLiquidGlassTheme = CustomAppTheme(
        id = "apple_liquid_glass",
        name = "Apple Liquid Glass",
        prompt = "Apple Music translucent liquid glass with neon crimson-rose",
        primaryColor = Color(0xFFFA2D48), // Apple Music Iconic Crimson Rose
        primaryBright = Color(0xFFFF375F),
        accentColor = Color(0xFFFC3C44),
        cardBackground = Color(0x28FFFFFF), // Frosted glass card fill
        isGlass = true,
        glassBorderColor = Color(0x44FFFFFF), // Reflective glass border
        isCustom = false
    )

    private val _currentTheme = MutableStateFlow(SpotifyDarkTheme)
    val currentTheme: StateFlow<CustomAppTheme> = _currentTheme.asStateFlow()

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val presetId = prefs.getString(KEY_PRESET_ID, AppThemePreset.SPOTIFY_DARK.id)

        when (presetId) {
            AppThemePreset.APPLE_LIQUID_GLASS.id -> {
                _currentTheme.value = AppleLiquidGlassTheme
            }
            AppThemePreset.CUSTOM_AI.id -> {
                val prompt = prefs.getString(KEY_THEME_PROMPT, null)
                val primaryHex = prefs.getString(KEY_PRIMARY_HEX, null)
                val accentHex = prefs.getString(KEY_ACCENT_HEX, null)
                val name = prefs.getString(KEY_NAME, null)

                if (!primaryHex.isNullOrBlank() && !accentHex.isNullOrBlank()) {
                    val primary = parseColorSafe(primaryHex, SpotifyGreen)
                    val accent = parseColorSafe(accentHex, SpotifyGreenBright)
                    _currentTheme.value = CustomAppTheme(
                        id = "custom_ai",
                        name = name ?: "Custom AI Theme",
                        prompt = prompt ?: "Custom prompt",
                        primaryColor = primary,
                        primaryBright = accent,
                        accentColor = accent,
                        cardBackground = SpotifyCard,
                        isGlass = false,
                        isCustom = true
                    )
                } else {
                    _currentTheme.value = SpotifyDarkTheme
                }
            }
            else -> {
                _currentTheme.value = SpotifyDarkTheme
            }
        }
    }

    fun setPresetTheme(context: Context, preset: AppThemePreset) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        when (preset) {
            AppThemePreset.SPOTIFY_DARK -> {
                _currentTheme.value = SpotifyDarkTheme
                prefs.edit().putString(KEY_PRESET_ID, preset.id).apply()
            }
            AppThemePreset.APPLE_LIQUID_GLASS -> {
                _currentTheme.value = AppleLiquidGlassTheme
                prefs.edit().putString(KEY_PRESET_ID, preset.id).apply()
            }
            AppThemePreset.CUSTOM_AI -> {
                // If custom exists in prefs, load it; otherwise keep current
                val primaryHex = prefs.getString(KEY_PRIMARY_HEX, null)
                if (!primaryHex.isNullOrBlank()) {
                    init(context)
                }
            }
        }
    }

    fun applyTheme(context: Context, theme: CustomAppTheme) {
        _currentTheme.value = theme
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_PRESET_ID, if (theme.id == "apple_liquid_glass") "apple_liquid_glass" else "custom_ai")
            .putString(KEY_THEME_PROMPT, theme.prompt)
            .putString(KEY_PRIMARY_HEX, colorToHex(theme.primaryColor))
            .putString(KEY_ACCENT_HEX, colorToHex(theme.accentColor))
            .putString(KEY_NAME, theme.name)
            .apply()
    }

    fun resetToDefault(context: Context) {
        _currentTheme.value = SpotifyDarkTheme
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
    }

    /**
     * Interprets user prompt with intelligent color mapping and semantic aesthetics
     */
    fun generateThemeFromPrompt(prompt: String): CustomAppTheme {
        val p = prompt.trim().lowercase(Locale.ROOT)

        // Check for Apple Liquid Glass prompt
        if (p.contains("apple") || p.contains("liquid glass") || p.contains("ios glass")) {
            return AppleLiquidGlassTheme
        }

        // Check for direct hex in prompt
        val hexRegex = Regex("#([A-Fa-f0-9]{6})")
        val hexMatches = hexRegex.findAll(prompt).map { it.value }.toList()
        if (hexMatches.isNotEmpty()) {
            val primary = parseColorSafe(hexMatches[0], SpotifyGreen)
            val accent = if (hexMatches.size > 1) parseColorSafe(hexMatches[1], primary) else primary
            return CustomAppTheme(
                id = "custom_ai",
                name = "Custom Hex Theme",
                prompt = prompt,
                primaryColor = primary,
                primaryBright = accent,
                accentColor = accent,
                isGlass = p.contains("glass"),
                glassBorderColor = if (p.contains("glass")) Color(0x40FFFFFF) else Color.Transparent,
                isCustom = true
            )
        }

        return when {
            p.contains("cyberpunk") || p.contains("neon") || (p.contains("cyan") && p.contains("pink")) -> {
                CustomAppTheme(
                    id = "custom_ai",
                    name = "Cyberpunk Neon",
                    prompt = prompt,
                    primaryColor = Color(0xFF00F0FF), // Electric Cyan
                    primaryBright = Color(0xFF38E5FF),
                    accentColor = Color(0xFFFF007F), // Neon Pink
                    cardBackground = Color(0xFF1E182A),
                    isGlass = false,
                    isCustom = true
                )
            }
            p.contains("sunset") || p.contains("synthwave") || p.contains("orange") || p.contains("retro") -> {
                CustomAppTheme(
                    id = "custom_ai",
                    name = "Sunset Synthwave",
                    prompt = prompt,
                    primaryColor = Color(0xFFFF6B35), // Vibrant Sunset Orange
                    primaryBright = Color(0xFFFF8E3C),
                    accentColor = Color(0xFFFF006E), // Hot Magenta
                    cardBackground = Color(0xFF24161C),
                    isGlass = false,
                    isCustom = true
                )
            }
            p.contains("ocean") || p.contains("blue") || p.contains("midnight") || p.contains("sea") || p.contains("aqua") -> {
                CustomAppTheme(
                    id = "custom_ai",
                    name = "Midnight Ocean",
                    prompt = prompt,
                    primaryColor = Color(0xFF38BDF8), // Sky / Aqua Blue
                    primaryBright = Color(0xFF67E8F9),
                    accentColor = Color(0xFF818CF8), // Electric Indigo
                    cardBackground = Color(0xFF131D2E),
                    isGlass = false,
                    isCustom = true
                )
            }
            p.contains("purple") || p.contains("violet") || p.contains("lavender") || p.contains("dream") -> {
                CustomAppTheme(
                    id = "custom_ai",
                    name = "Deep Violet",
                    prompt = prompt,
                    primaryColor = Color(0xFFA855F7), // Vivid Violet
                    primaryBright = Color(0xFFC084FC),
                    accentColor = Color(0xFFE879F9), // Soft Orchid
                    cardBackground = Color(0xFF1E172B),
                    isGlass = false,
                    isCustom = true
                )
            }
            p.contains("emerald") || p.contains("forest") || p.contains("mint") || p.contains("nature") || p.contains("jade") -> {
                CustomAppTheme(
                    id = "custom_ai",
                    name = "Emerald Forest",
                    prompt = prompt,
                    primaryColor = Color(0xFF10B981), // Rich Mint Emerald
                    primaryBright = Color(0xFF34D399),
                    accentColor = Color(0xFF2DD4BF), // Bright Teal
                    cardBackground = Color(0xFF14241E),
                    isGlass = false,
                    isCustom = true
                )
            }
            p.contains("gold") || p.contains("luxury") || p.contains("yellow") || p.contains("champagne") || p.contains("royal") -> {
                CustomAppTheme(
                    id = "custom_ai",
                    name = "Royal Gold",
                    prompt = prompt,
                    primaryColor = Color(0xFFFBBF24), // Amber Gold
                    primaryBright = Color(0xFFFCD34D),
                    accentColor = Color(0xFFF472B6), // Rose Accent
                    cardBackground = Color(0xFF251E14),
                    isGlass = false,
                    isCustom = true
                )
            }
            p.contains("pink") || p.contains("rose") || p.contains("cherry") || p.contains("pastel") || p.contains("barbie") -> {
                CustomAppTheme(
                    id = "custom_ai",
                    name = "Blossom Pink",
                    prompt = prompt,
                    primaryColor = Color(0xFFF43F5E), // Rose Pink
                    primaryBright = Color(0xFFFB7185),
                    accentColor = Color(0xFFFF70A6),
                    cardBackground = Color(0xFF27171C),
                    isGlass = false,
                    isCustom = true
                )
            }
            p.contains("fire") || p.contains("red") || p.contains("crimson") || p.contains("flame") -> {
                CustomAppTheme(
                    id = "custom_ai",
                    name = "Crimson Flame",
                    prompt = prompt,
                    primaryColor = Color(0xFFEF4444), // Crimson Red
                    primaryBright = Color(0xFFF87171),
                    accentColor = Color(0xFFF97316), // Flame Orange
                    cardBackground = Color(0xFF241515),
                    isGlass = false,
                    isCustom = true
                )
            }
            p.contains("matrix") || p.contains("code") || p.contains("terminal") || p.contains("hacker") -> {
                CustomAppTheme(
                    id = "custom_ai",
                    name = "Matrix Green",
                    prompt = prompt,
                    primaryColor = Color(0xFF00FF66), // Phosphor Matrix Green
                    primaryBright = Color(0xFF66FF99),
                    accentColor = Color(0xFF00CC44),
                    cardBackground = Color(0xFF0F1A12),
                    isGlass = false,
                    isCustom = true
                )
            }
            else -> {
                val hash = prompt.hashCode()
                val hue = (Math.abs(hash) % 360).toFloat()
                val hsvColor = android.graphics.Color.HSVToColor(floatArrayOf(hue, 0.85f, 0.95f))
                val accentHsvColor = android.graphics.Color.HSVToColor(floatArrayOf((hue + 40f) % 360f, 0.75f, 0.95f))

                CustomAppTheme(
                    id = "custom_ai",
                    name = "AI Aesthetic: ${prompt.take(16).replaceFirstChar { it.uppercase() }}",
                    prompt = prompt,
                    primaryColor = Color(hsvColor),
                    primaryBright = Color(accentHsvColor),
                    accentColor = Color(accentHsvColor),
                    cardBackground = SpotifyCard,
                    isGlass = p.contains("glass"),
                    glassBorderColor = if (p.contains("glass")) Color(0x40FFFFFF) else Color.Transparent,
                    isCustom = true
                )
            }
        }
    }

    fun parseColorSafe(hex: String, fallback: Color): Color {
        return try {
            val cleanHex = hex.trim().removePrefix("#")
            val colorLong = cleanHex.toLong(16)
            if (cleanHex.length == 6) {
                Color(0xFF000000 or colorLong)
            } else if (cleanHex.length == 8) {
                Color(colorLong)
            } else {
                fallback
            }
        } catch (_: Exception) {
            fallback
        }
    }

    fun colorToHex(color: Color): String {
        val red = (color.red * 255).toInt().coerceIn(0, 255)
        val green = (color.green * 255).toInt().coerceIn(0, 255)
        val blue = (color.blue * 255).toInt().coerceIn(0, 255)
        return String.format("#%02X%02X%02X", red, green, blue)
    }
}
