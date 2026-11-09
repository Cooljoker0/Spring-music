package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import com.example.data.repository.PlaylistImportStatus
import com.example.ui.theme.SpotifyCardElevated
import com.example.ui.theme.SpotifyGreen
import com.example.ui.theme.SpotifyTextPrimary
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.SpringApplication
import com.example.auth.AuthManager
import com.example.data.model.Playlist
import com.example.data.model.Song
import com.example.ui.components.AiCustomizerSheet
import com.example.ui.components.FullPlayerSheet
import com.example.ui.components.MiniPlayer
import com.example.ui.components.AppUpdateDialog
import com.example.ui.components.SpotifyImportSheet
import com.example.ui.theme.AiThemeManager
import com.example.ui.theme.SpotifyPitchBlack
import com.example.ui.theme.SpotifyTextSecondary
import com.example.updater.AppUpdateManager
import kotlinx.coroutines.launch

enum class MainTab(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    HOME("Home", Icons.Filled.Home, Icons.Outlined.Home),
    SEARCH("Search", Icons.Filled.Search, Icons.Outlined.Search),
    LIBRARY("Your Library", Icons.Filled.LibraryMusic, Icons.Outlined.LibraryMusic)
}

@Composable
fun MainScreen() {
    val context = LocalContext.current
    val app = context.applicationContext as? SpringApplication
    val playerManager = app?.playerManager
    val repository = app?.repository
    val firestoreSyncManager = app?.firestoreSyncManager
    val scope = rememberCoroutineScope()
    val primaryColor = MaterialTheme.colorScheme.primary
    val currentTheme by AiThemeManager.currentTheme.collectAsState()

    var currentTab by remember { mutableStateOf(MainTab.HOME) }
    var searchInitialQuery by remember { mutableStateOf("") }
    var showFullPlayer by remember { mutableStateOf(false) }
    var showImportSheet by remember { mutableStateOf(false) }
    var showAiCustomizer by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var selectedPlaylist by remember { mutableStateOf<Playlist?>(null) }

    val currentSong by playerManager?.currentSong?.collectAsState() ?: remember { mutableStateOf(null) }
    val isPlaying by playerManager?.isPlaying?.collectAsState() ?: remember { mutableStateOf(false) }
    val currentPositionMs by playerManager?.currentPositionMs?.collectAsState() ?: remember { mutableStateOf(0L) }
    val durationMs by playerManager?.durationMs?.collectAsState() ?: remember { mutableStateOf(0L) }
    val isShuffle by playerManager?.isShuffle?.collectAsState() ?: remember { mutableStateOf(false) }
    val repeatMode by playerManager?.repeatMode?.collectAsState() ?: remember { mutableStateOf(0) }
    val queue by playerManager?.queue?.collectAsState() ?: remember { mutableStateOf(emptyList()) }
    val audioQuality by playerManager?.audioQuality?.collectAsState() ?: remember { mutableStateOf("320kbps") }
    val sleepTimerMinutes by playerManager?.sleepTimerMinutes?.collectAsState() ?: remember { mutableStateOf(null) }
    val currentLyrics by playerManager?.currentLyrics?.collectAsState() ?: remember { mutableStateOf(null) }
    val isLoadingLyrics by playerManager?.isLoadingLyrics?.collectAsState() ?: remember { mutableStateOf(false) }
    val updateAvailable by AppUpdateManager.updateAvailable.collectAsState()
    val importStatus by repository?.importStatus?.collectAsState() ?: remember { mutableStateOf(PlaylistImportStatus()) }

    // Attempt silent auto-sign-in and check for OTA app updates on startup
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

    val pageBackground = if (currentTheme.isGlass) Color(0xFF0F0B18) else com.example.ui.theme.SpotifyBlack

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = pageBackground,
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (!showSettings) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Transparent)
                ) {
                // Background Spotify Playlist Import Banner
                if (importStatus.isImporting || importStatus.completedPlaylist != null || importStatus.errorMessage != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(SpotifyCardElevated)
                            .border(1.dp, primaryColor.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                            .clickable {
                                if (importStatus.completedPlaylist != null) {
                                    selectedPlaylist = importStatus.completedPlaylist
                                    repository?.dismissImportStatus()
                                } else if (importStatus.isImporting) {
                                    showImportSheet = true
                                }
                            }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                            .testTag("banner_bg_import")
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
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
                                        color = SpotifyGreen,
                                        strokeWidth = 2.dp
                                    )
                                } else if (importStatus.completedPlaylist != null) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = SpotifyGreen,
                                        modifier = Modifier.size(20.dp)
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = null,
                                        tint = Color(0xFFFF5555),
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
                                        importStatus.completedPlaylist != null -> "✓ '${importStatus.completedPlaylist?.title}' ready in Library"
                                        else -> "Import error: ${importStatus.errorMessage}"
                                    }
                                    Text(
                                        text = title,
                                        color = SpotifyTextPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )
                                    val sub = when {
                                        importStatus.isImporting -> if (importStatus.trackTitle.isNotBlank()) importStatus.trackTitle else "Processing in background..."
                                        importStatus.completedPlaylist != null -> "Tap to open playlist"
                                        else -> "Tap to dismiss"
                                    }
                                    Text(
                                        text = sub,
                                        color = SpotifyTextSecondary,
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )
                                }
                            }

                            IconButton(
                                onClick = { repository?.dismissImportStatus() },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Dismiss",
                                    tint = SpotifyTextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
                // Spotify Floating Mini Player
                if (currentSong != null) {
                    Box(modifier = Modifier.padding(horizontal = 14.dp, vertical = 2.dp)) {
                        MiniPlayer(
                            song = currentSong!!,
                            isPlaying = isPlaying,
                            currentPositionMs = currentPositionMs,
                            durationMs = durationMs,
                            onPlayPause = { playerManager?.togglePlayPause() },
                            onSkipNext = { playerManager?.skipNext() },
                            onClick = { showFullPlayer = true },
                            onFavoriteToggle = { handleFavoriteToggle(currentSong!!) }
                        )
                    }
                }

                // Floating Frosted Glass / Blur Acrylic Dock
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = 2.dp, bottom = 10.dp)
                        .navigationBarsPadding()
                        .shadow(
                            elevation = 16.dp,
                            shape = RoundedCornerShape(32.dp),
                            ambientColor = Color.Black.copy(alpha = 0.7f),
                            spotColor = Color.Black.copy(alpha = 0.7f)
                        )
                        .clip(RoundedCornerShape(32.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color(0xD9222222),
                                    Color(0xEB121212)
                                )
                            )
                        )
                        .border(
                            width = 1.dp,
                            brush = Brush.verticalGradient(
                                listOf(
                                    Color(0x60FFFFFF),
                                    Color(0x18FFFFFF)
                                )
                            ),
                            shape = RoundedCornerShape(32.dp)
                        )
                        .testTag("spotify_floating_dock")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        MainTab.entries.forEach { tab ->
                            val isSelected = currentTab == tab && !showSettings && selectedPlaylist == null
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .clickable {
                                        showSettings = false
                                        selectedPlaylist = null
                                        currentTab = tab
                                    }
                                    .background(
                                        if (isSelected) primaryColor.copy(alpha = 0.14f) else Color.Transparent
                                    )
                                    .padding(horizontal = 16.dp, vertical = 6.dp)
                                    .testTag("nav_${tab.name.lowercase()}")
                            ) {
                                Icon(
                                    imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                    contentDescription = tab.title,
                                    tint = if (isSelected) primaryColor else SpotifyTextSecondary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = tab.title,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) primaryColor else SpotifyTextSecondary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding())
        ) {
            when {
                showSettings -> {
                    SettingsScreen(
                        currentQuality = audioQuality,
                        onQualitySelected = { playerManager?.setAudioQuality(it) },
                        sleepTimerMinutes = sleepTimerMinutes,
                        onSetSleepTimer = { playerManager?.setSleepTimer(it) },
                        onBack = { showSettings = false },
                        onOpenAiCustomizer = { showAiCustomizer = true }
                    )
                }
                selectedPlaylist != null -> {
                    PlaylistDetailScreen(
                        playlist = selectedPlaylist!!,
                        currentSong = currentSong,
                        isPlaying = isPlaying,
                        onSongSelected = { song, list -> playerManager?.playSong(song, list) },
                        onFavoriteToggle = { handleFavoriteToggle(it) },
                        onDownloadClick = { handleDownloadClick(it) },
                        onBack = { selectedPlaylist = null },
                        onPlaylistDeleted = { selectedPlaylist = null }
                    )
                }
                currentTab == MainTab.HOME -> {
                    HomeScreen(
                        currentSong = currentSong,
                        isPlaying = isPlaying,
                        onSongSelected = { song, list -> playerManager?.playSong(song, list) },
                        onFavoriteToggle = { handleFavoriteToggle(it) },
                        onDownloadClick = { handleDownloadClick(it) },
                        onMoodClick = { mood ->
                            searchInitialQuery = mood.title
                            currentTab = MainTab.SEARCH
                        },
                        onPlaylistClick = { pl ->
                            selectedPlaylist = pl
                        },
                        onOpenImport = { showImportSheet = true },
                        onOpenSettings = { showSettings = true },
                        onOpenAiCustomizer = { showAiCustomizer = true }
                    )
                }
                currentTab == MainTab.SEARCH -> {
                    ExploreScreen(
                        currentSong = currentSong,
                        isPlaying = isPlaying,
                        onSongSelected = { song, list -> playerManager?.playSong(song, list) },
                        onFavoriteToggle = { handleFavoriteToggle(it) },
                        onDownloadClick = { handleDownloadClick(it) },
                        initialQuery = searchInitialQuery
                    )
                }
                currentTab == MainTab.LIBRARY -> {
                    LibraryScreen(
                        currentSong = currentSong,
                        isPlaying = isPlaying,
                        onSongSelected = { song, list -> playerManager?.playSong(song, list) },
                        onFavoriteToggle = { handleFavoriteToggle(it) },
                        onDownloadClick = { handleDownloadClick(it) },
                        onPlaylistClick = { pl ->
                            selectedPlaylist = pl
                        },
                        onOpenImport = { showImportSheet = true },
                        onOpenSettings = { showSettings = true }
                    )
                }
            }
        }
    }

    // Full Screen Now Playing Modal Sheet
    if (showFullPlayer && currentSong != null) {
        FullPlayerSheet(
            song = currentSong!!,
            isPlaying = isPlaying,
            currentPositionMs = currentPositionMs,
            durationMs = durationMs,
            isShuffle = isShuffle,
            repeatMode = repeatMode,
            audioQuality = audioQuality,
            queue = queue,
            lyrics = currentLyrics,
            isLoadingLyrics = isLoadingLyrics,
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
            onPlayQueueSong = { playerManager?.playSong(it, queue) }
        )
    }

    // Spotify Playlist URL Importer Sheet
    if (showImportSheet && repository != null) {
        SpotifyImportSheet(
            repository = repository,
            onPlaylistImported = { importedPlaylist ->
                selectedPlaylist = importedPlaylist
            },
            onDismiss = { showImportSheet = false }
        )
    }

    // AI Theme Studio Sheet
    if (showAiCustomizer) {
        AiCustomizerSheet(
            firestoreSyncManager = firestoreSyncManager,
            onDismiss = { showAiCustomizer = false }
        )
    }

    // Over-The-Air Update Dialog
    if (updateAvailable != null) {
        AppUpdateDialog(
            updateInfo = updateAvailable!!,
            onDismiss = { AppUpdateManager.dismissUpdate(context, updateAvailable?.versionCode) }
        )
    }
}
