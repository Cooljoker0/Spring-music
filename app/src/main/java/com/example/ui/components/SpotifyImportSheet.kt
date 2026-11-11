package com.example.ui.components

import android.widget.Toast
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
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
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val importStatus by repository.importStatus.collectAsState()

    // Unified input state: Handles both playlist name and URL
    var inputQuery by remember { mutableStateOf("") }
    var isSearchingPlaylists by remember { mutableStateOf(false) }
    var searchedPlaylists by remember { mutableStateOf<List<Playlist>>(emptyList()) }
    var hasSearched by remember { mutableStateOf(false) }
    var localError by remember { mutableStateOf<String?>(null) }

    val isImporting = importStatus.isImporting
    val progressPercent = if (importStatus.totalTracks > 0) {
        (importStatus.currentTrack.toFloat() / importStatus.totalTracks.toFloat()).coerceIn(0.05f, 1f)
    } else 0.1f

    val errorMessage = localError ?: importStatus.errorMessage

    fun isUrl(text: String): Boolean {
        val trimmed = text.trim()
        return trimmed.startsWith("http://", ignoreCase = true) ||
                trimmed.startsWith("https://", ignoreCase = true) ||
                trimmed.contains("spotify.com") ||
                trimmed.contains("spotify.link") ||
                trimmed.matches(Regex("^[a-zA-Z0-9]{22}$"))
    }

    fun submitQuery(query: String) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) {
            localError = "Please enter a playlist name or paste a link"
            return
        }
        keyboardController?.hide()
        localError = null

        if (isUrl(trimmed)) {
            val finalUrl = if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
                if (trimmed.contains("playlist/")) {
                    "https://open.spotify.com/$trimmed"
                } else {
                    "https://open.spotify.com/playlist/$trimmed"
                }
            } else {
                trimmed
            }
            repository.startBackgroundImport(finalUrl)
            Toast.makeText(context, "Playlist URL import started! Running in background...", Toast.LENGTH_SHORT).show()
        } else {
            // Search playlist by name
            scope.launch {
                isSearchingPlaylists = true
                hasSearched = true
                try {
                    val results = repository.searchPlaylists(trimmed)
                    searchedPlaylists = results
                } catch (e: Exception) {
                    searchedPlaylists = emptyList()
                    localError = "Failed to search playlists: ${e.message}"
                } finally {
                    isSearchingPlaylists = false
                }
            }
        }
    }

    // Live search as user types: automatically searches after 300ms pause
    androidx.compose.runtime.LaunchedEffect(inputQuery) {
        val trimmed = inputQuery.trim()
        if (trimmed.length >= 2 && !isUrl(trimmed)) {
            kotlinx.coroutines.delay(300)
            isSearchingPlaylists = true
            hasSearched = true
            localError = null
            try {
                val results = repository.searchPlaylists(trimmed)
                searchedPlaylists = results
            } catch (e: Exception) {
                searchedPlaylists = emptyList()
                localError = "Failed to search playlists: ${e.message}"
            } finally {
                isSearchingPlaylists = false
            }
        } else if (trimmed.isBlank()) {
            searchedPlaylists = emptyList()
            hasSearched = false
            isSearchingPlaylists = false
        }
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
                .verticalScroll(rememberScrollState())
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
                            .size(34.dp)
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
                    Column {
                        Text(
                            text = "Import Playlist",
                            color = SpotifyTextPrimary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Search by name or paste any playlist link",
                            color = SpotifyTextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = SpotifyTextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Single Unified Input Box
            OutlinedTextField(
                value = inputQuery,
                onValueChange = {
                    inputQuery = it
                    localError = null
                    if (it.isBlank()) {
                        searchedPlaylists = emptyList()
                        hasSearched = false
                    }
                },
                placeholder = {
                    Text(
                        text = "Search playlist name or paste link...",
                        color = SpotifyTextMuted,
                        fontSize = 13.sp
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = if (isUrl(inputQuery)) Icons.Default.ContentPaste else Icons.Default.Search,
                        contentDescription = "Search",
                        tint = SpotifyGreen,
                        modifier = Modifier.size(20.dp)
                    )
                },
                trailingIcon = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (inputQuery.isNotBlank()) {
                            IconButton(onClick = {
                                inputQuery = ""
                                searchedPlaylists = emptyList()
                                hasSearched = false
                                localError = null
                            }) {
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
                                inputQuery = clip
                                localError = null
                                submitQuery(clip)
                            } else {
                                Toast.makeText(
                                    context,
                                    "Clipboard is empty. Type a playlist name or use emulator 📋 to paste!",
                                    Toast.LENGTH_SHORT
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
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { submitQuery(inputQuery) }),
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
                    .testTag("input_unified_playlist")
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Action Button for URL or Name
            if (isUrl(inputQuery)) {
                Button(
                    onClick = { submitQuery(inputQuery) },
                    enabled = !isImporting,
                    colors = ButtonDefaults.buttonColors(containerColor = SpotifyGreen),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("btn_import_url")
                ) {
                    Text("Import Spotify / Web Link", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
                Spacer(modifier = Modifier.height(10.dp))
            } else if (inputQuery.isNotBlank() && !hasSearched) {
                Button(
                    onClick = { submitQuery(inputQuery) },
                    colors = ButtonDefaults.buttonColors(containerColor = SpotifyGreen),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("btn_search_submit")
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Search Playlists for \"$inputQuery\"", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Quick Topic Suggestion Chips
            val quickSuggestions = listOf(
                "Indie India",
                "Bollywood Butter",
                "Arijit Singh",
                "Top 50 Hits",
                "Chill Lo-Fi",
                "Workout Energy",
                "Punjabi 101",
                "Romantic Melodies",
                "Desi Hip Hop",
                "English Pop"
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(quickSuggestions) { chipName ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(SpotifyCardElevated)
                            .border(1.dp, Color(0xFF383838), RoundedCornerShape(16.dp))
                            .clickable {
                                inputQuery = chipName
                                submitQuery(chipName)
                            }
                            .padding(horizontal = 12.dp, vertical = 7.dp)
                    ) {
                        Text(
                            text = chipName,
                            color = SpotifyTextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Error display
            if (errorMessage != null) {
                Text(
                    text = errorMessage,
                    color = Color(0xFFE91429),
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Search Loading State
            if (isSearchingPlaylists) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(
                        color = SpotifyGreen,
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(34.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Searching playlists for \"$inputQuery\"...",
                        color = SpotifyTextSecondary,
                        fontSize = 13.sp
                    )
                }
            } else if (searchedPlaylists.isNotEmpty()) {
                // Search Results
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Matching Playlists (${searchedPlaylists.size})",
                        color = SpotifyTextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Tap + to import",
                        color = SpotifyTextMuted,
                        fontSize = 11.sp
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                searchedPlaylists.forEach { playlist ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 5.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(SpotifyCard)
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            if (playlist.coverUrl.isNotBlank()) {
                                AsyncImage(
                                    model = playlist.coverUrl,
                                    contentDescription = playlist.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(SpotifyCardElevated)
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(SpotifyCardElevated),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MusicNote,
                                        contentDescription = null,
                                        tint = SpotifyGreen,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                val isSpotify = playlist.id.startsWith("spotify_") ||
                                        playlist.id.startsWith("sp_") ||
                                        playlist.subtitle.contains("Spotify", ignoreCase = true)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (isSpotify) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(3.dp))
                                                .background(SpotifyGreen.copy(alpha = 0.2f))
                                                .padding(horizontal = 4.dp, vertical = 1.dp)
                                        ) {
                                            Text(
                                                text = "SPOTIFY",
                                                color = SpotifyGreen,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(6.dp))
                                    }
                                    Text(
                                        text = playlist.title,
                                        color = SpotifyTextPrimary,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (playlist.songCount > 0) "${playlist.songCount} songs • ${playlist.subtitle}" else playlist.subtitle.ifBlank { "Playlist" },
                                    color = SpotifyTextSecondary,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Button(
                            onClick = {
                                repository.startBackgroundImportPlaylist(playlist)
                                Toast.makeText(context, "Importing \"${playlist.title}\" in background...", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SpotifyGreen),
                            shape = RoundedCornerShape(20.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Import",
                                color = Color.Black,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
            } else if (hasSearched) {
                // Searched with 0 results
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(SpotifyCard)
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "No direct playlist matches found for \"$inputQuery\"",
                        color = SpotifyTextSecondary,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = {
                            repository.startBackgroundImportByName(inputQuery)
                            Toast.makeText(context, "Importing \"$inputQuery\" in background...", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SpotifyCardElevated),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text(
                            text = "Auto-Match Songs for \"$inputQuery\"",
                            color = SpotifyGreen,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
            } else {
                // Default View: Popular Sample Playlists with 1-Tap Import
                Text(
                    text = "Popular Playlists to Import:",
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
                                inputQuery = url
                                submitQuery(url)
                            }
                            .padding(vertical = 7.dp, horizontal = 4.dp),
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

                Spacer(modifier = Modifier.height(10.dp))

                // Tip card
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
                            text = "Type any playlist name, artist name, or paste a Spotify link directly in the box above to import it.",
                            color = SpotifyTextSecondary,
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
            }

            // Completed / Running Progress Status Card
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(SpotifyCardElevated)
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Importing: ${importStatus.playlistName.ifBlank { "Playlist" }}",
                        color = SpotifyTextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
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
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = SpotifyPitchBlack),
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
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
