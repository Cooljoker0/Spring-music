package com.example.data.local

import android.content.Context
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
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

object MediaCacheManager {
    private const val MAX_CACHE_SIZE_BYTES = 262144000L // 250 MB
    private var simpleCache: SimpleCache? = null
    private var cacheDataSourceFactory: CacheDataSource.Factory? = null

    @Synchronized
    fun getSimpleCache(context: Context): SimpleCache {
        if (simpleCache == null) {
            val cacheDir = File(context.cacheDir, "spring_media_cache").apply {
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
    fun getCacheDataSourceFactory(context: Context): CacheDataSource.Factory {
        if (cacheDataSourceFactory == null) {
            val cache = getSimpleCache(context)
            val defaultHeaders = mapOf(
                "User-Agent" to "Mozilla/5.0 (Linux; Android 14; Pixel 8 Pro) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36",
                "Accept" to "*/*",
                "Accept-Language" to "en-US,en;q=0.9"
            )

            val httpDataSourceFactory: DataSource.Factory = DefaultHttpDataSource.Factory()
                .setUserAgent("Mozilla/5.0 (Linux; Android 14; Pixel 8 Pro) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36")
                .setDefaultRequestProperties(defaultHeaders)
                .setConnectTimeoutMs(10000)
                .setReadTimeoutMs(15000)
                .setAllowCrossProtocolRedirects(true)

            cacheDataSourceFactory = CacheDataSource.Factory()
                .setCache(cache)
                .setUpstreamDataSourceFactory(DefaultDataSource.Factory(context, httpDataSourceFactory))
                .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
        }
        return cacheDataSourceFactory!!
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
        val cacheDir = File(context.cacheDir, "spring_media_cache")
        val sizeBytes = getFolderSize(cacheDir)
        val mb = sizeBytes.toDouble() / (1024.0 * 1024.0)
        String.format(Locale.US, "%.1f MB", mb)
    }

    suspend fun clearCache(context: Context) = withContext(Dispatchers.IO) {
        val cacheDir = File(context.cacheDir, "spring_media_cache")
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
