package com.example.service

import android.app.PendingIntent
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.net.Uri
import android.os.Bundle
import androidx.annotation.OptIn
import androidx.core.content.ContextCompat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionParameters
import androidx.media3.common.util.BitmapLoader
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSourceBitmapLoader
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.extractor.DefaultExtractorsFactory
import androidx.media3.session.CacheBitmapLoader
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionError
import com.example.MainActivity
import com.example.R
import com.example.SpringApplication
import com.example.data.local.MediaCacheManager
import com.example.data.model.Playlist
import com.example.data.model.Song
import com.example.data.network.JukeboxClient
import com.example.data.network.PresetCatalog
import com.example.data.repository.MusicRepository
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URLDecoder
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap

@OptIn(UnstableApi::class)
class SpringMediaLibraryService : MediaLibraryService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    lateinit var player: ExoPlayer
    private var mediaLibrarySession: MediaLibrarySession? = null

    // Thread-safe caches for resolving MediaItems across Android Auto / MediaBrowser IPC
    private val songCache = ConcurrentHashMap<String, Song>()
    private val songParentCache = ConcurrentHashMap<String, String>()
    private val containerSongsCache = ConcurrentHashMap<String, List<Song>>()
    private val searchResultsCache = ConcurrentHashMap<String, List<Song>>()

    companion object {
        const val ROOT_ID = "spring_root"
        const val RECENT_ROOT_ID = "spring_recent_root"

        // Top-level 4 Android Auto tabs
        const val CATEGORY_HOME = "cat_home"
        const val CATEGORY_LIBRARY = "cat_library"
        const val CATEGORY_PLAYLISTS = "cat_playlists"
        const val CATEGORY_EXPLORE = "cat_explore"

        // Sub-categories inside Library & Explore
        const val CATEGORY_FAVORITES = "cat_favorites"
        const val CATEGORY_DOWNLOADS = "cat_downloads"
        const val CATEGORY_RECENT = "cat_recent"
        const val CATEGORY_ARTISTS = "cat_artists"
        const val CATEGORY_ALBUMS = "cat_albums"
        const val CATEGORY_TOP_CHARTS = "cat_top_charts"

        // Prefixes for dynamic folders
        private const val PREFIX_PLAYLIST = "playlist_"
        private const val PREFIX_MOOD = "mood_"
        private const val PREFIX_ARTIST = "artist_"
        private const val PREFIX_ALBUM = "album_"
        private const val PREFIX_REC_SECTION = "rec_sec_"

        // Android Auto MediaConstants keys (consistent across Media3 & legacy MediaBrowser)
        private const val KEY_CONTENT_STYLE_SUPPORTED = "android.media.browse.CONTENT_STYLE_SUPPORTED"
        private const val KEY_CONTENT_STYLE_BROWSABLE = "android.media.browse.CONTENT_STYLE_BROWSABLE_HINT"
        private const val KEY_CONTENT_STYLE_PLAYABLE = "android.media.browse.CONTENT_STYLE_PLAYABLE_HINT"
        private const val KEY_SEARCH_SUPPORTED = "android.media.browse.SEARCH_SUPPORTED"
        private const val CONTENT_STYLE_LIST = 1
        private const val CONTENT_STYLE_GRID = 2

        var instance: SpringMediaLibraryService? = null
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        val audioAttributes = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .build()

        val cacheDataSourceFactory = MediaCacheManager.getCacheDataSourceFactory(this)
        val extractorsFactory = DefaultExtractorsFactory()
            .setConstantBitrateSeekingEnabled(true)

        val mediaSourceFactory = DefaultMediaSourceFactory(this, extractorsFactory)
            .setDataSourceFactory(cacheDataSourceFactory)

        // Use deterministic standard synchronous decoding and 16-bit PCM AudioTrack without offload/playbackParams drift
        val renderersFactory = DefaultRenderersFactory(this)
            .setEnableAudioFloatOutput(false)
            .setEnableAudioTrackPlaybackParams(false)

        val trackSelector = DefaultTrackSelector(this).apply {
            parameters = buildUponParameters()
                .setAudioOffloadPreferences(
                    TrackSelectionParameters.AudioOffloadPreferences.Builder()
                        .setAudioOffloadMode(TrackSelectionParameters.AudioOffloadPreferences.AUDIO_OFFLOAD_MODE_DISABLED)
                        .build()
                )
                .build()
        }

        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                /* minBufferMs = */ 25_000,
                /* maxBufferMs = */ 60_000,
                /* bufferForPlaybackMs = */ 2_000,
                /* bufferForPlaybackAfterRebufferMs = */ 3_500
            )
            .setBackBuffer(10_000, true)
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()

        player = ExoPlayer.Builder(this, renderersFactory)
            .setMediaSourceFactory(mediaSourceFactory)
            .setTrackSelector(trackSelector)
            .setLoadControl(loadControl)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .build()

        player.playbackParameters = PlaybackParameters(1.0f, 1.0f)

        player.addListener(object : Player.Listener {
            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                android.util.Log.e("SpringMediaService", "Player error encountered: ${error.errorCodeName} - ${error.message}", error)
                val app = applicationContext as? SpringApplication
                app?.playerManager?.onServicePlayerError(player, error)
            }
        })

        val sessionActivityIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            sessionActivityIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        mediaLibrarySession = MediaLibrarySession.Builder(this, player, SpringLibrarySessionCallback())
            .setSessionActivity(pendingIntent)
            .setBitmapLoader(CacheBitmapLoader(VectorAwareBitmapLoader(this)))
            .setId("SpringMediaSession")
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? {
        return mediaLibrarySession
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val prefs = getSharedPreferences("spring_prefs", MODE_PRIVATE)
        val stopOnTaskRemoved = prefs.getBoolean("stop_on_task_removed", false)
        if (stopOnTaskRemoved || !player.playWhenReady || player.mediaItemCount == 0) {
            player.pause()
            player.stop()
            stopSelf()
        }
    }

    override fun onDestroy() {
        instance = null
        serviceScope.cancel()
        mediaLibrarySession?.run {
            player.release()
            release()
            mediaLibrarySession = null
        }
        super.onDestroy()
    }

    private fun drawableUri(resId: Int): Uri {
        return Uri.parse("${ContentResolver.SCHEME_ANDROID_RESOURCE}://$packageName/$resId")
    }

    private fun getRepository(): MusicRepository? {
        return (applicationContext as? SpringApplication)?.repository
    }

    private fun applyPreferredQuality(song: Song): Song {
        if (!song.localFilePath.isNullOrBlank() || song.qualityList.isEmpty()) return song
        val prefs = getSharedPreferences("spring_prefs", MODE_PRIVATE)
        val targetQuality = prefs.getString("streaming_quality", "320kbps") ?: "320kbps"
        val targetBitrate = targetQuality.replace("kbps", "", ignoreCase = true).trim()
        val matched = song.qualityList.find {
            it.label.contains(targetBitrate, ignoreCase = true) && it.url.isNotBlank()
        }
        return if (matched != null && matched.url != song.audioUrl) {
            song.copy(audioUrl = matched.url)
        } else {
            song
        }
    }

    private fun cacheSongsForParent(parentId: String, songs: List<Song>) {
        if (songs.isEmpty()) return
        containerSongsCache[parentId] = songs
        for (s in songs) {
            songCache[s.id] = s
            songParentCache[s.id] = parentId
        }
    }

    private suspend fun collectAllLibrarySongs(repo: MusicRepository): List<Song> {
        val favs = runCatching { repo.getFavorites().first() }.getOrDefault(emptyList())
        val dls = runCatching { repo.getDownloads().first() }.getOrDefault(emptyList())
        val recents = runCatching { repo.getRecentlyPlayed().first() }.getOrDefault(emptyList())
        val playlists = runCatching { repo.getUserPlaylistsSync() }.getOrDefault(emptyList())
        val combined = (favs + dls + recents + playlists.flatMap { it.songs }).distinctBy { it.id }
        combined.forEach { songCache[it.id] = it }
        return combined
    }

    private suspend fun resolveSongById(mediaId: String): Song? {
        if (mediaId.isBlank()) return null

        songCache[mediaId]?.let { return it }

        val app = applicationContext as? SpringApplication
        app?.playerManager?.queue?.value?.find { it.id == mediaId }?.let {
            songCache[mediaId] = it
            return it
        }
        app?.playerManager?.currentSong?.value?.takeIf { it.id == mediaId }?.let {
            songCache[mediaId] = it
            return it
        }

        val repo = app?.repository
        repo?.findSongById(mediaId)?.let {
            songCache[mediaId] = it
            return it
        }

        PresetCatalog.PRESET_SONGS.find { it.id == mediaId }?.let {
            songCache[mediaId] = it
            return it
        }

        return null
    }

    private suspend fun resolvePlayableSong(item: MediaItem): Song? {
        val searchQuery = item.requestMetadata.searchQuery?.trim()
        if (!searchQuery.isNullOrBlank() && item.mediaId.isBlank()) {
            val repo = getRepository()
            val found = repo?.searchSongs(searchQuery, "Voice Search").orEmpty()
            if (found.isNotEmpty()) {
                cacheSongsForParent("search:$searchQuery", found)
                return preparePlayableSong(found.first())
            }
        }

        val resolved = resolveSongById(item.mediaId) ?: Song.fromMediaItem(item) ?: return null
        return preparePlayableSong(resolved)
    }

    private fun preparePlayableSong(song: Song): Song {
        val withQuality = applyPreferredQuality(song)
        return if (withQuality.effectivePlayableUri.isNotBlank()) {
            withQuality
        } else {
            JukeboxClient.createJukeboxFallbackSong(withQuality, context = applicationContext)
        }
    }

    inner class SpringLibrarySessionCallback : MediaLibrarySession.Callback {

        override fun onGetLibraryRoot(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            params: LibraryParams?
        ): ListenableFuture<LibraryResult<MediaItem>> {
            val rootExtras = Bundle().apply {
                putBoolean(KEY_CONTENT_STYLE_SUPPORTED, true)
                putBoolean(KEY_SEARCH_SUPPORTED, true)
                putInt(KEY_CONTENT_STYLE_BROWSABLE, CONTENT_STYLE_LIST)
                putInt(KEY_CONTENT_STYLE_PLAYABLE, CONTENT_STYLE_LIST)
            }
            val libraryParams = LibraryParams.Builder()
                .setExtras(rootExtras)
                .setRecent(params?.isRecent == true)
                .setOffline(params?.isOffline == true)
                .setSuggested(params?.isSuggested == true)
                .build()

            val targetRootId = if (params?.isRecent == true) RECENT_ROOT_ID else ROOT_ID
            val rootTitle = if (params?.isRecent == true) "Recently Played" else "Spring Music"

            val rootMetadata = MediaMetadata.Builder()
                .setTitle(rootTitle)
                .setIsBrowsable(true)
                .setIsPlayable(false)
                .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_MIXED)
                .build()

            val rootItem = MediaItem.Builder()
                .setMediaId(targetRootId)
                .setMediaMetadata(rootMetadata)
                .build()

            return Futures.immediateFuture(LibraryResult.ofItem(rootItem, libraryParams))
        }

        override fun onGetChildren(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            parentId: String,
            page: Int,
            pageSize: Int,
            params: LibraryParams?
        ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
            val future = SettableFuture.create<LibraryResult<ImmutableList<MediaItem>>>()

            if (parentId == ROOT_ID) {
                val rootTabs = listOf(
                    createBrowsableCategory(
                        id = CATEGORY_HOME,
                        title = "Home",
                        subtitle = "Recent tracks, mixes & charts",
                        iconRes = R.drawable.ic_auto_home,
                        mediaType = MediaMetadata.MEDIA_TYPE_FOLDER_MIXED,
                        browsableStyle = CONTENT_STYLE_LIST,
                        playableStyle = CONTENT_STYLE_LIST
                    ),
                    createBrowsableCategory(
                        id = CATEGORY_LIBRARY,
                        title = "Library",
                        subtitle = "Liked songs, downloads, playlists & more",
                        iconRes = R.drawable.ic_auto_library,
                        mediaType = MediaMetadata.MEDIA_TYPE_FOLDER_MIXED,
                        browsableStyle = CONTENT_STYLE_LIST,
                        playableStyle = CONTENT_STYLE_LIST
                    ),
                    createBrowsableCategory(
                        id = CATEGORY_PLAYLISTS,
                        title = "Playlists",
                        subtitle = "Your custom & imported playlists",
                        iconRes = R.drawable.ic_auto_playlists,
                        mediaType = MediaMetadata.MEDIA_TYPE_FOLDER_PLAYLISTS,
                        browsableStyle = CONTENT_STYLE_GRID,
                        playableStyle = CONTENT_STYLE_LIST
                    ),
                    createBrowsableCategory(
                        id = CATEGORY_EXPLORE,
                        title = "Explore",
                        subtitle = "Moods, genres & top charts",
                        iconRes = R.drawable.ic_auto_explore,
                        mediaType = MediaMetadata.MEDIA_TYPE_FOLDER_GENRES,
                        browsableStyle = CONTENT_STYLE_GRID,
                        playableStyle = CONTENT_STYLE_LIST
                    )
                )
                future.set(LibraryResult.ofItemList(rootTabs, params))
                return future
            }

            serviceScope.launch {
                try {
                    val items = loadChildrenForParent(parentId)
                    future.set(LibraryResult.ofItemList(items, params))
                } catch (e: Exception) {
                    android.util.Log.e("SpringMediaService", "onGetChildren($parentId) failed: ${e.message}", e)
                    future.set(LibraryResult.ofItemList(emptyList(), params))
                }
            }
            return future
        }

        private suspend fun loadChildrenForParent(parentId: String): List<MediaItem> {
            val repo = getRepository() ?: return emptyList()

            return when {
                parentId == RECENT_ROOT_ID -> {
                    val recents = runCatching { repo.getRecentlyPlayed().first() }.getOrDefault(emptyList())
                    val fallback = if (recents.isEmpty()) repo.getTopChartsSync() else recents
                    val tracks = fallback.take(15)
                    cacheSongsForParent(RECENT_ROOT_ID, tracks)
                    tracks.map { it.toMediaItem(groupTitle = "Jump Back In") }
                }

                parentId == CATEGORY_HOME -> {
                    val recents = runCatching { repo.getRecentlyPlayed().first() }.getOrDefault(emptyList())
                    val recsState = repo.ensureRecommendationsLoaded()
                    val charts = repo.getTopChartsSync()

                    val homeSongs = mutableListOf<Song>()
                    val items = mutableListOf<MediaItem>()

                    // Show direct playable songs grouped cleanly for 1-tap car playback
                    val recentSlice = recents.take(6)
                    for (song in recentSlice) {
                        homeSongs.add(song)
                        items.add(song.toMediaItem(groupTitle = "Recently Played"))
                    }

                    val recSongs = (recsState.discoverWeekly + recsState.sections.flatMap { it.songs })
                        .distinctBy { it.id }
                        .filter { s -> homeSongs.none { it.id == s.id } }
                        .take(12)
                    for (song in recSongs) {
                        homeSongs.add(song)
                        items.add(song.toMediaItem(groupTitle = "Recommended For You"))
                    }

                    val chartSlice = charts
                        .filter { s -> homeSongs.none { it.id == s.id } }
                        .take(10)
                    for (song in chartSlice) {
                        homeSongs.add(song)
                        items.add(song.toMediaItem(groupTitle = "Top Charts"))
                    }

                    // Also include browsable recommendation sections if available
                    for (section in recsState.sections.take(4)) {
                        if (section.songs.isNotEmpty()) {
                            val secId = "$PREFIX_REC_SECTION${encodeKey(section.categoryKey)}"
                            cacheSongsForParent(secId, section.songs)
                            items.add(
                                createBrowsableCategory(
                                    id = secId,
                                    title = section.title,
                                    subtitle = "${section.songs.size} tracks",
                                    coverUrl = section.songs.firstOrNull { it.coverUrl.isNotBlank() }?.coverUrl,
                                    iconRes = R.drawable.ic_auto_music,
                                    mediaType = MediaMetadata.MEDIA_TYPE_PLAYLIST,
                                    groupTitle = "Curated Mixes"
                                )
                            )
                        }
                    }

                    cacheSongsForParent(CATEGORY_HOME, homeSongs)
                    items
                }

                parentId == CATEGORY_LIBRARY -> {
                    val favs = runCatching { repo.getFavorites().first() }.getOrDefault(emptyList())
                    val dls = runCatching { repo.getDownloads().first() }.getOrDefault(emptyList())
                    val recents = runCatching { repo.getRecentlyPlayed().first() }.getOrDefault(emptyList())
                    val playlists = runCatching { repo.getUserPlaylistsSync() }.getOrDefault(emptyList())
                    val allLibrary = (favs + dls + recents + playlists.flatMap { it.songs }).distinctBy { it.id }
                    val artistCount = allLibrary.map { it.artist.ifBlank { "Various Artists" } }.distinct().size
                    val albumCount = allLibrary.map { it.album.ifBlank { it.title } }.distinct().size

                    val items = mutableListOf(
                        createBrowsableCategory(
                            id = CATEGORY_FAVORITES,
                            title = "Liked Songs",
                            subtitle = "${favs.size} favorite tracks",
                            iconRes = R.drawable.ic_auto_favorite,
                            mediaType = MediaMetadata.MEDIA_TYPE_PLAYLIST
                        ),
                        createBrowsableCategory(
                            id = CATEGORY_DOWNLOADS,
                            title = "Downloaded",
                            subtitle = "${dls.size} offline songs",
                            iconRes = R.drawable.ic_auto_download,
                            mediaType = MediaMetadata.MEDIA_TYPE_PLAYLIST
                        ),
                        createBrowsableCategory(
                            id = CATEGORY_PLAYLISTS,
                            title = "Playlists",
                            subtitle = "${playlists.size} playlists",
                            iconRes = R.drawable.ic_auto_playlists,
                            mediaType = MediaMetadata.MEDIA_TYPE_FOLDER_PLAYLISTS
                        ),
                        createBrowsableCategory(
                            id = CATEGORY_RECENT,
                            title = "Recently Played",
                            subtitle = "${recents.size} recent tracks",
                            iconRes = R.drawable.ic_auto_recent,
                            mediaType = MediaMetadata.MEDIA_TYPE_PLAYLIST
                        ),
                        createBrowsableCategory(
                            id = CATEGORY_ARTISTS,
                            title = "Artists",
                            subtitle = "$artistCount artists",
                            iconRes = R.drawable.ic_auto_artist,
                            mediaType = MediaMetadata.MEDIA_TYPE_FOLDER_ARTISTS
                        ),
                        createBrowsableCategory(
                            id = CATEGORY_ALBUMS,
                            title = "Albums",
                            subtitle = "$albumCount albums",
                            iconRes = R.drawable.ic_auto_album,
                            mediaType = MediaMetadata.MEDIA_TYPE_FOLDER_ALBUMS
                        )
                    )

                    // Surface top library tracks directly underneath for instant driving access
                    val quickLibraryTracks = (favs + dls + recents).distinctBy { it.id }.take(10)
                    if (quickLibraryTracks.isNotEmpty()) {
                        cacheSongsForParent(CATEGORY_LIBRARY, quickLibraryTracks)
                        quickLibraryTracks.forEach { song ->
                            items.add(song.toMediaItem(groupTitle = "Quick Play from Library"))
                        }
                    }

                    items
                }

                parentId == CATEGORY_PLAYLISTS -> {
                    val favs = runCatching { repo.getFavorites().first() }.getOrDefault(emptyList())
                    val playlists = runCatching { repo.getUserPlaylistsSync() }.getOrDefault(emptyList())
                    val items = mutableListOf<MediaItem>()

                    items.add(
                        createBrowsableCategory(
                            id = CATEGORY_FAVORITES,
                            title = "Liked Songs",
                            subtitle = "${favs.size} favorite tracks",
                            coverUrl = favs.firstOrNull { it.coverUrl.isNotBlank() }?.coverUrl,
                            iconRes = R.drawable.ic_auto_favorite,
                            mediaType = MediaMetadata.MEDIA_TYPE_PLAYLIST
                        )
                    )

                    for (playlist in playlists) {
                        val folderId = "$PREFIX_PLAYLIST${playlist.id}"
                        cacheSongsForParent(folderId, playlist.songs)
                        items.add(
                            createBrowsableCategory(
                                id = folderId,
                                title = playlist.title,
                                subtitle = playlist.subtitle.ifBlank { "${playlist.songs.size} songs" },
                                coverUrl = playlist.coverUrl.takeIf { it.isNotBlank() },
                                iconRes = R.drawable.ic_auto_playlists,
                                mediaType = MediaMetadata.MEDIA_TYPE_PLAYLIST
                            )
                        )
                    }
                    items
                }

                parentId == CATEGORY_FAVORITES -> {
                    val favs = runCatching { repo.getFavorites().first() }.getOrDefault(emptyList())
                    cacheSongsForParent(CATEGORY_FAVORITES, favs)
                    favs.map { it.toMediaItem() }
                }

                parentId == CATEGORY_DOWNLOADS -> {
                    val dls = runCatching { repo.getDownloads().first() }.getOrDefault(emptyList())
                    cacheSongsForParent(CATEGORY_DOWNLOADS, dls)
                    dls.map { it.toMediaItem() }
                }

                parentId == CATEGORY_RECENT -> {
                    val recents = runCatching { repo.getRecentlyPlayed().first() }.getOrDefault(emptyList())
                    cacheSongsForParent(CATEGORY_RECENT, recents)
                    recents.map { it.toMediaItem() }
                }

                parentId == CATEGORY_ARTISTS -> {
                    val allSongs = collectAllLibrarySongs(repo)
                    allSongs.groupBy { it.artist.ifBlank { "Various Artists" } }
                        .entries
                        .sortedByDescending { it.value.size }
                        .map { (artist, songs) ->
                            val artistId = "$PREFIX_ARTIST${encodeKey(artist)}"
                            cacheSongsForParent(artistId, songs)
                            createBrowsableCategory(
                                id = artistId,
                                title = artist,
                                subtitle = "${songs.size} songs",
                                coverUrl = songs.firstOrNull { it.coverUrl.isNotBlank() }?.coverUrl,
                                iconRes = R.drawable.ic_auto_artist,
                                mediaType = MediaMetadata.MEDIA_TYPE_ARTIST
                            )
                        }
                }

                parentId == CATEGORY_ALBUMS -> {
                    val allSongs = collectAllLibrarySongs(repo)
                    allSongs.groupBy { it.album.ifBlank { it.title } }
                        .entries
                        .sortedByDescending { it.value.size }
                        .map { (album, songs) ->
                            val albumId = "$PREFIX_ALBUM${encodeKey(album)}"
                            cacheSongsForParent(albumId, songs)
                            createBrowsableCategory(
                                id = albumId,
                                title = album,
                                subtitle = "${songs.firstOrNull()?.artist ?: "Spring Music"} • ${songs.size} songs",
                                coverUrl = songs.firstOrNull { it.coverUrl.isNotBlank() }?.coverUrl,
                                iconRes = R.drawable.ic_auto_album,
                                mediaType = MediaMetadata.MEDIA_TYPE_ALBUM
                            )
                        }
                }

                parentId == CATEGORY_EXPLORE -> {
                    val items = mutableListOf<MediaItem>()
                    items.add(
                        createBrowsableCategory(
                            id = CATEGORY_TOP_CHARTS,
                            title = "Top Charts",
                            subtitle = "Global Live Hits",
                            iconRes = R.drawable.ic_auto_explore,
                            mediaType = MediaMetadata.MEDIA_TYPE_PLAYLIST
                        )
                    )
                    for (mood in PresetCatalog.MOOD_CATEGORIES) {
                        items.add(
                            createBrowsableCategory(
                                id = "$PREFIX_MOOD${mood.id}",
                                title = mood.title,
                                subtitle = mood.description,
                                iconRes = R.drawable.ic_auto_music,
                                mediaType = MediaMetadata.MEDIA_TYPE_FOLDER_GENRES
                            )
                        )
                    }
                    items
                }

                parentId == CATEGORY_TOP_CHARTS -> {
                    val charts = repo.getTopChartsSync()
                    cacheSongsForParent(CATEGORY_TOP_CHARTS, charts)
                    charts.map { it.toMediaItem() }
                }

                parentId.startsWith(PREFIX_PLAYLIST) -> {
                    val playlistId = parentId.removePrefix(PREFIX_PLAYLIST)
                    val playlist: Playlist = repo.getPlaylistDetails(playlistId)
                    cacheSongsForParent(parentId, playlist.songs)
                    playlist.songs.map { it.toMediaItem() }
                }

                parentId.startsWith(PREFIX_MOOD) -> {
                    val moodId = parentId.removePrefix(PREFIX_MOOD)
                    val cached = containerSongsCache[parentId]
                    val songs = if (!cached.isNullOrEmpty()) {
                        cached
                    } else {
                        val mood = PresetCatalog.MOOD_CATEGORIES.find { it.id == moodId }
                        if (mood != null) {
                            repo.searchSongs(mood.searchTag, mood.title)
                        } else {
                            emptyList()
                        }
                    }
                    cacheSongsForParent(parentId, songs)
                    songs.map { it.toMediaItem() }
                }

                parentId.startsWith(PREFIX_ARTIST) -> {
                    val cached = containerSongsCache[parentId]
                    val songs = if (!cached.isNullOrEmpty()) {
                        cached
                    } else {
                        val artistName = decodeKey(parentId.removePrefix(PREFIX_ARTIST))
                        collectAllLibrarySongs(repo).filter {
                            it.artist.ifBlank { "Various Artists" }.equals(artistName, ignoreCase = true)
                        }
                    }
                    cacheSongsForParent(parentId, songs)
                    songs.map { it.toMediaItem() }
                }

                parentId.startsWith(PREFIX_ALBUM) -> {
                    val cached = containerSongsCache[parentId]
                    val songs = if (!cached.isNullOrEmpty()) {
                        cached
                    } else {
                        val albumName = decodeKey(parentId.removePrefix(PREFIX_ALBUM))
                        collectAllLibrarySongs(repo).filter {
                            it.album.ifBlank { it.title }.equals(albumName, ignoreCase = true)
                        }
                    }
                    cacheSongsForParent(parentId, songs)
                    songs.map { it.toMediaItem() }
                }

                parentId.startsWith(PREFIX_REC_SECTION) -> {
                    val cached = containerSongsCache[parentId]
                    val songs = if (!cached.isNullOrEmpty()) {
                        cached
                    } else {
                        val sectionKey = decodeKey(parentId.removePrefix(PREFIX_REC_SECTION))
                        val recs = repo.ensureRecommendationsLoaded()
                        recs.sections.find { it.categoryKey == sectionKey }?.songs.orEmpty()
                    }
                    cacheSongsForParent(parentId, songs)
                    songs.map { it.toMediaItem() }
                }

                else -> emptyList()
            }
        }

        override fun onGetItem(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            mediaId: String
        ): ListenableFuture<LibraryResult<MediaItem>> {
            val future = SettableFuture.create<LibraryResult<MediaItem>>()
            serviceScope.launch {
                try {
                    val song = resolveSongById(mediaId)
                    if (song != null) {
                        future.set(LibraryResult.ofItem(preparePlayableSong(song).toMediaItem(), null))
                        return@launch
                    }
                    val folderItem = when (mediaId) {
                        ROOT_ID -> createBrowsableCategory(ROOT_ID, "Spring Music", "", iconRes = R.drawable.ic_auto_music)
                        CATEGORY_HOME -> createBrowsableCategory(CATEGORY_HOME, "Home", "Recent & recommended", iconRes = R.drawable.ic_auto_home)
                        CATEGORY_LIBRARY -> createBrowsableCategory(CATEGORY_LIBRARY, "Library", "Saved music", iconRes = R.drawable.ic_auto_library)
                        CATEGORY_PLAYLISTS -> createBrowsableCategory(CATEGORY_PLAYLISTS, "Playlists", "Your playlists", iconRes = R.drawable.ic_auto_playlists)
                        CATEGORY_EXPLORE -> createBrowsableCategory(CATEGORY_EXPLORE, "Explore", "Moods & charts", iconRes = R.drawable.ic_auto_explore)
                        CATEGORY_FAVORITES -> createBrowsableCategory(CATEGORY_FAVORITES, "Liked Songs", "", iconRes = R.drawable.ic_auto_favorite)
                        CATEGORY_DOWNLOADS -> createBrowsableCategory(CATEGORY_DOWNLOADS, "Downloaded", "", iconRes = R.drawable.ic_auto_download)
                        CATEGORY_RECENT -> createBrowsableCategory(CATEGORY_RECENT, "Recently Played", "", iconRes = R.drawable.ic_auto_recent)
                        CATEGORY_ARTISTS -> createBrowsableCategory(CATEGORY_ARTISTS, "Artists", "", iconRes = R.drawable.ic_auto_artist)
                        CATEGORY_ALBUMS -> createBrowsableCategory(CATEGORY_ALBUMS, "Albums", "", iconRes = R.drawable.ic_auto_album)
                        CATEGORY_TOP_CHARTS -> createBrowsableCategory(CATEGORY_TOP_CHARTS, "Top Charts", "", iconRes = R.drawable.ic_auto_explore)
                        else -> null
                    }
                    if (folderItem != null) {
                        future.set(LibraryResult.ofItem(folderItem, null))
                    } else {
                        future.set(LibraryResult.ofError(SessionError.ERROR_BAD_VALUE))
                    }
                } catch (_: Exception) {
                    future.set(LibraryResult.ofError(SessionError.ERROR_IO))
                }
            }
            return future
        }

        override fun onSearch(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            query: String,
            params: LibraryParams?
        ): ListenableFuture<LibraryResult<Void>> {
            val future = SettableFuture.create<LibraryResult<Void>>()
            serviceScope.launch {
                try {
                    val repo = getRepository()
                    val cleanQuery = query.trim()
                    val combined = if (cleanQuery.isBlank() || repo == null) {
                        emptyList()
                    } else {
                        val localMatches = collectAllLibrarySongs(repo).filter { s ->
                            s.title.contains(cleanQuery, ignoreCase = true) ||
                                s.artist.contains(cleanQuery, ignoreCase = true) ||
                                s.album.contains(cleanQuery, ignoreCase = true)
                        }
                        val remoteMatches = runCatching { repo.searchSongs(cleanQuery, "Auto Search") }
                            .getOrDefault(emptyList())
                        (localMatches + remoteMatches).distinctBy { it.id }.take(25)
                    }
                    searchResultsCache[cleanQuery] = combined
                    cacheSongsForParent("search:$cleanQuery", combined)
                    withContext(Dispatchers.Main) {
                        session.notifySearchResultChanged(browser, query, combined.size, params)
                    }
                    future.set(LibraryResult.ofVoid())
                } catch (e: Exception) {
                    android.util.Log.e("SpringMediaService", "onSearch($query) failed: ${e.message}", e)
                    future.set(LibraryResult.ofVoid())
                }
            }
            return future
        }

        override fun onGetSearchResult(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            query: String,
            page: Int,
            pageSize: Int,
            params: LibraryParams?
        ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
            val future = SettableFuture.create<LibraryResult<ImmutableList<MediaItem>>>()
            serviceScope.launch {
                try {
                    val cleanQuery = query.trim()
                    val results = searchResultsCache[cleanQuery] ?: run {
                        val repo = getRepository()
                        if (cleanQuery.isBlank() || repo == null) emptyList()
                        else repo.searchSongs(cleanQuery, "Auto Search").also {
                            searchResultsCache[cleanQuery] = it
                            cacheSongsForParent("search:$cleanQuery", it)
                        }
                    }
                    val safeSize = if (pageSize > 0) pageSize else 25
                    val fromIndex = (page.coerceAtLeast(0) * safeSize).coerceAtMost(results.size)
                    val toIndex = (fromIndex + safeSize).coerceAtMost(results.size)
                    val paged = results.subList(fromIndex, toIndex).map { it.toMediaItem() }
                    future.set(LibraryResult.ofItemList(paged, params))
                } catch (e: Exception) {
                    android.util.Log.e("SpringMediaService", "onGetSearchResult($query) failed: ${e.message}", e)
                    future.set(LibraryResult.ofItemList(emptyList(), params))
                }
            }
            return future
        }

        override fun onAddMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: List<MediaItem>
        ): ListenableFuture<List<MediaItem>> {
            if (controller.packageName == packageName &&
                mediaItems.all { it.localConfiguration?.uri != null }
            ) {
                return Futures.immediateFuture(mediaItems)
            }
            val future = SettableFuture.create<List<MediaItem>>()
            serviceScope.launch {
                try {
                    val resolved = mediaItems.map { item ->
                        if (item.localConfiguration?.uri != null && item.mediaMetadata.isPlayable == true) {
                            item
                        } else {
                            resolvePlayableSong(item)?.toMediaItem() ?: item
                        }
                    }
                    future.set(resolved)
                } catch (e: Exception) {
                    future.set(mediaItems)
                }
            }
            return future
        }

        override fun onSetMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: List<MediaItem>,
            startIndex: Int,
            startPositionMs: Long
        ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
            // Preserve internal PlayerManager queue operations (including Jukebox startMs seeking & quality switches)
            if (controller.packageName == packageName &&
                mediaItems.all { it.localConfiguration?.uri != null }
            ) {
                return Futures.immediateFuture(
                    MediaSession.MediaItemsWithStartPosition(mediaItems, startIndex, startPositionMs)
                )
            }

            val future = SettableFuture.create<MediaSession.MediaItemsWithStartPosition>()
            serviceScope.launch {
                try {
                    val app = applicationContext as? SpringApplication

                    // Handle single-item Android Auto tap or voice search
                    if (mediaItems.size == 1) {
                        val singleItem = mediaItems.first()
                        val searchQuery = singleItem.requestMetadata.searchQuery?.trim()
                        if (!searchQuery.isNullOrBlank() && singleItem.mediaId.isBlank()) {
                            val searchSongs = getRepository()?.searchSongs(searchQuery, "Voice Search").orEmpty()
                            if (searchSongs.isNotEmpty()) {
                                val playableList = searchSongs.map { preparePlayableSong(it) }
                                cacheSongsForParent("search:$searchQuery", playableList)
                                app?.playerManager?.syncExternalPlayback(playableList, playableList.first())
                                val resolvedItems = playableList.map { it.toMediaItem() }
                                future.set(
                                    MediaSession.MediaItemsWithStartPosition(
                                        resolvedItems,
                                        0,
                                        startPositionMs.coerceAtLeast(0L)
                                    )
                                )
                                return@launch
                            }
                        }

                        val clickedSong = resolvePlayableSong(singleItem)
                        if (clickedSong != null) {
                            val parentId = songParentCache[clickedSong.id]
                            val parentSongs = if (parentId != null) containerSongsCache[parentId] else null
                            val queueSongs = if (!parentSongs.isNullOrEmpty() && parentSongs.any { it.id == clickedSong.id }) {
                                parentSongs.map { if (it.id == clickedSong.id) clickedSong else preparePlayableSong(it) }
                            } else {
                                listOf(clickedSong)
                            }
                            val targetIdx = queueSongs.indexOfFirst { it.id == clickedSong.id }.coerceAtLeast(0)
                            app?.playerManager?.syncExternalPlayback(queueSongs, clickedSong)
                            val resolvedItems = queueSongs.map { it.toMediaItem() }
                            future.set(
                                MediaSession.MediaItemsWithStartPosition(
                                    resolvedItems,
                                    targetIdx,
                                    startPositionMs.coerceAtLeast(0L)
                                )
                            )
                            return@launch
                        }
                    }

                    val resolvedSongs = mediaItems.mapNotNull { resolvePlayableSong(it) }
                    val safeIndex = startIndex.coerceIn(0, (resolvedSongs.size - 1).coerceAtLeast(0))
                    if (resolvedSongs.isNotEmpty()) {
                        app?.playerManager?.syncExternalPlayback(resolvedSongs, resolvedSongs[safeIndex])
                        future.set(
                            MediaSession.MediaItemsWithStartPosition(
                                resolvedSongs.map { it.toMediaItem() },
                                safeIndex,
                                startPositionMs.coerceAtLeast(0L)
                            )
                        )
                    } else {
                        future.set(
                            MediaSession.MediaItemsWithStartPosition(
                                mediaItems,
                                startIndex,
                                startPositionMs
                            )
                        )
                    }
                } catch (e: Exception) {
                    android.util.Log.e("SpringMediaService", "onSetMediaItems error: ${e.message}", e)
                    future.set(MediaSession.MediaItemsWithStartPosition(mediaItems, startIndex, startPositionMs))
                }
            }
            return future
        }

        override fun onPlaybackResumption(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo
        ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
            val future = SettableFuture.create<MediaSession.MediaItemsWithStartPosition>()
            serviceScope.launch {
                try {
                    val repo = getRepository()
                    val recents = repo?.getRecentlyPlayed()?.first().orEmpty()
                    val favs = if (recents.isEmpty()) repo?.getFavorites()?.first().orEmpty() else emptyList()
                    val charts = if (recents.isEmpty() && favs.isEmpty()) repo?.getTopChartsSync().orEmpty() else emptyList()
                    val songs = (recents.ifEmpty { favs.ifEmpty { charts } }).take(15).map { preparePlayableSong(it) }
                    if (songs.isNotEmpty()) {
                        cacheSongsForParent(CATEGORY_RECENT, songs)
                        (applicationContext as? SpringApplication)?.playerManager?.syncExternalPlayback(songs, songs.first())
                        future.set(MediaSession.MediaItemsWithStartPosition(songs.map { it.toMediaItem() }, 0, 0L))
                    } else {
                        future.setException(IllegalStateException("No songs available to resume"))
                    }
                } catch (e: Exception) {
                    future.setException(e)
                }
            }
            return future
        }

        private fun createBrowsableCategory(
            id: String,
            title: String,
            subtitle: String,
            coverUrl: String? = null,
            iconRes: Int = R.drawable.ic_auto_music,
            mediaType: Int = MediaMetadata.MEDIA_TYPE_FOLDER_MIXED,
            browsableStyle: Int = CONTENT_STYLE_LIST,
            playableStyle: Int = CONTENT_STYLE_LIST,
            groupTitle: String? = null
        ): MediaItem {
            val extras = Bundle().apply {
                putInt(KEY_CONTENT_STYLE_BROWSABLE, browsableStyle)
                putInt(KEY_CONTENT_STYLE_PLAYABLE, playableStyle)
                if (!groupTitle.isNullOrBlank()) {
                    putString("android.media.browse.CONTENT_STYLE_GROUP_TITLE_HINT", groupTitle)
                }
            }
            val artwork = if (!coverUrl.isNullOrBlank()) Uri.parse(coverUrl) else drawableUri(iconRes)
            val metadata = MediaMetadata.Builder()
                .setTitle(title)
                .setSubtitle(subtitle)
                .setArtworkUri(artwork)
                .setIsBrowsable(true)
                .setIsPlayable(false)
                .setMediaType(mediaType)
                .setExtras(extras)
                .build()
            return MediaItem.Builder()
                .setMediaId(id)
                .setMediaMetadata(metadata)
                .build()
        }

        private fun encodeKey(raw: String): String = URLEncoder.encode(raw, "UTF-8")

        private fun decodeKey(encoded: String): String = runCatching {
            URLDecoder.decode(encoded, "UTF-8")
        }.getOrDefault(encoded)
    }

    private class VectorAwareBitmapLoader(
        private val context: Context
    ) : BitmapLoader {
        private val delegate = DataSourceBitmapLoader(context)

        override fun supportsMimeType(mimeType: String): Boolean {
            return delegate.supportsMimeType(mimeType)
        }

        override fun decodeBitmap(data: ByteArray): ListenableFuture<Bitmap> {
            return delegate.decodeBitmap(data)
        }

        override fun loadBitmap(uri: Uri): ListenableFuture<Bitmap> {
            if (uri.scheme == ContentResolver.SCHEME_ANDROID_RESOURCE) {
                val resId = uri.lastPathSegment?.toIntOrNull() ?: 0
                if (resId != 0) {
                    val drawable = ContextCompat.getDrawable(context, resId)
                    if (drawable != null) {
                        val width = drawable.intrinsicWidth.takeIf { it > 0 } ?: 128
                        val height = drawable.intrinsicHeight.takeIf { it > 0 } ?: 128
                        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                        val canvas = Canvas(bitmap)
                        drawable.setBounds(0, 0, canvas.width, canvas.height)
                        drawable.draw(canvas)
                        return Futures.immediateFuture(bitmap)
                    }
                    val decoded = BitmapFactory.decodeResource(context.resources, resId)
                    if (decoded != null) {
                        return Futures.immediateFuture(decoded)
                    }
                }
            }
            return delegate.loadBitmap(uri)
        }
    }
}
