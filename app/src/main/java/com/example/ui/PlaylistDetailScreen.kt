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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import com.example.data.model.Playlist
import com.example.data.model.Song
import com.example.data.network.PresetCatalog
import com.example.ui.components.SongRowItem
import com.example.ui.theme.AuroraCyan
import com.example.ui.theme.AuroraMagenta
import com.example.ui.theme.AuroraViolet
import com.example.ui.theme.CrispWhite
import com.example.ui.theme.FrostedGlassBorder
import com.example.ui.theme.FrostedGlassSurface
import com.example.ui.theme.FusionAmbience
import com.example.ui.theme.IosRed
import com.example.ui.theme.MidnightCanvas
import com.example.ui.theme.MidnightElevated
import com.example.ui.theme.SlateMuted
import com.example.ui.theme.SlateSecondary
import kotlinx.coroutines.launch

@Composable
fun PlaylistDetailScreen(
    playlist: Playlist,
    currentSong: Song?,
    isPlaying: Boolean,
    onSongSelected: (Song, List<Song>) -> Unit,
    onFavoriteToggle: (Song) -> Unit,
    onDownloadClick: (Song) -> Unit,
    onBack: () -> Unit,
    onPlaylistDeleted: () -> Unit = {}
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val app = context.applicationContext as? SpringApplication
    val repository = app?.repository
    val scope = rememberCoroutineScope()
    var showDeleteDialog by remember { mutableStateOf(false) }
    val isUserDeletable = playlist.id != "pl_liked_songs" &&
        playlist.id.startsWith("pl_") &&
        PresetCatalog.PRESET_PLAYLISTS.none { it.id == playlist.id }

    val ambientSeedSong = currentSong ?: playlist.songs.firstOrNull()
    val ambientColors = remember(ambientSeedSong?.id, ambientSeedSong?.mood) { FusionAmbience.getColors(ambientSeedSong) }
    val accentColor = ambientColors.firstOrNull() ?: AuroraCyan

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            containerColor = MidnightElevated,
            title = {
                Text(
                    text = "Delete Playlist?",
                    color = CrispWhite,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to remove \"${playlist.title}\" from your library?",
                    color = SlateSecondary,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteDialog = false
                        scope.launch {
                            repository?.deletePlaylist(playlist.id)
                            onPlaylistDeleted()
                            onBack()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = IosRed)
                ) {
                    Text("Delete", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel", color = SlateSecondary)
                }
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MidnightCanvas)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(340.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            accentColor.copy(alpha = 0.28f),
                            AuroraViolet.copy(alpha = 0.18f),
                            Color.Transparent
                        ),
                        center = Offset(500f, 200f),
                        radius = 850f
                    )
                )
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 165.dp)
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(start = 16.dp, end = 16.dp, top = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(FrostedGlassSurface)
                            .border(1.dp, FrostedGlassBorder, CircleShape)
                            .clickable { onBack() }
                            .testTag("btn_playlist_back"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = CrispWhite,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    if (isUserDeletable) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(FrostedGlassSurface)
                                .border(1.dp, FrostedGlassBorder, CircleShape)
                                .clickable { showDeleteDialog = true }
                                .testTag("btn_delete_playlist"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Delete Playlist",
                                tint = CrispWhite,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(210.dp)
                            .shadow(
                                elevation = 26.dp,
                                shape = RoundedCornerShape(24.dp),
                                ambientColor = AuroraViolet.copy(alpha = 0.5f),
                                spotColor = Color.Black
                            )
                            .clip(RoundedCornerShape(24.dp))
                            .border(1.2.dp, FusionAmbience.GlassBorderBrush, RoundedCornerShape(24.dp))
                            .background(FrostedGlassSurface),
                        contentAlignment = Alignment.Center
                    ) {
                        if (playlist.coverUrl.isNotBlank()) {
                            AsyncImage(
                                model = playlist.coverUrl,
                                contentDescription = playlist.title,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.MusicNote,
                                contentDescription = null,
                                tint = AuroraCyan,
                                modifier = Modifier.size(64.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Text(
                        text = playlist.title,
                        color = CrispWhite,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        textAlign = TextAlign.Center,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = playlist.subtitle.ifBlank { "${playlist.songs.size} tracks" },
                        color = SlateSecondary,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        maxLines = 2
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(FrostedGlassSurface)
                            .border(1.dp, FrostedGlassBorder, RoundedCornerShape(24.dp))
                            .clickable {
                                if (playlist.songs.isNotEmpty()) {
                                    val shuffled = playlist.songs.shuffled()
                                    onSongSelected(shuffled.first(), shuffled)
                                }
                            },
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shuffle,
                            contentDescription = "Shuffle Play",
                            tint = CrispWhite,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Shuffle",
                            color = CrispWhite,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }

                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .shadow(14.dp, RoundedCornerShape(24.dp), ambientColor = AuroraCyan, spotColor = AuroraViolet)
                            .clip(RoundedCornerShape(24.dp))
                            .background(Brush.horizontalGradient(listOf(AuroraCyan, AuroraViolet)))
                            .clickable {
                                if (playlist.songs.isNotEmpty()) {
                                    onSongSelected(playlist.songs.first(), playlist.songs)
                                }
                            }
                            .testTag("btn_playlist_play_all"),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Play Playlist",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Play All",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            if (playlist.songs.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No tracks in this collection",
                            color = CrispWhite,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Import tracks or add from Explore",
                            color = SlateMuted,
                            fontSize = 12.sp
                        )
                    }
                }
            } else {
                itemsIndexed(
                    items = playlist.songs,
                    key = { index, song -> "pl_${index}_${song.id}" },
                    contentType = { _, _ -> "playlist_song_row" }
                ) { index, song ->
                    val isCurrent = currentSong?.id == song.id
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSongSelected(song, playlist.songs) }
                            .padding(start = 12.dp, end = 6.dp, top = 2.dp, bottom = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = String.format("%02d", index + 1),
                            color = if (isCurrent) AuroraCyan else SlateMuted,
                            fontSize = 12.sp,
                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                            modifier = Modifier.width(28.dp)
                        )

                        Box(modifier = Modifier.weight(1f)) {
                            SongRowItem(
                                song = song,
                                isCurrent = isCurrent,
                                isPlaying = isPlaying && isCurrent,
                                onClick = { onSongSelected(song, playlist.songs) },
                                onFavoriteToggle = { onFavoriteToggle(song) },
                                onDownloadClick = { onDownloadClick(song) }
                            )
                        }
                    }
                }
            }
        }
    }
}
