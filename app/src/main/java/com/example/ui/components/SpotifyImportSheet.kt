package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Playlist
import com.example.data.repository.MusicRepository
import com.example.ui.theme.SpotifyBlack
import com.example.ui.theme.SpotifyCard
import com.example.ui.theme.SpotifyCardElevated
import com.example.ui.theme.SpotifyGreen
import com.example.ui.theme.SpotifyPitchBlack
import com.example.ui.theme.SpotifyTextMuted
import com.example.ui.theme.SpotifyTextPrimary
import com.example.ui.theme.SpotifyTextSecondary
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpotifyImportSheet(
    repository: MusicRepository,
    onPlaylistImported: (Playlist) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current

    val importStatus by repository.importStatus.collectAsState()
    var playlistUrl by remember { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) }

    val isImporting = importStatus.isImporting
    val progressPercent = if (importStatus.totalTracks > 0) {
        (importStatus.currentTrack.toFloat() / importStatus.totalTracks.toFloat()).coerceIn(0.05f, 1f)
    } else 0.1f

    val context = androidx.compose.ui.platform.LocalContext.current
    val errorMessage = localError ?: importStatus.errorMessage

    fun startImport(urlToUse: String) {
        val trimmed = urlToUse.trim()
        if (trimmed.isBlank()) {
            localError = "Please enter or paste a valid playlist link"
            return
        }
        val finalUrl = if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
            if (trimmed.contains("playlist/")) {
                "https://open.spotify.com/$trimmed"
            } else {
                "https://open.spotify.com/playlist/$trimmed"
            }
        } else {
            trimmed
        }
        localError = null
        playlistUrl = finalUrl
        repository.startBackgroundImport(finalUrl)
        android.widget.Toast.makeText(context, "Playlist import started! Running in background...", android.widget.Toast.LENGTH_SHORT).show()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SpotifyBlack,
        dragHandle = null,
        modifier = Modifier.testTag("spotify_import_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 20.dp)
        ) {
            // Drag handle pill
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFF404040))
                    .align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(SpotifyGreen),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = "Spring",
                            tint = Color.Black,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Import Spring Playlist",
                        color = SpotifyTextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = SpotifyTextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Paste any public playlist URL to parse songs, match streaming audio, and add it directly to Your Library.",
                color = SpotifyTextSecondary,
                fontSize = 13.sp,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Input field
            OutlinedTextField(
                value = playlistUrl,
                onValueChange = { playlistUrl = it },
                placeholder = {
                    Text(
                        text = "https://open.spotify.com/playlist/...",
                        color = SpotifyTextMuted,
                        fontSize = 13.sp
                    )
                },
                trailingIcon = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (playlistUrl.isNotBlank()) {
                            IconButton(onClick = { playlistUrl = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear",
                                    tint = SpotifyTextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        IconButton(onClick = {
                            val clip = clipboardManager.getText()?.text?.trim()
                            if (!clip.isNullOrBlank()) {
                                playlistUrl = clip
                                localError = null
                            } else {
                                android.widget.Toast.makeText(
                                    context,
                                    "Android clipboard is empty. Use emulator toolbar 📋 icon to paste, or tap sample playlists below!",
                                    android.widget.Toast.LENGTH_LONG
                                ).show()
                            }
                        }) {
                            Icon(
                                imageVector = Icons.Default.ContentPaste,
                                contentDescription = "Paste from clipboard",
                                tint = SpotifyGreen
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = SpotifyCardElevated,
                    unfocusedContainerColor = SpotifyCard,
                    focusedBorderColor = SpotifyGreen,
                    unfocusedBorderColor = Color(0xFF383838),
                    focusedTextColor = SpotifyTextPrimary,
                    unfocusedTextColor = SpotifyTextPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_spotify_url")
            )

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = errorMessage!!,
                    color = Color(0xFFE91429),
                    fontSize = 13.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // AI Studio Preview Clipboard Tip
            Card(
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF142419)),
                border = androidx.compose.foundation.BorderStroke(1.dp, SpotifyGreen.copy(alpha = 0.25f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = SpotifyGreen,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Laptop paste issue? Web emulators block direct Ctrl+V. Use the emulator sidebar 📋 (Clipboard) icon, type just the Playlist ID, or tap any preset below!",
                        color = SpotifyTextSecondary,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (importStatus.completedPlaylist != null) {
                val completed = importStatus.completedPlaylist!!
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(SpotifyCardElevated)
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = SpotifyGreen,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Playlist Imported Successfully!",
                        color = SpotifyTextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "${completed.title} • ${completed.songCount} tracks added to your Library",
                        color = SpotifyTextSecondary,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = {
                            onPlaylistImported(completed)
                            repository.dismissImportStatus()
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SpotifyGreen),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                    ) {
                        Text("Open Playlist", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            } else if (isImporting) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    LinearProgressIndicator(
                        progress = { progressPercent },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = SpotifyGreen,
                        trackColor = Color(0xFF383838)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (importStatus.totalTracks > 0) "${importStatus.currentTrack}/${importStatus.totalTracks} songs matched" else "Parsing playlist...",
                            color = SpotifyGreen,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${(progressPercent * 100).toInt()}%",
                            color = SpotifyTextSecondary,
                            fontSize = 12.sp
                        )
                    }

                    if (importStatus.trackTitle.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = importStatus.trackTitle,
                            color = SpotifyTextSecondary,
                            fontSize = 11.sp,
                            maxLines = 1
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = SpotifyCardElevated),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("btn_close_import_bg")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = null,
                            tint = SpotifyTextPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Close & Run in Background",
                            color = SpotifyTextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Import continues in the background; you can keep listening to music.",
                        color = SpotifyTextMuted,
                        fontSize = 11.sp
                    )
                }
            } else {
                Button(
                    onClick = { startImport(playlistUrl) },
                    enabled = playlistUrl.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SpotifyGreen,
                        disabledContainerColor = SpotifyGreen.copy(alpha = 0.4f)
                    ),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("btn_start_import")
                ) {
                    Text("Import Playlist", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Demo / Quick Try Suggestion Row
            Text(
                text = "Try with popular sample playlists:",
                color = SpotifyTextSecondary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(8.dp))

            val samplePlaylists = listOf(
                "Today's Top Hits" to "https://open.spotify.com/playlist/37i9dQZF1DXcBWIGoYBM5M",
                "Bollywood Butter" to "https://open.spotify.com/playlist/37i9dQZF1DX0XUfTFmNBRM",
                "Chill Lofi Beats" to "https://open.spotify.com/playlist/37i9dQZF1DXdLEN7aqioXM",
                "Global Top 50" to "https://open.spotify.com/playlist/37i9dQZEVXbMDoHDwVN2tF",
                "All Out 2010s" to "https://open.spotify.com/playlist/37i9dQZF1DX5Ejj0EkURtP"
            )

            samplePlaylists.forEach { (name, url) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .clickable {
                            playlistUrl = url
                            startImport(url)
                        }
                        .padding(vertical = 8.dp, horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(SpotifyCardElevated),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.MusicNote,
                                contentDescription = null,
                                tint = SpotifyGreen,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = name,
                            color = SpotifyTextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Text(
                        text = "Import",
                        color = SpotifyGreen,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
