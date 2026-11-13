package com.example.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
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
import com.example.auth.AuthManager
import com.example.data.model.MoodCategory
import com.example.data.model.Playlist
import com.example.data.model.Song
import com.example.ui.components.RecommendationCarousel
import com.example.ui.components.RecommendationLoadingSkeleton
import com.example.ui.components.SpotifyMediaCard
import com.example.ui.components.bouncyClick
import com.example.ui.theme.AuroraCyan
import com.example.ui.theme.AuroraDisplayFontFamily
import com.example.ui.theme.AuroraMagenta
import com.example.ui.theme.AuroraViolet
import com.example.ui.theme.CrispWhite
import com.example.ui.theme.FrostedGlassBorder
import com.example.ui.theme.FrostedGlassSurface
import com.example.ui.theme.FusionAmbience
import com.example.ui.theme.GoldBright
import com.example.ui.theme.MidnightCanvas
import com.example.ui.theme.MidnightElevated
import com.example.ui.theme.SlateSecondary
import java.util.Calendar
import kotlin.math.sin

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

    val currentUser by AuthManager.currentUser.collectAsState()
    val recentlyPlayed by repository.getRecentlyPlayed().collectAsState(initial = emptyList())
    val favorites by repository.getFavorites().collectAsState(initial = emptyList())
    val userPlaylists by repository.getUserPlaylists().collectAsState(initial = emptyList())
    val recommendationsState by repository.recommendationsState.collectAsState()

    val greeting = remember {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        when {
            hour < 12 -> "Good morning"
            hour < 18 -> "Good afternoon"
            else -> "Good evening"
        }
    }

    // Assemble 8 playable tracks for the 2x4 Recently Played compact grid
    val quickGridTracks = remember(recentlyPlayed, favorites, recommendationsState.sections) {
        val combined = LinkedHashMap<String, Song>()
        recentlyPlayed.forEach { combined.putIfAbsent(it.id, it) }
        favorites.forEach { combined.putIfAbsent(it.id, it) }
        recommendationsState.sections.flatMap { it.songs }.forEach {
            combined.putIfAbsent(it.id, it)
        }
        combined.values.take(8)
    }

    val peakMixSongs = remember(recommendationsState.sections, quickGridTracks) {
        recommendationsState.sections.firstOrNull {
            it.categoryKey.contains("daily", true) || it.categoryKey.contains("for_you", true)
        }?.songs?.takeIf { it.isNotEmpty() }
            ?: recommendationsState.sections.flatMap { it.songs }.distinctBy { it.id }.take(15)
                .ifEmpty { quickGridTracks }
    }

    val ambientColors = remember(currentSong?.id, currentSong?.mood) { FusionAmbience.getColors(currentSong) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MidnightCanvas)
    ) {
        // Luminous Ambient Aurora Atmospheric Glow behind the top header & grid
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(380.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            ambientColors[0].copy(alpha = 0.28f),
                            AuroraViolet.copy(alpha = 0.22f),
                            Color.Transparent
                        ),
                        center = Offset(760f, 120f),
                        radius = 950f
                    )
                )
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(320.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            AuroraMagenta.copy(alpha = 0.16f),
                            AuroraCyan.copy(alpha = 0.12f),
                            Color.Transparent
                        ),
                        center = Offset(140f, 260f),
                        radius = 780f
                    )
                )
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 165.dp)
        ) {
            // 1. iOS Large Iridescent Greeting Header + Glowing Avatar Ring
            item(key = "home_header", contentType = "home_header") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = greeting,
                            style = TextStyle(
                                fontFamily = AuroraDisplayFontFamily,
                                fontSize = 34.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = (-0.8).sp,
                                brush = Brush.horizontalGradient(
                                    listOf(GoldBright, AuroraCyan, AuroraMagenta)
                                )
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Luminous mixes tailored to your sonic aura",
                            color = SlateSecondary,
                            fontSize = 13.sp
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Compact Theme Studio pill
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(FrostedGlassSurface)
                                .border(1.dp, FrostedGlassBorder, CircleShape)
                                .clickable { onOpenAiCustomizer() }
                                .padding(horizontal = 10.dp, vertical = 8.dp)
                                .testTag("btn_home_ai_customize"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "Theme Studio",
                                tint = AuroraCyan,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Circular User Avatar wrapped in a Glowing Iridescent Gradient Ring
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .shadow(
                                    elevation = 12.dp,
                                    shape = CircleShape,
                                    ambientColor = AuroraCyan,
                                    spotColor = AuroraMagenta
                                )
                                .clip(CircleShape)
                                .background(
                                    Brush.sweepGradient(
                                        listOf(AuroraCyan, AuroraViolet, AuroraMagenta, AuroraCyan)
                                    )
                                )
                                .padding(2.5.dp)
                                .clip(CircleShape)
                                .background(MidnightElevated)
                                .clickable { onOpenSettings() }
                                .testTag("btn_profile_avatar_settings"),
                            contentAlignment = Alignment.Center
                        ) {
                            if (currentUser != null && !currentUser?.photoUrl?.toString().isNullOrBlank()) {
                                AsyncImage(
                                    model = currentUser?.photoUrl.toString(),
                                    contentDescription = "Profile",
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else if (currentUser != null) {
                                val initial = (currentUser?.displayName?.takeIf { it.isNotBlank() } ?: currentUser?.email)
                                    ?.take(1)
                                    ?.uppercase()
                                if (initial != null) {
                                    Text(
                                        text = initial,
                                        color = CrispWhite,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = "Profile",
                                        tint = CrispWhite,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = "Sign in",
                                    tint = AuroraCyan,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 2. Recently Played (2x4 Compact Grid)
            item(key = "recent_grid", contentType = "recent_grid") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "Recently Played",
                        color = CrispWhite,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.3).sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    val slots = remember(quickGridTracks) { List(8) { idx -> quickGridTracks.getOrNull(idx) } }
                    val fallbackMoods = remember { com.example.data.network.PresetCatalog.MOOD_CATEGORIES }
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        for (rowIndex in 0..1) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                for (colIndex in 0..3) {
                                    val slotIdx = rowIndex * 4 + colIndex
                                    val song = slots[slotIdx]
                                    val fallbackMood = fallbackMoods[slotIdx % fallbackMoods.size]
                                    val isCurrent = song != null && currentSong?.id == song.id
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(1f)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(
                                                if (song != null && song.coverUrl.isNotBlank()) {
                                                    Brush.linearGradient(listOf(FrostedGlassSurface, FrostedGlassSurface))
                                                } else {
                                                    Brush.linearGradient(
                                                        listOf(
                                                            Color(fallbackMood.gradientStart).copy(alpha = 0.65f),
                                                            Color(fallbackMood.gradientEnd).copy(alpha = 0.85f)
                                                        )
                                                    )
                                                }
                                            )
                                            .border(
                                                width = if (isCurrent) 1.5.dp else 0.8.dp,
                                                color = if (isCurrent) AuroraCyan else FrostedGlassBorder,
                                                shape = RoundedCornerShape(10.dp)
                                            )
                                            .bouncyClick(pressedScale = 0.94f) {
                                                if (song != null) {
                                                    onSongSelected(song, quickGridTracks)
                                                } else {
                                                    onMoodClick(fallbackMood)
                                                }
                                            }
                                            .testTag("recent_tile_${rowIndex}_$colIndex"),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (song != null && song.coverUrl.isNotBlank()) {
                                            AsyncImage(
                                                model = song.coverUrl,
                                                contentDescription = song.title,
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .align(Alignment.BottomCenter)
                                                    .fillMaxWidth()
                                                    .background(
                                                        Brush.verticalGradient(
                                                            listOf(Color.Transparent, Color(0xCC08090F))
                                                        )
                                                    )
                                                    .padding(horizontal = 5.dp, vertical = 4.dp)
                                            ) {
                                                Text(
                                                    text = song.title,
                                                    color = CrispWhite,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        } else {
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.Center,
                                                modifier = Modifier.padding(6.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.MusicNote,
                                                    contentDescription = fallbackMood.title,
                                                    tint = Color.White.copy(alpha = 0.9f),
                                                    modifier = Modifier.size(20.dp)
                                                )
                                                Spacer(modifier = Modifier.height(3.dp))
                                                Text(
                                                    text = song?.title ?: fallbackMood.title.substringBefore(" "),
                                                    color = Color.White,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
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
                    }
                }
            }

            // 3. Featured Hero Banner: "Peak Hours Mix" over holographic mesh gradient + waveform + centered frosted Play
            item(key = "hero_banner", contentType = "hero_banner") {
                PeakHoursHeroBanner(
                    subtitle = if (peakMixSongs.isNotEmpty()) "${peakMixSongs.size} high-energy curated tracks" else "Adaptive sonic wave • Updated daily",
                    onPlay = {
                        if (peakMixSongs.isNotEmpty()) {
                            onSongSelected(peakMixSongs.first(), peakMixSongs)
                        } else {
                            onMoodClick(
                                MoodCategory(
                                    id = "peak_hours",
                                    title = "Peak Hours Mix",
                                    description = "High energy mixes",
                                    searchTag = "Peak Hours Workout Electronic Pop",
                                    emoji = "⚡",
                                    gradientStart = 0xFF38BDF8,
                                    gradientEnd = 0xFFEC4899
                                )
                            )
                        }
                    },
                    onOpenImport = onOpenImport
                )
            }

            // 4. Quick Launch Carousel: 72dp Circular Artist Portraits with Multi-Color Gradient Rings
            item(key = "quick_launch", contentType = "quick_launch") {
                Column(modifier = Modifier.padding(top = 18.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Quick Launch",
                            color = CrispWhite,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.3).sp
                        )
                        Text(
                            text = "Tap an artist aura",
                            color = SlateSecondary,
                            fontSize = 12.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    val quickArtists = remember {
                        listOf(
                            "Arijit Singh" to "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=400&auto=format&fit=crop&q=80",
                            "The Weeknd" to "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=400&auto=format&fit=crop&q=80",
                            "Fred again.." to "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=400&auto=format&fit=crop&q=80",
                            "Billie Eilish" to "https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f?w=400&auto=format&fit=crop&q=80",
                            "Taylor Swift" to "https://images.unsplash.com/photo-1508700115892-45ecd05ae2ad?w=400&auto=format&fit=crop&q=80",
                            "Prateek Kuhad" to "https://images.unsplash.com/photo-1511379938547-c1f69419868d?w=400&auto=format&fit=crop&q=80"
                        )
                    }

                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 18.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(quickArtists, key = { it.first }, contentType = { "quick_artist" }) { (name, img) ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .width(78.dp)
                                    .bouncyClick(pressedScale = 0.93f) {
                                        onMoodClick(
                                            MoodCategory(
                                                id = name.lowercase().replace(" ", "_"),
                                                title = name,
                                                description = "Explore tracks by $name",
                                                searchTag = name,
                                                emoji = "✨",
                                                gradientStart = 0xFF38BDF8,
                                                gradientEnd = 0xFF8B5CF6
                                            )
                                        )
                                    }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(72.dp)
                                        .clip(CircleShape)
                                        .background(
                                            Brush.sweepGradient(
                                                listOf(AuroraCyan, AuroraViolet, AuroraMagenta, AuroraCyan)
                                            )
                                        )
                                        .padding(2.5.dp)
                                        .clip(CircleShape)
                                        .background(MidnightElevated)
                                ) {
                                    AsyncImage(
                                        model = img,
                                        contentDescription = name,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                                Spacer(modifier = Modifier.height(7.dp))
                                Text(
                                    text = name,
                                    color = CrispWhite,
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

            // 5. Playlist Import Frosted Banner
            item(key = "import_banner", contentType = "import_banner") {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 16.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .border(1.dp, FrostedGlassBorder, RoundedCornerShape(18.dp))
                        .bouncyClick { onOpenImport() },
                    colors = CardDefaults.cardColors(containerColor = FrostedGlassSurface),
                    shape = RoundedCornerShape(18.dp)
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
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Brush.linearGradient(listOf(AuroraCyan, AuroraViolet))),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Import",
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Import Spotify Playlist",
                                    color = CrispWhite,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                Text(
                                    text = "Search by name or paste any link",
                                    color = SlateSecondary,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0x22FFFFFF))
                                .border(0.8.dp, FrostedGlassBorder, RoundedCornerShape(16.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "Import",
                                color = AuroraCyan,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            // 6. Intelligent Personalized Recommendation Sections
            if (recommendationsState.isLoading && recommendationsState.sections.isEmpty()) {
                item {
                    RecommendationLoadingSkeleton()
                }
            }

            items(
                items = recommendationsState.sections,
                key = { "rec_${it.categoryKey}" },
                contentType = { "rec_carousel" }
            ) { section ->
                RecommendationCarousel(
                    section = section,
                    onSongSelected = onSongSelected,
                    onFavoriteToggle = onFavoriteToggle,
                    onFeedback = { s, f -> repository.recordSongFeedback(s, f) },
                    onRefreshSection = if (section.canRefresh) { { repository.refreshRecommendations(force = true) } } else null
                )
            }

            // 7. User Playlists Carousel
            if (userPlaylists.isNotEmpty()) {
                item(key = "user_playlists_section", contentType = "user_playlists") {
                    Column(modifier = Modifier.padding(top = 20.dp)) {
                        Text(
                            text = "Your Mixes & Playlists",
                            color = CrispWhite,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp)
                        )
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 18.dp),
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

@Composable
private fun PeakHoursHeroBanner(
    subtitle: String,
    onPlay: () -> Unit,
    onOpenImport: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "hero_wave")
    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (Math.PI * 2).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(3200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave_phase"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 10.dp)
            .height(184.dp)
            .shadow(
                elevation = 24.dp,
                shape = RoundedCornerShape(20.dp),
                ambientColor = AuroraViolet.copy(alpha = 0.45f),
                spotColor = AuroraMagenta.copy(alpha = 0.45f)
            )
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF171324),
                        Color(0xFF281D3B),
                        Color(0xFF3A222D)
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(listOf(Color(0x55F5E6C8), FrostedGlassBorder)),
                shape = RoundedCornerShape(20.dp)
            )
            .bouncyClick(pressedScale = 0.97f) { onPlay() }
            .testTag("hero_peak_hours_banner")
    ) {
        // Holographic mesh glow orbs + animated waveform graphic (zero per-frame Path allocation)
        val pathPrimary = remember { Path() }
        val pathSecondary = remember { Path() }
        val waveBrush = remember { Brush.horizontalGradient(listOf(AuroraCyan, Color.White, AuroraMagenta)) }

        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(AuroraCyan.copy(alpha = 0.42f), Color.Transparent)
                ),
                radius = size.width * 0.45f,
                center = Offset(size.width * 0.16f, size.height * 0.25f)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(AuroraMagenta.copy(alpha = 0.45f), Color.Transparent)
                ),
                radius = size.width * 0.5f,
                center = Offset(size.width * 0.85f, size.height * 0.8f)
            )

            pathPrimary.reset()
            pathSecondary.reset()
            val midY = size.height * 0.52f
            val steps = 40
            for (i in 0..steps) {
                val x = (i.toFloat() / steps) * size.width
                val norm = i.toFloat() / steps
                val env = sin(norm * Math.PI).toFloat()
                val y1 = midY + sin(norm * 10f + wavePhase) * 28f * env
                val y2 = midY + sin(norm * 7f - wavePhase * 1.2f) * 20f * env
                if (i == 0) {
                    pathPrimary.moveTo(x, y1)
                    pathSecondary.moveTo(x, y2)
                } else {
                    pathPrimary.lineTo(x, y1)
                    pathSecondary.lineTo(x, y2)
                }
            }
            drawPath(
                path = pathSecondary,
                color = AuroraCyan.copy(alpha = 0.4f),
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
            )
            drawPath(
                path = pathPrimary,
                brush = waveBrush,
                style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round)
            )
        }

        // Top badge
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(14.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0x6607070B))
                .border(0.8.dp, Color(0x45EAD9B7), RoundedCornerShape(20.dp))
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Text(
                text = "FEATURED MIX",
                color = AuroraCyan,
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.sp
            )
        }

        // Centered Luminous Champagne-Rimmed Play Button
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(62.dp)
                .shadow(18.dp, CircleShape, ambientColor = AuroraCyan, spotColor = Color.Black)
                .clip(CircleShape)
                .background(FusionAmbience.GoldMetallicBrush)
                .border(1.4.dp, GoldBright, CircleShape)
                .bouncyClick(pressedScale = 0.90f) { onPlay() }
                .testTag("btn_hero_peak_play"),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "Play Peak Hours Mix",
                tint = MidnightCanvas,
                modifier = Modifier.size(34.dp)
            )
        }

        // Bottom left title & subtitle
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Text(
                text = "Peak Hours Mix",
                color = CrispWhite,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.4).sp
            )
            Text(
                text = subtitle,
                color = Color(0xDDEBEBF5),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
