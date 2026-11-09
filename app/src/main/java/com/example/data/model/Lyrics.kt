package com.example.data.model

data class LyricLine(
    val timeMs: Long,
    val text: String
)

data class SongLyrics(
    val songId: String,
    val title: String,
    val artist: String,
    val plainLyrics: String? = null,
    val syncedLyrics: List<LyricLine> = emptyList(),
    val isInstrumental: Boolean = false
) {
    val hasSynced: Boolean get() = syncedLyrics.isNotEmpty()
    val isAvailable: Boolean get() = isInstrumental || hasSynced || !plainLyrics.isNullOrBlank()
}
