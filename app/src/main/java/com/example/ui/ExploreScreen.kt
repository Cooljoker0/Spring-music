package com.example.ui

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.SpringApplication
import com.example.data.model.Song
import com.example.ui.components.SongRowItem
import com.example.ui.components.SpotifyBrowseTile
import com.example.ui.components.bouncyClick
import com.example.ui.theme.AuroraCyan
import com.example.ui.theme.AuroraMagenta
import com.example.ui.theme.AuroraViolet
import com.example.ui.theme.CrispWhite
import com.example.ui.theme.FrostedGlassBorder
import com.example.ui.theme.FrostedGlassSurface
import com.example.ui.theme.FusionAmbience
import com.example.ui.theme.IosEmerald
import com.example.ui.theme.IosRed
import com.example.ui.theme.MidnightCanvas
import com.example.ui.theme.MidnightElevated
import com.example.ui.theme.SlateSecondary
import kotlinx.coroutines.launch
import java.util.Locale

data class BentoCategoryItem(
    val title: String,
    val startColorHex: Long,
    val endColorHex: Long,
    val query: String,
    val artworkUrl: String
)

@Composable
fun ExploreScreen(
    currentSong: Song?,
    isPlaying: Boolean,
    onSongSelected: (Song, List<Song>) -> Unit,
    onFavoriteToggle: (Song) -> Unit,
    onDownloadClick: (Song) -> Unit,
    initialQuery: String = "",
    onOpenSettings: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val app = context.applicationContext as SpringApplication
    val repository = app.repository
    val scope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current
    val recommendationsState by repository.recommendationsState.collectAsState()

    var searchQuery by remember { mutableStateOf(initialQuery) }
    var isLoading by remember { mutableStateOf(false) }
    var searchResults by remember { mutableStateOf<List<Song>>(emptyList()) }
    var hasSearched by remember { mutableStateOf(false) }
    val topCharts by repository.topChartsState.collectAsState()

    BackHandler(enabled = hasSearched) {
        searchQuery = ""
        searchResults = emptyList()
        hasSearched = false
    }

    fun doSearch(query: String) {
        if (query.isBlank()) {
            searchResults = emptyList()
            hasSearched = false
            return
        }
        isLoading = true
        hasSearched = true
        keyboardController?.hide()

        scope.launch {
            val songs = repository.searchSongs(query)
            searchResults = songs
            isLoading = false
        }
    }

    val voiceSearchLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spoken = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
                ?.trim()
            if (!spoken.isNullOrBlank()) {
                searchQuery = spoken
                doSearch(spoken)
            }
        }
    }

    LaunchedEffect(initialQuery) {
        if (initialQuery.isNotBlank() && initialQuery != searchQuery) {
            searchQuery = initialQuery
            doSearch(initialQuery)
        }
    }

    // Load cached Top Charts once so Explore tab switches instantaneously
    LaunchedEffect(Unit) {
        repository.loadTopChartsIfNeeded()
    }

    // Exact 2x2 Bento Grid requested: Pop, Hip-Hop, Chill, Focus
    val bentoCategories = remember {
        listOf(
            BentoCategoryItem(
                title = "Pop",
                startColorHex = 0xFF9F1239,
                endColorHex = 0xFF4C1D95,
                query = "Pop Hits 2025",
                artworkUrl = "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=300&auto=format&fit=crop&q=80"
            ),
            BentoCategoryItem(
                title = "Hip-Hop",
                startColorHex = 0xFFB45309,
                endColorHex = 0xFF881337,
                query = "Hip Hop Rap Beats",
                artworkUrl = "https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f?w=300&auto=format&fit=crop&q=80"
            ),
            BentoCategoryItem(
                title = "Chill",
                startColorHex = 0xFF1E3A8A,
                endColorHex = 0xFF312E81,
                query = "Chillout Lounge Acoustic",
                artworkUrl = "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=300&auto=format&fit=crop&q=80"
            ),
            BentoCategoryItem(
                title = "Focus",
                startColorHex = 0xFF0F766E,
                endColorHex = 0xFF1E1B4B,
                query = "Study Lofi Focus Beats",
                artworkUrl = "https://images.unsplash.com/photo-1511379938547-c1f69419868d?w=300&auto=format&fit=crop&q=80"
            )
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MidnightCanvas)
    ) {
        // Top Aurora ambient wash
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            AuroraViolet.copy(alpha = 0.22f),
                            AuroraCyan.copy(alpha = 0.12f),
                            Color.Transparent
                        ),
                        center = Offset(300f, 90f),
                        radius = 850f
                    )
                )
        )

        Column(modifier = Modifier.fillMaxSize()) {
            // Large iOS Explore Title Header
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 12.dp)
            ) {
                Text(
                    text = "Explore",
                    color = CrispWhite,
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.8).sp
                )
            }

            // Frosted Glass Search Bar with Warm-Violet Border + Mic Voice Search
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .border(
                        width = 1.2.dp,
                        brush = FusionAmbience.GoldRimBorderBrush,
                        shape = RoundedCornerShape(20.dp)
                    )
                    .background(FusionAmbience.VelvetSurfaceBrush)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = {
                        searchQuery = it
                        if (it.isBlank()) {
                            searchResults = emptyList()
                            hasSearched = false
                        }
                    },
                    placeholder = {
                        Text(
                            text = "Artists, songs, lyrics...",
                            color = SlateSecondary,
                            fontSize = 15.sp
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = AuroraCyan,
                            modifier = Modifier.size(21.dp)
                        )
                    },
                    trailingIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (searchQuery.isNotBlank()) {
                                IconButton(onClick = {
                                    searchQuery = ""
                                    searchResults = emptyList()
                                    hasSearched = false
                                }) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Clear",
                                        tint = CrispWhite
                                    )
                                }
                            }
                            IconButton(
                                onClick = {
                                    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                        putExtra(
                                            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                                            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                                        )
                                        putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                                        putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak a song, artist, or lyrics...")
                                    }
                                    try {
                                        voiceSearchLauncher.launch(intent)
                                    } catch (_: ActivityNotFoundException) {
                                        Toast.makeText(
                                            context,
                                            "Voice search not supported on this device",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                },
                                modifier = Modifier.testTag("btn_explore_voice_search")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Mic,
                                    contentDescription = "Voice Search",
                                    tint = AuroraMagenta,
                                    modifier = Modifier.size(21.dp)
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(20.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        focusedTextColor = CrispWhite,
                        unfocusedTextColor = CrispWhite,
                        cursorColor = AuroraCyan
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { doSearch(searchQuery) }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("spotify_search_bar")
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 140.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = AuroraCyan)
                }
            } else if (hasSearched) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 165.dp)
                ) {
                    item {
                        Text(
                            text = if (searchResults.isNotEmpty()) "Top Results" else "No results found for \"$searchQuery\"",
                            color = CrispWhite,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp)
                        )
                    }
                    items(searchResults, key = { "res_${it.id}" }, contentType = { "song_row" }) { song ->
                        val isCurrent = currentSong?.id == song.id
                        SongRowItem(
                            song = song,
                            isCurrent = isCurrent,
                            isPlaying = isPlaying && isCurrent,
                            onClick = { onSongSelected(song, searchResults) },
                            onFavoriteToggle = { onFavoriteToggle(song) },
                            onDownloadClick = { onDownloadClick(song) }
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 18.dp, end = 18.dp, bottom = 165.dp)
                ) {
                    // Browse Categories (2x2 Bento Grid)
                    item {
                        Text(
                            text = "Browse Categories",
                            color = CrispWhite,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.3).sp,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                    }

                    items(
                        items = bentoCategories.chunked(2),
                        key = { pair -> "bento_${pair.joinToString("_") { it.title }}" },
                        contentType = { "bento_row" }
                    ) { pair ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            for (cat in pair) {
                                SpotifyBrowseTile(
                                    title = cat.title,
                                    colorHex = cat.startColorHex,
                                    secondaryColorHex = cat.endColorHex,
                                    artworkUrl = cat.artworkUrl,
                                    onClick = {
                                        searchQuery = cat.query
                                        doSearch(cat.query)
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    // Top Charts Section
                    item {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Top Charts",
                                color = CrispWhite,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = (-0.3).sp
                            )
                            Text(
                                text = "Global Live",
                                color = AuroraCyan,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    if (topCharts.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(120.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = AuroraViolet, modifier = Modifier.size(28.dp))
                            }
                        }
                    } else {
                        itemsIndexed(
                            items = topCharts,
                            key = { idx, song -> "chart_${idx}_${song.id}" },
                            contentType = { _, _ -> "chart_row" }
                        ) { index, song ->
                            val isUp = index % 3 != 1
                            val isCurrent = currentSong?.id == song.id
                            TopChartRow(
                                rank = index + 1,
                                song = song,
                                isRankUp = isUp,
                                isCurrent = isCurrent,
                                onClick = { onSongSelected(song, topCharts) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TopChartRow(
    rank: Int,
    song: Song,
    isRankUp: Boolean,
    isCurrent: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(FusionAmbience.VelvetSurfaceBrush)
            .border(
                width = 1.dp,
                brush = if (isCurrent) FusionAmbience.GoldMetallicBrush else FusionAmbience.GoldRimBorderBrush,
                shape = RoundedCornerShape(16.dp)
            )
            .bouncyClick { onClick() }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Rank 01, 02, 03 + UP/DOWN arrow
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(34.dp)
        ) {
            Text(
                text = String.format("%02d", rank),
                color = if (rank <= 3) AuroraCyan else CrispWhite,
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Icon(
                imageVector = if (isRankUp) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                contentDescription = if (isRankUp) "Rank Up" else "Rank Down",
                tint = if (isRankUp) IosEmerald else IosRed,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Square Cover Thumbnail
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(10.dp))
                .border(0.8.dp, FrostedGlassBorder, RoundedCornerShape(10.dp))
                .background(MidnightElevated),
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
                    tint = AuroraViolet
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Track & Artist
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                color = if (isCurrent) AuroraCyan else CrispWhite,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
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

        Spacer(modifier = Modifier.width(10.dp))

        // Quick-Preview Cover Art Badge on the right
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(10.dp))
                .border(1.dp, FrostedGlassBorder, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (song.coverUrl.isNotBlank()) {
                AsyncImage(
                    model = song.coverUrl,
                    contentDescription = "Quick Play ${song.title}",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x6608090F)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Play",
                    tint = CrispWhite,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
