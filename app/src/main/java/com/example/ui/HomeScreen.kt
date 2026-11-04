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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import coil.compose.AsyncImage

import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.SpringApplication
import com.example.data.model.MoodCategory
import com.example.data.model.Playlist
import com.example.data.model.Song
import com.example.data.network.PresetCatalog
import com.example.ui.components.SongRowItem
import com.example.ui.components.SpotifyMediaCard
import com.example.ui.components.SpotifyQuickPickCard
import com.example.ui.theme.SpotifyBlack
import com.example.ui.theme.SpotifyCard
import com.example.ui.theme.SpotifyCardElevated
import com.example.ui.theme.SpotifyGreen
import com.example.ui.theme.SpotifyPitchBlack
import com.example.ui.theme.SpotifyTextMuted
import com.example.ui.theme.SpotifyTextPrimary
import com.example.ui.theme.SpotifyTextSecondary
import java.util.Calendar

import androidx.compose.foundation.border
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.MaterialTheme
import com.example.auth.AuthManager

@Composable
fun HomeScreen(
    currentSong: Song?,
    isPlaying: Boolean,
    onSongSelected: (Song, List<Song>) -> Unit,
    onFavoriteToggle: (Song) -> Unit,
    onDownloadClick: (Song) -> Unit,
    onMoodClick: (MoodCategory) -> Unit,
    onPlaylistClick: (Playlist) -> Unit,
    onOpenImport: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenAiCustomizer: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val app = context.applicationContext as SpringApplication
    val repository = app.repository
    val primaryColor = MaterialTheme.colorScheme.primary

    val currentUser by AuthManager.currentUser.collectAsState()
    val recentlyPlayed by repository.getRecentlyPlayed().collectAsState(initial = emptyList())
    val favorites by repository.getFavorites().collectAsState(initial = emptyList())
    val userPlaylists by repository.getUserPlaylists().collectAsState(initial = emptyList())

    var selectedFilter by remember { mutableStateOf("All") }

    // Dynamic greeting based on current local hour
    val greeting = remember {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        when {
            hour < 12 -> "Good morning"
            hour < 18 -> "Good afternoon"
            else -> "Good evening"
        }
    }

    val displayTracks = recentlyPlayed

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        primaryColor.copy(alpha = 0.25f),
                        SpotifyBlack,
                        SpotifyPitchBlack
                    ),
                    startY = 0f,
                    endY = 1200f
                )
            ),
        contentPadding = PaddingValues(bottom = 120.dp)
    ) {
        // Top Header: User Profile Avatar (opens settings) + Filter Pills + AI Customize
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Profile Avatar (Clicking opens Settings)
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(primaryColor)
                            .clickable { onOpenSettings() }
                            .testTag("btn_profile_avatar_settings"),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!currentUser?.photoUrl?.toString().isNullOrBlank()) {
                            AsyncImage(
                                model = currentUser?.photoUrl.toString(),
                                contentDescription = "Profile Settings",
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Text(
                                text = (currentUser?.displayName ?: currentUser?.email ?: "M").take(1).uppercase(),
                                color = Color.Black,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    // Pills
                    listOf("All", "Music", "Podcasts").forEach { filter ->
                        val isSelected = selectedFilter == filter
                        Box(
                            modifier = Modifier
                                .padding(end = 6.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (isSelected) primaryColor else SpotifyCardElevated)
                                .clickable { selectedFilter = filter }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = filter,
                                color = if (isSelected) Color.Black else SpotifyTextPrimary,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }

                    // Sparkling AI Customize Pill
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(primaryColor.copy(alpha = 0.18f))
                            .border(1.dp, primaryColor.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
                            .clickable { onOpenAiCustomizer() }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                            .testTag("btn_home_ai_customize"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = primaryColor,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "AI Theme",
                                color = primaryColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Greeting Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = greeting,
                    color = SpotifyTextPrimary,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // 6-Grid Quick Picks (2 Columns x 3 Rows, dynamic based on user content)
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Row 1: Liked Songs & First Playlist / Import
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SpotifyQuickPickCard(
                        title = "Liked Songs",
                        coverUrl = favorites.firstOrNull()?.coverUrl.orEmpty(),
                        isLikedCard = true,
                        onClick = {
                            val likedPlaylist = Playlist(
                                id = "pl_liked_songs",
                                title = "Liked Songs",
                                subtitle = "${favorites.size} songs",
                                coverUrl = favorites.firstOrNull()?.coverUrl.orEmpty(),
                                songCount = favorites.size,
                                songs = favorites
                            )
                            onPlaylistClick(likedPlaylist)
                        },
                        modifier = Modifier.weight(1f)
                    )

                    val p1 = userPlaylists.getOrNull(0)
                    SpotifyQuickPickCard(
                        title = p1?.title ?: "Import Playlist",
                        coverUrl = p1?.coverUrl.orEmpty(),
                        onClick = {
                            if (p1 != null) onPlaylistClick(p1) else onOpenImport()
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Row 2
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val p2 = userPlaylists.getOrNull(1)
                    SpotifyQuickPickCard(
                        title = p2?.title ?: "Focus & Flow",
                        coverUrl = p2?.coverUrl.orEmpty(),
                        onClick = {
                            if (p2 != null) onPlaylistClick(p2) else onMoodClick(PresetCatalog.MOOD_CATEGORIES[0])
                        },
                        modifier = Modifier.weight(1f)
                    )

                    val p3 = userPlaylists.getOrNull(2)
                    SpotifyQuickPickCard(
                        title = p3?.title ?: "Chill Beats",
                        coverUrl = p3?.coverUrl.orEmpty(),
                        onClick = {
                            if (p3 != null) onPlaylistClick(p3) else onMoodClick(PresetCatalog.MOOD_CATEGORIES[1])
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Row 3
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val p4 = userPlaylists.getOrNull(3)
                    SpotifyQuickPickCard(
                        title = p4?.title ?: "Feel Good",
                        coverUrl = p4?.coverUrl.orEmpty(),
                        onClick = {
                            if (p4 != null) onPlaylistClick(p4) else onMoodClick(PresetCatalog.MOOD_CATEGORIES[2])
                        },
                        modifier = Modifier.weight(1f)
                    )

                    val p5 = userPlaylists.getOrNull(4)
                    SpotifyQuickPickCard(
                        title = p5?.title ?: "Night Drive",
                        coverUrl = p5?.coverUrl.orEmpty(),
                        onClick = {
                            if (p5 != null) onPlaylistClick(p5) else onMoodClick(PresetCatalog.MOOD_CATEGORIES.last())
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Spotify Import Spotlight Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onOpenImport() },
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2824)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(SpotifyGreen),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Import",
                                tint = Color.Black,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                text = "Import Spring Playlist",
                                color = SpotifyTextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Paste any public link to stream and sync",
                                color = SpotifyTextSecondary,
                                fontSize = 12.sp
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(SpotifyGreen)
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "Import",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        // Section: "Made For You"
        item {
            Column(modifier = Modifier.padding(top = 16.dp)) {
                Text(
                    text = "Made For You",
                    color = SpotifyTextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )

                if (userPlaylists.isNotEmpty()) {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(userPlaylists) { userPl ->
                            SpotifyMediaCard(
                                title = userPl.title,
                                subtitle = userPl.subtitle,
                                coverUrl = userPl.coverUrl,
                                onClick = { onPlaylistClick(userPl) }
                            )
                        }
                    }
                } else {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .clickable { onOpenImport() },
                        colors = CardDefaults.cardColors(containerColor = SpotifyCardElevated),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.MusicNote,
                                contentDescription = null,
                                tint = primaryColor,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Your imported playlists appear here",
                                    color = SpotifyTextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                Text(
                                    text = "Tap to import any playlist link",
                                    color = SpotifyTextSecondary,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // Section: "Popular Artists" (Navigates to Search to query real songs by artist)
        item {
            Column(modifier = Modifier.padding(top = 22.dp)) {
                Text(
                    text = "Popular artists",
                    color = SpotifyTextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )

                val artists = listOf(
                    "Arijit Singh" to "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=500&auto=format&fit=crop&q=80",
                    "Tycho" to "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=500&auto=format&fit=crop&q=80",
                    "The Weeknd" to "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=500&auto=format&fit=crop&q=80",
                    "Coldplay" to "https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f?w=500&auto=format&fit=crop&q=80",
                    "Taylor Swift" to "https://images.unsplash.com/photo-1508700115892-45ecd05ae2ad?w=500&auto=format&fit=crop&q=80"
                )

                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(artists) { (name, img) ->
                        SpotifyMediaCard(
                            title = name,
                            subtitle = "Artist",
                            coverUrl = img,
                            isCircular = true,
                            onClick = {
                                onMoodClick(
                                    MoodCategory(
                                        id = name.lowercase().replace(" ", "_"),
                                        title = name,
                                        description = "Explore tracks by $name",
                                        searchTag = name,
                                        emoji = "🎵",
                                        gradientStart = 0xFF1DB954,
                                        gradientEnd = 0xFF191414
                                    )
                                )
                            }
                        )
                    }
                }
            }
        }

        // Section: "Jump back in" (Song rows if user has recently played songs)
        if (displayTracks.isNotEmpty()) {
            item {
                Text(
                    text = "Jump back in",
                    color = SpotifyTextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 8.dp)
                )
            }
        }

        items(displayTracks) { song ->
            val isCurrent = currentSong?.id == song.id
            SongRowItem(
                song = song,
                isCurrent = isCurrent,
                isPlaying = isPlaying && isCurrent,
                onClick = { onSongSelected(song, displayTracks) },
                onFavoriteToggle = { onFavoriteToggle(song) },
                onDownloadClick = { onDownloadClick(song) }
            )
        }
    }
}
