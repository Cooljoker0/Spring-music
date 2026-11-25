package com.example.data.model

data class Playlist(
    val id: String,
    val title: String,
    val description: String = "",
    val subtitle: String = description,
    val coverUrl: String = "",
    val songCount: Int = 0,
    val songs: List<Song> = emptyList()
)

data class MoodCategory(
    val id: String,
    val title: String,
    val description: String,
    val searchTag: String = title,
    val emoji: String = "",
    val gradientStart: Long = 0xFF23262F,
    val gradientEnd: Long = 0xFF171A21
)

data class AudioQuality(
    val label: String,
    val url: String
)
