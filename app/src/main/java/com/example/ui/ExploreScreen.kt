package com.example.ui

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.SpringApplication
import com.example.data.model.Song
import com.example.data.network.PresetCatalog
import com.example.ui.components.SongRowItem
import com.example.ui.components.SpotifyBrowseTile
import com.example.ui.theme.SpotifyBlack
import com.example.ui.theme.SpotifyCard
import com.example.ui.theme.SpotifyCardElevated
import com.example.ui.theme.SpotifyGreen
import com.example.ui.theme.SpotifyTextMuted
import com.example.ui.theme.SpotifyTextPrimary
import com.example.ui.theme.SpotifyTextSecondary
import kotlinx.coroutines.launch

data class SpotifyGenreTile(
    val title: String,
    val colorHex: Long,
    val query: String
)

@Composable
fun ExploreScreen(
    currentSong: Song?,
    isPlaying: Boolean,
    onSongSelected: (Song, List<Song>) -> Unit,
    onFavoriteToggle: (Song) -> Unit,
    onDownloadClick: (Song) -> Unit,
    initialQuery: String = "",
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val app = context.applicationContext as SpringApplication
    val repository = app.repository
    val scope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current

    var searchQuery by remember { mutableStateOf(initialQuery) }
    var isLoading by remember { mutableStateOf(false) }
    var searchResults by remember { mutableStateOf<List<Song>>(emptyList()) }
    var hasSearched by remember { mutableStateOf(false) }

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

    LaunchedEffect(initialQuery) {
        if (initialQuery.isNotBlank() && initialQuery != searchQuery) {
            searchQuery = initialQuery
            doSearch(initialQuery)
        }
    }

    // Official Spotify Browse All Categories
    val spotifyBrowseCategories = remember {
        listOf(
            SpotifyGenreTile("Podcasts", 0xFFE13300, "Podcast Talk Show"),
            SpotifyGenreTile("Made For You", 0xFF1E3264, "Daily Mix Playlist"),
            SpotifyGenreTile("Charts", 0xFF8D67AB, "Top 50 Hits"),
            SpotifyGenreTile("New Releases", 0xFFE8115B, "New Release Friday"),
            SpotifyGenreTile("Discover", 0xFF8C1932, "Discover Weekly Indie"),
            SpotifyGenreTile("Live Events", 0xFF7358FF, "Live Concert Acoustic"),
            SpotifyGenreTile("Pop", 0xFF509BF5, "Pop Hits 2024"),
            SpotifyGenreTile("Hip-Hop", 0xFFBA5D07, "Hip Hop Rap Beats"),
            SpotifyGenreTile("Bollywood", 0xFFD84000, "Bollywood Hits Arijit"),
            SpotifyGenreTile("Rock", 0xFFE91429, "Classic Rock Anthems"),
            SpotifyGenreTile("Dance / Electronic", 0xFF006450, "EDM Electronic Club"),
            SpotifyGenreTile("Chill", 0xFF503750, "Chillout Lounge"),
            SpotifyGenreTile("Focus", 0xFF2D46B9, "Study Lofi Focus"),
            SpotifyGenreTile("Sleep", 0xFF1E3264, "Ambient Sleep White Noise"),
            SpotifyGenreTile("Workout", 0xFF777777, "Workout Gym Motivation"),
            SpotifyGenreTile("Acoustic", 0xFFB02897, "Acoustic Morning Coffee")
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SpotifyBlack)
    ) {
        // Spotify Search Header: User avatar + "Search" title + Camera icon
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF006450)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "M",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = "Search",
                    color = SpotifyTextPrimary,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            IconButton(onClick = { /* Spotify Camera / Scan code */ }) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = "Camera",
                    tint = SpotifyTextPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // Spotify Iconic White Search Bar Pill
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
                    text = "What do you want to listen to?",
                    color = Color(0xFF535353),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = Color.Black,
                    modifier = Modifier.size(24.dp)
                )
            },
            trailingIcon = {
                if (searchQuery.isNotBlank()) {
                    IconButton(onClick = {
                        searchQuery = ""
                        searchResults = emptyList()
                        hasSearched = false
                    }) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear",
                            tint = Color.Black
                        )
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(6.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent,
                focusedTextColor = Color.Black,
                unfocusedTextColor = Color.Black
            ),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { doSearch(searchQuery) }),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .testTag("spotify_search_bar")
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 120.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = SpotifyGreen)
            }
        } else if (hasSearched) {
            // Live Search Results
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 120.dp)
            ) {
                item {
                    Text(
                        text = if (searchResults.isNotEmpty()) "Top results" else "No results found for \"$searchQuery\"",
                        color = SpotifyTextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }

                items(searchResults) { song ->
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
            // "Browse all" 2-Column Tilted Tiles (Exact Spotify Search View)
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 120.dp)
            ) {
                item {
                    Text(
                        text = "Browse all",
                        color = SpotifyTextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                }

                // Pairs for 2-column layout
                items(spotifyBrowseCategories.chunked(2)) { pair ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        for (cat in pair) {
                            SpotifyBrowseTile(
                                title = cat.title,
                                colorHex = cat.colorHex,
                                onClick = {
                                    searchQuery = cat.query
                                    doSearch(cat.query)
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}
