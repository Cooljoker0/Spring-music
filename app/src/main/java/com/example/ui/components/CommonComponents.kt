package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
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
import com.example.ui.theme.AuroraCyan
import com.example.ui.theme.AuroraMagenta
import com.example.ui.theme.AuroraViolet
import com.example.ui.theme.CrispWhite
import com.example.ui.theme.FrostedGlassBorder
import com.example.ui.theme.FrostedGlassSurface
import com.example.ui.theme.FusionAmbience
import com.example.ui.theme.GoldBright
import com.example.ui.theme.IosEmerald
import com.example.ui.theme.MidnightCanvas
import com.example.ui.theme.MidnightElevated
import com.example.ui.theme.SlateMuted
import com.example.ui.theme.SlateSecondary
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.abs
import kotlin.math.sin

/**
 * Tactile iOS spring bounce press modifier for fluid responsiveness.
 */
fun Modifier.bouncyClick(
    enabled: Boolean = true,
    pressedScale: Float = 0.96f,
    onClick: () -> Unit
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) pressedScale else 1f,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 550f),
        label = "bouncy_scale"
    )
    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            enabled = enabled,
            onClick = onClick
        )
}

/**
 * Pure Canvas-drawn audio frequency waveform bars (zero per-frame relayouts).
 */
@Composable
fun AnimatedEqualizer(
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    barColor: Color = AuroraCyan
) {
    val phase = if (isPlaying) {
        val infiniteTransition = rememberInfiniteTransition(label = "eq_wave")
        val animatedPhase by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = (Math.PI * 2).toFloat(),
            animationSpec = infiniteRepeatable(
                animation = tween(1100, easing = FastOutSlowInEasing),
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
            .width(16.dp)
            .height(16.dp)
    ) {
        val barCount = 4
        val gap = 2.dp.toPx()
        val barWidth = (size.width - gap * (barCount - 1)) / barCount
        val staticHeights = floatArrayOf(0.35f, 0.58f, 0.45f, 0.30f)

        for (i in 0 until barCount) {
            val hFraction = if (isPlaying) {
                (0.28f + 0.68f * abs(sin(phase + i * 1.15f))).coerceIn(0.22f, 1.0f)
            } else {
                staticHeights[i]
            }
            val barH = (size.height * hFraction).coerceAtLeast(3.dp.toPx())
            val x = i * (barWidth + gap)
            val y = (size.height - barH) / 2f
            drawRoundRect(
                color = if (i % 2 == 0) barColor else AuroraViolet,
                topLeft = Offset(x, y),
                size = Size(barWidth, barH),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
            )
        }
    }
}

/**
 * Interactive real-time audio frequency waveform scrubber (cyan-to-violet gradient fill).
 * Only animates pulse when actively playing to save GPU cycles when paused.
 */
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
            val wave = 0.38f + 0.52f * abs(sin((idx * 0.42f) + (seed % 17))).toFloat()
            wave.coerceIn(0.22f, 1.0f)
        }
    }

    val activeBrush = remember {
        Brush.horizontalGradient(
            colors = listOf(GoldBright, AuroraCyan, AuroraViolet, AuroraMagenta)
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(42.dp)
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
            val totalSpacing = size.width * 0.34f
            val barWidth = ((size.width - totalSpacing) / count).coerceAtLeast(2.5f)
            val gap = totalSpacing / (count - 1).coerceAtLeast(1)
            val centerY = size.height / 2f
            val inactiveColor = Color(0x36EBEBF5)

            baseBars.forEachIndexed { index, baseAmp ->
                val barFraction = index.toFloat() / (count - 1).coerceAtLeast(1)
                val isLit = barFraction <= activeProgress

                val liveMod = if (isPlaying && isLit) {
                    1f + 0.16f * sin(pulsePhase + index * 0.65f)
                } else {
                    1f
                }
                val barHeight = (size.height * 0.85f * baseAmp * liveMod)
                    .coerceIn(size.height * 0.16f, size.height * 0.94f)
                val x = index * (barWidth + gap)
                val y = centerY - barHeight / 2f

                if (isLit) {
                    drawRoundRect(
                        brush = activeBrush,
                        topLeft = Offset(x, y),
                        size = Size(barWidth, barHeight),
                        cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
                    )
                } else {
                    drawRoundRect(
                        color = inactiveColor,
                        topLeft = Offset(x, y),
                        size = Size(barWidth, barHeight),
                        cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
                    )
                }
            }
        }
    }
}

/**
 * Custom iOS-style switch with Emerald green (#34C759) active track and spring thumb.
 */
@Composable
fun IosAuroraSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    activeColor: Color = IosEmerald
) {
    val thumbOffset by animateFloatAsState(
        targetValue = if (checked) 20f else 2f,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = 420f),
        label = "ios_switch"
    )

    Box(
        modifier = modifier
            .width(50.dp)
            .height(30.dp)
            .clip(CircleShape)
            .background(if (checked) activeColor else Color(0x33EBEBF5))
            .border(
                width = 0.8.dp,
                color = if (checked) activeColor.copy(alpha = 0.8f) else FrostedGlassBorder,
                shape = CircleShape
            )
            .clickable { onCheckedChange(!checked) },
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .offset(x = thumbOffset.dp)
                .size(26.dp)
                .shadow(4.dp, CircleShape, ambientColor = Color.Black, spotColor = Color.Black)
                .clip(CircleShape)
                .background(Color.White)
        )
    }
}

/**
 * Frosted Dark-Glass Song Row Item
 */
@Composable
fun SongRowItem(
    song: Song,
    isCurrent: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onFavoriteToggle: () -> Unit,
    onDownloadClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ambientColors = remember(song.id, song.mood) { FusionAmbience.getColors(song) }
    val rowAccent = ambientColors.firstOrNull() ?: AuroraCyan

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 3.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (isCurrent) FrostedGlassSurface else Color.Transparent)
            .border(
                width = if (isCurrent) 1.dp else 0.dp,
                color = if (isCurrent) rowAccent.copy(alpha = 0.45f) else Color.Transparent,
                shape = RoundedCornerShape(14.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 9.dp)
            .testTag("song_row_${song.id}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(RoundedCornerShape(10.dp))
                .border(0.8.dp, FrostedGlassBorder, RoundedCornerShape(10.dp))
                .background(MidnightElevated),
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
                    tint = rowAccent,
                    modifier = Modifier.size(24.dp)
                )
            }

            if (isCurrent) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(Color.Black.copy(alpha = 0.45f)),
                    contentAlignment = Alignment.Center
                ) {
                    AnimatedEqualizer(isPlaying = isPlaying, barColor = rowAccent)
                }
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                color = if (isCurrent) AuroraCyan else CrispWhite,
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
                        tint = IosEmerald,
                        modifier = Modifier
                            .size(13.dp)
                            .padding(end = 4.dp)
                    )
                }
                Text(
                    text = song.artist,
                    color = SlateSecondary,
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
                tint = if (song.isFavorite) AuroraMagenta else SlateSecondary,
                modifier = Modifier.size(20.dp)
            )
        }

        IconButton(
            onClick = onDownloadClick,
            modifier = Modifier
                .size(48.dp)
                .testTag("btn_download_${song.id}")
        ) {
            Icon(
                imageVector = if (song.isDownloaded) Icons.Default.CheckCircle else Icons.Default.Download,
                contentDescription = "Download Options",
                tint = if (song.isDownloaded) IosEmerald else SlateMuted,
                modifier = Modifier.size(19.dp)
            )
        }
    }
}

/**
 * Floating Luminous iOS Frosted Mini-Player Pill
 * Isolates progress tick collection inside MiniPlayerProgressRibbon so parent screens never recompose.
 * Supports horizontal swipe left/right to skip tracks seamlessly.
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
    val ambientColors = remember(song.id, song.mood) { FusionAmbience.getColors(song) }
    val accentColor = ambientColors.firstOrNull() ?: AuroraCyan
    var swipeDeltaX by remember { mutableFloatStateOf(0f) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .testTag("mini_player")
            .shadow(
                elevation = 16.dp,
                shape = RoundedCornerShape(24.dp),
                ambientColor = AuroraViolet.copy(alpha = 0.25f),
                spotColor = Color.Black.copy(alpha = 0.85f)
            )
            .pointerInput(song.id) {
                detectHorizontalDragGestures(
                    onDragStart = { swipeDeltaX = 0f },
                    onHorizontalDrag = { _, dragAmount -> swipeDeltaX += dragAmount },
                    onDragEnd = {
                        if (swipeDeltaX < -90f) {
                            onSkipNext()
                        } else if (swipeDeltaX > 90f) {
                            onSkipPrevious()
                        }
                        swipeDeltaX = 0f
                    },
                    onDragCancel = { swipeDeltaX = 0f }
                )
            }
            .bouncyClick(pressedScale = 0.985f) { onClick() },
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(
            width = 1.dp,
            brush = FusionAmbience.GoldRimBorderBrush
        ),
        colors = CardDefaults.cardColors(containerColor = FrostedGlassSurface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(FusionAmbience.miniPlayerBrush(ambientColors))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .border(0.8.dp, FrostedGlassBorder, RoundedCornerShape(10.dp))
                        .background(MidnightElevated),
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
                            tint = accentColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(Color.Black.copy(alpha = if (isPlaying) 0.42f else 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        AnimatedEqualizer(isPlaying = isPlaying, barColor = AuroraCyan)
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = song.title,
                        color = CrispWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(1.dp))
                    Text(
                        text = song.artist,
                        color = SlateSecondary,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(
                    onClick = onSkipPrevious,
                    modifier = Modifier
                        .size(42.dp)
                        .testTag("btn_mini_skip_prev")
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipPrevious,
                        contentDescription = "Previous Track",
                        tint = CrispWhite,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .shadow(10.dp, CircleShape, ambientColor = AuroraCyan, spotColor = AuroraCyan)
                        .clip(CircleShape)
                        .background(FusionAmbience.GoldMetallicBrush)
                        .border(1.dp, GoldBright.copy(alpha = 0.7f), CircleShape)
                        .bouncyClick(pressedScale = 0.90f) { onPlayPause() }
                        .testTag("btn_mini_play_pause"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = MidnightCanvas,
                        modifier = Modifier.size(22.dp)
                    )
                }

                IconButton(
                    onClick = onSkipNext,
                    modifier = Modifier
                        .size(42.dp)
                        .testTag("btn_mini_skip_next")
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Next Track",
                        tint = CrispWhite,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            MiniPlayerProgressRibbon(
                positionFlow = positionFlow,
                durationFlow = durationFlow,
                fallbackDurationMs = song.durationMs
            )
            Spacer(modifier = Modifier.height(2.dp))
        }
    }
}

@Composable
private fun MiniPlayerProgressRibbon(
    positionFlow: StateFlow<Long>?,
    durationFlow: StateFlow<Long>?,
    fallbackDurationMs: Long
) {
    val currentPositionMs by positionFlow?.collectAsState() ?: remember { mutableStateOf(0L) }
    val durationMs by durationFlow?.collectAsState() ?: remember { mutableStateOf(fallbackDurationMs) }
    val effectiveDuration = if (durationMs > 0L) durationMs else fallbackDurationMs.coerceAtLeast(1000L)
    val targetProgress = (currentPositionMs.toFloat() / effectiveDuration.toFloat()).coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = tween(durationMillis = 220),
        label = "mini_pill_progress"
    )
    val gradientBrush = remember {
        Brush.horizontalGradient(listOf(AuroraCyan, AuroraViolet, AuroraMagenta))
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(2.5.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(Color(0x1FFFFFFF))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(animatedProgress)
                .height(2.5.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(gradientBrush)
        )
    }
}

/**
 * Legacy helper preserved for secondary sheets and cards
 */
@Composable
fun SpotifyQuickPickCard(
    title: String,
    coverUrl: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isLikedCard: Boolean = false
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(58.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, FrostedGlassBorder),
        colors = CardDefaults.cardColors(containerColor = FrostedGlassSurface)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(58.dp)
                    .background(
                        if (isLikedCard) Brush.linearGradient(listOf(AuroraViolet, AuroraMagenta))
                        else Brush.linearGradient(listOf(MidnightElevated, FrostedGlassSurface))
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isLikedCard) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = "Liked Songs",
                        tint = Color.White,
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
                        tint = SlateMuted
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Text(
                text = title,
                color = CrispWhite,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
            )
        }
    }
}

/**
 * Frosted Media Card
 */
@Composable
fun SpotifyMediaCard(
    title: String,
    subtitle: String,
    coverUrl: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isCircular: Boolean = false
) {
    val shape = if (isCircular) CircleShape else RoundedCornerShape(16.dp)
    Column(
        modifier = modifier
            .width(148.dp)
            .bouncyClick { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(148.dp)
                .clip(shape)
                .border(1.dp, FrostedGlassBorder, shape)
                .background(FrostedGlassSurface),
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
                    tint = SlateMuted,
                    modifier = Modifier.size(44.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = title,
            color = CrispWhite,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = subtitle,
            color = SlateSecondary,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * iOS Bento Category Tile with tilted 3D mini album thumbnail tucked into the top-right corner.
 */
@Composable
fun SpotifyBrowseTile(
    title: String,
    colorHex: Long,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    secondaryColorHex: Long? = null,
    artworkUrl: String = ""
) {
    val startColor = Color(colorHex)
    val endColor = if (secondaryColorHex != null) Color(secondaryColorHex) else startColor.copy(alpha = 0.45f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(106.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(startColor, endColor, Color(0xFF141626))
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(listOf(Color(0x45FFFFFF), FrostedGlassBorder)),
                shape = RoundedCornerShape(20.dp)
            )
            .bouncyClick { onClick() }
            .padding(14.dp)
    ) {
        // Tilted 3D Mini Album Thumbnail tucked into the top-right corner (no offscreen shadow clipping during scroll)
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 8.dp, y = (-4).dp)
                .size(56.dp)
                .rotate(16f)
                .clip(RoundedCornerShape(10.dp))
                .border(1.2.dp, Color(0x66FFFFFF), RoundedCornerShape(10.dp))
                .background(FrostedGlassSurface),
            contentAlignment = Alignment.Center
        ) {
            if (artworkUrl.isNotBlank()) {
                AsyncImage(
                    model = artworkUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Text(
            text = title,
            color = Color.White,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 18.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(end = 52.dp)
        )
    }
}

fun formatDuration(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%d:%02d", minutes, seconds)
}

fun formatRemainingDuration(currentMs: Long, durationMs: Long): String {
    val remainingSec = ((durationMs - currentMs).coerceAtLeast(0L) / 1000L)
    val minutes = remainingSec / 60
    val seconds = remainingSec % 60
    return String.format("-%d:%02d", minutes, seconds)
}
