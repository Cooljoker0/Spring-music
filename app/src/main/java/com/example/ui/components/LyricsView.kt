package com.example.ui.components

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.example.ui.theme.BodyFontFamily
import com.example.ui.theme.DarkGlass
import com.example.ui.theme.DisplayFontFamily
import com.example.ui.theme.LocalAppCustomTheme
import com.example.ui.theme.MutedGrey
import com.example.ui.theme.Silver
import com.example.ui.theme.SoftWhite
import com.example.ui.theme.WhiteHighlight
import kotlinx.coroutines.launch

@Composable
fun LyricsView(
    lyrics: SongLyrics?,
    isLoading: Boolean,
    currentPositionMs: Long,
    primaryColor: Color = SoftWhite,
    fadeColor: Color = LocalAppCustomTheme.current.canvasBackground,
    onSeekTo: (Long) -> Unit,
    onRetry: () -> Unit,
    onClose: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    val activeIndex by remember(lyrics, currentPositionMs) {
        derivedStateOf {
            if (lyrics == null || !lyrics.hasSynced) -1
            else {
                val idx = lyrics.syncedLyrics.indexOfLast { it.timeMs <= currentPositionMs }
                if (idx < 0) 0 else idx
            }
        }
    }

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
                        modifier = Modifier.size(34.dp),
                        color = SoftWhite,
                        strokeWidth = 2.5.dp
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Loading lyrics...",
                        color = Silver,
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
                        tint = MutedGrey,
                        modifier = Modifier.size(44.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No lyrics found for this track",
                        fontFamily = DisplayFontFamily,
                        color = SoftWhite,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Synced lyrics aren't available yet.",
                        color = Silver,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Surface(
                        onClick = onRetry,
                        shape = RoundedCornerShape(50),
                        color = DarkGlass,
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .border(0.8.dp, WhiteHighlight, RoundedCornerShape(50))
                            .testTag("btn_retry_lyrics")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                tint = SoftWhite,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Retry",
                                color = SoftWhite,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
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
                        tint = SoftWhite,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Instrumental",
                        fontFamily = DisplayFontFamily,
                        color = SoftWhite,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "This track has no vocals",
                        color = Silver,
                        fontSize = 13.sp
                    )
                }
            }

            lyrics.hasSynced -> {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("synced_lyrics_list"),
                    contentPadding = PaddingValues(top = 32.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    itemsIndexed(lyrics.syncedLyrics) { index, line ->
                        val isActive = index == activeIndex
                        val isPast = index < activeIndex

                        val textColor by animateColorAsState(
                            targetValue = when {
                                isActive -> SoftWhite
                                isPast -> Silver.copy(alpha = 0.72f)
                                else -> MutedGrey.copy(alpha = 0.56f)
                            },
                            animationSpec = tween(durationMillis = 240),
                            label = "lyric_color"
                        )

                        val textSize = if (isActive) 22.sp else 18.sp
                        val fontWeight = if (isActive) FontWeight.Bold else FontWeight.SemiBold

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onSeekTo(line.timeMs) }
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = line.text,
                                fontFamily = if (isActive) DisplayFontFamily else BodyFontFamily,
                                color = textColor,
                                fontSize = textSize,
                                fontWeight = fontWeight,
                                lineHeight = (textSize.value * 1.38f).sp
                            )
                        }
                    }
                }
            }

            !lyrics.plainLyrics.isNullOrBlank() -> {
                val scrollState = rememberScrollState()
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(horizontal = 16.dp, vertical = 24.dp)
                        .testTag("plain_lyrics_text")
                ) {
                    Text(
                        text = lyrics.plainLyrics,
                        fontFamily = BodyFontFamily,
                        color = SoftWhite,
                        fontSize = 17.sp,
                        lineHeight = 27.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp)
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        listOf(fadeColor.copy(alpha = 0.90f), Color.Transparent)
                    )
                )
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, fadeColor.copy(alpha = 0.90f))
                    )
                )
        )
    }
}
