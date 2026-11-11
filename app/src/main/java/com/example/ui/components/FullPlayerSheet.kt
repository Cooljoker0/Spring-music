package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.Song
import com.example.data.model.SongLyrics
import com.example.ui.theme.SpotifyBlack
import com.example.ui.theme.SpotifyCard
import com.example.ui.theme.SpotifyCardElevated
import com.example.ui.theme.SpotifyGreen
import com.example.ui.theme.SpotifyPitchBlack
import com.example.ui.theme.SpotifyTextMuted
import com.example.ui.theme.SpotifyTextPrimary
import com.example.ui.theme.SpotifyTextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullPlayerSheet(
    song: Song,
    isPlaying: Boolean,
    currentPositionMs: Long,
    durationMs: Long,
    isShuffle: Boolean,
    repeatMode: Int,
    audioQuality: String,
    queue: List<Song>,
    lyrics: SongLyrics? = null,
    isLoadingLyrics: Boolean = false,
    onRetryLyrics: (() -> Unit)? = null,
    onPlayPause: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onToggleShuffle: () -> Unit,
    onToggleRepeat: () -> Unit,
    onToggleFavorite: () -> Unit,
    onDownloadClick: () -> Unit,
    onSelectQuality: (String) -> Unit,
    onDismiss: () -> Unit,
    onPlayQueueSong: (Song) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showQueue by remember { mutableStateOf(false) }
    var showLyrics by remember { mutableStateOf(false) }
    var isDraggingSlider by remember { mutableStateOf(false) }
    var dragSliderValue by remember { mutableFloatStateOf(0f) }

    val effectiveDuration = if (durationMs > 0) durationMs else song.durationMs.coerceAtLeast(1000L)
    val calculatedProgress = (currentPositionMs.toFloat() / effectiveDuration.toFloat()).coerceIn(0f, 1f)
    val animatedProgress by androidx.compose.animation.core.animateFloatAsState(
        targetValue = calculatedProgress,
        animationSpec = androidx.compose.animation.core.tween(durationMillis = 180, easing = androidx.compose.animation.core.LinearEasing),
        label = "player_progress_glide"
    )
    val sliderValue = if (isDraggingSlider) dragSliderValue else animatedProgress

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SpotifyPitchBlack,
        dragHandle = null,
        modifier = Modifier
            .fillMaxSize()
            .testTag("full_player_sheet")
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFF2E3D35),
                            Color(0xFF1A2420),
                            SpotifyBlack,
                            SpotifyPitchBlack
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Navigation Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("btn_dismiss_player")
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Collapse",
                            tint = SpotifyTextPrimary,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "PLAYING FROM PLAYLIST",
                            color = SpotifyTextSecondary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = song.album.ifBlank { "Spring" },
                            color = SpotifyTextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = {
                                showLyrics = !showLyrics
                                if (showLyrics) showQueue = false
                            },
                            modifier = Modifier.testTag("btn_toggle_lyrics")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "Lyrics",
                                tint = if (showLyrics) SpotifyGreen else SpotifyTextPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        IconButton(
                            onClick = {
                                showQueue = !showQueue
                                if (showQueue) showLyrics = false
                            },
                            modifier = Modifier.testTag("btn_toggle_queue")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                                contentDescription = "Queue",
                                tint = if (showQueue) SpotifyGreen else SpotifyTextPrimary,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                when {
                    showQueue -> {
                        // Queue list view
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Up Next (${queue.size} songs)",
                                color = SpotifyTextPrimary,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )

                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(queue) { item ->
                                    val isItemCurrent = item.id == song.id
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (isItemCurrent) SpotifyCardElevated else Color.Transparent)
                                            .clickable { onPlayQueueSong(item) }
                                            .padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(44.dp)
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(SpotifyCard),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (item.coverUrl.isNotBlank()) {
                                                AsyncImage(
                                                    model = item.coverUrl,
                                                    contentDescription = null,
                                                    modifier = Modifier.fillMaxSize(),
                                                    contentScale = ContentScale.Crop
                                                )
                                            }
                                            if (isItemCurrent) {
                                                AnimatedEqualizer(isPlaying = isPlaying)
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(12.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = item.title,
                                                color = if (isItemCurrent) SpotifyGreen else SpotifyTextPrimary,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = item.artist,
                                                color = SpotifyTextSecondary,
                                                fontSize = 12.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    showLyrics -> {
                        // Fullscreen / Expanded Karaoke Synchronized Lyrics View
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Mic,
                                        contentDescription = null,
                                        tint = SpotifyGreen,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "LIVE LYRICS",
                                        color = SpotifyGreen,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    )
                                }
                                Text(
                                    text = "Tap line to seek • Tap mic to close",
                                    color = SpotifyTextSecondary,
                                    fontSize = 11.sp
                                )
                            }

                            LyricsView(
                                lyrics = lyrics,
                                isLoading = isLoadingLyrics,
                                currentPositionMs = currentPositionMs,
                                primaryColor = SpotifyGreen,
                                onSeekTo = onSeekTo,
                                onRetry = { onRetryLyrics?.invoke() },
                                onClose = { showLyrics = false },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }

                    else -> {
                    // Large Spotify Square Cover Art
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth(0.92f)
                                .aspectRatio(1f)
                                .testTag("full_player_art"),
                            shape = RoundedCornerShape(8.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
                        ) {
                            if (song.coverUrl.isNotBlank()) {
                                AsyncImage(
                                    model = song.coverUrl,
                                    contentDescription = "Artwork for ${song.title}",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(SpotifyCard),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MusicNote,
                                        contentDescription = null,
                                        tint = SpotifyGreen,
                                        modifier = Modifier.size(80.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (!showQueue) {
                // Track Details & Liked Heart Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = song.title,
                                color = SpotifyTextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 22.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = song.artist,
                                color = SpotifyTextSecondary,
                                fontSize = 16.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = onDownloadClick) {
                                Icon(
                                    imageVector = if (song.isDownloaded) Icons.Default.CheckCircle else Icons.Default.Download,
                                    contentDescription = "Download",
                                    tint = if (song.isDownloaded) SpotifyGreen else SpotifyTextSecondary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            IconButton(
                                onClick = onToggleFavorite,
                                modifier = Modifier.testTag("btn_full_favorite")
                            ) {
                                Icon(
                                    imageVector = if (song.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    contentDescription = "Like Song",
                                    tint = if (song.isFavorite) SpotifyGreen else SpotifyTextSecondary,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Progress Slider
                    Slider(
                        value = sliderValue,
                        onValueChange = {
                            isDraggingSlider = true
                            dragSliderValue = it
                        },
                        onValueChangeFinished = {
                            val targetMs = (dragSliderValue * effectiveDuration).toLong()
                            onSeekTo(targetMs)
                            isDraggingSlider = false
                        },
                        colors = SliderDefaults.colors(
                            thumbColor = SpotifyTextPrimary,
                            activeTrackColor = SpotifyTextPrimary,
                            inactiveTrackColor = Color(0xFF4D4D4D)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("full_player_slider")
                    )

                    // Timestamps
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val currentMs = if (isDraggingSlider) (dragSliderValue * effectiveDuration).toLong() else currentPositionMs
                        Text(
                            text = formatDuration(currentMs),
                            color = SpotifyTextSecondary,
                            fontSize = 12.sp
                        )
                        Text(
                            text = formatDuration(effectiveDuration),
                            color = SpotifyTextSecondary,
                            fontSize = 12.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Spotify Main Controls Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Shuffle
                        IconButton(onClick = onToggleShuffle) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.Shuffle,
                                    contentDescription = "Shuffle",
                                    tint = if (isShuffle) SpotifyGreen else SpotifyTextPrimary,
                                    modifier = Modifier.size(24.dp)
                                )
                                if (isShuffle) {
                                    Box(
                                        modifier = Modifier
                                            .size(4.dp)
                                            .clip(CircleShape)
                                            .background(SpotifyGreen)
                                    )
                                }
                            }
                        }

                        // Previous Track
                        IconButton(
                            onClick = onSkipPrevious,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SkipPrevious,
                                contentDescription = "Previous",
                                tint = SpotifyTextPrimary,
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        // Big Solid White Circular Play/Pause Button (Spotify Icon)
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(SpotifyTextPrimary)
                                .clickable { onPlayPause() }
                                .testTag("btn_full_play_pause"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                tint = Color.Black,
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        // Next Track
                        IconButton(
                            onClick = onSkipNext,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SkipNext,
                                contentDescription = "Next",
                                tint = SpotifyTextPrimary,
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        // Repeat Mode
                        IconButton(onClick = onToggleRepeat) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = if (repeatMode == 1) Icons.Default.RepeatOne else Icons.Default.Repeat,
                                    contentDescription = "Repeat",
                                    tint = if (repeatMode != 0) SpotifyGreen else SpotifyTextPrimary,
                                    modifier = Modifier.size(24.dp)
                                )
                                if (repeatMode != 0) {
                                    Box(
                                        modifier = Modifier
                                            .size(4.dp)
                                            .clip(CircleShape)
                                            .background(SpotifyGreen)
                                    )
                                }
                            }
                        }
                    }

                    // Spotify-Style Quick Lyrics Card (only visible in artwork mode)
                    if (!showLyrics) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    showLyrics = true
                                    showQueue = false
                                }
                                .testTag("lyrics_preview_card"),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1B2921)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x35FFFFFF))
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Mic,
                                            contentDescription = null,
                                            tint = SpotifyGreen,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "LYRICS",
                                            color = SpotifyGreen,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 1.sp
                                        )
                                    }
                                    Text(
                                        text = "Tap for Live Karaoke",
                                        color = SpotifyTextSecondary,
                                        fontSize = 10.sp
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                val previewText = when {
                                    isLoadingLyrics -> "Fetching lyrics..."
                                    lyrics?.isInstrumental == true -> "Instrumental track 🎵"
                                    lyrics?.hasSynced == true -> {
                                        val idx = (lyrics.syncedLyrics.indexOfLast { it.timeMs <= currentPositionMs }).coerceAtLeast(0)
                                        lyrics.syncedLyrics.getOrNull(idx)?.text ?: lyrics.syncedLyrics.firstOrNull()?.text ?: "Lyrics ready"
                                    }
                                    !lyrics?.plainLyrics.isNullOrBlank() -> lyrics.plainLyrics.lines().firstOrNull { it.isNotBlank() } ?: "Lyrics ready"
                                    else -> "Lyrics available • Tap to view"
                                }
                                Text(
                                    text = previewText,
                                    color = SpotifyTextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Bottom Utility Row (Devices Connect, Lyrics, Share, Queue)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Speaker,
                                contentDescription = "Listening Device",
                                tint = SpotifyGreen,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Spring Connect • Device",
                                color = SpotifyGreen,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "Lyrics",
                                tint = if (showLyrics) SpotifyGreen else SpotifyTextSecondary,
                                modifier = Modifier
                                    .size(20.dp)
                                    .clickable {
                                        showLyrics = !showLyrics
                                        if (showLyrics) showQueue = false
                                    }
                                    .testTag("btn_bottom_lyrics")
                            )

                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Share",
                                tint = SpotifyTextSecondary,
                                modifier = Modifier
                                    .size(20.dp)
                                    .clickable { /* Share action */ }
                            )

                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                                contentDescription = "Queue",
                                tint = if (showQueue) SpotifyGreen else SpotifyTextSecondary,
                                modifier = Modifier
                                    .size(22.dp)
                                    .clickable {
                                        showQueue = !showQueue
                                        if (showQueue) showLyrics = false
                                    }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}
