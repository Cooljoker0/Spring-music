package com.example.player

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.SpringApplication
import com.example.data.model.AudioQuality
import com.example.data.model.Song
import com.example.data.model.SongLyrics
import com.example.data.network.JukeboxClient
import com.example.data.network.LyricsService
import com.example.service.SpringMediaLibraryService
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs

class PlayerManager(private val context: Context) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPositionMs = MutableStateFlow(0L)
    val currentPositionMs: StateFlow<Long> = _currentPositionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    private val _bufferedPositionMs = MutableStateFlow(0L)
    val bufferedPositionMs: StateFlow<Long> = _bufferedPositionMs.asStateFlow()

    private val _isShuffle = MutableStateFlow(false)
    val isShuffle: StateFlow<Boolean> = _isShuffle.asStateFlow()

    private val _repeatMode = MutableStateFlow(Player.REPEAT_MODE_OFF)
    val repeatMode: StateFlow<Int> = _repeatMode.asStateFlow()

    private val _queue = MutableStateFlow<List<Song>>(emptyList())
    val queue: StateFlow<List<Song>> = _queue.asStateFlow()

    private val audioPrefs = context.getSharedPreferences("spring_prefs", Context.MODE_PRIVATE)
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? android.media.AudioManager

    private val _audioQuality = MutableStateFlow(audioPrefs.getString("streaming_quality", "320kbps") ?: "320kbps")
    val audioQuality: StateFlow<String> = _audioQuality.asStateFlow()

    private val _downloadQuality = MutableStateFlow(audioPrefs.getString("download_quality", "Lossless") ?: "Lossless")
    val downloadQuality: StateFlow<String> = _downloadQuality.asStateFlow()

    private val _equalizerEnabled = MutableStateFlow(audioPrefs.getBoolean("equalizer_enabled", true))
    val equalizerEnabled: StateFlow<Boolean> = _equalizerEnabled.asStateFlow()

    private val _gaplessPlayback = MutableStateFlow(audioPrefs.getBoolean("gapless_playback", true))
    val gaplessPlayback: StateFlow<Boolean> = _gaplessPlayback.asStateFlow()

    private val _crossfadeEnabled = MutableStateFlow(audioPrefs.getBoolean("crossfade_enabled", true))
    val crossfadeEnabled: StateFlow<Boolean> = _crossfadeEnabled.asStateFlow()

    private val _automateVolume = MutableStateFlow(audioPrefs.getBoolean("automate_volume", true))
    val automateVolume: StateFlow<Boolean> = _automateVolume.asStateFlow()

    private val _playerVolume = MutableStateFlow(readSystemMusicVolume())
    val playerVolume: StateFlow<Float> = _playerVolume.asStateFlow()

    private val _sleepTimerMinutes = MutableStateFlow<Int?>(null)
    val sleepTimerMinutes: StateFlow<Int?> = _sleepTimerMinutes.asStateFlow()

    private val _currentLyrics = MutableStateFlow<SongLyrics?>(null)
    val currentLyrics: StateFlow<SongLyrics?> = _currentLyrics.asStateFlow()

    private val _isLoadingLyrics = MutableStateFlow(false)
    val isLoadingLyrics: StateFlow<Boolean> = _isLoadingLyrics.asStateFlow()

    private var mediaController: MediaController? = null
    private var listenedPlayer: Player? = null
    private var playJob: Job? = null
    private var progressJob: Job? = null
    private var sleepTimerJob: Job? = null
    private var lyricsJob: Job? = null
    private var similarSongsJob: Job? = null
    private var lastErrorTimestamp = 0L
    private var lastHandledErrorTimestamp = 0L
    private var lastHandledSongId: String? = null
    private var consecutiveErrors = 0
    private val maxConsecutiveSkips = 5
    private var songPlayStartTimeMs: Long = 0L
    private var lastActiveSong: Song? = null
    private var lastStartedTrackId: String? = null
    private var streamBaseOffsetMs: Long = 0L

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(playing: Boolean) {
            _isPlaying.value = playing
            if (playing) {
                startProgressTracking()
            } else {
                progressJob?.cancel()
            }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            val active = listenedPlayer ?: getActivePlayer()
            val duration = active?.duration ?: -1L
            if (duration > 0) {
                _durationMs.value = duration
            }
            if (playbackState == Player.STATE_READY && active?.isPlaying == true) {
                consecutiveErrors = 0
                if (active.playbackParameters.speed != 1.0f || active.playbackParameters.pitch != 1.0f) {
                    active.playbackParameters = PlaybackParameters(1.0f, 1.0f)
                }
            }
        }

        override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) {
            if (abs(playbackParameters.speed - 1.0f) > 0.01f || abs(playbackParameters.pitch - 1.0f) > 0.01f) {
                android.util.Log.w(
                    "PlayerManager",
                    "Detected unexpected playbackParams speed=${playbackParameters.speed}, pitch=${playbackParameters.pitch}; resetting to 1.0x"
                )
                listenedPlayer?.playbackParameters = PlaybackParameters(1.0f, 1.0f)
            }
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            val previous = lastActiveSong
            if (previous != null && songPlayStartTimeMs > 0L && previous.id != mediaItem?.mediaId) {
                val elapsed = (System.currentTimeMillis() - songPlayStartTimeMs).coerceAtLeast(0L)
                val isCompleted = previous.durationMs > 0 && elapsed >= (previous.durationMs * 0.7)
                val isSkipped = elapsed < 30_000L && !isCompleted
                recordSongPlayed(previous, elapsed, isCompleted, isSkipped)
            }

            val currentId = mediaItem?.mediaId ?: return
            val match = _queue.value.find { it.id == currentId }
            if (match != null) {
                val active = listenedPlayer ?: getActivePlayer()
                if (active != null && (active.playbackParameters.speed != 1.0f || active.playbackParameters.pitch != 1.0f)) {
                    active.playbackParameters = PlaybackParameters(1.0f, 1.0f)
                }
                if (lastStartedTrackId != match.id) {
                    streamBaseOffsetMs = 0L
                    _currentPositionMs.value = 0L
                    _durationMs.value = match.durationMs.coerceAtLeast(0L)
                    lastStartedTrackId = match.id
                    songPlayStartTimeMs = System.currentTimeMillis()
                    recordSongPlayed(match, 0L, isCompleted = false, isSkipped = false)
                    scheduleSimilarSongs(match)
                }
                _currentSong.value = match
                lastActiveSong = match
            }
        }

        override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
            android.util.Log.e("PlayerManager", "Playback error: ${error.errorCodeName} - ${error.message}", error)
            _isPlaying.value = false
            progressJob?.cancel()
            handlePlaybackError(listenedPlayer ?: getActivePlayer(), error)
        }
    }

    init {
        initializeMediaController()
    }

    private fun readSystemMusicVolume(): Float {
        val am = audioManager ?: return 0.78f
        val max = am.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        val cur = am.getStreamVolume(android.media.AudioManager.STREAM_MUSIC)
        return (cur.toFloat() / max.toFloat()).coerceIn(0f, 1f)
    }

    private fun initializeMediaController() {
        val sessionToken = SessionToken(
            context,
            ComponentName(context, SpringMediaLibraryService::class.java)
        )
        val controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture.addListener({
            try {
                val controller = controllerFuture.get()
                mediaController = controller
                attachListenerIfNeeded(controller)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(context))
    }

    private fun attachListenerIfNeeded(player: Player) {
        if (listenedPlayer === player) return
        listenedPlayer?.removeListener(playerListener)
        listenedPlayer = player
        player.addListener(playerListener)
        if (player.playbackParameters.speed != 1.0f || player.playbackParameters.pitch != 1.0f) {
            player.playbackParameters = PlaybackParameters(1.0f, 1.0f)
        }
        _isPlaying.value = player.isPlaying
        if (player.duration > 0L) {
            _durationMs.value = player.duration
        }
        if (player.isPlaying) {
            startProgressTracking()
        }
    }

    fun onServicePlayerError(sourcePlayer: Player, error: androidx.media3.common.PlaybackException) {
        // Only handle service-level errors directly if no MediaController listener is already receiving them
        if (listenedPlayer == null) {
            handlePlaybackError(sourcePlayer, error)
        }
    }

    /**
     * Implements the engine streaming flow:
     * Spring Music -> JioSaavn -> Is playable?
     *   - Yes: Play JioSaavn
     *   - No: Jukebox (alexwohlbruck/jukebox) -> Try streaming
     *   - If both fail: Skip to next track
     */
    fun handlePlaybackError(sourcePlayer: Player? = null, error: androidx.media3.common.PlaybackException? = null) {
        val now = System.currentTimeMillis()
        val playerInstance = sourcePlayer ?: getActivePlayer()
        val currentMediaId = playerInstance?.currentMediaItem?.mediaId
        val songToHandle = if (currentMediaId != null) {
            _queue.value.find { it.id == currentMediaId } ?: _currentSong.value
        } else {
            _currentSong.value
        }

        val failedSong = songToHandle
        val errorKey = "${failedSong?.id}_${failedSong?.sourceEngine}"

        // Prevent duplicate concurrent handling on rapid callbacks
        if (now - lastHandledErrorTimestamp < 1000L && errorKey == lastHandledSongId) {
            android.util.Log.d("PlayerManager", "Ignoring duplicate error callback for ${failedSong?.title}")
            return
        }
        lastHandledErrorTimestamp = now
        lastHandledSongId = errorKey

        // Step 1A: If JioSaavn failed on 320kbps, try a verified lower CDN bitrate (160kbps / 96kbps) first before switching to Jukebox live transcoding
        if (failedSong != null && !failedSong.isJukeboxEngine && failedSong.localFilePath.isNullOrBlank()) {
            val nextCdnFallback = failedSong.qualityList.firstOrNull {
                it.url.isNotBlank() && it.url != failedSong.audioUrl
            }
            if (nextCdnFallback != null) {
                val retrySong = failedSong.copy(
                    audioUrl = nextCdnFallback.url,
                    qualityList = failedSong.qualityList.filter { it.url != failedSong.audioUrl }
                )
                _currentSong.value = retrySong
                val updatedQueue = _queue.value.map { if (it.id == failedSong.id) retrySong else it }
                _queue.value = updatedQueue
                playJob?.cancel()
                playJob = scope.launch {
                    val (mediaItems, targetIndex) = withContext(Dispatchers.Default) {
                        val items = updatedQueue.map { it.toMediaItem() }
                        val idx = updatedQueue.indexOfFirst { it.id == retrySong.id }.coerceAtLeast(0)
                        items to idx
                    }
                    val active = getActivePlayer() ?: playerInstance
                    if (active != null) {
                        try {
                            active.playbackParameters = PlaybackParameters(1.0f, 1.0f)
                            active.setMediaItems(mediaItems, targetIndex, 0L)
                            active.prepare()
                            active.play()
                            _isPlaying.value = true
                            startProgressTracking()
                        } catch (_: Exception) {
                            handleAutoSkipOnError(active, null)
                        }
                    }
                }
                return
            }

            android.util.Log.w(
                "PlayerManager",
                "Primary stream 404/failed for \"${failedSong.title}\". Switching to Jukebox streaming fallback..."
            )

            val jukeboxSong = JukeboxClient.createJukeboxFallbackSong(failedSong, context = context)
            _currentSong.value = jukeboxSong
            val currentQ = _queue.value
            val updatedQueue = if (currentQ.any { it.id == failedSong.id }) {
                currentQ.map { if (it.id == failedSong.id) jukeboxSong else it }
            } else {
                listOf(jukeboxSong)
            }
            _queue.value = updatedQueue

            playJob?.cancel()
            playJob = scope.launch {
                val (mediaItems, targetIndex) = withContext(Dispatchers.Default) {
                    val items = updatedQueue.map { it.toMediaItem() }
                    val idx = updatedQueue.indexOfFirst { it.id == jukeboxSong.id }.coerceAtLeast(0)
                    items to idx
                }
                delay(40)
                val active = getActivePlayer() ?: playerInstance
                if (active != null) {
                    try {
                        active.stop()
                        active.clearMediaItems()
                        active.playbackParameters = PlaybackParameters(1.0f, 1.0f)
                        active.setMediaItems(mediaItems, targetIndex, 0L)
                        active.prepare()
                        active.play()
                        _isPlaying.value = true
                        startProgressTracking()
                    } catch (e: Exception) {
                        android.util.Log.e("PlayerManager", "Failed to switch stream to Jukebox: ${e.message}", e)
                        handleAutoSkipOnError(active, null)
                    }
                }
            }
            return
        }

        // Step 2: Both JioSaavn and Jukebox failed or already on Jukebox: skip to next song smoothly!
        android.util.Log.w("PlayerManager", "Song unavailable on both engines: \"${failedSong?.title}\". Skipping to next song...")
        handleAutoSkipOnError(playerInstance, error)
    }

    fun handleAutoSkipOnError(sourcePlayer: Player? = null, error: androidx.media3.common.PlaybackException? = null) {
        val now = System.currentTimeMillis()
        if (now - lastErrorTimestamp < 800L) {
            return
        }
        lastErrorTimestamp = now

        val player = sourcePlayer ?: getActivePlayer()
        val failedSong = _currentSong.value
        val failedTitle = failedSong?.title ?: "Track"
        val q = _queue.value

        consecutiveErrors++
        val limit = if (q.isNotEmpty()) minOf(q.size, maxConsecutiveSkips) else maxConsecutiveSkips

        if (consecutiveErrors > limit) {
            android.util.Log.w("PlayerManager", "Aborting auto-skip: $consecutiveErrors consecutive errors.")
            _isPlaying.value = false
            progressJob?.cancel()
            player?.pause()
            consecutiveErrors = 0
            showToast("Tracks currently unavailable. Please check your network connection.")
            return
        }

        android.util.Log.w("PlayerManager", "Auto-skipping unplayable song: $failedTitle. Advancing to next track...")

        scope.launch {
            delay(60)
            val active = getActivePlayer() ?: player
            if (q.isNotEmpty()) {
                val currentIndex = if (failedSong != null) q.indexOfFirst { it.id == failedSong.id } else -1
                val nextIndex = if (currentIndex in 0 until (q.size - 1)) {
                    currentIndex + 1
                } else if (q.size > 1) {
                    0
                } else {
                    -1
                }

                if (nextIndex in q.indices && q[nextIndex].id != failedSong?.id) {
                    val nextSong = q[nextIndex]
                    playSong(nextSong, q)
                } else if (active != null && active.hasNextMediaItem()) {
                    active.stop()
                    active.seekToNextMediaItem()
                    active.prepare()
                    active.play()
                    _isPlaying.value = true
                    startProgressTracking()
                } else {
                    _isPlaying.value = false
                    progressJob?.cancel()
                }
            } else {
                _isPlaying.value = false
                progressJob?.cancel()
            }
        }
    }

    private fun showToast(message: String) {
        ContextCompat.getMainExecutor(context).execute {
            try {
                android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun getActivePlayer(): Player? {
        return mediaController ?: SpringMediaLibraryService.instance?.player
    }

    private fun applyPreferredQuality(song: Song): Song {
        if (!song.localFilePath.isNullOrBlank() || song.qualityList.isEmpty()) return song
        val targetBitrate = _audioQuality.value.replace("kbps", "", ignoreCase = true).trim()
        val matched = song.qualityList.find {
            it.label.contains(targetBitrate, ignoreCase = true) && it.url.isNotBlank()
        }
        return if (matched != null && matched.url != song.audioUrl) {
            song.copy(audioUrl = matched.url)
        } else {
            song
        }
    }

    fun playSong(song: Song, newQueue: List<Song> = listOf(song)) {
        val baseQueue = if (newQueue.contains(song)) newQueue else listOf(song) + newQueue

        // Update UI state immediately with clicked track and reset progress/offset cleanly
        streamBaseOffsetMs = 0L
        _currentPositionMs.value = 0L
        _bufferedPositionMs.value = 0L
        _durationMs.value = song.durationMs.coerceAtLeast(0L)
        _currentSong.value = song
        _queue.value = baseQueue
        lastStartedTrackId = null
        loadLyricsForSong(song)

        try {
            val intent = Intent(context, SpringMediaLibraryService::class.java)
            context.startService(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        playJob?.cancel()
        playJob = scope.launch {
            val prepared = withContext(Dispatchers.Default) {
                val preferredSong = applyPreferredQuality(song)
                val targetSong = if (preferredSong.effectivePlayableUri.isNotBlank()) {
                    preferredSong
                } else {
                    JukeboxClient.createJukeboxFallbackSong(preferredSong, context = context)
                }
                val fullQueue = baseQueue.map { s ->
                    if (s.id == song.id) {
                        targetSong
                    } else {
                        val qSong = applyPreferredQuality(s)
                        if (qSong.effectivePlayableUri.isBlank()) {
                            JukeboxClient.createJukeboxFallbackSong(qSong, context = context)
                        } else {
                            qSong
                        }
                    }
                }
                val mediaItems = fullQueue.map { it.toMediaItem() }
                val targetIndex = fullQueue.indexOfFirst { it.id == targetSong.id }.coerceAtLeast(0)
                Triple(targetSong, fullQueue, Pair(mediaItems, targetIndex))
            }

            val (targetSong, fullQueue, itemsAndIndex) = prepared
            _currentSong.value = targetSong
            _queue.value = fullQueue

            var player = getActivePlayer()
            var retries = 0
            while (player == null && retries < 20) {
                delay(75)
                player = getActivePlayer()
                retries++
            }

            if (player != null) {
                attachListenerIfNeeded(player)
                player.playbackParameters = PlaybackParameters(1.0f, 1.0f)
                player.setMediaItems(itemsAndIndex.first, itemsAndIndex.second, 0L)
                player.prepare()
                player.play()
                _isPlaying.value = true
                startProgressTracking()
                if (lastStartedTrackId != targetSong.id) {
                    lastStartedTrackId = targetSong.id
                    lastActiveSong = targetSong
                    songPlayStartTimeMs = System.currentTimeMillis()
                    recordSongPlayed(targetSong, 0L, isCompleted = false, isSkipped = false)
                    scheduleSimilarSongs(targetSong)
                }
            }
        }
    }

    fun togglePlayPause() {
        val player = getActivePlayer() ?: return
        attachListenerIfNeeded(player)
        if (player.isPlaying) {
            player.pause()
            _isPlaying.value = false
        } else {
            if (player.playbackParameters.speed != 1.0f || player.playbackParameters.pitch != 1.0f) {
                player.playbackParameters = PlaybackParameters(1.0f, 1.0f)
            }
            if (player.playbackState == Player.STATE_IDLE) {
                player.prepare()
            }
            player.play()
            _isPlaying.value = true
            startProgressTracking()
        }
    }

    fun seekTo(positionMs: Long) {
        val player = getActivePlayer() ?: return
        val current = _currentSong.value

        // For live Jukebox transcoding streams, client-side byte seeking is not supported
        // because the stream is dynamically generated via chunked transfer without Content-Length.
        // We use Jukebox's official startMs & requestedAtMs parameters and track streamBaseOffsetMs.
        if (current != null && current.isJukeboxEngine && current.localFilePath.isNullOrBlank()) {
            streamBaseOffsetMs = positionMs.coerceAtLeast(0L)
            _currentPositionMs.value = streamBaseOffsetMs
            playJob?.cancel()
            playJob = scope.launch {
                val updatedSong = JukeboxClient.createJukeboxFallbackSong(current, startMs = streamBaseOffsetMs, context = context)
                _currentSong.value = updatedSong
                val updatedQueue = _queue.value.map { if (it.id == current.id) updatedSong else it }
                _queue.value = updatedQueue
                try {
                    attachListenerIfNeeded(player)
                    player.stop()
                    player.playbackParameters = PlaybackParameters(1.0f, 1.0f)
                    val targetIndex = updatedQueue.indexOfFirst { it.id == updatedSong.id }.coerceAtLeast(0)
                    player.setMediaItems(updatedQueue.map { it.toMediaItem() }, targetIndex, 0L)
                    player.prepare()
                    player.play()
                    _isPlaying.value = true
                    startProgressTracking()
                } catch (_: Exception) {
                    streamBaseOffsetMs = 0L
                    player.seekTo(positionMs)
                }
            }
        } else {
            streamBaseOffsetMs = 0L
            player.seekTo(positionMs)
            _currentPositionMs.value = positionMs
        }
    }

    fun skipNext() {
        consecutiveErrors = 0
        val previous = lastActiveSong
        if (previous != null && songPlayStartTimeMs > 0L) {
            val elapsed = (System.currentTimeMillis() - songPlayStartTimeMs).coerceAtLeast(0L)
            val isSkipped = elapsed < 30_000L
            val isCompleted = previous.durationMs > 0 && elapsed >= (previous.durationMs * 0.7)
            recordSongPlayed(previous, elapsed, isCompleted, isSkipped)
        }
        val player = getActivePlayer() ?: return
        if (player.hasNextMediaItem()) {
            player.seekToNextMediaItem()
            if (player.playbackState == Player.STATE_IDLE) {
                player.prepare()
            }
            player.play()
        } else {
            val q = _queue.value
            if (q.isNotEmpty()) {
                playSong(q.first(), q)
            }
        }
    }

    fun skipPrevious() {
        consecutiveErrors = 0
        val player = getActivePlayer() ?: return
        if (player.currentPosition > 3000) {
            player.seekTo(0L)
            _currentPositionMs.value = 0L
        } else if (player.hasPreviousMediaItem()) {
            player.seekToPreviousMediaItem()
            if (player.playbackState == Player.STATE_IDLE) {
                player.prepare()
            }
            player.play()
        } else {
            player.seekTo(0L)
            _currentPositionMs.value = 0L
        }
    }

    fun toggleShuffle() {
        val next = !_isShuffle.value
        _isShuffle.value = next
        getActivePlayer()?.shuffleModeEnabled = next
    }

    fun toggleRepeatMode() {
        val next = when (_repeatMode.value) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
        _repeatMode.value = next
        getActivePlayer()?.repeatMode = next
    }

    fun setAudioQuality(quality: String) {
        _audioQuality.value = quality
        audioPrefs.edit().putString("streaming_quality", quality).apply()
        val current = _currentSong.value ?: return
        val matchingQuality = current.qualityList.find {
            it.label.contains(quality.replace("kbps", ""), ignoreCase = true)
        }
        if (matchingQuality != null && matchingQuality.url != current.audioUrl) {
            val updatedSong = current.copy(audioUrl = matchingQuality.url)
            val player = getActivePlayer() ?: return
            val currentPos = (streamBaseOffsetMs + player.currentPosition).coerceAtLeast(0L)
            val wasPlaying = player.isPlaying

            _currentSong.value = updatedSong
            val updatedQueue = _queue.value.map { if (it.id == current.id) updatedSong else it }
            _queue.value = updatedQueue
            val targetIdx = updatedQueue.indexOfFirst { it.id == updatedSong.id }.coerceAtLeast(0)

            attachListenerIfNeeded(player)
            player.playbackParameters = PlaybackParameters(1.0f, 1.0f)
            player.setMediaItems(updatedQueue.map { it.toMediaItem() }, targetIdx, currentPos)
            player.prepare()
            if (wasPlaying) {
                player.play()
            }
        }
    }

    fun setDownloadQuality(quality: String) {
        _downloadQuality.value = quality
        audioPrefs.edit().putString("download_quality", quality).apply()
    }

    fun setEqualizerEnabled(enabled: Boolean) {
        _equalizerEnabled.value = enabled
        audioPrefs.edit().putBoolean("equalizer_enabled", enabled).apply()
    }

    fun setGaplessPlayback(enabled: Boolean) {
        _gaplessPlayback.value = enabled
        audioPrefs.edit().putBoolean("gapless_playback", enabled).apply()
    }

    fun setCrossfadeEnabled(enabled: Boolean) {
        _crossfadeEnabled.value = enabled
        audioPrefs.edit().putBoolean("crossfade_enabled", enabled).apply()
    }

    fun setAutomateVolume(enabled: Boolean) {
        _automateVolume.value = enabled
        audioPrefs.edit().putBoolean("automate_volume", enabled).apply()
    }

    fun setPlaybackVolume(fraction: Float) {
        val clamped = fraction.coerceIn(0f, 1f)
        _playerVolume.value = clamped
        audioManager?.let { am ->
            val max = am.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC).coerceAtLeast(1)
            val target = (clamped * max).toInt().coerceIn(0, max)
            try {
                am.setStreamVolume(android.media.AudioManager.STREAM_MUSIC, target, 0)
            } catch (_: SecurityException) {
                getActivePlayer()?.volume = clamped
            }
        }
    }

    fun setSleepTimer(minutes: Int?) {
        _sleepTimerMinutes.value = minutes
        sleepTimerJob?.cancel()
        if (minutes != null && minutes > 0) {
            sleepTimerJob = scope.launch {
                delay(minutes * 60 * 1000L)
                val player = getActivePlayer()
                player?.pause()
                _isPlaying.value = false
                _sleepTimerMinutes.value = null
            }
        }
    }

    private fun startProgressTracking() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive) {
                val player = getActivePlayer()
                if (player != null) {
                    val rawPos = player.currentPosition.coerceAtLeast(0L)
                    val pos = streamBaseOffsetMs + rawPos
                    if (abs(pos - _currentPositionMs.value) >= 180L || (pos == 0L && _currentPositionMs.value != 0L)) {
                        _currentPositionMs.value = pos
                    }
                    val buf = streamBaseOffsetMs + player.bufferedPosition.coerceAtLeast(0L)
                    if (abs(buf - _bufferedPositionMs.value) >= 1000L) {
                        _bufferedPositionMs.value = buf
                    }
                    val dur = player.duration
                    val fallbackDur = _currentSong.value?.durationMs ?: 0L
                    val effectiveDur = if (dur > 0L) dur else fallbackDur
                    if (effectiveDur > 0L && _durationMs.value != effectiveDur) {
                        _durationMs.value = effectiveDur
                    }
                    delay(if (player.isPlaying) 250L else 500L)
                } else {
                    delay(500L)
                }
            }
        }
    }

    private fun recordSongPlayed(
        song: Song,
        playDurationMs: Long = 0L,
        isCompleted: Boolean = false,
        isSkipped: Boolean = false
    ) {
        scope.launch(Dispatchers.IO) {
            try {
                (context.applicationContext as? SpringApplication)?.repository?.recordPlayed(
                    song = song,
                    playDurationMs = playDurationMs,
                    isCompleted = isCompleted,
                    isSkipped = isSkipped
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun scheduleSimilarSongs(song: Song) {
        similarSongsJob?.cancel()
        similarSongsJob = scope.launch(Dispatchers.IO) {
            delay(650)
            if (_currentSong.value?.id == song.id) {
                loadSimilarSongs(song)
            }
        }
    }

    fun loadSimilarSongs(song: Song) {
        scope.launch(Dispatchers.IO) {
            try {
                (context.applicationContext as? SpringApplication)?.repository?.getSimilarSongs(song)
            } catch (e: Exception) {
                android.util.Log.w("PlayerManager", "Failed to load similar songs: ${e.message}")
            }
        }
    }

    fun recordFeedback(song: Song, feedbackType: com.example.data.recommendation.FeedbackType) {
        scope.launch(Dispatchers.IO) {
            try {
                (context.applicationContext as? SpringApplication)?.repository?.recordSongFeedback(song, feedbackType)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun loadLyricsForSong(song: Song) {
        lyricsJob?.cancel()
        _currentLyrics.value = null
        lyricsJob = scope.launch {
            _isLoadingLyrics.value = true
            try {
                val lyrics = withContext(Dispatchers.IO) {
                    LyricsService.getLyrics(song)
                }
                if (_currentSong.value?.id == song.id) {
                    _currentLyrics.value = lyrics
                }
            } catch (e: Exception) {
                android.util.Log.w("PlayerManager", "Lyrics fetch error: ${e.message}")
            } finally {
                _isLoadingLyrics.value = false
            }
        }
    }
}
