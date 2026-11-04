package com.example.data.network

import com.example.data.model.AudioQuality
import com.example.data.model.Playlist
import com.example.data.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.net.URL
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

data class SpotifyImportedTrack(
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val cover: String,
    val durationMs: Long
)

data class SpotifyImportResult(
    val title: String,
    val thumbnail: String,
    val playlistUrl: String,
    val tracks: List<SpotifyImportedTrack>
)

object SpotifyImporter {

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .followRedirects(true)
            .build()
    }

    private fun decodeHtml(value: String): String {
        return value
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&apos;", "'")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
    }

    fun parsePlaylistId(rawUrl: String): String? {
        var trimmed = rawUrl.trim()
        if (trimmed.contains("spotify.link/")) {
            try {
                val req = Request.Builder().url(trimmed).head().build()
                val resp = httpClient.newCall(req).execute()
                val finalUrl = resp.request.url.toString()
                if (finalUrl.contains("/playlist/")) {
                    trimmed = finalUrl
                }
            } catch (_: Exception) {}
        }

        val urlMatch = Regex("""(?:open\.spotify\.com|spotify\.com)/playlist/([A-Za-z0-9]+)""").find(trimmed)
        if (urlMatch != null) return urlMatch.groupValues[1]

        val uriMatch = Regex("""spotify:playlist:([A-Za-z0-9]+)""").find(trimmed)
        return uriMatch?.groupValues?.get(1)
    }

    private fun collectTracks(value: Any?, found: MutableList<SpotifyImportedTrack>, seen: MutableSet<String>) {
        if (value == null) return

        when (value) {
            is JSONArray -> {
                for (i in 0 until value.length()) {
                    collectTracks(value.opt(i), found, seen)
                }
            }
            is JSONObject -> {
                val title = (value.optString("name").ifBlank {
                    value.optString("title").ifBlank {
                        value.optString("trackName")
                    }
                }).trim()

                var artist = ""
                val artistsVal = value.opt("artists")
                if (artistsVal is JSONArray) {
                    val list = mutableListOf<String>()
                    for (i in 0 until artistsVal.length()) {
                        val a = artistsVal.opt(i)
                        if (a is JSONObject) {
                            val aName = a.optString("name")
                            if (aName.isNotBlank()) list.add(aName)
                        } else if (a is String && a.isNotBlank()) {
                            list.add(a)
                        }
                    }
                    artist = list.joinToString(", ")
                }
                if (artist.isBlank()) {
                    artist = value.optString("subtitle").ifBlank {
                        value.optString("artistName").ifBlank {
                            value.optJSONObject("artist")?.optString("name") ?: ""
                        }
                    }
                }

                val uri = value.optString("uri").ifBlank {
                    value.optJSONObject("external_urls")?.optString("spotify") ?: ""
                }

                val idMatch = Regex("""(?:spotify:track:|track/)([A-Za-z0-9]+)""").find(uri)
                val id = value.optString("id").ifBlank { idMatch?.groupValues?.get(1) ?: "" }
                val type = (value.optString("type").ifBlank { value.optString("__typename") }).lowercase()

                if (title.isNotBlank() && artist.isNotBlank() &&
                    (id.isNotBlank() || type == "track" || type.contains("trackentity"))
                ) {
                    val key = if (id.isNotBlank()) id else "$title|$artist".lowercase()
                    if (!seen.contains(key)) {
                        seen.add(key)

                        var cover = ""
                        val albumObj = value.optJSONObject("album")
                        val images = albumObj?.optJSONArray("images") ?: value.optJSONArray("images")
                        if (images != null && images.length() > 0) {
                            cover = images.optJSONObject(0)?.optString("url") ?: ""
                        }
                        if (cover.isBlank()) {
                            cover = albumObj?.optString("imageUrl").orEmpty()
                                .ifBlank { value.optString("imageUrl") }
                                .ifBlank { value.optJSONObject("image")?.optString("url").orEmpty() }
                        }

                        val albumName = albumObj?.optString("name")
                            ?.ifBlank { value.optString("albumName") }
                            ?: "Spring Playlist"

                        val durationMs = value.optLong("duration_ms",
                            value.optLong("durationMs", value.optLong("duration", 180000L))
                        )

                        found.add(
                            SpotifyImportedTrack(
                                id = key,
                                title = decodeHtml(title),
                                artist = decodeHtml(artist),
                                album = decodeHtml(albumName),
                                cover = cover,
                                durationMs = durationMs
                            )
                        )
                    }
                }

                // Recurse into child properties
                val keys = value.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    if (k == "images" || k == "artists" || k == "external_urls" || k == "owner") continue
                    collectTracks(value.opt(k), found, seen)
                }
            }
        }
    }

    private fun extractTracksFromHtml(html: String): List<SpotifyImportedTrack> {
        val found = mutableListOf<SpotifyImportedTrack>()
        val seen = mutableSetOf<String>()

        val pattern = Pattern.compile("<script\\b[^>]*>([\\s\\S]*?)</script>", Pattern.CASE_INSENSITIVE)
        val matcher = pattern.matcher(html)

        while (matcher.find()) {
            val body = matcher.group(1)?.trim().orEmpty()
            if (body.isBlank() || body.length > 10_000_000) continue

            for (candidate in listOf(body, decodeHtml(body))) {
                try {
                    if (candidate.startsWith("{")) {
                        val json = JSONObject(candidate)
                        collectTracks(json, found, seen)
                    } else if (candidate.startsWith("[")) {
                        val json = JSONArray(candidate)
                        collectTracks(json, found, seen)
                    }
                } catch (_: Exception) {
                    // Ignore non-standalone JSON scripts
                }
            }
        }

        return found
    }

    suspend fun importPlaylist(url: String): SpotifyImportResult = withContext(Dispatchers.IO) {
        val playlistId = parsePlaylistId(url)
            ?: throw IllegalArgumentException("Please paste a valid public playlist URL (e.g. https://open.spotify.com/playlist/...)")

        val playlistUrl = "https://open.spotify.com/playlist/$playlistId"
        val headers = mapOf(
            "Accept" to "text/html,application/xhtml+xml,application/json;q=0.9,*/*;q=0.8",
            "Accept-Language" to "en-US,en;q=0.9",
            "Referer" to "https://open.spotify.com/",
            "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"
        )

        var playlistTitle = "Spring Playlist"
        var playlistThumbnail = ""

        // Concurrently fetch oEmbed, embed HTML, and public HTML for maximum speed
        var oembedRes = ""
        val pages = mutableListOf<String>()

        coroutineScope {
            val oembedDeferred = async(Dispatchers.IO) {
                try {
                    val oembedUrl = "https://open.spotify.com/oembed?url=" + URLEncoder.encode(playlistUrl, "UTF-8")
                    val reqBuilder = Request.Builder().url(oembedUrl)
                    headers.forEach { (k, v) -> reqBuilder.addHeader(k, v) }
                    val resp = httpClient.newCall(reqBuilder.build()).execute()
                    if (resp.isSuccessful) resp.body?.string().orEmpty() else ""
                } catch (_: Exception) { "" }
            }

            val urlsToFetch = listOf(
                "https://open.spotify.com/embed/playlist/$playlistId",
                playlistUrl
            )

            val pageDeferreds = urlsToFetch.map { pageUrl ->
                async(Dispatchers.IO) {
                    try {
                        val reqBuilder = Request.Builder().url(pageUrl)
                        headers.forEach { (k, v) -> reqBuilder.addHeader(k, v) }
                        val resp = httpClient.newCall(reqBuilder.build()).execute()
                        if (resp.isSuccessful) resp.body?.string().orEmpty() else ""
                    } catch (_: Exception) { "" }
                }
            }

            oembedRes = oembedDeferred.await()
            val fetched = pageDeferreds.awaitAll()
            for (p in fetched) {
                if (p.isNotBlank()) {
                    pages.add(p)
                }
            }
        }

        if (oembedRes.isNotBlank()) {
            try {
                val oembedJson = JSONObject(oembedRes)
                playlistTitle = oembedJson.optString("title", playlistTitle)
                playlistThumbnail = oembedJson.optString("thumbnail_url", playlistThumbnail)
            } catch (_: Exception) {}
        }

        val allFound = mutableListOf<SpotifyImportedTrack>()
        val seen = mutableSetOf<String>()

        for (page in pages) {
            val extracted = extractTracksFromHtml(page)
            for (t in extracted) {
                val key = if (t.id.isNotBlank()) t.id else "${t.title}|${t.artist}".lowercase()
                if (!seen.contains(key)) {
                    seen.add(key)
                    allFound.add(t)
                }
            }
        }

        if (allFound.isEmpty()) {
            throw IllegalStateException("Unable to expose the track list. The playlist may be private or unavailable.")
        }

        SpotifyImportResult(
            title = playlistTitle,
            thumbnail = playlistThumbnail.ifBlank { allFound.firstOrNull()?.cover.orEmpty() },
            playlistUrl = playlistUrl,
            tracks = allFound.take(200)
        )
    }
}
