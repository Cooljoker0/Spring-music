package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Midnight Liquid Glass — Strictly Monochromatic Palette (Zero Green / Zero Neon)
val MidnightBlack = Color(0xFF08090D)       // Level 0: Primary Backdrop
val DeepBlack = Color(0xFF0D0F14)           // Level 1: Elevated Sheets & Content Base
val Graphite = Color(0xFF171A21)            // Level 2: Clean Card & Row Surface
val GraphiteElevated = Color(0xFF1F232C)    // Level 3: Hover / Interactive Elevated Surface
val DarkGlass = Color(0x7A23262F)           // rgba(35, 38, 47, 0.48) Translucent Liquid Glass
val DarkGlassOpaque = Color(0xC2171A21)     // Higher-opacity Frosted Graphite for text contrast

val Silver = Color(0xFFAEB4C0)              // Secondary Text / Refined Metal Highlight
val SoftWhite = Color(0xFFF4F5F7)           // Primary Ink & Active Glass Accent
val MutedGrey = Color(0xFF858B98)           // Tertiary Metadata & Inactive Icons

// Liquid Glass Specular Highlights & Refractions
val WhiteHighlight = Color(0x33FFFFFF)      // rgba(255, 255, 255, 0.20) Primary Rim Reflection
val WhiteHighlightCrisp = Color(0x4DFFFFFF) // Top Specular Glint
val WhiteHighlightSoft = Color(0x14FFFFFF)  // Subtle Inner Separator & Hairline
val GlassShadowTint = Color(0x99000000)     // Natural Depth Shadow

// Restrained Monochrome-Compatible Status Destructive Tint (No Green Anywhere)
val DestructiveRose = Color(0xFFE07381)

// Canonical Mappings & Compatibility Aliases (Strictly Monochromatic)
val MidnightCanvas = MidnightBlack
val MidnightElevated = DeepBlack
val FrostedGlassSurface = Graphite
val FrostedGlassLight = GraphiteElevated
val FrostedGlassBorder = WhiteHighlightSoft
val FrostedGlassHighlight = WhiteHighlight

val CrispWhite = SoftWhite
val SlateSecondary = Silver
val SlateMuted = MutedGrey

// Legacy symbol aliases mapped strictly to monochromatic Midnight Liquid Glass tokens
val AuroraCyan = SoftWhite
val GoldBright = Silver
val GoldBronze = MutedGrey
val AuroraViolet = Silver
val AuroraMagenta = Silver
val IosEmerald = SoftWhite
val IosRed = DestructiveRose

val SpotifyGreen = SoftWhite
val SpotifyGreenBright = Silver
val SpotifyGreenDark = GraphiteElevated
val SpotifyPitchBlack = MidnightBlack
val SpotifyBlack = MidnightBlack
val SpotifySurface = DeepBlack
val SpotifyCard = Graphite
val SpotifyCardElevated = GraphiteElevated
val SpotifyMiniPlayer = DarkGlass
val SpotifyBorder = WhiteHighlight

val SpotifyTextPrimary = SoftWhite
val SpotifyTextSecondary = Silver
val SpotifyTextMuted = MutedGrey

val SpotifyLikedPurpleStart = GraphiteElevated
val SpotifyLikedPurpleEnd = Graphite

val DarkBackground = MidnightBlack
val DarkSurface = DeepBlack
val DarkSurfaceElevated = Graphite
val DarkSurfaceCard = Graphite
val DarkSurfaceBorder = WhiteHighlightSoft
val SpringEmerald = SoftWhite
val SpringEmeraldLight = Silver
val SpringEmeraldDark = MutedGrey
val SpringCyan = SoftWhite
val SpringCyanLight = Silver
val SpringIndigo = Silver
val SpringViolet = Silver
val TextPrimary = SoftWhite
val TextSecondary = Silver
val TextMuted = MutedGrey
val AccentRose = Silver
val AccentAmber = Silver
val GlassWhite = WhiteHighlightSoft
val GlassWhiteBorder = WhiteHighlight
