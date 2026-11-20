package com.example.data.model

import android.net.Uri
import android.os.Bundle
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import java.io.File

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
    val spotifyId: String? = null,
    val qualityList: List<AudioQuality> = emptyList(),
    val isFavorite: Boolean = false,
    val isDownloaded: Boolean = false,
    val localFilePath: String? = null
) {
    val isJukeboxEngine: Boolean
        get() = sourceEngine.equals("jukebox", ignoreCase = true) ||
            audioUrl.contains("/v1/stream", ignoreCase = true)

    val effectivePlayableUri: String
        get() {
            val local = localFilePath?.trim().orEmpty()
            if (local.isNotBlank()) {
                val file = File(local.removePrefix("file://"))
                if (file.exists() && file.length() > 0L) {
                    return Uri.fromFile(file).toString()
                }
            }
            return audioUrl
        }

    fun toMediaItem(groupTitle: String? = null): MediaItem {
        val uriString = if (effectivePlayableUri.isNotBlank()) {
            effectivePlayableUri
        } else {
            com.example.data.network.JukeboxClient.buildStreamUrl(title, artist, spotifyId)
        }
        val playableUri = Uri.parse(uriString)

        val extras = Bundle().apply {
            putString(EXTRA_AUDIO_URL, uriString)
            putString(EXTRA_COVER_URL, coverUrl)
            putLong(EXTRA_DURATION_MS, durationMs)
            putString(EXTRA_SOURCE_ENGINE, sourceEngine)
            putString(EXTRA_MOOD, mood)
            if (isDownloaded && !localFilePath.isNullOrBlank()) {
                // MediaDescriptionCompat.EXTRA_DOWNLOAD_STATUS / STATUS_DOWNLOADED = 2L
                putLong("android.media.extra.DOWNLOAD_STATUS", 2L)
            }
            if (!groupTitle.isNullOrBlank()) {
                putString("android.media.browse.CONTENT_STYLE_GROUP_TITLE_HINT", groupTitle)
            }
        }

        val metadata = MediaMetadata.Builder()
            .setTitle(title)
            .setArtist(artist)
            .setSubtitle(artist)
            .setAlbumTitle(album)
            .setArtworkUri(if (coverUrl.isNotBlank()) Uri.parse(coverUrl) else null)
            .setDescription(mood)
            .setIsBrowsable(false)
            .setIsPlayable(true)
            .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
            .setExtras(extras)
            .build()

        return MediaItem.Builder()
            .setMediaId(id)
            .setUri(playableUri)
            .setRequestMetadata(
                MediaItem.RequestMetadata.Builder()
                    .setMediaUri(playableUri)
                    .build()
            )
            .setMediaMetadata(metadata)
            .build()
    }

    companion object {
        const val EXTRA_AUDIO_URL = "spring_extra_audio_url"
        const val EXTRA_COVER_URL = "spring_extra_cover_url"
        const val EXTRA_DURATION_MS = "spring_extra_duration_ms"
        const val EXTRA_SOURCE_ENGINE = "spring_extra_source_engine"
        const val EXTRA_MOOD = "spring_extra_mood"

        fun fromMediaItem(mediaItem: MediaItem): Song? {
            val meta = mediaItem.mediaMetadata
            val title = meta.title?.toString()?.trim().orEmpty()
            if (title.isBlank()) return null
            val artist = meta.artist?.toString()?.trim().orEmpty().ifBlank {
                meta.subtitle?.toString()?.trim().orEmpty().ifBlank { "Spring Music" }
            }
            val extras = meta.extras
            val audioUri = mediaItem.localConfiguration?.uri?.toString()
                ?: extras?.getString(EXTRA_AUDIO_URL)
                ?: ""
            val cover = meta.artworkUri?.toString()
                ?: extras?.getString(EXTRA_COVER_URL)
                ?: ""
            return Song(
                id = mediaItem.mediaId.ifBlank { "media_${title.hashCode()}" },
                title = title,
                artist = artist,
                album = meta.albumTitle?.toString()?.trim().orEmpty().ifBlank { "Spring Music" },
                coverUrl = cover,
                audioUrl = audioUri,
                durationMs = extras?.getLong(EXTRA_DURATION_MS, 0L) ?: 0L,
                mood = extras?.getString(EXTRA_MOOD).orEmpty().ifBlank { "Spring" },
                sourceEngine = extras?.getString(EXTRA_SOURCE_ENGINE).orEmpty().ifBlank { "jiosaavn" }
            )
        }
    }
}
