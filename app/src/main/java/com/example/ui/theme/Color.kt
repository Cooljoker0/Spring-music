package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Luxury Obsidian Champagne & Imperial Velvet Palette
val MidnightCanvas = Color(0xFF06060A) // Deepest Velvet Obsidian (Level 0)
val MidnightElevated = Color(0xFF101119) // Smoked Charcoal (Level 1)
val FrostedGlassSurface = Color(0xD9171924) // Rich Satin Obsidian Glass (Level 2, 85%)
val FrostedGlassLight = Color(0xE6222536) // Elevated Satin Glass (Level 3)
val FrostedGlassBorder = Color(0x36EAD5A8) // Warm Brushed Champagne Rim
val FrostedGlassHighlight = Color(0x66F7E2AE) // Specular Sunlit Gold Rim

// Signature Luxury Accents
val AuroraCyan = Color(0xFFE8C57B) // Brushed Champagne Gold (Primary Luxury Accent)
val GoldBright = Color(0xFFF7E2AE) // Sunlit Champagne Highlight
val GoldBronze = Color(0xFFB5893D) // Burnished Antique Bronze
val AuroraViolet = Color(0xFF8B6EFF) // Imperial Amethyst Indigo
val AuroraMagenta = Color(0xFFE07A96) // Velvet Rose-Copper
val IosEmerald = Color(0xFF34D399) // Emerald Jewel
val IosRed = Color(0xFFFB7185) // Velvet Ruby

// Typography Colors (WCAG 2.2 AA >= 4.5:1 on Obsidian & Satin Surfaces)
val CrispWhite = Color(0xFFFBF8F2) // Warm Ivory Pearl
val SlateSecondary = Color(0xFFC4BFB6) // Warm Platinum Silver (~9.1:1 on card)
val SlateMuted = Color(0xFF97928A) // Refined Muted Platinum (~5.2:1 on card)

// Harmonized App Theme Aliases (ensures seamless cohesion across all components)
val SpotifyGreen = AuroraCyan
val SpotifyGreenBright = Color(0xFFF3D99B)
val SpotifyGreenDark = Color(0xFFB8924A)

val SpotifyPitchBlack = MidnightCanvas
val SpotifyBlack = MidnightCanvas
val SpotifySurface = MidnightElevated
val SpotifyCard = FrostedGlassSurface
val SpotifyCardElevated = Color(0xDB1C1D28)
val SpotifyMiniPlayer = FrostedGlassSurface
val SpotifyBorder = FrostedGlassBorder

val SpotifyTextPrimary = CrispWhite
val SpotifyTextSecondary = SlateSecondary
val SpotifyTextMuted = SlateMuted

val SpotifyLikedPurpleStart = AuroraViolet
val SpotifyLikedPurpleEnd = AuroraMagenta

// Backward Compatibility Aliases
val DarkBackground = MidnightCanvas
val DarkSurface = MidnightElevated
val DarkSurfaceElevated = FrostedGlassSurface
val DarkSurfaceCard = FrostedGlassSurface
val DarkSurfaceBorder = FrostedGlassBorder
val SpringEmerald = AuroraCyan
val SpringEmeraldLight = Color(0xFFF3D99B)
val SpringEmeraldDark = SpotifyGreenDark
val SpringCyan = AuroraCyan
val SpringCyanLight = Color(0xFFF5DFB0)
val SpringIndigo = AuroraViolet
val SpringViolet = AuroraMagenta
val TextPrimary = CrispWhite
val TextSecondary = SlateSecondary
val TextMuted = SlateMuted
val AccentRose = AuroraMagenta
val AccentAmber = Color(0xFFF5C469)
val GlassWhite = Color(0x1AF5E6C8)
val GlassWhiteBorder = FrostedGlassBorder
