package com.example

import android.app.Application
import android.graphics.Bitmap
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.example.data.firestore.FirestoreSyncManager
import com.example.data.local.SpringDatabase
import com.example.data.repository.MusicRepository
import com.example.player.PlayerManager
import com.example.ui.theme.AiThemeManager
import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

class SpringApplication : Application(), ImageLoaderFactory {
    lateinit var database: SpringDatabase
        private set

    lateinit var firestoreSyncManager: FirestoreSyncManager
        private set

    lateinit var repository: MusicRepository
        private set

    lateinit var playerManager: PlayerManager
        private set

    override fun onCreate() {
        super.onCreate()
        AiThemeManager.init(this)
        database = SpringDatabase.getInstance(this)
        firestoreSyncManager = FirestoreSyncManager(this, database.playlistDao(), database.songDao())
        repository = MusicRepository(database.songDao(), database.playlistDao(), this, firestoreSyncManager)
        playerManager = PlayerManager(this)
    }

    override fun newImageLoader(): ImageLoader {
        // Cap concurrent image requests so artwork loading never starves audio streaming bandwidth
        val imageDispatcher = Dispatcher().apply {
            maxRequests = 10
            maxRequestsPerHost = 3
        }
        val imageOkHttp = OkHttpClient.Builder()
            .dispatcher(imageDispatcher)
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()

        return ImageLoader.Builder(this)
            .okHttpClient(imageOkHttp)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.18)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(64L * 1024L * 1024L)
                    .build()
            }
            .bitmapConfig(Bitmap.Config.RGB_565)
            .respectCacheHeaders(false)
            .crossfade(200)
            .build()
    }
}
