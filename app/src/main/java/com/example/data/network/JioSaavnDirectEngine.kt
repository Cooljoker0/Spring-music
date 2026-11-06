package com.example.data.network

import android.util.Base64
import com.example.data.model.AudioQuality
import com.example.data.model.Playlist
import com.example.data.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

object JioSaavnDirectEngine {
    private const val JIOSAAVN_API_BASE = "https://www.jiosaavn.com/api.php"
    private const val DES_SECRET_KEY = "38346591"

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(12, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .followRedirects(true)
            .build()
    }

    /**
     * Decrypts the DES ECB encrypted_media_url returned by JioSaavn's internal API into direct CDN link
     */
    fun decryptMediaUrl(encryptedUrl: String?): String? {
        if (encryptedUrl.isNullOrBlank()) return null
        return try {
            val keySpec = SecretKeySpec(DES_SECRET_KEY.toByteArray(StandardCharsets.UTF_8), "DES")
            val cipher = Cipher.getInstance("DES/ECB/PKCS5Padding")
            cipher.init(Cipher.DECRYPT_MODE, keySpec)
            val decoded = Base64.decode(encryptedUrl, Base64.DEFAULT)
            val decryptedBytes = cipher.doFinal(decoded)
            String(decryptedBytes, StandardCharsets.UTF_8).trim()
        } catch (e: Exception) {
            android.util.Log.e("JioSaavnDirect", "Failed to decrypt media url: ${e.message}")
            null
        }
    }

    /**
     * Generates all bitrates (320kbps, 160kbps, 128kbps, 96kbps) from decrypted stream URL
     */
    fun buildQualityList(rawUrl: String): List<AudioQuality> {
        if (rawUrl.isBlank()) return emptyList()
        val qualities = mutableListOf<AudioQuality>()

        if (rawUrl.contains("_96.")) {
            qualities.add(AudioQuality("320kbps", rawUrl.replace("_96.", "_320.")))
            qualities.add(AudioQuality("160kbps", rawUrl.replace("_96.", "_160.")))
            qualities.add(AudioQuality("128kbps", rawUrl.replace("_96.", "_128.")))
            qualities.add(AudioQuality("96kbps", rawUrl))
        } else if (rawUrl.contains("_160.")) {
            qualities.add(AudioQuality("320kbps", rawUrl.replace("_160.", "_320.")))
            qualities.add(AudioQuality("160kbps", rawUrl))
            qualities.add(AudioQuality("128kbps", rawUrl.replace("_160.", "_128.")))
            qualities.add(AudioQuality("96kbps", rawUrl.replace("_160.", "_96.")))
        } else if (rawUrl.contains("_320.")) {
            qualities.add(AudioQuality("320kbps", rawUrl))
            qualities.add(AudioQuality("160kbps", rawUrl.replace("_320.", "_160.")))
            qualities.add(AudioQuality("128kbps", rawUrl.replace("_320.", "_128.")))
            qualities.add(AudioQuality("96kbps", rawUrl.replace("_320.", "_96.")))
        } else {
            qualities.add(AudioQuality("320kbps", rawUrl))
        }
        return qualities
    }

    /**
     * Searches JioSaavn directly for songs
     */
    suspend fun searchSongs(query: String, moodTag: String = "Spring Vibes", limit: Int = 25): List<Song> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        val encQuery = URLEncoder.encode(query.trim(), StandardCharsets.UTF_8.toString())
        val url = "$JIOSAAVN_API_BASE?__call=search.getResults&_format=json&_marker=0&api_version=4&ctx=web6dot0&n=$limit&p=1&q=$encQuery"

        try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
                .header("Accept", "application/json")
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext emptyList()
                val bodyStr = response.body?.string() ?: return@withContext emptyList()
                val rootJson = JSONObject(bodyStr)
                val results = rootJson.optJSONArray("results") ?: return@withContext emptyList()

                val songs = mutableListOf<Song>()
                for (i in 0 until results.length()) {
                    val item = results.optJSONObject(i) ?: continue
                    val song = parseSongJson(item, moodTag)
                    if (song != null) {
                        songs.add(song)
                    }
                }
                songs
            }
        } catch (e: Exception) {
            android.util.Log.e("JioSaavnDirect", "searchSongs error: ${e.message}")
            emptyList()
        }
    }

    /**
     * Searches JioSaavn directly for playlists
     */
    suspend fun searchPlaylists(query: String, limit: Int = 20): List<Playlist> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        val encQuery = URLEncoder.encode(query.trim(), StandardCharsets.UTF_8.toString())
        val url = "$JIOSAAVN_API_BASE?__call=search.getPlaylistResults&_format=json&_marker=0&api_version=4&ctx=web6dot0&n=$limit&p=1&q=$encQuery"

        try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
                .header("Accept", "application/json")
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext emptyList()
                val bodyStr = response.body?.string() ?: return@withContext emptyList()
                val rootJson = JSONObject(bodyStr)
                val results = rootJson.optJSONArray("results") ?: return@withContext emptyList()

                val playlists = mutableListOf<Playlist>()
                for (i in 0 until results.length()) {
                    val item = results.optJSONObject(i) ?: continue
                    val id = item.optString("id").ifBlank { "pl_$i" }
                    val rawTitle = item.optString("title").ifBlank { "Playlist" }
                    val title = decodeHtml(rawTitle)
                    val rawDesc = item.optString("subtitle").ifBlank { item.optString("description") }
                    val subtitle = decodeHtml(rawDesc)
                    val rawImg = item.optString("image")
                    val cover = formatHighResImage(rawImg)
                    val moreInfo = item.optJSONObject("more_info")
                    val songCount = moreInfo?.optString("song_count")?.toIntOrNull()
                        ?: moreInfo?.optInt("song_count", 0) ?: 0

                    playlists.add(
                        Playlist(
                            id = id,
                            title = title,
                            description = subtitle,
                            subtitle = if (songCount > 0) "$songCount songs" else subtitle,
                            coverUrl = cover,
                            songCount = songCount
                        )
                    )
                }
                playlists
            }
        } catch (e: Exception) {
            android.util.Log.e("JioSaavnDirect", "searchPlaylists error: ${e.message}")
            emptyList()
        }
    }

    /**
     * Gets full playlist details and all song tracks from JioSaavn directly
     */
    suspend fun getPlaylistDetails(playlistId: String): Playlist? = withContext(Dispatchers.IO) {
        val url = "$JIOSAAVN_API_BASE?__call=playlist.getDetails&_format=json&_marker=0&api_version=4&ctx=web6dot0&listid=$playlistId"

        try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
                .header("Accept", "application/json")
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val bodyStr = response.body?.string() ?: return@withContext null
                val rootJson = JSONObject(bodyStr)

                val id = rootJson.optString("id").ifBlank { playlistId }
                val title = decodeHtml(rootJson.optString("title").ifBlank { "Curated Playlist" })
                val cover = formatHighResImage(rootJson.optString("image"))

                val songsList = mutableListOf<Song>()
                val listArr = rootJson.optJSONArray("list")
                if (listArr != null) {
                    for (i in 0 until listArr.length()) {
                        val item = listArr.optJSONObject(i) ?: continue
                        val song = parseSongJson(item, title)
                        if (song != null) {
                            songsList.add(song)
                        }
                    }
                }

                Playlist(
                    id = id,
                    title = title,
                    description = decodeHtml(rootJson.optString("subtitle").ifBlank { "${songsList.size} songs" }),
                    subtitle = "${songsList.size} songs",
                    coverUrl = cover.ifBlank { songsList.firstOrNull()?.coverUrl.orEmpty() },
                    songCount = songsList.size,
                    songs = songsList
                )
            }
        } catch (e: Exception) {
            android.util.Log.e("JioSaavnDirect", "getPlaylistDetails error: ${e.message}")
            null
        }
    }

    private fun parseSongJson(item: JSONObject, moodTag: String): Song? {
        val id = item.optString("id").ifBlank { return null }
        val rawTitle = item.optString("title").ifBlank { item.optString("song") }
        val title = decodeHtml(rawTitle.ifBlank { "Unknown Track" })

        val moreInfo = item.optJSONObject("more_info") ?: JSONObject()

        // Parse artist
        var artist = ""
        val artistMap = moreInfo.optJSONObject("artistMap")
        if (artistMap != null) {
            val primary = artistMap.optJSONArray("primary_artists")
            if (primary != null && primary.length() > 0) {
                val names = mutableListOf<String>()
                for (j in 0 until primary.length()) {
                    val aObj = primary.optJSONObject(j)
                    val aName = aObj?.optString("name")
                    if (!aName.isNullOrBlank()) names.add(aName)
                }
                artist = names.joinToString(", ")
            }
        }
        if (artist.isBlank()) {
            artist = moreInfo.optString("singers").ifBlank {
                moreInfo.optString("music").ifBlank {
                    item.optString("subtitle").ifBlank { "Various Artists" }
                }
            }
        }
        artist = decodeHtml(artist)

        // Parse album
        val album = decodeHtml(moreInfo.optString("album").ifBlank { "Spring Music" })

        // Parse cover art
        val coverUrl = formatHighResImage(item.optString("image"))

        // Parse duration
        val durSec = moreInfo.optString("duration").toLongOrNull() ?: moreInfo.optLong("duration", 180L)
        val durationMs = durSec.coerceAtLeast(10L) * 1000L

        // Decrypt media URL
        val encUrl = moreInfo.optString("encrypted_media_url")
        val decrypted = decryptMediaUrl(encUrl)

        var bestAudioUrl = ""
        var qualityList = emptyList<AudioQuality>()

        if (!decrypted.isNullOrBlank()) {
            qualityList = buildQualityList(decrypted)
            bestAudioUrl = qualityList.find { it.label == "320kbps" }?.url ?: decrypted
        }

        if (bestAudioUrl.isBlank()) {
            // If encrypted URL was missing, fallback to Jukebox stream directly
            bestAudioUrl = JukeboxClient.buildStreamUrl(title, artist)
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
            sourceEngine = if (decrypted.isNullOrBlank()) "jukebox" else "jiosaavn",
            qualityList = qualityList
        )
    }

    private fun formatHighResImage(imgUrl: String?): String {
        if (imgUrl.isNullOrBlank()) {
            return "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=700&auto=format&fit=crop&q=85"
        }
        return imgUrl
            .replace("150x150", "500x500")
            .replace("50x50", "500x500")
            .replace("http://", "https://")
    }

    private fun decodeHtml(text: String): String {
        return text.replace("&quot;", "\"")
            .replace("&#039;", "'")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .trim()
    }
}
