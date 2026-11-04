package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.Song

@Entity(tableName = "saved_songs")
data class SavedSongEntity(
    @PrimaryKey val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val coverUrl: String,
    val audioUrl: String,
    val durationMs: Long,
    val mood: String,
    val isFavorite: Boolean = false,
    val isDownloaded: Boolean = false,
    val localFilePath: String? = null,
    val lastPlayedTimestamp: Long? = null
) {
    fun toSong(): Song = Song(
        id = id,
        title = title,
        artist = artist,
        album = album,
        coverUrl = coverUrl,
        audioUrl = audioUrl,
        durationMs = durationMs,
        mood = mood,
        isFavorite = isFavorite,
        isDownloaded = isDownloaded,
        localFilePath = localFilePath
    )

    companion object {
        fun fromSong(
            song: Song,
            isFavorite: Boolean = song.isFavorite,
            isDownloaded: Boolean = song.isDownloaded,
            localFilePath: String? = song.localFilePath,
            lastPlayedTimestamp: Long? = null
        ): SavedSongEntity = SavedSongEntity(
            id = song.id,
            title = song.title,
            artist = song.artist,
            album = song.album,
            coverUrl = song.coverUrl,
            audioUrl = song.audioUrl,
            durationMs = song.durationMs,
            mood = song.mood,
            isFavorite = isFavorite,
            isDownloaded = isDownloaded,
            localFilePath = localFilePath,
            lastPlayedTimestamp = lastPlayedTimestamp
        )
    }
}

@Entity(tableName = "user_playlists")
data class UserPlaylistEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String = "",
    val coverUrl: String = "",
    val songCount: Int = 0,
    val songsJson: String = "",
    val isSpotifyImport: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
