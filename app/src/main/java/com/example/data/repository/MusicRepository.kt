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

    fun startBackgroundImport(url: String) {
        if (_importStatus.value.isImporting) return
        repositoryScope.launch {
            _importStatus.value = PlaylistImportStatus(
                isImporting = true,
                playlistName = "Connecting...",
                currentTrack = 0,
                totalTracks = 0,
                trackTitle = "Fetching playlist tracks..."
            )
            try {
                val playlist = importSpotifyPlaylist(url)
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
            val response = NetworkClient.apiService.searchSongs(query)
            val songs = response.data?.results?.map { dto ->
                NetworkClient.mapSongDtoToDomain(dto, moodTag)
            } ?: emptyList()

            if (songs.isNotEmpty()) songs else fallbackSearch(query)
        } catch (e: Exception) {
            fallbackSearch(query)
        }
    }

    suspend fun searchPlaylists(query: String): List<Playlist> = withContext(Dispatchers.IO) {
        try {
            val response = NetworkClient.apiService.searchPlaylists(query)
            val playlists = response.data?.results?.map { dto ->
                NetworkClient.mapPlaylistDtoToDomain(dto)
            } ?: emptyList()

            if (playlists.isNotEmpty()) playlists else emptyList()
        } catch (e: Exception) {
            emptyList()
        }
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

        // Query remote API
        try {
            val response = NetworkClient.apiService.getPlaylistDetail(playlistId)
            val data = response.data
            if (data != null) {
                val songs = data.songs?.map { dto ->
                    NetworkClient.mapSongDtoToDomain(dto, data.title ?: "Playlist")
                } ?: emptyList()

                val title = data.name ?: data.title ?: "Playlist"
                Playlist(
                    id = data.id ?: playlistId,
                    title = title,
                    subtitle = "${songs.size} songs",
                    coverUrl = songs.firstOrNull()?.coverUrl ?: "",
                    songCount = songs.size,
                    songs = songs
                )
            } else {
                Playlist(id = playlistId, title = "Playlist", subtitle = "0 songs", coverUrl = "", songCount = 0, songs = emptyList())
            }
        } catch (e: Exception) {
            Playlist(id = playlistId, title = "Playlist", subtitle = "0 songs", coverUrl = "", songCount = 0, songs = emptyList())
        }
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

                    try {
                        val searchRes = NetworkClient.apiService.searchSongs("${track.title} ${track.artist}")
                        val firstMatch = searchRes.data?.results?.firstOrNull()
                        if (firstMatch != null) {
                            val domainSong = NetworkClient.mapSongDtoToDomain(firstMatch, "Spring Import")
                            streamUrl = domainSong.audioUrl
                            if (coverUrl.isBlank()) coverUrl = domainSong.coverUrl
                            audioQualities = domainSong.qualityList
                        }
                    } catch (_: Exception) {}

                    // Fallback audio if not resolved directly
                    if (streamUrl.isBlank() && PresetCatalog.PRESET_SONGS.isNotEmpty()) {
                        val presetFallback = PresetCatalog.PRESET_SONGS[trackIndex % PresetCatalog.PRESET_SONGS.size]
                        streamUrl = presetFallback.audioUrl
                        if (coverUrl.isBlank()) coverUrl = presetFallback.coverUrl
                        audioQualities = presetFallback.qualityList
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
                        sourceEngine = "spring_import",
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
        return emptyList()
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
