package com.example.data.network

import com.example.data.model.AudioQuality
import com.example.data.model.Playlist
import com.example.data.model.PlaylistItemDto
import com.example.data.model.Song
import com.example.data.model.SongItemDto
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

object NetworkClient {
    private const val BASE_URL = "https://spring-fqiz24.v2.appdeploy.ai/"

    private val okHttpClient: OkHttpClient by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(15L, TimeUnit.SECONDS)
            .readTimeout(20L, TimeUnit.SECONDS)
            .build()
    }

    private val moshi: Moshi by lazy {
        Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()
    }

    val apiService: MusicApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(MusicApiService::class.java)
    }

    fun mapSongDtoToDomain(dto: SongItemDto, moodTag: String = "Spring Vibes"): Song {
        val id = dto.id ?: "song_${System.currentTimeMillis()}"
        val rawTitle = dto.name ?: "Unknown Track"
        val title = decodeHtml(rawTitle)

        // Parse artist
        var artist = "Various Artists"
        when (val art = dto.artists) {
            is Map<*, *> -> {
                val primary = art["primary"]
                if (primary is List<*>) {
                    val names = primary.mapNotNull {
                        (it as? Map<*, *>)?.get("name")?.toString()
                    }
                    if (names.isNotEmpty()) artist = names.joinToString(", ")
                } else if (art["name"] != null) {
                    artist = art["name"].toString()
                }
            }
            is List<*> -> {
                val names = art.mapNotNull {
                    if (it is Map<*, *>) it["name"]?.toString() else it?.toString()
                }
                if (names.isNotEmpty()) artist = names.joinToString(", ")
            }
            is String -> if (art.isNotBlank()) artist = art
        }
        artist = decodeHtml(artist)

        // Parse album
        var album = "Spring Music"
        when (val alb = dto.album) {
            is Map<*, *> -> alb["name"]?.toString()?.let { album = decodeHtml(it) }
            is String -> if (alb.isNotBlank()) album = decodeHtml(alb)
        }

        // Parse image url
        var coverUrl = ""
        when (val img = dto.image) {
            is List<*> -> {
                val lastItem = img.lastOrNull()
                if (lastItem is Map<*, *>) {
                    coverUrl = lastItem["url"]?.toString() ?: ""
                } else if (lastItem is String) {
                    coverUrl = lastItem
                }
            }
            is Map<*, *> -> coverUrl = img["url"]?.toString() ?: ""
            is String -> coverUrl = img
        }
        if (coverUrl.isBlank()) {
            coverUrl = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=700&auto=format&fit=crop&q=85"
        }

        // Parse duration
        var durationMs = 180000L
        when (val dur = dto.duration) {
            is Number -> durationMs = dur.toLong() * 1000L
            is String -> dur.toLongOrNull()?.let { durationMs = it * 1000L }
        }

        // Parse audio download URLs
        val qualities = mutableListOf<AudioQuality>()
        var bestAudioUrl = ""
        when (val dl = dto.downloadUrl) {
            is List<*> -> {
                for (item in dl) {
                    if (item is Map<*, *>) {
                        val quality = item["quality"]?.toString() ?: "320kbps"
                        val url = item["url"]?.toString() ?: ""
                        if (url.isNotBlank()) {
                            qualities.add(AudioQuality(quality, url))
                        }
                    }
                }
                bestAudioUrl = qualities.lastOrNull()?.url ?: ""
            }
            is Map<*, *> -> {
                val url = dl["url"]?.toString() ?: ""
                if (url.isNotBlank()) {
                    bestAudioUrl = url
                    qualities.add(AudioQuality("Standard", url))
                }
            }
            is String -> if (dl.isNotBlank()) {
                bestAudioUrl = dl
                qualities.add(AudioQuality("Standard", dl))
            }
        }

        if (bestAudioUrl.isBlank()) {
            bestAudioUrl = "https://cdn.pixabay.com/download/audio/2022/10/25/audio_9467c7c9d0.mp3?filename=ambient-piano-logo-125040.mp3"
        }

        return Song(
            id = id,
            title = title,
            artist = artist,
            album = album,
            coverUrl = coverUrl,
            audioUrl = bestAudioUrl,
            durationMs = durationMs,
            mood = moodTag,
            qualityList = qualities
        )
    }

    fun mapPlaylistDtoToDomain(dto: PlaylistItemDto): Playlist {
        val id = dto.id ?: "pl_${System.currentTimeMillis()}"
        val rawName = dto.name ?: dto.title ?: "Curated Playlist"
        val title = decodeHtml(rawName)
        val rawSub = dto.subtitle ?: dto.description ?: "JioSaavn Playlist"
        val subtitle = decodeHtml(rawSub)

        var coverUrl = ""
        when (val img = dto.image) {
            is List<*> -> {
                val lastItem = img.lastOrNull()
                if (lastItem is Map<*, *>) {
                    coverUrl = lastItem["url"]?.toString() ?: ""
                } else if (lastItem is String) {
                    coverUrl = lastItem
                }
            }
            is Map<*, *> -> coverUrl = img["url"]?.toString() ?: ""
            is String -> coverUrl = img
        }
        if (coverUrl.isBlank()) {
            coverUrl = "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=700&auto=format&fit=crop&q=85"
        }

        val count = when (val c = dto.songCount) {
            is Number -> c.toInt()
            is String -> c.toIntOrNull() ?: 0
            else -> 0
        }

        return Playlist(
            id = id,
            title = title,
            description = subtitle,
            coverUrl = coverUrl,
            songCount = count
        )
    }

    private fun decodeHtml(text: String): String {
        return text.replace("&quot;", "\"")
            .replace("&#039;", "'")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
    }
}
