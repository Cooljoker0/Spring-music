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
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.SpringApplication
import com.example.data.model.Playlist
import com.example.data.model.Song
import com.example.ui.components.AnimatedEqualizer
import com.example.ui.components.EditorialSectionHeader
import com.example.ui.components.LiquidGlassPill
import com.example.ui.components.LiquidGlassSurface
import com.example.ui.components.SongRowItem
import com.example.ui.components.SpotifyBrowseTile
import com.example.ui.components.SpotifyMediaCard
import com.example.ui.components.bouncyClick
import com.example.ui.components.formatDuration
import com.example.ui.theme.CustomAppTheme
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
import kotlinx.coroutines.launch
import java.util.Locale

data class EditorialDiscoverCard(
    val title: String,
    val subtitle: String,
    val query: String,
    val artworkUrl: String
)

private data class ExploreAlbumItem(
    val id: String,
    val title: String,
    val artist: String,
    val coverUrl: String,
    val songs: List<Song>
) {
    fun toPlaylist(): Playlist = Playlist(
        id = "exp_album_$id",
        title = title,
        subtitle = "$artist • ${songs.size} tracks",
        coverUrl = coverUrl,
        songCount = songs.size,
        songs = songs
    )
}

@Composable
fun ExploreScreen(
    currentSong: Song?,
    isPlaying: Boolean,
    onSongSelected: (Song, List<Song>) -> Unit,
    onFavoriteToggle: (Song) -> Unit,
    onDownloadClick: (Song) -> Unit,
    initialQuery: String = "",
    searchTrigger: Int = 0,
    onOpenSettings: () -> Unit = {},
    onPlaylistClick: ((Playlist) -> Unit)? = null,
    onPlayNext: ((Song) -> Unit)? = null,
    onAddToQueue: ((Song) -> Unit)? = null,
    onAddToPlaylist: ((Song) -> Unit)? = null,
    onStartRadio: ((Song) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val app = context.applicationContext as SpringApplication
    val repository = app.repository
    val scope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current
    val theme = LocalAppCustomTheme.current

    var searchQuery by remember { mutableStateOf(initialQuery) }
    var isLoading by remember { mutableStateOf(false) }
    var searchResults by remember { mutableStateOf<List<Song>>(emptyList()) }
    var hasSearched by remember { mutableStateOf(false) }

    val topCharts by repository.topChartsState.collectAsState()
    val newReleases by repository.newReleasesState.collectAsState()
    val recommendationsState by repository.recommendationsState.collectAsState()

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

    LaunchedEffect(initialQuery, searchTrigger) {
        if (initialQuery.isNotBlank() && (initialQuery != searchQuery || !hasSearched)) {
            searchQuery = initialQuery
            doSearch(initialQuery)
        }
    }

    LaunchedEffect(Unit) {
        repository.loadTopChartsIfNeeded()
        repository.loadNewReleasesIfNeeded()
    }

    val trendingSearches = remember {
        listOf(
            "Arijit Singh" to "Arijit Singh Hits",
            "Bollywood 2025" to "Bollywood Hits 2025",
            "Punjabi Top 20" to "Punjabi Hit Songs",
            "The Weeknd" to "The Weeknd",
            "Late Night Chill" to "Late Night Chill Vibes",
            "Romantic Hindi" to "Romantic Hindi Songs",
            "Gym Phonk & Workout" to "Workout High Energy"
        )
    }

    val popularArtists = remember(topCharts, recommendationsState.sections) {
        val fallbackArtists = listOf(
            "Arijit Singh" to "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=400&auto=format&fit=crop&q=80",
            "Diljit Dosanjh" to "https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f?w=400&auto=format&fit=crop&q=80",
            "Shreya Ghoshal" to "https://images.unsplash.com/photo-1508700115892-45ecd05ae2ad?w=400&auto=format&fit=crop&q=80",
            "The Weeknd" to "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=400&auto=format&fit=crop&q=80",
            "AP Dhillon" to "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=400&auto=format&fit=crop&q=80",
            "Anirudh Ravichander" to "https://images.unsplash.com/photo-1511379938547-c1f69419868d?w=400&auto=format&fit=crop&q=80"
        )
        val live = LinkedHashMap<String, String>()
        (topCharts + recommendationsState.sections.flatMap { it.songs }).forEach { s ->
            val name = s.artist.split(",").firstOrNull()?.trim().orEmpty()
            if (name.length > 2 && s.coverUrl.isNotBlank()) {
                live.putIfAbsent(name, s.coverUrl)
            }
        }
        val list = live.entries.map { it.key to it.value }.toMutableList()
        fallbackArtists.forEach { (name, img) ->
            if (list.none { it.first.equals(name, true) }) {
                list.add(name to img)
            }
        }
        list.take(10)
    }

    val popularAlbums = remember(topCharts, newReleases, recommendationsState.sections) {
        (topCharts + newReleases + recommendationsState.sections.flatMap { it.songs })
            .distinctBy { it.id }
            .filter { it.album.isNotBlank() && !it.album.equals("Single", true) }
            .groupBy { it.album.trim() }
            .entries
            .map { (albumName, songs) ->
                val lead = songs.first()
                ExploreAlbumItem(
                    id = albumName.lowercase().replace(Regex("[^a-z0-9]+"), "_"),
                    title = albumName,
                    artist = lead.artist,
                    coverUrl = lead.coverUrl,
                    songs = songs
                )
            }
            .take(10)
    }

    val languageScenes = remember {
        listOf(
            EditorialDiscoverCard(
                title = "Hindi Music",
                subtitle = "Timeless & modern vocals",
                query = "Hindi Top Hits",
                artworkUrl = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=450&auto=format&fit=crop&q=80"
            ),
            EditorialDiscoverCard(
                title = "Bollywood Hits",
                subtitle = "Soundtracks & chart toppers",
                query = "Bollywood Chartbusters Hits",
                artworkUrl = "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=450&auto=format&fit=crop&q=80"
            ),
            EditorialDiscoverCard(
                title = "Punjabi Music",
                subtitle = "Bhangra, hip-hop & urban beats",
                query = "Punjabi Trending Hits",
                artworkUrl = "https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f?w=450&auto=format&fit=crop&q=80"
            ),
            EditorialDiscoverCard(
                title = "English Music",
                subtitle = "Global pop & R&B",
                query = "English Pop Global Hits",
                artworkUrl = "https://images.unsplash.com/photo-1508700115892-45ecd05ae2ad?w=450&auto=format&fit=crop&q=80"
            ),
            EditorialDiscoverCard(
                title = "Regional Music",
                subtitle = "Tamil, Telugu, Marathi & Indie",
                query = "Tamil Telugu Regional Hits",
                artworkUrl = "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=450&auto=format&fit=crop&q=80"
            )
        )
    }

    val moodEditorialCards = remember {
        listOf(
            EditorialDiscoverCard(
                title = "Romantic Songs",
                subtitle = "Warm acoustic & love ballads",
                query = "Romantic Love Songs",
                artworkUrl = "https://images.unsplash.com/photo-1518609878373-06d740f60d8b?w=450&auto=format&fit=crop&q=80"
            ),
            EditorialDiscoverCard(
                title = "Sad Songs",
                subtitle = "Emotional & soulful melodies",
                query = "Sad Soulful Melodies",
                artworkUrl = "https://images.unsplash.com/photo-1507838153414-b4b713384a76?w=450&auto=format&fit=crop&q=80"
            ),
            EditorialDiscoverCard(
                title = "Chill Music",
                subtitle = "Late night lofi & ambient",
                query = "Chill Lounge Lofi Beats",
                artworkUrl = "https://images.unsplash.com/photo-1511379938547-c1f69419868d?w=450&auto=format&fit=crop&q=80"
            ),
            EditorialDiscoverCard(
                title = "Workout Music",
                subtitle = "High-tempo gym & running fuel",
                query = "Gym Workout High Energy",
                artworkUrl = "https://images.unsplash.com/photo-1571019613454-1cb2f99b2d8b?w=450&auto=format&fit=crop&q=80"
            )
        )
    }

    val moodGridCategories = remember {
        listOf(
            EditorialDiscoverCard("Focus", "Deep work & study", "Study Lofi Focus Beats", "https://images.unsplash.com/photo-1511379938547-c1f69419868d?w=400&auto=format&fit=crop&q=80"),
            EditorialDiscoverCard("Late Night", "Midnight drive & atmospheric", "Late Night Drive Synth", "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=400&auto=format&fit=crop&q=80"),
            EditorialDiscoverCard("Feel Good", "Upbeat & uplifting", "Feel Good Happy Hits", "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=400&auto=format&fit=crop&q=80"),
            EditorialDiscoverCard("Acoustic", "Unplugged & stripped back", "Acoustic Unplugged Sessions", "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=400&auto=format&fit=crop&q=80")
        )
    }

    val genreGridCategories = remember {
        listOf(
            EditorialDiscoverCard("Pop", "Charting pop anthems", "Pop Hits 2025", "https://images.unsplash.com/photo-1508700115892-45ecd05ae2ad?w=400&auto=format&fit=crop&q=80"),
            EditorialDiscoverCard("Hip-Hop", "Rap, trap & urban flow", "Hip Hop Rap Beats", "https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f?w=400&auto=format&fit=crop&q=80"),
            EditorialDiscoverCard("Electronic", "House, techno & dance", "Electronic Dance House", "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=400&auto=format&fit=crop&q=80"),
            EditorialDiscoverCard("Indie", "Alternative & modern indie", "Indie Alternative Hits", "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=400&auto=format&fit=crop&q=80"),
            EditorialDiscoverCard("R&B & Soul", "Velvet vocals & grooves", "RnB Soul Essentials", "https://images.unsplash.com/photo-1518609878373-06d740f60d8b?w=400&auto=format&fit=crop&q=80"),
            EditorialDiscoverCard("Rock & Sufi", "Guitars & classic Sufi rock", "Sufi Rock Classics", "https://images.unsplash.com/photo-1507838153414-b4b713384a76?w=400&auto=format&fit=crop&q=80")
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(theme.canvasBackground),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 880.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 10.dp)
            ) {
                Text(
                    text = "Explore",
                    color = SoftWhite,
                    fontFamily = DisplayFontFamily,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Scenes, regional spotlights, moods, and live charts",
                    color = Silver,
                    fontSize = 13.sp
                )
            }

            // Prominent Floating Liquid Glass Search Bar
            LiquidGlassSurface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                shape = RoundedCornerShape(18.dp),
                elevation = 14.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 6.dp, top = 4.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = Silver,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 44.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (searchQuery.isEmpty()) {
                            Text(
                                text = "Search artists, songs, moods, or albums",
                                color = MutedGrey,
                                fontSize = 14.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = {
                                searchQuery = it
                                if (it.isBlank()) {
                                    searchResults = emptyList()
                                    hasSearched = false
                                }
                            },
                            singleLine = true,
                            textStyle = TextStyle(
                                color = SoftWhite,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            cursorBrush = SolidColor(SoftWhite),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = { doSearch(searchQuery) }),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("spotify_search_bar")
                        )
                    }

                    if (searchQuery.isNotBlank()) {
                        IconButton(
                            onClick = {
                                searchQuery = ""
                                searchResults = emptyList()
                                hasSearched = false
                            },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear search",
                                tint = Silver,
                                modifier = Modifier.size(19.dp)
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
                                putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak a song, artist, or mood...")
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
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("btn_explore_voice_search")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Voice Search",
                            tint = SoftWhite,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 160.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = SoftWhite, strokeWidth = 2.5.dp)
                }
            } else if (hasSearched) {
                if (searchResults.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 24.dp, vertical = 48.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = MutedGrey,
                            modifier = Modifier.size(44.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No results for \"$searchQuery\"",
                            color = SoftWhite,
                            fontFamily = DisplayFontFamily,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Check the spelling or explore curated scenes below.",
                            color = Silver,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                searchQuery = ""
                                searchResults = emptyList()
                                hasSearched = false
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SoftWhite,
                                contentColor = MidnightBlack
                            ),
                            shape = RoundedCornerShape(50),
                            modifier = Modifier.height(48.dp)
                        ) {
                            Text("Back to Explore", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 186.dp)
                    ) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 20.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Results (${searchResults.size})",
                                    color = SoftWhite,
                                    fontFamily = DisplayFontFamily,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    TextButton(
                                        onClick = { onSongSelected(searchResults.first(), searchResults) },
                                        modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PlayArrow,
                                            contentDescription = null,
                                            tint = SoftWhite,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Play All",
                                            color = SoftWhite,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                    TextButton(
                                        onClick = {
                                            searchQuery = ""
                                            searchResults = emptyList()
                                            hasSearched = false
                                        },
                                        modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                                    ) {
                                        Text(
                                            text = "Clear",
                                            color = Silver,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }

                        itemsIndexed(
                            items = searchResults,
                            key = { _, song -> "res_${song.id}" },
                            contentType = { _, _ -> "song_row" }
                        ) { index, song ->
                            val isCurrent = currentSong?.id == song.id
                            Column(modifier = Modifier.fillMaxWidth()) {
                                SongRowItem(
                                    song = song,
                                    isCurrent = isCurrent,
                                    isPlaying = isPlaying && isCurrent,
                                    onClick = { onSongSelected(song, searchResults) },
                                    onFavoriteToggle = { onFavoriteToggle(song) },
                                    onDownloadClick = { onDownloadClick(song) },
                                    onPlayNext = onPlayNext?.let { cb -> { cb(song) } },
                                    onAddToQueue = onAddToQueue?.let { cb -> { cb(song) } },
                                    onAddToPlaylist = onAddToPlaylist?.let { cb -> { cb(song) } },
                                    onStartRadio = onStartRadio?.let { cb -> { cb(song) } }
                                )
                                if (index < searchResults.lastIndex) {
                                    HorizontalDivider(
                                        color = WhiteHighlightSoft,
                                        thickness = 0.5.dp,
                                        modifier = Modifier.padding(start = 78.dp, end = 16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 186.dp)
                ) {
                    // 1. TRENDING SEARCHES
                    item(key = "trending_searches") {
                        Column(modifier = Modifier.padding(top = 6.dp)) {
                            EditorialSectionHeader(
                                title = "Trending Searches",
                                subtitle = "What listeners are searching right now"
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 20.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(trendingSearches, key = { it.first }) { (label, query) ->
                                    LiquidGlassPill(
                                        onClick = {
                                            searchQuery = query
                                            doSearch(query)
                                        }
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.TrendingUp,
                                                contentDescription = null,
                                                tint = Silver,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = label,
                                                color = SoftWhite,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 2. LANGUAGE & REGIONAL SPOTLIGHTS (Hindi, Bollywood, Punjabi, English, Regional)
                    item(key = "language_scenes") {
                        Column(modifier = Modifier.padding(top = 24.dp)) {
                            EditorialSectionHeader(
                                title = "Music by Language & Region",
                                subtitle = "Hindi, Bollywood, Punjabi, English & Regional"
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 20.dp),
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                items(languageScenes, key = { it.title }) { card ->
                                    EditorialArtworkSpotlightCard(
                                        card = card,
                                        onClick = {
                                            searchQuery = card.query
                                            doSearch(card.query)
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // 3. POPULAR ARTISTS
                    item(key = "explore_popular_artists") {
                        Column(modifier = Modifier.padding(top = 26.dp)) {
                            EditorialSectionHeader(
                                title = "Popular Artists",
                                subtitle = "Tap any artist to explore their catalog"
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 20.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                items(popularArtists, key = { it.first }) { (name, artwork) ->
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier
                                            .width(80.dp)
                                            .bouncyClick {
                                                searchQuery = name
                                                doSearch(name)
                                            }
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(74.dp)
                                                .clip(CircleShape)
                                                .background(Graphite)
                                                .border(0.85.dp, WhiteHighlight, CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (artwork.isNotBlank()) {
                                                AsyncImage(
                                                    model = artwork,
                                                    contentDescription = name,
                                                    modifier = Modifier.fillMaxSize(),
                                                    contentScale = ContentScale.Crop
                                                )
                                            } else {
                                                Text(
                                                    text = name.take(1).uppercase(),
                                                    color = SoftWhite,
                                                    fontFamily = DisplayFontFamily,
                                                    fontSize = 22.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(7.dp))
                                        Text(
                                            text = name,
                                            color = SoftWhite,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 4. EMOTIONAL & ACTIVITY SPOTLIGHTS (Romantic, Sad, Chill, Workout)
                    item(key = "mood_spotlight_cards") {
                        Column(modifier = Modifier.padding(top = 26.dp)) {
                            EditorialSectionHeader(
                                title = "Vibes & Soundtracks",
                                subtitle = "Romantic, Sad, Chill, and Workout"
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 20.dp),
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                items(moodEditorialCards, key = { it.title }) { card ->
                                    EditorialArtworkSpotlightCard(
                                        card = card,
                                        widthDp = 196,
                                        onClick = {
                                            searchQuery = card.query
                                            doSearch(card.query)
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // 5. POPULAR ALBUMS
                    if (popularAlbums.isNotEmpty()) {
                        item(key = "explore_popular_albums") {
                            Column(modifier = Modifier.padding(top = 26.dp)) {
                                EditorialSectionHeader(
                                    title = "Popular Albums",
                                    subtitle = "Albums featured across live charts"
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 20.dp),
                                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    items(popularAlbums, key = { it.id }) { album ->
                                        SpotifyMediaCard(
                                            title = album.title,
                                            subtitle = album.artist,
                                            coverUrl = album.coverUrl,
                                            onClick = {
                                                if (onPlaylistClick != null) {
                                                    onPlaylistClick(album.toPlaylist())
                                                } else if (album.songs.isNotEmpty()) {
                                                    onSongSelected(album.songs.first(), album.songs)
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 6. MUSIC BY MOOD (Monochrome Artwork Tiles)
                    item(key = "music_by_mood_header") {
                        Spacer(modifier = Modifier.height(26.dp))
                        EditorialSectionHeader(
                            title = "Music by Mood",
                            subtitle = "Curated tonal collections for every hour"
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    items(
                        items = moodGridCategories.chunked(2),
                        key = { pair -> "mood_row_${pair.joinToString("_") { it.title }}" }
                    ) { pair ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp)
                                .padding(bottom = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            pair.forEach { cat ->
                                SpotifyBrowseTile(
                                    title = cat.title,
                                    subtitle = cat.subtitle,
                                    colorHex = 0xFF171A21,
                                    secondaryColorHex = 0xFF0D0F14,
                                    artworkUrl = cat.artworkUrl,
                                    onClick = {
                                        searchQuery = cat.query
                                        doSearch(cat.query)
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            if (pair.size == 1) Spacer(modifier = Modifier.weight(1f))
                        }
                    }

                    // 7. MUSIC BY GENRE (Monochrome Artwork Tiles)
                    item(key = "music_by_genre_header") {
                        Spacer(modifier = Modifier.height(20.dp))
                        EditorialSectionHeader(
                            title = "Music by Genre",
                            subtitle = "Browse across styles and eras"
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    items(
                        items = genreGridCategories.chunked(2),
                        key = { pair -> "genre_row_${pair.joinToString("_") { it.title }}" }
                    ) { pair ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp)
                                .padding(bottom = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            pair.forEach { cat ->
                                SpotifyBrowseTile(
                                    title = cat.title,
                                    subtitle = cat.subtitle,
                                    colorHex = 0xFF171A21,
                                    secondaryColorHex = 0xFF0D0F14,
                                    artworkUrl = cat.artworkUrl,
                                    onClick = {
                                        searchQuery = cat.query
                                        doSearch(cat.query)
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            if (pair.size == 1) Spacer(modifier = Modifier.weight(1f))
                        }
                    }

                    // 8. TOP CHARTS (Live Chart Leaderboard)
                    item(key = "top_charts_header") {
                        Spacer(modifier = Modifier.height(20.dp))
                        EditorialSectionHeader(
                            title = "Top Charts",
                            subtitle = "Global & regional chart rankings",
                            actionLabel = if (topCharts.isNotEmpty()) "Play All" else null,
                            onActionClick = if (topCharts.isNotEmpty()) {
                                { onSongSelected(topCharts.first(), topCharts) }
                            } else null
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    if (topCharts.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(120.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = SoftWhite, modifier = Modifier.size(28.dp))
                            }
                        }
                    } else {
                        itemsIndexed(
                            items = topCharts,
                            key = { idx, song -> "chart_${idx}_${song.id}" },
                            contentType = { _, _ -> "chart_row" }
                        ) { index, song ->
                            val isCurrent = currentSong?.id == song.id
                            TopChartRow(
                                rank = index + 1,
                                song = song,
                                isCurrent = isCurrent,
                                isPlaying = isPlaying && isCurrent,
                                theme = theme,
                                onClick = { onSongSelected(song, topCharts) },
                                modifier = Modifier.padding(horizontal = 20.dp)
                            )
                            if (index < topCharts.lastIndex) {
                                HorizontalDivider(
                                    color = WhiteHighlightSoft,
                                    thickness = 0.5.dp,
                                    modifier = Modifier.padding(start = 62.dp, end = 20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EditorialArtworkSpotlightCard(
    card: EditorialDiscoverCard,
    onClick: () -> Unit,
    widthDp: Int = 172
) {
    LiquidGlassSurface(
        modifier = Modifier
            .width(widthDp.dp)
            .height(132.dp)
            .bouncyClick { onClick() },
        shape = RoundedCornerShape(18.dp),
        elevation = 10.dp
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = card.artworkUrl,
                contentDescription = card.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                alpha = 0.68f
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0x3308090D),
                                Color(0xD808090D)
                            )
                        )
                    )
            )
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(14.dp)
            ) {
                Text(
                    text = card.title,
                    color = SoftWhite,
                    fontFamily = DisplayFontFamily,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = card.subtitle,
                    color = Silver,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun TopChartRow(
    rank: Int,
    song: Song,
    isCurrent: Boolean,
    isPlaying: Boolean,
    theme: CustomAppTheme,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val durationLabel = remember(song.durationMs) {
        if (song.durationMs > 0L) formatDuration(song.durationMs) else ""
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isCurrent) WhiteHighlightSoft else Color.Transparent)
            .clickable { onClick() }
            .padding(horizontal = 4.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.width(32.dp)
        ) {
            Text(
                text = String.format(Locale.US, "%02d", rank),
                color = if (isCurrent || rank <= 3) SoftWhite else MutedGrey,
                fontFamily = MonoFontFamily,
                fontSize = 14.sp,
                fontWeight = if (isCurrent || rank <= 3) FontWeight.Bold else FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(GraphiteElevated)
                .border(0.6.dp, WhiteHighlightSoft, RoundedCornerShape(10.dp)),
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
                    tint = MutedGrey
                )
            }
            if (isCurrent) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
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
                fontSize = 15.sp,
                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = song.artist,
                color = Silver,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (durationLabel.isNotEmpty()) {
            Text(
                text = durationLabel,
                color = MutedGrey,
                fontFamily = MonoFontFamily,
                fontSize = 12.sp,
                modifier = Modifier.padding(end = 4.dp)
            )
        }

        IconButton(
            onClick = onClick,
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "Play ${song.title}",
                tint = if (isCurrent) SoftWhite else Silver,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}
