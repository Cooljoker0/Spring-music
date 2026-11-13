package com.example.data.local

import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.TransferListener
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import com.example.data.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

@OptIn(UnstableApi::class)
object MediaCacheManager {
    private const val MAX_CACHE_SIZE_BYTES = 262144000L // 250 MB
    private const val CACHE_DIR_NAME = "spring_media_cache_v3"
    private var simpleCache: SimpleCache? = null
    private var smartDataSourceFactory: DataSource.Factory? = null

    @Synchronized
    fun getSimpleCache(context: Context): SimpleCache {
        if (simpleCache == null) {
            // Purge legacy cache directories once if present so no corrupted partial chunks remain
            listOf("spring_media_cache", "spring_media_cache_v2").forEach { oldName ->
                val legacyDir = File(context.cacheDir, oldName)
                if (legacyDir.exists()) {
                    runCatching { legacyDir.deleteRecursively() }
                }
            }
            val cacheDir = File(context.cacheDir, CACHE_DIR_NAME).apply {
                if (!exists()) mkdirs()
            }
            simpleCache = SimpleCache(
                cacheDir,
                LeastRecentlyUsedCacheEvictor(MAX_CACHE_SIZE_BYTES),
                StandaloneDatabaseProvider(context)
            )
        }
        return simpleCache!!
    }

    @Synchronized
    fun getCacheDataSourceFactory(context: Context): DataSource.Factory {
        if (smartDataSourceFactory == null) {
            val cache = getSimpleCache(context)
            val defaultHeaders = mapOf(
                "Accept" to "*/*",
                "Accept-Language" to "en-US,en;q=0.9"
            )

            val httpDataSourceFactory: DataSource.Factory = DefaultHttpDataSource.Factory()
                .setUserAgent("SpringMusic/9.5 (Linux; Android) ExoPlayerLib/1.5.1")
                .setDefaultRequestProperties(defaultHeaders)
                .setConnectTimeoutMs(15000)
                .setReadTimeoutMs(30000)
                .setAllowCrossProtocolRedirects(true)

            val upstreamFactory = DefaultDataSource.Factory(context.applicationContext, httpDataSourceFactory)

            val cachedFactory = CacheDataSource.Factory()
                .setCache(cache)
                .setUpstreamDataSourceFactory(upstreamFactory)
                .setFlags(
                    CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR or
                        CacheDataSource.FLAG_IGNORE_CACHE_FOR_UNSET_LENGTH_REQUESTS
                )

            // Route live transcoded / chunked / local streams directly without SimpleCache so partial chunks never stitch/corrupt audio timing
            smartDataSourceFactory = DataSource.Factory {
                LiveBypassDataSource(
                    directFactory = upstreamFactory,
                    cachedFactory = cachedFactory
                )
            }
        }
        return smartDataSourceFactory!!
    }

    private class LiveBypassDataSource(
        private val directFactory: DataSource.Factory,
        private val cachedFactory: DataSource.Factory
    ) : DataSource {
        private var activeSource: DataSource? = null
        private val listeners = mutableListOf<TransferListener>()

        override fun addTransferListener(transferListener: TransferListener) {
            if (!listeners.contains(transferListener)) {
                listeners.add(transferListener)
                activeSource?.addTransferListener(transferListener)
            }
        }

        override fun open(dataSpec: DataSpec): Long {
            activeSource?.let { runCatching { it.close() } }
            activeSource = null

            val uriStr = dataSpec.uri.toString()
            val isLiveOrLocal = uriStr.contains("/v1/stream", ignoreCase = true) ||
                uriStr.contains("requestedAtMs=", ignoreCase = true) ||
                uriStr.contains("jukebox", ignoreCase = true) ||
                uriStr.startsWith("file://") ||
                uriStr.startsWith("content://") ||
                uriStr.startsWith("/")

            val chosen = if (isLiveOrLocal) directFactory.createDataSource() else cachedFactory.createDataSource()
            listeners.forEach { chosen.addTransferListener(it) }
            activeSource = chosen
            return chosen.open(dataSpec)
        }

        override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
            return activeSource?.read(buffer, offset, length) ?: -1
        }

        override fun getUri(): Uri? = activeSource?.uri

        override fun getResponseHeaders(): Map<String, List<String>> {
            return activeSource?.responseHeaders ?: emptyMap()
        }

        override fun close() {
            try {
                activeSource?.close()
            } finally {
                activeSource = null
            }
        }
    }

    suspend fun downloadSongDirectly(context: Context, song: Song): String? = withContext(Dispatchers.IO) {
        try {
            val downloadsDir = File(context.filesDir, "downloads").apply {
                if (!exists()) mkdirs()
            }
            val safeName = "${song.id}_${song.title.replace(Regex("[^a-zA-Z0-9]"), "_")}.mp3"
            val targetFile = File(downloadsDir, safeName)

            if (targetFile.exists() && targetFile.length() > 1024) {
                return@withContext targetFile.absolutePath
            }

            val connection = (URL(song.audioUrl).openConnection() as HttpURLConnection).apply {
                setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 14; Pixel 8 Pro) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36")
                setRequestProperty("Accept", "*/*")
                connectTimeout = 20000
                readTimeout = 30000
                instanceFollowRedirects = true
            }


            connection.inputStream.use { input ->
                FileOutputStream(targetFile).use { output ->
                    input.copyTo(output)
                }
            }

            if (targetFile.exists() && targetFile.length() > 0) {
                targetFile.absolutePath
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun deleteDownloadedSong(filePath: String?): Boolean = withContext(Dispatchers.IO) {
        if (filePath.isNullOrBlank()) return@withContext false
        try {
            val file = File(filePath)
            if (file.exists()) file.delete() else true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getCacheSizeFormatted(context: Context): String = withContext(Dispatchers.IO) {
        val cacheDir = File(context.cacheDir, CACHE_DIR_NAME)
        val sizeBytes = getFolderSize(cacheDir)
        val mb = sizeBytes.toDouble() / (1024.0 * 1024.0)
        String.format(Locale.US, "%.1f MB", mb)
    }

    suspend fun clearCache(context: Context) = withContext(Dispatchers.IO) {
        val cacheDir = File(context.cacheDir, CACHE_DIR_NAME)
        cacheDir.deleteRecursively()
        cacheDir.mkdirs()
    }

    private fun getFolderSize(file: File): Long {
        if (!file.isDirectory) return file.length()
        var size = 0L
        file.listFiles()?.forEach { size += getFolderSize(it) }
        return size
    }
}
