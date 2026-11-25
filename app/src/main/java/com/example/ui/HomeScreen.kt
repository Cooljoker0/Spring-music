package com.example.ui

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.QueuePlayNext
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.SpringApplication
import com.example.auth.AuthManager
import com.example.data.model.MoodCategory
import com.example.data.model.Playlist
import com.example.data.model.Song
import com.example.data.network.PresetCatalog
import com.example.ui.components.AnimatedEqualizer
import com.example.ui.components.BrandSpringLogo
import com.example.ui.components.EditorialSectionHeader
import com.example.ui.components.LiquidGlassIconButton
import com.example.ui.components.LiquidGlassPill
import com.example.ui.components.LiquidGlassSurface
import com.example.ui.components.MadeForYouCarousel
import com.example.ui.components.RecommendationCard
import com.example.ui.components.RecommendationCarousel
import com.example.ui.components.RecommendationLoadingSkeleton
import com.example.ui.components.SpotifyMediaCard
import com.example.ui.components.adaptiveBackdropBlur
import com.example.ui.components.bouncyClick
import com.example.ui.theme.DarkGlassOpaque
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
import com.example.updater.AppUpdateManager
import java.util.Calendar
import java.util.Locale

private enum class HomeRecFilter(val label: String) {
    ALL("All"),
    MADE_FOR_YOU("Made For You"),
    MIXES("Mixes"),
    DISCOVER("Discover")
}

private data class AlbumShowcase(
    val id: String,
    val title: String,
    val artist: String,
    val coverUrl: String,
    val songs: List<Song>
) {
    fun toPlaylist(): Playlist = Playlist(
        id = "album_$id",
        title = title,
        subtitle = "$artist • ${songs.size} tracks",
        coverUrl = coverUrl,
        songCount = songs.size,
        songs = songs
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    currentSong: Song?,
    isPlaying: Boolean,
    onSongSelected: (Song, List<Song>) -> Unit,
    onTogglePlayPause: () -> Unit = {},
    onFavoriteToggle: (Song) -> Unit,
    onDownloadClick: (Song) -> Unit,
    onMoodClick: (MoodCategory) -> Unit,
    onPlaylistClick: (Playlist) -> Unit,
    onOpenImport: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenAiCustomizer: () -> Unit = {},
    onStartRadio: ((Song) -> Unit)? = null,
    onOpenSearch: () -> Unit = {},
    onPlayNext: ((Song) -> Unit)? = null,
    onAddToQueue: ((Song) -> Unit)? = null,
    onAddToPlaylist: ((Song) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val app = context.applicationContext as SpringApplication
    val repository = app.repository
    val theme = LocalAppCustomTheme.current

    val currentUser by AuthManager.currentUser.collectAsState()
    val recentlyPlayed by repository.getRecentlyPlayed().collectAsState(initial = emptyList())
    val favorites by repository.getFavorites().collectAsState(initial = emptyList())
    val userPlaylists by repository.getUserPlaylists().collectAsState(initial = emptyList())
    val recommendationsState by repository.recommendationsState.collectAsState()
    val topCharts by repository.topChartsState.collectAsState()
    val newReleases by repository.newReleasesState.collectAsState()
    val updateAvailable by AppUpdateManager.updateAvailable.collectAsState()
    val updateStatusMsg by AppUpdateManager.statusMessage.collectAsState()
    val importStatus by repository.importStatus.collectAsState()

    var activeFilter by remember { mutableStateOf(HomeRecFilter.ALL) }
    var showNotificationsSheet by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        repository.loadTopChartsIfNeeded()
        repository.loadNewReleasesIfNeeded()
    }

    val greeting = remember {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        when {
            hour < 12 -> "Good morning"
            hour < 18 -> "Good afternoon"
            else -> "Good evening"
        }
    }

    val madeForYouSections = remember(recommendationsState.sections) {
        recommendationsState.sections.filter {
            it.sectionGroup == "personalized" || it.sectionGroup == "repeat"
        }
    }

    val discoverSections = remember(recommendationsState.sections) {
        recommendationsState.sections.filter { it.sectionGroup == "discover" }
    }

    val filteredExtraSections = remember(recommendationsState.sections, activeFilter) {
        when (activeFilter) {
            HomeRecFilter.ALL -> recommendationsState.sections
            HomeRecFilter.MADE_FOR_YOU -> madeForYouSections
            HomeRecFilter.MIXES -> recommendationsState.sections.filter {
                it.sectionGroup == "repeat" || it.categoryKey == "discover_weekly"
            }
            HomeRecFilter.DISCOVER -> discoverSections
        }
    }

    val recentlyPlayedTracks = remember(recentlyPlayed, favorites, recommendationsState.sections) {
        val combined = LinkedHashMap<String, Song>()
        recentlyPlayed.forEach { combined.putIfAbsent(it.id, it) }
        if (combined.size < 4) {
            favorites.forEach { combined.putIfAbsent(it.id, it) }
        }
        combined.values.take(6)
    }

    val featuredSong = remember(currentSong, recommendationsState.discoverWeekly, recommendationsState.sections, topCharts) {
        currentSong
            ?: recommendationsState.discoverWeekly.firstOrNull()
            ?: recommendationsState.sections.firstOrNull()?.songs?.firstOrNull()
            ?: topCharts.firstOrNull()
    }
    val featuredMix = recommendationsState.madeForYouMixes.firstOrNull()

    val trendingTracks = remember(topCharts, recommendationsState.sections) {
        val trendingSec = recommendationsState.sections.firstOrNull {
            it.categoryKey.contains("trending", true) || it.categoryKey.contains("popular", true)
        }?.songs.orEmpty()
        (trendingSec + topCharts).distinctBy { it.id }.take(10)
    }

    val recommendedAlbums = remember(recommendationsState.sections, topCharts, newReleases) {
        val allSongs = (recommendationsState.sections.flatMap { it.songs } + topCharts + newReleases)
            .distinctBy { it.id }
        allSongs
            .filter { it.album.isNotBlank() && !it.album.equals("Single", true) }
            .groupBy { it.album.trim() }
            .entries
            .map { (albumTitle, songs) ->
                val lead = songs.first()
                AlbumShowcase(
                    id = albumTitle.lowercase().replace(Regex("[^a-z0-9]+"), "_"),
                    title = albumTitle,
                    artist = lead.artist,
                    coverUrl = lead.coverUrl,
                    songs = songs
                )
            }
            .distinctBy { it.title.lowercase() }
            .take(10)
    }

    val popularArtists = remember(recommendationsState.sections, topCharts, recentlyPlayed) {
        val fallbackImages = mapOf(
            "arijit singh" to "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=400&auto=format&fit=crop&q=80",
            "the weeknd" to "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=400&auto=format&fit=crop&q=80",
            "pritam" to "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=400&auto=format&fit=crop&q=80",
            "diljit dosanjh" to "https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f?w=400&auto=format&fit=crop&q=80",
            "billie eilish" to "https://images.unsplash.com/photo-1508700115892-45ecd05ae2ad?w=400&auto=format&fit=crop&q=80",
            "taylor swift" to "https://images.unsplash.com/photo-1511379938547-c1f69419868d?w=400&auto=format&fit=crop&q=80"
        )
        val dynamic = LinkedHashMap<String, String>()
        (recentlyPlayed + topCharts + recommendationsState.sections.flatMap { it.songs }).forEach { song ->
            val primaryArtist = song.artist.split(",").firstOrNull()?.trim().orEmpty()
            if (primaryArtist.length > 2 && song.coverUrl.isNotBlank()) {
                dynamic.putIfAbsent(primaryArtist, song.coverUrl)
            }
        }
        val merged = dynamic.entries.map { it.key to it.value }.toMutableList()
        val defaults = listOf(
            "Arijit Singh", "The Weeknd", "Pritam", "Diljit Dosanjh", "Billie Eilish", "Taylor Swift"
        )
        defaults.forEach { name ->
            if (merged.none { it.first.equals(name, true) }) {
                merged.add(name to (fallbackImages[name.lowercase()] ?: ""))
            }
        }
        merged.take(10)
    }

    val effectiveNewReleases = remember(newReleases, recommendationsState.sections) {
        if (newReleases.isNotEmpty()) newReleases.take(10)
        else recommendationsState.sections.lastOrNull()?.songs?.take(8).orEmpty()
    }

    val hasNotificationBadge = updateAvailable != null || importStatus.isImporting || importStatus.completedPlaylist != null

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
            // HEADER: Spring Music Logo + Greeting + Search + Notifications + Profile
            item(key = "home_header", contentType = "home_header") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(start = 20.dp, end = 14.dp, top = 14.dp, bottom = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        BrandSpringLogo(iconSize = 36.dp)

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            IconButton(
                                onClick = onOpenSearch,
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("btn_home_search")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Search Music",
                                    tint = SoftWhite,
                                    modifier = Modifier.size(21.dp)
                                )
                            }

                            Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                                IconButton(
                                    onClick = { showNotificationsSheet = true },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .testTag("btn_home_notifications")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Notifications,
                                        contentDescription = "Activity and Updates",
                                        tint = SoftWhite,
                                        modifier = Modifier.size(21.dp)
                                    )
                                }
                                if (hasNotificationBadge) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(top = 11.dp, end = 11.dp)
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(SoftWhite)
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .clickable { onOpenSettings() }
                                    .padding(6.dp)
                                    .testTag("btn_profile_avatar_settings"),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(GraphiteElevated)
                                        .border(0.85.dp, WhiteHighlight, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (currentUser != null && !currentUser?.photoUrl?.toString().isNullOrBlank()) {
                                        AsyncImage(
                                            model = currentUser?.photoUrl.toString(),
                                            contentDescription = "Profile",
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else if (currentUser != null) {
                                        val initial = (currentUser?.displayName?.takeIf { it.isNotBlank() } ?: currentUser?.email)
                                            ?.take(1)
                                            ?.uppercase()
                                        if (initial != null) {
                                            Text(
                                                text = initial,
                                                color = SoftWhite,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp
                                            )
                                        } else {
                                            Icon(
                                                imageVector = Icons.Default.Person,
                                                contentDescription = "Profile",
                                                tint = SoftWhite,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.Person,
                                            contentDescription = "Profile and Settings",
                                            tint = Silver,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = greeting,
                        color = SoftWhite,
                        fontFamily = DisplayFontFamily,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.5).sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = currentUser?.displayName?.takeIf { it.isNotBlank() }?.let { "Welcome back, $it" }
                            ?: "Midnight Liquid Glass • High-res audio",
                        color = Silver,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Filter Pills
            item(key = "home_filter_chips", contentType = "filter_chips") {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(HomeRecFilter.entries, key = { it.name }) { filter ->
                        val isSelected = activeFilter == filter
                        LiquidGlassPill(
                            isActive = isSelected,
                            onClick = { activeFilter = filter },
                            modifier = Modifier.testTag("home_filter_${filter.name.lowercase()}")
                        ) {
                            Text(
                                text = filter.label,
                                color = if (isSelected) MidnightBlack else SoftWhite,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            // 1. FEATURED MUSIC RECOMMENDATION (Hero Liquid Glass Editorial Showcase)
            if ((activeFilter == HomeRecFilter.ALL || activeFilter == HomeRecFilter.MADE_FOR_YOU) &&
                (featuredSong != null || featuredMix != null)
            ) {
                item(key = "featured_hero", contentType = "featured_hero") {
                    val heroCover = featuredSong?.coverUrl?.takeIf { it.isNotBlank() }
                        ?: featuredMix?.collageUrls?.firstOrNull().orEmpty()
                    val heroTag = when {
                        currentSong != null && isPlaying -> "NOW PLAYING"
                        currentSong != null -> "RESUME SESSION"
                        featuredMix != null -> "FEATURED RECOMMENDATION"
                        else -> "EDITORIAL PICK"
                    }
                    val heroTitle = featuredSong?.title ?: featuredMix?.title ?: "Featured Mix"
                    val heroArtist = featuredSong?.artist ?: featuredMix?.subtitle ?: "Tailored to your taste"

                    LiquidGlassSurface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 10.dp)
                            .bouncyClick {
                                if (currentSong != null) {
                                    onTogglePlayPause()
                                } else if (featuredSong != null) {
                                    onSongSelected(featuredSong, listOfNotNull(featuredSong) + topCharts)
                                } else if (featuredMix != null && featuredMix.songs.isNotEmpty()) {
                                    onSongSelected(featuredMix.songs.first(), featuredMix.songs)
                                }
                            }
                            .testTag("home_spotlight_card"),
                        shape = RoundedCornerShape(24.dp),
                        elevation = 16.dp
                    ) {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            if (heroCover.isNotBlank()) {
                                AsyncImage(
                                    model = heroCover,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .matchParentSize()
                                        .adaptiveBackdropBlur(30.dp),
                                    contentScale = ContentScale.Crop,
                                    alpha = 0.34f
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                Color(0x6608090D),
                                                Color(0xD608090D)
                                            )
                                        )
                                    )
                            )

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(96.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(Graphite)
                                        .border(0.8.dp, WhiteHighlight, RoundedCornerShape(16.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (heroCover.isNotBlank()) {
                                        AsyncImage(
                                            model = heroCover,
                                            contentDescription = heroTitle,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.MusicNote,
                                            contentDescription = null,
                                            tint = MutedGrey,
                                            modifier = Modifier.size(34.dp)
                                        )
                                    }
                                    if (currentSong != null && isPlaying) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(MidnightBlack.copy(alpha = 0.5f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            AnimatedEqualizer(isPlaying = true, barColor = SoftWhite)
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.width(16.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = heroTag,
                                        color = Silver,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        letterSpacing = 1.1.sp,
                                        maxLines = 1
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = heroTitle,
                                        color = SoftWhite,
                                        fontFamily = DisplayFontFamily,
                                        fontSize = 19.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = heroArtist,
                                        color = Silver,
                                        fontSize = 13.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        LiquidGlassPill(
                                            isActive = true,
                                            onClick = {
                                                if (currentSong != null) {
                                                    onTogglePlayPause()
                                                } else if (featuredSong != null) {
                                                    onSongSelected(featuredSong, listOfNotNull(featuredSong) + topCharts)
                                                } else if (featuredMix != null && featuredMix.songs.isNotEmpty()) {
                                                    onSongSelected(featuredMix.songs.first(), featuredMix.songs)
                                                }
                                            }
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = if (currentSong != null && isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                                    contentDescription = null,
                                                    tint = MidnightBlack,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = if (currentSong != null && isPlaying) "Pause" else "Play",
                                                    color = MidnightBlack,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }

                                        if (featuredSong != null && onPlayNext != null && currentSong?.id != featuredSong.id) {
                                            LiquidGlassIconButton(
                                                icon = Icons.Default.QueuePlayNext,
                                                contentDescription = "Play Next",
                                                onClick = { onPlayNext(featuredSong) },
                                                size = 40.dp,
                                                iconSize = 18.dp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 2. RECENTLY PLAYED (2-Column Compact Tiles)
            if (activeFilter == HomeRecFilter.ALL || activeFilter == HomeRecFilter.MIXES) {
                item(key = "recently_played_section", contentType = "recently_played") {
                    if (recentlyPlayedTracks.isNotEmpty()) {
                        val rows = remember(recentlyPlayedTracks) { recentlyPlayedTracks.chunked(2) }
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 18.dp)
                        ) {
                            EditorialSectionHeader(
                                title = "Recently Played",
                                subtitle = "Jump right back into your rotation"
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Column(
                                modifier = Modifier.padding(horizontal = 20.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                rows.forEachIndexed { rowIndex, rowSongs ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        rowSongs.forEachIndexed { colIndex, song ->
                                            val isCurrent = currentSong?.id == song.id
                                            Row(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(58.dp)
                                                    .clip(RoundedCornerShape(14.dp))
                                                    .background(
                                                        if (isCurrent) WhiteHighlightSoft else theme.cardBackground
                                                    )
                                                    .border(
                                                        width = 0.75.dp,
                                                        color = if (isCurrent) WhiteHighlight else WhiteHighlightSoft,
                                                        shape = RoundedCornerShape(14.dp)
                                                    )
                                                    .bouncyClick {
                                                        onSongSelected(song, recentlyPlayedTracks)
                                                    }
                                                    .testTag("recent_tile_${rowIndex}_$colIndex"),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(58.dp)
                                                        .background(GraphiteElevated),
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
                                                            tint = MutedGrey,
                                                            modifier = Modifier.size(20.dp)
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
                                                Column(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .padding(horizontal = 10.dp),
                                                    verticalArrangement = Arrangement.Center
                                                ) {
                                                    Text(
                                                        text = song.title,
                                                        color = SoftWhite,
                                                        fontSize = 13.sp,
                                                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Text(
                                                        text = song.artist,
                                                        color = Silver,
                                                        fontSize = 11.sp,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            }
                                        }
                                        if (rowSongs.size == 1) {
                                            Spacer(modifier = Modifier.weight(1f))
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        val moods = remember { PresetCatalog.MOOD_CATEGORIES }
                        Column(modifier = Modifier.padding(top = 18.dp)) {
                            EditorialSectionHeader(
                                title = "Recently Played",
                                subtitle = "Pick a mood to begin your listening profile"
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 20.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(moods, key = { it.id }) { mood ->
                                    LiquidGlassPill(onClick = { onMoodClick(mood) }) {
                                        Text(
                                            text = mood.title,
                                            color = SoftWhite,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 3. MADE FOR YOU (Personalized Weekly & Taste Carousels)
            if (activeFilter == HomeRecFilter.ALL || activeFilter == HomeRecFilter.MADE_FOR_YOU) {
                if (recommendationsState.isLoading && madeForYouSections.isEmpty()) {
                    item(key = "made_for_you_loading") {
                        RecommendationLoadingSkeleton()
                    }
                } else if (madeForYouSections.isNotEmpty()) {
                    items(
                        items = madeForYouSections.take(2),
                        key = { "mfy_${it.categoryKey}" },
                        contentType = { "recommendation_carousel" }
                    ) { section ->
                        RecommendationCarousel(
                            section = section,
                            onSongSelected = onSongSelected,
                            onFavoriteToggle = onFavoriteToggle,
                            onFeedback = { song, fb -> repository.recordSongFeedback(song, fb) },
                            onRefreshSection = if (section.canRefresh) {
                                { repository.refreshRecommendations(force = true) }
                            } else null,
                            onStartRadio = onStartRadio,
                            onPlayNext = onPlayNext,
                            onAddToQueue = onAddToQueue
                        )
                    }
                }
            }

            // 4. DAILY MIXES
            if (recommendationsState.madeForYouMixes.isNotEmpty() && activeFilter != HomeRecFilter.DISCOVER) {
                item(key = "daily_mixes", contentType = "daily_mixes") {
                    MadeForYouCarousel(
                        mixes = recommendationsState.madeForYouMixes,
                        title = "Daily Mixes",
                        subtitle = "Blended playlists crafted from your artists",
                        onOpenMix = { mix -> onPlaylistClick(mix.toPlaylist()) },
                        onPlayMix = { mix ->
                            if (mix.songs.isNotEmpty()) {
                                onSongSelected(mix.songs.first(), mix.songs)
                            }
                        }
                    )
                }
            }

            // 5. TRENDING MUSIC (Editorial Ranked Strip)
            if ((activeFilter == HomeRecFilter.ALL || activeFilter == HomeRecFilter.DISCOVER) &&
                trendingTracks.isNotEmpty()
            ) {
                item(key = "trending_music", contentType = "trending_music") {
                    Column(modifier = Modifier.padding(top = 28.dp)) {
                        EditorialSectionHeader(
                            title = "Trending Music",
                            subtitle = "Most streamed tracks right now",
                            actionLabel = "Play All",
                            onActionClick = { onSongSelected(trendingTracks.first(), trendingTracks) }
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            itemsIndexed(
                                items = trendingTracks,
                                key = { idx, song -> "trend_${idx}_${song.id}" }
                            ) { index, song ->
                                val isCurrent = currentSong?.id == song.id
                                LiquidGlassSurface(
                                    modifier = Modifier
                                        .width(236.dp)
                                        .bouncyClick { onSongSelected(song, trendingTracks) },
                                    shape = RoundedCornerShape(18.dp),
                                    elevation = 8.dp
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = String.format(Locale.US, "%02d", index + 1),
                                            color = if (index < 3 || isCurrent) SoftWhite else MutedGrey,
                                            fontFamily = MonoFontFamily,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.width(26.dp)
                                        )
                                        Box(
                                            modifier = Modifier
                                                .size(54.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(GraphiteElevated),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (song.coverUrl.isNotBlank()) {
                                                AsyncImage(
                                                    model = song.coverUrl,
                                                    contentDescription = song.title,
                                                    modifier = Modifier.fillMaxSize(),
                                                    contentScale = ContentScale.Crop
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
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = song.title,
                                                color = SoftWhite,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.SemiBold,
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
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 6. RECOMMENDED ALBUMS
            if ((activeFilter == HomeRecFilter.ALL || activeFilter == HomeRecFilter.DISCOVER) &&
                recommendedAlbums.isNotEmpty()
            ) {
                item(key = "recommended_albums", contentType = "recommended_albums") {
                    Column(modifier = Modifier.padding(top = 28.dp)) {
                        EditorialSectionHeader(
                            title = "Recommended Albums",
                            subtitle = "Full releases tailored to your library"
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            items(
                                items = recommendedAlbums,
                                key = { "album_${it.id}" }
                            ) { album ->
                                SpotifyMediaCard(
                                    title = album.title,
                                    subtitle = album.artist,
                                    coverUrl = album.coverUrl,
                                    onClick = { onPlaylistClick(album.toPlaylist()) }
                                )
                            }
                        }
                    }
                }
            }

            // 7. POPULAR ARTISTS
            if (activeFilter == HomeRecFilter.ALL || activeFilter == HomeRecFilter.DISCOVER) {
                item(key = "popular_artists", contentType = "popular_artists") {
                    Column(modifier = Modifier.padding(top = 28.dp)) {
                        EditorialSectionHeader(
                            title = "Popular Artists",
                            subtitle = "Explore top discographies and radios"
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(popularArtists, key = { it.first }, contentType = { "quick_artist" }) { (name, img) ->
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .width(82.dp)
                                        .bouncyClick {
                                            onMoodClick(
                                                MoodCategory(
                                                    id = name.lowercase().replace(" ", "_"),
                                                    title = name,
                                                    description = "Explore tracks by $name",
                                                    searchTag = name,
                                                    emoji = "",
                                                    gradientStart = 0xFF23262F,
                                                    gradientEnd = 0xFF171A21
                                                )
                                            )
                                        }
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(76.dp)
                                            .clip(CircleShape)
                                            .background(Graphite)
                                            .border(0.85.dp, WhiteHighlight, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (img.isNotBlank()) {
                                            AsyncImage(
                                                model = img,
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
                                    Spacer(modifier = Modifier.height(8.dp))
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
            }

            // 8. NEW RELEASES (Wide Editorial Cards)
            if ((activeFilter == HomeRecFilter.ALL || activeFilter == HomeRecFilter.DISCOVER) &&
                effectiveNewReleases.isNotEmpty()
            ) {
                item(key = "new_releases", contentType = "new_releases") {
                    Column(modifier = Modifier.padding(top = 28.dp)) {
                        EditorialSectionHeader(
                            title = "New Releases",
                            subtitle = "Fresh drops and latest singles",
                            actionLabel = "Search",
                            onActionClick = onOpenSearch
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            items(
                                items = effectiveNewReleases,
                                key = { "rel_${it.id}" }
                            ) { song ->
                                RecommendationCard(
                                    song = song,
                                    onClick = { onSongSelected(song, effectiveNewReleases) },
                                    onFavoriteToggle = { onFavoriteToggle(song) },
                                    onFeedback = { fb -> repository.recordSongFeedback(song, fb) },
                                    onStartRadio = onStartRadio?.let { cb -> { cb(song) } },
                                    onPlayNext = onPlayNext?.let { cb -> { cb(song) } },
                                    onAddToQueue = onAddToQueue?.let { cb -> { cb(song) } },
                                    modifier = Modifier.width(156.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Additional Discover / Filtered Personalized Carousels
            val extraNonDuplicateSections = filteredExtraSections.filterNot { sec ->
                activeFilter == HomeRecFilter.ALL && madeForYouSections.take(2).any { it.categoryKey == sec.categoryKey }
            }
            if (extraNonDuplicateSections.isNotEmpty()) {
                items(
                    items = extraNonDuplicateSections,
                    key = { "extra_${it.categoryKey}" },
                    contentType = { "recommendation_carousel" }
                ) { section ->
                    RecommendationCarousel(
                        section = section,
                        onSongSelected = onSongSelected,
                        onFavoriteToggle = onFavoriteToggle,
                        onFeedback = { song, fb -> repository.recordSongFeedback(song, fb) },
                        onRefreshSection = if (section.canRefresh) {
                            { repository.refreshRecommendations(force = true) }
                        } else null,
                        onStartRadio = onStartRadio,
                        onPlayNext = onPlayNext,
                        onAddToQueue = onAddToQueue
                    )
                }
            } else if (!recommendationsState.isLoading && filteredExtraSections.isEmpty() && recommendationsState.madeForYouMixes.isEmpty()) {
                item(key = "rec_empty_state") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 24.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(Graphite)
                            .border(0.8.dp, WhiteHighlightSoft, RoundedCornerShape(18.dp))
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No ${activeFilter.label} picks yet",
                            color = SoftWhite,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Refresh your feed or switch back to All recommendations.",
                            color = Silver,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = {
                                if (activeFilter != HomeRecFilter.ALL) {
                                    activeFilter = HomeRecFilter.ALL
                                } else {
                                    repository.refreshRecommendations(force = true)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SoftWhite,
                                contentColor = MidnightBlack
                            ),
                            shape = RoundedCornerShape(50),
                            modifier = Modifier.height(48.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (activeFilter != HomeRecFilter.ALL) "Show All" else "Refresh Feed",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }

            // 9. RECENTLY ADDED PLAYLISTS
            if (activeFilter == HomeRecFilter.ALL || activeFilter == HomeRecFilter.MADE_FOR_YOU) {
                item(key = "recently_added_playlists", contentType = "user_playlists") {
                    Column(modifier = Modifier.padding(top = 28.dp)) {
                        EditorialSectionHeader(
                            title = "Recently Added Playlists",
                            subtitle = "Your collections and Spotify imports",
                            actionLabel = "Import",
                            onActionClick = onOpenImport
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        if (userPlaylists.isEmpty()) {
                            LiquidGlassSurface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp)
                                    .clickable { onOpenImport() },
                                shape = RoundedCornerShape(18.dp),
                                elevation = 8.dp
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(WhiteHighlightSoft),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PlaylistAdd,
                                            contentDescription = null,
                                            tint = SoftWhite,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Import a Spotify playlist",
                                            color = SoftWhite,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "Search or paste a public playlist link to match songs",
                                            color = Silver,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        } else {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 20.dp),
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                items(userPlaylists, key = { it.id }, contentType = { "playlist_card" }) { userPl ->
                                    SpotifyMediaCard(
                                        title = userPl.title,
                                        subtitle = userPl.subtitle,
                                        coverUrl = userPl.coverUrl,
                                        onClick = { onPlaylistClick(userPl) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showNotificationsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showNotificationsSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = DarkGlassOpaque,
            contentColor = SoftWhite
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 28.dp)
            ) {
                Text(
                    text = "Activity & Updates",
                    color = SoftWhite,
                    fontFamily = DisplayFontFamily,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "App updates, playlist syncs, and theme controls",
                    color = Silver,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Software Update Status Row
                LiquidGlassSurface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            showNotificationsSheet = false
                            AppUpdateManager.checkForUpdates(context, isManualCheck = true)
                        },
                    shape = RoundedCornerShape(16.dp),
                    elevation = 6.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.SystemUpdate,
                            contentDescription = null,
                            tint = SoftWhite,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (updateAvailable != null) "Update v${updateAvailable?.versionName} Ready" else "Spring Music Software",
                                color = SoftWhite,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = updateStatusMsg ?: "Running v${AppUpdateManager.currentVersionName}. Tap to check.",
                                color = Silver,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Spotify Import Shortcut Row
                LiquidGlassSurface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            showNotificationsSheet = false
                            onOpenImport()
                        },
                    shape = RoundedCornerShape(16.dp),
                    elevation = 6.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlaylistAdd,
                            contentDescription = null,
                            tint = SoftWhite,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (importStatus.isImporting) "Importing ${importStatus.playlistName}" else "Import Spotify Playlist",
                                color = SoftWhite,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = when {
                                    importStatus.isImporting -> "${importStatus.currentTrack}/${importStatus.totalTracks} tracks matched"
                                    importStatus.completedPlaylist != null -> "'${importStatus.completedPlaylist?.title}' added to Library"
                                    else -> "Bring playlists from Spotify into Spring Music"
                                },
                                color = Silver,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Liquid Glass Finish Shortcut
                LiquidGlassSurface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            showNotificationsSheet = false
                            onOpenAiCustomizer()
                        },
                    shape = RoundedCornerShape(16.dp),
                    elevation = 6.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = null,
                            tint = SoftWhite,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Liquid Glass Finish",
                                color = SoftWhite,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Customize monochromatic dark glass finishes",
                                color = Silver,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
