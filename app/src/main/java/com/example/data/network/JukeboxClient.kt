package com.example.data.network

import android.content.Context
import com.example.data.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit

data class JukeboxStatus(
    val isAvailable: Boolean,
    val latencyMs: Long = 0L,
    val serverUrl: String,
    val message: String
)

object JukeboxClient {
    const val DEFAULT_SERVER_URL = "https://jukebox.wohlbruck.dev"
    private const val PREFS_NAME = "spring_prefs"
    private const val KEY_JUKEBOX_URL = "jukebox_server_url"

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(35, TimeUnit.SECONDS)
            .followRedirects(true)
            .build()
    }

    fun getServerUrl(context: Context? = null): String {
        if (context == null) return DEFAULT_SERVER_URL
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val custom = prefs.getString(KEY_JUKEBOX_URL, null)?.trim()
        return if (!custom.isNullOrBlank()) {
            custom.trimEnd('/')
        } else {
            DEFAULT_SERVER_URL
        }
    }

    fun setServerUrl(context: Context, url: String) {
        val trimmed = url.trim().trimEnd('/')
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_JUKEBOX_URL, if (trimmed == DEFAULT_SERVER_URL) null else trimmed)
            .apply()
    }

    fun resetServerUrl(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_JUKEBOX_URL)
            .apply()
    }

    /**
     * Data class matching Jukebox GET /v1/metadata response from alexwohlbruck/jukebox
     */
    data class JukeboxTrackMetadata(
        val spotifyTrackId: String,
        val artist: String,
        val track: String,
        val album: String?,
        val artwork: String?,
        val durationMs: Long
    )

    /**
     * Constructs the official Jukebox /v1/stream URL from alexwohlbruck/jukebox.
     * Supports:
     * - Direct artist & track query
     * - 22-character Spotify Track ID resolution
     * - Optional startMs (in milliseconds, up to 6 hours) for live server-side seeking
     * - Optional requestedAtMs (13-digit Unix timestamp) to compensate for decoder/network latency
     */
    fun buildStreamUrl(
        title: String,
        artist: String,
        spotifyTrackId: String? = null,
        startMs: Long? = null,
        requestedAtMs: Long? = null,
        context: Context? = null
    ): String {
        val base = getServerUrl(context)
        val params = StringBuilder()

        val cleanSpotifyId = spotifyTrackId?.trim()?.takeIf { it.length == 22 && it.matches(Regex("^[A-Za-z0-9]{22}$")) }

        if (!cleanSpotifyId.isNullOrBlank()) {
            val encodedId = URLEncoder.encode(cleanSpotifyId, StandardCharsets.UTF_8.toString())
            params.append("spotifyTrackId=").append(encodedId)
        } else {
            val cleanTitle = cleanTrackQuery(title)
            val cleanArtist = cleanArtistQuery(artist)
            val encTitle = URLEncoder.encode(cleanTitle, StandardCharsets.UTF_8.toString())
            val encArtist = URLEncoder.encode(cleanArtist, StandardCharsets.UTF_8.toString())
            params.append("artist=").append(encArtist).append("&track=").append(encTitle)
        }

        if (startMs != null && startMs > 0L) {
            val safeStartMs = startMs.coerceIn(0L, 6L * 60 * 60 * 1000) // Max 6 hours supported by Jukebox
            params.append("&startMs=").append(safeStartMs)
            val nowMs = requestedAtMs ?: System.currentTimeMillis()
            params.append("&requestedAtMs=").append(nowMs)
        }

        return "$base/v1/stream?$params"
    }

    /**
     * Fetches metadata for a Spotify Track ID via Jukebox GET /v1/metadata
     */
    suspend fun getMetadata(spotifyTrackId: String, context: Context? = null): JukeboxTrackMetadata? = withContext(Dispatchers.IO) {
        val cleanId = spotifyTrackId.trim()
        if (cleanId.length != 22) return@withContext null
        val base = getServerUrl(context)
        val url = "$base/v1/metadata?spotifyTrackId=${URLEncoder.encode(cleanId, StandardCharsets.UTF_8.toString())}"

        try {
            val req = Request.Builder()
                .url(url)
                .header("User-Agent", "SpringMusic/9.1 (Android)")
                .get()
                .build()
            httpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext null
                val body = resp.body?.string().orEmpty()
                if (body.isBlank()) return@withContext null
                val json = org.json.JSONObject(body)
                JukeboxTrackMetadata(
                    spotifyTrackId = json.optString("spotifyTrackId", cleanId),
                    artist = json.optString("artist", "Unknown Artist"),
                    track = json.optString("track", "Unknown Track"),
                    album = json.optString("album").takeIf { it.isNotBlank() },
                    artwork = json.optString("artwork").takeIf { it.isNotBlank() },
                    durationMs = json.optLong("durationMs", 180000L)
                )
            }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Resolves the exact YouTube video URL that Jukebox stream would transcode
     * via GET /v1/debug/resolve (from alexwohlbruck/jukebox)
     */
    suspend fun resolveSourceUrl(
        title: String,
        artist: String,
        spotifyTrackId: String? = null,
        context: Context? = null
    ): String? = withContext(Dispatchers.IO) {
        val base = getServerUrl(context)
        val cleanSpotifyId = spotifyTrackId?.trim()?.takeIf { it.length == 22 && it.matches(Regex("^[A-Za-z0-9]{22}$")) }
        val query = if (!cleanSpotifyId.isNullOrBlank()) {
            "spotifyTrackId=${URLEncoder.encode(cleanSpotifyId, StandardCharsets.UTF_8.toString())}"
        } else {
            val encTitle = URLEncoder.encode(cleanTrackQuery(title), StandardCharsets.UTF_8.toString())
            val encArtist = URLEncoder.encode(cleanArtistQuery(artist), StandardCharsets.UTF_8.toString())
            "artist=$encArtist&track=$encTitle"
        }

        try {
            val req = Request.Builder()
                .url("$base/v1/debug/resolve?$query")
                .header("User-Agent", "SpringMusic/9.1 (Android)")
                .get()
                .build()
            httpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext null
                val body = resp.body?.string().orEmpty()
                val json = org.json.JSONObject(body)
                json.optString("url").takeIf { it.isNotBlank() }
            }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Creates a fallback Jukebox song instance when JioSaavn fails or is unplayable
     */
    fun createJukeboxFallbackSong(
        song: Song,
        startMs: Long? = null,
        context: Context? = null
    ): Song {
        val fallbackUrl = buildStreamUrl(
            title = song.title,
            artist = song.artist,
            spotifyTrackId = song.spotifyId,
            startMs = startMs,
            requestedAtMs = System.currentTimeMillis(),
            context = context
        )
        return song.copy(
            audioUrl = fallbackUrl,
            sourceEngine = "jukebox"
        )
    }

    /**
     * Checks if a stream URL is reachable and playable (HTTP 200/206)
     */
    suspend fun checkIsPlayable(url: String?): Boolean = withContext(Dispatchers.IO) {
        if (url.isNullOrBlank()) return@withContext false
        if (url.startsWith("file://") || url.startsWith("/")) return@withContext true

        try {
            val request = Request.Builder()
                .url(url)
                .head()
                .header("User-Agent", "SpringMusic/9.1 (Android; ExoPlayer)")
                .build()
            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful || response.code in 200..299 || response.code == 206) {
                    true
                } else {
                    // Try lightweight range request if HEAD is rejected by CDN
                    val rangeReq = Request.Builder()
                        .url(url)
                        .header("Range", "bytes=0-1024")
                        .header("User-Agent", "SpringMusic/9.1 (Android; ExoPlayer)")
                        .get()
                        .build()
                    httpClient.newCall(rangeReq).execute().use { rangeResp ->
                        rangeResp.isSuccessful || rangeResp.code in 200..299 || rangeResp.code == 206
                    }
                }
            }
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Tests connection to the configured Jukebox server instance via /health
     */
    suspend fun testConnection(context: Context? = null): JukeboxStatus = withContext(Dispatchers.IO) {
        val serverUrl = getServerUrl(context)
        val healthUrl = "$serverUrl/health"
        val start = System.currentTimeMillis()
        try {
            val request = Request.Builder()
                .url(healthUrl)
                .get()
                .build()
            httpClient.newCall(request).execute().use { response ->
                val duration = System.currentTimeMillis() - start
                if (response.isSuccessful) {
                    JukeboxStatus(
                        isAvailable = true,
                        latencyMs = duration,
                        serverUrl = serverUrl,
                        message = "Connected to Jukebox (${response.code} OK - ${duration}ms)"
                    )
                } else {
                    JukeboxStatus(
                        isAvailable = false,
                        latencyMs = duration,
                        serverUrl = serverUrl,
                        message = "Server returned HTTP ${response.code}"
                    )
                }
            }
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - start
            JukeboxStatus(
                isAvailable = false,
                latencyMs = duration,
                serverUrl = serverUrl,
                message = "Connection failed: ${e.message ?: "Unknown error"}"
            )
        }
    }

    private fun cleanTrackQuery(title: String): String {
        return title
            .replace(Regex("""\s*\(.*?(feat|ft|remix|official|video|audio|lyrics).*?\)""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""\s*\[.*?(feat|ft|remix|official|video|audio|lyrics).*?\]""", RegexOption.IGNORE_CASE), "")
            .trim()
    }

    private fun cleanArtistQuery(artist: String): String {
        return artist.split(",", "&", "feat.", "ft.", "/").firstOrNull()?.trim() ?: artist.trim()
    }
}
