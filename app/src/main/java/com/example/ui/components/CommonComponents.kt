package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueuePlayNext
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.Song
import com.example.ui.theme.CustomAppTheme
import com.example.ui.theme.DarkGlassOpaque
import com.example.ui.theme.DestructiveRose
import com.example.ui.theme.DisplayFontFamily
import com.example.ui.theme.Graphite
import com.example.ui.theme.GraphiteElevated
import com.example.ui.theme.LocalAppCustomTheme
import com.example.ui.theme.MidnightBlack
import com.example.ui.theme.MutedGrey
import com.example.ui.theme.Silver
import com.example.ui.theme.SoftWhite
import com.example.ui.theme.WhiteHighlight
import com.example.ui.theme.WhiteHighlightSoft
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.abs
import kotlin.math.sin

fun Modifier.bouncyClick(
    enabled: Boolean = true,
    pressedScale: Float = 0.975f,
    onClick: () -> Unit
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) pressedScale else 1f,
        animationSpec = spring(dampingRatio = 0.80f, stiffness = 540f),
        label = "press_scale"
    )
    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(
            interactionSource = interactionSource,
            indication = LocalIndication.current,
            enabled = enabled,
            onClick = onClick
        )
}

@Composable
fun AnimatedEqualizer(
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    barColor: Color = SoftWhite
) {
    val phase = if (isPlaying) {
        val infiniteTransition = rememberInfiniteTransition(label = "eq_wave")
        val animatedPhase by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = (Math.PI * 2).toFloat(),
            animationSpec = infiniteRepeatable(
                animation = tween(1050, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "eq_phase"
        )
        animatedPhase
    } else {
        0f
    }

    Canvas(
        modifier = modifier
            .width(14.dp)
            .height(14.dp)
    ) {
        val barCount = 3
        val gap = 2.dp.toPx()
        val barWidth = (size.width - gap * (barCount - 1)) / barCount
        val staticHeights = floatArrayOf(0.40f, 0.72f, 0.50f)

        for (i in 0 until barCount) {
            val hFraction = if (isPlaying) {
                (0.28f + 0.68f * abs(sin(phase + i * 1.25f))).coerceIn(0.25f, 1.0f)
            } else {
                staticHeights[i]
            }
            val barH = (size.height * hFraction).coerceAtLeast(3.dp.toPx())
            val x = i * (barWidth + gap)
            val y = size.height - barH
            drawRoundRect(
                color = barColor,
                topLeft = Offset(x, y),
                size = Size(barWidth, barH),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
            )
        }
    }
}

@Composable
fun InteractiveWaveformScrubber(
    progress: Float,
    isPlaying: Boolean,
    seedKey: String,
    onSeekProgress: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val pulsePhase = if (isPlaying) {
        val infiniteTransition = rememberInfiniteTransition(label = "waveform_pulse")
        val animated by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = (Math.PI * 2).toFloat(),
            animationSpec = infiniteRepeatable(
                animation = tween(1800, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "pulse_phase"
        )
        animated
    } else {
        0f
    }

    var isDragging by remember { mutableStateOf(false) }
    var dragProgress by remember { mutableFloatStateOf(progress) }
    val activeProgress = if (isDragging) dragProgress else progress

    val baseBars = remember(seedKey) {
        val seed = abs(seedKey.hashCode())
        List(44) { idx ->
            val wave = 0.36f + 0.54f * abs(sin((idx * 0.42f) + (seed % 17))).toFloat()
            wave.coerceIn(0.22f, 1.0f)
        }
    }

    val theme = LocalAppCustomTheme.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .pointerInput(seedKey) {
                detectTapGestures { offset ->
                    val fraction = (offset.x / size.width.coerceAtLeast(1)).coerceIn(0f, 1f)
                    onSeekProgress(fraction)
                }
            }
            .pointerInput(seedKey) {
                detectHorizontalDragGestures(
                    onDragStart = { offset ->
                        isDragging = true
                        dragProgress = (offset.x / size.width.coerceAtLeast(1)).coerceIn(0f, 1f)
                    },
                    onHorizontalDrag = { change, _ ->
                        change.consume()
                        dragProgress = (change.position.x / size.width.coerceAtLeast(1)).coerceIn(0f, 1f)
                    },
                    onDragEnd = {
                        onSeekProgress(dragProgress)
                        isDragging = false
                    },
                    onDragCancel = {
                        isDragging = false
                    }
                )
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val count = baseBars.size
            val totalSpacing = size.width * 0.38f
            val barWidth = ((size.width - totalSpacing) / count).coerceAtLeast(2.5f)
            val gap = totalSpacing / (count - 1).coerceAtLeast(1)
            val centerY = size.height / 2f
            val inactiveColor = Color(0x30FFFFFF)

            baseBars.forEachIndexed { index, baseAmp ->
                val barFraction = index.toFloat() / (count - 1).coerceAtLeast(1)
                val isLit = barFraction <= activeProgress

                val liveMod = if (isPlaying && isLit) {
                    1f + 0.10f * sin(pulsePhase + index * 0.65f)
                } else {
                    1f
                }
                val barHeight = (size.height * 0.80f * baseAmp * liveMod)
                    .coerceIn(size.height * 0.16f, size.height * 0.88f)
                val x = index * (barWidth + gap)
                val y = centerY - barHeight / 2f

                drawRoundRect(
                    color = if (isLit) theme.primaryColor else inactiveColor,
                    topLeft = Offset(x, y),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
                )
            }
        }
    }
}

@Composable
fun IosAuroraSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    activeColor: Color = SoftWhite
) {
    val thumbOffset by animateFloatAsState(
        targetValue = if (checked) 21f else 2f,
        animationSpec = spring(dampingRatio = 0.78f, stiffness = 460f),
        label = "switch_thumb"
    )

    Box(
        modifier = modifier
            .size(width = 52.dp, height = 48.dp)
            .clip(RoundedCornerShape(24.dp))
            .clickable { onCheckedChange(!checked) },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .width(48.dp)
                .height(28.dp)
                .clip(CircleShape)
                .background(if (checked) activeColor else Color(0x2EFFFFFF))
                .border(0.8.dp, if (checked) activeColor else WhiteHighlight, CircleShape),
            contentAlignment = Alignment.CenterStart
        ) {
            Box(
                modifier = Modifier
                    .offset(x = thumbOffset.dp)
                    .size(23.dp)
                    .clip(CircleShape)
                    .background(if (checked) MidnightBlack else SoftWhite)
            )
        }
    }
}

@Composable
fun SongRowItem(
    song: Song,
    isCurrent: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onFavoriteToggle: () -> Unit,
    onDownloadClick: () -> Unit,
    modifier: Modifier = Modifier,
    onPlayNext: (() -> Unit)? = null,
    onAddToQueue: (() -> Unit)? = null,
    onAddToPlaylist: (() -> Unit)? = null,
    onStartRadio: (() -> Unit)? = null,
    onRemoveFromPlaylist: (() -> Unit)? = null
) {
    val theme = LocalAppCustomTheme.current
    var showOverflowMenu by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (isCurrent) WhiteHighlightSoft else Color.Transparent)
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 8.dp)
            .testTag("song_row_${song.id}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(theme.cardBackground)
                .border(0.6.dp, WhiteHighlightSoft, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (song.coverUrl.isNotBlank()) {
                AsyncImage(
                    model = song.coverUrl,
                    contentDescription = "Cover for ${song.title}",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = MutedGrey,
                    modifier = Modifier.size(22.dp)
                )
            }

            if (isCurrent) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(MidnightBlack.copy(alpha = 0.56f)),
                    contentAlignment = Alignment.Center
                ) {
                    AnimatedEqualizer(isPlaying = isPlaying, barColor = SoftWhite)
                }
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                color = SoftWhite,
                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.SemiBold,
                fontSize = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (song.isDownloaded) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Downloaded",
                        tint = Silver,
                        modifier = Modifier
                            .size(13.dp)
                            .padding(end = 4.dp)
                    )
                }
                Text(
                    text = song.artist,
                    color = Silver,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        IconButton(
            onClick = onFavoriteToggle,
            modifier = Modifier
                .size(48.dp)
                .testTag("btn_favorite_${song.id}")
        ) {
            Icon(
                imageVector = if (song.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                contentDescription = if (song.isFavorite) "Liked" else "Add to Liked Songs",
                tint = if (song.isFavorite) SoftWhite else MutedGrey,
                modifier = Modifier.size(20.dp)
            )
        }

        val hasOverflowActions = onPlayNext != null || onAddToQueue != null || onAddToPlaylist != null || onStartRadio != null || onRemoveFromPlaylist != null
        if (hasOverflowActions) {
            Box {
                IconButton(
                    onClick = { showOverflowMenu = true },
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("btn_more_${song.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Track Options",
                        tint = Silver,
                        modifier = Modifier.size(20.dp)
                    )
                }
                DropdownMenu(
                    expanded = showOverflowMenu,
                    onDismissRequest = { showOverflowMenu = false },
                    modifier = Modifier
                        .background(DarkGlassOpaque)
                        .border(0.8.dp, WhiteHighlight, RoundedCornerShape(12.dp))
                ) {
                    if (onPlayNext != null) {
                        DropdownMenuItem(
                            text = { Text("Play Next", color = SoftWhite) },
                            leadingIcon = { Icon(Icons.Default.QueuePlayNext, contentDescription = null, tint = SoftWhite) },
                            onClick = {
                                showOverflowMenu = false
                                onPlayNext()
                            }
                        )
                    }
                    if (onAddToQueue != null) {
                        DropdownMenuItem(
                            text = { Text("Add to Queue", color = SoftWhite) },
                            leadingIcon = { Icon(Icons.AutoMirrored.Filled.QueueMusic, contentDescription = null, tint = Silver) },
                            onClick = {
                                showOverflowMenu = false
                                onAddToQueue()
                            }
                        )
                    }
                    if (onAddToPlaylist != null) {
                        DropdownMenuItem(
                            text = { Text("Add to Playlist", color = SoftWhite) },
                            leadingIcon = { Icon(Icons.AutoMirrored.Filled.PlaylistAdd, contentDescription = null, tint = Silver) },
                            onClick = {
                                showOverflowMenu = false
                                onAddToPlaylist()
                            }
                        )
                    }
                    if (onStartRadio != null) {
                        DropdownMenuItem(
                            text = { Text("Start Song Radio", color = SoftWhite) },
                            leadingIcon = { Icon(Icons.Default.Radio, contentDescription = null, tint = Silver) },
                            onClick = {
                                showOverflowMenu = false
                                onStartRadio()
                            }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text(if (song.isDownloaded) "Remove Download" else "Download", color = SoftWhite) },
                        leadingIcon = {
                            Icon(
                                imageVector = if (song.isDownloaded) Icons.Default.CheckCircle else Icons.Default.Download,
                                contentDescription = null,
                                tint = Silver
                            )
                        },
                        onClick = {
                            showOverflowMenu = false
                            onDownloadClick()
                        }
                    )
                    if (onRemoveFromPlaylist != null) {
                        DropdownMenuItem(
                            text = { Text("Remove from Playlist", color = DestructiveRose) },
                            leadingIcon = { Icon(Icons.Default.RemoveCircleOutline, contentDescription = null, tint = DestructiveRose) },
                            onClick = {
                                showOverflowMenu = false
                                onRemoveFromPlaylist()
                            }
                        )
                    }
                }
            }
        } else {
            IconButton(
                onClick = onDownloadClick,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("btn_download_${song.id}")
            ) {
                Icon(
                    imageVector = if (song.isDownloaded) Icons.Default.CheckCircle else Icons.Default.Download,
                    contentDescription = "Download Options",
                    tint = if (song.isDownloaded) SoftWhite else MutedGrey,
                    modifier = Modifier.size(19.dp)
                )
            }
        }
    }
}

/**
 * Floating Midnight Liquid Glass Mini-Player sitting seamlessly above Bottom Navigation.
 * Features translucent graphite depth, specular white edge reflection, playback progress bar,
 * Play/Pause + Next controls, and swipe left/right track skipping.
 */
@Composable
fun MiniPlayer(
    song: Song,
    isPlaying: Boolean,
    positionFlow: StateFlow<Long>?,
    durationFlow: StateFlow<Long>?,
    onPlayPause: () -> Unit,
    onSkipPrevious: () -> Unit,
    onSkipNext: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val theme = LocalAppCustomTheme.current
    var swipeDeltaX by remember { mutableFloatStateOf(0f) }

    LiquidGlassSurface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp)
            .testTag("mini_player")
            .pointerInput(song.id) {
                detectHorizontalDragGestures(
                    onDragStart = { swipeDeltaX = 0f },
                    onHorizontalDrag = { _, dragAmount -> swipeDeltaX += dragAmount },
                    onDragEnd = {
                        if (swipeDeltaX < -85f) {
                            onSkipNext()
                        } else if (swipeDeltaX > 85f) {
                            onSkipPrevious()
                        }
                        swipeDeltaX = 0f
                    },
                    onDragCancel = { swipeDeltaX = 0f }
                )
            },
        shape = RoundedCornerShape(20.dp),
        elevation = 16.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 10.dp, end = 6.dp, top = 7.dp, bottom = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(11.dp))
                        .background(theme.cardBackground)
                        .border(0.6.dp, WhiteHighlight, RoundedCornerShape(11.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (song.coverUrl.isNotBlank()) {
                        AsyncImage(
                            model = song.coverUrl,
                            contentDescription = "Cover for ${song.title}",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = SoftWhite,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = song.title,
                        color = SoftWhite,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(1.dp))
                    Text(
                        text = song.artist,
                        color = Silver,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(
                    onClick = onPlayPause,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("btn_mini_play_pause")
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(SoftWhite),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = MidnightBlack,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                IconButton(
                    onClick = onSkipNext,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("btn_mini_skip_next")
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Next Track",
                        tint = SoftWhite,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            MiniPlayerProgressRibbon(
                positionFlow = positionFlow,
                durationFlow = durationFlow,
                fallbackDurationMs = song.durationMs,
                theme = theme
            )
        }
    }
}

@Composable
private fun MiniPlayerProgressRibbon(
    positionFlow: StateFlow<Long>?,
    durationFlow: StateFlow<Long>?,
    fallbackDurationMs: Long,
    theme: CustomAppTheme
) {
    val currentPositionMs by positionFlow?.collectAsState() ?: remember { mutableStateOf(0L) }
    val durationMs by durationFlow?.collectAsState() ?: remember { mutableStateOf(fallbackDurationMs) }
    val effectiveDuration = if (durationMs > 0L) durationMs else fallbackDurationMs.coerceAtLeast(1000L)
    val targetProgress = (currentPositionMs.toFloat() / effectiveDuration.toFloat()).coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = tween(durationMillis = 220),
        label = "mini_progress"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp)
            .padding(bottom = 4.dp)
            .height(2.5.dp)
            .clip(CircleShape)
            .background(Color(0x26FFFFFF))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(animatedProgress)
                .height(2.5.dp)
                .clip(CircleShape)
                .background(theme.primaryColor)
        )
    }
}

@Composable
fun SpotifyQuickPickCard(
    title: String,
    coverUrl: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isLikedCard: Boolean = false
) {
    val theme = LocalAppCustomTheme.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(58.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(theme.cardBackground)
            .border(0.7.dp, WhiteHighlightSoft, RoundedCornerShape(14.dp))
            .bouncyClick { onClick() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(58.dp)
                .background(if (isLikedCard) GraphiteElevated else theme.elevatedBackground),
            contentAlignment = Alignment.Center
        ) {
            if (isLikedCard) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = "Liked Songs",
                    tint = SoftWhite,
                    modifier = Modifier.size(22.dp)
                )
            } else if (coverUrl.isNotBlank()) {
                AsyncImage(
                    model = coverUrl,
                    contentDescription = title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = MutedGrey
                )
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        Text(
            text = title,
            color = SoftWhite,
            fontWeight = FontWeight.Medium,
            fontSize = 13.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .padding(end = 8.dp)
        )
    }
}

@Composable
fun SpotifyMediaCard(
    title: String,
    subtitle: String,
    coverUrl: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isCircular: Boolean = false
) {
    val theme = LocalAppCustomTheme.current
    val shape = if (isCircular) CircleShape else RoundedCornerShape(14.dp)
    Column(
        modifier = modifier
            .width(150.dp)
            .bouncyClick { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(150.dp)
                .clip(shape)
                .background(theme.cardBackground)
                .border(0.7.dp, WhiteHighlightSoft, shape),
            contentAlignment = Alignment.Center
        ) {
            if (coverUrl.isNotBlank()) {
                AsyncImage(
                    model = coverUrl,
                    contentDescription = title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = MutedGrey,
                    modifier = Modifier.size(40.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = title,
            color = SoftWhite,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = subtitle,
            color = Silver,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Editorial monochrome artwork-driven category card (zero colorful sticker backgrounds).
 */
@Composable
fun SpotifyBrowseTile(
    title: String,
    colorHex: Long,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    secondaryColorHex: Long? = null,
    artworkUrl: String = "",
    subtitle: String = "Explore"
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(98.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Graphite)
            .border(0.8.dp, WhiteHighlightSoft, RoundedCornerShape(16.dp))
            .bouncyClick { onClick() }
    ) {
        if (artworkUrl.isNotBlank()) {
            AsyncImage(
                model = artworkUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                alpha = 0.54f
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            MidnightBlack.copy(alpha = 0.92f),
                            MidnightBlack.copy(alpha = 0.62f),
                            Color(0x33171A21)
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text = title,
                color = SoftWhite,
                fontFamily = DisplayFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = subtitle,
                color = Silver,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

fun formatDuration(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(java.util.Locale.US, "%d:%02d", minutes, seconds)
}

fun formatRemainingDuration(currentMs: Long, durationMs: Long): String {
    val remainingSec = ((durationMs - currentMs).coerceAtLeast(0L) / 1000L)
    val minutes = remainingSec / 60
    val seconds = remainingSec % 60
    return String.format(java.util.Locale.US, "-%d:%02d", minutes, seconds)
}
