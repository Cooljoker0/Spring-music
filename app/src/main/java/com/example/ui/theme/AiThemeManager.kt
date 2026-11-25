package com.example.ui.theme

import android.content.Context
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

enum class AppThemePreset(val id: String, val title: String, val subtitle: String) {
    NOCTURNE_JADE("nocturne_jade", "Midnight Glass", "Pure midnight black with translucent graphite & silver highlights"),
    APPLE_LIQUID_GLASS("apple_liquid_glass", "Frosted Silver", "Luminous frosted graphite glass with soft white refraction"),
    OBSIDIAN_GOLD("obsidian_gold", "Obsidian Matte", "Deep matte charcoal with pewter silver accents"),
    ARCTIC_SAPPHIRE("arctic_sapphire", "Crystal Mist", "Cool abyssal slate with crisp ice-silver glass edges"),
    CUSTOM_AI("custom_ai", "Custom Glass Finish", "Monochrome Liquid Glass intensity tailored to your preference")
}

data class CustomAppTheme(
    val id: String = AppThemePreset.NOCTURNE_JADE.id,
    val name: String = "Midnight Glass",
    val prompt: String = "Pure midnight black with translucent graphite & silver highlights",
    val primaryColor: Color = SoftWhite,
    val primaryBright: Color = Silver,
    val accentColor: Color = Silver,
    val canvasBackground: Color = MidnightBlack,
    val elevatedBackground: Color = DeepBlack,
    val cardBackground: Color = Graphite,
    val glassSurfaceColor: Color = DarkGlass,
    val isGlass: Boolean = true,
    val glassBorderColor: Color = WhiteHighlight,
    val isCustom: Boolean = false
)

val LocalAppCustomTheme = compositionLocalOf { CustomAppTheme() }

fun onAccentColor(accent: Color, darkFallback: Color = MidnightBlack): Color {
    return if (accent.luminance() > 0.44f) darkFallback else SoftWhite
}

object AiThemeManager {
    private const val PREFS_NAME = "spring_theme_prefs"
    private const val KEY_PRESET_ID = "theme_preset_id"
    private const val KEY_THEME_PROMPT = "theme_prompt"
    private const val KEY_PRIMARY_HEX = "theme_primary_hex"
    private const val KEY_ACCENT_HEX = "theme_accent_hex"
    private const val KEY_NAME = "theme_name"

    val MidnightGlassTheme = CustomAppTheme(
        id = AppThemePreset.NOCTURNE_JADE.id,
        name = "Midnight Glass",
        prompt = "Pure midnight black with translucent graphite & silver highlights",
        primaryColor = SoftWhite,
        primaryBright = Silver,
        accentColor = MutedGrey,
        canvasBackground = MidnightBlack,
        elevatedBackground = DeepBlack,
        cardBackground = Graphite,
        glassSurfaceColor = DarkGlass,
        isGlass = true,
        glassBorderColor = WhiteHighlight,
        isCustom = false
    )

    val FrostedSilverTheme = CustomAppTheme(
        id = AppThemePreset.APPLE_LIQUID_GLASS.id,
        name = "Frosted Silver",
        prompt = "Luminous frosted graphite glass with crisp white edge reflections",
        primaryColor = SoftWhite,
        primaryBright = Color(0xFFC8CDD8),
        accentColor = Silver,
        canvasBackground = Color(0xFF0A0C10),
        elevatedBackground = Color(0xFF101319),
        cardBackground = Color(0xFF191D26),
        glassSurfaceColor = Color(0x85282C38),
        isGlass = true,
        glassBorderColor = Color(0x3BFFFFFF),
        isCustom = false
    )

    val ObsidianMatteTheme = CustomAppTheme(
        id = AppThemePreset.OBSIDIAN_GOLD.id,
        name = "Obsidian Matte",
        prompt = "Deep matte obsidian black with muted pewter contrast",
        primaryColor = Color(0xFFE8EAEE),
        primaryBright = Color(0xFFA0A6B2),
        accentColor = MutedGrey,
        canvasBackground = Color(0xFF06070A),
        elevatedBackground = Color(0xFF0C0E13),
        cardBackground = Color(0xFF14171E),
        glassSurfaceColor = Color(0x751E212A),
        isGlass = true,
        glassBorderColor = Color(0x28FFFFFF),
        isCustom = false
    )

    val CrystalMistTheme = CustomAppTheme(
        id = AppThemePreset.ARCTIC_SAPPHIRE.id,
        name = "Crystal Mist",
        prompt = "Abyssal charcoal with translucent cool-silver liquid rim highlights",
        primaryColor = SoftWhite,
        primaryBright = Color(0xFFB8C0CE),
        accentColor = Silver,
        canvasBackground = Color(0xFF090B11),
        elevatedBackground = Color(0xFF0F1219),
        cardBackground = Color(0xFF181C25),
        glassSurfaceColor = Color(0x80252A36),
        isGlass = true,
        glassBorderColor = Color(0x36FFFFFF),
        isCustom = false
    )

    // Backward-compatible property names
    val NocturneJadeTheme: CustomAppTheme get() = MidnightGlassTheme
    val AppleLiquidGlassTheme: CustomAppTheme get() = FrostedSilverTheme
    val ObsidianGoldTheme: CustomAppTheme get() = ObsidianMatteTheme
    val ArcticSapphireTheme: CustomAppTheme get() = CrystalMistTheme
    val SpotifyDarkTheme: CustomAppTheme get() = MidnightGlassTheme

    val CuratedThemes: List<CustomAppTheme> = listOf(
        MidnightGlassTheme,
        FrostedSilverTheme,
        ObsidianMatteTheme,
        CrystalMistTheme
    )

    private val _currentTheme = MutableStateFlow(MidnightGlassTheme)
    val currentTheme: StateFlow<CustomAppTheme> = _currentTheme.asStateFlow()

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val presetId = prefs.getString(KEY_PRESET_ID, AppThemePreset.NOCTURNE_JADE.id)

        when (presetId) {
            AppThemePreset.APPLE_LIQUID_GLASS.id -> _currentTheme.value = FrostedSilverTheme
            AppThemePreset.OBSIDIAN_GOLD.id -> _currentTheme.value = ObsidianMatteTheme
            AppThemePreset.ARCTIC_SAPPHIRE.id -> _currentTheme.value = CrystalMistTheme
            AppThemePreset.CUSTOM_AI.id -> {
                val prompt = prefs.getString(KEY_THEME_PROMPT, null)
                val name = prefs.getString(KEY_NAME, null)
                val primaryHex = prefs.getString(KEY_PRIMARY_HEX, null)
                val accentHex = prefs.getString(KEY_ACCENT_HEX, null)

                if (!primaryHex.isNullOrBlank()) {
                    // Enforce monochrome luminance normalization so restored prefs never leak non-monochrome colors
                    val primary = toMonochromeTone(parseColorSafe(primaryHex, SoftWhite), minBrightness = 0.88f)
                    val accent = toMonochromeTone(parseColorSafe(accentHex ?: "", Silver), minBrightness = 0.68f)
                    _currentTheme.value = MidnightGlassTheme.copy(
                        id = AppThemePreset.CUSTOM_AI.id,
                        name = name?.takeIf { it.isNotBlank() } ?: "Custom Glass",
                        prompt = prompt ?: "Monochrome Liquid Glass",
                        primaryColor = primary,
                        primaryBright = accent,
                        accentColor = accent,
                        isCustom = true
                    )
                } else {
                    _currentTheme.value = MidnightGlassTheme
                }
            }
            else -> _currentTheme.value = MidnightGlassTheme
        }
    }

    fun setPresetTheme(context: Context, preset: AppThemePreset) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        when (preset) {
            AppThemePreset.NOCTURNE_JADE -> {
                _currentTheme.value = MidnightGlassTheme
                prefs.edit().putString(KEY_PRESET_ID, preset.id).apply()
            }
            AppThemePreset.APPLE_LIQUID_GLASS -> {
                _currentTheme.value = FrostedSilverTheme
                prefs.edit().putString(KEY_PRESET_ID, preset.id).apply()
            }
            AppThemePreset.OBSIDIAN_GOLD -> {
                _currentTheme.value = ObsidianMatteTheme
                prefs.edit().putString(KEY_PRESET_ID, preset.id).apply()
            }
            AppThemePreset.ARCTIC_SAPPHIRE -> {
                _currentTheme.value = CrystalMistTheme
                prefs.edit().putString(KEY_PRESET_ID, preset.id).apply()
            }
            AppThemePreset.CUSTOM_AI -> {
                init(context)
            }
        }
    }

    fun applyTheme(context: Context, theme: CustomAppTheme) {
        val safePrimary = toMonochromeTone(theme.primaryColor, minBrightness = 0.88f)
        val safeBright = toMonochromeTone(theme.primaryBright, minBrightness = 0.68f)
        val normalized = theme.copy(
            primaryColor = safePrimary,
            primaryBright = safeBright,
            accentColor = safeBright,
            isGlass = true,
            glassBorderColor = WhiteHighlight
        )
        _currentTheme.value = normalized
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val storedId = when (normalized.id) {
            AppThemePreset.NOCTURNE_JADE.id,
            AppThemePreset.APPLE_LIQUID_GLASS.id,
            AppThemePreset.OBSIDIAN_GOLD.id,
            AppThemePreset.ARCTIC_SAPPHIRE.id -> normalized.id
            else -> AppThemePreset.CUSTOM_AI.id
        }
        prefs.edit()
            .putString(KEY_PRESET_ID, storedId)
            .putString(KEY_THEME_PROMPT, normalized.prompt)
            .putString(KEY_PRIMARY_HEX, colorToHex(normalized.primaryColor))
            .putString(KEY_ACCENT_HEX, colorToHex(normalized.accentColor))
            .putString(KEY_NAME, normalized.name)
            .apply()
    }

    fun resetToDefault(context: Context) {
        _currentTheme.value = MidnightGlassTheme
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
    }

    fun generateThemeFromPrompt(prompt: String): CustomAppTheme {
        val p = prompt.trim().lowercase(Locale.ROOT)
        return when {
            p.contains("frosted") || p.contains("silver") || p.contains("apple") || p.contains("liquid glass") ->
                FrostedSilverTheme.copy(id = AppThemePreset.CUSTOM_AI.id, name = "Frosted Silver", prompt = prompt, isCustom = true)
            p.contains("obsidian") || p.contains("matte") || p.contains("stealth") || p.contains("black") ->
                ObsidianMatteTheme.copy(id = AppThemePreset.CUSTOM_AI.id, name = "Obsidian Matte", prompt = prompt, isCustom = true)
            p.contains("crystal") || p.contains("mist") || p.contains("ice") || p.contains("chrome") ->
                CrystalMistTheme.copy(id = AppThemePreset.CUSTOM_AI.id, name = "Crystal Mist", prompt = prompt, isCustom = true)
            else -> {
                val displayName = prompt.trim().take(22).ifBlank { "Midnight Glass" }
                    .replaceFirstChar { it.uppercase() }
                MidnightGlassTheme.copy(
                    id = AppThemePreset.CUSTOM_AI.id,
                    name = displayName,
                    prompt = prompt,
                    isCustom = true
                )
            }
        }
    }

    private fun toMonochromeTone(color: Color, minBrightness: Float): Color {
        val lum = (0.299f * color.red + 0.587f * color.green + 0.114f * color.blue)
            .coerceIn(minBrightness, 0.96f)
        return Color(lum, lum, (lum + 0.015f).coerceAtMost(0.98f), 1f)
    }

    fun parseColorSafe(hex: String, fallback: Color): Color {
        return try {
            val cleanHex = hex.trim().removePrefix("#")
            val colorLong = cleanHex.toLong(16)
            when (cleanHex.length) {
                6 -> Color(0xFF000000 or colorLong)
                8 -> Color(colorLong)
                else -> fallback
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
