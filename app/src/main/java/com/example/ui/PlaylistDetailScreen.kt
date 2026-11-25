package com.example.ui

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
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
import com.example.data.network.PresetCatalog
import com.example.ui.components.LiquidGlassPill
import com.example.ui.components.LiquidGlassSurface
import com.example.ui.components.SongRowItem
import com.example.ui.components.adaptiveBackdropBlur
import com.example.ui.theme.DarkGlassOpaque
import com.example.ui.theme.DestructiveRose
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistDetailScreen(
    playlist: Playlist,
    currentSong: Song?,
    isPlaying: Boolean,
    onSongSelected: (Song, List<Song>) -> Unit,
    onFavoriteToggle: (Song) -> Unit,
    onDownloadClick: (Song) -> Unit,
    onBack: () -> Unit,
    onPlaylistDeleted: () -> Unit = {},
    onPlayNext: ((Song) -> Unit)? = null,
    onAddToQueue: ((Song) -> Unit)? = null,
    onStartRadio: ((Song) -> Unit)? = null
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val app = context.applicationContext as? SpringApplication
    val repository = app?.repository
    val scope = rememberCoroutineScope()
    val theme = LocalAppCustomTheme.current

    val userPlaylists by repository?.getUserPlaylists()?.collectAsState(initial = emptyList())
        ?: remember { mutableStateOf(emptyList()) }
    val favorites by repository?.getFavorites()?.collectAsState(initial = emptyList())
        ?: remember { mutableStateOf(emptyList()) }
    val topCharts by repository?.topChartsState?.collectAsState()
        ?: remember { mutableStateOf(emptyList()) }

    val livePlaylist = remember(playlist, userPlaylists, favorites) {
        when {
            playlist.id == "pl_liked_songs" -> playlist.copy(
                subtitle = "${favorites.size} favorite tracks",
                coverUrl = favorites.firstOrNull { it.coverUrl.isNotBlank() }?.coverUrl ?: playlist.coverUrl,
                songCount = favorites.size,
                songs = favorites
            )
            else -> userPlaylists.find { it.id == playlist.id } ?: playlist
        }
    }

    val isUserOwned = livePlaylist.id != "pl_liked_songs" &&
        userPlaylists.any { it.id == livePlaylist.id } &&
        PresetCatalog.PRESET_PLAYLISTS.none { it.id == livePlaylist.id }

    var showDeleteDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showAddSongsSheet by remember { mutableStateOf(false) }
    var editTitle by remember(livePlaylist.title) { mutableStateOf(livePlaylist.title) }
    var editDescription by remember(livePlaylist.subtitle) { mutableStateOf(livePlaylist.subtitle) }

    val listState = rememberLazyListState()
    val showStickyTitle by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 300
        }
    }

    val totalDurationText = remember(livePlaylist.songs) {
        val totalSeconds = livePlaylist.songs.sumOf { (it.durationMs / 1000L).coerceAtLeast(0L) }
        if (totalSeconds <= 0L) "" else {
            val hours = totalSeconds / 3600L
            val minutes = (totalSeconds % 3600L) / 60L
            if (hours > 0L) "${hours}h ${minutes}m" else "${minutes.coerceAtLeast(1L)} min"
        }
    }

    val subtitleText = remember(livePlaylist.subtitle, livePlaylist.songs.size, totalDurationText) {
        val base = livePlaylist.subtitle.ifBlank { "${livePlaylist.songs.size} tracks" }
        if (totalDurationText.isNotEmpty() && !base.contains("min") && !base.contains("h ")) {
            "$base • $totalDurationText"
        } else {
            base
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            containerColor = DarkGlassOpaque,
            title = {
                Text(
                    text = "Delete Playlist?",
                    color = SoftWhite,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to remove \"${livePlaylist.title}\" from your library?",
                    color = Silver,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteDialog = false
                        scope.launch {
                            repository?.deletePlaylist(livePlaylist.id)
                            onPlaylistDeleted()
                            onBack()
                        }
                    },
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.defaultMinSize(minHeight = 48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DestructiveRose, contentColor = MidnightBlack)
                ) {
                    Text("Delete", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteDialog = false },
                    modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                ) {
                    Text("Cancel", color = Silver)
                }
            }
        )
    }

    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
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
                        value = editTitle,
                        onValueChange = { editTitle = it },
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
                        value = editDescription,
                        onValueChange = { editDescription = it },
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
                        val trimmed = editTitle.trim()
                        if (trimmed.isNotEmpty()) {
                            scope.launch {
                                repository?.updatePlaylistDetails(livePlaylist.id, trimmed, editDescription.trim())
                            }
                            showEditDialog = false
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
                    onClick = { showEditDialog = false },
                    modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                ) {
                    Text("Cancel", color = Silver)
                }
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(theme.canvasBackground),
        contentAlignment = Alignment.TopCenter
    ) {
        if (livePlaylist.coverUrl.isNotBlank()) {
            AsyncImage(
                model = livePlaylist.coverUrl,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(380.dp)
                    .adaptiveBackdropBlur(38.dp),
                contentScale = ContentScale.Crop,
                alpha = 0.28f
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                MidnightBlack.copy(alpha = 0.35f),
                                theme.canvasBackground
                            )
                        )
                    )
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 880.dp)
        ) {
            // Sticky Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (showStickyTitle) DarkGlassOpaque else Color.Transparent)
                    .statusBarsPadding()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("btn_playlist_back")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = SoftWhite
                    )
                }

                AnimatedVisibility(
                    visible = showStickyTitle,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp)
                ) {
                    Text(
                        text = livePlaylist.title,
                        color = SoftWhite,
                        fontFamily = DisplayFontFamily,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                }

                if (!showStickyTitle) {
                    Spacer(modifier = Modifier.weight(1f))
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = {
                            val shareText = buildString {
                                append("Spring Music Playlist: ${livePlaylist.title}\n")
                                append("${livePlaylist.songs.size} tracks")
                                val topList = livePlaylist.songs.take(5).joinToString(", ") { "${it.title} – ${it.artist}" }
                                if (topList.isNotBlank()) append("\nFeaturing: $topList")
                            }
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, livePlaylist.title)
                                putExtra(Intent.EXTRA_TEXT, shareText)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share Playlist"))
                        },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share Playlist",
                            tint = Silver,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    if (isUserOwned) {
                        IconButton(
                            onClick = {
                                editTitle = livePlaylist.title
                                editDescription = livePlaylist.subtitle
                                showEditDialog = true
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("btn_edit_playlist")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Playlist",
                                tint = Silver,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        IconButton(
                            onClick = { showDeleteDialog = true },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("btn_delete_playlist")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Delete Playlist",
                                tint = Silver,
                                modifier = Modifier.size(21.dp)
                            )
                        }
                    }
                }
            }

            if (showStickyTitle) {
                HorizontalDivider(color = WhiteHighlightSoft, thickness = 0.5.dp)
            }

            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 186.dp)
            ) {
                // Hero Artwork & Metadata
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(204.dp)
                                .shadow(
                                    elevation = 18.dp,
                                    shape = RoundedCornerShape(18.dp),
                                    ambientColor = Color.Black,
                                    spotColor = Color.Black
                                )
                                .clip(RoundedCornerShape(18.dp))
                                .background(Graphite)
                                .border(0.85.dp, WhiteHighlight, RoundedCornerShape(18.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (livePlaylist.coverUrl.isNotBlank()) {
                                AsyncImage(
                                    model = livePlaylist.coverUrl,
                                    contentDescription = livePlaylist.title,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.MusicNote,
                                    contentDescription = null,
                                    tint = MutedGrey,
                                    modifier = Modifier.size(64.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        Text(
                            text = livePlaylist.title,
                            color = SoftWhite,
                            fontFamily = DisplayFontFamily,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            textAlign = TextAlign.Center,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = subtitleText,
                            color = Silver,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            maxLines = 2
                        )
                    }
                }

                // Playback & Add Songs Actions
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LiquidGlassPill(
                            onClick = {
                                if (livePlaylist.songs.isNotEmpty()) {
                                    val shuffled = livePlaylist.songs.shuffled()
                                    onSongSelected(shuffled.first(), shuffled)
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Shuffle,
                                    contentDescription = "Shuffle Play",
                                    tint = SoftWhite,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Shuffle",
                                    color = SoftWhite,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp
                                )
                            }
                        }

                        LiquidGlassPill(
                            isActive = true,
                            onClick = {
                                if (livePlaylist.songs.isNotEmpty()) {
                                    onSongSelected(livePlaylist.songs.first(), livePlaylist.songs)
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("btn_playlist_play_all")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Play Playlist",
                                    tint = MidnightBlack,
                                    modifier = Modifier.size(21.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = "Play All",
                                    color = MidnightBlack,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }

                        if (isUserOwned) {
                            LiquidGlassPill(
                                onClick = { showAddSongsSheet = true },
                                modifier = Modifier
                                    .height(48.dp)
                                    .testTag("btn_playlist_add_songs")
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Add Songs",
                                        tint = SoftWhite,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Add",
                                        color = SoftWhite,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    }
                }

                if (livePlaylist.songs.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 32.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(Graphite)
                                .border(0.75.dp, WhiteHighlightSoft, RoundedCornerShape(18.dp))
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "No tracks in this collection",
                                color = SoftWhite,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (isUserOwned) "Tap Add Songs to search and build this playlist." else "Play or favorite songs to populate this list.",
                                color = Silver,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center
                            )
                            if (isUserOwned) {
                                Spacer(modifier = Modifier.height(14.dp))
                                Button(
                                    onClick = { showAddSongsSheet = true },
                                    shape = RoundedCornerShape(50),
                                    colors = ButtonDefaults.buttonColors(containerColor = SoftWhite, contentColor = MidnightBlack),
                                    modifier = Modifier.height(48.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Add Songs", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                } else {
                    itemsIndexed(
                        items = livePlaylist.songs,
                        key = { index, song -> "pl_${index}_${song.id}" },
                        contentType = { _, _ -> "playlist_song_row" }
                    ) { index, song ->
                        val isCurrent = currentSong?.id == song.id
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 16.dp, end = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = String.format(Locale.US, "%02d", index + 1),
                                    fontFamily = MonoFontFamily,
                                    color = if (isCurrent) SoftWhite else MutedGrey,
                                    fontSize = 13.sp,
                                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                                    modifier = Modifier.width(28.dp)
                                )

                                Box(modifier = Modifier.weight(1f)) {
                                    SongRowItem(
                                        song = song,
                                        isCurrent = isCurrent,
                                        isPlaying = isPlaying && isCurrent,
                                        onClick = { onSongSelected(song, livePlaylist.songs) },
                                        onFavoriteToggle = { onFavoriteToggle(song) },
                                        onDownloadClick = { onDownloadClick(song) },
                                        onPlayNext = onPlayNext?.let { cb -> { cb(song) } },
                                        onAddToQueue = onAddToQueue?.let { cb -> { cb(song) } },
                                        onStartRadio = onStartRadio?.let { cb -> { cb(song) } },
                                        onRemoveFromPlaylist = if (isUserOwned && repository != null) {
                                            {
                                                scope.launch {
                                                    repository.removeSongFromPlaylist(livePlaylist.id, song.id)
                                                }
                                            }
                                        } else null
                                    )
                                }
                            }
                            if (index < livePlaylist.songs.lastIndex) {
                                HorizontalDivider(
                                    color = WhiteHighlightSoft,
                                    thickness = 0.5.dp,
                                    modifier = Modifier.padding(start = 94.dp, end = 16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddSongsSheet && repository != null) {
        var addQuery by remember { mutableStateOf("") }
        var searchMatches by remember { mutableStateOf<List<Song>>(emptyList()) }
        var isSearching by remember { mutableStateOf(false) }

        val suggestions = remember(favorites, topCharts) {
            (favorites + topCharts).distinctBy { it.id }.take(20)
        }

        LaunchedEffect(addQuery) {
            if (addQuery.trim().length >= 2) {
                isSearching = true
                kotlinx.coroutines.delay(320)
                searchMatches = repository.searchSongs(addQuery.trim())
                isSearching = false
            } else {
                searchMatches = emptyList()
                isSearching = false
            }
        }

        val displayCandidates = if (addQuery.trim().length >= 2) searchMatches else suggestions
        val existingIds = remember(livePlaylist.songs) { livePlaylist.songs.map { it.id }.toSet() }

        ModalBottomSheet(
            onDismissRequest = { showAddSongsSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = DarkGlassOpaque,
            contentColor = SoftWhite
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 380.dp, max = 600.dp)
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 24.dp)
            ) {
                Text(
                    text = "Add Songs to ${livePlaylist.title}",
                    color = SoftWhite,
                    fontFamily = DisplayFontFamily,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(12.dp))

                LiquidGlassSurface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    elevation = 6.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Search, contentDescription = null, tint = Silver, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Box(modifier = Modifier.weight(1f)) {
                            if (addQuery.isEmpty()) {
                                Text("Search any track or artist to add", color = MutedGrey, fontSize = 13.sp)
                            }
                            BasicTextField(
                                value = addQuery,
                                onValueChange = { addQuery = it },
                                singleLine = true,
                                textStyle = TextStyle(color = SoftWhite, fontSize = 14.sp),
                                cursorBrush = SolidColor(SoftWhite),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (isSearching) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = SoftWhite, modifier = Modifier.size(26.dp))
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxWidth()) {
                        itemsIndexed(displayCandidates, key = { idx, s -> "add_${idx}_${s.id}" }) { _, candidate ->
                            val alreadyIn = existingIds.contains(candidate.id)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable(enabled = !alreadyIn) {
                                        scope.launch {
                                            repository.addSongToPlaylist(livePlaylist.id, candidate)
                                        }
                                    }
                                    .padding(vertical = 8.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(GraphiteElevated),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (candidate.coverUrl.isNotBlank()) {
                                        AsyncImage(
                                            model = candidate.coverUrl,
                                            contentDescription = candidate.title,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Icon(Icons.Default.MusicNote, contentDescription = null, tint = MutedGrey)
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(candidate.title, color = SoftWhite, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(candidate.artist, color = Silver, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                                Box(
                                    modifier = Modifier.size(44.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (alreadyIn) Icons.Default.Check else Icons.Default.Add,
                                        contentDescription = if (alreadyIn) "Added" else "Add to Playlist",
                                        tint = if (alreadyIn) MutedGrey else SoftWhite
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
