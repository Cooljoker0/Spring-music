package com.example.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.SpringApplication
import com.example.auth.AuthManager
import com.example.data.model.Playlist
import com.example.data.model.Song
import com.example.data.repository.PlaylistImportStatus
import com.example.ui.components.AiCustomizerSheet
import com.example.ui.components.AppUpdateDialog
import com.example.ui.components.FullPlayerSheet
import com.example.ui.components.MiniPlayer
import com.example.ui.components.SpotifyImportSheet
import com.example.ui.components.bouncyClick
import com.example.ui.theme.AuroraCyan
import com.example.ui.theme.AuroraMagenta
import com.example.ui.theme.AuroraViolet
import com.example.ui.theme.CrispWhite
import com.example.ui.theme.FrostedGlassBorder
import com.example.ui.theme.FrostedGlassSurface
import com.example.ui.theme.FusionAmbience
import com.example.ui.theme.IosRed
import com.example.ui.theme.MidnightCanvas
import com.example.ui.theme.SlateSecondary
import com.example.updater.AppUpdateManager
import kotlinx.coroutines.launch

enum class MainTab(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    HOME("Home", Icons.Filled.Home, Icons.Outlined.Home),
    SEARCH("Explore", Icons.Filled.Explore, Icons.Outlined.Explore),
    LIBRARY("Library", Icons.Filled.LibraryMusic, Icons.Outlined.LibraryMusic),
    SETTINGS("Settings", Icons.Filled.Settings, Icons.Outlined.Settings)
}

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
    var showFullPlayer by remember { mutableStateOf(false) }
    var showImportSheet by remember { mutableStateOf(false) }
    var showAiCustomizer by remember { mutableStateOf(false) }
    var selectedPlaylist by remember { mutableStateOf<Playlist?>(null) }

    val currentSong by playerManager?.currentSong?.collectAsState() ?: remember { mutableStateOf(null) }
    val isPlaying by playerManager?.isPlaying?.collectAsState() ?: remember { mutableStateOf(false) }
    val isShuffle by playerManager?.isShuffle?.collectAsState() ?: remember { mutableStateOf(false) }
    val repeatMode by playerManager?.repeatMode?.collectAsState() ?: remember { mutableStateOf(0) }
    val queue by playerManager?.queue?.collectAsState() ?: remember { mutableStateOf(emptyList()) }
    val audioQuality by playerManager?.audioQuality?.collectAsState() ?: remember { mutableStateOf("320kbps") }
    val sleepTimerMinutes by playerManager?.sleepTimerMinutes?.collectAsState() ?: remember { mutableStateOf(null) }
    val currentLyrics by playerManager?.currentLyrics?.collectAsState() ?: remember { mutableStateOf(null) }
    val isLoadingLyrics by playerManager?.isLoadingLyrics?.collectAsState() ?: remember { mutableStateOf(false) }
    val updateAvailable by AppUpdateManager.updateAvailable.collectAsState()
    val importStatus by repository?.importStatus?.collectAsState() ?: remember { mutableStateOf(PlaylistImportStatus()) }
    val similarSongs by repository?.similarSongsState?.collectAsState() ?: remember { mutableStateOf(emptyList()) }

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

    Box(modifier = Modifier.fillMaxSize().background(MidnightCanvas)) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MidnightCanvas,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color(0x9908090F),
                                    Color(0xE608090F)
                                )
                            )
                        )
                ) {
                    // Background Playlist Import Status Pill
                    if (importStatus.isImporting || importStatus.completedPlaylist != null || importStatus.errorMessage != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 4.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(FrostedGlassSurface)
                                .border(1.dp, AuroraCyan.copy(alpha = 0.5f), RoundedCornerShape(18.dp))
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
                                            color = AuroraCyan,
                                            strokeWidth = 2.dp
                                        )
                                    } else if (importStatus.completedPlaylist != null) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = AuroraCyan,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = null,
                                            tint = IosRed,
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
                                            color = CrispWhite,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
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
                                            color = SlateSecondary,
                                            fontSize = 11.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
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
                                        tint = SlateSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Floating Mini-Player Pill pinned 8dp above the 4-Tab Bar
                    if (currentSong != null) {
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
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // 4-Tab Translucent Blurred iOS Bottom Bar (Home, Explore, Library, Settings)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 14.dp, end = 14.dp, bottom = 10.dp)
                            .navigationBarsPadding()
                            .shadow(
                                elevation = 24.dp,
                                shape = RoundedCornerShape(30.dp),
                                ambientColor = AuroraViolet.copy(alpha = 0.25f),
                                spotColor = Color.Black.copy(alpha = 0.9f)
                            )
                            .clip(RoundedCornerShape(30.dp))
                            .background(FusionAmbience.RichDockBrush)
                            .border(
                                width = 1.dp,
                                brush = FusionAmbience.GoldRimBorderBrush,
                                shape = RoundedCornerShape(30.dp)
                            )
                            .testTag("spotify_floating_dock")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 6.dp, vertical = 7.dp),
                            horizontalArrangement = Arrangement.SpaceAround,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            MainTab.entries.forEach { tab ->
                                val isSelected = currentTab == tab && selectedPlaylist == null
                                val activeGlow = when (tab) {
                                    MainTab.HOME -> AuroraCyan
                                    MainTab.SEARCH -> AuroraViolet
                                    MainTab.LIBRARY -> AuroraMagenta
                                    MainTab.SETTINGS -> AuroraCyan
                                }
                                val iconScale by animateFloatAsState(
                                    targetValue = if (isSelected) 1.12f else 1.0f,
                                    animationSpec = spring(dampingRatio = 0.65f, stiffness = 480f),
                                    label = "tab_icon_scale"
                                )
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(20.dp))
                                        .bouncyClick(pressedScale = 0.92f) {
                                            selectedPlaylist = null
                                            currentTab = tab
                                        }
                                        .background(
                                            if (isSelected) activeGlow.copy(alpha = 0.16f) else Color.Transparent
                                        )
                                        .padding(horizontal = 14.dp, vertical = 6.dp)
                                        .testTag("nav_${tab.name.lowercase()}")
                                ) {
                                    Icon(
                                        imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.title,
                                        tint = if (isSelected) activeGlow else SlateSecondary,
                                        modifier = Modifier
                                            .size(23.dp)
                                            .graphicsLayer {
                                                scaleX = iconScale
                                                scaleY = iconScale
                                            }
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = tab.title,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) activeGlow else SlateSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        ) { innerPadding ->
            val saveableStateHolder = rememberSaveableStateHolder()
            val activeDestinationKey: Any = selectedPlaylist?.let { "playlist_${it.id}" } ?: currentTab

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = innerPadding.calculateTopPadding())
            ) {
                AnimatedContent(
                    targetState = activeDestinationKey,
                    transitionSpec = {
                        (fadeIn(animationSpec = tween(180)) +
                            scaleIn(initialScale = 0.985f, animationSpec = tween(180)))
                            .togetherWith(fadeOut(animationSpec = tween(140)))
                    },
                    label = "main_tab_transition"
                ) { destKey ->
                    saveableStateHolder.SaveableStateProvider(destKey) {
                        when {
                            destKey is String && destKey.startsWith("playlist_") && selectedPlaylist != null -> {
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
                            destKey == MainTab.HOME -> {
                                HomeScreen(
                                    currentSong = currentSong,
                                    isPlaying = isPlaying,
                                    onSongSelected = { song, list -> playerManager?.playSong(song, list) },
                                    onFavoriteToggle = { handleFavoriteToggle(it) },
                                    onDownloadClick = { handleDownloadClick(it) },
                                    onMoodClick = { mood ->
                                        searchInitialQuery = mood.searchTag
                                        currentTab = MainTab.SEARCH
                                    },
                                    onPlaylistClick = { pl -> selectedPlaylist = pl },
                                    onOpenImport = { showImportSheet = true },
                                    onOpenSettings = { currentTab = MainTab.SETTINGS },
                                    onOpenAiCustomizer = { showAiCustomizer = true }
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
                                    onOpenSettings = { currentTab = MainTab.SETTINGS }
                                )
                            }
                            destKey == MainTab.LIBRARY -> {
                                LibraryScreen(
                                    currentSong = currentSong,
                                    isPlaying = isPlaying,
                                    onSongSelected = { song, list -> playerManager?.playSong(song, list) },
                                    onFavoriteToggle = { handleFavoriteToggle(it) },
                                    onDownloadClick = { handleDownloadClick(it) },
                                    onPlaylistClick = { pl -> selectedPlaylist = pl },
                                    onOpenImport = { showImportSheet = true },
                                    onOpenSettings = { currentTab = MainTab.SETTINGS }
                                )
                            }
                            else -> {
                                SettingsScreen(
                                    currentQuality = audioQuality,
                                    onQualitySelected = { playerManager?.setAudioQuality(it) },
                                    sleepTimerMinutes = sleepTimerMinutes,
                                    onSetSleepTimer = { playerManager?.setSleepTimer(it) },
                                    onBack = null,
                                    onOpenAiCustomizer = { showAiCustomizer = true }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Smooth Spring Transition (dampingRatio = 0.8f ~ iOS response 0.35 / damping 0.8) expanding Mini-Player into Full-Screen Player
        AnimatedVisibility(
            visible = showFullPlayer && currentSong != null,
            enter = slideInVertically(
                animationSpec = spring(dampingRatio = 0.8f, stiffness = 420f),
                initialOffsetY = { it }
            ) + fadeIn(animationSpec = spring(stiffness = 500f)),
            exit = slideOutVertically(
                animationSpec = spring(dampingRatio = 0.85f, stiffness = 460f),
                targetOffsetY = { it }
            ) + fadeOut(animationSpec = spring(stiffness = 600f))
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
                    onPlayQueueSong = { playerManager?.playSong(it, queue) }
                )
            }
        }

        // Spotify Playlist Importer Sheet
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
}
