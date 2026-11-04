package com.example.data.network

import com.example.data.model.AudioQuality
import com.example.data.model.MoodCategory
import com.example.data.model.Playlist
import com.example.data.model.Song

object PresetCatalog {
    val MOOD_CATEGORIES = listOf(
        MoodCategory("focus", "Focus & Flow", "Binaural soundscapes & deep ambient tones", "Focus Lofi Study", "🧠", 0xFF059669, 0xFF047857),
        MoodCategory("chill", "Chill Beats", "Relaxing melodies to unwind and de-stress", "Chill Lounge Relaxing", "☕", 0xFF0284C7, 0xFF0369A1),
        MoodCategory("feel_good", "Feel Good", "Upbeat, sunny rhythms and positive vibrations", "Summer Happy Pop", "✨", 0xFFF59E0B, 0xFFD97706),
        MoodCategory("ambient", "Ambient Space", "Floating synthesizers and atmospheric calm", "Ambient Meditative Peace", "🌌", 0xFF8B5CF6, 0xFF7C3AED),
        MoodCategory("acoustic", "Acoustic Warmth", "Gentle guitars, soothing vocals & warm strings", "Acoustic Folk Morning", "🎸", 0xFFEC4899, 0xFFDB2777),
        MoodCategory("bollywood", "Bollywood Hits", "Trending soundtracks, romantic & soulful melodies", "Arijit Singh Hits", "🎵", 0xFFF43F5E, 0xFFE11D48),
        MoodCategory("workout", "Workout Energy", "High tempo beats and motivational anthems", "Workout EDM Fitness", "⚡", 0xFF10B981, 0xFF059669),
        MoodCategory("night", "Night Drive", "Late night lofi, synthwave & calm horizons", "Night Synthwave City", "🌙", 0xFF6366F1, 0xFF4F46E5)
    )

    val PRESET_SONGS: List<Song> = emptyList()

    val PRESET_PLAYLISTS: List<Playlist> = emptyList()
}
