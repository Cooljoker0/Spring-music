package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.SongLyrics
import com.example.ui.theme.SpotifyCard
import com.example.ui.theme.SpotifyGreen
import com.example.ui.theme.SpotifyPitchBlack
import com.example.ui.theme.SpotifyTextPrimary
import com.example.ui.theme.SpotifyTextSecondary
import kotlinx.coroutines.launch

/**
 * Spotify-Style Karaoke Synchronized Lyrics Component.
 * Supports:
 * - Live real-time lyric line highlighting with smooth color transitions
 * - Interactive tap-to-seek directly to the line timestamp
 * - Automatic smooth scrolling keeping current line centered
 * - Plain lyrics fallback
 * - Instrumental indicator
 * - Loading & error retry states
 */
@Composable
fun LyricsView(
    lyrics: SongLyrics?,
    isLoading: Boolean,
    currentPositionMs: Long,
    primaryColor: Color = SpotifyGreen,
    onSeekTo: (Long) -> Unit,
    onRetry: () -> Unit,
    onClose: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    var userIsScrolling by remember { mutableStateOf(false) }

    val activeIndex by remember(lyrics, currentPositionMs) {
        derivedStateOf {
            if (lyrics == null || !lyrics.hasSynced) -1
            else {
                val idx = lyrics.syncedLyrics.indexOfLast { it.timeMs <= currentPositionMs }
                if (idx < 0) 0 else idx
            }
        }
    }

    // Auto-scroll to current lyric when active line changes (unless user touched scroll)
    LaunchedEffect(activeIndex) {
        if (activeIndex >= 0 && lyrics?.hasSynced == true && !listState.isScrollInProgress) {
            val targetScroll = (activeIndex - 2).coerceAtLeast(0)
            coroutineScope.launch {
                listState.animateScrollToItem(targetScroll)
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("lyrics_view")
    ) {
        when {
            isLoading -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(36.dp),
                        color = primaryColor,
                        strokeWidth = 3.dp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Loading lyrics...",
                        color = SpotifyTextSecondary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            lyrics == null || !lyrics.isAvailable -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = null,
                        tint = SpotifyTextSecondary.copy(alpha = 0.5f),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Couldn't find lyrics for this song",
                        color = SpotifyTextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Lyrics might not be available or published yet.",
                        color = SpotifyTextSecondary,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Surface(
                        onClick = onRetry,
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xFF2A2A2A),
                        modifier = Modifier.testTag("btn_retry_lyrics")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                tint = primaryColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Retry",
                                color = SpotifyTextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            lyrics.isInstrumental -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = primaryColor,
                        modifier = Modifier.size(54.dp)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "This song is an instrumental",
                        color = SpotifyTextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Enjoy the melody 🎵",
                        color = SpotifyTextSecondary,
                        fontSize = 13.sp
                    )
                }
            }

            lyrics.hasSynced -> {
                // Synchronized Live Karaoke View
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("synced_lyrics_list"),
                    contentPadding = PaddingValues(top = 40.dp, bottom = 120.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    itemsIndexed(lyrics.syncedLyrics) { index, line ->
                        val isActive = index == activeIndex
                        val isPast = index < activeIndex

                        val textColor by animateColorAsState(
                            targetValue = when {
                                isActive -> Color.White
                                isPast -> Color(0xB3FFFFFF)
                                else -> Color(0x40FFFFFF)
                            },
                            animationSpec = tween(durationMillis = 250),
                            label = "lyric_color"
                        )

                        val textSize = if (isActive) 23.sp else 18.sp
                        val fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.Bold

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onSeekTo(line.timeMs) }
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = line.text,
                                color = textColor,
                                fontSize = textSize,
                                fontWeight = fontWeight,
                                lineHeight = (textSize.value * 1.35f).sp
                            )
                        }
                    }
                }
            }

            !lyrics.plainLyrics.isNullOrBlank() -> {
                // Plain Lyrics View (Static fallback)
                val scrollState = rememberScrollState()
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(horizontal = 20.dp, vertical = 24.dp)
                        .testTag("plain_lyrics_text")
                ) {
                    Text(
                        text = lyrics.plainLyrics,
                        color = SpotifyTextPrimary,
                        fontSize = 17.sp,
                        lineHeight = 26.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Top gradient mask for smooth fade
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(30.dp)
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xE6121212), Color.Transparent)
                    )
                )
        )

        // Bottom gradient mask for smooth fade
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color(0xE6121212))
                    )
                )
        )
    }
}
