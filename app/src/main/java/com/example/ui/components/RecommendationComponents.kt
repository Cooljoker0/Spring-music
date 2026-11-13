package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.shadow
import com.example.ui.theme.FusionAmbience
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.Song
import com.example.data.recommendation.FeedbackType
import com.example.data.recommendation.RecommendationSection
import com.example.ui.theme.AuroraCyan
import com.example.ui.theme.AuroraMagenta
import com.example.ui.theme.AuroraViolet
import com.example.ui.theme.CrispWhite
import com.example.ui.theme.FrostedGlassBorder
import com.example.ui.theme.FrostedGlassSurface
import com.example.ui.theme.IosRed
import com.example.ui.theme.MidnightElevated
import com.example.ui.theme.SlateMuted
import com.example.ui.theme.SlateSecondary

@Composable
fun RecommendationCarousel(
    section: RecommendationSection,
    onSongSelected: (Song, List<Song>) -> Unit,
    onFavoriteToggle: (Song) -> Unit,
    onFeedback: (Song, FeedbackType) -> Unit,
    onRefreshSection: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    if (section.songs.isEmpty()) return

    Column(modifier = modifier.fillMaxWidth().padding(top = 22.dp)) {
        // Section Header with Spotifusion Accent Pill
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .width(3.5.dp)
                        .height(22.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(AuroraCyan, AuroraViolet, AuroraMagenta)
                            )
                        )
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = section.title,
                        color = CrispWhite,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (section.subtitle.isNotBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = section.subtitle,
                            color = SlateSecondary,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            if (section.canRefresh && onRefreshSection != null) {
                IconButton(
                    onClick = onRefreshSection,
                    modifier = Modifier.size(34.dp).testTag("btn_refresh_${section.categoryKey}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh ${section.title}",
                        tint = AuroraCyan,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Horizontal Row
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(
                items = section.songs,
                key = { "${section.categoryKey}_${it.id}" },
                contentType = { "rec_card" }
            ) { song ->
                RecommendationCard(
                    song = song,
                    onClick = { onSongSelected(song, section.songs) },
                    onFavoriteToggle = { onFavoriteToggle(song) },
                    onFeedback = { type -> onFeedback(song, type) }
                )
            }
        }
    }
}

@Composable
fun RecommendationCard(
    song: Song,
    onClick: () -> Unit,
    onFavoriteToggle: () -> Unit,
    onFeedback: (FeedbackType) -> Unit,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .width(150.dp)
            .bouncyClick { onClick() }
            .testTag("rec_card_${song.id}")
    ) {
        // Artwork Box with Frosted Border, Play Overlay & 3-dots
        Box(
            modifier = Modifier
                .size(150.dp)
                .clip(RoundedCornerShape(16.dp))
                .border(
                    width = 1.dp,
                    brush = FusionAmbience.GlassBorderBrush,
                    shape = RoundedCornerShape(16.dp)
                )
                .background(FrostedGlassSurface),
            contentAlignment = Alignment.Center
        ) {
            if (song.coverUrl.isNotBlank()) {
                AsyncImage(
                    model = song.coverUrl,
                    contentDescription = song.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = SlateMuted,
                    modifier = Modifier.size(48.dp)
                )
            }

            // Bottom-right Luminous Aurora Play button (lightweight hardware layer without scroll-lagging shadow)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(AuroraCyan, AuroraViolet)
                        )
                    )
                    .border(1.dp, Color(0x55FFFFFF), CircleShape)
                    .clickable { onClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Play",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }

            // Top-right 3-dots action menu
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Color(0x9908090F))
                        .border(0.8.dp, FrostedGlassBorder, CircleShape)
                        .clickable { showMenu = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Song options",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }

                if (showMenu) {
                    DropdownMenu(
                        expanded = true,
                        onDismissRequest = { showMenu = false },
                        modifier = Modifier.background(MidnightElevated)
                    ) {
                        DropdownMenuItem(
                            text = { Text(if (song.isFavorite) "Liked" else "Like", color = CrispWhite) },
                            leadingIcon = {
                                Icon(
                                    imageVector = if (song.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    contentDescription = null,
                                    tint = if (song.isFavorite) AuroraMagenta else SlateSecondary
                                )
                            },
                            onClick = {
                                showMenu = false
                                onFavoriteToggle()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Play More Like This", color = CrispWhite) },
                            leadingIcon = {
                                Icon(Icons.Default.Whatshot, contentDescription = null, tint = AuroraCyan)
                            },
                            onClick = {
                                showMenu = false
                                onFeedback(FeedbackType.PLAY_MORE_LIKE_THIS)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Not Interested", color = CrispWhite) },
                            leadingIcon = {
                                Icon(Icons.Default.VisibilityOff, contentDescription = null, tint = SlateSecondary)
                            },
                            onClick = {
                                showMenu = false
                                onFeedback(FeedbackType.NOT_INTERESTED)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Don't Recommend Artist", color = IosRed) },
                            leadingIcon = {
                                Icon(Icons.Default.Block, contentDescription = null, tint = Color(0xFFFF453A))
                            },
                            onClick = {
                                showMenu = false
                                onFeedback(FeedbackType.DONT_RECOMMEND_ARTIST)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Dislike Track", color = Color(0xFFFF453A)) },
                            leadingIcon = {
                                Icon(Icons.Default.ThumbDown, contentDescription = null, tint = Color(0xFFFF453A))
                            },
                            onClick = {
                                showMenu = false
                                onFeedback(FeedbackType.DISLIKE)
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = song.title,
            color = CrispWhite,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = song.artist,
            color = SlateSecondary,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun RecommendationLoadingSkeleton(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Column(modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp)) {
        Box(
            modifier = Modifier
                .width(180.dp)
                .height(18.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color.White.copy(alpha = alpha))
        )
        Spacer(modifier = Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .width(240.dp)
                .height(12.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color.White.copy(alpha = alpha * 0.7f))
        )
        Spacer(modifier = Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            repeat(3) {
                Column(modifier = Modifier.width(148.dp)) {
                    Box(
                        modifier = Modifier
                            .size(148.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White.copy(alpha = alpha))
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .height(12.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.White.copy(alpha = alpha))
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.55f)
                            .height(10.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.White.copy(alpha = alpha * 0.7f))
                    )
                }
            }
        }
    }
}

@Composable
fun SimilarSongsSection(
    currentSong: Song,
    similarSongs: List<Song>,
    onSongSelected: (Song, List<Song>) -> Unit,
    onFavoriteToggle: (Song) -> Unit,
    onFeedback: (Song, FeedbackType) -> Unit,
    modifier: Modifier = Modifier
) {
    if (similarSongs.isEmpty()) return

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(FrostedGlassSurface)
            .border(1.dp, FrostedGlassBorder, RoundedCornerShape(18.dp))
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Whatshot,
                    contentDescription = null,
                    tint = AuroraCyan,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "You May Also Like",
                    color = AuroraCyan,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp
                )
            }
            Text(
                text = "Based on \"${currentSong.title.take(18)}\"",
                color = SlateSecondary,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(similarSongs, key = { "sim_${it.id}" }, contentType = { "sim_card" }) { song ->
                RecommendationCard(
                    song = song,
                    onClick = { onSongSelected(song, similarSongs) },
                    onFavoriteToggle = { onFavoriteToggle(song) },
                    onFeedback = { type -> onFeedback(song, type) },
                    modifier = Modifier.width(136.dp)
                )
            }
        }
    }
}
