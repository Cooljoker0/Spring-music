package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.media.AudioDeviceInfo
import android.media.AudioManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreHoriz
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
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import kotlinx.coroutines.flow.StateFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.Song
import com.example.data.model.SongLyrics
import com.example.data.recommendation.FeedbackType
import com.example.ui.theme.AuroraCyan
import com.example.ui.theme.AuroraDisplayFontFamily
import com.example.ui.theme.AuroraMagenta
import com.example.ui.theme.AuroraViolet
import com.example.ui.theme.CrispWhite
import com.example.ui.theme.FrostedGlassBorder
import com.example.ui.theme.FrostedGlassSurface
import com.example.ui.theme.FusionAmbience
import com.example.ui.theme.GoldBright
import com.example.ui.theme.IosEmerald
import com.example.ui.theme.IosRed
import com.example.ui.theme.MidnightCanvas
import com.example.ui.theme.MidnightElevated
import com.example.ui.theme.SlateSecondary
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
    onFeedback: ((Song, FeedbackType) -> Unit)? = null
) {
    BackHandler { onDismiss() }

    val context = LocalContext.current
    var showQueue by remember { mutableStateOf(false) }
    var showLyrics by remember { mutableStateOf(false) }
    var showOptionsMenu by remember { mutableStateOf(false) }
    var showDeviceSheet by remember { mutableStateOf(false) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }

    val ambientColors = remember(song.id, song.mood) { FusionAmbience.getColors(song) }
    val primaryGlow = ambientColors.firstOrNull() ?: AuroraCyan

    Box(
        modifier = Modifier
            .fillMaxSize()
            .offset { IntOffset(0, dragOffsetY.roundToInt().coerceAtLeast(0)) }
            .background(MidnightCanvas)
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
            .testTag("full_player_sheet")
    ) {
        // Dynamic Ambient Color Glow behind Album Cover & Sheet
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        primaryGlow.copy(alpha = 0.42f),
                        AuroraViolet.copy(alpha = 0.28f),
                        Color.Transparent
                    )
                ),
                radius = size.width * 0.78f,
                center = Offset(size.width * 0.5f, size.height * 0.34f)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        AuroraMagenta.copy(alpha = 0.22f),
                        Color.Transparent
                    )
                ),
                radius = size.width * 0.65f,
                center = Offset(size.width * 0.25f, size.height * 0.72f)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // iOS Drag Indicator Pill
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .height(5.dp)
                    .clip(CircleShape)
                    .background(Color(0x55FFFFFF))
                    .clickable { onDismiss() }
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Top Header: Collapse Chevron + "PLAYING FROM PLAYLIST" + Circular "..." Menu Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(FrostedGlassSurface)
                        .border(1.dp, FrostedGlassBorder, CircleShape)
                        .clickable { onDismiss() }
                        .testTag("btn_dismiss_player"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Collapse Player",
                        tint = CrispWhite,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f).padding(horizontal = 12.dp)
                ) {
                    Text(
                        text = "PLAYING FROM PLAYLIST",
                        color = SlateSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = song.album.ifBlank { "Spring Aurora" },
                        color = CrispWhite,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Circular "..." Options Menu Button
                Box {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(FrostedGlassSurface)
                            .border(1.dp, FrostedGlassBorder, CircleShape)
                            .clickable { showOptionsMenu = true }
                            .testTag("btn_player_options"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreHoriz,
                            contentDescription = "Track Options",
                            tint = CrispWhite,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = showOptionsMenu,
                        onDismissRequest = { showOptionsMenu = false },
                        modifier = Modifier.background(MidnightElevated)
                    ) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    if (song.isDownloaded) "Downloaded ($audioQuality)" else "Download Track",
                                    color = CrispWhite
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = if (song.isDownloaded) Icons.Default.CheckCircle else Icons.Default.Download,
                                    contentDescription = null,
                                    tint = if (song.isDownloaded) IosEmerald else AuroraCyan
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
                                        color = if (isSel) AuroraCyan else CrispWhite
                                    )
                                },
                                onClick = {
                                    showOptionsMenu = false
                                    onSelectQuality(q)
                                }
                            )
                        }
                        if (onFeedback != null) {
                            DropdownMenuItem(
                                text = { Text("Play More Like This", color = CrispWhite) },
                                leadingIcon = {
                                    Icon(Icons.Default.Whatshot, contentDescription = null, tint = AuroraCyan)
                                },
                                onClick = {
                                    showOptionsMenu = false
                                    onFeedback(song, FeedbackType.PLAY_MORE_LIKE_THIS)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Not Interested", color = CrispWhite) },
                                leadingIcon = {
                                    Icon(Icons.Default.VisibilityOff, contentDescription = null, tint = SlateSecondary)
                                },
                                onClick = {
                                    showOptionsMenu = false
                                    onFeedback(song, FeedbackType.NOT_INTERESTED)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Don't Recommend Artist", color = IosRed) },
                                leadingIcon = {
                                    Icon(Icons.Default.Block, contentDescription = null, tint = IosRed)
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

            Spacer(modifier = Modifier.height(14.dp))

            // Main Stage: Artwork / Live Lyrics / Queue
            when {
                showQueue -> {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            Text(
                                text = "Up Next (${queue.size} songs)",
                                color = CrispWhite,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                        }
                        items(queue, key = { it.id }) { item ->
                            val isItemCurrent = item.id == song.id
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(if (isItemCurrent) FrostedGlassSurface else Color.Transparent)
                                    .border(
                                        width = if (isItemCurrent) 1.dp else 0.dp,
                                        color = if (isItemCurrent) AuroraCyan.copy(alpha = 0.5f) else Color.Transparent,
                                        shape = RoundedCornerShape(14.dp)
                                    )
                                    .clickable { onPlayQueueSong(item) }
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(MidnightElevated),
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
                                        AnimatedEqualizer(isPlaying = isPlaying, barColor = AuroraCyan)
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.title,
                                        color = if (isItemCurrent) AuroraCyan else CrispWhite,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = item.artist,
                                        color = SlateSecondary,
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        if (similarSongs.isNotEmpty()) {
                            item {
                                Spacer(modifier = Modifier.height(10.dp))
                                SimilarSongsSection(
                                    currentSong = song,
                                    similarSongs = similarSongs,
                                    onSongSelected = { s, _ -> onSelectSimilarSong?.invoke(s) ?: onPlayQueueSong(s) },
                                    onFavoriteToggle = { onToggleFavorite() },
                                    onFeedback = { s, type -> onFeedback?.invoke(s, type) }
                                )
                            }
                        }
                    }
                }

                showLyrics -> {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(22.dp))
                            .background(FrostedGlassSurface)
                            .border(1.dp, FrostedGlassBorder, RoundedCornerShape(22.dp))
                            .padding(12.dp)
                    ) {
                        LyricsStage(
                            lyrics = lyrics,
                            isLoading = isLoadingLyrics,
                            positionFlow = positionFlow,
                            onSeekTo = onSeekTo,
                            onRetry = { onRetryLyrics?.invoke() },
                            onClose = { showLyrics = false }
                        )
                    }
                }

                else -> {
                    // Centered Hero Album Cover (24dp corner radius) with Ambient Shadow Glow
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.90f)
                                .aspectRatio(1f)
                                .shadow(
                                    elevation = 34.dp,
                                    shape = RoundedCornerShape(24.dp),
                                    ambientColor = primaryGlow.copy(alpha = 0.7f),
                                    spotColor = AuroraMagenta.copy(alpha = 0.6f)
                                )
                                .clip(RoundedCornerShape(24.dp))
                                .border(1.2.dp, FusionAmbience.GlassBorderBrush, RoundedCornerShape(24.dp))
                                .background(FrostedGlassSurface)
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
                                    tint = AuroraCyan,
                                    modifier = Modifier.size(84.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Track Info & Magenta-Pink Favorite + Share Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = song.title,
                        color = CrispWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = song.artist,
                        color = SlateSecondary,
                        fontSize = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier.testTag("btn_full_favorite")
                    ) {
                        Icon(
                            imageVector = if (song.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favorite Song",
                            tint = if (song.isFavorite) AuroraMagenta else SlateSecondary,
                            modifier = Modifier
                                .size(26.dp)
                                .let {
                                    if (song.isFavorite) it.shadow(10.dp, CircleShape, ambientColor = AuroraMagenta, spotColor = AuroraMagenta) else it
                                }
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
                        modifier = Modifier.testTag("btn_share_song")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share Song",
                            tint = SlateSecondary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Isolated Waveform Scrubber + Tabular Timestamps (only this block ticks every 250ms)
            FullPlayerWaveformSection(
                song = song,
                isPlaying = isPlaying,
                positionFlow = positionFlow,
                durationFlow = durationFlow,
                onSeekTo = onSeekTo
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Transport Controls Row: Shuffle, Previous, 72dp Luminous Frosted Play/Pause, Next, Repeat
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onToggleShuffle) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Shuffle,
                            contentDescription = "Shuffle",
                            tint = if (isShuffle) AuroraCyan else SlateSecondary,
                            modifier = Modifier.size(22.dp)
                        )
                        if (isShuffle) {
                            Spacer(modifier = Modifier.height(3.dp))
                            Box(
                                modifier = Modifier
                                    .size(4.dp)
                                    .clip(CircleShape)
                                    .background(AuroraCyan)
                            )
                        }
                    }
                }

                IconButton(
                    onClick = onSkipPrevious,
                    modifier = Modifier.size(50.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipPrevious,
                        contentDescription = "Previous Track",
                        tint = CrispWhite,
                        modifier = Modifier.size(36.dp)
                    )
                }

                // 72dp Luminous Champagne-Gold Primary Play/Pause Button
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .shadow(
                            elevation = 24.dp,
                            shape = CircleShape,
                            ambientColor = AuroraCyan,
                            spotColor = AuroraViolet
                        )
                        .clip(CircleShape)
                        .background(FusionAmbience.GoldMetallicBrush)
                        .border(
                            width = 1.6.dp,
                            brush = Brush.verticalGradient(listOf(GoldBright, AuroraCyan.copy(alpha = 0.65f))),
                            shape = CircleShape
                        )
                        .bouncyClick(pressedScale = 0.90f) { onPlayPause() }
                        .testTag("btn_full_play_pause"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = MidnightCanvas,
                        modifier = Modifier.size(38.dp)
                    )
                }

                IconButton(
                    onClick = onSkipNext,
                    modifier = Modifier.size(50.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Next Track",
                        tint = CrispWhite,
                        modifier = Modifier.size(36.dp)
                    )
                }

                IconButton(onClick = onToggleRepeat) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = if (repeatMode == 1) Icons.Default.RepeatOne else Icons.Default.Repeat,
                            contentDescription = "Repeat",
                            tint = if (repeatMode != 0) AuroraCyan else SlateSecondary,
                            modifier = Modifier.size(22.dp)
                        )
                        if (repeatMode != 0) {
                            Spacer(modifier = Modifier.height(3.dp))
                            Box(
                                modifier = Modifier
                                    .size(4.dp)
                                    .clip(CircleShape)
                                    .background(AuroraCyan)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Bottom Utility Bar: 3 Sleek Translucent Glass Pills (Lyrics, AirPlay / Devices, Queue)
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
                    label = "Output",
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

            Spacer(modifier = Modifier.height(6.dp))
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
            containerColor = MidnightElevated,
            title = {
                Text("Audio Output & Devices", color = CrispWhite, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Streaming Engine: ${song.sourceEngine.uppercase()} • $audioQuality",
                        color = AuroraCyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    outputDevices.forEach { dev ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(FrostedGlassSurface)
                                .border(1.dp, FrostedGlassBorder, RoundedCornerShape(12.dp))
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Speaker,
                                contentDescription = null,
                                tint = AuroraCyan,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(text = dev, color = CrispWhite, fontSize = 13.sp)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDeviceSheet = false }) {
                    Text("Done", color = AuroraCyan, fontWeight = FontWeight.Bold)
                }
            }
        )
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
    val effectiveDuration = if (durationMs > 0) durationMs else song.durationMs.coerceAtLeast(1000L)
    val calculatedProgress = (currentPositionMs.toFloat() / effectiveDuration.toFloat()).coerceIn(0f, 1f)

    InteractiveWaveformScrubber(
        progress = calculatedProgress,
        isPlaying = isPlaying,
        seedKey = song.id + song.title,
        onSeekProgress = { fraction ->
            val targetMs = (fraction * effectiveDuration).toLong()
            onSeekTo(targetMs)
        },
        modifier = Modifier.testTag("full_player_waveform")
    )

    Spacer(modifier = Modifier.height(6.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = formatDuration(currentPositionMs),
            style = TextStyle(
                fontFamily = AuroraDisplayFontFamily,
                color = SlateSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                fontFeatureSettings = "tnum"
            )
        )
        Text(
            text = formatRemainingDuration(currentPositionMs, effectiveDuration),
            style = TextStyle(
                fontFamily = AuroraDisplayFontFamily,
                color = SlateSecondary,
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
    onSeekTo: (Long) -> Unit,
    onRetry: () -> Unit,
    onClose: () -> Unit
) {
    val currentPositionMs by positionFlow?.collectAsState() ?: remember { mutableStateOf(0L) }
    LyricsView(
        lyrics = lyrics,
        isLoading = isLoading,
        currentPositionMs = currentPositionMs,
        primaryColor = AuroraCyan,
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
    Row(
        modifier = modifier
            .height(42.dp)
            .clip(RoundedCornerShape(21.dp))
            .background(if (isActive) AuroraCyan.copy(alpha = 0.22f) else FrostedGlassSurface)
            .border(
                width = 1.dp,
                color = if (isActive) AuroraCyan.copy(alpha = 0.7f) else FrostedGlassBorder,
                shape = RoundedCornerShape(21.dp)
            )
            .bouncyClick { onClick() }
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isActive) AuroraCyan else SlateSecondary,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            color = if (isActive) AuroraCyan else CrispWhite,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
    }
}
