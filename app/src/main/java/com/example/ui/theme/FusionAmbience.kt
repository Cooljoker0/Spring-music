package com.example.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.example.data.model.Song

/**
 * FusionAmbience: Provides monochromatic Midnight Liquid Glass specular brushes,
 * edge refractions, and neutral depth gradients.
 */
object FusionAmbience {

    val SpecularRimBrush: Brush = Brush.verticalGradient(
        colors = listOf(
            WhiteHighlight,
            Color(0x16FFFFFF),
            Color(0x0AFFFFFF)
        )
    )

    val GlassBorderBrush: Brush = SpecularRimBrush

    val SubtleRimBrush: Brush = Brush.verticalGradient(
        colors = listOf(
            Color(0x26FFFFFF),
            Color(0x0EFFFFFF)
        )
    )

    val LiquidGlassFillBrush: Brush = Brush.verticalGradient(
        colors = listOf(
            Color(0x96272B35),
            DarkGlass,
            Color(0x8814171E)
        )
    )

    val FloatingDockBrush: Brush = Brush.verticalGradient(
        colors = listOf(
            Color(0xB821252E),
            Color(0xCC12141A)
        )
    )

    val VelvetSurfaceBrush: Brush = Brush.verticalGradient(
        colors = listOf(
            Graphite,
            DeepBlack
        )
    )

    val RichDockBrush: Brush = FloatingDockBrush

    fun themedAccentBrush(theme: CustomAppTheme): Brush = Brush.linearGradient(
        colors = listOf(theme.primaryColor, theme.primaryBright)
    )

    fun themedRimBorderBrush(theme: CustomAppTheme): Brush = Brush.verticalGradient(
        colors = listOf(
            theme.glassBorderColor,
            Color(0x12FFFFFF),
            Color(0x08FFFFFF)
        )
    )

    fun themedSurfaceBrush(theme: CustomAppTheme): Brush = Brush.verticalGradient(
        colors = listOf(
            theme.cardBackground,
            theme.elevatedBackground
        )
    )

    fun themedDockBrush(theme: CustomAppTheme): Brush = Brush.verticalGradient(
        colors = listOf(
            theme.glassSurfaceColor,
            Color(0xCC0D0F14)
        )
    )

    private val NEUTRAL_GLASS_PALETTE = listOf(SoftWhite, Graphite, MidnightBlack)

    fun getColors(song: Song?): List<Color> = NEUTRAL_GLASS_PALETTE

    fun playerAuraBrush(colors: List<Color>): Brush {
        return Brush.verticalGradient(
            colors = listOf(
                Color(0xCC08090D),
                Color(0xEA08090D),
                MidnightBlack
            )
        )
    }

    fun miniPlayerBrush(colors: List<Color>, theme: CustomAppTheme? = null): Brush {
        return Brush.verticalGradient(
            colors = listOf(
                theme?.glassSurfaceColor ?: DarkGlass,
                Color(0xBA12141B)
            )
        )
    }

    fun heroCardBrush(colors: List<Color>, theme: CustomAppTheme? = null): Brush {
        val surface = theme?.cardBackground ?: Graphite
        return Brush.verticalGradient(
            colors = listOf(
                surface,
                theme?.elevatedBackground ?: DeepBlack
            )
        )
    }
}
