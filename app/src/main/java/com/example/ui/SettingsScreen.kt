package com.example.ui

import android.app.Activity
import android.content.Context
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import com.example.updater.AppUpdateManager
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import com.example.data.local.MediaCacheManager
import com.example.ui.theme.AiThemeManager
import com.example.ui.theme.AppThemePreset
import com.example.ui.theme.SpotifyBlack
import com.example.ui.theme.SpotifyCard
import com.example.ui.theme.SpotifyCardElevated
import com.example.ui.theme.SpotifyGreen
import com.example.ui.theme.SpotifyTextMuted
import com.example.ui.theme.SpotifyTextPrimary
import com.example.ui.theme.SpotifyTextSecondary
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    currentQuality: String,
    onQualitySelected: (String) -> Unit,
    sleepTimerMinutes: Int?,
    onSetSleepTimer: (Int?) -> Unit,
    onBack: (() -> Unit)? = null,
    onOpenAiCustomizer: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    if (onBack != null) {
        BackHandler { onBack() }
    }

    val context = LocalContext.current
    val activity = context as? Activity
    val app = context.applicationContext as? SpringApplication
    val firestoreSyncManager = app?.firestoreSyncManager
    val scope = rememberCoroutineScope()
    val primaryColor = MaterialTheme.colorScheme.primary

    val currentUser by AuthManager.currentUser.collectAsState()
    val isAuthLoading by AuthManager.isLoading.collectAsState()
    val authError by AuthManager.errorMessage.collectAsState()
    val syncStatus by firestoreSyncManager?.syncStatus?.collectAsState() ?: remember { mutableStateOf("Ready") }
    val currentTheme by AiThemeManager.currentTheme.collectAsState()

    var cacheSizeText by remember { mutableStateOf("Calculating...") }
    val prefs = remember { context.getSharedPreferences("spring_prefs", Context.MODE_PRIVATE) }
    var stopOnTaskRemoved by remember {
        mutableStateOf(prefs.getBoolean("stop_on_task_removed", false))
    }
    var normalizeVolume by remember { mutableStateOf(true) }
    var automix by remember { mutableStateOf(true) }
    var carModeAuto by remember { mutableStateOf(true) }

    val isCheckingUpdates by AppUpdateManager.isChecking.collectAsState()
    val updateStatusMsg by AppUpdateManager.statusMessage.collectAsState()
    val updateAvailable by AppUpdateManager.updateAvailable.collectAsState()

    var showPusherCard by remember { mutableStateOf(false) }
    var pushVersionName by remember { mutableStateOf("9.5") }
    var pushVersionCode by remember { mutableStateOf((AppUpdateManager.currentVersionCode + 1).toString()) }
    var pushDownloadUrl by remember { mutableStateOf("") }
    var pushChangelog by remember { mutableStateOf("New features, bug fixes, and audio quality improvements.") }
    var skipUrlProbe by remember { mutableStateOf(false) }
    var isPushingUpdate by remember { mutableStateOf(false) }
    var pushFeedbackMsg by remember { mutableStateOf<String?>(null) }
    var isProbingUrl by remember { mutableStateOf(false) }
    var probeResultMsg by remember { mutableStateOf<String?>(null) }
    var isClearingBroadcast by remember { mutableStateOf(false) }

    var versionTapCount by remember { mutableStateOf(0) }
    var developerModeUnlocked by remember {
        mutableStateOf(prefs.getBoolean("developer_mode_unlocked", false))
    }
    val isAdmin = remember(currentUser, developerModeUnlocked) {
        val email = currentUser?.email?.lowercase()?.trim()
        email == "meta58583200@gmail.com" || developerModeUnlocked
    }

    fun refreshCacheSize() {
        scope.launch {
            cacheSizeText = MediaCacheManager.getCacheSizeFormatted(context)
        }
    }

    LaunchedEffect(Unit) {
        refreshCacheSize()
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(SpotifyBlack)
            .navigationBarsPadding(),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        // Spotify Top Header with Back button
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (onBack != null) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("btn_settings_back")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = SpotifyTextPrimary
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }

                Text(
                    text = "Settings",
                    color = SpotifyTextPrimary,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Section: Google Account & Cloud Backup
        item {
            SpotifySectionHeader("Google Account & Cloud Backup")

            if (currentUser != null) {
                // Signed In User Profile Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SpotifyCardElevated)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (!currentUser?.photoUrl?.toString().isNullOrBlank()) {
                                AsyncImage(
                                    model = currentUser?.photoUrl.toString(),
                                    contentDescription = "Profile Photo",
                                    modifier = Modifier
                                        .size(54.dp)
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(54.dp)
                                        .clip(CircleShape)
                                        .background(primaryColor),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = (currentUser?.displayName ?: currentUser?.email ?: "U").take(1).uppercase(),
                                        color = Color.Black,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 22.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = currentUser?.displayName ?: "Google User",
                                    color = SpotifyTextPrimary,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = currentUser?.email.orEmpty(),
                                    color = SpotifyTextSecondary,
                                    fontSize = 13.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Sync indicator
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CloudDone,
                                contentDescription = null,
                                tint = primaryColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Cloud Sync Active • Playlists & Settings Saved ($syncStatus)",
                                color = primaryColor,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = {
                                AuthManager.signOut(context, scope)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp)
                                .testTag("btn_google_sign_out"),
                            shape = RoundedCornerShape(20.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SpotifyCard,
                                contentColor = Color(0xFFFF5555)
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Logout,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Sign Out", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            } else {
                // Not Signed In Card with Google Sign-In Button
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SpotifyCardElevated)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(Color.White),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "G",
                                    color = Color(0xFF4285F4),
                                    fontWeight = FontWeight.Black,
                                    fontSize = 24.sp
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column {
                                Text(
                                    text = "Connect Google Account",
                                    color = SpotifyTextPrimary,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Cloud backup for playlists, liked tracks & themes",
                                    color = SpotifyTextSecondary,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        if (authError != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = authError!!,
                                color = Color(0xFFFF5555),
                                fontSize = 12.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = {
                                if (activity != null) {
                                    AuthManager.signInWithGoogle(activity, scope)
                                }
                            },
                            enabled = !isAuthLoading,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("btn_google_sign_in"),
                            shape = RoundedCornerShape(24.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = Color(0xFF1F1F1F)
                            )
                        ) {
                            if (isAuthLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color.Black,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text(
                                    text = "Sign in with Google",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // Section: App Theme & Appearance
        item {
            SpotifySectionHeader("Theme & Appearance")

            // Spotify Dark Option
            val isSpotifyDark = currentTheme.id == "spotify_dark"
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .clickable {
                        AiThemeManager.setPresetTheme(context, AppThemePreset.SPOTIFY_DARK)
                    }
                    .testTag("theme_option_spotify_dark"),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSpotifyDark) primaryColor.copy(alpha = 0.12f) else SpotifyCardElevated
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF121212)),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(SpotifyGreen)
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column {
                            Text(
                                text = "Spring Dark",
                                color = SpotifyTextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Pitch-black background & Spring Green accents",
                                color = SpotifyTextSecondary,
                                fontSize = 12.sp
                            )
                        }
                    }

                    if (isSpotifyDark) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Selected",
                            tint = primaryColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            // Apple Liquid Glass Option
            val isLiquidGlass = currentTheme.id == "apple_liquid_glass"
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .clickable {
                        AiThemeManager.setPresetTheme(context, AppThemePreset.APPLE_LIQUID_GLASS)
                    }
                    .testTag("theme_option_apple_liquid_glass"),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isLiquidGlass) Color(0xFFFA2D48).copy(alpha = 0.16f) else SpotifyCardElevated
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        listOf(Color(0x66FFFFFF), Color(0x22FFFFFF))
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFA2D48))
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column {
                            Text(
                                text = "Apple Liquid Glass",
                                color = SpotifyTextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Frosted translucent glass, crimson-rose & neon ambiance",
                                color = SpotifyTextSecondary,
                                fontSize = 12.sp
                            )
                        }
                    }

                    if (isLiquidGlass) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Selected",
                            tint = Color(0xFFFA2D48),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            // AI Theme Studio / Custom Prompt Option
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .clickable { onOpenAiCustomizer() }
                    .testTag("theme_option_ai_customizer"),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (currentTheme.isCustom) primaryColor.copy(alpha = 0.12f) else SpotifyCardElevated
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        listOf(primaryColor, currentTheme.accentColor)
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column {
                            Text(
                                text = "AI Prompt Customizer",
                                color = SpotifyTextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (currentTheme.isCustom) "Active: ${currentTheme.name}" else "Customize theme with text prompts",
                                color = if (currentTheme.isCustom) primaryColor else SpotifyTextSecondary,
                                fontSize = 12.sp,
                                fontWeight = if (currentTheme.isCustom) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                        contentDescription = "Customize",
                        tint = SpotifyTextSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }

        // Section: Audio Quality
        item {
            SpotifySectionHeader("Audio Quality")
            val qualities = listOf(
                "320kbps" to "Very High (320 kbit/s)",
                "160kbps" to "High (160 kbit/s)",
                "128kbps" to "Normal (128 kbit/s)",
                "96kbps" to "Low (96 kbit/s)"
            )

            qualities.forEach { (q, label) ->
                val isSelected = currentQuality.contains(q.replace("kbps", ""), ignoreCase = true)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onQualitySelected(q) }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = label,
                            color = if (isSelected) primaryColor else SpotifyTextPrimary,
                            fontSize = 15.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }

                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Selected",
                            tint = primaryColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // Section: Playback (including stop playback when removed from recent section)
        item {
            SpotifySectionHeader("Playback")

            SpotifyToggleRow(
                title = "Stop on App Close (Recents)",
                subtitle = "Stop song when app is removed from recent apps section",
                checked = stopOnTaskRemoved,
                onCheckedChange = { newValue ->
                    stopOnTaskRemoved = newValue
                    prefs.edit().putBoolean("stop_on_task_removed", newValue).apply()
                    scope.launch {
                        firestoreSyncManager?.saveSettings(stopOnTaskRemoved = newValue)
                    }
                },
                accentColor = primaryColor
            )

            SpotifyToggleRow(
                title = "Automix",
                subtitle = "Allows seamless transitions between songs",
                checked = automix,
                onCheckedChange = { automix = it },
                accentColor = primaryColor
            )

            SpotifyToggleRow(
                title = "Normalize volume",
                subtitle = "Set the same volume level for all tracks",
                checked = normalizeVolume,
                onCheckedChange = { normalizeVolume = it },
                accentColor = primaryColor
            )

            // Sleep Timer
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Sleep timer",
                        color = SpotifyTextPrimary,
                        fontSize = 15.sp
                    )
                    Text(
                        text = if (sleepTimerMinutes != null) "Stops in $sleepTimerMinutes min" else "Turn off audio after a set time",
                        color = if (sleepTimerMinutes != null) primaryColor else SpotifyTextSecondary,
                        fontSize = 12.sp
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(null to "Off", 15 to "15m", 30 to "30m", 60 to "1h").forEach { (min, lbl) ->
                        val isSel = sleepTimerMinutes == min
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (isSel) primaryColor else SpotifyCardElevated)
                                .clickable { onSetSleepTimer(min) }
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = lbl,
                                color = if (isSel) Color.Black else SpotifyTextPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Section: Storage
        item {
            SpotifySectionHeader("Storage & Offline Cache")

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Offline Cached Audio",
                        color = SpotifyTextPrimary,
                        fontSize = 15.sp
                    )
                    Text(
                        text = cacheSizeText,
                        color = primaryColor,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Button(
                    onClick = {
                        scope.launch {
                            MediaCacheManager.clearCache(context)
                            refreshCacheSize()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SpotifyCardElevated),
                    shape = RoundedCornerShape(20.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Text("Clear cache", color = SpotifyTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Section: In-App Updates
        item {
            SpotifySectionHeader(if (isAdmin) "App Updates & OTA Pusher (Admin)" else "App Updates")

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SpotifyCardElevated)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(primaryColor.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SystemUpdate,
                                    contentDescription = null,
                                    tint = primaryColor,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(
                                modifier = Modifier.clickable {
                                    if (!isAdmin) {
                                        versionTapCount++
                                        if (versionTapCount >= 5) {
                                            developerModeUnlocked = true
                                            prefs.edit().putBoolean("developer_mode_unlocked", true).apply()
                                            android.widget.Toast.makeText(context, "Admin OTA Pusher unlocked!", android.widget.Toast.LENGTH_SHORT).show()
                                        } else if (versionTapCount >= 2) {
                                            android.widget.Toast.makeText(context, "${5 - versionTapCount} more taps to unlock Pusher", android.widget.Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            ) {
                                Text(
                                    text = "Spring Music v${AppUpdateManager.currentVersionName}",
                                    color = SpotifyTextPrimary,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (isAdmin) "Build ${AppUpdateManager.currentVersionCode} • Admin Pusher Active" else "Build ${AppUpdateManager.currentVersionCode}",
                                    color = SpotifyTextSecondary,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        Button(
                            onClick = {
                                AppUpdateManager.checkForUpdates(context, isManualCheck = true)
                            },
                            enabled = !isCheckingUpdates,
                            shape = RoundedCornerShape(20.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = primaryColor,
                                contentColor = Color.Black
                            ),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("btn_check_updates")
                        ) {
                            if (isCheckingUpdates) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = Color.Black,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text(
                                    text = "Check",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }

                    if (!updateStatusMsg.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        val isErr = updateStatusMsg!!.contains("Error", ignoreCase = true) ||
                                updateStatusMsg!!.contains("404") ||
                                updateStatusMsg!!.contains("failed", ignoreCase = true)
                        Text(
                            text = updateStatusMsg!!,
                            color = if (isErr) Color(0xFFFF5555) else primaryColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // For regular users: If an update is available, show Install & Dismiss buttons
                    if (updateAvailable != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    AppUpdateManager.downloadAndInstall(context, updateAvailable!!)
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp),
                                shape = RoundedCornerShape(22.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = primaryColor,
                                    contentColor = Color.Black
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SystemUpdate,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Install v${updateAvailable!!.versionName}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }

                            Button(
                                onClick = {
                                    AppUpdateManager.dismissUpdate()
                                },
                                modifier = Modifier.height(44.dp),
                                shape = RoundedCornerShape(22.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = SpotifyCard,
                                    contentColor = SpotifyTextSecondary
                                )
                            ) {
                                Text("Dismiss", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }

                    // ADMIN ONLY: OTA Update Pusher Controls
                    if (isAdmin) {
                        Spacer(modifier = Modifier.height(14.dp))

                        // OTA Update Pusher Toggle
                        Button(
                            onClick = { showPusherCard = !showPusherCard },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(42.dp)
                                .testTag("btn_toggle_ota_pusher"),
                            shape = RoundedCornerShape(21.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SpotifyCard,
                                contentColor = SpotifyTextPrimary
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Upload,
                                contentDescription = null,
                                tint = primaryColor,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (showPusherCard) "Hide Admin Update Pusher" else "Open Admin Update Pusher",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                    // Expandable OTA Pusher Form
                    if (showPusherCard) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(SpotifyCard)
                                .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(12.dp))
                                .padding(14.dp)
                        ) {
                            Text(
                                text = "Push Update to All Installed Phones",
                                color = SpotifyTextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Broadcast updates directly from here. Phones with Spring installed will receive the update dialog instantly over Wi-Fi/cellular without connecting to a computer.",
                                color = SpotifyTextSecondary,
                                fontSize = 11.sp,
                                lineHeight = 16.sp
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Version name & code row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = pushVersionName,
                                    onValueChange = { pushVersionName = it },
                                    label = { Text("Version Name", fontSize = 11.sp) },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = Color(0xFF1E1E1E),
                                        unfocusedContainerColor = Color(0xFF1E1E1E),
                                        focusedTextColor = SpotifyTextPrimary,
                                        unfocusedTextColor = SpotifyTextPrimary
                                    )
                                )
                                OutlinedTextField(
                                    value = pushVersionCode,
                                    onValueChange = { pushVersionCode = it },
                                    label = { Text("Build Code", fontSize = 11.sp) },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = Color(0xFF1E1E1E),
                                        unfocusedContainerColor = Color(0xFF1E1E1E),
                                        focusedTextColor = SpotifyTextPrimary,
                                        unfocusedTextColor = SpotifyTextPrimary
                                    )
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = pushDownloadUrl,
                                onValueChange = {
                                    pushDownloadUrl = it
                                    probeResultMsg = null
                                },
                                label = { Text("APK Download URL", fontSize = 11.sp) },
                                placeholder = { Text("e.g. Google Drive link or GitHub release APK", fontSize = 11.sp) },
                                modifier = Modifier.fillMaxWidth(),
                                trailingIcon = {
                                    val clipboard = androidx.compose.ui.platform.LocalClipboardManager.current
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (pushDownloadUrl.isNotBlank()) {
                                            IconButton(onClick = { pushDownloadUrl = "" }) {
                                                Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = "Clear",
                                                    tint = SpotifyTextSecondary,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                        IconButton(onClick = {
                                            val clip = clipboard.getText()?.text?.trim()
                                            if (!clip.isNullOrBlank()) {
                                                pushDownloadUrl = clip
                                                probeResultMsg = null
                                            } else {
                                                android.widget.Toast.makeText(
                                                    context,
                                                    "Android clipboard empty. Use emulator toolbar 📋 or tap presets below",
                                                    android.widget.Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        }) {
                                            Icon(
                                                imageVector = Icons.Default.ContentPaste,
                                                contentDescription = "Paste",
                                                tint = primaryColor
                                            )
                                        }
                                    }
                                },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = Color(0xFF1E1E1E),
                                    unfocusedContainerColor = Color(0xFF1E1E1E),
                                    focusedTextColor = SpotifyTextPrimary,
                                    unfocusedTextColor = SpotifyTextPrimary
                                )
                            )

                            if (pushDownloadUrl.contains("drive.google.com") || pushDownloadUrl.contains("drive.usercontent")) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Card(
                                    shape = RoundedCornerShape(8.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2822)),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, primaryColor.copy(alpha = 0.3f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Info,
                                            contentDescription = null,
                                            tint = primaryColor,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Google Drive Tip: Ensure 'Anyone with the link' is selected under Share -> General access in Google Drive. If set to Restricted, other devices will get an owner permission error.",
                                            color = SpotifyTextSecondary,
                                            fontSize = 11.sp,
                                            lineHeight = 15.sp
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // Test Link Button
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Button(
                                    onClick = {
                                        if (pushDownloadUrl.isBlank()) {
                                            probeResultMsg = "Please enter a URL first"
                                            return@Button
                                        }
                                        isProbingUrl = true
                                        probeResultMsg = "Testing download link..."
                                        scope.launch {
                                            val res = AppUpdateManager.probeUrl(pushDownloadUrl)
                                            isProbingUrl = false
                                            probeResultMsg = res.second
                                        }
                                    },
                                    enabled = !isProbingUrl,
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF2E2E2E),
                                        contentColor = SpotifyTextPrimary
                                    ),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    modifier = Modifier.height(34.dp)
                                ) {
                                    if (isProbingUrl) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(14.dp),
                                            color = primaryColor,
                                            strokeWidth = 2.dp
                                        )
                                    } else {
                                        Text("Test Link", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                Text(
                                    text = "Verifies before broadcasting",
                                    color = SpotifyTextSecondary,
                                    fontSize = 11.sp
                                )
                            }

                            if (!probeResultMsg.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                val isProbeErr = probeResultMsg!!.contains("404") ||
                                        probeResultMsg!!.contains("failed", ignoreCase = true) ||
                                        probeResultMsg!!.contains("Invalid")
                                Text(
                                    text = probeResultMsg!!,
                                    color = if (isProbeErr) Color(0xFFFF6666) else primaryColor,
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Informational Guide Card on Google Drive & GitHub
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF141E18)),
                                border = androidx.compose.foundation.BorderStroke(1.dp, primaryColor.copy(alpha = 0.35f))
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = "Which APK Link is Needed to Push Updates?",
                                        color = primaryColor,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "• MUST USE app-debug.apk:\n" +
                                                "  Android requires matching signatures to update an installed app. Since this app was installed with the debug key, pushing release.apk will trigger 'App not installed / Signature mismatch'. Always upload and push app-debug.apk.\n\n" +
                                                "• GitHub Link Format:\n" +
                                                "  Must be the direct asset download URL from GitHub Releases:\n" +
                                                "  https://github.com/<owner>/<repo>/releases/download/<tag>/app-debug.apk\n\n" +
                                                "• Public Repo Required:\n" +
                                                "  Your GitHub repository MUST be Public. Private repos block anonymous downloads and return HTTP 404 to user devices.\n\n" +
                                                "• Google Drive Alternative:\n" +
                                                "  Upload app-debug.apk to Google Drive -> Share -> 'Anyone with the link can view' -> Paste link. Auto-direct download is supported!",
                                        color = SpotifyTextSecondary,
                                        fontSize = 11.sp,
                                        lineHeight = 15.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedTextField(
                                value = pushChangelog,
                                onValueChange = { pushChangelog = it },
                                label = { Text("Changelog / Release Notes", fontSize = 11.sp) },
                                modifier = Modifier.fillMaxWidth(),
                                maxLines = 3,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = Color(0xFF1E1E1E),
                                    unfocusedContainerColor = Color(0xFF1E1E1E),
                                    focusedTextColor = SpotifyTextPrimary,
                                    unfocusedTextColor = SpotifyTextPrimary
                                )
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { skipUrlProbe = !skipUrlProbe }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = skipUrlProbe,
                                    onCheckedChange = { skipUrlProbe = it },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = primaryColor,
                                        checkmarkColor = Color.Black,
                                        uncheckedColor = SpotifyTextSecondary
                                    )
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Skip link reachability probe (Force Broadcast)",
                                    color = SpotifyTextPrimary,
                                    fontSize = 11.sp
                                )
                            }

                            if (pushFeedbackMsg != null) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = pushFeedbackMsg!!,
                                    color = if (pushFeedbackMsg!!.startsWith("✓")) primaryColor else Color(0xFFFF5555),
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Button(
                                onClick = {
                                    val code = pushVersionCode.toIntOrNull() ?: (AppUpdateManager.currentVersionCode + 1)
                                    if (pushDownloadUrl.isBlank()) {
                                        pushFeedbackMsg = "Please enter an APK download link"
                                        return@Button
                                    }
                                    isPushingUpdate = true
                                    pushFeedbackMsg = "Testing link & broadcasting update..."
                                    scope.launch {
                                        val result = AppUpdateManager.pushUpdate(
                                            versionCode = code,
                                            versionName = pushVersionName,
                                            downloadUrl = pushDownloadUrl,
                                            changelog = pushChangelog,
                                            skipUrlProbe = skipUrlProbe
                                        )
                                        isPushingUpdate = false
                                        pushFeedbackMsg = result.fold(
                                            onSuccess = { "✓ Update v$pushVersionName pushed! All phones will prompt to install." },
                                            onFailure = { it.message ?: "Failed to push update. Check your network or URL." }
                                        )
                                    }
                                },
                                enabled = !isPushingUpdate,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                                    .testTag("btn_broadcast_ota_update"),
                                shape = RoundedCornerShape(22.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = primaryColor,
                                    contentColor = Color.Black
                                )
                            ) {
                                if (isPushingUpdate) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        color = Color.Black,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Upload,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Broadcast Update to All Devices",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Button to clear broken update from cloud
                            Button(
                                onClick = {
                                    isClearingBroadcast = true
                                    scope.launch {
                                        val res = AppUpdateManager.clearPushedUpdate()
                                        isClearingBroadcast = false
                                        pushFeedbackMsg = res.fold(
                                            onSuccess = { "✓ Cloud update broadcast cleared. Phones will no longer prompt for update." },
                                            onFailure = { "Failed to clear update: ${it.message}" }
                                        )
                                    }
                                },
                                enabled = !isClearingBroadcast,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(38.dp),
                                shape = RoundedCornerShape(19.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF332020),
                                    contentColor = Color(0xFFFF9999)
                                )
                            ) {
                                if (isClearingBroadcast) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(14.dp),
                                        color = Color(0xFFFF9999),
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Text(
                                        text = "Clear Current Cloud Broadcast (Fix 404 Prompt)",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

        // About
        item {
            SpotifySectionHeader("About")
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text(
                    text = "Spring Music v${AppUpdateManager.currentVersionName}",
                    color = SpotifyTextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Equipped with Google Cloud Sync, AI Theme Studio, Playlist Import, and Android Auto.",
                    color = SpotifyTextMuted,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun SpotifySectionHeader(title: String) {
    Text(
        text = title,
        color = SpotifyTextPrimary,
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 4.dp)
    )
}

@Composable
private fun SpotifyToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    accentColor: Color = SpotifyGreen
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = SpotifyTextPrimary,
                fontSize = 15.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                color = SpotifyTextSecondary,
                fontSize = 12.sp
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = accentColor,
                uncheckedThumbColor = Color(0xFFB3B3B3),
                uncheckedTrackColor = Color(0xFF383838),
                uncheckedBorderColor = Color.Transparent
            )
        )
    }
}
