package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.media.AudioDeviceInfo
import android.media.AudioManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import com.example.data.model.Song
import com.example.data.model.SongLyrics
import com.example.data.recommendation.FeedbackType
import com.example.ui.theme.DarkGlassOpaque
import com.example.ui.theme.DestructiveRose
import com.example.ui.theme.DisplayFontFamily
import com.example.ui.theme.Graphite
import com.example.ui.theme.GraphiteElevated
import com.example.ui.theme.LocalAppCustomTheme
import com.example.ui.theme.MidnightBlack
import com.example.ui.theme.MonoFontFamily
import com.example.ui.theme.MutedGrey
import com.example.ui.theme.Silver
import com.example.ui.theme.SoftWhite
import com.example.ui.theme.WhiteHighlight
import com.example.ui.theme.WhiteHighlightSoft
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.roundToInt

@Composable
fun FullPlayerSheet(
    song: Song,
    isPlaying: Boolean,
    positionFlow: StateFlow<Long>?,
    durationFlow: StateFlow<Long>?,
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
    onPlayQueueSong: (Song) -> Unit,
    similarSongs: List<Song> = emptyList(),
    onSelectSimilarSong: ((Song) -> Unit)? = null,
    onFeedback: ((Song, FeedbackType) -> Unit)? = null,
    autoplayEnabled: Boolean = true,
    autoplayStatusLabel: String? = null,
    onToggleAutoplay: ((Boolean) -> Unit)? = null,
    onStartRadio: ((Song) -> Unit)? = null,
    onMoveQueueItem: ((Int, Int) -> Unit)? = null,
    onRemoveQueueItem: ((Int) -> Unit)? = null,
    onClearUpcomingQueue: (() -> Unit)? = null,
    onPlayNext: ((Song) -> Unit)? = null,
    onAddToQueue: ((Song) -> Unit)? = null
) {
    BackHandler {
        onDismiss()
    }

    val context = LocalContext.current
    val theme = LocalAppCustomTheme.current

    var showQueue by remember { mutableStateOf(false) }
    var showLyrics by remember { mutableStateOf(false) }
    var showOptionsMenu by remember { mutableStateOf(false) }
    var showDeviceSheet by remember { mutableStateOf(false) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }

    val artScale by animateFloatAsState(
        targetValue = if (isPlaying) 1.0f else 0.95f,
        animationSpec = spring(dampingRatio = 0.78f, stiffness = 360f),
        label = "full_art_scale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .offset { IntOffset(0, dragOffsetY.roundToInt().coerceAtLeast(0)) }
            .background(MidnightBlack)
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onVerticalDrag = { _, dragAmount ->
                        if (dragOffsetY + dragAmount >= 0f) {
                            dragOffsetY += dragAmount
                        }
                    },
                    onDragEnd = {
                        if (dragOffsetY > 180f) {
                            onDismiss()
                        }
                        dragOffsetY = 0f
                    },
                    onDragCancel = {
                        dragOffsetY = 0f
                    }
                )
            }
            .testTag("full_player_sheet"),
        contentAlignment = Alignment.TopCenter
    ) {
        // Artwork-derived Ambient Blur Backdrop
        AnimatedContent(
            targetState = song.coverUrl,
            transitionSpec = { fadeIn(tween(420)) togetherWith fadeOut(tween(420)) },
            label = "backdrop_crossfade"
        ) { cover ->
            Box(modifier = Modifier.fillMaxSize()) {
                if (cover.isNotBlank()) {
                    AsyncImage(
                        model = cover,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .adaptiveBackdropBlur(42.dp),
                        contentScale = ContentScale.Crop,
                        alpha = 0.42f
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0x9908090D),
                                    Color(0xCC08090D),
                                    Color(0xF508090D),
                                    MidnightBlack
                                )
                            )
                        )
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 580.dp)
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(WhiteHighlight)
                    .clickable { onDismiss() }
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Top Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("btn_dismiss_player")
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Collapse Player",
                        tint = SoftWhite,
                        modifier = Modifier.size(26.dp)
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp)
                ) {
                    Text(
                        text = (autoplayStatusLabel ?: "NOW PLAYING").uppercase(),
                        color = Silver,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.2.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = song.album.ifBlank { "Spring Music • $audioQuality" },
                        fontFamily = DisplayFontFamily,
                        color = SoftWhite,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Box {
                    IconButton(
                        onClick = { showOptionsMenu = true },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("btn_player_options")
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreHoriz,
                            contentDescription = "Track Options",
                            tint = SoftWhite,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = showOptionsMenu,
                        onDismissRequest = { showOptionsMenu = false },
                        modifier = Modifier
                            .background(DarkGlassOpaque)
                            .border(0.8.dp, WhiteHighlight, RoundedCornerShape(12.dp))
                    ) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    if (song.isDownloaded) "Downloaded ($audioQuality)" else "Download Track",
                                    color = SoftWhite
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = if (song.isDownloaded) Icons.Default.CheckCircle else Icons.Default.Download,
                                    contentDescription = null,
                                    tint = SoftWhite
                                )
                            },
                            onClick = {
                                showOptionsMenu = false
                                onDownloadClick()
                            }
                        )
                        listOf("320kbps", "160kbps", "128kbps").forEach { q ->
                            val isSel = audioQuality.equals(q, ignoreCase = true)
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = if (isSel) "Quality: $q (Active)" else "Switch Quality: $q",
                                        color = if (isSel) SoftWhite else Silver,
                                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                onClick = {
                                    showOptionsMenu = false
                                    onSelectQuality(q)
                                }
                            )
                        }
                        if (onStartRadio != null) {
                            DropdownMenuItem(
                                text = { Text("Go to Song Radio", color = SoftWhite, fontWeight = FontWeight.SemiBold) },
                                leadingIcon = {
                                    Icon(Icons.Default.Radio, contentDescription = null, tint = SoftWhite)
                                },
                                onClick = {
                                    showOptionsMenu = false
                                    onStartRadio(song)
                                }
                            )
                        }
                        if (onFeedback != null) {
                            DropdownMenuItem(
                                text = { Text("Play More Like This", color = SoftWhite) },
                                leadingIcon = {
                                    Icon(Icons.Default.Whatshot, contentDescription = null, tint = Silver)
                                },
                                onClick = {
                                    showOptionsMenu = false
                                    onFeedback(song, FeedbackType.PLAY_MORE_LIKE_THIS)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Not Interested", color = Silver) },
                                leadingIcon = {
                                    Icon(Icons.Default.VisibilityOff, contentDescription = null, tint = Silver)
                                },
                                onClick = {
                                    showOptionsMenu = false
                                    onFeedback(song, FeedbackType.NOT_INTERESTED)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Don't Recommend Artist", color = DestructiveRose) },
                                leadingIcon = {
                                    Icon(Icons.Default.Block, contentDescription = null, tint = DestructiveRose)
                                },
                                onClick = {
                                    showOptionsMenu = false
                                    onFeedback(song, FeedbackType.DONT_RECOMMEND_ARTIST)
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Main Stage: Artwork / Lyrics / Queue
            when {
                showQueue -> {
                    QueueStage(
                        currentSong = song,
                        isPlaying = isPlaying,
                        queue = queue,
                        similarSongs = similarSongs,
                        autoplayEnabled = autoplayEnabled,
                        onToggleAutoplay = onToggleAutoplay,
                        onPlayQueueSong = onPlayQueueSong,
                        onMoveQueueItem = onMoveQueueItem,
                        onRemoveQueueItem = onRemoveQueueItem,
                        onClearUpcomingQueue = onClearUpcomingQueue,
                        onStartRadio = onStartRadio,
                        onSelectSimilarSong = onSelectSimilarSong,
                        onToggleFavorite = onToggleFavorite,
                        onFeedback = onFeedback,
                        onPlayNext = onPlayNext,
                        onAddToQueue = onAddToQueue,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    )
                }

                showLyrics -> {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        LyricsStage(
                            lyrics = lyrics,
                            isLoading = isLoadingLyrics,
                            positionFlow = positionFlow,
                            primaryColor = SoftWhite,
                            fadeColor = MidnightBlack,
                            onSeekTo = onSeekTo,
                            onRetry = { onRetryLyrics?.invoke() },
                            onClose = { showLyrics = false }
                        )
                    }
                }

                else -> {
                    BoxWithConstraints(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        val artSize = min(maxWidth * 0.90f, maxHeight * 0.92f)
                        AnimatedContent(
                            targetState = song.id,
                            transitionSpec = {
                                (fadeIn(tween(260)) + scaleIn(initialScale = 0.94f, animationSpec = tween(260)))
                                    .togetherWith(fadeOut(tween(180)) + scaleOut(targetScale = 0.96f, animationSpec = tween(180)))
                            },
                            label = "artwork_switch"
                        ) { _ ->
                            Box(
                                modifier = Modifier
                                    .size(artSize)
                                    .graphicsLayer {
                                        scaleX = artScale
                                        scaleY = artScale
                                    }
                                    .shadow(
                                        elevation = 24.dp,
                                        shape = RoundedCornerShape(22.dp),
                                        ambientColor = Color.Black,
                                        spotColor = Color.Black
                                    )
                                    .clip(RoundedCornerShape(22.dp))
                                    .background(Graphite)
                                    .border(0.85.dp, WhiteHighlight, RoundedCornerShape(22.dp))
                                    .testTag("full_player_art"),
                                contentAlignment = Alignment.Center
                            ) {
                                if (song.coverUrl.isNotBlank()) {
                                    AsyncImage(
                                        model = song.coverUrl,
                                        contentDescription = "Artwork for ${song.title}",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.MusicNote,
                                        contentDescription = null,
                                        tint = MutedGrey,
                                        modifier = Modifier.size(72.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Track Metadata + Favorite + Share
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = song.title,
                        fontFamily = DisplayFontFamily,
                        color = SoftWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = song.artist,
                        color = Silver,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("btn_full_favorite")
                    ) {
                        Icon(
                            imageVector = if (song.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favorite Song",
                            tint = if (song.isFavorite) SoftWhite else Silver,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "${song.title} — ${song.artist}")
                                putExtra(
                                    Intent.EXTRA_TEXT,
                                    "Listening to ${song.title} by ${song.artist} on Spring Music"
                                )
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share song"))
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("btn_share_song")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share Song",
                            tint = Silver,
                            modifier = Modifier.size(21.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            FullPlayerWaveformSection(
                song = song,
                isPlaying = isPlaying,
                positionFlow = positionFlow,
                durationFlow = durationFlow,
                onSeekTo = onSeekTo
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Floating Liquid Glass Transport Control Panel
            LiquidGlassSurface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                elevation = 16.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onToggleShuffle,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Shuffle,
                                contentDescription = "Shuffle",
                                tint = if (isShuffle) SoftWhite else MutedGrey,
                                modifier = Modifier.size(21.dp)
                            )
                            if (isShuffle) {
                                Spacer(modifier = Modifier.height(3.dp))
                                Box(
                                    modifier = Modifier
                                        .size(4.dp)
                                        .clip(CircleShape)
                                        .background(SoftWhite)
                                )
                            }
                        }
                    }

                    IconButton(
                        onClick = onSkipPrevious,
                        modifier = Modifier.size(52.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipPrevious,
                            contentDescription = "Previous Track",
                            tint = SoftWhite,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(SoftWhite)
                            .clickable { onPlayPause() }
                            .testTag("btn_full_play_pause"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = MidnightBlack,
                            modifier = Modifier.size(34.dp)
                        )
                    }

                    IconButton(
                        onClick = onSkipNext,
                        modifier = Modifier.size(52.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = "Next Track",
                            tint = SoftWhite,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    IconButton(
                        onClick = onToggleRepeat,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = if (repeatMode == 1) Icons.Default.RepeatOne else Icons.Default.Repeat,
                                contentDescription = "Repeat",
                                tint = if (repeatMode != 0) SoftWhite else MutedGrey,
                                modifier = Modifier.size(21.dp)
                            )
                            if (repeatMode != 0) {
                                Spacer(modifier = Modifier.height(3.dp))
                                Box(
                                    modifier = Modifier
                                        .size(4.dp)
                                        .clip(CircleShape)
                                        .background(SoftWhite)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Utility Bar: Lyrics, Audio Info / Output, Queue
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PlayerUtilityPill(
                    icon = Icons.Default.ChatBubbleOutline,
                    label = "Lyrics",
                    isActive = showLyrics,
                    onClick = {
                        showLyrics = !showLyrics
                        if (showLyrics) showQueue = false
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("btn_bottom_lyrics")
                )

                PlayerUtilityPill(
                    icon = Icons.Default.Speaker,
                    label = audioQuality,
                    isActive = showDeviceSheet,
                    onClick = { showDeviceSheet = true },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("btn_bottom_airplay")
                )

                PlayerUtilityPill(
                    icon = Icons.AutoMirrored.Filled.QueueMusic,
                    label = "Queue",
                    isActive = showQueue,
                    onClick = {
                        showQueue = !showQueue
                        if (showQueue) showLyrics = false
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("btn_bottom_queue")
                )
            }

            Spacer(modifier = Modifier.height(4.dp))
        }
    }

    if (showDeviceSheet) {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        val outputDevices = remember(showDeviceSheet) {
            audioManager?.getDevices(AudioManager.GET_DEVICES_OUTPUTS)?.map { device ->
                val typeLabel = when (device.type) {
                    AudioDeviceInfo.TYPE_BLUETOOTH_A2DP, AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> "Bluetooth Audio"
                    AudioDeviceInfo.TYPE_WIRED_HEADSET, AudioDeviceInfo.TYPE_WIRED_HEADPHONES -> "Wired Headphones"
                    AudioDeviceInfo.TYPE_USB_HEADSET, AudioDeviceInfo.TYPE_USB_DEVICE -> "USB DAC / Audio"
                    AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> "Phone Speaker"
                    else -> "Audio Output"
                }
                val name = device.productName?.toString()?.takeIf { it.isNotBlank() } ?: typeLabel
                "$name ($typeLabel)"
            }?.distinct() ?: listOf("Phone Speaker")
        }

        AlertDialog(
            onDismissRequest = { showDeviceSheet = false },
            containerColor = DarkGlassOpaque,
            title = {
                Text("Audio & Output", fontFamily = DisplayFontFamily, color = SoftWhite, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "${song.sourceEngine.uppercase()} • $audioQuality Stream",
                        color = Silver,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    outputDevices.forEach { dev ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Graphite)
                                .border(0.7.dp, WhiteHighlightSoft, RoundedCornerShape(12.dp))
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Speaker,
                                contentDescription = null,
                                tint = SoftWhite,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(text = dev, color = SoftWhite, fontSize = 13.sp)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDeviceSheet = false }) {
                    Text("Close", color = SoftWhite, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@Composable
private fun QueueStage(
    currentSong: Song,
    isPlaying: Boolean,
    queue: List<Song>,
    similarSongs: List<Song>,
    autoplayEnabled: Boolean,
    onToggleAutoplay: ((Boolean) -> Unit)?,
    onPlayQueueSong: (Song) -> Unit,
    onMoveQueueItem: ((Int, Int) -> Unit)?,
    onRemoveQueueItem: ((Int) -> Unit)?,
    onClearUpcomingQueue: (() -> Unit)?,
    onStartRadio: ((Song) -> Unit)?,
    onSelectSimilarSong: ((Song) -> Unit)?,
    onToggleFavorite: () -> Unit,
    onFeedback: ((Song, FeedbackType) -> Unit)?,
    onPlayNext: ((Song) -> Unit)?,
    onAddToQueue: ((Song) -> Unit)?,
    modifier: Modifier = Modifier
) {
    val currentQueueIndex = remember(queue, currentSong.id) {
        queue.indexOfFirst { it.id == currentSong.id }
    }
    val upNextIndexed = remember(queue, currentQueueIndex) {
        if (currentQueueIndex >= 0 && currentQueueIndex < queue.lastIndex) {
            queue.mapIndexed { idx, s -> idx to s }.drop(currentQueueIndex + 1)
        } else if (currentQueueIndex < 0) {
            queue.mapIndexed { idx, s -> idx to s }
        } else {
            emptyList()
        }
    }

    var draggingActualIdx by remember { mutableStateOf<Int?>(null) }
    var dragDeltaY by remember { mutableFloatStateOf(0f) }
    val rowHeightPx = with(LocalDensity.current) { 64.dp.toPx() }

    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Currently Playing Card
        item(key = "queue_now_playing_card") {
            Text(
                text = "CURRENTLY PLAYING",
                color = MutedGrey,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.1.sp,
                modifier = Modifier.padding(bottom = 6.dp)
            )
            LiquidGlassSurface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                elevation = 10.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(GraphiteElevated),
                        contentAlignment = Alignment.Center
                    ) {
                        if (currentSong.coverUrl.isNotBlank()) {
                            AsyncImage(
                                model = currentSong.coverUrl,
                                contentDescription = currentSong.title,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MidnightBlack.copy(alpha = 0.50f)),
                            contentAlignment = Alignment.Center
                        ) {
                            AnimatedEqualizer(isPlaying = isPlaying, barColor = SoftWhite)
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = currentSong.title,
                            color = SoftWhite,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = currentSong.artist,
                            color = Silver,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        // Up Next Header + Actions
        item(key = "queue_up_next_header") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Up Next (${upNextIndexed.size})",
                    fontFamily = DisplayFontFamily,
                    color = SoftWhite,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (onClearUpcomingQueue != null && upNextIndexed.isNotEmpty()) {
                        LiquidGlassPill(
                            onClick = onClearUpcomingQueue,
                            modifier = Modifier.testTag("btn_queue_clear")
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.ClearAll, contentDescription = "Clear Queue", tint = Silver, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Clear", color = Silver, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }

                    if (onStartRadio != null) {
                        LiquidGlassPill(
                            onClick = { onStartRadio(currentSong) },
                            modifier = Modifier.testTag("btn_queue_start_radio")
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Radio, contentDescription = "Start Song Radio", tint = SoftWhite, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(5.dp))
                                Text("Radio", color = SoftWhite, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }

        if (onToggleAutoplay != null) {
            item(key = "queue_autoplay_toggle") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Graphite)
                        .border(0.7.dp, WhiteHighlightSoft, RoundedCornerShape(14.dp))
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
                        Text(
                            text = "Autoplay similar music",
                            color = SoftWhite,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = if (autoplayEnabled) "Continues with similar songs" else "Stops at end of queue",
                            color = Silver,
                            fontSize = 11.sp
                        )
                    }
                    IosAuroraSwitch(
                        checked = autoplayEnabled,
                        onCheckedChange = onToggleAutoplay,
                        activeColor = SoftWhite,
                        modifier = Modifier.testTag("switch_queue_autoplay")
                    )
                }
            }
        }

        if (upNextIndexed.isEmpty()) {
            item(key = "queue_empty_up_next") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Graphite)
                        .border(0.7.dp, WhiteHighlightSoft, RoundedCornerShape(14.dp))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (autoplayEnabled) "Up next is empty — Autoplay will continue after this track" else "No upcoming tracks queued",
                        color = Silver,
                        fontSize = 12.sp
                    )
                }
            }
        } else {
            itemsIndexed(
                items = upNextIndexed,
                key = { _, pair -> "upnext_${pair.first}_${pair.second.id}" }
            ) { listPos, (actualIdx, item) ->
                val isDragging = draggingActualIdx == actualIdx
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .zIndex(if (isDragging) 2f else 0f)
                        .offset {
                            IntOffset(0, if (isDragging) dragDeltaY.roundToInt() else 0)
                        }
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isDragging) DarkGlassOpaque else Color.Transparent)
                        .then(
                            if (isDragging) Modifier.border(0.8.dp, WhiteHighlight, RoundedCornerShape(12.dp))
                            else Modifier
                        )
                        .clickable { onPlayQueueSong(item) }
                        .padding(horizontal = 4.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (onMoveQueueItem != null) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .pointerInput(actualIdx, upNextIndexed.size) {
                                    detectVerticalDragGestures(
                                        onDragStart = {
                                            draggingActualIdx = actualIdx
                                            dragDeltaY = 0f
                                        },
                                        onVerticalDrag = { change, dragAmount ->
                                            change.consume()
                                            dragDeltaY += dragAmount
                                            val curActual = draggingActualIdx ?: actualIdx
                                            val minUpNextActual = upNextIndexed.firstOrNull()?.first ?: 0
                                            val maxUpNextActual = upNextIndexed.lastOrNull()?.first ?: curActual
                                            if (dragDeltaY > rowHeightPx * 0.75f && curActual < maxUpNextActual) {
                                                onMoveQueueItem(curActual, curActual + 1)
                                                draggingActualIdx = curActual + 1
                                                dragDeltaY -= rowHeightPx
                                            } else if (dragDeltaY < -rowHeightPx * 0.75f && curActual > minUpNextActual) {
                                                onMoveQueueItem(curActual, curActual - 1)
                                                draggingActualIdx = curActual - 1
                                                dragDeltaY += rowHeightPx
                                            }
                                        },
                                        onDragEnd = {
                                            draggingActualIdx = null
                                            dragDeltaY = 0f
                                        },
                                        onDragCancel = {
                                            draggingActualIdx = null
                                            dragDeltaY = 0f
                                        }
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.DragHandle,
                                contentDescription = "Reorder ${item.title}",
                                tint = if (isDragging) SoftWhite else MutedGrey,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(GraphiteElevated),
                        contentAlignment = Alignment.Center
                    ) {
                        if (item.coverUrl.isNotBlank()) {
                            AsyncImage(
                                model = item.coverUrl,
                                contentDescription = item.title,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(Icons.Default.MusicNote, contentDescription = null, tint = MutedGrey)
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.title,
                            color = SoftWhite,
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = item.artist,
                            color = Silver,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (onRemoveQueueItem != null) {
                        IconButton(
                            onClick = { onRemoveQueueItem(actualIdx) },
                            modifier = Modifier.size(44.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Remove ${item.title} from queue",
                                tint = MutedGrey,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
                if (listPos < upNextIndexed.lastIndex) {
                    HorizontalDivider(
                        color = WhiteHighlightSoft,
                        thickness = 0.5.dp,
                        modifier = Modifier.padding(start = 60.dp)
                    )
                }
            }
        }

        if (similarSongs.isNotEmpty()) {
            item(key = "queue_similar_songs") {
                Spacer(modifier = Modifier.height(14.dp))
                SimilarSongsSection(
                    currentSong = currentSong,
                    similarSongs = similarSongs,
                    onSongSelected = { s, _ -> onSelectSimilarSong?.invoke(s) ?: onPlayQueueSong(s) },
                    onFavoriteToggle = { onToggleFavorite() },
                    onFeedback = { s, type -> onFeedback?.invoke(s, type) },
                    onStartRadio = onStartRadio,
                    onPlayNext = onPlayNext,
                    onAddToQueue = onAddToQueue
                )
            }
        }
    }
}

@Composable
private fun FullPlayerWaveformSection(
    song: Song,
    isPlaying: Boolean,
    positionFlow: StateFlow<Long>?,
    durationFlow: StateFlow<Long>?,
    onSeekTo: (Long) -> Unit
) {
    val currentPositionMs by positionFlow?.collectAsState() ?: remember { mutableStateOf(0L) }
    val durationMs by durationFlow?.collectAsState() ?: remember { mutableStateOf(song.durationMs) }
    val effectiveDuration = if (durationMs > 0L) durationMs else song.durationMs.coerceAtLeast(1000L)
    val progress = (currentPositionMs.toFloat() / effectiveDuration.toFloat()).coerceIn(0f, 1f)

    InteractiveWaveformScrubber(
        progress = progress,
        isPlaying = isPlaying,
        seedKey = song.id,
        onSeekProgress = { fraction ->
            onSeekTo((fraction * effectiveDuration).toLong())
        },
        modifier = Modifier.testTag("full_player_waveform")
    )

    Spacer(modifier = Modifier.height(4.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = formatDuration(currentPositionMs),
            style = TextStyle(
                fontFamily = MonoFontFamily,
                color = Silver,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                fontFeatureSettings = "tnum"
            )
        )
        Text(
            text = formatRemainingDuration(currentPositionMs, effectiveDuration),
            style = TextStyle(
                fontFamily = MonoFontFamily,
                color = Silver,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                fontFeatureSettings = "tnum"
            )
        )
    }
}

@Composable
private fun LyricsStage(
    lyrics: SongLyrics?,
    isLoading: Boolean,
    positionFlow: StateFlow<Long>?,
    primaryColor: Color,
    fadeColor: Color,
    onSeekTo: (Long) -> Unit,
    onRetry: () -> Unit,
    onClose: () -> Unit
) {
    val currentPositionMs by positionFlow?.collectAsState() ?: remember { mutableStateOf(0L) }
    LyricsView(
        lyrics = lyrics,
        isLoading = isLoading,
        currentPositionMs = currentPositionMs,
        primaryColor = primaryColor,
        fadeColor = fadeColor,
        onSeekTo = onSeekTo,
        onRetry = onRetry,
        onClose = onClose,
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
private fun PlayerUtilityPill(
    icon: ImageVector,
    label: String,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    LiquidGlassPill(
        isActive = isActive,
        onClick = onClick,
        modifier = modifier.height(46.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isActive) MidnightBlack else SoftWhite,
                modifier = Modifier.size(17.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                color = if (isActive) MidnightBlack else SoftWhite,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
        }
    }
}
