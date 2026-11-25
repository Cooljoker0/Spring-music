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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.SpringApplication
import com.example.data.model.Playlist
import com.example.data.model.Song
import com.example.ui.components.EditorialSectionHeader
import com.example.ui.components.LiquidGlassPill
import com.example.ui.components.LiquidGlassSurface
import com.example.ui.components.SongRowItem
import com.example.ui.components.bouncyClick
import com.example.ui.theme.DarkGlassOpaque
import com.example.ui.theme.DestructiveRose
import com.example.ui.theme.DisplayFontFamily
import com.example.ui.theme.Graphite
import com.example.ui.theme.GraphiteElevated
import com.example.ui.theme.LocalAppCustomTheme
import com.example.ui.theme.MidnightBlack
import com.example.ui.theme.MutedGrey
import com.example.ui.theme.Silver
import com.example.ui.theme.SoftWhite
import com.example.ui.theme.WhiteHighlight
import com.example.ui.theme.WhiteHighlightSoft
import kotlinx.coroutines.launch

enum class LibrarySubView {
    OVERVIEW, PLAYLISTS, IMPORTED, ARTISTS, ALBUMS, DOWNLOADED, RECENT
}

private enum class LibraryFilterChip(val label: String, val subView: LibrarySubView) {
    ALL("All", LibrarySubView.OVERVIEW),
    PLAYLISTS("Playlists", LibrarySubView.PLAYLISTS),
    IMPORTED("Spotify Imports", LibrarySubView.IMPORTED),
    ALBUMS("Albums", LibrarySubView.ALBUMS),
    ARTISTS("Artists", LibrarySubView.ARTISTS),
    DOWNLOADED("Downloaded", LibrarySubView.DOWNLOADED),
    RECENT("Recently Played", LibrarySubView.RECENT)
}

private enum class LibrarySortOption(val label: String) {
    RECENT("Recently Added"),
    ALPHABETICAL("Title A–Z"),
    TRACK_COUNT("Most Tracks")
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
    val theme = LocalAppCustomTheme.current

    val favorites by repository.getFavorites().collectAsState(initial = emptyList())
    val downloads by repository.getDownloads().collectAsState(initial = emptyList())
    val recentlyPlayed by repository.getRecentlyPlayed().collectAsState(initial = emptyList())
    val userPlaylists by repository.getUserPlaylists().collectAsState(initial = emptyList())
    val recommendationsState by repository.recommendationsState.collectAsState()

    var activeSubView by remember { mutableStateOf(LibrarySubView.OVERVIEW) }
    var searchQuery by remember { mutableStateOf("") }
    var isGridView by remember { mutableStateOf(true) }
    var sortOption by remember { mutableStateOf(LibrarySortOption.RECENT) }
    var showSortMenu by remember { mutableStateOf(false) }
    var isEditMode by remember { mutableStateOf(false) }

    var playlistToDelete by remember { mutableStateOf<Playlist?>(null) }
    var playlistToEdit by remember { mutableStateOf<Playlist?>(null) }
    var editPlaylistTitle by remember { mutableStateOf("") }
    var editPlaylistDescription by remember { mutableStateOf("") }

    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }
    var newPlaylistDescription by remember { mutableStateOf("") }

    val canEditCurrentView = activeSubView == LibrarySubView.OVERVIEW ||
        activeSubView == LibrarySubView.PLAYLISTS ||
        activeSubView == LibrarySubView.IMPORTED

    LaunchedEffect(activeSubView) {
        if (!canEditCurrentView && isEditMode) {
            isEditMode = false
        }
    }

    BackHandler(enabled = activeSubView != LibrarySubView.OVERVIEW || searchQuery.isNotBlank()) {
        if (searchQuery.isNotBlank()) {
            searchQuery = ""
        } else {
            isEditMode = false
            activeSubView = LibrarySubView.OVERVIEW
        }
    }

    val likedPlaylist = remember(favorites) {
        Playlist(
            id = "pl_liked_songs",
            title = "Liked Songs",
            subtitle = "${favorites.size} favorite tracks",
            coverUrl = favorites.firstOrNull { it.coverUrl.isNotBlank() }?.coverUrl.orEmpty(),
            songCount = favorites.size,
            songs = favorites
        )
    }

    val importedPlaylists = remember(userPlaylists) {
        userPlaylists.filter {
            it.id.startsWith("spring_") ||
                it.id.startsWith("spotify_") ||
                it.subtitle.contains("Imported", ignoreCase = true)
        }
    }

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
                    subtitle = "${songs.size} tracks",
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
                    subtitle = "${songs.firstOrNull()?.artist ?: "Spring"} • ${songs.size} tracks",
                    coverUrl = songs.firstOrNull { it.coverUrl.isNotBlank() }?.coverUrl.orEmpty(),
                    songCount = songs.size,
                    songs = songs
                )
            }
    }

    fun applySortAndFilter(items: List<Playlist>): List<Playlist> {
        val q = searchQuery.trim()
        val filtered = if (q.isEmpty()) items else {
            items.filter {
                it.title.contains(q, ignoreCase = true) ||
                    it.subtitle.contains(q, ignoreCase = true) ||
                    it.songs.any { s -> s.title.contains(q, true) || s.artist.contains(q, true) }
            }
        }
        return when (sortOption) {
            LibrarySortOption.RECENT -> filtered
            LibrarySortOption.ALPHABETICAL -> filtered.sortedBy { it.title.lowercase() }
            LibrarySortOption.TRACK_COUNT -> filtered.sortedByDescending { it.songs.size.coerceAtLeast(it.songCount) }
        }
    }

    val matchingSongs = remember(searchQuery, allKnownSongs) {
        val q = searchQuery.trim()
        if (q.isEmpty()) emptyList()
        else allKnownSongs.filter {
            it.title.contains(q, true) || it.artist.contains(q, true) || it.album.contains(q, true)
        }.take(25)
    }

    val overviewCollections = remember(userPlaylists, albumGroups, sortOption, searchQuery) {
        val merged = (userPlaylists + albumGroups).distinctBy { it.id }
        applySortAndFilter(merged)
    }

    val userPlaylistIds = remember(userPlaylists) { userPlaylists.map { it.id }.toSet() }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(theme.canvasBackground),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 880.dp),
            contentPadding = PaddingValues(bottom = 186.dp)
        ) {
            // Header
            item(key = "lib_header") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(start = 16.dp, end = 10.dp, top = 16.dp, bottom = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (activeSubView != LibrarySubView.OVERVIEW) {
                            IconButton(
                                onClick = {
                                    isEditMode = false
                                    activeSubView = LibrarySubView.OVERVIEW
                                },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back to Library",
                                    tint = SoftWhite
                                )
                            }
                        } else {
                            Spacer(modifier = Modifier.width(4.dp))
                        }

                        Text(
                            text = when (activeSubView) {
                                LibrarySubView.OVERVIEW -> "Library"
                                LibrarySubView.PLAYLISTS -> "Playlists"
                                LibrarySubView.IMPORTED -> "Spotify Imports"
                                LibrarySubView.ARTISTS -> "Artists"
                                LibrarySubView.ALBUMS -> "Albums"
                                LibrarySubView.DOWNLOADED -> "Downloaded"
                                LibrarySubView.RECENT -> "Recently Played"
                            },
                            color = SoftWhite,
                            fontFamily = DisplayFontFamily,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.5).sp
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        if (canEditCurrentView && userPlaylists.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isEditMode) WhiteHighlightSoft else Color.Transparent)
                                    .clickable { isEditMode = !isEditMode }
                                    .padding(horizontal = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (isEditMode) "Done" else "Edit",
                                    color = if (isEditMode) SoftWhite else Silver,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.testTag("btn_library_edit")
                                )
                            }
                        }

                        IconButton(
                            onClick = { isGridView = !isGridView },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("btn_library_view_toggle")
                        ) {
                            Icon(
                                imageVector = if (isGridView) Icons.AutoMirrored.Filled.ViewList else Icons.Default.GridView,
                                contentDescription = if (isGridView) "Switch to List View" else "Switch to Grid View",
                                tint = Silver,
                                modifier = Modifier.size(21.dp)
                            )
                        }

                        IconButton(
                            onClick = { showCreatePlaylistDialog = true },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("btn_library_add_playlist")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Create Playlist",
                                tint = SoftWhite,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }

            // Library Search Bar + Sort Control
            item(key = "lib_search_sort") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    LiquidGlassSurface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        elevation = 8.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search Library",
                                tint = Silver,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .defaultMinSize(minHeight = 42.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                if (searchQuery.isEmpty()) {
                                    Text(
                                        text = "Search playlists, songs, or artists",
                                        color = MutedGrey,
                                        fontSize = 13.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                BasicTextField(
                                    value = searchQuery,
                                    onValueChange = { searchQuery = it },
                                    singleLine = true,
                                    textStyle = TextStyle(color = SoftWhite, fontSize = 14.sp),
                                    cursorBrush = SolidColor(SoftWhite),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("library_search_input")
                                )
                            }
                            if (searchQuery.isNotBlank()) {
                                IconButton(
                                    onClick = { searchQuery = "" },
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Clear",
                                        tint = Silver,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }

                    Box {
                        LiquidGlassSurface(
                            modifier = Modifier
                                .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                                .clickable { showSortMenu = true }
                                .testTag("btn_library_sort"),
                            shape = RoundedCornerShape(16.dp),
                            elevation = 8.dp
                        ) {
                            Box(
                                modifier = Modifier.size(48.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Sort,
                                    contentDescription = "Sort Library",
                                    tint = SoftWhite,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false },
                            modifier = Modifier
                                .background(DarkGlassOpaque)
                                .border(0.8.dp, WhiteHighlight, RoundedCornerShape(12.dp))
                        ) {
                            LibrarySortOption.entries.forEach { option ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = option.label,
                                            color = if (sortOption == option) SoftWhite else Silver,
                                            fontWeight = if (sortOption == option) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    onClick = {
                                        sortOption = option
                                        showSortMenu = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Filter Pills
            item(key = "lib_filter_chips") {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(LibraryFilterChip.entries, key = { it.name }) { chip ->
                        val isSelected = activeSubView == chip.subView
                        LiquidGlassPill(
                            isActive = isSelected,
                            onClick = { activeSubView = chip.subView }
                        ) {
                            Text(
                                text = chip.label,
                                color = if (isSelected) MidnightBlack else SoftWhite,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }

            if (searchQuery.isNotBlank() && matchingSongs.isNotEmpty() &&
                activeSubView != LibrarySubView.DOWNLOADED && activeSubView != LibrarySubView.RECENT
            ) {
                item(key = "lib_matching_songs_header") {
                    EditorialSectionHeader(
                        title = "Matching Tracks",
                        subtitle = "${matchingSongs.size} in your library"
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                }
                itemsIndexed(
                    items = matchingSongs,
                    key = { idx, s -> "lib_search_${idx}_${s.id}" }
                ) { index, song ->
                    val isCurrent = currentSong?.id == song.id
                    Column(modifier = Modifier.fillMaxWidth()) {
                        SongRowItem(
                            song = song,
                            isCurrent = isCurrent,
                            isPlaying = isPlaying && isCurrent,
                            onClick = { onSongSelected(song, matchingSongs) },
                            onFavoriteToggle = { onFavoriteToggle(song) },
                            onDownloadClick = { onDownloadClick(song) },
                            onPlayNext = onPlayNext?.let { cb -> { cb(song) } },
                            onAddToQueue = onAddToQueue?.let { cb -> { cb(song) } },
                            onAddToPlaylist = onAddToPlaylist?.let { cb -> { cb(song) } },
                            onStartRadio = onStartRadio?.let { cb -> { cb(song) } }
                        )
                        if (index < matchingSongs.lastIndex) {
                            HorizontalDivider(
                                color = WhiteHighlightSoft,
                                thickness = 0.5.dp,
                                modifier = Modifier.padding(start = 78.dp, end = 20.dp)
                            )
                        }
                    }
                }
                item { Spacer(modifier = Modifier.height(14.dp)) }
            }

            when (activeSubView) {
                LibrarySubView.OVERVIEW -> {
                    // Featured Liked Songs Liquid Glass Banner
                    item(key = "liked_banner") {
                        LiquidGlassSurface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 4.dp)
                                .bouncyClick(pressedScale = 0.985f) { onPlaylistClick(likedPlaylist) }
                                .testTag("banner_pinned_liked_songs"),
                            shape = RoundedCornerShape(18.dp),
                            elevation = 10.dp
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(50.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(WhiteHighlightSoft)
                                        .border(0.7.dp, WhiteHighlight, RoundedCornerShape(12.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Favorite,
                                        contentDescription = "Liked Songs",
                                        tint = SoftWhite,
                                        modifier = Modifier.size(23.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Liked Songs",
                                        color = SoftWhite,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "${favorites.size} favorite tracks",
                                        color = Silver,
                                        fontSize = 13.sp
                                    )
                                }
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = "Open Liked Songs",
                                    tint = MutedGrey,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    // All 7 Collections Index
                    item(key = "lib_categories") {
                        Spacer(modifier = Modifier.height(14.dp))
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(Graphite)
                                .border(0.75.dp, WhiteHighlightSoft, RoundedCornerShape(18.dp))
                        ) {
                            LibraryCategoryRow(
                                icon = Icons.AutoMirrored.Filled.PlaylistPlay,
                                title = "Playlists",
                                countText = "${userPlaylists.size + 1}",
                                onClick = { activeSubView = LibrarySubView.PLAYLISTS }
                            )
                            LibraryRowDivider()
                            LibraryCategoryRow(
                                icon = Icons.Default.CloudDownload,
                                title = "Imported Spotify Playlists",
                                countText = "${importedPlaylists.size}",
                                onClick = { activeSubView = LibrarySubView.IMPORTED }
                            )
                            LibraryRowDivider()
                            LibraryCategoryRow(
                                icon = Icons.Default.Album,
                                title = "Albums",
                                countText = "${albumGroups.size}",
                                onClick = { activeSubView = LibrarySubView.ALBUMS }
                            )
                            LibraryRowDivider()
                            LibraryCategoryRow(
                                icon = Icons.Default.Person,
                                title = "Artists",
                                countText = "${artistGroups.size}",
                                onClick = { activeSubView = LibrarySubView.ARTISTS }
                            )
                            LibraryRowDivider()
                            LibraryCategoryRow(
                                icon = Icons.Default.DownloadDone,
                                title = "Downloaded Music",
                                countText = "${downloads.size}",
                                onClick = { activeSubView = LibrarySubView.DOWNLOADED }
                            )
                            LibraryRowDivider()
                            LibraryCategoryRow(
                                icon = Icons.Default.History,
                                title = "Recently Played",
                                countText = "${recentlyPlayed.size}",
                                onClick = { activeSubView = LibrarySubView.RECENT }
                            )
                        }
                    }

                    // Playlists & Collections Grid or List
                    item(key = "lib_collections_header") {
                        Spacer(modifier = Modifier.height(24.dp))
                        EditorialSectionHeader(
                            title = "Your Collections",
                            subtitle = "${overviewCollections.size} playlists & albums • ${sortOption.label}",
                            actionLabel = "Import",
                            onActionClick = onOpenImport
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    if (overviewCollections.isEmpty()) {
                        item(key = "lib_empty_collections") {
                            EmptyLibraryCard(
                                title = if (searchQuery.isNotBlank()) "No matching collections" else "No playlists or albums yet",
                                subtitle = if (searchQuery.isNotBlank()) "Try a different keyword." else "Create a custom playlist or import one from Spotify.",
                                actionLabel = if (searchQuery.isNotBlank()) "Clear Search" else "Import Spotify Playlist",
                                onAction = {
                                    if (searchQuery.isNotBlank()) searchQuery = "" else onOpenImport()
                                }
                            )
                        }
                    } else if (isGridView) {
                        val rows = overviewCollections.chunked(2)
                        itemsIndexed(
                            items = rows,
                            key = { idx, pair -> "grid_row_${idx}_${pair.first().id}" }
                        ) { _, rowItems ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp)
                                    .padding(bottom = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                rowItems.forEach { item ->
                                    val isUserOwned = userPlaylistIds.contains(item.id)
                                    val trackCount = item.songs.size.coerceAtLeast(item.songCount)
                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .bouncyClick(pressedScale = 0.97f) {
                                                if (isEditMode && isUserOwned) {
                                                    playlistToEdit = item
                                                    editPlaylistTitle = item.title
                                                    editPlaylistDescription = item.subtitle
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
                                                .background(Graphite)
                                                .border(0.7.dp, WhiteHighlightSoft, RoundedCornerShape(14.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (item.coverUrl.isNotBlank()) {
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
                                                    tint = MutedGrey,
                                                    modifier = Modifier.size(36.dp)
                                                )
                                            }

                                            if (isEditMode && isUserOwned) {
                                                Row(
                                                    modifier = Modifier
                                                        .align(Alignment.TopEnd)
                                                        .padding(6.dp),
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(34.dp)
                                                            .clip(CircleShape)
                                                            .background(MidnightBlack.copy(alpha = 0.85f))
                                                            .border(0.7.dp, WhiteHighlight, CircleShape)
                                                            .clickable {
                                                                playlistToEdit = item
                                                                editPlaylistTitle = item.title
                                                                editPlaylistDescription = item.subtitle
                                                            },
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Edit,
                                                            contentDescription = "Edit ${item.title}",
                                                            tint = SoftWhite,
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }
                                                    Box(
                                                        modifier = Modifier
                                                            .size(34.dp)
                                                            .clip(CircleShape)
                                                            .background(DestructiveRose)
                                                            .clickable { playlistToDelete = item },
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.DeleteOutline,
                                                            contentDescription = "Remove ${item.title}",
                                                            tint = MidnightBlack,
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = item.title,
                                            color = SoftWhite,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "$trackCount tracks",
                                            color = Silver,
                                            fontSize = 12.sp,
                                            maxLines = 1
                                        )
                                    }
                                }
                                repeat((2 - rowItems.size).coerceAtLeast(0)) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    } else {
                        itemsIndexed(
                            items = overviewCollections,
                            key = { _, it -> "list_${it.id}" }
                        ) { index, collection ->
                            CollectionListRow(
                                collection = collection,
                                isLiked = collection.id == "pl_liked_songs",
                                isEditable = isEditMode && userPlaylistIds.contains(collection.id),
                                onClick = { onPlaylistClick(collection) },
                                onEdit = {
                                    playlistToEdit = collection
                                    editPlaylistTitle = collection.title
                                    editPlaylistDescription = collection.subtitle
                                },
                                onDelete = { playlistToDelete = collection }
                            )
                            if (index < overviewCollections.lastIndex) {
                                HorizontalDivider(
                                    color = WhiteHighlightSoft,
                                    thickness = 0.5.dp,
                                    modifier = Modifier.padding(start = 86.dp, end = 20.dp)
                                )
                            }
                        }
                    }
                }

                LibrarySubView.DOWNLOADED, LibrarySubView.RECENT -> {
                    val baseSongs = if (activeSubView == LibrarySubView.DOWNLOADED) downloads else recentlyPlayed
                    val q = searchQuery.trim()
                    val filteredSongs = if (q.isEmpty()) baseSongs else {
                        baseSongs.filter {
                            it.title.contains(q, true) || it.artist.contains(q, true)
                        }
                    }
                    if (filteredSongs.isEmpty()) {
                        item {
                            EmptyLibraryCard(
                                title = if (activeSubView == LibrarySubView.DOWNLOADED) "No downloaded tracks" else "No listening history yet",
                                subtitle = if (activeSubView == LibrarySubView.DOWNLOADED) "Download songs for offline playback." else "Tracks you play will appear here.",
                                actionLabel = null,
                                onAction = null
                            )
                        }
                    } else {
                        itemsIndexed(
                            items = filteredSongs,
                            key = { idx, song -> "${activeSubView.name}_${idx}_${song.id}" }
                        ) { index, song ->
                            val isCurrent = currentSong?.id == song.id
                            Column(modifier = Modifier.fillMaxWidth()) {
                                SongRowItem(
                                    song = song,
                                    isCurrent = isCurrent,
                                    isPlaying = isPlaying && isCurrent,
                                    onClick = { onSongSelected(song, filteredSongs) },
                                    onFavoriteToggle = { onFavoriteToggle(song) },
                                    onDownloadClick = { onDownloadClick(song) },
                                    onPlayNext = onPlayNext?.let { cb -> { cb(song) } },
                                    onAddToQueue = onAddToQueue?.let { cb -> { cb(song) } },
                                    onAddToPlaylist = onAddToPlaylist?.let { cb -> { cb(song) } },
                                    onStartRadio = onStartRadio?.let { cb -> { cb(song) } }
                                )
                                if (index < filteredSongs.lastIndex) {
                                    HorizontalDivider(
                                        color = WhiteHighlightSoft,
                                        thickness = 0.5.dp,
                                        modifier = Modifier.padding(start = 78.dp, end = 20.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                LibrarySubView.PLAYLISTS, LibrarySubView.IMPORTED, LibrarySubView.ARTISTS, LibrarySubView.ALBUMS -> {
                    val baseList = when (activeSubView) {
                        LibrarySubView.PLAYLISTS -> listOf(likedPlaylist) + userPlaylists
                        LibrarySubView.IMPORTED -> importedPlaylists
                        LibrarySubView.ARTISTS -> artistGroups
                        LibrarySubView.ALBUMS -> albumGroups
                        else -> emptyList()
                    }
                    val targetList = applySortAndFilter(baseList)

                    if (targetList.isEmpty()) {
                        item {
                            EmptyLibraryCard(
                                title = "Nothing here yet",
                                subtitle = if (activeSubView == LibrarySubView.IMPORTED) {
                                    "Import any public Spotify playlist to listen inside Spring Music."
                                } else {
                                    "Create a playlist or explore music to populate this section."
                                },
                                actionLabel = "Import Playlist",
                                onAction = onOpenImport
                            )
                        }
                    } else if (isGridView && activeSubView != LibrarySubView.ARTISTS) {
                        val rows = targetList.chunked(2)
                        itemsIndexed(
                            items = rows,
                            key = { idx, pair -> "${activeSubView.name}_grid_${idx}_${pair.first().id}" }
                        ) { _, rowItems ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp)
                                    .padding(bottom = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                rowItems.forEach { item ->
                                    val isUserOwned = userPlaylistIds.contains(item.id)
                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .bouncyClick { onPlaylistClick(item) }
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .aspectRatio(1f)
                                                .clip(RoundedCornerShape(14.dp))
                                                .background(Graphite)
                                                .border(0.7.dp, WhiteHighlightSoft, RoundedCornerShape(14.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (item.id == "pl_liked_songs") {
                                                Icon(Icons.Default.Favorite, contentDescription = null, tint = SoftWhite, modifier = Modifier.size(36.dp))
                                            } else if (item.coverUrl.isNotBlank()) {
                                                AsyncImage(
                                                    model = item.coverUrl,
                                                    contentDescription = item.title,
                                                    modifier = Modifier.fillMaxSize(),
                                                    contentScale = ContentScale.Crop
                                                )
                                            } else {
                                                Icon(Icons.Default.MusicNote, contentDescription = null, tint = MutedGrey, modifier = Modifier.size(36.dp))
                                            }

                                            if (isEditMode && isUserOwned) {
                                                Row(
                                                    modifier = Modifier.align(Alignment.TopEnd).padding(6.dp),
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(34.dp)
                                                            .clip(CircleShape)
                                                            .background(MidnightBlack.copy(alpha = 0.85f))
                                                            .clickable {
                                                                playlistToEdit = item
                                                                editPlaylistTitle = item.title
                                                                editPlaylistDescription = item.subtitle
                                                            },
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = SoftWhite, modifier = Modifier.size(16.dp))
                                                    }
                                                    Box(
                                                        modifier = Modifier
                                                            .size(34.dp)
                                                            .clip(CircleShape)
                                                            .background(DestructiveRose)
                                                            .clickable { playlistToDelete = item },
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = MidnightBlack, modifier = Modifier.size(16.dp))
                                                    }
                                                }
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(item.title, color = SoftWhite, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text(item.subtitle, color = Silver, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                }
                                repeat((2 - rowItems.size).coerceAtLeast(0)) { Spacer(modifier = Modifier.weight(1f)) }
                            }
                        }
                    } else {
                        itemsIndexed(
                            items = targetList,
                            key = { _, it -> "${activeSubView.name}_${it.id}" }
                        ) { index, collection ->
                            val isLiked = collection.id == "pl_liked_songs"
                            val isEditable = isEditMode && userPlaylistIds.contains(collection.id)
                            CollectionListRow(
                                collection = collection,
                                isLiked = isLiked,
                                isEditable = isEditable,
                                isCircularAvatar = activeSubView == LibrarySubView.ARTISTS,
                                onClick = { onPlaylistClick(collection) },
                                onEdit = {
                                    playlistToEdit = collection
                                    editPlaylistTitle = collection.title
                                    editPlaylistDescription = collection.subtitle
                                },
                                onDelete = { playlistToDelete = collection }
                            )
                            if (index < targetList.lastIndex) {
                                HorizontalDivider(
                                    color = WhiteHighlightSoft,
                                    thickness = 0.5.dp,
                                    modifier = Modifier.padding(start = 86.dp, end = 20.dp)
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
            containerColor = DarkGlassOpaque,
            title = {
                Text(
                    text = "New Playlist",
                    color = SoftWhite,
                    fontFamily = DisplayFontFamily,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = newPlaylistName,
                        onValueChange = { newPlaylistName = it },
                        placeholder = { Text("Playlist name", color = MutedGrey) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SoftWhite,
                            unfocusedBorderColor = WhiteHighlightSoft,
                            focusedTextColor = SoftWhite,
                            unfocusedTextColor = SoftWhite
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newPlaylistDescription,
                        onValueChange = { newPlaylistDescription = it },
                        placeholder = { Text("Description (optional)", color = MutedGrey) },
                        maxLines = 2,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SoftWhite,
                            unfocusedBorderColor = WhiteHighlightSoft,
                            focusedTextColor = SoftWhite,
                            unfocusedTextColor = SoftWhite
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    TextButton(
                        onClick = {
                            showCreatePlaylistDialog = false
                            onOpenImport()
                        },
                        modifier = Modifier
                            .align(Alignment.Start)
                            .defaultMinSize(minHeight = 48.dp)
                    ) {
                        Icon(Icons.Default.PlaylistAdd, contentDescription = null, tint = SoftWhite, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Import from Spotify or Link", color = SoftWhite, fontWeight = FontWeight.SemiBold)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val title = newPlaylistName.trim()
                        if (title.isNotEmpty()) {
                            scope.launch {
                                repository.createCustomPlaylist(title, newPlaylistDescription.trim())
                            }
                            newPlaylistName = ""
                            newPlaylistDescription = ""
                            showCreatePlaylistDialog = false
                        }
                    },
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.defaultMinSize(minHeight = 48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SoftWhite, contentColor = MidnightBlack)
                ) {
                    Text("Create", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showCreatePlaylistDialog = false },
                    modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                ) {
                    Text("Cancel", color = Silver)
                }
            }
        )
    }

    if (playlistToEdit != null) {
        val editing = playlistToEdit!!
        AlertDialog(
            onDismissRequest = { playlistToEdit = null },
            containerColor = DarkGlassOpaque,
            title = {
                Text(
                    text = "Edit Playlist",
                    color = SoftWhite,
                    fontFamily = DisplayFontFamily,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = editPlaylistTitle,
                        onValueChange = { editPlaylistTitle = it },
                        placeholder = { Text("Playlist title", color = MutedGrey) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SoftWhite,
                            unfocusedBorderColor = WhiteHighlightSoft,
                            focusedTextColor = SoftWhite,
                            unfocusedTextColor = SoftWhite
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editPlaylistDescription,
                        onValueChange = { editPlaylistDescription = it },
                        placeholder = { Text("Description", color = MutedGrey) },
                        maxLines = 2,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SoftWhite,
                            unfocusedBorderColor = WhiteHighlightSoft,
                            focusedTextColor = SoftWhite,
                            unfocusedTextColor = SoftWhite
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmed = editPlaylistTitle.trim()
                        if (trimmed.isNotEmpty()) {
                            scope.launch {
                                repository.updatePlaylistDetails(editing.id, trimmed, editPlaylistDescription.trim())
                            }
                            playlistToEdit = null
                        }
                    },
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.defaultMinSize(minHeight = 48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SoftWhite, contentColor = MidnightBlack)
                ) {
                    Text("Save", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { playlistToEdit = null },
                    modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                ) {
                    Text("Cancel", color = Silver)
                }
            }
        )
    }

    if (playlistToDelete != null) {
        val pl = playlistToDelete!!
        AlertDialog(
            onDismissRequest = { playlistToDelete = null },
            containerColor = DarkGlassOpaque,
            title = {
                Text(
                    text = "Remove Playlist?",
                    color = SoftWhite,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to remove \"${pl.title}\" from your library?",
                    color = Silver,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch { repository.deletePlaylist(pl.id) }
                        playlistToDelete = null
                    },
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.defaultMinSize(minHeight = 48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DestructiveRose, contentColor = MidnightBlack)
                ) {
                    Text("Remove", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { playlistToDelete = null },
                    modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                ) {
                    Text("Cancel", color = Silver)
                }
            }
        )
    }
}

@Composable
private fun LibraryRowDivider() {
    HorizontalDivider(
        color = WhiteHighlightSoft,
        thickness = 0.5.dp,
        modifier = Modifier.padding(start = 54.dp)
    )
}

@Composable
private fun LibraryCategoryRow(
    icon: ImageVector,
    title: String,
    countText: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 52.dp)
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = SoftWhite,
            modifier = Modifier.size(21.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = title,
            color = SoftWhite,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = countText,
            color = Silver,
            fontSize = 13.sp
        )
        Spacer(modifier = Modifier.width(6.dp))
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MutedGrey,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun CollectionListRow(
    collection: Playlist,
    isLiked: Boolean,
    isEditable: Boolean,
    isCircularAvatar: Boolean = false,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val thumbShape = if (isCircularAvatar) CircleShape else RoundedCornerShape(10.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(thumbShape)
                .background(if (isLiked) WhiteHighlightSoft else Graphite)
                .border(0.6.dp, WhiteHighlightSoft, thumbShape),
            contentAlignment = Alignment.Center
        ) {
            if (isLiked) {
                Icon(Icons.Default.Favorite, contentDescription = null, tint = SoftWhite, modifier = Modifier.size(22.dp))
            } else if (collection.coverUrl.isNotBlank()) {
                AsyncImage(
                    model = collection.coverUrl,
                    contentDescription = collection.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(Icons.Default.MusicNote, contentDescription = null, tint = MutedGrey)
            }
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = collection.title,
                color = SoftWhite,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = collection.subtitle,
                color = Silver,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (isEditable) {
            IconButton(onClick = onEdit, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Default.Edit, contentDescription = "Edit ${collection.title}", tint = SoftWhite, modifier = Modifier.size(19.dp))
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Default.DeleteOutline, contentDescription = "Delete ${collection.title}", tint = DestructiveRose, modifier = Modifier.size(19.dp))
            }
        } else {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MutedGrey
            )
        }
    }
}

@Composable
private fun EmptyLibraryCard(
    title: String,
    subtitle: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 20.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Graphite)
            .border(0.75.dp, WhiteHighlightSoft, RoundedCornerShape(18.dp))
            .padding(22.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            color = SoftWhite,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = subtitle,
            color = Silver,
            fontSize = 13.sp,
            textAlign = TextAlign.Center
        )
        if (actionLabel != null && onAction != null) {
            Spacer(modifier = Modifier.height(14.dp))
            Button(
                onClick = onAction,
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SoftWhite,
                    contentColor = MidnightBlack
                ),
                modifier = Modifier.height(48.dp)
            ) {
                Text(actionLabel, fontWeight = FontWeight.Bold)
            }
        }
    }
}
