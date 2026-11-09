package com.example.data.network

import android.util.Log
import com.example.data.model.LyricLine
import com.example.data.model.Song
import com.example.data.model.SongLyrics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

object LyricsService {
    private const val TAG = "LyricsService"
    private const val LRCLIB_BASE = "https://lrclib.net/api"

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .followRedirects(true)
            .build()
    }

    // In-memory cache to make re-opening songs instant
    private val lyricsCache = ConcurrentHashMap<String, SongLyrics>()

    suspend fun getLyrics(song: Song): SongLyrics? = withContext(Dispatchers.IO) {
        val cacheKey = song.id.ifBlank { "${song.title}_${song.artist}" }
        lyricsCache[cacheKey]?.let { return@withContext it }

        // Clean queries to maximize match rates
        val cleanTitle = cleanTitle(song.title)
        val cleanArtist = cleanArtist(song.artist)
        val durationSec = if (song.durationMs > 0) (song.durationMs / 1000L).toInt() else null

        // 1. Try exact match via /api/get
        var result = fetchExact(cleanTitle, cleanArtist, durationSec, song.id)

        // 2. Fallback to /api/search if exact returned null
        if (result == null || (!result.hasSynced && result.plainLyrics.isNullOrBlank() && !result.isInstrumental)) {
            result = fetchSearch(cleanTitle, cleanArtist, song.id)
        }

        if (result != null) {
            lyricsCache[cacheKey] = result
        }

        result
    }

    private fun fetchExact(title: String, artist: String, durationSec: Int?, songId: String): SongLyrics? {
        try {
            val encTitle = URLEncoder.encode(title, StandardCharsets.UTF_8.toString())
            val encArtist = URLEncoder.encode(artist, StandardCharsets.UTF_8.toString())
            val urlBuilder = StringBuilder("$LRCLIB_BASE/get?track_name=$encTitle&artist_name=$encArtist")
            if (durationSec != null && durationSec > 0) {
                urlBuilder.append("&duration=").append(durationSec)
            }

            val request = Request.Builder()
                .url(urlBuilder.toString())
                .addHeader("User-Agent", "SpringMusic/9.5 (Android; https://github.com/alexwohlbruck/jukebox)")
                .get()
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val body = response.body?.string().orEmpty()
                if (body.isBlank()) return null
                val json = JSONObject(body)
                return parseLyricsJson(json, songId, title, artist)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Exact lyrics fetch failed: ${e.message}")
            return null
        }
    }

    private fun fetchSearch(title: String, artist: String, songId: String): SongLyrics? {
        try {
            val query = "$title $artist".trim()
            val encQuery = URLEncoder.encode(query, StandardCharsets.UTF_8.toString())
            val url = "$LRCLIB_BASE/search?q=$encQuery"

            val request = Request.Builder()
                .url(url)
                .addHeader("User-Agent", "SpringMusic/9.5 (Android; https://github.com/alexwohlbruck/jukebox)")
                .get()
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val body = response.body?.string().orEmpty()
                if (body.isBlank()) return null
                val array = JSONArray(body)
                if (array.length() == 0) return null

                // Look for an entry with synced lyrics first, then plain lyrics
                var bestJson: JSONObject? = null
                for (i in 0 until array.length()) {
                    val item = array.optJSONObject(i) ?: continue
                    val synced = item.optString("syncedLyrics", "")
                    if (synced.isNotBlank()) {
                        bestJson = item
                        break
                    }
                    if (bestJson == null && item.optString("plainLyrics", "").isNotBlank()) {
                        bestJson = item
                    }
                }

                val target = bestJson ?: array.optJSONObject(0) ?: return null
                return parseLyricsJson(target, songId, title, artist)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Search lyrics fetch failed: ${e.message}")
            return null
        }
    }

    private fun parseLyricsJson(json: JSONObject, songId: String, title: String, artist: String): SongLyrics {
        val instrumental = json.optBoolean("instrumental", false)
        val plain = json.optString("plainLyrics", "").takeIf { it.isNotBlank() }
        val rawSynced = json.optString("syncedLyrics", "").takeIf { it.isNotBlank() }

        val syncedLines = if (rawSynced != null) {
            parseLrc(rawSynced)
        } else {
            emptyList()
        }

        return SongLyrics(
            songId = songId,
            title = json.optString("trackName", title),
            artist = json.optString("artistName", artist),
            plainLyrics = plain,
            syncedLyrics = syncedLines,
            isInstrumental = instrumental
        )
    }

    /**
     * Parses standard LRC format:
     * [mm:ss.xx] Lyric text
     * Returns sorted list of LyricLine
     */
    fun parseLrc(lrcText: String): List<LyricLine> {
        val lines = mutableListOf<LyricLine>()
        val pattern = Pattern.compile("""\[(\d{1,2}):(\d{2})(?:\.(\d{1,3}))?\](.*)""")

        lrcText.lines().forEach { line ->
            val matcher = pattern.matcher(line.trim())
            if (matcher.matches()) {
                val min = matcher.group(1)?.toLongOrNull() ?: 0L
                val sec = matcher.group(2)?.toLongOrNull() ?: 0L
                val fracStr = matcher.group(3).orEmpty()
                val frac = when (fracStr.length) {
                    1 -> fracStr.toLongOrNull()?.times(100) ?: 0L
                    2 -> fracStr.toLongOrNull()?.times(10) ?: 0L
                    3 -> fracStr.toLongOrNull() ?: 0L
                    else -> 0L
                }
                val timeMs = (min * 60 + sec) * 1000L + frac
                val text = matcher.group(4)?.trim().orEmpty()

                if (text.isNotBlank()) {
                    lines.add(LyricLine(timeMs, text))
                }
            }
        }

        return lines.sortedBy { it.timeMs }
    }

    private fun cleanTitle(title: String): String {
        return title
            .replace(Regex("""\s*\(.*?(feat|ft|from|with|original|soundtrack|version).*?\)""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""\s*\[.*?(feat|ft|from|with|original|soundtrack|version).*?\]""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""\s*-\s*.*?(From|Soundtrack|Theme|Remix).*?""", RegexOption.IGNORE_CASE), "")
            .trim()
    }

    private fun cleanArtist(artist: String): String {
        return artist.split(",", ";", "&", "feat.", "ft.", "featuring").firstOrNull()?.trim() ?: artist
    }
}
