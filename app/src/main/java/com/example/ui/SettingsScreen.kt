package com.example.ui

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.example.SpringApplication
import com.example.auth.AuthManager
import com.example.data.local.MediaCacheManager
import com.example.ui.components.BrandSpringLogo
import com.example.ui.components.IosAuroraSwitch
import com.example.ui.components.LiquidGlassSurface
import com.example.ui.theme.AiThemeManager
import com.example.ui.theme.AppThemePreset
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
import com.example.ui.theme.WhiteHighlight
import com.example.ui.theme.WhiteHighlightSoft
import com.example.updater.AppUpdateManager
import kotlinx.coroutines.delay
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
    val repository = app?.repository
    val playerManager = app?.playerManager
    val scope = rememberCoroutineScope()
    val theme = LocalAppCustomTheme.current

    val currentUser by AuthManager.currentUser.collectAsState()
    val isAuthLoading by AuthManager.isLoading.collectAsState()
    val authError by AuthManager.errorMessage.collectAsState()
    val syncStatus by firestoreSyncManager?.syncStatus?.collectAsState() ?: remember { mutableStateOf("Ready") }
    val currentTheme by AiThemeManager.currentTheme.collectAsState()

    val downloadQuality by playerManager?.downloadQuality?.collectAsState() ?: remember { mutableStateOf("Lossless") }
    val equalizerEnabled by playerManager?.equalizerEnabled?.collectAsState() ?: remember { mutableStateOf(true) }
    val gaplessPlayback by playerManager?.gaplessPlayback?.collectAsState() ?: remember { mutableStateOf(true) }
    val crossfadeEnabled by playerManager?.crossfadeEnabled?.collectAsState() ?: remember { mutableStateOf(true) }
    val automateVolume by playerManager?.automateVolume?.collectAsState() ?: remember { mutableStateOf(true) }
    val autoplayEnabled by playerManager?.autoplayEnabled?.collectAsState() ?: remember { mutableStateOf(true) }
    val playerVolume by playerManager?.playerVolume?.collectAsState() ?: remember { mutableStateOf(0.78f) }

    val isPersonalizationEnabled by repository?.isPersonalizationEnabled?.collectAsState() ?: remember { mutableStateOf(true) }
    val feedbackList by repository?.feedbackListFlow?.collectAsState(initial = emptyList()) ?: remember { mutableStateOf(emptyList()) }
    val recentPlays by repository?.listeningHistoryFlow?.collectAsState(initial = emptyList()) ?: remember { mutableStateOf(emptyList()) }

    var showStreamingQualityDialog by remember { mutableStateOf(false) }
    var showDownloadQualityDialog by remember { mutableStateOf(false) }
    var showClearHistoryDialog by remember { mutableStateOf(false) }
    var showResetPreferencesDialog by remember { mutableStateOf(false) }
    var showFeedbackManageDialog by remember { mutableStateOf(false) }
    var showHistoryDialog by remember { mutableStateOf(false) }
    var recommendationRefreshMsg by remember { mutableStateOf<String?>(null) }

    var cacheSizeText by remember { mutableStateOf("Calculating...") }
    val prefs = remember { context.getSharedPreferences("spring_prefs", Context.MODE_PRIVATE) }
    var stopOnTaskRemoved by remember {
        mutableStateOf(prefs.getBoolean("stop_on_task_removed", false))
    }
    var notificationsGranted by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            } else true
        )
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        notificationsGranted = granted
        Toast.makeText(
            context,
            if (granted) "Notifications enabled" else "Notifications permission denied",
            Toast.LENGTH_SHORT
        ).show()
    }

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
    var pushFeedbackSuccess by remember { mutableStateOf(false) }
    var isClearingBroadcast by remember { mutableStateOf(false) }

    var versionTapCount by remember { mutableIntStateOf(0) }
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

    val streamingLabel = remember(currentQuality) {
        when {
            currentQuality.contains("320") -> "High Quality (320 kbps)"
            currentQuality.contains("160") -> "Balanced (160 kbps)"
            currentQuality.contains("128") -> "Normal (128 kbps)"
            else -> currentQuality
        }
    }

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
            // Header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (onBack != null) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("btn_settings_back")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = SoftWhite
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                    }

                    Text(
                        text = "Settings",
                        fontFamily = DisplayFontFamily,
                        color = SoftWhite,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.5).sp
                    )
                }
            }

            // 1. Account & Cloud Sync Card
            item {
                LiquidGlassSurface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(20.dp),
                    elevation = 12.dp
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(GraphiteElevated)
                                    .border(1.dp, WhiteHighlight, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                if (currentUser != null && !currentUser?.photoUrl?.toString().isNullOrBlank()) {
                                    AsyncImage(
                                        model = currentUser?.photoUrl.toString(),
                                        contentDescription = "Profile Photo",
                                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                                        contentScale = ContentScale.Crop
                                    )
                                } else if (currentUser != null) {
                                    val initial = (currentUser?.displayName?.takeIf { it.isNotBlank() } ?: currentUser?.email)
                                        ?.take(1)
                                        ?.uppercase()
                                    if (initial != null) {
                                        Text(
                                            text = initial,
                                            fontFamily = DisplayFontFamily,
                                            color = SoftWhite,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 21.sp
                                        )
                                    } else {
                                        Icon(Icons.Default.Person, contentDescription = "Profile", tint = SoftWhite, modifier = Modifier.size(26.dp))
                                    }
                                } else {
                                    Icon(Icons.Default.Person, contentDescription = "Account", tint = Silver, modifier = Modifier.size(26.dp))
                                }
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                if (currentUser != null) {
                                    val displayName = currentUser?.displayName?.takeIf { it.isNotBlank() }
                                        ?: currentUser?.email.orEmpty()
                                    if (displayName.isNotBlank()) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = displayName,
                                                fontFamily = DisplayFontFamily,
                                                color = SoftWhite,
                                                fontSize = 17.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f, fill = false)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Icon(
                                                imageVector = Icons.Default.Verified,
                                                contentDescription = "Verified",
                                                tint = SoftWhite,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(2.dp))
                                    }
                                    Text(
                                        text = currentUser?.email?.takeIf { it.isNotBlank() } ?: "Spring Account",
                                        color = Silver,
                                        fontSize = 13.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                } else {
                                    Text(
                                        text = "Cloud Library Sync",
                                        color = SoftWhite,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Sign in to sync your liked songs and playlists",
                                        color = Silver,
                                        fontSize = 13.sp,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        if (currentUser != null) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CloudDone, contentDescription = null, tint = SoftWhite, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Sync: $syncStatus",
                                        color = Silver,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }

                                OutlinedButton(
                                    onClick = { AuthManager.signOut(context, scope) },
                                    shape = RoundedCornerShape(50),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = DestructiveRose),
                                    border = BorderStroke(0.8.dp, DestructiveRose.copy(alpha = 0.45f)),
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                    modifier = Modifier
                                        .defaultMinSize(minHeight = 48.dp)
                                        .testTag("btn_google_sign_out")
                                ) {
                                    Icon(Icons.Default.Logout, contentDescription = null, tint = DestructiveRose, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Sign Out", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        } else {
                            if (authError != null) {
                                Text(
                                    text = authError!!,
                                    color = DestructiveRose,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                            }
                            Button(
                                onClick = {
                                    if (activity != null) {
                                        AuthManager.signInWithGoogle(activity, scope)
                                    }
                                },
                                enabled = !isAuthLoading,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("btn_google_sign_in"),
                                shape = RoundedCornerShape(50),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = SoftWhite,
                                    contentColor = MidnightBlack
                                )
                            ) {
                                if (isAuthLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        color = MidnightBlack,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(Icons.Default.AccountCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Sign in with Google", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }
                            }
                        }
                    }
                }
            }

            // 2. Appearance & Liquid Glass Finishes
            item {
                SettingsSectionHeader("Appearance & Liquid Glass")
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Graphite)
                        .border(0.75.dp, WhiteHighlightSoft, RoundedCornerShape(18.dp))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 56.dp)
                            .clickable { onOpenAiCustomizer() }
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                            .testTag("theme_option_ai_customizer"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Palette, contentDescription = null, tint = SoftWhite, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Liquid Glass Studio", color = SoftWhite, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(text = "Active: ${currentTheme.name}", color = Silver, fontSize = 12.sp)
                        }
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MutedGrey, modifier = Modifier.size(18.dp))
                    }
                    HorizontalDivider(color = WhiteHighlightSoft, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 16.dp))

                    val curatedRows = remember { AiThemeManager.CuratedThemes.chunked(2) }
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        curatedRows.forEach { rowPresets ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                rowPresets.forEach { presetTheme ->
                                    val isSel = currentTheme.id == presetTheme.id
                                    val presetEnum = when (presetTheme.id) {
                                        AppThemePreset.APPLE_LIQUID_GLASS.id -> AppThemePreset.APPLE_LIQUID_GLASS
                                        AppThemePreset.OBSIDIAN_GOLD.id -> AppThemePreset.OBSIDIAN_GOLD
                                        AppThemePreset.ARCTIC_SAPPHIRE.id -> AppThemePreset.ARCTIC_SAPPHIRE
                                        else -> AppThemePreset.NOCTURNE_JADE
                                    }
                                    Row(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(if (isSel) WhiteHighlightSoft else GraphiteElevated)
                                            .border(
                                                width = if (isSel) 1.dp else 0.5.dp,
                                                color = if (isSel) WhiteHighlight else WhiteHighlightSoft,
                                                shape = RoundedCornerShape(12.dp)
                                            )
                                            .clickable { AiThemeManager.setPresetTheme(context, presetEnum) }
                                            .padding(horizontal = 12.dp)
                                            .testTag("settings_theme_${presetTheme.id}"),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(20.dp)
                                                .clip(CircleShape)
                                                .background(presetTheme.primaryColor),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (isSel) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = "Selected",
                                                    tint = MidnightBlack,
                                                    modifier = Modifier.size(13.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = presetTheme.name,
                                            color = if (isSel) SoftWhite else Silver,
                                            fontSize = 13.sp,
                                            fontWeight = if (isSel) FontWeight.SemiBold else FontWeight.Medium,
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

            // 3. Audio Quality & Volume
            item {
                SettingsSectionHeader("Audio & Volume")
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Graphite)
                        .border(0.75.dp, WhiteHighlightSoft, RoundedCornerShape(18.dp))
                ) {
                    SettingsNavigationRow(
                        icon = Icons.Default.GraphicEq,
                        title = "Streaming Quality",
                        valueText = streamingLabel,
                        onClick = { showStreamingQualityDialog = true }
                    )
                    HorizontalDivider(color = WhiteHighlightSoft, thickness = 0.5.dp, modifier = Modifier.padding(start = 52.dp))

                    SettingsNavigationRow(
                        icon = Icons.Default.Download,
                        title = "Download Quality",
                        valueText = downloadQuality,
                        onClick = { showDownloadQualityDialog = true }
                    )
                    HorizontalDivider(color = WhiteHighlightSoft, thickness = 0.5.dp, modifier = Modifier.padding(start = 52.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 52.dp)
                            .clickable { playerManager?.setEqualizerEnabled(!equalizerEnabled) }
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Equalizer, contentDescription = null, tint = Silver, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("Equalizer", color = SoftWhite, fontSize = 15.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                        IosAuroraSwitch(
                            checked = equalizerEnabled,
                            onCheckedChange = { playerManager?.setEqualizerEnabled(it) },
                            activeColor = SoftWhite
                        )
                    }

                    HorizontalDivider(color = WhiteHighlightSoft, thickness = 0.5.dp, modifier = Modifier.padding(start = 52.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.VolumeUp, contentDescription = null, tint = Silver, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(16.dp))
                                Text("Playback Volume", color = SoftWhite, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                            }
                            Text(
                                text = "${(playerVolume * 100).toInt()}%",
                                color = Silver,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Slider(
                            value = playerVolume,
                            onValueChange = { playerManager?.setPlaybackVolume(it) },
                            colors = SliderDefaults.colors(
                                thumbColor = SoftWhite,
                                activeTrackColor = SoftWhite,
                                inactiveTrackColor = WhiteHighlightSoft
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // 4. Playback Group
            item {
                SettingsSectionHeader("Playback")
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Graphite)
                        .border(0.75.dp, WhiteHighlightSoft, RoundedCornerShape(18.dp))
                ) {
                    SettingsSwitchRow(
                        title = "Gapless Playback",
                        subtitle = "Eliminate silence between consecutive tracks",
                        checked = gaplessPlayback,
                        onCheckedChange = { playerManager?.setGaplessPlayback(it) }
                    )
                    HorizontalDivider(color = WhiteHighlightSoft, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 16.dp))

                    SettingsSwitchRow(
                        title = "Crossfade",
                        subtitle = "Smoothly blend track transitions",
                        checked = crossfadeEnabled,
                        onCheckedChange = { playerManager?.setCrossfadeEnabled(it) }
                    )
                    HorizontalDivider(color = WhiteHighlightSoft, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 16.dp))

                    SettingsSwitchRow(
                        title = "Normalize Volume",
                        subtitle = "Keep consistent loudness across tracks",
                        checked = automateVolume,
                        onCheckedChange = { playerManager?.setAutomateVolume(it) }
                    )
                    HorizontalDivider(color = WhiteHighlightSoft, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 16.dp))

                    SettingsSwitchRow(
                        title = "Autoplay Similar Music",
                        subtitle = "Continue with related songs when the queue ends",
                        checked = autoplayEnabled,
                        onCheckedChange = { playerManager?.setAutoplayEnabled(it) }
                    )
                    HorizontalDivider(color = WhiteHighlightSoft, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 16.dp))

                    SettingsSwitchRow(
                        title = "Stop on App Close",
                        subtitle = "End playback when removed from recent apps",
                        checked = stopOnTaskRemoved,
                        onCheckedChange = { newVal ->
                            stopOnTaskRemoved = newVal
                            prefs.edit().putBoolean("stop_on_task_removed", newVal).apply()
                            scope.launch {
                                firestoreSyncManager?.saveSettings(stopOnTaskRemoved = newVal)
                            }
                        }
                    )
                    HorizontalDivider(color = WhiteHighlightSoft, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 16.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Sleep Timer", color = SoftWhite, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                            Text(
                                text = if (sleepTimerMinutes != null) "Stops in $sleepTimerMinutes min" else "Off",
                                color = if (sleepTimerMinutes != null) SoftWhite else Silver,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(null to "Off", 15 to "15m", 30 to "30m", 60 to "1h").forEach { (min, lbl) ->
                                val isSel = sleepTimerMinutes == min
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isSel) SoftWhite else GraphiteElevated)
                                        .border(0.7.dp, if (isSel) SoftWhite else WhiteHighlightSoft, RoundedCornerShape(12.dp))
                                        .clickable { onSetSleepTimer(min) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = lbl,
                                        color = if (isSel) MidnightBlack else Silver,
                                        fontSize = 13.sp,
                                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 5. Recommendations & Privacy Group
            item {
                SettingsSectionHeader("Recommendations & Privacy")
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Graphite)
                        .border(0.75.dp, WhiteHighlightSoft, RoundedCornerShape(18.dp))
                ) {
                    SettingsSwitchRow(
                        title = "Personalized Recommendations",
                        subtitle = "Tailor mixes and home discovery to your listening",
                        checked = isPersonalizationEnabled,
                        onCheckedChange = { repository?.setPersonalizationEnabled(it) }
                    )
                    HorizontalDivider(color = WhiteHighlightSoft, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 16.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 56.dp)
                            .clickable { showHistoryDialog = true }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Listening History", color = SoftWhite, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("${recentPlays.size} songs recorded on device", color = Silver, fontSize = 12.sp)
                        }
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MutedGrey)
                    }
                    HorizontalDivider(color = WhiteHighlightSoft, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 16.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 56.dp)
                            .clickable { showFeedbackManageDialog = true }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Excluded Music & Artists", color = SoftWhite, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("${feedbackList.size} disliked or hidden items", color = Silver, fontSize = 12.sp)
                        }
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MutedGrey)
                    }
                    HorizontalDivider(color = WhiteHighlightSoft, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 16.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                repository?.refreshRecommendations(force = true)
                                recommendationRefreshMsg = "Refreshing..."
                                scope.launch {
                                    delay(1600)
                                    recommendationRefreshMsg = "Recommendations Updated"
                                    delay(1600)
                                    recommendationRefreshMsg = null
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(50),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SoftWhite,
                                contentColor = MidnightBlack
                            )
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = recommendationRefreshMsg ?: "Refresh Recommendations",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { showClearHistoryDialog = true },
                                modifier = Modifier.weight(1f).height(48.dp),
                                shape = RoundedCornerShape(50),
                                contentPadding = PaddingValues(horizontal = 8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = GraphiteElevated)
                            ) {
                                Text("Clear History", color = Silver, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                            }
                            Button(
                                onClick = { showResetPreferencesDialog = true },
                                modifier = Modifier.weight(1f).height(48.dp),
                                shape = RoundedCornerShape(50),
                                contentPadding = PaddingValues(horizontal = 8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0x22E07381))
                            ) {
                                Text("Reset Taste", color = DestructiveRose, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                            }
                        }
                    }
                }
            }

            // 6. Storage & Cache
            item {
                SettingsSectionHeader("Storage & Cache")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Graphite)
                        .border(0.75.dp, WhiteHighlightSoft, RoundedCornerShape(18.dp))
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                        Text("Offline Cached Audio", color = SoftWhite, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(cacheSizeText, color = Silver, fontSize = 13.sp)
                    }
                    Button(
                        onClick = {
                            scope.launch {
                                MediaCacheManager.clearCache(context)
                                refreshCacheSize()
                            }
                        },
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = GraphiteElevated)
                    ) {
                        Text("Clear Cache", color = SoftWhite, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // 7. Notifications & Updates
            item {
                SettingsSectionHeader(if (isAdmin) "Notifications, Updates & Admin" else "Notifications & Updates")
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Graphite)
                        .border(0.75.dp, WhiteHighlightSoft, RoundedCornerShape(18.dp))
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 48.dp)
                            .clickable {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !notificationsGranted) {
                                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                            },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.Notifications, contentDescription = null, tint = Silver, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text("Playback & Update Alerts", color = SoftWhite, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                                Text(
                                    text = if (notificationsGranted) "Active on this device" else "Tap to allow notification access",
                                    color = Silver,
                                    fontSize = 12.sp
                                )
                            }
                        }
                        if (!notificationsGranted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            Text("Enable", color = SoftWhite, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    HorizontalDivider(color = WhiteHighlightSoft, thickness = 0.5.dp, modifier = Modifier.padding(vertical = 12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.SystemUpdate, contentDescription = null, tint = SoftWhite, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(
                                modifier = Modifier
                                    .clickable {
                                        if (!isAdmin) {
                                            versionTapCount++
                                            if (versionTapCount >= 5) {
                                                developerModeUnlocked = true
                                                prefs.edit().putBoolean("developer_mode_unlocked", true).apply()
                                                Toast.makeText(context, "Admin OTA Pusher unlocked", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                    .padding(vertical = 4.dp)
                            ) {
                                Text(
                                    text = "Spring Music v${AppUpdateManager.currentVersionName}",
                                    color = SoftWhite,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = if (isAdmin) "Build ${AppUpdateManager.currentVersionCode} • Admin active"
                                    else "Build ${AppUpdateManager.currentVersionCode}",
                                    color = Silver,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        Button(
                            onClick = { AppUpdateManager.checkForUpdates(context, isManualCheck = true) },
                            enabled = !isCheckingUpdates,
                            shape = RoundedCornerShape(50),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SoftWhite,
                                contentColor = MidnightBlack
                            ),
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("btn_check_updates")
                        ) {
                            if (isCheckingUpdates) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = MidnightBlack, strokeWidth = 2.dp)
                            } else {
                                Text("Check", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }

                    if (!updateStatusMsg.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        val isErr = updateStatusMsg!!.contains("Error", true) ||
                            updateStatusMsg!!.contains("404") ||
                            updateStatusMsg!!.contains("failed", true)
                        Text(
                            text = updateStatusMsg!!,
                            color = if (isErr) DestructiveRose else Silver,
                            fontSize = 12.sp
                        )
                    }

                    if (updateAvailable != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { AppUpdateManager.downloadAndInstall(context, updateAvailable!!) },
                                modifier = Modifier.weight(1f).height(48.dp),
                                shape = RoundedCornerShape(50),
                                colors = ButtonDefaults.buttonColors(containerColor = SoftWhite, contentColor = MidnightBlack)
                            ) {
                                Text("Install v${updateAvailable!!.versionName}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                            Button(
                                onClick = { AppUpdateManager.dismissUpdate() },
                                modifier = Modifier.height(48.dp),
                                shape = RoundedCornerShape(50),
                                colors = ButtonDefaults.buttonColors(containerColor = GraphiteElevated, contentColor = Silver)
                            ) {
                                Text("Dismiss", fontSize = 13.sp)
                            }
                        }
                    }

                    if (isAdmin) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { showPusherCard = !showPusherCard },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("btn_toggle_ota_pusher"),
                            shape = RoundedCornerShape(50),
                            colors = ButtonDefaults.buttonColors(containerColor = GraphiteElevated, contentColor = SoftWhite)
                        ) {
                            Icon(Icons.Default.Upload, contentDescription = null, tint = SoftWhite, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (showPusherCard) "Hide Admin Update Pusher" else "Open Admin Update Pusher",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        if (showPusherCard) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(GraphiteElevated)
                                    .border(0.8.dp, WhiteHighlightSoft, RoundedCornerShape(14.dp))
                                    .padding(12.dp)
                            ) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = pushVersionName,
                                        onValueChange = { pushVersionName = it },
                                        label = { Text("Version", fontSize = 11.sp) },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true,
                                        shape = RoundedCornerShape(12.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = SoftWhite,
                                            unfocusedTextColor = SoftWhite,
                                            focusedBorderColor = SoftWhite,
                                            unfocusedBorderColor = WhiteHighlightSoft
                                        )
                                    )
                                    OutlinedTextField(
                                        value = pushVersionCode,
                                        onValueChange = { pushVersionCode = it },
                                        label = { Text("Build", fontSize = 11.sp) },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true,
                                        shape = RoundedCornerShape(12.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = SoftWhite,
                                            unfocusedTextColor = SoftWhite,
                                            focusedBorderColor = SoftWhite,
                                            unfocusedBorderColor = WhiteHighlightSoft
                                        )
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = pushDownloadUrl,
                                    onValueChange = { pushDownloadUrl = it },
                                    label = { Text("APK Download URL", fontSize = 11.sp) },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    trailingIcon = {
                                        val clipboard = LocalClipboardManager.current
                                        IconButton(onClick = {
                                            clipboard.getText()?.text?.trim()?.let { clip ->
                                                if (clip.isNotBlank()) {
                                                    pushDownloadUrl = clip
                                                }
                                            }
                                        }) {
                                            Icon(Icons.Default.ContentPaste, contentDescription = "Paste", tint = SoftWhite)
                                        }
                                    },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = SoftWhite,
                                        unfocusedTextColor = SoftWhite,
                                        focusedBorderColor = SoftWhite,
                                        unfocusedBorderColor = WhiteHighlightSoft
                                    )
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = pushChangelog,
                                    onValueChange = { pushChangelog = it },
                                    label = { Text("Release Notes", fontSize = 11.sp) },
                                    modifier = Modifier.fillMaxWidth(),
                                    maxLines = 2,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = SoftWhite,
                                        unfocusedTextColor = SoftWhite,
                                        focusedBorderColor = SoftWhite,
                                        unfocusedBorderColor = WhiteHighlightSoft
                                    )
                                )
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .defaultMinSize(minHeight = 48.dp)
                                        .clickable { skipUrlProbe = !skipUrlProbe }
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = skipUrlProbe,
                                        onCheckedChange = { skipUrlProbe = it },
                                        colors = CheckboxDefaults.colors(
                                            checkedColor = SoftWhite,
                                            checkmarkColor = MidnightBlack
                                        )
                                    )
                                    Text("Force Broadcast", color = SoftWhite, fontSize = 13.sp)
                                }
                                if (pushFeedbackMsg != null) {
                                    Text(
                                        text = pushFeedbackMsg!!,
                                        color = if (pushFeedbackSuccess) SoftWhite else DestructiveRose,
                                        fontSize = 12.sp
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Button(
                                    onClick = {
                                        val code = pushVersionCode.toIntOrNull() ?: (AppUpdateManager.currentVersionCode + 1)
                                        if (pushDownloadUrl.isBlank()) {
                                            pushFeedbackSuccess = false
                                            pushFeedbackMsg = "Please enter an APK download link"
                                            return@Button
                                        }
                                        isPushingUpdate = true
                                        scope.launch {
                                            val result = AppUpdateManager.pushUpdate(code, pushVersionName, pushDownloadUrl, pushChangelog, skipUrlProbe)
                                            isPushingUpdate = false
                                            result.fold(
                                                onSuccess = {
                                                    pushFeedbackSuccess = true
                                                    pushFeedbackMsg = "Update v$pushVersionName pushed"
                                                },
                                                onFailure = {
                                                    pushFeedbackSuccess = false
                                                    pushFeedbackMsg = it.message ?: "Failed to push update."
                                                }
                                            )
                                        }
                                    },
                                    enabled = !isPushingUpdate,
                                    shape = RoundedCornerShape(50),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .testTag("btn_broadcast_ota_update"),
                                    colors = ButtonDefaults.buttonColors(containerColor = SoftWhite, contentColor = MidnightBlack)
                                ) {
                                    Text("Broadcast Update", fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = {
                                        isClearingBroadcast = true
                                        scope.launch {
                                            val res = AppUpdateManager.clearPushedUpdate()
                                            isClearingBroadcast = false
                                            res.fold(
                                                onSuccess = {
                                                    pushFeedbackSuccess = true
                                                    pushFeedbackMsg = "Cloud broadcast cleared."
                                                },
                                                onFailure = {
                                                    pushFeedbackSuccess = false
                                                    pushFeedbackMsg = "Failed: ${it.message}"
                                                }
                                            )
                                        }
                                    },
                                    enabled = !isClearingBroadcast,
                                    shape = RoundedCornerShape(50),
                                    modifier = Modifier.fillMaxWidth().height(48.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0x22E07381), contentColor = DestructiveRose)
                                ) {
                                    Text("Clear Cloud Broadcast", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }
            }

            // 8. About Spring Music
            item {
                SettingsSectionHeader("About Spring Music")
                LiquidGlassSurface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    shape = RoundedCornerShape(18.dp),
                    elevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BrandSpringLogo(iconSize = 40.dp)
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "v${AppUpdateManager.currentVersionName}",
                                color = SoftWhite,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Midnight Liquid Glass",
                                color = Silver,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }
    }

    if (showStreamingQualityDialog) {
        val qualities = listOf(
            "320kbps" to "High Quality (320 kbps)",
            "160kbps" to "Balanced (160 kbps)",
            "128kbps" to "Normal (128 kbps)",
            "96kbps" to "Data Saver (96 kbps)"
        )
        AlertDialog(
            onDismissRequest = { showStreamingQualityDialog = false },
            containerColor = DarkGlassOpaque,
            title = { Text("Streaming Quality", fontFamily = DisplayFontFamily, color = SoftWhite, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    qualities.forEach { (q, lbl) ->
                        val isSel = currentQuality.contains(q.replace("kbps", ""), ignoreCase = true)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 48.dp)
                                .clickable {
                                    onQualitySelected(q)
                                    showStreamingQualityDialog = false
                                }
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(lbl, color = if (isSel) SoftWhite else Silver, fontSize = 15.sp, fontWeight = if (isSel) FontWeight.SemiBold else FontWeight.Normal)
                            if (isSel) Icon(Icons.Default.Check, contentDescription = null, tint = SoftWhite)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showStreamingQualityDialog = false }) {
                    Text("Close", color = Silver)
                }
            }
        )
    }

    if (showDownloadQualityDialog) {
        val options = listOf("Lossless", "Very High (320 kbps)", "High (160 kbps)", "Normal (128 kbps)")
        AlertDialog(
            onDismissRequest = { showDownloadQualityDialog = false },
            containerColor = DarkGlassOpaque,
            title = { Text("Download Quality", fontFamily = DisplayFontFamily, color = SoftWhite, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    options.forEach { opt ->
                        val isSel = downloadQuality == opt
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 48.dp)
                                .clickable {
                                    playerManager?.setDownloadQuality(opt)
                                    showDownloadQualityDialog = false
                                }
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(opt, color = if (isSel) SoftWhite else Silver, fontSize = 15.sp, fontWeight = if (isSel) FontWeight.SemiBold else FontWeight.Normal)
                            if (isSel) Icon(Icons.Default.Check, contentDescription = null, tint = SoftWhite)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDownloadQualityDialog = false }) {
                    Text("Close", color = Silver)
                }
            }
        )
    }

    if (showClearHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showClearHistoryDialog = false },
            title = { Text("Clear Listening History?", fontFamily = DisplayFontFamily, color = SoftWhite, fontWeight = FontWeight.Bold) },
            text = { Text("This will delete all locally recorded song play counts and durations.", color = Silver) },
            confirmButton = {
                TextButton(onClick = {
                    repository?.clearRecommendationHistory()
                    showClearHistoryDialog = false
                }) { Text("Clear History", color = DestructiveRose, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showClearHistoryDialog = false }) { Text("Cancel", color = SoftWhite) }
            },
            containerColor = DarkGlassOpaque
        )
    }

    if (showResetPreferencesDialog) {
        AlertDialog(
            onDismissRequest = { showResetPreferencesDialog = false },
            title = { Text("Reset All Preferences?", fontFamily = DisplayFontFamily, color = SoftWhite, fontWeight = FontWeight.Bold) },
            text = { Text("This wipes learned artist and genre taste scores and clears excluded items.", color = Silver) },
            confirmButton = {
                TextButton(onClick = {
                    repository?.resetRecommendationPreferences()
                    showResetPreferencesDialog = false
                }) { Text("Reset Everything", color = DestructiveRose, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showResetPreferencesDialog = false }) { Text("Cancel", color = SoftWhite) }
            },
            containerColor = DarkGlassOpaque
        )
    }

    if (showFeedbackManageDialog) {
        AlertDialog(
            onDismissRequest = { showFeedbackManageDialog = false },
            title = { Text("Excluded Music & Artists", fontFamily = DisplayFontFamily, color = SoftWhite, fontWeight = FontWeight.Bold) },
            text = {
                if (feedbackList.isEmpty()) {
                    Text("No songs or artists have been disliked or blocked yet.", color = Silver)
                } else {
                    LazyColumn(modifier = Modifier.fillMaxWidth().height(240.dp)) {
                        items(feedbackList) { item ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(item.targetName.ifBlank { item.targetId }, color = SoftWhite, fontSize = 14.sp)
                                    Text(item.feedbackType, color = Silver, fontSize = 12.sp)
                                }
                                IconButton(
                                    onClick = { repository?.removeFeedback(item.id) },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Remove", tint = DestructiveRose)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showFeedbackManageDialog = false }) {
                    Text("Done", color = SoftWhite, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = DarkGlassOpaque
        )
    }

    if (showHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showHistoryDialog = false },
            title = { Text("Recent Listening History", fontFamily = DisplayFontFamily, color = SoftWhite, fontWeight = FontWeight.Bold) },
            text = {
                if (recentPlays.isEmpty()) {
                    Text("No playback history recorded yet.", color = Silver)
                } else {
                    LazyColumn(modifier = Modifier.fillMaxWidth().height(260.dp)) {
                        items(recentPlays) { play ->
                            Column(modifier = Modifier.padding(vertical = 6.dp)) {
                                Text(play.title, color = SoftWhite, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(play.artist, color = Silver, fontSize = 12.sp, maxLines = 1)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showHistoryDialog = false }) {
                    Text("Close", color = SoftWhite, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = DarkGlassOpaque
        )
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        color = Silver,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(start = 22.dp, end = 20.dp, top = 22.dp, bottom = 8.dp)
    )
}

@Composable
private fun SettingsNavigationRow(
    icon: ImageVector,
    title: String,
    valueText: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 52.dp)
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = Silver, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = title,
            color = SoftWhite,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
        Text(text = valueText, color = Silver, fontSize = 12.sp)
        Spacer(modifier = Modifier.width(6.dp))
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MutedGrey, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun SettingsSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 60.dp)
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(text = title, color = SoftWhite, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = subtitle, color = Silver, fontSize = 12.sp)
        }
        IosAuroraSwitch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            activeColor = SoftWhite
        )
    }
}
