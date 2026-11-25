package com.example.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.example.auth.AuthManager
import com.example.data.model.Playlist
import com.example.data.model.Song
import com.example.data.repository.PlaylistImportStatus
import com.example.ui.components.AiCustomizerSheet
import com.example.ui.components.AppUpdateDialog
import com.example.ui.components.FullPlayerSheet
import com.example.ui.components.LiquidGlassSurface
import com.example.ui.components.MiniPlayer
import com.example.ui.components.SpotifyImportSheet
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
import com.example.ui.theme.WhiteHighlightSoft
import com.example.updater.AppUpdateManager
import kotlinx.coroutines.launch

enum class MainTab(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    HOME("Home", Icons.Filled.Home, Icons.Outlined.Home),
    SEARCH("Explore", Icons.Filled.Explore, Icons.Outlined.Explore),
    LIBRARY("Library", Icons.Filled.LibraryMusic, Icons.Outlined.LibraryMusic)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val context = LocalContext.current
    val app = context.applicationContext as? SpringApplication
    val playerManager = app?.playerManager
    val repository = app?.repository
    val firestoreSyncManager = app?.firestoreSyncManager
    val scope = rememberCoroutineScope()

    var currentTab by remember { mutableStateOf(MainTab.HOME) }
    var searchInitialQuery by remember { mutableStateOf("") }
    var searchTriggerNonce by remember { mutableIntStateOf(0) }
    var showFullPlayer by remember { mutableStateOf(false) }
    var showImportSheet by remember { mutableStateOf(false) }
    var showAiCustomizer by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var selectedPlaylist by remember { mutableStateOf<Playlist?>(null) }
    var songPendingPlaylistAdd by remember { mutableStateOf<Song?>(null) }

    val currentSong by playerManager?.currentSong?.collectAsState() ?: remember { mutableStateOf(null) }
    val isPlaying by playerManager?.isPlaying?.collectAsState() ?: remember { mutableStateOf(false) }
    val isShuffle by playerManager?.isShuffle?.collectAsState() ?: remember { mutableStateOf(false) }
    val repeatMode by playerManager?.repeatMode?.collectAsState() ?: remember { mutableStateOf(0) }
    val queue by playerManager?.queue?.collectAsState() ?: remember { mutableStateOf(emptyList()) }
    val audioQuality by playerManager?.audioQuality?.collectAsState() ?: remember { mutableStateOf("320kbps") }
    val sleepTimerMinutes by playerManager?.sleepTimerMinutes?.collectAsState() ?: remember { mutableStateOf(null) }
    val currentLyrics by playerManager?.currentLyrics?.collectAsState() ?: remember { mutableStateOf(null) }
    val isLoadingLyrics by playerManager?.isLoadingLyrics?.collectAsState() ?: remember { mutableStateOf(false) }
    val autoplayEnabled by playerManager?.autoplayEnabled?.collectAsState() ?: remember { mutableStateOf(true) }
    val autoplayStatusLabel by playerManager?.autoplayStatusLabel?.collectAsState() ?: remember { mutableStateOf<String?>(null) }
    val updateAvailable by AppUpdateManager.updateAvailable.collectAsState()
    val importStatus by repository?.importStatus?.collectAsState() ?: remember { mutableStateOf(PlaylistImportStatus()) }
    val similarSongs by repository?.similarSongsState?.collectAsState() ?: remember { mutableStateOf(emptyList()) }
    val userPlaylists by repository?.getUserPlaylists()?.collectAsState(initial = emptyList()) ?: remember { mutableStateOf(emptyList()) }

    LaunchedEffect(Unit) {
        AuthManager.attemptAutoSignIn(context, scope)
        AppUpdateManager.init(context)
        AppUpdateManager.checkForUpdates(context)
    }

    fun handleFavoriteToggle(song: Song) {
        scope.launch {
            repository?.toggleFavorite(song)
        }
    }

    fun handleDownloadClick(song: Song) {
        scope.launch {
            if (song.isDownloaded) {
                repository?.deleteDownload(song)
            } else {
                repository?.downloadSong(song)
            }
        }
    }

    val theme = LocalAppCustomTheme.current

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(theme.canvasBackground)
    ) {
        val useNavigationRail = maxWidth >= 600.dp

        Row(modifier = Modifier.fillMaxSize()) {
            if (useNavigationRail) {
                NavigationRail(
                    containerColor = GraphiteElevated,
                    modifier = Modifier
                        .fillMaxHeight()
                        .statusBarsPadding()
                        .navigationBarsPadding()
                        .testTag("spotify_floating_dock")
                ) {
                    Spacer(modifier = Modifier.height(16.dp))
                    MainTab.entries.forEach { tab ->
                        val isSelected = currentTab == tab && selectedPlaylist == null && !showSettings
                        NavigationRailItem(
                            selected = isSelected,
                            onClick = {
                                selectedPlaylist = null
                                showSettings = false
                                currentTab = tab
                            },
                            icon = {
                                Icon(
                                    imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                    contentDescription = tab.title
                                )
                            },
                            label = {
                                Text(
                                    text = tab.title,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
                                )
                            },
                            colors = NavigationRailItemDefaults.colors(
                                selectedIconColor = SoftWhite,
                                selectedTextColor = SoftWhite,
                                indicatorColor = WhiteHighlightSoft,
                                unselectedIconColor = MutedGrey,
                                unselectedTextColor = MutedGrey
                            ),
                            modifier = Modifier.testTag("nav_${tab.name.lowercase()}")
                        )
                    }
                }
                VerticalDivider(color = WhiteHighlightSoft, thickness = 0.6.dp)
            }

            Scaffold(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                containerColor = theme.canvasBackground,
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                bottomBar = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        MidnightBlack.copy(alpha = 0.72f),
                                        MidnightBlack.copy(alpha = 0.94f)
                                    )
                                )
                            )
                    ) {
                        // Background Playlist Import Status Strip
                        if (importStatus.isImporting || importStatus.completedPlaylist != null || importStatus.errorMessage != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = if (useNavigationRail) 24.dp else 14.dp, vertical = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                LiquidGlassSurface(
                                    modifier = Modifier
                                        .widthIn(max = 680.dp)
                                        .fillMaxWidth()
                                        .clickable {
                                            if (importStatus.completedPlaylist != null) {
                                                selectedPlaylist = importStatus.completedPlaylist
                                                repository?.dismissImportStatus()
                                            } else if (importStatus.isImporting) {
                                                showImportSheet = true
                                            }
                                        }
                                        .testTag("banner_bg_import"),
                                    shape = RoundedCornerShape(16.dp),
                                    elevation = 10.dp
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(start = 14.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            if (importStatus.isImporting) {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(18.dp),
                                                    color = SoftWhite,
                                                    strokeWidth = 2.dp
                                                )
                                            } else if (importStatus.completedPlaylist != null) {
                                                Icon(
                                                    imageVector = Icons.Default.CheckCircle,
                                                    contentDescription = null,
                                                    tint = SoftWhite,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            } else {
                                                Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = null,
                                                    tint = DestructiveRose,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }

                                            Spacer(modifier = Modifier.width(10.dp))

                                            Column {
                                                val title = when {
                                                    importStatus.isImporting -> {
                                                        if (importStatus.totalTracks > 0) {
                                                            "Importing (${importStatus.currentTrack}/${importStatus.totalTracks}): ${importStatus.playlistName}"
                                                        } else {
                                                            "Importing: ${importStatus.playlistName}"
                                                        }
                                                    }
                                                    importStatus.completedPlaylist != null -> "'${importStatus.completedPlaylist?.title}' added to Library"
                                                    else -> "Import error: ${importStatus.errorMessage}"
                                                }
                                                Text(
                                                    text = title,
                                                    color = SoftWhite,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                val sub = when {
                                                    importStatus.isImporting -> if (importStatus.trackTitle.isNotBlank()) importStatus.trackTitle else "Processing in background..."
                                                    importStatus.completedPlaylist != null -> "Tap to open playlist"
                                                    else -> "Tap to dismiss"
                                                }
                                                Text(
                                                    text = sub,
                                                    color = Silver,
                                                    fontSize = 11.sp,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }

                                        IconButton(
                                            onClick = { repository?.dismissImportStatus() },
                                            modifier = Modifier.size(48.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Dismiss",
                                                tint = Silver,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Floating Liquid Glass Mini-Player
                        if (currentSong != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = if (useNavigationRail) 24.dp else 0.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(modifier = Modifier.widthIn(max = 680.dp)) {
                                    MiniPlayer(
                                        song = currentSong!!,
                                        isPlaying = isPlaying,
                                        positionFlow = playerManager?.currentPositionMs,
                                        durationFlow = playerManager?.durationMs,
                                        onPlayPause = { playerManager?.togglePlayPause() },
                                        onSkipPrevious = { playerManager?.skipPrevious() },
                                        onSkipNext = { playerManager?.skipNext() },
                                        onClick = { showFullPlayer = true }
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        if (!useNavigationRail) {
                            // Floating Liquid Glass Bottom Navigation Dock
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .navigationBarsPadding()
                                    .padding(horizontal = 14.dp)
                                    .padding(bottom = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                LiquidGlassSurface(
                                    modifier = Modifier
                                        .widthIn(max = 680.dp)
                                        .fillMaxWidth()
                                        .testTag("spotify_floating_dock"),
                                    shape = RoundedCornerShape(26.dp),
                                    elevation = 18.dp
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 8.dp, vertical = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceAround,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        MainTab.entries.forEach { tab ->
                                            val isSelected = currentTab == tab && selectedPlaylist == null && !showSettings
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.Center,
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .heightIn(min = 50.dp)
                                                    .clip(RoundedCornerShape(18.dp))
                                                    .background(
                                                        if (isSelected) WhiteHighlightSoft else Color.Transparent
                                                    )
                                                    .clickable {
                                                        selectedPlaylist = null
                                                        showSettings = false
                                                        currentTab = tab
                                                    }
                                                    .padding(vertical = 6.dp)
                                                    .testTag("nav_${tab.name.lowercase()}")
                                            ) {
                                                Icon(
                                                    imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                                    contentDescription = tab.title,
                                                    tint = if (isSelected) SoftWhite else MutedGrey,
                                                    modifier = Modifier.size(21.dp)
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = tab.title,
                                                    fontSize = 11.sp,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                    color = if (isSelected) SoftWhite else MutedGrey,
                                                    maxLines = 1
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            Spacer(modifier = Modifier.navigationBarsPadding().height(6.dp))
                        }
                    }
                }
            ) { innerPadding ->
                val saveableStateHolder = rememberSaveableStateHolder()
                val activeDestinationKey: Any = when {
                    showSettings -> "settings"
                    selectedPlaylist != null -> "playlist_${selectedPlaylist!!.id}"
                    else -> currentTab
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = innerPadding.calculateTopPadding())
                ) {
                    AnimatedContent(
                        targetState = activeDestinationKey,
                        transitionSpec = {
                            fadeIn(animationSpec = tween(180))
                                .togetherWith(fadeOut(animationSpec = tween(140)))
                        },
                        label = "main_tab_transition"
                    ) { destKey ->
                        saveableStateHolder.SaveableStateProvider(destKey) {
                            when {
                                destKey == "settings" -> {
                                    SettingsScreen(
                                        currentQuality = audioQuality,
                                        onQualitySelected = { playerManager?.setAudioQuality(it) },
                                        sleepTimerMinutes = sleepTimerMinutes,
                                        onSetSleepTimer = { playerManager?.setSleepTimer(it) },
                                        onBack = { showSettings = false },
                                        onOpenAiCustomizer = { showAiCustomizer = true }
                                    )
                                }
                                destKey is String && destKey.startsWith("playlist_") && selectedPlaylist != null -> {
                                    PlaylistDetailScreen(
                                        playlist = selectedPlaylist!!,
                                        currentSong = currentSong,
                                        isPlaying = isPlaying,
                                        onSongSelected = { song, list -> playerManager?.playSong(song, list) },
                                        onFavoriteToggle = { handleFavoriteToggle(it) },
                                        onDownloadClick = { handleDownloadClick(it) },
                                        onBack = { selectedPlaylist = null },
                                        onPlaylistDeleted = { selectedPlaylist = null },
                                        onPlayNext = { playerManager?.playNext(it) },
                                        onAddToQueue = { playerManager?.addToQueue(it) },
                                        onStartRadio = { playerManager?.startSongRadio(it) }
                                    )
                                }
                                destKey == MainTab.HOME -> {
                                    HomeScreen(
                                        currentSong = currentSong,
                                        isPlaying = isPlaying,
                                        onSongSelected = { song, list -> playerManager?.playSong(song, list) },
                                        onTogglePlayPause = { playerManager?.togglePlayPause() },
                                        onFavoriteToggle = { handleFavoriteToggle(it) },
                                        onDownloadClick = { handleDownloadClick(it) },
                                        onMoodClick = { mood ->
                                            searchInitialQuery = mood.searchTag
                                            searchTriggerNonce++
                                            currentTab = MainTab.SEARCH
                                        },
                                        onPlaylistClick = { pl -> selectedPlaylist = pl },
                                        onOpenImport = { showImportSheet = true },
                                        onOpenSettings = { showSettings = true },
                                        onOpenAiCustomizer = { showAiCustomizer = true },
                                        onStartRadio = { playerManager?.startSongRadio(it) },
                                        onOpenSearch = { currentTab = MainTab.SEARCH },
                                        onPlayNext = { playerManager?.playNext(it) },
                                        onAddToQueue = { playerManager?.addToQueue(it) },
                                        onAddToPlaylist = { songPendingPlaylistAdd = it }
                                    )
                                }
                                destKey == MainTab.SEARCH -> {
                                    ExploreScreen(
                                        currentSong = currentSong,
                                        isPlaying = isPlaying,
                                        onSongSelected = { song, list -> playerManager?.playSong(song, list) },
                                        onFavoriteToggle = { handleFavoriteToggle(it) },
                                        onDownloadClick = { handleDownloadClick(it) },
                                        initialQuery = searchInitialQuery,
                                        searchTrigger = searchTriggerNonce,
                                        onOpenSettings = { showSettings = true },
                                        onPlaylistClick = { pl -> selectedPlaylist = pl },
                                        onPlayNext = { playerManager?.playNext(it) },
                                        onAddToQueue = { playerManager?.addToQueue(it) },
                                        onAddToPlaylist = { songPendingPlaylistAdd = it },
                                        onStartRadio = { playerManager?.startSongRadio(it) }
                                    )
                                }
                                else -> {
                                    LibraryScreen(
                                        currentSong = currentSong,
                                        isPlaying = isPlaying,
                                        onSongSelected = { song, list -> playerManager?.playSong(song, list) },
                                        onFavoriteToggle = { handleFavoriteToggle(it) },
                                        onDownloadClick = { handleDownloadClick(it) },
                                        onPlaylistClick = { pl -> selectedPlaylist = pl },
                                        onOpenImport = { showImportSheet = true },
                                        onOpenSettings = { showSettings = true },
                                        onPlayNext = { playerManager?.playNext(it) },
                                        onAddToQueue = { playerManager?.addToQueue(it) },
                                        onAddToPlaylist = { songPendingPlaylistAdd = it },
                                        onStartRadio = { playerManager?.startSongRadio(it) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = showFullPlayer && currentSong != null,
            enter = slideInVertically(
                animationSpec = spring(dampingRatio = 0.85f, stiffness = 420f),
                initialOffsetY = { it }
            ) + fadeIn(animationSpec = tween(180)),
            exit = slideOutVertically(
                animationSpec = spring(dampingRatio = 0.88f, stiffness = 460f),
                targetOffsetY = { it }
            ) + fadeOut(animationSpec = tween(160))
        ) {
            if (currentSong != null) {
                FullPlayerSheet(
                    song = currentSong!!,
                    isPlaying = isPlaying,
                    positionFlow = playerManager?.currentPositionMs,
                    durationFlow = playerManager?.durationMs,
                    isShuffle = isShuffle,
                    repeatMode = repeatMode,
                    audioQuality = audioQuality,
                    queue = queue,
                    lyrics = currentLyrics,
                    isLoadingLyrics = isLoadingLyrics,
                    similarSongs = similarSongs,
                    onSelectSimilarSong = { sel -> playerManager?.playSong(sel, listOf(sel) + similarSongs) },
                    onFeedback = { song, fbType ->
                        scope.launch {
                            repository?.recordSongFeedback(song, fbType)
                        }
                    },
                    onRetryLyrics = { currentSong?.let { playerManager?.loadLyricsForSong(it) } },
                    onPlayPause = { playerManager?.togglePlayPause() },
                    onSeekTo = { playerManager?.seekTo(it) },
                    onSkipNext = { playerManager?.skipNext() },
                    onSkipPrevious = { playerManager?.skipPrevious() },
                    onToggleShuffle = { playerManager?.toggleShuffle() },
                    onToggleRepeat = { playerManager?.toggleRepeatMode() },
                    onToggleFavorite = { handleFavoriteToggle(currentSong!!) },
                    onDownloadClick = { handleDownloadClick(currentSong!!) },
                    onSelectQuality = { playerManager?.setAudioQuality(it) },
                    onDismiss = { showFullPlayer = false },
                    onPlayQueueSong = { playerManager?.playSong(it, queue) },
                    autoplayEnabled = autoplayEnabled,
                    autoplayStatusLabel = autoplayStatusLabel,
                    onToggleAutoplay = { playerManager?.setAutoplayEnabled(it) },
                    onStartRadio = { playerManager?.startSongRadio(it) },
                    onMoveQueueItem = { from, to -> playerManager?.moveQueueItem(from, to) },
                    onRemoveQueueItem = { index -> playerManager?.removeFromQueue(index) },
                    onClearUpcomingQueue = { playerManager?.clearUpcomingQueue() },
                    onPlayNext = { playerManager?.playNext(it) },
                    onAddToQueue = { playerManager?.addToQueue(it) }
                )
            }
        }

        if (showImportSheet && repository != null) {
            SpotifyImportSheet(
                repository = repository,
                onPlaylistImported = { importedPlaylist ->
                    selectedPlaylist = importedPlaylist
                },
                onDismiss = { showImportSheet = false }
            )
        }

        if (showAiCustomizer) {
            AiCustomizerSheet(
                firestoreSyncManager = firestoreSyncManager,
                onDismiss = { showAiCustomizer = false }
            )
        }

        if (songPendingPlaylistAdd != null && repository != null) {
            val targetSong = songPendingPlaylistAdd!!
            ModalBottomSheet(
                onDismissRequest = { songPendingPlaylistAdd = null },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                containerColor = DarkGlassOpaque,
                contentColor = SoftWhite
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 24.dp)
                ) {
                    Text(
                        text = "Add to Playlist",
                        color = SoftWhite,
                        fontFamily = DisplayFontFamily,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${targetSong.title} • ${targetSong.artist}",
                        color = Silver,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    LiquidGlassSurface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                scope.launch {
                                    val created = repository.createCustomPlaylist(targetSong.title.take(28))
                                    repository.addSongToPlaylist(created.id, targetSong)
                                    songPendingPlaylistAdd = null
                                }
                            },
                        shape = RoundedCornerShape(14.dp),
                        elevation = 6.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = SoftWhite, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Create New Playlist with Song", color = SoftWhite, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 320.dp)
                    ) {
                        items(userPlaylists, key = { it.id }) { pl ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        scope.launch {
                                            repository.addSongToPlaylist(pl.id, targetSong)
                                            songPendingPlaylistAdd = null
                                        }
                                    }
                                    .padding(vertical = 8.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Graphite),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (pl.coverUrl.isNotBlank()) {
                                        AsyncImage(
                                            model = pl.coverUrl,
                                            contentDescription = pl.title,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Icon(Icons.Default.MusicNote, contentDescription = null, tint = MutedGrey)
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(pl.title, color = SoftWhite, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text("${pl.songs.size} tracks", color = Silver, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        if (updateAvailable != null) {
            AppUpdateDialog(
                updateInfo = updateAvailable!!,
                onDismiss = { AppUpdateManager.dismissUpdate(context, updateAvailable?.versionCode) }
            )
        }
    }
}
