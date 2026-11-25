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
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueuePlayNext
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.example.data.recommendation.MadeForYouMix
import com.example.data.recommendation.RecommendationSection
import com.example.ui.theme.DarkGlassOpaque
import com.example.ui.theme.DestructiveRose
import com.example.ui.theme.DisplayFontFamily
import com.example.ui.theme.LocalAppCustomTheme
import com.example.ui.theme.MutedGrey
import com.example.ui.theme.Silver
import com.example.ui.theme.SoftWhite
import com.example.ui.theme.WhiteHighlight
import com.example.ui.theme.WhiteHighlightSoft

@Composable
fun MadeForYouCarousel(
    mixes: List<MadeForYouMix>,
    onOpenMix: (MadeForYouMix) -> Unit,
    onPlayMix: (MadeForYouMix) -> Unit,
    modifier: Modifier = Modifier,
    title: String = "Daily Mixes",
    subtitle: String = "Curated blends evolving with your listening"
) {
    if (mixes.isEmpty()) return

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 26.dp)
    ) {
        EditorialSectionHeader(
            title = title,
            subtitle = subtitle
        )

        Spacer(modifier = Modifier.height(14.dp))

        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(
                items = mixes,
                key = { "mix_${it.id}" },
                contentType = { "made_for_you_mix" }
            ) { mix ->
                MadeForYouMixCard(
                    mix = mix,
                    onClick = { onOpenMix(mix) },
                    onPlayClick = { onPlayMix(mix) }
                )
            }
        }
    }
}

@Composable
fun MadeForYouMixCard(
    mix: MadeForYouMix,
    onClick: () -> Unit,
    onPlayClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val theme = LocalAppCustomTheme.current

    Column(
        modifier = modifier
            .width(160.dp)
            .bouncyClick { onClick() }
            .testTag("mix_card_${mix.id}")
    ) {
        Box(
            modifier = Modifier
                .size(160.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(theme.cardBackground)
                .border(0.8.dp, WhiteHighlightSoft, RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (mix.collageUrls.size >= 4) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(1.5.dp)
                ) {
                    Row(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(1.5.dp)
                    ) {
                        AsyncImage(
                            model = mix.collageUrls[0],
                            contentDescription = null,
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                            contentScale = ContentScale.Crop
                        )
                        AsyncImage(
                            model = mix.collageUrls[1],
                            contentDescription = null,
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                            contentScale = ContentScale.Crop
                        )
                    }
                    Row(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(1.5.dp)
                    ) {
                        AsyncImage(
                            model = mix.collageUrls[2],
                            contentDescription = null,
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                            contentScale = ContentScale.Crop
                        )
                        AsyncImage(
                            model = mix.collageUrls[3],
                            contentDescription = null,
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            } else if (mix.collageUrls.isNotEmpty()) {
                AsyncImage(
                    model = mix.collageUrls.first(),
                    contentDescription = mix.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = MutedGrey,
                    modifier = Modifier.size(44.dp)
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color(0x8808090D))
                        )
                    )
            )

            LiquidGlassIconButton(
                icon = Icons.Default.PlayArrow,
                contentDescription = "Play ${mix.title}",
                onClick = onPlayClick,
                size = 36.dp,
                iconSize = 20.dp,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(4.dp)
                    .testTag("btn_play_mix_${mix.id}")
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = mix.title,
            color = SoftWhite,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = mix.subtitle,
            color = Silver,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun RecommendationCarousel(
    section: RecommendationSection,
    onSongSelected: (Song, List<Song>) -> Unit,
    onFavoriteToggle: (Song) -> Unit,
    onFeedback: (Song, FeedbackType) -> Unit,
    onRefreshSection: (() -> Unit)? = null,
    onStartRadio: ((Song) -> Unit)? = null,
    onPlayNext: ((Song) -> Unit)? = null,
    onAddToQueue: ((Song) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    if (section.songs.isEmpty()) return
    val theme = LocalAppCustomTheme.current

    Column(modifier = modifier.fillMaxWidth().padding(top = 28.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!section.seedImageUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = section.seedImageUrl,
                        contentDescription = section.title,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(theme.cardBackground)
                            .border(0.7.dp, WhiteHighlight, CircleShape),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                }

                Column {
                    if (!section.badgeText.isNullOrBlank()) {
                        Text(
                            text = section.badgeText.uppercase(),
                            color = MutedGrey,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 1.0.sp,
                            maxLines = 1
                        )
                    }
                    Text(
                        text = section.title,
                        color = SoftWhite,
                        fontFamily = DisplayFontFamily,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (section.subtitle.isNotBlank()) {
                        Spacer(modifier = Modifier.height(1.dp))
                        Text(
                            text = section.subtitle,
                            color = Silver,
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
                    modifier = Modifier.size(48.dp).testTag("btn_refresh_${section.categoryKey}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh ${section.title}",
                        tint = Silver,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
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
                    onFeedback = { type -> onFeedback(song, type) },
                    onStartRadio = onStartRadio?.let { cb -> { cb(song) } },
                    onPlayNext = onPlayNext?.let { cb -> { cb(song) } },
                    onAddToQueue = onAddToQueue?.let { cb -> { cb(song) } }
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
    onStartRadio: (() -> Unit)? = null,
    onPlayNext: (() -> Unit)? = null,
    onAddToQueue: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }
    val theme = LocalAppCustomTheme.current

    Column(
        modifier = modifier
            .width(150.dp)
            .bouncyClick { onClick() }
            .testTag("rec_card_${song.id}")
    ) {
        Box(
            modifier = Modifier
                .size(150.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(theme.cardBackground)
                .border(0.7.dp, WhiteHighlightSoft, RoundedCornerShape(14.dp)),
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
                    tint = MutedGrey,
                    modifier = Modifier.size(44.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
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

            Box {
                Box(
                    modifier = Modifier
                        .defaultMinSize(minWidth = 36.dp, minHeight = 44.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { showMenu = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Song options for ${song.title}",
                        tint = MutedGrey,
                        modifier = Modifier.size(18.dp)
                    )
                }

                if (showMenu) {
                    DropdownMenu(
                        expanded = true,
                        onDismissRequest = { showMenu = false },
                        modifier = Modifier
                            .background(DarkGlassOpaque)
                            .border(0.8.dp, WhiteHighlight, RoundedCornerShape(12.dp))
                    ) {
                        if (onPlayNext != null) {
                            DropdownMenuItem(
                                text = { Text("Play Next", color = SoftWhite, fontWeight = FontWeight.Medium) },
                                leadingIcon = {
                                    Icon(Icons.Default.QueuePlayNext, contentDescription = null, tint = SoftWhite)
                                },
                                onClick = {
                                    showMenu = false
                                    onPlayNext()
                                }
                            )
                        }
                        if (onAddToQueue != null) {
                            DropdownMenuItem(
                                text = { Text("Add to Queue", color = SoftWhite) },
                                leadingIcon = {
                                    Icon(Icons.AutoMirrored.Filled.QueueMusic, contentDescription = null, tint = Silver)
                                },
                                onClick = {
                                    showMenu = false
                                    onAddToQueue()
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
                                    showMenu = false
                                    onStartRadio()
                                }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text(if (song.isFavorite) "Liked" else "Like", color = SoftWhite) },
                            leadingIcon = {
                                Icon(
                                    imageVector = if (song.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    contentDescription = null,
                                    tint = if (song.isFavorite) SoftWhite else Silver
                                )
                            },
                            onClick = {
                                showMenu = false
                                onFavoriteToggle()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Play More Like This", color = SoftWhite) },
                            leadingIcon = {
                                Icon(Icons.Default.Whatshot, contentDescription = null, tint = Silver)
                            },
                            onClick = {
                                showMenu = false
                                onFeedback(FeedbackType.PLAY_MORE_LIKE_THIS)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Not Interested", color = Silver) },
                            leadingIcon = {
                                Icon(Icons.Default.VisibilityOff, contentDescription = null, tint = Silver)
                            },
                            onClick = {
                                showMenu = false
                                onFeedback(FeedbackType.NOT_INTERESTED)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Don't Recommend Artist", color = DestructiveRose) },
                            leadingIcon = {
                                Icon(Icons.Default.Block, contentDescription = null, tint = DestructiveRose)
                            },
                            onClick = {
                                showMenu = false
                                onFeedback(FeedbackType.DONT_RECOMMEND_ARTIST)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Dislike Track", color = DestructiveRose) },
                            leadingIcon = {
                                Icon(Icons.Default.ThumbDown, contentDescription = null, tint = DestructiveRose)
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
    }
}

@Composable
fun RecommendationLoadingSkeleton(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.06f,
        targetValue = 0.14f,
        animationSpec = infiniteRepeatable(
            animation = tween(850, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Column(modifier = modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp)) {
        Box(
            modifier = Modifier
                .width(170.dp)
                .height(18.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color.White.copy(alpha = alpha))
        )
        Spacer(modifier = Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .width(220.dp)
                .height(12.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color.White.copy(alpha = alpha * 0.8f))
        )
        Spacer(modifier = Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            repeat(3) {
                Column(modifier = Modifier.width(150.dp)) {
                    Box(
                        modifier = Modifier
                            .size(150.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.White.copy(alpha = alpha))
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.8f)
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
                            .background(Color.White.copy(alpha = alpha * 0.8f))
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
    onStartRadio: ((Song) -> Unit)? = null,
    onPlayNext: ((Song) -> Unit)? = null,
    onAddToQueue: ((Song) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    if (similarSongs.isEmpty()) return

    LiquidGlassSurface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = 8.dp
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "You may also like",
                    color = SoftWhite,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Based on ${currentSong.title.take(18)}",
                    color = Silver,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(similarSongs, key = { "sim_${it.id}" }, contentType = { "sim_card" }) { song ->
                    RecommendationCard(
                        song = song,
                        onClick = { onSongSelected(song, similarSongs) },
                        onFavoriteToggle = { onFavoriteToggle(song) },
                        onFeedback = { type -> onFeedback(song, type) },
                        onStartRadio = onStartRadio?.let { cb -> { cb(song) } },
                        onPlayNext = onPlayNext?.let { cb -> { cb(song) } },
                        onAddToQueue = onAddToQueue?.let { cb -> { cb(song) } },
                        modifier = Modifier.width(134.dp)
                    )
                }
            }
        }
    }
}
