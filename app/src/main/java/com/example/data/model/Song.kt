package com.example.data.model

import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata

data class Song(
    val id: String,
    val title: String,
    val artist: String,
    val album: String = "Spring Music",
    val coverUrl: String = "",
    val audioUrl: String = "",
    val durationMs: Long = 0L,
    val mood: String = "Focus",
    val streamId: String = "",
    val sourceEngine: String = "jiosaavn",
    val qualityList: List<AudioQuality> = emptyList(),
    val isFavorite: Boolean = false,
    val isDownloaded: Boolean = false,
    val localFilePath: String? = null
) {
    val effectivePlayableUri: String
        get() = if (!localFilePath.isNullOrBlank()) localFilePath else audioUrl

    fun toMediaItem(): MediaItem {
        val metadata = MediaMetadata.Builder()
            .setTitle(title)
            .setArtist(artist)
            .setAlbumTitle(album)
            .setArtworkUri(if (coverUrl.isNotBlank()) Uri.parse(coverUrl) else null)
            .setDescription(mood)
            .build()

        return MediaItem.Builder()
            .setMediaId(id)
            .setUri(Uri.parse(effectivePlayableUri))
            .setMediaMetadata(metadata)
            .build()
    }
}
