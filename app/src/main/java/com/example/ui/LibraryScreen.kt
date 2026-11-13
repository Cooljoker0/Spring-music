package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.example.ui.components.SongRowItem
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

enum class LibrarySubView {
    OVERVIEW, PLAYLISTS, ARTISTS, ALBUMS, DOWNLOADED
}

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
    val scope = rememberCoroutineScope()

    val favorites by repository.getFavorites().collectAsState(initial = emptyList())
    val downloads by repository.getDownloads().collectAsState(initial = emptyList())
    val recentlyPlayed by repository.getRecentlyPlayed().collectAsState(initial = emptyList())
    val userPlaylists by repository.getUserPlaylists().collectAsState(initial = emptyList())
    val recommendationsState by repository.recommendationsState.collectAsState()

    var isEditMode by remember { mutableStateOf(false) }
    var activeSubView by remember { mutableStateOf(LibrarySubView.OVERVIEW) }
    var playlistToDelete by remember { mutableStateOf<Playlist?>(null) }
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }

    BackHandler(enabled = activeSubView != LibrarySubView.OVERVIEW) {
        activeSubView = LibrarySubView.OVERVIEW
    }

    val likedPlaylist = remember(favorites) {
        Playlist(
            id = "pl_liked_songs",
            title = "Liked Songs",
            subtitle = "${favorites.size} songs",
            coverUrl = favorites.firstOrNull()?.coverUrl.orEmpty(),
            songCount = favorites.size,
            songs = favorites
        )
    }

    // All library tracks for deriving Artists and Albums dynamically
    val allKnownSongs = remember(favorites, downloads, recentlyPlayed, userPlaylists, recommendationsState.sections) {
        (favorites + downloads + recentlyPlayed + userPlaylists.flatMap { it.songs } +
            recommendationsState.sections.flatMap { it.songs })
            .distinctBy { it.id }
    }

    val artistGroups = remember(allKnownSongs) {
        allKnownSongs.groupBy { it.artist.ifBlank { "Various Artists" } }.entries
            .sortedByDescending { it.value.size }
            .map { (artist, songs) ->
                Playlist(
                    id = "artist_${artist.hashCode()}",
                    title = artist,
                    subtitle = "${songs.size} songs",
                    coverUrl = songs.firstOrNull { it.coverUrl.isNotBlank() }?.coverUrl.orEmpty(),
                    songCount = songs.size,
                    songs = songs
                )
            }
    }

    val albumGroups = remember(allKnownSongs) {
        allKnownSongs.groupBy { it.album.ifBlank { it.title } }.entries
            .sortedByDescending { it.value.size }
            .map { (album, songs) ->
                Playlist(
                    id = "album_${album.hashCode()}",
                    title = album,
                    subtitle = "${songs.firstOrNull()?.artist ?: "Spring"} • ${songs.size} songs",
                    coverUrl = songs.firstOrNull { it.coverUrl.isNotBlank() }?.coverUrl.orEmpty(),
                    songCount = songs.size,
                    songs = songs
                )
            }
    }

    // 6-Card Recently Added (3x2 Grid)
    val recentlyAddedGrid = remember(likedPlaylist, userPlaylists, albumGroups) {
        val list = mutableListOf(likedPlaylist)
        userPlaylists.forEach { if (list.size < 6) list.add(it) }
        albumGroups.forEach { album ->
            if (list.size < 6 && list.none { it.title == album.title }) {
                list.add(album)
            }
        }
        list.take(6)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MidnightCanvas)
    ) {
        // Subtle Ambient Aurora Glow
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            AuroraMagenta.copy(alpha = 0.18f),
                            AuroraViolet.copy(alpha = 0.14f),
                            Color.Transparent
                        ),
                        center = Offset(780f, 100f),
                        radius = 850f
                    )
                )
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 165.dp)
        ) {
            // 1. Header: Large "Library" Title + Frosted "Edit" Pill & Circular "+" Button
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (activeSubView != LibrarySubView.OVERVIEW) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(FrostedGlassSurface)
                                    .border(1.dp, FusionAmbience.GoldRimBorderBrush, CircleShape)
                                    .clickable { activeSubView = LibrarySubView.OVERVIEW },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back to Library",
                                    tint = CrispWhite,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                        }

                        Text(
                            text = when (activeSubView) {
                                LibrarySubView.OVERVIEW -> "Library"
                                LibrarySubView.PLAYLISTS -> "Playlists"
                                LibrarySubView.ARTISTS -> "Artists"
                                LibrarySubView.ALBUMS -> "Albums"
                                LibrarySubView.DOWNLOADED -> "Downloaded"
                            },
                            color = CrispWhite,
                            fontSize = 34.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.8).sp
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Frosted Glass "Edit" Pill
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (isEditMode) AuroraCyan.copy(alpha = 0.25f) else FrostedGlassSurface)
                                .border(
                                    width = 1.dp,
                                    color = if (isEditMode) AuroraCyan else FrostedGlassBorder,
                                    shape = RoundedCornerShape(20.dp)
                                )
                                .clickable { isEditMode = !isEditMode }
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .testTag("btn_library_edit")
                        ) {
                            Text(
                                text = if (isEditMode) "Done" else "Edit",
                                color = if (isEditMode) AuroraCyan else CrispWhite,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        // Circular "+" Create / Import Playlist Button
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(FrostedGlassSurface)
                                .border(1.dp, FusionAmbience.GoldRimBorderBrush, CircleShape)
                                .clickable { showCreatePlaylistDialog = true }
                                .testTag("btn_library_add_playlist"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Create Playlist",
                                tint = CrispWhite,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }

            if (activeSubView == LibrarySubView.OVERVIEW) {
                // 2. Category Navigation Rows (iOS Inset Grouped List)
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(FusionAmbience.VelvetSurfaceBrush)
                            .border(1.dp, FusionAmbience.GoldRimBorderBrush, RoundedCornerShape(18.dp))
                    ) {
                        LibraryCategoryRow(
                            icon = Icons.AutoMirrored.Filled.PlaylistPlay,
                            title = "Playlists",
                            countText = "${userPlaylists.size + 1}",
                            tint = AuroraCyan,
                            onClick = { activeSubView = LibrarySubView.PLAYLISTS }
                        )
                        HorizontalDivider(color = FrostedGlassBorder, modifier = Modifier.padding(start = 58.dp))
                        LibraryCategoryRow(
                            icon = Icons.Default.Person,
                            title = "Artists",
                            countText = "${artistGroups.size}",
                            tint = AuroraViolet,
                            onClick = { activeSubView = LibrarySubView.ARTISTS }
                        )
                        HorizontalDivider(color = FrostedGlassBorder, modifier = Modifier.padding(start = 58.dp))
                        LibraryCategoryRow(
                            icon = Icons.Default.Album,
                            title = "Albums",
                            countText = "${albumGroups.size}",
                            tint = AuroraMagenta,
                            onClick = { activeSubView = LibrarySubView.ALBUMS }
                        )
                        HorizontalDivider(color = FrostedGlassBorder, modifier = Modifier.padding(start = 58.dp))
                        LibraryCategoryRow(
                            icon = Icons.Default.DownloadDone,
                            title = "Downloaded",
                            countText = "${downloads.size}",
                            tint = IosEmerald,
                            onClick = { activeSubView = LibrarySubView.DOWNLOADED }
                        )
                    }
                }

                // 3. Recently Added (3x2 Grid -> 3 Columns x 2 Rows)
                item {
                    Spacer(modifier = Modifier.height(22.dp))
                    Text(
                        text = "Recently Added",
                        color = CrispWhite,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.3).sp,
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 6.dp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    val rows = recentlyAddedGrid.chunked(3)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        rows.forEach { rowItems ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                rowItems.forEach { item ->
                                    val isLiked = item.id == "pl_liked_songs"
                                    val isDeletable = isEditMode && !isLiked && item.id.startsWith("pl_")
                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .bouncyClick(pressedScale = 0.95f) {
                                                if (isDeletable) {
                                                    playlistToDelete = item
                                                } else {
                                                    onPlaylistClick(item)
                                                }
                                            }
                                            .testTag("library_grid_${item.id}")
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .aspectRatio(1f)
                                                .clip(RoundedCornerShape(14.dp))
                                                .border(1.dp, FrostedGlassBorder, RoundedCornerShape(14.dp))
                                                .background(
                                                    if (isLiked) Brush.linearGradient(listOf(AuroraMagenta, AuroraViolet))
                                                    else Brush.linearGradient(listOf(MidnightElevated, FrostedGlassSurface))
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (isLiked) {
                                                Icon(
                                                    imageVector = Icons.Default.Favorite,
                                                    contentDescription = "Liked Songs",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(34.dp)
                                                )
                                                // Green downloaded checkmark badge
                                                Icon(
                                                    imageVector = Icons.Default.CheckCircle,
                                                    contentDescription = "Synced & Offline Ready",
                                                    tint = IosEmerald,
                                                    modifier = Modifier
                                                        .align(Alignment.BottomEnd)
                                                        .padding(6.dp)
                                                        .size(18.dp)
                                                        .clip(CircleShape)
                                                        .background(MidnightCanvas)
                                                )
                                            } else if (item.coverUrl.isNotBlank()) {
                                                AsyncImage(
                                                    model = item.coverUrl,
                                                    contentDescription = item.title,
                                                    modifier = Modifier.fillMaxSize(),
                                                    contentScale = ContentScale.Crop
                                                )
                                            } else {
                                                Icon(
                                                    imageVector = Icons.Default.MusicNote,
                                                    contentDescription = null,
                                                    tint = AuroraCyan,
                                                    modifier = Modifier.size(32.dp)
                                                )
                                            }

                                            if (isDeletable) {
                                                Box(
                                                    modifier = Modifier
                                                        .align(Alignment.TopEnd)
                                                        .padding(6.dp)
                                                        .size(24.dp)
                                                        .clip(CircleShape)
                                                        .background(IosRed),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.DeleteOutline,
                                                        contentDescription = "Remove",
                                                        tint = Color.White,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(6.dp))

                                        Text(
                                            text = item.title,
                                            color = CrispWhite,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${item.songs.size.coerceAtLeast(item.songCount)} songs",
                                            color = SlateSecondary,
                                            fontSize = 11.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                repeat((3 - rowItems.size).coerceAtLeast(0)) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }

                // 4. Pinned Liked Songs Full-Width Magenta-Violet Glass Banner at Bottom
                item {
                    Spacer(modifier = Modifier.height(22.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp)
                            .shadow(
                                elevation = 18.dp,
                                shape = RoundedCornerShape(20.dp),
                                ambientColor = AuroraMagenta.copy(alpha = 0.35f),
                                spotColor = AuroraViolet.copy(alpha = 0.35f)
                            )
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        AuroraMagenta.copy(alpha = 0.35f),
                                        AuroraViolet.copy(alpha = 0.42f),
                                        FrostedGlassSurface
                                    )
                                )
                            )
                            .border(
                                width = 1.dp,
                                brush = Brush.horizontalGradient(
                                    listOf(AuroraMagenta.copy(alpha = 0.65f), FrostedGlassBorder)
                                ),
                                shape = RoundedCornerShape(20.dp)
                            )
                            .bouncyClick(pressedScale = 0.97f) { onPlaylistClick(likedPlaylist) }
                            .padding(16.dp)
                            .testTag("banner_pinned_liked_songs")
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Box(
                                    modifier = Modifier
                                        .size(50.dp)
                                        .shadow(12.dp, CircleShape, ambientColor = AuroraMagenta, spotColor = AuroraMagenta)
                                        .clip(CircleShape)
                                        .background(Brush.linearGradient(listOf(AuroraMagenta, AuroraViolet))),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Favorite,
                                        contentDescription = "Liked Songs",
                                        tint = Color.White,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                Column {
                                    Text(
                                        text = "Liked Songs",
                                        color = CrispWhite,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${favorites.size} favorite tracks • Always synced",
                                        color = Color(0xDDEBEBF5),
                                        fontSize = 12.sp
                                    )
                                }
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = "Open Liked Songs",
                                tint = CrispWhite,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            } else if (activeSubView == LibrarySubView.DOWNLOADED) {
                if (downloads.isEmpty()) {
                    item {
                        EmptySubViewState("No Downloaded Tracks", "Tap the download button on any track to save it offline.")
                    }
                } else {
                    items(downloads, key = { "dl_${it.id}" }, contentType = { "download_row" }) { song ->
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
                val targetList = when (activeSubView) {
                    LibrarySubView.PLAYLISTS -> listOf(likedPlaylist) + userPlaylists
                    LibrarySubView.ARTISTS -> artistGroups
                    LibrarySubView.ALBUMS -> albumGroups
                    else -> emptyList()
                }
                if (targetList.isEmpty()) {
                    item {
                        EmptySubViewState("Nothing here yet", "Play or import tracks to populate your library.")
                    }
                } else {
                    items(targetList, key = { "${activeSubView.name}_${it.id}" }, contentType = { "collection_row" }) { collection ->
                        val isLiked = collection.id == "pl_liked_songs"
                        val isDeletable = isEditMode && activeSubView == LibrarySubView.PLAYLISTS && !isLiked
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 18.dp, vertical = 4.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(FrostedGlassSurface)
                                .border(1.dp, FrostedGlassBorder, RoundedCornerShape(16.dp))
                                .bouncyClick { onPlaylistClick(collection) }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (isLiked) Brush.linearGradient(listOf(AuroraMagenta, AuroraViolet))
                                        else Brush.linearGradient(listOf(MidnightElevated, FrostedGlassSurface))
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isLiked) {
                                    Icon(Icons.Default.Favorite, contentDescription = null, tint = Color.White)
                                } else if (collection.coverUrl.isNotBlank()) {
                                    AsyncImage(
                                        model = collection.coverUrl,
                                        contentDescription = collection.title,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Icon(Icons.Default.MusicNote, contentDescription = null, tint = AuroraCyan)
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = collection.title,
                                    color = CrispWhite,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = collection.subtitle,
                                    color = SlateSecondary,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            if (isDeletable) {
                                IconButton(onClick = { playlistToDelete = collection }) {
                                    Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = IosRed)
                                }
                            } else {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = null,
                                    tint = SlateSecondary
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreatePlaylistDialog) {
        AlertDialog(
            onDismissRequest = { showCreatePlaylistDialog = false },
            containerColor = MidnightElevated,
            title = {
                Text(
                    text = "New Playlist",
                    color = CrispWhite,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = newPlaylistName,
                        onValueChange = { newPlaylistName = it },
                        placeholder = { Text("Playlist name", color = SlateSecondary) },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AuroraCyan,
                            unfocusedBorderColor = FrostedGlassBorder,
                            focusedTextColor = CrispWhite,
                            unfocusedTextColor = CrispWhite
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    TextButton(
                        onClick = {
                            showCreatePlaylistDialog = false
                            onOpenImport()
                        },
                        modifier = Modifier.align(Alignment.Start)
                    ) {
                        Text("Or Import from Spotify Link / Search", color = AuroraViolet, fontWeight = FontWeight.SemiBold)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val title = newPlaylistName.trim()
                        if (title.isNotEmpty()) {
                            scope.launch { repository.createCustomPlaylist(title) }
                            newPlaylistName = ""
                            showCreatePlaylistDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AuroraCyan)
                ) {
                    Text("Create", color = MidnightCanvas, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreatePlaylistDialog = false }) {
                    Text("Cancel", color = SlateSecondary)
                }
            }
        )
    }

    if (playlistToDelete != null) {
        val pl = playlistToDelete!!
        AlertDialog(
            onDismissRequest = { playlistToDelete = null },
            containerColor = MidnightElevated,
            title = {
                Text(
                    text = "Remove Playlist?",
                    color = CrispWhite,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to remove \"${pl.title}\" from your library?",
                    color = SlateSecondary,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch { repository.deletePlaylist(pl.id) }
                        playlistToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = IosRed)
                ) {
                    Text("Remove", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { playlistToDelete = null }) {
                    Text("Cancel", color = SlateSecondary)
                }
            }
        )
    }
}

@Composable
private fun LibraryCategoryRow(
    icon: ImageVector,
    title: String,
    countText: String,
    tint: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(tint.copy(alpha = 0.18f))
                .border(0.8.dp, tint.copy(alpha = 0.5f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(19.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = title,
            color = CrispWhite,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )

        Text(
            text = countText,
            color = SlateSecondary,
            fontSize = 14.sp
        )

        Spacer(modifier = Modifier.width(6.dp))

        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = SlateSecondary,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun EmptySubViewState(title: String, subtitle: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 56.dp, start = 24.dp, end = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = title, color = CrispWhite, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(6.dp))
        Text(text = subtitle, color = SlateSecondary, fontSize = 13.sp)
    }
}
