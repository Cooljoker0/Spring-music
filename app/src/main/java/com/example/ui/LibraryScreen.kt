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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.SpringApplication
import com.example.data.model.Playlist
import com.example.data.model.Song
import com.example.data.network.PresetCatalog
import com.example.ui.components.SongRowItem
import com.example.ui.theme.SpotifyBlack
import com.example.ui.theme.SpotifyCard
import com.example.ui.theme.SpotifyCardElevated
import com.example.ui.theme.SpotifyGreen
import com.example.ui.theme.SpotifyTextMuted
import com.example.ui.theme.SpotifyTextPrimary
import com.example.ui.theme.SpotifyTextSecondary

import com.example.auth.AuthManager
import androidx.compose.material3.MaterialTheme

@Composable
fun LibraryScreen(
    currentSong: Song?,
    isPlaying: Boolean,
    onSongSelected: (Song, List<Song>) -> Unit,
    onFavoriteToggle: (Song) -> Unit,
    onDownloadClick: (Song) -> Unit,
    onPlaylistClick: (Playlist) -> Unit,
    onOpenImport: () -> Unit,
    onOpenSettings: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val app = context.applicationContext as SpringApplication
    val repository = app.repository
    val primaryColor = MaterialTheme.colorScheme.primary

    val currentUser by AuthManager.currentUser.collectAsState()
    val favorites by repository.getFavorites().collectAsState(initial = emptyList())
    val downloads by repository.getDownloads().collectAsState(initial = emptyList())
    val userPlaylists by repository.getUserPlaylists().collectAsState(initial = emptyList())

    val scope = rememberCoroutineScope()
    var playlistToDelete by remember { mutableStateOf<Playlist?>(null) }
    var selectedFilter by remember { mutableStateOf("Playlists") }
    val filters = listOf("Playlists", "Downloaded", "Podcasts", "Albums", "Artists")

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(SpotifyBlack),
        contentPadding = PaddingValues(bottom = 120.dp)
    ) {
        // Spotify Library Top Header: User avatar + "Your Library" title + Search + "+"
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(primaryColor)
                            .clickable { onOpenSettings() }
                            .testTag("btn_library_profile_settings"),
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

                    Spacer(modifier = Modifier.width(12.dp))

                    Text(
                        text = "Your Library",
                        color = SpotifyTextPrimary,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                }


                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { /* Search in library */ }) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search Library",
                            tint = SpotifyTextPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    IconButton(
                        onClick = onOpenImport,
                        modifier = Modifier.testTag("btn_library_add_playlist")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add / Import Playlist",
                            tint = SpotifyTextPrimary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        }

        // Horizontal Filter Pills (Playlists, Downloaded, Podcasts, Albums, Artists)
        item {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filters) { filter ->
                    val isSelected = selectedFilter == filter
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isSelected) SpotifyGreen else SpotifyCardElevated)
                            .clickable { selectedFilter = filter }
                            .padding(horizontal = 14.dp, vertical = 7.dp)
                            .testTag("library_filter_$filter")
                    ) {
                        Text(
                            text = filter,
                            color = if (isSelected) Color.Black else SpotifyTextPrimary,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        }

        // Sort Row ("Recents ▾" + Grid view icon)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { /* Toggle sort */ }
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Sort,
                        contentDescription = "Sort",
                        tint = SpotifyTextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Recents",
                        color = SpotifyTextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Icon(
                    imageVector = Icons.Default.GridView,
                    contentDescription = "Grid View",
                    tint = SpotifyTextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        if (selectedFilter == "Downloaded") {
            // Downloaded tracks list
            if (downloads.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No downloaded tracks",
                            color = SpotifyTextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Tap the download button on any song to listen offline",
                            color = SpotifyTextSecondary,
                            fontSize = 13.sp
                        )
                    }
                }
            } else {
                items(downloads) { song ->
                    val isCurrent = currentSong?.id == song.id
                    SongRowItem(
                        song = song,
                        isCurrent = isCurrent,
                        isPlaying = isPlaying && isCurrent,
                        onClick = { onSongSelected(song, downloads) },
                        onFavoriteToggle = { onFavoriteToggle(song) },
                        onDownloadClick = { onDownloadClick(song) }
                    )
                }
            }
        } else {
            // Pinned "Liked Songs" Item (Exact Spotify Liked Songs Row)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val likedPlaylist = Playlist(
                                id = "pl_liked_songs",
                                title = "Liked Songs",
                                subtitle = "Your favorites • ${favorites.size} songs",
                                coverUrl = favorites.firstOrNull()?.coverUrl.orEmpty(),
                                songCount = favorites.size,
                                songs = favorites
                            )
                            onPlaylistClick(likedPlaylist)
                        }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    // 64dp x 64dp Purple Gradient Square with White Heart
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFF450AF5), Color(0xFF8E8EE5))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = "Liked Songs",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Liked Songs",
                            color = SpotifyTextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.PushPin,
                                contentDescription = "Pinned",
                                tint = SpotifyGreen,
                                modifier = Modifier
                                    .size(13.dp)
                                    .padding(end = 4.dp)
                            )
                            Text(
                                text = "Playlist • ${favorites.size} songs",
                                color = SpotifyTextSecondary,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }

            // User Imported Spotify Playlists
            items(userPlaylists) { userPl ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onPlaylistClick(userPl) }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(SpotifyCard),
                        contentAlignment = Alignment.Center
                    ) {
                        if (userPl.coverUrl.isNotBlank()) {
                            AsyncImage(
                                model = userPl.coverUrl,
                                contentDescription = userPl.title,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.MusicNote,
                                contentDescription = null,
                                tint = SpotifyGreen,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = userPl.title,
                            color = SpotifyTextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = userPl.subtitle,
                            color = SpotifyTextSecondary,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    IconButton(
                        onClick = { playlistToDelete = userPl },
                        modifier = Modifier.testTag("btn_remove_playlist_${userPl.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Remove playlist",
                            tint = SpotifyTextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }


            // Import Spotify Playlist Call to Action
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(SpotifyCardElevated)
                        .clickable { onOpenImport() }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
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
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Import more playlists",
                            color = SpotifyTextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Paste any playlist link to stream",
                            color = SpotifyTextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }

    if (playlistToDelete != null) {
        val pl = playlistToDelete!!
        AlertDialog(
            onDismissRequest = { playlistToDelete = null },
            containerColor = SpotifyCardElevated,
            title = {
                Text(
                    text = "Remove Playlist?",
                    color = SpotifyTextPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to remove \"${pl.title}\" from your library?",
                    color = SpotifyTextSecondary,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            repository.deletePlaylist(pl.id)
                        }
                        playlistToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE91429))
                ) {
                    Text("Remove", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { playlistToDelete = null }) {
                    Text("Cancel", color = SpotifyTextSecondary)
                }
            }
        )
    }
}
