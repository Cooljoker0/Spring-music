package com.example.updater

import android.app.Activity
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.core.content.FileProvider
import com.example.BuildConfig
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

data class AppUpdateInfo(
    val versionCode: Int = 1,
    val versionName: String = "1.0",
    val downloadUrl: String = "",
    val changelog: String = "",
    val isMandatory: Boolean = false,
    val publishedAt: Long = System.currentTimeMillis()
)

/**
 * In-memory CookieJar to maintain Google Drive, Dropbox, and CDN session cookies across redirects.
 */
private class InMemoryCookieJar : CookieJar {
    private val cookieStore = ConcurrentHashMap<String, MutableList<Cookie>>()

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        val host = url.host
        val existing = cookieStore.getOrPut(host) { mutableListOf() }
        synchronized(existing) {
            existing.removeAll { old -> cookies.any { it.name == old.name } }
            existing.addAll(cookies)
        }
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val host = url.host
        val cookies = cookieStore[host] ?: return emptyList()
        val now = System.currentTimeMillis()
        synchronized(cookies) {
            cookies.removeAll { it.expiresAt < now }
            return ArrayList(cookies)
        }
    }
}

object AppUpdateManager {
    private val firestore by lazy { FirebaseFirestore.getInstance() }
    private val scope = CoroutineScope(Dispatchers.IO)

    private val cookieJar = InMemoryCookieJar()
    private val httpClient = OkHttpClient.Builder()
        .cookieJar(cookieJar)
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(180, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .retryOnConnectionFailure(true)
        .build()

    val currentVersionCode: Int = try {
        BuildConfig.APP_VERSION_CODE
    } catch (_: Exception) {
        try { BuildConfig.VERSION_CODE } catch (_: Exception) { 2 }
    }

    val currentVersionName: String = try {
        BuildConfig.APP_VERSION_NAME
    } catch (_: Exception) {
        try { BuildConfig.VERSION_NAME } catch (_: Exception) { "9.1" }
    }

    val currentVersionFull: String = try {
        BuildConfig.APP_VERSION_FULL
    } catch (_: Exception) {
        "v$currentVersionName (Build $currentVersionCode)"
    }

    private val _updateAvailable = MutableStateFlow<AppUpdateInfo?>(null)
    val updateAvailable: StateFlow<AppUpdateInfo?> = _updateAvailable.asStateFlow()

    private val _isChecking = MutableStateFlow(false)
    val isChecking: StateFlow<Boolean> = _isChecking.asStateFlow()

    private val _downloadProgress = MutableStateFlow<Float?>(null)
    val downloadProgress: StateFlow<Float?> = _downloadProgress.asStateFlow()

    private val _isDownloading = MutableStateFlow(false)
    val isDownloading: StateFlow<Boolean> = _isDownloading.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val _downloadedApkFile = MutableStateFlow<File?>(null)
    val downloadedApkFile: StateFlow<File?> = _downloadedApkFile.asStateFlow()

    private val _downloadSpeedText = MutableStateFlow<String?>(null)
    val downloadSpeedText: StateFlow<String?> = _downloadSpeedText.asStateFlow()

    private var hasInitialized = false

    fun init(context: Context) {
        if (hasInitialized) return
        hasInitialized = true

        // Listen for real-time OTA updates pushed from anywhere
        try {
            firestore.collection("app_updates").document("latest")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        return@addSnapshotListener
                    }
                    if (snapshot == null || !snapshot.exists()) {
                        _updateAvailable.value = null
                        _statusMessage.value = "Spring Music is up to date (v$currentVersionName)"
                        return@addSnapshotListener
                    }
                    val remoteCode = snapshot.getLong("versionCode")?.toInt() ?: 0
                    val remoteName = snapshot.getString("versionName") ?: ""
                    val downloadUrl = snapshot.getString("downloadUrl") ?: ""
                    val changelog = snapshot.getString("changelog") ?: ""
                    val isMandatory = snapshot.getBoolean("isMandatory") ?: false
                    val publishedAt = snapshot.getLong("publishedAt") ?: System.currentTimeMillis()

                    if (remoteCode > currentVersionCode && downloadUrl.isNotBlank()) {
                        val prefs = context.getSharedPreferences("spring_updater_prefs", Context.MODE_PRIVATE)
                        val dismissedCode = prefs.getInt("dismissed_version_code", -1)
                        if (!isMandatory && remoteCode <= dismissedCode) {
                            return@addSnapshotListener
                        }

                        val info = AppUpdateInfo(
                            versionCode = remoteCode,
                            versionName = remoteName,
                            downloadUrl = downloadUrl,
                            changelog = changelog,
                            isMandatory = isMandatory,
                            publishedAt = publishedAt
                        )
                        _updateAvailable.value = info
                        // Check if file is already on device
                        findExistingDownloadedApk(context, remoteCode)?.let {
                            _downloadedApkFile.value = it
                        }
                    } else {
                        _updateAvailable.value = null
                        _statusMessage.value = "Spring Music is up to date (v$currentVersionName)"
                    }
                }
        } catch (_: Exception) {}
    }

    fun checkForUpdates(
        context: Context,
        isManualCheck: Boolean = false,
        onComplete: ((AppUpdateInfo?) -> Unit)? = null
    ) {
        scope.launch {
            _isChecking.value = true
            _statusMessage.value = "Checking for updates..."
            try {
                val doc = firestore.collection("app_updates").document("latest").get().awaitTask()
                if (!doc.exists()) {
                    _updateAvailable.value = null
                    _statusMessage.value = "Spring Music is up to date (v$currentVersionName)"
                    withContext(Dispatchers.Main) { onComplete?.invoke(null) }
                    return@launch
                }

                val remoteCode = doc.getLong("versionCode")?.toInt() ?: 0
                val remoteName = doc.getString("versionName") ?: ""
                val downloadUrl = doc.getString("downloadUrl") ?: ""
                val changelog = doc.getString("changelog") ?: ""
                val isMandatory = doc.getBoolean("isMandatory") ?: false
                val publishedAt = doc.getLong("publishedAt") ?: System.currentTimeMillis()

                if (remoteCode > currentVersionCode && downloadUrl.isNotBlank()) {
                    val prefs = context.getSharedPreferences("spring_updater_prefs", Context.MODE_PRIVATE)
                    val dismissedCode = prefs.getInt("dismissed_version_code", -1)

                    if (!isManualCheck && !isMandatory && remoteCode <= dismissedCode) {
                        _statusMessage.value = "Update v$remoteName available"
                        withContext(Dispatchers.Main) { onComplete?.invoke(null) }
                        return@launch
                    }

                    val info = AppUpdateInfo(
                        versionCode = remoteCode,
                        versionName = remoteName,
                        downloadUrl = downloadUrl,
                        changelog = changelog,
                        isMandatory = isMandatory,
                        publishedAt = publishedAt
                    )
                    _updateAvailable.value = info
                    findExistingDownloadedApk(context, remoteCode)?.let {
                        _downloadedApkFile.value = it
                    }
                    _statusMessage.value = "Update v$remoteName available!"
                    withContext(Dispatchers.Main) { onComplete?.invoke(info) }
                } else {
                    _updateAvailable.value = null
                    _statusMessage.value = "Spring Music is up to date (v$currentVersionName)"
                    withContext(Dispatchers.Main) { onComplete?.invoke(null) }
                }
            } catch (e: Exception) {
                _statusMessage.value = "Check completed. App is running latest version."
                withContext(Dispatchers.Main) { onComplete?.invoke(null) }
            } finally {
                _isChecking.value = false
            }
        }
    }

    fun dismissUpdate(context: Context? = null, forVersionCode: Int? = null) {
        _updateAvailable.value = null
        if (context != null && forVersionCode != null) {
            val prefs = context.getSharedPreferences("spring_updater_prefs", Context.MODE_PRIVATE)
            prefs.edit().putInt("dismissed_version_code", forVersionCode).apply()
        }
    }

    fun extractGoogleDriveId(url: String): String? {
        val trimmed = url.trim()
        val driveRegex = Regex("""drive\.google\.com/file/d/([a-zA-Z0-9_-]+)""")
        driveRegex.find(trimmed)?.let { return it.groupValues[1] }

        val driveOpenRegex = Regex("""drive\.google\.com/open\?id=([a-zA-Z0-9_-]+)""")
        driveOpenRegex.find(trimmed)?.let { return it.groupValues[1] }

        val driveUcRegex = Regex("""drive\.google\.com/uc\?(?:.*&)?id=([a-zA-Z0-9_-]+)""")
        driveUcRegex.find(trimmed)?.let { return it.groupValues[1] }

        val driveUsercontentRegex = Regex("""drive\.usercontent\.google\.com/download\?(?:.*&)?id=([a-zA-Z0-9_-]+)""")
        driveUsercontentRegex.find(trimmed)?.let { return it.groupValues[1] }

        return null
    }

    fun normalizeDownloadUrl(url: String): String {
        val trimmed = url.trim()
        val driveId = extractGoogleDriveId(trimmed)
        if (driveId != null) {
            return "https://drive.usercontent.google.com/download?id=$driveId&export=download&authuser=0&confirm=t"
        }

        // Dropbox: dl=0 -> dl=1 for direct download
        if (trimmed.contains("dropbox.com") && trimmed.contains("dl=0")) {
            return trimmed.replace("dl=0", "dl=1")
        }
        return trimmed
    }

    fun getBrowserFriendlyUrl(url: String): String {
        val trimmed = url.trim()
        val driveId = extractGoogleDriveId(trimmed)
        if (driveId != null) {
            return "https://drive.google.com/file/d/$driveId/view?usp=sharing"
        }
        if (trimmed.contains("dropbox.com") && trimmed.contains("dl=0")) {
            return trimmed.replace("dl=0", "dl=1")
        }
        return trimmed
    }

    /**
     * Attempts to resolve a GitHub release page (tag or latest) to a direct .apk download asset URL.
     */
    suspend fun resolveGithubReleaseApk(url: String): String? = withContext(Dispatchers.IO) {
        val trimmed = url.trim()
        val tagRegex = Regex("""github\.com/([^/]+)/([^/]+)/releases/tag/([^/?#]+)""")
        val latestRegex = Regex("""github\.com/([^/]+)/([^/]+)/releases(?:/latest)?/?$""")

        val (owner, repo, tag) = when {
            tagRegex.containsMatchIn(trimmed) -> {
                val match = tagRegex.find(trimmed)!!
                Triple(match.groupValues[1], match.groupValues[2], match.groupValues[3])
            }
            latestRegex.containsMatchIn(trimmed) -> {
                val match = latestRegex.find(trimmed)!!
                Triple(match.groupValues[1], match.groupValues[2], "latest")
            }
            else -> return@withContext null
        }

        try {
            val apiUrl = if (tag == "latest") {
                "https://api.github.com/repos/$owner/$repo/releases/latest"
            } else {
                "https://api.github.com/repos/$owner/$repo/releases/tags/$tag"
            }

            val request = Request.Builder()
                .url(apiUrl)
                .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 14) Chrome/128.0.0.0 Mobile SpringMusic/${currentVersionName}")
                .addHeader("Accept", "application/vnd.github.v3+json")
                .build()

            httpClient.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext null
                val body = resp.body?.string() ?: return@withContext null
                val json = org.json.JSONObject(body)
                val assets = json.optJSONArray("assets") ?: return@withContext null
                var fallbackApkUrl: String? = null
                for (i in 0 until assets.length()) {
                    val asset = assets.getJSONObject(i)
                    val name = asset.optString("name", "")
                    val downloadUrl = asset.optString("browser_download_url", "")
                    if (name.endsWith(".apk", ignoreCase = true)) {
                        // Strongly prefer debug build because installed app is signed with debug keystore!
                        if (name.contains("debug", ignoreCase = true)) {
                            return@withContext downloadUrl
                        }
                        fallbackApkUrl = downloadUrl
                    }
                }
                return@withContext fallbackApkUrl
            }
        } catch (_: Exception) {
            null
        }
    }

    fun openInBrowser(context: Context, url: String) {
        val browserUrl = getBrowserFriendlyUrl(url)
        try {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(browserUrl)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(browserIntent)
        } catch (_: Exception) {
            try {
                val fallbackIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url.trim())).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(fallbackIntent)
            } catch (_: Exception) {}
        }
    }

    /**
     * Download using Android system DownloadManager in the notification bar
     */
    fun downloadWithSystemManager(context: Context, info: AppUpdateInfo): Boolean {
        return try {
            val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val downloadUrl = normalizeDownloadUrl(info.downloadUrl)
            val request = DownloadManager.Request(Uri.parse(downloadUrl)).apply {
                setTitle("Spring Music v${info.versionName}")
                setDescription("Downloading APK update package...")
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "spring_music_v${info.versionCode}.apk")
                setMimeType("application/vnd.android.package-archive")
                addRequestHeader("User-Agent", "Mozilla/5.0 (Linux; Android 14) SpringMusic/${currentVersionName}")
            }
            dm.enqueue(request)
            _statusMessage.value = "Download started in notifications! Check your notification bar."
            true
        } catch (e: Exception) {
            _statusMessage.value = "Opening browser for download..."
            openInBrowser(context, info.downloadUrl)
            false
        }
    }

    fun isValidApkFile(file: File?): Boolean {
        if (file == null || !file.exists() || file.length() < 10000) return false
        return try {
            val header = ByteArray(4)
            FileInputStream(file).use { it.read(header) }
            // ZIP magic bytes: PK\x03\x04 or PK\x05\x06
            header[0] == 0x50.toByte() && header[1] == 0x4B.toByte()
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Inspects the internal package manifest of an APK file to read its actual versionCode.
     */
    fun getApkVersionCode(context: Context, file: File?): Int {
        if (file == null || !file.exists()) return -1
        return try {
            val pm = context.packageManager
            val pi = pm.getPackageArchiveInfo(file.absolutePath, 0) ?: return -1
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                pi.longVersionCode.toInt()
            } else {
                @Suppress("DEPRECATION")
                pi.versionCode
            }
        } catch (_: Exception) {
            -1
        }
    }

    /**
     * Searches for any previously downloaded APK on the device matching this update.
     * Verifies that the APK's internal versionCode actually matches or exceeds the update.
     */
    fun findExistingDownloadedApk(context: Context, targetVersionCode: Int): File? {
        val targetName = "spring_music_v$targetVersionCode.apk"

        fun isMatchingApk(file: File?): Boolean {
            if (!isValidApkFile(file)) return false
            val fileCode = getApkVersionCode(context, file)
            // MUST match or exceed targetVersionCode to prevent reinstalling outdated versions
            return fileCode >= targetVersionCode
        }

        // 1. App internal cache
        val cacheFile = File(File(context.cacheDir, "apk_updates"), targetName)
        if (isMatchingApk(cacheFile)) return cacheFile

        // 2. App external files dir
        val extAppFile = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), targetName)
        if (isMatchingApk(extAppFile)) return extAppFile

        // 3. Public Downloads directory
        try {
            val publicDownloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val publicFile = File(publicDownloads, targetName)
            if (isMatchingApk(publicFile)) return publicFile

            // Check recently modified candidates in Downloads ONLY IF internal version matches
            val candidates = publicDownloads.listFiles { f ->
                f.isFile && f.extension.equals("apk", ignoreCase = true) &&
                        (f.name.contains("spring", ignoreCase = true) || f.name.contains("app-debug", ignoreCase = true))
            }
            val matchingRecent = candidates?.filter { isMatchingApk(it) }?.maxByOrNull { it.lastModified() }
            if (matchingRecent != null && (System.currentTimeMillis() - matchingRecent.lastModified()) < 24 * 3600 * 1000) {
                return matchingRecent
            }
        } catch (_: Exception) {}

        return null
    }

    fun downloadAndInstall(
        context: Context,
        info: AppUpdateInfo,
        onComplete: (() -> Unit)? = null,
        onError: ((String) -> Unit)? = null
    ) {
        // If already downloaded and valid with matching version, directly trigger install
        val cachedApk = _downloadedApkFile.value ?: findExistingDownloadedApk(context, info.versionCode)
        if (cachedApk != null && isValidApkFile(cachedApk) && getApkVersionCode(context, cachedApk) >= info.versionCode) {
            _downloadedApkFile.value = cachedApk
            launchPackageInstaller(context, cachedApk)
            onComplete?.invoke()
            return
        }

        if (_isDownloading.value) return

        scope.launch {
            _isDownloading.value = true
            _downloadProgress.value = 0.05f
            _statusMessage.value = "Connecting to download server..."
            _downloadSpeedText.value = null

            val targetDir = File(context.cacheDir, "apk_updates").apply { mkdirs() }
            val apkFile = File(targetDir, "spring_music_v${info.versionCode}.apk")
            if (apkFile.exists()) {
                apkFile.delete()
            }

            try {
                var directUrl = normalizeDownloadUrl(info.downloadUrl)
                var request = Request.Builder()
                    .url(directUrl)
                    .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 14) Chrome/128.0.0.0 Mobile SpringMusic/${currentVersionName}")
                    .addHeader("Accept", "*/*")
                    .build()

                _statusMessage.value = "Requesting APK package..."
                var response = httpClient.newCall(request).execute()

                val finalHost = response.request.url.host
                val finalUrlStr = response.request.url.toString()
                val contentType = response.header("Content-Type", "") ?: ""
                val isHtml = contentType.contains("text/html", ignoreCase = true)
                val isDrive = info.downloadUrl.contains("drive.google.com") || info.downloadUrl.contains("drive.usercontent")

                // Detect Google Drive Restricted access / Permission barrier
                if (finalHost.contains("accounts.google.com") || finalUrlStr.contains("ServiceLogin") || finalUrlStr.contains("signin") || (isDrive && (response.code == 401 || response.code == 403))) {
                    throw IllegalStateException(
                        "Google Drive Permission Error: This Google Drive link is Restricted.\n\n" +
                        "To fix in Google Drive:\n" +
                        "1. Open Google Drive on PC or phone.\n" +
                        "2. Tap the 3 dots on the APK file -> 'Share'.\n" +
                        "3. Change General Access from 'Restricted' to 'Anyone with the link' (Viewer).\n" +
                        "4. Tap 'Copy link' and update the broadcast."
                    )
                }

                if (isHtml) {
                    val htmlBody = response.body?.string().orEmpty()

                    if (htmlBody.contains("Sign in - Google Accounts") || htmlBody.contains("You need access") || htmlBody.contains("Request access")) {
                        throw IllegalStateException(
                            "Google Drive Permission Error: This Google Drive link is Restricted.\n\n" +
                            "To fix in Google Drive:\n" +
                            "1. Open Google Drive on PC or phone.\n" +
                            "2. Tap the 3 dots on the APK file -> 'Share'.\n" +
                            "3. Change General Access from 'Restricted' to 'Anyone with the link' (Viewer).\n" +
                            "4. Tap 'Copy link' and update the broadcast."
                        )
                    }

                    // Parse Google Drive confirm form/token
                    val confirmRegex = Regex("""confirm=([0-9a-zA-Z_-]+)""")
                    val confirmMatch = confirmRegex.find(htmlBody)
                    val formActionRegex = Regex("""<form[^>]*action=["']([^"']+)["']""", RegexOption.IGNORE_CASE)
                    val formActionMatch = formActionRegex.find(htmlBody)

                    val driveId = extractGoogleDriveId(info.downloadUrl)

                    var newTargetUrl: String? = null
                    if (formActionMatch != null && formActionMatch.groupValues[1].isNotBlank()) {
                        var action = formActionMatch.groupValues[1]
                        if (!action.startsWith("http")) {
                            action = "https://drive.usercontent.google.com$action"
                        }
                        // Extract hidden fields
                        val hiddenInputs = Regex("""<input[^>]*name=["']([^"']+)["'][^>]*value=["']([^"']+)["']""", RegexOption.IGNORE_CASE)
                        val params = hiddenInputs.findAll(htmlBody).map { "${it.groupValues[1]}=${it.groupValues[2]}" }.joinToString("&")
                        newTargetUrl = if (params.isNotBlank()) "$action?$params" else action
                    } else if (confirmMatch != null && driveId != null) {
                        val token = confirmMatch.groupValues[1]
                        newTargetUrl = "https://drive.usercontent.google.com/download?id=$driveId&export=download&confirm=$token"
                    } else if (driveId != null) {
                        newTargetUrl = "https://drive.usercontent.google.com/download?id=$driveId&export=download&confirm=t"
                    }

                    if (newTargetUrl != null) {
                        directUrl = newTargetUrl
                        request = Request.Builder()
                            .url(directUrl)
                            .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 14) Chrome/128.0.0.0 Mobile SpringMusic/${currentVersionName}")
                            .addHeader("Accept", "*/*")
                            .build()
                        response = httpClient.newCall(request).execute()
                    }
                }

                if (!response.isSuccessful) {
                    if (response.code == 404) {
                        val isGithub = directUrl.contains("github.com", ignoreCase = true)
                        val advice = if (isGithub) {
                            "Error 404 (Not Found): GitHub returned 404 for this APK link.\n\n" +
                            "Possible reasons:\n" +
                            "1. The GitHub repository is Private (GitHub blocks downloads from private repos).\n" +
                            "2. The release or tag does not exist (check if tag needs 'v' prefix, e.g. v9.3.1).\n" +
                            "3. The asset 'app-debug.apk' was not uploaded to this release.\n\n" +
                            "Solution: Make the repository Public, or upload the APK to Google Drive (set to 'Anyone with link') and use the Google Drive link."
                        } else {
                            "Error 404 (Not Found): The APK file was not found on the download server.\n" +
                            "Please check the URL or host the APK on Google Drive."
                        }
                        throw IllegalStateException(advice)
                    }
                    throw IllegalStateException("Error ${response.code}: Server returned HTTP ${response.code}")
                }

                val body = response.body ?: throw IllegalStateException("Empty response body")
                val totalBytes = body.contentLength()

                _statusMessage.value = "Downloading update package..."
                val startTime = System.currentTimeMillis()
                body.byteStream().use { input ->
                    FileOutputStream(apkFile).use { output ->
                        val buffer = ByteArray(64 * 1024)
                        var bytesRead: Int
                        var downloadedBytes = 0L

                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                            downloadedBytes += bytesRead
                            if (totalBytes > 0) {
                                val progress = (downloadedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
                                _downloadProgress.value = progress

                                val elapsedSec = (System.currentTimeMillis() - startTime) / 1000.0
                                if (elapsedSec > 0.4) {
                                    val mbDownloaded = downloadedBytes / (1024.0 * 1024.0)
                                    val mbTotal = totalBytes / (1024.0 * 1024.0)
                                    val speed = mbDownloaded / elapsedSec
                                    _downloadSpeedText.value = String.format("%.1f MB / %.1f MB (%.1f MB/s)", mbDownloaded, mbTotal, speed)
                                }
                            }
                        }
                        output.flush()
                    }
                }

                // Verify file header: APK/ZIP files must start with PK (0x50 0x4B)
                val isValid = isValidApkFile(apkFile)

                if (!isValid) {
                    apkFile.delete()
                    // Auto handoff to System DownloadManager & Browser so user is never stuck
                    _statusMessage.value = "Direct stream required browser confirmation. Opening Chrome..."
                    withContext(Dispatchers.Main) {
                        openInBrowser(context, info.downloadUrl)
                    }
                    throw IllegalStateException("File host required browser confirmation. Opening Chrome to download.")
                }

                apkFile.setReadable(true, false)
                _downloadedApkFile.value = apkFile
                _downloadProgress.value = 1.0f
                _statusMessage.value = "Download completed! Preparing installer..."

                withContext(Dispatchers.Main) {
                    launchPackageInstaller(context, apkFile)
                    onComplete?.invoke()
                }
            } catch (e: Exception) {
                val errorMsg = e.localizedMessage ?: "Failed to download update"
                _statusMessage.value = errorMsg
                withContext(Dispatchers.Main) {
                    onError?.invoke(errorMsg)
                }
            } finally {
                _isDownloading.value = false
            }
        }
    }

    fun launchPackageInstaller(context: Context, apkFile: File) {
        if (!isValidApkFile(apkFile)) {
            _statusMessage.value = "Error: Downloaded APK file is incomplete or invalid"
            return
        }

        // On Android 8.0+, verify unknown app install permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!context.packageManager.canRequestPackageInstalls()) {
                _statusMessage.value = "Please allow 'Install unknown apps' in Settings, then tap Install Update again."
                val intent = Intent(
                    Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:${context.packageName}")
                ).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                return
            }
        }

        // Ensure file is in app's cache directory where FileProvider has full authority
        val installableFile = if (apkFile.parentFile == File(context.cacheDir, "apk_updates")) {
            apkFile
        } else {
            try {
                val targetDir = File(context.cacheDir, "apk_updates").apply { mkdirs() }
                val dest = File(targetDir, apkFile.name)
                apkFile.copyTo(dest, overwrite = true)
                dest.setReadable(true, false)
                dest
            } catch (_: Exception) {
                apkFile
            }
        }

        val authority = "${context.packageName}.fileprovider"
        val apkUri = FileProvider.getUriForFile(context, authority, installableFile)

        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
            val resolveInfos = context.packageManager.queryIntentActivities(installIntent, 0)
            for (resolveInfo in resolveInfos) {
                val pkg = resolveInfo.activityInfo.packageName
                context.grantUriPermission(pkg, apkUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        } catch (_: Exception) {}

        try {
            context.startActivity(installIntent)
            _statusMessage.value = "Package installer launched!"
        } catch (e: Exception) {
            _statusMessage.value = "Installer error: ${e.message}. Try browser download."
        }
    }

    /**
     * Probes an APK download URL to verify reachability and provide clear troubleshooting.
     * Supports Google Drive, GitHub Releases, direct CDNs, and custom web servers.
     */
    suspend fun probeUrl(url: String): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val trimmed = url.trim()
        if (trimmed.isBlank() || (!trimmed.startsWith("http://", ignoreCase = true) && !trimmed.startsWith("https://", ignoreCase = true))) {
            return@withContext Pair(false, "Invalid link: URL must begin with https:// or http://")
        }

        var effectiveUrl = trimmed
        if (trimmed.contains("github.com", ignoreCase = true) && !trimmed.contains("/download/") && !trimmed.contains("/raw/")) {
            val resolved = resolveGithubReleaseApk(trimmed)
            if (resolved != null) {
                effectiveUrl = resolved
            } else if (trimmed.endsWith(".git", ignoreCase = true) || !trimmed.contains("/releases")) {
                return@withContext Pair(
                    false,
                    "GitHub Link Notice: You entered a repository link ($trimmed).\n\n" +
                    "To broadcast from GitHub, use the direct download link to app-debug.apk under Releases:\n" +
                    "https://github.com/OWNER/REPO/releases/download/TAG/app-debug.apk\n\n" +
                    "Tip: You can also paste the release tag page (e.g. .../releases/tag/v9.4) and Spring Music will auto-detect the APK!"
                )
            }
        }

        val driveId = extractGoogleDriveId(effectiveUrl)
        val testUrl = if (driveId != null) {
            "https://drive.usercontent.google.com/download?id=$driveId&export=download&confirm=t"
        } else {
            normalizeDownloadUrl(effectiveUrl)
        }

        try {
            val request = Request.Builder()
                .url(testUrl)
                .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 14) Chrome/128.0.0.0 Mobile SpringMusic/${currentVersionName}")
                .addHeader("Accept", "*/*")
                .head()
                .build()

            var response = httpClient.newCall(request).execute()
            if (response.code == 405 || response.code == 403 || response.code == 400) {
                // Some servers reject HEAD requests; fallback to a 1-byte GET range probe
                val getRequest = Request.Builder()
                    .url(testUrl)
                    .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 14) Chrome/128.0.0.0 Mobile SpringMusic/${currentVersionName}")
                    .addHeader("Range", "bytes=0-1024")
                    .get()
                    .build()
                response = httpClient.newCall(getRequest).execute()
            }

            val finalHost = response.request.url.host
            val finalUrlStr = response.request.url.toString()
            val isDrive = effectiveUrl.contains("drive.google.com") || effectiveUrl.contains("drive.usercontent")

            if (finalHost.contains("accounts.google.com") || finalUrlStr.contains("ServiceLogin") || finalUrlStr.contains("signin") || (isDrive && (response.code == 401 || response.code == 403))) {
                return@withContext Pair(
                    false,
                    "Google Drive Permission Error: This link requires owner permission (Restricted access).\n\n" +
                    "To fix in Google Drive:\n" +
                    "1. Open Google Drive on your PC or phone.\n" +
                    "2. Tap the 3 dots on the APK file -> 'Share'.\n" +
                    "3. Under 'General access', change from 'Restricted' to 'Anyone with the link' (Viewer).\n" +
                    "4. Tap 'Copy link' and test/broadcast again."
                )
            }

            if (response.code == 404) {
                val isGithub = effectiveUrl.contains("github.com", ignoreCase = true)
                val msg = if (isGithub) {
                    "HTTP 404 Not Found on GitHub:\n" +
                    "• Is the GitHub repo Public? (Private repo downloads require authentication and return 404).\n" +
                    "• Check the release tag name (e.g., does it need 'v' prefix like 'v9.1'?)\n" +
                    "• Make sure 'app-debug.apk' was actually uploaded under this release."
                } else {
                    "HTTP 404 Not Found: The file does not exist at this link. Please check your URL."
                }
                return@withContext Pair(false, msg)
            } else if (response.code in 200..399) {
                val successMsg = if (effectiveUrl != trimmed) {
                    "✓ Resolved GitHub APK ($testUrl) is active & accessible (HTTP ${response.code})"
                } else {
                    "✓ Download link is active and accessible (HTTP ${response.code})"
                }
                return@withContext Pair(true, successMsg)
            } else {
                return@withContext Pair(false, "Server returned HTTP ${response.code}")
            }
        } catch (e: Exception) {
            return@withContext Pair(false, "Connection check: ${e.message ?: "Failed to reach server"}")
        }
    }


    /**
     * Broadcast an update to all installed phones over-the-air.
     */
    suspend fun pushUpdate(
        versionCode: Int,
        versionName: String,
        downloadUrl: String,
        changelog: String,
        isMandatory: Boolean = false,
        skipUrlProbe: Boolean = false
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            var trimmedUrl = downloadUrl.trim()
            if (trimmedUrl.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("Please enter an APK download link"))
            }

            if (trimmedUrl.contains("github.com", ignoreCase = true) && !trimmedUrl.contains("/download/") && !trimmedUrl.contains("/raw/")) {
                val resolved = resolveGithubReleaseApk(trimmedUrl)
                if (resolved != null) {
                    trimmedUrl = resolved
                } else if (trimmedUrl.endsWith(".git", ignoreCase = true) || !trimmedUrl.contains("/releases")) {
                    return@withContext Result.failure(
                        IllegalArgumentException(
                            "You entered a GitHub repository link ($trimmedUrl). An update broadcast requires a direct link to the APK file in GitHub Releases (e.g. https://github.com/USER/REPO/releases/download/v9.1/app-debug.apk) or Google Drive."
                        )
                    )
                }
            }

            // Probe URL before broadcasting unless explicitly skipped
            if (!skipUrlProbe) {
                val probeResult = probeUrl(trimmedUrl)
                if (!probeResult.first && probeResult.second.contains("404")) {
                    return@withContext Result.failure(
                        IllegalArgumentException(probeResult.second)
                    )
                }
            }

            val data = hashMapOf(
                "versionCode" to versionCode,
                "versionName" to versionName.trim(),
                "downloadUrl" to trimmedUrl,
                "changelog" to changelog.trim().ifBlank { "Performance improvements and bug fixes." },
                "isMandatory" to isMandatory,
                "publishedAt" to System.currentTimeMillis()
            )

            // Save to 'latest' document (which all installed apps check and listen to)
            firestore.collection("app_updates").document("latest").set(data).awaitTask()
            // Archive to historical document
            firestore.collection("app_updates").document("v$versionCode").set(data).awaitTask()

            // Update in-memory state so local device reflects broadcast immediately
            _updateAvailable.value = AppUpdateInfo(
                versionCode = versionCode,
                versionName = versionName,
                downloadUrl = trimmedUrl,
                changelog = changelog,
                isMandatory = isMandatory
            )

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Clear the active cloud broadcast so devices stop prompting for broken updates.
     */
    suspend fun clearPushedUpdate(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            firestore.collection("app_updates").document("latest").delete().awaitTask()
            _updateAvailable.value = null
            _statusMessage.value = "Update broadcast removed from cloud."
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun <T> com.google.android.gms.tasks.Task<T>.awaitTask(): T {
        return kotlin.coroutines.suspendCoroutine { cont ->
            addOnSuccessListener { cont.resumeWith(Result.success(it)) }
            addOnFailureListener { cont.resumeWith(Result.failure(it)) }
        }
    }
}
