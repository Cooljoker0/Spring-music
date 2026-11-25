package com.example.ui.components

import android.widget.Toast
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import com.example.ui.theme.DarkGlass
import com.example.ui.theme.DeepBlack
import com.example.ui.theme.DestructiveRose
import com.example.ui.theme.DisplayFontFamily
import com.example.ui.theme.Graphite
import com.example.ui.theme.LocalAppCustomTheme
import com.example.ui.theme.MidnightBlack
import com.example.ui.theme.MutedGrey
import com.example.ui.theme.Silver
import com.example.ui.theme.SoftWhite
import com.example.ui.theme.WhiteHighlight
import com.example.ui.theme.WhiteHighlightSoft
import kotlinx.coroutines.delay
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
    val theme = LocalAppCustomTheme.current

    val importStatus by repository.importStatus.collectAsState()

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

    LaunchedEffect(inputQuery) {
        val trimmed = inputQuery.trim()
        if (trimmed.length >= 2 && !isUrl(trimmed)) {
            delay(300)
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
        containerColor = DeepBlack,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        modifier = Modifier.testTag("spotify_import_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Import Playlist",
                        fontFamily = DisplayFontFamily,
                        color = SoftWhite,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Search by name or paste any playlist link",
                        color = Silver,
                        fontSize = 12.sp
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Silver
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

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
                        color = MutedGrey,
                        fontSize = 13.sp
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = if (isUrl(inputQuery)) Icons.Default.ContentPaste else Icons.Default.Search,
                        contentDescription = "Search",
                        tint = SoftWhite,
                        modifier = Modifier.size(20.dp)
                    )
                },
                trailingIcon = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (inputQuery.isNotBlank()) {
                            IconButton(
                                onClick = {
                                    inputQuery = ""
                                    searchedPlaylists = emptyList()
                                    hasSearched = false
                                    localError = null
                                },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear",
                                    tint = Silver,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        IconButton(
                            onClick = {
                                val clip = clipboardManager.getText()?.text?.trim()
                                if (!clip.isNullOrBlank()) {
                                    inputQuery = clip
                                    localError = null
                                    submitQuery(clip)
                                } else {
                                    Toast.makeText(
                                        context,
                                        "Clipboard is empty. Type a playlist name or paste a link.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentPaste,
                                contentDescription = "Paste from clipboard",
                                tint = SoftWhite
                            )
                        }
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { submitQuery(inputQuery) }),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Graphite,
                    unfocusedContainerColor = DarkGlass,
                    focusedBorderColor = WhiteHighlight,
                    unfocusedBorderColor = WhiteHighlightSoft,
                    focusedTextColor = SoftWhite,
                    unfocusedTextColor = SoftWhite,
                    cursorColor = SoftWhite
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_unified_playlist")
            )

            Spacer(modifier = Modifier.height(10.dp))

            if (isUrl(inputQuery)) {
                Button(
                    onClick = { submitQuery(inputQuery) },
                    enabled = !isImporting,
                    colors = ButtonDefaults.buttonColors(containerColor = SoftWhite, contentColor = MidnightBlack),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("btn_import_url")
                ) {
                    Text("Import Playlist Link", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
                Spacer(modifier = Modifier.height(10.dp))
            } else if (inputQuery.isNotBlank() && !hasSearched) {
                Button(
                    onClick = { submitQuery(inputQuery) },
                    colors = ButtonDefaults.buttonColors(containerColor = SoftWhite, contentColor = MidnightBlack),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("btn_search_submit")
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Search Playlists for \"$inputQuery\"", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

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
                            .heightIn(min = 42.dp)
                            .clip(RoundedCornerShape(50))
                            .background(DarkGlass)
                            .border(0.7.dp, WhiteHighlightSoft, RoundedCornerShape(50))
                            .clickable {
                                inputQuery = chipName
                                submitQuery(chipName)
                            }
                            .padding(horizontal = 14.dp, vertical = 9.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = chipName,
                            color = SoftWhite,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (errorMessage != null) {
                Text(
                    text = errorMessage,
                    color = DestructiveRose,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (isSearchingPlaylists) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(
                        color = SoftWhite,
                        strokeWidth = 2.5.dp,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Searching playlists for \"$inputQuery\"...",
                        color = Silver,
                        fontSize = 13.sp
                    )
                }
            } else if (searchedPlaylists.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Matching Playlists (${searchedPlaylists.size})",
                        fontFamily = DisplayFontFamily,
                        color = SoftWhite,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                searchedPlaylists.forEachIndexed { idx, playlist ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
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
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Graphite)
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Graphite),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MusicNote,
                                        contentDescription = null,
                                        tint = Silver,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = playlist.title,
                                    color = SoftWhite,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (playlist.songCount > 0) "${playlist.songCount} songs • ${playlist.subtitle}" else playlist.subtitle.ifBlank { "Playlist" },
                                    color = Silver,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Button(
                            onClick = {
                                repository.startBackgroundImportPlaylist(playlist)
                                Toast.makeText(context, "Importing \"${playlist.title}\" in background...", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SoftWhite, contentColor = MidnightBlack),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp),
                            modifier = Modifier.height(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Import",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    if (idx < searchedPlaylists.lastIndex) {
                        HorizontalDivider(color = WhiteHighlightSoft)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
            } else if (hasSearched) {
                LiquidGlassSurface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    elevation = 4.dp
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No direct playlist matches found for \"$inputQuery\"",
                            color = Silver,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = {
                                repository.startBackgroundImportByName(inputQuery)
                                Toast.makeText(context, "Importing \"$inputQuery\" in background...", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SoftWhite, contentColor = MidnightBlack),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.height(48.dp)
                        ) {
                            Text(
                                text = "Auto-Match Songs for \"$inputQuery\"",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
            } else {
                Text(
                    text = "Popular Playlists",
                    color = Silver,
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

                samplePlaylists.forEachIndexed { idx, (name, url) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 54.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                inputQuery = url
                                submitQuery(url)
                            }
                            .padding(vertical = 10.dp, horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Graphite)
                                    .border(0.6.dp, WhiteHighlightSoft, RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MusicNote,
                                    contentDescription = null,
                                    tint = SoftWhite,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = name,
                                color = SoftWhite,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = SoftWhite,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Import",
                                color = SoftWhite,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    if (idx < samplePlaylists.lastIndex) {
                        HorizontalDivider(color = WhiteHighlightSoft)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Graphite)
                        .border(0.6.dp, WhiteHighlightSoft, RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = Silver,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Search any playlist or paste a Spotify URL above to build it in your Library.",
                        color = Silver,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
            }

            if (importStatus.completedPlaylist != null) {
                val completed = importStatus.completedPlaylist!!
                LiquidGlassSurface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    elevation = 4.dp
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = SoftWhite,
                            modifier = Modifier.size(34.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Playlist Imported",
                            fontFamily = DisplayFontFamily,
                            color = SoftWhite,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${completed.title} • ${completed.songCount} tracks added",
                            color = Silver,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = {
                                onPlaylistImported(completed)
                                repository.dismissImportStatus()
                                onDismiss()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SoftWhite, contentColor = MidnightBlack),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            Text("Open Playlist", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else if (isImporting) {
                LiquidGlassSurface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    elevation = 4.dp
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Importing ${importStatus.playlistName.ifBlank { "Playlist" }}",
                            fontFamily = DisplayFontFamily,
                            color = SoftWhite,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        LinearProgressIndicator(
                            progress = { progressPercent },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(5.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = SoftWhite,
                            trackColor = Graphite
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = if (importStatus.totalTracks > 0) "${importStatus.currentTrack}/${importStatus.totalTracks} songs" else "Reading playlist...",
                                color = SoftWhite,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "${(progressPercent * 100).toInt()}%",
                                color = Silver,
                                fontSize = 12.sp
                            )
                        }

                        if (importStatus.trackTitle.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = importStatus.trackTitle,
                                color = Silver,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        OutlinedButton(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, WhiteHighlight),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("btn_close_import_bg")
                        ) {
                            Text(
                                text = "Continue in Background",
                                color = SoftWhite,
                                fontWeight = FontWeight.Medium,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
