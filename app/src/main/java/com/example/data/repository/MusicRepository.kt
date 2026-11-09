package com.example.data.repository

import android.content.Context
import com.example.data.local.MediaCacheManager
import com.example.data.local.PlaylistDao
import com.example.data.local.SavedSongEntity
import com.example.data.local.SongDao
import com.example.data.local.UserPlaylistEntity
import com.example.data.model.AudioQuality
import com.example.data.model.Playlist
import com.example.data.model.Song
import com.example.data.network.JioSaavnDirectEngine
import com.example.data.network.JukeboxClient
import com.example.data.network.NetworkClient
import com.example.data.network.PresetCatalog
import com.example.data.network.SpotifyImporter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

import com.example.data.firestore.FirestoreSyncManager

data class PlaylistImportStatus(
    val isImporting: Boolean = false,
    val playlistName: String = "",
    val currentTrack: Int = 0,
    val totalTracks: Int = 0,
    val trackTitle: String = "",
    val errorMessage: String? = null,
    val completedPlaylist: Playlist? = null
)

class MusicRepository(
    private val songDao: SongDao,
    private val playlistDao: PlaylistDao,
    private val context: Context,
    private val firestoreSyncManager: FirestoreSyncManager? = null
) {
    private val repositoryScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _importStatus = MutableStateFlow(PlaylistImportStatus())
    val importStatus: StateFlow<PlaylistImportStatus> = _importStatus.asStateFlow()

    fun dismissImportStatus() {
        _importStatus.value = PlaylistImportStatus()
    }

    fun startBackgroundImport(urlOrName: String) {
        if (_importStatus.value.isImporting) return
        val trimmed = urlOrName.trim()
        val isUrl = trimmed.startsWith("http://", ignoreCase = true) ||
                trimmed.startsWith("https://", ignoreCase = true) ||
                trimmed.contains("spotify.com") ||
                trimmed.contains("spotify.link") ||
                trimmed.matches(Regex("^[a-zA-Z0-9]{22}$"))

        if (isUrl) {
            repositoryScope.launch {
                _importStatus.value = PlaylistImportStatus(
                    isImporting = true,
                    playlistName = "Connecting...",
                    currentTrack = 0,
                    totalTracks = 0,
                    trackTitle = "Fetching playlist tracks..."
                )
                try {
                    val playlist = importSpotifyPlaylist(trimmed)
                    _importStatus.value = PlaylistImportStatus(
                        isImporting = false,
                        playlistName = playlist.title,
                        currentTrack = playlist.songCount,
                        totalTracks = playlist.songCount,
                        completedPlaylist = playlist
                    )
                } catch (e: Exception) {
                    _importStatus.value = PlaylistImportStatus(
                        isImporting = false,
                        errorMessage = e.message ?: "Failed to import playlist. Please ensure the playlist is public."
                    )
                }
            }
        } else {
            startBackgroundImportByName(trimmed)
        }
    }

    fun startBackgroundImportByName(query: String) {
        if (_importStatus.value.isImporting) return
        repositoryScope.launch {
            _importStatus.value = PlaylistImportStatus(
                isImporting = true,
                playlistName = query,
                currentTrack = 0,
                totalTracks = 0,
                trackTitle = "Searching for '$query' playlists..."
            )
            try {
                val matches = searchPlaylists(query)
                if (matches.isEmpty()) {
                    _importStatus.value = PlaylistImportStatus(
                        isImporting = false,
                        errorMessage = "No playlist found matching '$query'. Try another name or keyword."
                    )
                    return@launch
                }
                importSearchedPlaylist(matches.first())
            } catch (e: Exception) {
                _importStatus.value = PlaylistImportStatus(
                    isImporting = false,
                    errorMessage = e.message ?: "Failed to import playlist by name."
                )
            }
        }
    }

    fun startBackgroundImportPlaylist(playlist: Playlist) {
        if (_importStatus.value.isImporting) return
        repositoryScope.launch {
            try {
                importSearchedPlaylist(playlist)
            } catch (e: Exception) {
                _importStatus.value = PlaylistImportStatus(
                    isImporting = false,
                    errorMessage = e.message ?: "Failed to import playlist."
                )
            }
        }
    }

    fun getFavorites(): Flow<List<Song>> = songDao.getFavorites().map { list ->
        list.map { it.toSong() }
    }

    fun getDownloads(): Flow<List<Song>> = songDao.getDownloads().map { list ->
        list.map { it.toSong() }
    }

    fun getRecentlyPlayed(): Flow<List<Song>> = songDao.getRecentlyPlayed().map { list ->
        list.map { it.toSong() }
    }

    fun getUserPlaylists(): Flow<List<Playlist>> = playlistDao.getAllPlaylists().map { entities ->
        entities.map { entity ->
            val songs = parseSongsJson(entity.songsJson)
            Playlist(
                id = entity.id,
                title = entity.title,
                subtitle = if (entity.isSpotifyImport) "Imported Playlist • ${entity.songCount} songs" else "${entity.songCount} songs",
                coverUrl = entity.coverUrl.ifBlank { songs.firstOrNull()?.coverUrl.orEmpty() },
                songCount = entity.songCount,
                songs = songs
            )
        }
    }

    suspend fun searchSongs(query: String, moodTag: String = "Spring Vibe"): List<Song> = withContext(Dispatchers.IO) {
        try {
            // Priority 1: Direct JioSaavn API with decrypted high-speed 320kbps CDN audio
            val directSongs = JioSaavnDirectEngine.searchSongs(query, moodTag)
            if (directSongs.isNotEmpty()) {
                return@withContext directSongs
            }
        } catch (e: Exception) {
            android.util.Log.e("MusicRepository", "JioSaavnDirectEngine search error: ${e.message}")
        }

        try {
            // Priority 2: Secondary NetworkClient proxy
            val response = NetworkClient.apiService.searchSongs(query)
            val songs = response.data?.results?.map { dto ->
                NetworkClient.mapSongDtoToDomain(dto, moodTag)
            } ?: emptyList()

            if (songs.isNotEmpty()) return@withContext songs
        } catch (_: Exception) {}

        fallbackSearch(query)
    }

    data class CuratedPlaylist(
        val id: String,
        val title: String,
        val subtitle: String,
        val coverUrl: String,
        val songCount: Int,
        val keywords: List<String>
    )

    private val curatedPlaylists = listOf(
        CuratedPlaylist(
            id = "spotify_37i9dQZF1DX5q67ZpWyRrZ",
            title = "Indie India",
            subtitle = "The best of the Indian indie scene • 75 songs",
            coverUrl = "https://image-cdn-fa.spotifycdn.com/image/ab67706f00000002656f14776fbb39aa2ae900f0",
            songCount = 75,
            keywords = listOf("indie", "india", "indie india", "hindi indie", "prateek kuhad", "anuv jain")
        ),
        CuratedPlaylist(
            id = "spotify_37i9dQZF1DX0XUfTFmNBRM",
            title = "Bollywood Butter",
            subtitle = "Finest modern Bollywood hits • 50 songs",
            coverUrl = "https://i.scdn.co/image/ab67706f00000002f90a980757a3e4307ef19688",
            songCount = 50,
            keywords = listOf("bollywood", "butter", "hindi", "arijit", "top bollywood")
        ),
        CuratedPlaylist(
            id = "spotify_37i9dQZF1DXcBWIGoYBM5M",
            title = "Today's Top Hits",
            subtitle = "Hottest tracks worldwide • 50 songs",
            coverUrl = "https://i.scdn.co/image/ab67706f000000029bb644264627d33e5c9a4185",
            songCount = 50,
            keywords = listOf("top hits", "today", "global", "english", "pop")
        ),
        CuratedPlaylist(
            id = "spotify_37i9dQZF1DXdLEN7aqioXM",
            title = "Chill Lofi Beats",
            subtitle = "Beats to relax, study, and vibe to • 60 songs",
            coverUrl = "https://i.scdn.co/image/ab67706f00000002bca487b3370f80b2a8d5f308",
            songCount = 60,
            keywords = listOf("lofi", "chill", "lo-fi", "study", "relax")
        ),
        CuratedPlaylist(
            id = "spotify_37i9dQZF1DX5cZuAHLNjGz",
            title = "Punjabi 101",
            subtitle = "Essential Punjabi chartbusters • 50 songs",
            coverUrl = "https://i.scdn.co/image/ab67706f000000024f2b9637c35ae97d91788267",
            songCount = 50,
            keywords = listOf("punjabi", "punjabi 101", "bhangra", "diljit", "sidhu")
        ),
        CuratedPlaylist(
            id = "spotify_37i9dQZF1DX4WYpdgoIcn6",
            title = "I-Pop Superhits",
            subtitle = "Biggest Indian Pop songs • 50 songs",
            coverUrl = "https://i.scdn.co/image/ab67706f00000002c918ef8f8eb04a7541662589",
            songCount = 50,
            keywords = listOf("ipop", "i-pop", "pop", "indian pop", "darshan raval")
        ),
        CuratedPlaylist(
            id = "spotify_37i9dQZF1DX6MVGQ4S8ZtC",
            title = "Bollywood Romance",
            subtitle = "Soulful love melodies • 50 songs",
            coverUrl = "https://i.scdn.co/image/ab67706f00000002b80456209214df9275cb1a5e",
            songCount = 50,
            keywords = listOf("romance", "romantic", "love", "bollywood romance", "soulful")
        ),
        CuratedPlaylist(
            id = "spotify_37i9dQZEVXbMDoHDwVN2tF",
            title = "Global Top 50",
            subtitle = "Most played songs around the world • 50 songs",
            coverUrl = "https://i.scdn.co/image/ab67706f00000002a24c58cf3e20d2d3e9c9c34a",
            songCount = 50,
            keywords = listOf("global", "top 50", "charts", "trending")
        ),
        CuratedPlaylist(
            id = "spotify_37i9dQZF1DX76Wlfdnj7AP",
            title = "Workout Energy",
            subtitle = "Beast mode pump up music • 50 songs",
            coverUrl = "https://i.scdn.co/image/ab67706f000000025f385c7d0d0fbb3298a0c20a",
            songCount = 50,
            keywords = listOf("workout", "gym", "energy", "fitness", "motivation")
        ),
        CuratedPlaylist(
            id = "spotify_37i9dQZF1DX3O7zMhH00lP",
            title = "Desi Hip Hop",
            subtitle = "South Asian hip hop & underground rap • 50 songs",
            coverUrl = "https://i.scdn.co/image/ab67706f0000000253f937cb68e146eb20199e43",
            songCount = 50,
            keywords = listOf("desi hip hop", "rap", "hip hop", "emiway", "divine", "seedhe maut", "krsna")
        ),
        CuratedPlaylist(
            id = "spotify_37i9dQZF1DX14qpQjK0v2E",
            title = "Tollywood Top 50",
            subtitle = "Telugu chartbusters • 50 songs",
            coverUrl = "https://i.scdn.co/image/ab67706f000000027f3b89fa5a9ebf5aa7c90b82",
            songCount = 50,
            keywords = listOf("tollywood", "telugu", "top telugu", "anirudh", "dsp")
        ),
        CuratedPlaylist(
            id = "spotify_37i9dQZF1DX1i3hvzJpcNu",
            title = "Kollywood Top 50",
            subtitle = "Tamil chartbusters • 50 songs",
            coverUrl = "https://i.scdn.co/image/ab67706f0000000201d4a9ec0fefc6cfbf456bf1",
            songCount = 50,
            keywords = listOf("kollywood", "tamil", "top tamil", "anirudh", "ar rahman")
        ),
        CuratedPlaylist(
            id = "spotify_37i9dQZF1DX5Ejj0EkURtP",
            title = "All Out 2010s",
            subtitle = "The biggest hits of the decade • 75 songs",
            coverUrl = "https://i.scdn.co/image/ab67706f0000000236a28ff7227e7d6cfec390be",
            songCount = 75,
            keywords = listOf("2010s", "decade", "nostalgia", "throwback")
        ),
        CuratedPlaylist(
            id = "spotify_37i9dQZF1DX4o1oenSJRJd",
            title = "All Out 2000s",
            subtitle = "Defining tracks of the 2000s • 75 songs",
            coverUrl = "https://i.scdn.co/image/ab67706f00000002e20b327b0365779c1186e246",
            songCount = 75,
            keywords = listOf("2000s", "throwback", "retro", "nostalgia")
        )
    )

    suspend fun searchPlaylists(query: String): List<Playlist> = withContext(Dispatchers.IO) {
        val qClean = query.trim().lowercase()
        val combinedResults = mutableListOf<Playlist>()
        val seenIds = mutableSetOf<String>()

        // 1. Check curated Spotify & Global catalogs
        val curatedMatches = curatedPlaylists.filter { entry ->
            entry.title.lowercase().contains(qClean) ||
                    entry.keywords.any { qClean.contains(it) || it.contains(qClean) }
        }.map { entry ->
            Playlist(
                id = entry.id,
                title = entry.title,
                subtitle = entry.subtitle,
                coverUrl = entry.coverUrl,
                songCount = entry.songCount,
                songs = emptyList()
            )
        }
        for (p in curatedMatches) {
            if (seenIds.add(p.id)) combinedResults.add(p)
        }

        // 2. Priority: Direct JioSaavn Playlists
        try {
            val directPlaylists = JioSaavnDirectEngine.searchPlaylists(query)
            for (p in directPlaylists) {
                if (seenIds.add(p.id)) combinedResults.add(p)
            }
        } catch (e: Exception) {
            android.util.Log.e("MusicRepository", "JioSaavnDirectEngine playlist error: ${e.message}")
        }

        // 3. Fallback to secondary network proxy if needed
        if (combinedResults.isEmpty()) {
            try {
                val response = NetworkClient.apiService.searchPlaylists(query)
                val playlists = response.data?.results?.map { dto ->
                    NetworkClient.mapPlaylistDtoToDomain(dto)
                } ?: emptyList()
                for (p in playlists) {
                    if (seenIds.add(p.id)) combinedResults.add(p)
                }
            } catch (_: Exception) {}
        }

        combinedResults
    }

    suspend fun getPlaylistDetails(playlistId: String): Playlist = withContext(Dispatchers.IO) {
        // Check user/imported playlists first
        val userPlaylist = playlistDao.getPlaylistById(playlistId)
        if (userPlaylist != null) {
            val songs = parseSongsJson(userPlaylist.songsJson)
            return@withContext Playlist(
                id = userPlaylist.id,
                title = userPlaylist.title,
                subtitle = if (userPlaylist.isSpotifyImport) "Imported Playlist" else userPlaylist.description,
                coverUrl = userPlaylist.coverUrl.ifBlank { songs.firstOrNull()?.coverUrl.orEmpty() },
                songCount = songs.size,
                songs = songs
            )
        }

        // Check preset playlists
        val preset = PresetCatalog.PRESET_PLAYLISTS.find { it.id == playlistId }
        if (preset != null) return@withContext preset

        // Query direct JioSaavn API
        try {
            val direct = JioSaavnDirectEngine.getPlaylistDetails(playlistId)
            if (direct != null && direct.songs.isNotEmpty()) {
                return@withContext direct
            }
        } catch (_: Exception) {}

        // Query secondary API
        try {
            val response = NetworkClient.apiService.getPlaylistDetail(playlistId)
            val data = response.data
            if (data != null) {
                val songs = data.songs?.map { dto ->
                    NetworkClient.mapSongDtoToDomain(dto, data.title ?: "Playlist")
                } ?: emptyList()

                val title = data.name ?: data.title ?: "Playlist"
                return@withContext Playlist(
                    id = data.id ?: playlistId,
                    title = title,
                    subtitle = "${songs.size} songs",
                    coverUrl = songs.firstOrNull()?.coverUrl ?: "",
                    songCount = songs.size,
                    songs = songs
                )
            }
        } catch (_: Exception) {}

        Playlist(id = playlistId, title = "Playlist", subtitle = "0 songs", coverUrl = "", songCount = 0, songs = emptyList())
    }

    suspend fun importSpotifyPlaylist(
        url: String,
        onProgress: ((current: Int, total: Int, trackName: String) -> Unit)? = null
    ): Playlist = withContext(Dispatchers.IO) {
        _importStatus.value = PlaylistImportStatus(
            isImporting = true,
            playlistName = "Reading playlist...",
            currentTrack = 0,
            totalTracks = 0,
            trackTitle = "Connecting..."
        )
        val result = SpotifyImporter.importPlaylist(url)
        val total = result.tracks.size
        _importStatus.value = PlaylistImportStatus(
            isImporting = true,
            playlistName = result.title,
            currentTrack = 0,
            totalTracks = total,
            trackTitle = "Matching songs in high speed..."
        )

        val completedCount = java.util.concurrent.atomic.AtomicInteger(0)
        val resolvedSongs = mutableListOf<Song>()

        // Concurrently match songs in batches of 6 for 6x faster import
        val chunks = result.tracks.chunked(6)
        for (chunk in chunks) {
            val deferredList = chunk.mapIndexed { idxInChunk, track ->
                val trackIndex = resolvedSongs.size + idxInChunk
                async(Dispatchers.IO) {
                    var streamUrl = ""
                    var coverUrl = track.cover
                    var audioQualities = listOf<AudioQuality>()

                    var sourceEngine = "jiosaavn"
                    try {
                        val searchRes = JioSaavnDirectEngine.searchSongs("${track.title} ${track.artist}")
                        val firstMatch = searchRes.firstOrNull()
                        if (firstMatch != null) {
                            streamUrl = firstMatch.audioUrl
                            if (coverUrl.isBlank()) coverUrl = firstMatch.coverUrl
                            audioQualities = firstMatch.qualityList
                            sourceEngine = "jiosaavn"
                        } else {
                            val legacyRes = NetworkClient.apiService.searchSongs("${track.title} ${track.artist}")
                            val legacyMatch = legacyRes.data?.results?.firstOrNull()
                            if (legacyMatch != null) {
                                val domainSong = NetworkClient.mapSongDtoToDomain(legacyMatch, "Spring Import")
                                streamUrl = domainSong.audioUrl
                                if (coverUrl.isBlank()) coverUrl = domainSong.coverUrl
                                audioQualities = domainSong.qualityList
                                sourceEngine = "jiosaavn"
                            }
                        }
                    } catch (_: Exception) {}

                    // Fallback to Jukebox streaming API if JioSaavn doesn't have the song!
                    if (streamUrl.isBlank()) {
                        streamUrl = JukeboxClient.buildStreamUrl(
                            title = track.title,
                            artist = track.artist,
                            spotifyTrackId = track.id,
                            context = context
                        )
                        sourceEngine = "jukebox"
                    }

                    val current = completedCount.incrementAndGet()
                    onProgress?.invoke(current, total, track.title)
                    _importStatus.value = _importStatus.value.copy(
                        isImporting = true,
                        playlistName = result.title,
                        currentTrack = current,
                        totalTracks = total,
                        trackTitle = track.title
                    )

                    Song(
                        id = "sp_${track.id.ifBlank { System.currentTimeMillis().toString() + trackIndex }}",
                        title = track.title,
                        artist = track.artist,
                        album = track.album,
                        coverUrl = coverUrl,
                        audioUrl = streamUrl,
                        durationMs = if (track.durationMs > 0) track.durationMs else 180000L,
                        mood = "Spring",
                        sourceEngine = sourceEngine,
                        spotifyId = track.id.ifBlank { null },
                        qualityList = audioQualities
                    )
                }
            }
            resolvedSongs.addAll(deferredList.awaitAll())
        }

        val playlistId = "spring_${System.currentTimeMillis()}"
        val playlist = Playlist(
            id = playlistId,
            title = result.title,
            subtitle = "Imported Playlist • ${resolvedSongs.size} songs",
            coverUrl = result.thumbnail.ifBlank { resolvedSongs.firstOrNull()?.coverUrl.orEmpty() },
            songCount = resolvedSongs.size,
            songs = resolvedSongs
        )

        // Save into Room database
        val entity = UserPlaylistEntity(
            id = playlistId,
            title = playlist.title,
            description = "Imported from ${result.playlistUrl}",
            coverUrl = playlist.coverUrl,
            songCount = resolvedSongs.size,
            songsJson = serializeSongsJson(resolvedSongs),
            isSpotifyImport = true
        )
        playlistDao.insertPlaylist(entity)
        firestoreSyncManager?.savePlaylist(entity)

        _importStatus.value = PlaylistImportStatus(
            isImporting = false,
            playlistName = playlist.title,
            currentTrack = resolvedSongs.size,
            totalTracks = resolvedSongs.size,
            completedPlaylist = playlist
        )

        playlist
    }

    suspend fun importSearchedPlaylist(playlist: Playlist): Playlist = withContext(Dispatchers.IO) {
        val isSpotify = playlist.id.startsWith("spotify_") ||
                playlist.id.startsWith("sp_") ||
                playlist.id.matches(Regex("^[a-zA-Z0-9]{22}$")) ||
                playlist.id.contains("spotify.com")

        if (isSpotify) {
            val spotifyId = playlist.id.removePrefix("spotify_").removePrefix("sp_")
            val url = if (spotifyId.startsWith("http")) spotifyId else "https://open.spotify.com/playlist/$spotifyId"
            return@withContext importSpotifyPlaylist(url)
        }

        _importStatus.value = PlaylistImportStatus(
            isImporting = true,
            playlistName = playlist.title,
            currentTrack = 0,
            totalTracks = playlist.songCount.coerceAtLeast(1),
            trackTitle = "Fetching playlist tracks..."
        )
        val fullPlaylist = getPlaylistDetails(playlist.id)
        val songs = if (fullPlaylist.songs.isNotEmpty()) fullPlaylist.songs else playlist.songs
        val playlistId = "spring_${System.currentTimeMillis()}"
        val imported = Playlist(
            id = playlistId,
            title = fullPlaylist.title.ifBlank { playlist.title },
            subtitle = "Imported Playlist • ${songs.size} songs",
            coverUrl = fullPlaylist.coverUrl.ifBlank { playlist.coverUrl.ifBlank { songs.firstOrNull()?.coverUrl.orEmpty() } },
            songCount = songs.size,
            songs = songs
        )
        val entity = UserPlaylistEntity(
            id = playlistId,
            title = imported.title,
            description = "Imported by name: ${imported.title}",
            coverUrl = imported.coverUrl,
            songCount = songs.size,
            songsJson = serializeSongsJson(songs),
            isSpotifyImport = true
        )
        playlistDao.insertPlaylist(entity)
        firestoreSyncManager?.savePlaylist(entity)
        _importStatus.value = PlaylistImportStatus(
            isImporting = false,
            playlistName = imported.title,
            currentTrack = songs.size,
            totalTracks = songs.size,
            completedPlaylist = imported
        )
        imported
    }

    suspend fun createCustomPlaylist(title: String, description: String = ""): Playlist = withContext(Dispatchers.IO) {
        val id = "pl_custom_${System.currentTimeMillis()}"
        val entity = UserPlaylistEntity(
            id = id,
            title = title,
            description = description,
            coverUrl = "",
            songCount = 0,
            songsJson = "[]",
            isSpotifyImport = false
        )
        playlistDao.insertPlaylist(entity)
        firestoreSyncManager?.savePlaylist(entity)
        Playlist(
            id = id,
            title = title,
            subtitle = "0 songs",
            coverUrl = "",
            songCount = 0,
            songs = emptyList()
        )
    }

    suspend fun deletePlaylist(playlistId: String) = withContext(Dispatchers.IO) {
        playlistDao.deletePlaylist(playlistId)
        firestoreSyncManager?.deletePlaylist(playlistId)
    }

    suspend fun toggleFavorite(song: Song): Boolean = withContext(Dispatchers.IO) {
        val existing = songDao.getSongById(song.id)
        val newFav = if (existing != null) !existing.isFavorite else true

        if (existing != null) {
            songDao.updateFavorite(song.id, newFav)
        } else {
            songDao.upsertSong(
                SavedSongEntity.fromSong(song, isFavorite = newFav)
            )
        }
        firestoreSyncManager?.syncLikedSong(song, newFav)
        newFav
    }


    suspend fun downloadSong(song: Song): Boolean = withContext(Dispatchers.IO) {
        val filePath = MediaCacheManager.downloadSongDirectly(context, song)
        if (filePath != null) {
            val existing = songDao.getSongById(song.id)
            if (existing != null) {
                songDao.updateDownloadStatus(song.id, true, filePath)
            } else {
                songDao.upsertSong(
                    SavedSongEntity.fromSong(
                        song,
                        isDownloaded = true,
                        localFilePath = filePath
                    )
                )
            }
            true
        } else {
            false
        }
    }

    suspend fun deleteDownload(song: Song): Boolean = withContext(Dispatchers.IO) {
        val existing = songDao.getSongById(song.id)
        if (existing?.localFilePath != null) {
            MediaCacheManager.deleteDownloadedSong(existing.localFilePath)
            songDao.updateDownloadStatus(song.id, false, null)
            true
        } else {
            false
        }
    }

    suspend fun recordPlayed(song: Song) = withContext(Dispatchers.IO) {
        val existing = songDao.getSongById(song.id)
        val now = System.currentTimeMillis()
        if (existing != null) {
            songDao.updateLastPlayed(song.id, now)
        } else {
            songDao.upsertSong(
                SavedSongEntity.fromSong(song, lastPlayedTimestamp = now)
            )
        }
    }

    private fun fallbackSearch(query: String): List<Song> {
        val cleanQuery = query.trim()
        if (cleanQuery.isBlank()) return emptyList()
        val streamUrl = JukeboxClient.buildStreamUrl(cleanQuery, "Spring Fallback", context = context)
        return listOf(
            Song(
                id = "jb_${System.currentTimeMillis()}",
                title = cleanQuery.replaceFirstChar { it.uppercase() },
                artist = "Spring Fallback",
                album = "Direct Stream",
                coverUrl = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=500&q=80",
                audioUrl = streamUrl,
                durationMs = 210000L,
                mood = "Spring",
                sourceEngine = "jukebox"
            )
        )
    }

    private fun serializeSongsJson(songs: List<Song>): String {
        val array = JSONArray()
        for (s in songs) {
            val obj = JSONObject().apply {
                put("id", s.id)
                put("title", s.title)
                put("artist", s.artist)
                put("album", s.album)
                put("coverUrl", s.coverUrl)
                put("audioUrl", s.audioUrl)
                put("durationMs", s.durationMs)
                put("mood", s.mood)
                put("sourceEngine", s.sourceEngine)
            }
            array.put(obj)
        }
        return array.toString()
    }

    private fun parseSongsJson(jsonStr: String): List<Song> {
        if (jsonStr.isBlank()) return emptyList()
        val list = mutableListOf<Song>()
        try {
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    Song(
                        id = obj.optString("id"),
                        title = obj.optString("title"),
                        artist = obj.optString("artist"),
                        album = obj.optString("album", "Spring"),
                        coverUrl = obj.optString("coverUrl"),
                        audioUrl = obj.optString("audioUrl"),
                        durationMs = obj.optLong("durationMs", 180000L),
                        mood = obj.optString("mood", "Spring"),
                        sourceEngine = obj.optString("sourceEngine", "spring_import")
                    )
                )
            }
        } catch (_: Exception) {}
        return list
    }
}
