package com.example.data.network

import com.example.data.model.AudioQuality
import com.example.data.model.MoodCategory
import com.example.data.model.Playlist
import com.example.data.model.Song

object PresetCatalog {
    val MOOD_CATEGORIES = listOf(
        MoodCategory("focus", "Focus & Flow", "Binaural soundscapes & deep ambient tones", "Focus Lofi Study", "", 0xFF23262F, 0xFF171A21),
        MoodCategory("chill", "Chill Beats", "Relaxing melodies to unwind and de-stress", "Chill Lounge Relaxing", "", 0xFF282C37, 0xFF171A21),
        MoodCategory("feel_good", "Feel Good", "Upbeat, sunny rhythms and positive vibrations", "Summer Happy Pop", "", 0xFF2C303C, 0xFF171A21),
        MoodCategory("ambient", "Ambient Space", "Floating synthesizers and atmospheric calm", "Ambient Meditative Peace", "", 0xFF21242D, 0xFF13151B),
        MoodCategory("acoustic", "Acoustic Warmth", "Gentle guitars, soothing vocals & warm strings", "Acoustic Folk Morning", "", 0xFF272B35, 0xFF151820),
        MoodCategory("bollywood", "Bollywood Hits", "Trending soundtracks, romantic & soulful melodies", "Arijit Singh Hits", "", 0xFF2B2F3A, 0xFF171A21),
        MoodCategory("workout", "Workout Energy", "High tempo beats and motivational anthems", "Workout EDM Fitness", "", 0xFF252933, 0xFF14171D),
        MoodCategory("night", "Night Drive", "Late night lofi, synthwave & calm horizons", "Night Synthwave City", "", 0xFF1E222B, 0xFF101218)
    )

    val PRESET_SONGS: List<Song> = emptyList()

    val PRESET_PLAYLISTS: List<Playlist> = emptyList()
}
