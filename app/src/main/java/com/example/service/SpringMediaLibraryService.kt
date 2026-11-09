package com.example.service

import android.app.PendingIntent
import android.content.Intent
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import com.example.MainActivity
import com.example.SpringApplication
import com.example.data.local.MediaCacheManager
import com.example.data.model.Song
import com.example.data.network.PresetCatalog
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SpringMediaLibraryService : MediaLibraryService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    lateinit var player: ExoPlayer
    private var mediaLibrarySession: MediaLibrarySession? = null

    companion object {
        const val ROOT_ID = "spring_root"
        const val CATEGORY_FEATURED = "cat_featured"
        const val CATEGORY_MOODS = "cat_moods"
        const val CATEGORY_FAVORITES = "cat_favorites"
        const val CATEGORY_DOWNLOADS = "cat_downloads"
        const val CATEGORY_RECENT = "cat_recent"

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
        val mediaSourceFactory = DefaultMediaSourceFactory(this)
            .setDataSourceFactory(cacheDataSourceFactory)

        val loadControl = androidx.media3.exoplayer.DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                /* minBufferMs = */ 15_000,
                /* maxBufferMs = */ 50_000,
                /* bufferForPlaybackMs = */ 1_000,
                /* bufferForPlaybackAfterRebufferMs = */ 2_000
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()

        player = ExoPlayer.Builder(this)
            .setMediaSourceFactory(mediaSourceFactory)
            .setLoadControl(loadControl)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .build()

        player.addListener(object : androidx.media3.common.Player.Listener {
            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                android.util.Log.e("SpringMediaService", "Player error encountered: ${error.errorCodeName} - ${error.message}", error)
                val app = applicationContext as? SpringApplication
                app?.playerManager?.handlePlaybackError(player, error)
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
            .setId("SpringMediaSession")
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? {
        return mediaLibrarySession
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val prefs = getSharedPreferences("spring_prefs", android.content.Context.MODE_PRIVATE)
        val stopOnTaskRemoved = prefs.getBoolean("stop_on_task_removed", false)
        if (stopOnTaskRemoved || !player.playWhenReady || player.mediaItemCount == 0) {
            player.pause()
            player.stop()
            stopSelf()
        }
    }


    override fun onDestroy() {
        instance = null
        mediaLibrarySession?.run {
            player.release()
            release()
            mediaLibrarySession = null
        }
        super.onDestroy()
    }

    inner class SpringLibrarySessionCallback : MediaLibrarySession.Callback {

        override fun onGetLibraryRoot(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            params: LibraryParams?
        ): ListenableFuture<LibraryResult<MediaItem>> {
            val rootMetadata = MediaMetadata.Builder()
                .setTitle("Spring Music")
                .setIsBrowsable(true)
                .setIsPlayable(false)
                .build()

            val rootItem = MediaItem.Builder()
                .setMediaId(ROOT_ID)
                .setMediaMetadata(rootMetadata)
                .build()

            return Futures.immediateFuture(LibraryResult.ofItem(rootItem, params))
        }

        override fun onGetChildren(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            parentId: String,
            page: Int,
            pageSize: Int,
            params: LibraryParams?
        ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
            return when (parentId) {
                ROOT_ID -> {
                    val categories = listOf(
                        createBrowsableCategory(CATEGORY_FEATURED, "Curated Spring Melodies", "Handpicked soothing tracks"),
                        createBrowsableCategory(CATEGORY_MOODS, "Moods & Atmospheres", "Browse by vibes and settings"),
                        createBrowsableCategory(CATEGORY_FAVORITES, "Favorites", "Your loved songs"),
                        createBrowsableCategory(CATEGORY_DOWNLOADS, "Downloaded", "Available for offline listening"),
                        createBrowsableCategory(CATEGORY_RECENT, "Recently Played", "Quickly resume recent tracks")
                    )
                    Futures.immediateFuture(LibraryResult.ofItemList(categories, params))
                }
                CATEGORY_FEATURED -> {
                    val items = PresetCatalog.PRESET_SONGS.map { it.toMediaItem() }
                    Futures.immediateFuture(LibraryResult.ofItemList(items, params))
                }
                CATEGORY_MOODS -> {
                    val moodItems = PresetCatalog.MOOD_CATEGORIES.map { mood ->
                        val meta = MediaMetadata.Builder()
                            .setTitle("${mood.emoji} ${mood.title}")
                            .setSubtitle(mood.description)
                            .setIsBrowsable(false)
                            .setIsPlayable(false)
                            .build()
                        MediaItem.Builder()
                            .setMediaId("mood_${mood.id}")
                            .setMediaMetadata(meta)
                            .build()
                    }
                    Futures.immediateFuture(LibraryResult.ofItemList(moodItems, params))
                }
                CATEGORY_FAVORITES -> {
                    val future = SettableFuture.create<LibraryResult<ImmutableList<MediaItem>>>()
                    serviceScope.launch {
                        try {
                            val repo = (applicationContext as? SpringApplication)?.repository
                            val favs = repo?.getFavorites()?.first() ?: emptyList()
                            val items = favs.map { it.toMediaItem() }
                            future.set(LibraryResult.ofItemList(items, params))
                        } catch (e: Exception) {
                            future.set(LibraryResult.ofItemList(emptyList(), params))
                        }
                    }
                    future
                }
                CATEGORY_DOWNLOADS -> {
                    val future = SettableFuture.create<LibraryResult<ImmutableList<MediaItem>>>()
                    serviceScope.launch {
                        try {
                            val repo = (applicationContext as? SpringApplication)?.repository
                            val dls = repo?.getDownloads()?.first() ?: emptyList()
                            val items = dls.map { it.toMediaItem() }
                            future.set(LibraryResult.ofItemList(items, params))
                        } catch (e: Exception) {
                            future.set(LibraryResult.ofItemList(emptyList(), params))
                        }
                    }
                    future
                }
                CATEGORY_RECENT -> {
                    val future = SettableFuture.create<LibraryResult<ImmutableList<MediaItem>>>()
                    serviceScope.launch {
                        try {
                            val repo = (applicationContext as? SpringApplication)?.repository
                            val recents = repo?.getRecentlyPlayed()?.first() ?: emptyList()
                            val items = recents.map { it.toMediaItem() }
                            future.set(LibraryResult.ofItemList(items, params))
                        } catch (e: Exception) {
                            future.set(LibraryResult.ofItemList(emptyList(), params))
                        }
                    }
                    future
                }
                else -> {
                    Futures.immediateFuture(LibraryResult.ofItemList(emptyList(), params))
                }
            }
        }

        override fun onGetItem(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            mediaId: String
        ): ListenableFuture<LibraryResult<MediaItem>> {
            val song = PresetCatalog.PRESET_SONGS.find { it.id == mediaId }
            return if (song != null) {
                Futures.immediateFuture(LibraryResult.ofItem(song.toMediaItem(), null))
            } else {
                Futures.immediateFuture(LibraryResult.ofError(LibraryResult.RESULT_ERROR_BAD_VALUE))
            }
        }

        override fun onAddMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: List<MediaItem>
        ): ListenableFuture<List<MediaItem>> {
            val updatedItems = mediaItems.map { mediaItem ->
                if (mediaItem.localConfiguration?.uri == null) {
                    val song = PresetCatalog.PRESET_SONGS.find { it.id == mediaItem.mediaId }
                    song?.toMediaItem() ?: mediaItem
                } else {
                    mediaItem
                }
            }
            return Futures.immediateFuture(updatedItems)
        }

        private fun createBrowsableCategory(id: String, title: String, subtitle: String): MediaItem {
            val metadata = MediaMetadata.Builder()
                .setTitle(title)
                .setSubtitle(subtitle)
                .setIsBrowsable(true)
                .setIsPlayable(false)
                .build()
            return MediaItem.Builder()
                .setMediaId(id)
                .setMediaMetadata(metadata)
                .build()
        }
    }
}
