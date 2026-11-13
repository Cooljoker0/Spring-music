package com.example.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.example.data.model.Song
import kotlin.math.abs

/**
 * FusionAmbience: Generates dynamic, track-reactive iridescent color trios and
 * frosted glass aurora gradients for the iOS Midnight Glassmorphism aesthetic.
 */
object FusionAmbience {

    val IridescentAuroraBrush: Brush = Brush.linearGradient(
        colors = listOf(GoldBright, AuroraCyan, AuroraViolet, AuroraMagenta)
    )

    val GoldMetallicBrush: Brush = Brush.linearGradient(
        colors = listOf(GoldBright, AuroraCyan, GoldBronze)
    )

    val CyanVioletBrush: Brush = Brush.horizontalGradient(
        colors = listOf(AuroraCyan, AuroraViolet)
    )

    val MagentaVioletBrush: Brush = Brush.linearGradient(
        colors = listOf(AuroraMagenta, AuroraViolet)
    )

    val GlassBorderBrush: Brush = Brush.verticalGradient(
        colors = listOf(
            FrostedGlassHighlight,
            FrostedGlassBorder,
            Color(0x0DEAD5A8)
        )
    )

    val GoldRimBorderBrush: Brush = Brush.verticalGradient(
        colors = listOf(
            Color(0x73F7E2AE),
            Color(0x30EAD5A8),
            Color(0x108B6EFF)
        )
    )

    val VelvetSurfaceBrush: Brush = Brush.linearGradient(
        colors = listOf(
            Color(0xE81C1E2D),
            FrostedGlassSurface,
            Color(0xF0101119)
        )
    )

    val RichDockBrush: Brush = Brush.verticalGradient(
        colors = listOf(
            Color(0xEB1A1C2A),
            Color(0xF70B0C12)
        )
    )

    private val PALETTE_AURORA = listOf(AuroraCyan, Color(0xFF231D15), MidnightCanvas)
    private val PALETTE_VIOLET = listOf(AuroraViolet, Color(0xFF20173B), MidnightCanvas)
    private val PALETTE_MAGENTA = listOf(AuroraMagenta, Color(0xFF331823), MidnightCanvas)
    private val PALETTE_SKY = listOf(Color(0xFF93C5FD), Color(0xFF141F38), MidnightCanvas)
    private val PALETTE_INDIGO = listOf(Color(0xFF818CF8), Color(0xFF191B3A), MidnightCanvas)
    private val PALETTE_TEAL = listOf(Color(0xFF5EEAD4), Color(0xFF102A2E), MidnightCanvas)
    private val PALETTE_ROSE = listOf(Color(0xFFF0ABAE), Color(0xFF361922), MidnightCanvas)
    private val PALETTE_EMERALD = listOf(IosEmerald, Color(0xFF102D22), MidnightCanvas)

    private val ALL_PALETTES = listOf(
        PALETTE_AURORA,
        PALETTE_VIOLET,
        PALETTE_MAGENTA,
        PALETTE_SKY,
        PALETTE_INDIGO,
        PALETTE_TEAL,
        PALETTE_ROSE,
        PALETTE_EMERALD
    )

    fun getColors(song: Song?): List<Color> {
        if (song == null) return PALETTE_AURORA

        val mood = song.mood.lowercase()
        return when {
            mood.contains("chill") || mood.contains("relax") || mood.contains("ambient") -> PALETTE_TEAL
            mood.contains("workout") || mood.contains("energy") || mood.contains("gym") -> PALETTE_MAGENTA
            mood.contains("focus") || mood.contains("study") || mood.contains("flow") -> PALETTE_SKY
            mood.contains("party") || mood.contains("dance") || mood.contains("edm") -> PALETTE_VIOLET
            mood.contains("romantic") || mood.contains("love") -> PALETTE_ROSE
            mood.contains("happy") || mood.contains("feel good") || mood.contains("acoustic") -> PALETTE_AURORA
            mood.contains("night") || mood.contains("drive") || mood.contains("sleep") -> PALETTE_INDIGO
            else -> {
                val key = (song.artist + song.title).trim()
                val index = abs(key.hashCode()) % ALL_PALETTES.size
                ALL_PALETTES[index]
            }
        }
    }

    fun playerAuraBrush(colors: List<Color>): Brush {
        return Brush.radialGradient(
            colors = listOf(
                colors[0].copy(alpha = 0.30f),
                colors[1].copy(alpha = 0.74f),
                MidnightCanvas
            ),
            radius = 1500f
        )
    }

    fun miniPlayerBrush(colors: List<Color>): Brush {
        return Brush.horizontalGradient(
            colors = listOf(
                colors[1].copy(alpha = 0.72f),
                Color(0xEB191B28),
                Color(0xF2101119)
            )
        )
    }

    fun heroCardBrush(colors: List<Color>): Brush {
        return Brush.linearGradient(
            colors = listOf(
                AuroraCyan.copy(alpha = 0.26f),
                AuroraViolet.copy(alpha = 0.32f),
                AuroraMagenta.copy(alpha = 0.26f)
            )
        )
    }
}
