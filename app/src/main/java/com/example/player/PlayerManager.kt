package com.example.player

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.SpringApplication
import com.example.data.model.AudioQuality
import com.example.data.model.Song
import com.example.data.network.JukeboxClient
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

    private val _audioQuality = MutableStateFlow("320kbps")
    val audioQuality: StateFlow<String> = _audioQuality.asStateFlow()

    private val _sleepTimerMinutes = MutableStateFlow<Int?>(null)
    val sleepTimerMinutes: StateFlow<Int?> = _sleepTimerMinutes.asStateFlow()

    private var mediaController: MediaController? = null
    private var progressJob: Job? = null
    private var sleepTimerJob: Job? = null
    private var lastErrorTimestamp = 0L
    private var lastHandledErrorTimestamp = 0L
    private var lastHandledSongId: String? = null
    private var consecutiveErrors = 0
    private val maxConsecutiveSkips = 5

    init {
        initializeMediaController()
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
                setupPlayerListener(controller)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(context))
    }

    private fun setupPlayerListener(player: Player) {
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                _isPlaying.value = playing
                if (playing) {
                    consecutiveErrors = 0
                    startProgressTracking()
                } else {
                    progressJob?.cancel()
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                val duration = player.duration
                if (duration > 0) {
                    _durationMs.value = duration
                }
                if (playbackState == Player.STATE_READY && player.isPlaying) {
                    consecutiveErrors = 0
                }
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                val currentId = mediaItem?.mediaId ?: return
                val match = _queue.value.find { it.id == currentId }
                if (match != null) {
                    _currentSong.value = match
                    recordSongPlayed(match)
                }
            }

            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                android.util.Log.e("PlayerManager", "Playback error: ${error.errorCodeName} - ${error.message}", error)
                _isPlaying.value = false
                progressJob?.cancel()
                handlePlaybackError(player, error)
            }
        })
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
        val failedSong = _currentSong.value

        // Prevent duplicate concurrent handling on rapid callbacks
        if (now - lastHandledErrorTimestamp < 1200L && failedSong?.id == lastHandledSongId) {
            android.util.Log.d("PlayerManager", "Ignoring duplicate error callback for ${failedSong?.title}")
            return
        }
        lastHandledErrorTimestamp = now
        lastHandledSongId = failedSong?.id

        val player = sourcePlayer ?: getActivePlayer()

        // Check if JioSaavn failed and Jukebox hasn't been tried yet
        if (failedSong != null && !failedSong.isJukeboxEngine && failedSong.localFilePath.isNullOrBlank()) {
            android.util.Log.w(
                "PlayerManager",
                "JioSaavn stream not playable for \"${failedSong.title}\" (${error?.errorCodeName}). Routing to Jukebox fallback stream..."
            )

            val jukeboxSong = JukeboxClient.createJukeboxFallbackSong(failedSong, context)
            _currentSong.value = jukeboxSong
            val updatedQueue = _queue.value.map { if (it.id == failedSong.id) jukeboxSong else it }
            _queue.value = updatedQueue

            scope.launch {
                delay(80)
                val active = getActivePlayer() ?: player
                if (active != null) {
                    try {
                        active.stop()
                        val mediaItems = updatedQueue.map { it.toMediaItem() }
                        val targetIndex = updatedQueue.indexOfFirst { it.id == jukeboxSong.id }.coerceAtLeast(0)
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

        // Both engines failed or already on Jukebox: skip to next song
        android.util.Log.w("PlayerManager", "Audio unplayable on both JioSaavn and Jukebox: ${failedSong?.title}. Skipping to next track...")
        handleAutoSkipOnError(player, error)
    }

    fun handleAutoSkipOnError(sourcePlayer: Player? = null, error: androidx.media3.common.PlaybackException? = null) {
        val now = System.currentTimeMillis()
        if (now - lastErrorTimestamp < 1000L) {
            // Debounce rapid duplicate callbacks
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
            showToast("Multiple tracks failed to load. Please check your network connection.")
            return
        }

        android.util.Log.w("PlayerManager", "Skipping unplayable song: $failedTitle. Advancing to next track...")

        scope.launch {
            delay(100)
            val active = getActivePlayer() ?: player
            if (q.isNotEmpty()) {
                val currentIndex = if (failedSong != null) q.indexOfFirst { it.id == failedSong.id } else -1
                val nextIndex = if (currentIndex in 0 until (q.size - 1)) {
                    currentIndex + 1
                } else {
                    0
                }
                val nextSong = q[nextIndex]
                if (nextSong.id != failedSong?.id) {
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

    fun playSong(song: Song, newQueue: List<Song> = listOf(song)) {
        val baseQueue = if (newQueue.contains(song)) newQueue else listOf(song) + newQueue

        // Pipeline: JioSaavn -> Is playable? -> Yes: play -> No: Jukebox -> Try streaming
        val targetSong = if (song.effectivePlayableUri.isNotBlank()) {
            song
        } else {
            // JioSaavn has no stream, route directly to Jukebox fallback
            android.util.Log.i("PlayerManager", "No JioSaavn stream for \"${song.title}\". Routing to Jukebox...")
            JukeboxClient.createJukeboxFallbackSong(song, context)
        }

        val fullQueue = baseQueue.map { if (it.id == song.id) targetSong else it }

        _currentSong.value = targetSong
        _queue.value = fullQueue

        try {
            val intent = Intent(context, SpringMediaLibraryService::class.java)
            context.startService(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        scope.launch {
            var player = getActivePlayer()
            var retries = 0
            while (player == null && retries < 15) {
                delay(100)
                player = getActivePlayer()
                retries++
            }

            if (player != null) {
                val mediaItems = fullQueue.map { it.toMediaItem() }
                val targetIndex = fullQueue.indexOfFirst { it.id == targetSong.id }.coerceAtLeast(0)

                player.setMediaItems(mediaItems, targetIndex, 0L)
                player.prepare()
                player.play()
                _isPlaying.value = true
                startProgressTracking()
                recordSongPlayed(targetSong)
            }
        }
    }

    fun togglePlayPause() {
        val player = getActivePlayer() ?: return
        if (player.isPlaying) {
            player.pause()
            _isPlaying.value = false
        } else {
            player.play()
            _isPlaying.value = true
            startProgressTracking()
        }
    }

    fun seekTo(positionMs: Long) {
        val player = getActivePlayer() ?: return
        player.seekTo(positionMs)
        _currentPositionMs.value = positionMs
    }

    fun skipNext() {
        consecutiveErrors = 0
        val player = getActivePlayer() ?: return
        if (player.hasNextMediaItem()) {
            player.seekToNextMediaItem()
            player.prepare()
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
            player.prepare()
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
        val current = _currentSong.value ?: return
        val matchingQuality = current.qualityList.find {
            it.label.contains(quality.replace("kbps", ""), ignoreCase = true)
        }
        if (matchingQuality != null && matchingQuality.url != current.audioUrl) {
            val updatedSong = current.copy(audioUrl = matchingQuality.url)
            val player = getActivePlayer()
            val currentPos = player?.currentPosition ?: 0L
            val wasPlaying = player?.isPlaying ?: false

            _currentSong.value = updatedSong
            player?.setMediaItem(updatedSong.toMediaItem(), currentPos)
            if (wasPlaying) {
                player?.play()
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
                    _currentPositionMs.value = player.currentPosition
                    _bufferedPositionMs.value = player.bufferedPosition
                    if (player.duration > 0) {
                        _durationMs.value = player.duration
                    }
                }
                delay(500)
            }
        }
    }

    private fun recordSongPlayed(song: Song) {
        scope.launch(Dispatchers.IO) {
            try {
                (context.applicationContext as? SpringApplication)?.repository?.recordPlayed(song)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
