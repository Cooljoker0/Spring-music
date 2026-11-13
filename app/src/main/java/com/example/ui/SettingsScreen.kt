package com.example.ui

import android.app.Activity
import android.content.Context
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
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
import com.example.ui.components.IosAuroraSwitch
import com.example.ui.theme.AiThemeManager
import com.example.ui.theme.AppThemePreset
import com.example.ui.theme.AuroraCyan
import com.example.ui.theme.AuroraMagenta
import com.example.ui.theme.AuroraViolet
import com.example.ui.theme.CrispWhite
import com.example.ui.theme.FrostedGlassBorder
import com.example.ui.theme.FrostedGlassSurface
import com.example.ui.theme.IosEmerald
import com.example.ui.theme.IosRed
import com.example.ui.theme.MidnightCanvas
import com.example.ui.theme.MidnightElevated
import com.example.ui.theme.SlateMuted
import com.example.ui.theme.SlateSecondary
import com.example.updater.AppUpdateManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.sin

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
            currentQuality.contains("320") -> "High Quality"
            currentQuality.contains("160") -> "Balanced"
            currentQuality.contains("128") -> "Normal"
            else -> currentQuality
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MidnightCanvas)
    ) {
        // Top Ambient Aurora glow
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            AuroraViolet.copy(alpha = 0.2f),
                            AuroraCyan.copy(alpha = 0.12f),
                            Color.Transparent
                        ),
                        center = Offset(720f, 100f),
                        radius = 850f
                    )
                )
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 165.dp)
        ) {
            // Large iOS Navigation Title Header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (onBack != null) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(FrostedGlassSurface)
                                .border(1.dp, FrostedGlassBorder, CircleShape)
                                .clickable { onBack() }
                                .testTag("btn_settings_back"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = CrispWhite,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                    }

                    Text(
                        text = "Settings",
                        color = CrispWhite,
                        fontSize = 34.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.8).sp
                    )
                }
            }

            // 1. Glossy Dark-Glass Profile Card
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 6.dp)
                        .shadow(
                            elevation = 16.dp,
                            shape = RoundedCornerShape(22.dp),
                            ambientColor = AuroraViolet.copy(alpha = 0.35f),
                            spotColor = Color.Black
                        )
                        .clip(RoundedCornerShape(22.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    Color(0xD41B1E30),
                                    FrostedGlassSurface
                                )
                            )
                        )
                        .border(
                            width = 1.dp,
                            brush = Brush.horizontalGradient(
                                listOf(AuroraCyan.copy(alpha = 0.45f), FrostedGlassBorder, AuroraMagenta.copy(alpha = 0.4f))
                            ),
                            shape = RoundedCornerShape(22.dp)
                        )
                        .padding(16.dp)
                ) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Glowing Gradient Ring Avatar
                            Box(
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.sweepGradient(listOf(AuroraCyan, AuroraViolet, AuroraMagenta, AuroraCyan))
                                    )
                                    .padding(2.5.dp)
                                    .clip(CircleShape)
                                    .background(MidnightElevated),
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
                                            color = CrispWhite,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 22.sp
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.Person,
                                            contentDescription = "Profile",
                                            tint = CrispWhite,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = "Account",
                                        tint = AuroraCyan,
                                        modifier = Modifier.size(28.dp)
                                    )
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
                                                color = CrispWhite,
                                                fontSize = 19.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            // Glowing Iridescent Verified Checkmark Badge
                                            Box(
                                                modifier = Modifier
                                                    .size(22.dp)
                                                    .shadow(8.dp, CircleShape, ambientColor = AuroraCyan, spotColor = AuroraMagenta)
                                                    .clip(CircleShape)
                                                    .background(Brush.linearGradient(listOf(AuroraCyan, AuroraViolet, AuroraMagenta))),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Verified,
                                                    contentDescription = "Verified",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(2.dp))
                                    }
                                    Text(
                                        text = currentUser?.email?.takeIf { it.isNotBlank() }
                                            ?.let { "Premium Plan • $it" }
                                            ?: "Premium Plan",
                                        color = SlateSecondary,
                                        fontSize = 13.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                } else {
                                    Text(
                                        text = "Sign in to sync your library & playlists",
                                        color = SlateSecondary,
                                        fontSize = 14.sp,
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
                                    Icon(
                                        imageVector = Icons.Default.CloudDone,
                                        contentDescription = null,
                                        tint = AuroraCyan,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Cloud Sync: $syncStatus",
                                        color = AuroraCyan,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(Color(0x28FF453A))
                                        .border(0.8.dp, IosRed.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                                        .clickable { AuthManager.signOut(context, scope) }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                        .testTag("btn_google_sign_out")
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Logout,
                                            contentDescription = null,
                                            tint = IosRed,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(5.dp))
                                        Text("Sign Out", color = IosRed, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        } else {
                            if (authError != null) {
                                Text(
                                    text = authError!!,
                                    color = IosRed,
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
                                    .height(42.dp)
                                    .testTag("btn_google_sign_in"),
                                shape = RoundedCornerShape(21.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.White,
                                    contentColor = Color(0xFF12141E)
                                )
                            ) {
                                if (isAuthLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        color = MidnightCanvas,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Text("Sync Cloud Backup with Google", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
            }

            // 2. Audio Quality Group (iOS Inset Grouped Frosted Glass Container)
            item {
                IosSectionLabel("AUDIO QUALITY")
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(FrostedGlassSurface)
                        .border(1.dp, FrostedGlassBorder, RoundedCornerShape(20.dp))
                ) {
                    // Streaming row -> High Quality >
                    IosChevronRow(
                        icon = Icons.Default.GraphicEq,
                        iconTint = AuroraCyan,
                        title = "Streaming",
                        valueText = streamingLabel,
                        onClick = { showStreamingQualityDialog = true }
                    )
                    HorizontalDivider(color = FrostedGlassBorder, modifier = Modifier.padding(start = 56.dp))

                    // Downloads row -> Lossless >
                    IosChevronRow(
                        icon = Icons.Default.Download,
                        iconTint = AuroraViolet,
                        title = "Downloads",
                        valueText = downloadQuality,
                        onClick = { showDownloadQualityDialog = true }
                    )
                    HorizontalDivider(color = FrostedGlassBorder, modifier = Modifier.padding(start = 56.dp))

                    // Equalizer row with active green iOS switch
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IosIconBadge(icon = Icons.Default.Equalizer, tint = AuroraMagenta)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Equalizer",
                            color = CrispWhite,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f)
                        )
                        IosAuroraSwitch(
                            checked = equalizerEnabled,
                            onCheckedChange = { playerManager?.setEqualizerEnabled(it) }
                        )
                    }

                    HorizontalDivider(color = FrostedGlassBorder, modifier = Modifier.padding(start = 56.dp))

                    // Horizontal Volume Slider with Glowing Violet Thumb
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.VolumeUp,
                                    contentDescription = null,
                                    tint = AuroraViolet,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Volume",
                                    color = CrispWhite,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Text(
                                text = "${(playerVolume * 100).toInt()}%",
                                color = AuroraViolet,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Slider(
                            value = playerVolume,
                            onValueChange = { playerManager?.setPlaybackVolume(it) },
                            colors = SliderDefaults.colors(
                                thumbColor = AuroraViolet,
                                activeTrackColor = AuroraViolet,
                                inactiveTrackColor = Color(0x30EBEBF5)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // 3. Playback Group (Decorated with glowing purple/cyan frequency curve along the top edge!)
            item {
                IosSectionLabel("PLAYBACK")
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(FrostedGlassSurface)
                        .border(1.dp, FrostedGlassBorder, RoundedCornerShape(20.dp))
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Glowing purple/cyan frequency curve along top edge
                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(20.dp)
                        ) {
                            val path = Path()
                            val steps = 40
                            for (i in 0..steps) {
                                val norm = i.toFloat() / steps
                                val x = norm * size.width
                                val y = size.height * 0.55f + sin(norm * 9f) * (size.height * 0.35f)
                                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                            }
                            drawPath(
                                path = path,
                                brush = Brush.horizontalGradient(listOf(AuroraViolet, AuroraCyan, AuroraMagenta)),
                                style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                            )
                        }

                        IosSwitchRow(
                            title = "Gapless Playback",
                            subtitle = "Eliminate silence between consecutive tracks",
                            checked = gaplessPlayback,
                            onCheckedChange = { playerManager?.setGaplessPlayback(it) }
                        )
                        HorizontalDivider(color = FrostedGlassBorder, modifier = Modifier.padding(horizontal = 14.dp))

                        IosSwitchRow(
                            title = "Crossfade",
                            subtitle = "Smoothly blend track transitions",
                            checked = crossfadeEnabled,
                            onCheckedChange = { playerManager?.setCrossfadeEnabled(it) }
                        )
                        HorizontalDivider(color = FrostedGlassBorder, modifier = Modifier.padding(horizontal = 14.dp))

                        IosSwitchRow(
                            title = "Automate Volume",
                            subtitle = "Consistent loudness level across all tracks",
                            checked = automateVolume,
                            onCheckedChange = { playerManager?.setAutomateVolume(it) }
                        )
                        HorizontalDivider(color = FrostedGlassBorder, modifier = Modifier.padding(horizontal = 14.dp))

                        IosSwitchRow(
                            title = "Stop on App Close",
                            subtitle = "Stop audio when removed from recent apps",
                            checked = stopOnTaskRemoved,
                            onCheckedChange = { newVal ->
                                stopOnTaskRemoved = newVal
                                prefs.edit().putBoolean("stop_on_task_removed", newVal).apply()
                                scope.launch {
                                    firestoreSyncManager?.saveSettings(stopOnTaskRemoved = newVal)
                                }
                            }
                        )
                        HorizontalDivider(color = FrostedGlassBorder, modifier = Modifier.padding(horizontal = 14.dp))

                        // Sleep Timer row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Sleep Timer", color = CrispWhite, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                                Text(
                                    text = if (sleepTimerMinutes != null) "Stops in $sleepTimerMinutes min" else "Auto pause after timer",
                                    color = if (sleepTimerMinutes != null) AuroraCyan else SlateSecondary,
                                    fontSize = 12.sp
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf(null to "Off", 15 to "15m", 30 to "30m", 60 to "1h").forEach { (min, lbl) ->
                                    val isSel = sleepTimerMinutes == min
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(if (isSel) AuroraCyan else Color(0x22FFFFFF))
                                            .clickable { onSetSleepTimer(min) }
                                            .padding(horizontal = 10.dp, vertical = 5.dp)
                                    ) {
                                        Text(
                                            text = lbl,
                                            color = if (isSel) MidnightCanvas else CrispWhite,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 4. Theme Studio & Appearance Group
            item {
                IosSectionLabel("APPEARANCE & AI THEME")
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(FrostedGlassSurface)
                        .border(1.dp, FrostedGlassBorder, RoundedCornerShape(20.dp))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenAiCustomizer() }
                            .padding(horizontal = 14.dp, vertical = 14.dp)
                            .testTag("theme_option_ai_customizer"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IosIconBadge(icon = Icons.Default.AutoAwesome, tint = AuroraCyan)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("AI Prompt Theme Studio", color = CrispWhite, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                            Text(
                                text = if (currentTheme.isCustom) "Active: ${currentTheme.name}" else "Customize accents via prompt",
                                color = SlateSecondary,
                                fontSize = 12.sp
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = SlateSecondary
                        )
                    }
                    HorizontalDivider(color = FrostedGlassBorder, modifier = Modifier.padding(start = 56.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            AppThemePreset.SPOTIFY_DARK to "Aurora",
                            AppThemePreset.APPLE_LIQUID_GLASS to "Liquid Rose"
                        ).forEach { (preset, label) ->
                            val isSel = currentTheme.id == preset.id
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(if (isSel) AuroraViolet.copy(alpha = 0.25f) else Color(0x16FFFFFF))
                                    .border(1.dp, if (isSel) AuroraViolet else FrostedGlassBorder, RoundedCornerShape(14.dp))
                                    .clickable { AiThemeManager.setPresetTheme(context, preset) }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    color = if (isSel) CrispWhite else SlateSecondary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }

            // 5. Recommendations & Privacy Group
            item {
                IosSectionLabel("RECOMMENDATIONS & PRIVACY")
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(FrostedGlassSurface)
                        .border(1.dp, FrostedGlassBorder, RoundedCornerShape(20.dp))
                ) {
                    IosSwitchRow(
                        title = "Personalized Recommendations",
                        subtitle = "Tailor Daily Mix & Discover Weekly to your listening",
                        checked = isPersonalizationEnabled,
                        onCheckedChange = { repository?.setPersonalizationEnabled(it) }
                    )
                    HorizontalDivider(color = FrostedGlassBorder, modifier = Modifier.padding(horizontal = 14.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showHistoryDialog = true }
                            .padding(horizontal = 14.dp, vertical = 13.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Listening History", color = CrispWhite, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                            Text("${recentPlays.size} songs tracked locally", color = SlateSecondary, fontSize = 12.sp)
                        }
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = SlateSecondary)
                    }
                    HorizontalDivider(color = FrostedGlassBorder, modifier = Modifier.padding(horizontal = 14.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showFeedbackManageDialog = true }
                            .padding(horizontal = 14.dp, vertical = 13.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Excluded Music & Artists", color = CrispWhite, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                            Text("${feedbackList.size} disliked or blocked items", color = SlateSecondary, fontSize = 12.sp)
                        }
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = SlateSecondary)
                    }
                    HorizontalDivider(color = FrostedGlassBorder, modifier = Modifier.padding(horizontal = 14.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { showClearHistoryDialog = true },
                            modifier = Modifier.weight(1f).height(38.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0x22FFFFFF))
                        ) {
                            Text("Clear History", color = SlateSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Button(
                            onClick = { showResetPreferencesDialog = true },
                            modifier = Modifier.weight(1f).height(38.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0x22FF453A))
                        ) {
                            Text("Reset Taste", color = IosRed, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Button(
                            onClick = {
                                repository?.refreshRecommendations(force = true)
                                recommendationRefreshMsg = "Refreshing..."
                                scope.launch {
                                    delay(1600)
                                    recommendationRefreshMsg = "Updated!"
                                    delay(1600)
                                    recommendationRefreshMsg = null
                                }
                            },
                            modifier = Modifier.weight(1f).height(38.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AuroraCyan)
                        ) {
                            Text(
                                text = recommendationRefreshMsg ?: "Refresh",
                                color = MidnightCanvas,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            // 6. Storage & Offline Cache
            item {
                IosSectionLabel("STORAGE")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(FrostedGlassSurface)
                        .border(1.dp, FrostedGlassBorder, RoundedCornerShape(20.dp))
                        .padding(horizontal = 14.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Offline Cached Audio", color = CrispWhite, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                        Text(cacheSizeText, color = AuroraCyan, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Button(
                        onClick = {
                            scope.launch {
                                MediaCacheManager.clearCache(context)
                                refreshCacheSize()
                            }
                        },
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0x24FFFFFF))
                    ) {
                        Text("Clear Cache", color = CrispWhite, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // 7. App Updates & Admin OTA Pusher
            item {
                IosSectionLabel(if (isAdmin) "APP UPDATES & OTA PUSHER (ADMIN)" else "APP UPDATES")
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(FrostedGlassSurface)
                        .border(1.dp, FrostedGlassBorder, RoundedCornerShape(20.dp))
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            IosIconBadge(icon = Icons.Default.SystemUpdate, tint = AuroraCyan)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(
                                modifier = Modifier.clickable {
                                    if (!isAdmin) {
                                        versionTapCount++
                                        if (versionTapCount >= 5) {
                                            developerModeUnlocked = true
                                            prefs.edit().putBoolean("developer_mode_unlocked", true).apply()
                                            android.widget.Toast.makeText(context, "Admin OTA Pusher unlocked!", android.widget.Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            ) {
                                Text(
                                    text = "Spring Music v${AppUpdateManager.currentVersionName}",
                                    color = CrispWhite,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (isAdmin) "Build ${AppUpdateManager.currentVersionCode} • Admin Pusher Active" else "Build ${AppUpdateManager.currentVersionCode}",
                                    color = SlateSecondary,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        Button(
                            onClick = { AppUpdateManager.checkForUpdates(context, isManualCheck = true) },
                            enabled = !isCheckingUpdates,
                            shape = RoundedCornerShape(18.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AuroraCyan, contentColor = MidnightCanvas),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("btn_check_updates")
                        ) {
                            if (isCheckingUpdates) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = MidnightCanvas, strokeWidth = 2.dp)
                            } else {
                                Text("Check", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }

                    if (!updateStatusMsg.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        val isErr = updateStatusMsg!!.contains("Error", true) || updateStatusMsg!!.contains("404") || updateStatusMsg!!.contains("failed", true)
                        Text(
                            text = updateStatusMsg!!,
                            color = if (isErr) IosRed else AuroraCyan,
                            fontSize = 12.sp
                        )
                    }

                    if (updateAvailable != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { AppUpdateManager.downloadAndInstall(context, updateAvailable!!) },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = AuroraCyan, contentColor = MidnightCanvas)
                            ) {
                                Text("Install v${updateAvailable!!.versionName}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                            Button(
                                onClick = { AppUpdateManager.dismissUpdate() },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0x22FFFFFF), contentColor = SlateSecondary)
                            ) {
                                Text("Dismiss", fontSize = 12.sp)
                            }
                        }
                    }

                    if (isAdmin) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { showPusherCard = !showPusherCard },
                            modifier = Modifier.fillMaxWidth().testTag("btn_toggle_ota_pusher"),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0x22FFFFFF), contentColor = CrispWhite)
                        ) {
                            Icon(Icons.Default.Upload, contentDescription = null, tint = AuroraCyan, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (showPusherCard) "Hide Admin Update Pusher" else "Open Admin Update Pusher", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        if (showPusherCard) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(MidnightElevated)
                                    .border(1.dp, FrostedGlassBorder, RoundedCornerShape(14.dp))
                                    .padding(12.dp)
                            ) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = pushVersionName,
                                        onValueChange = { pushVersionName = it },
                                        label = { Text("Version", fontSize = 11.sp) },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = CrispWhite, unfocusedTextColor = CrispWhite)
                                    )
                                    OutlinedTextField(
                                        value = pushVersionCode,
                                        onValueChange = { pushVersionCode = it },
                                        label = { Text("Build", fontSize = 11.sp) },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = CrispWhite, unfocusedTextColor = CrispWhite)
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
                                    modifier = Modifier.fillMaxWidth(),
                                    trailingIcon = {
                                        val clipboard = LocalClipboardManager.current
                                        IconButton(onClick = {
                                            clipboard.getText()?.text?.trim()?.let { clip ->
                                                if (clip.isNotBlank()) {
                                                    pushDownloadUrl = clip
                                                    probeResultMsg = null
                                                }
                                            }
                                        }) {
                                            Icon(Icons.Default.ContentPaste, contentDescription = "Paste", tint = AuroraCyan)
                                        }
                                    },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = CrispWhite, unfocusedTextColor = CrispWhite)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = pushChangelog,
                                    onValueChange = { pushChangelog = it },
                                    label = { Text("Release Notes", fontSize = 11.sp) },
                                    modifier = Modifier.fillMaxWidth(),
                                    maxLines = 2,
                                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = CrispWhite, unfocusedTextColor = CrispWhite)
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth().clickable { skipUrlProbe = !skipUrlProbe }.padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = skipUrlProbe,
                                        onCheckedChange = { skipUrlProbe = it },
                                        colors = CheckboxDefaults.colors(checkedColor = AuroraCyan)
                                    )
                                    Text("Force Broadcast", color = CrispWhite, fontSize = 12.sp)
                                }
                                if (pushFeedbackMsg != null) {
                                    Text(pushFeedbackMsg!!, color = if (pushFeedbackMsg!!.startsWith("✓")) AuroraCyan else IosRed, fontSize = 12.sp)
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = {
                                        val code = pushVersionCode.toIntOrNull() ?: (AppUpdateManager.currentVersionCode + 1)
                                        if (pushDownloadUrl.isBlank()) {
                                            pushFeedbackMsg = "Please enter an APK download link"
                                            return@Button
                                        }
                                        isPushingUpdate = true
                                        scope.launch {
                                            val result = AppUpdateManager.pushUpdate(code, pushVersionName, pushDownloadUrl, pushChangelog, skipUrlProbe)
                                            isPushingUpdate = false
                                            pushFeedbackMsg = result.fold(
                                                onSuccess = { "✓ Update v$pushVersionName pushed!" },
                                                onFailure = { it.message ?: "Failed to push update." }
                                            )
                                        }
                                    },
                                    enabled = !isPushingUpdate,
                                    modifier = Modifier.fillMaxWidth().testTag("btn_broadcast_ota_update"),
                                    colors = ButtonDefaults.buttonColors(containerColor = AuroraCyan, contentColor = MidnightCanvas)
                                ) {
                                    Text("Broadcast Update", fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Button(
                                    onClick = {
                                        isClearingBroadcast = true
                                        scope.launch {
                                            val res = AppUpdateManager.clearPushedUpdate()
                                            isClearingBroadcast = false
                                            pushFeedbackMsg = res.fold(
                                                onSuccess = { "✓ Cloud broadcast cleared." },
                                                onFailure = { "Failed: ${it.message}" }
                                            )
                                        }
                                    },
                                    enabled = !isClearingBroadcast,
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0x33FF453A), contentColor = IosRed)
                                ) {
                                    Text("Clear Cloud Broadcast", fontSize = 12.sp)
                                }
                            }
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
            containerColor = MidnightElevated,
            title = { Text("Streaming Audio Quality", color = CrispWhite, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    qualities.forEach { (q, lbl) ->
                        val isSel = currentQuality.contains(q.replace("kbps", ""), ignoreCase = true)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onQualitySelected(q)
                                    showStreamingQualityDialog = false
                                }
                                .padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(lbl, color = if (isSel) AuroraCyan else CrispWhite, fontSize = 15.sp)
                            if (isSel) Icon(Icons.Default.Check, contentDescription = null, tint = AuroraCyan)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showStreamingQualityDialog = false }) {
                    Text("Close", color = SlateSecondary)
                }
            }
        )
    }

    if (showDownloadQualityDialog) {
        val options = listOf("Lossless", "Very High (320 kbps)", "High (160 kbps)", "Normal (128 kbps)")
        AlertDialog(
            onDismissRequest = { showDownloadQualityDialog = false },
            containerColor = MidnightElevated,
            title = { Text("Download Quality", color = CrispWhite, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    options.forEach { opt ->
                        val isSel = downloadQuality == opt
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    playerManager?.setDownloadQuality(opt)
                                    showDownloadQualityDialog = false
                                }
                                .padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(opt, color = if (isSel) AuroraViolet else CrispWhite, fontSize = 15.sp)
                            if (isSel) Icon(Icons.Default.Check, contentDescription = null, tint = AuroraViolet)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDownloadQualityDialog = false }) {
                    Text("Close", color = SlateSecondary)
                }
            }
        )
    }

    if (showClearHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showClearHistoryDialog = false },
            title = { Text("Clear Listening History?", color = CrispWhite, fontWeight = FontWeight.Bold) },
            text = { Text("This will delete all locally recorded song play counts and durations.", color = SlateSecondary) },
            confirmButton = {
                TextButton(onClick = {
                    repository?.clearRecommendationHistory()
                    showClearHistoryDialog = false
                }) { Text("Clear History", color = IosRed, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showClearHistoryDialog = false }) { Text("Cancel", color = CrispWhite) }
            },
            containerColor = MidnightElevated
        )
    }

    if (showResetPreferencesDialog) {
        AlertDialog(
            onDismissRequest = { showResetPreferencesDialog = false },
            title = { Text("Reset All Preferences?", color = CrispWhite, fontWeight = FontWeight.Bold) },
            text = { Text("This wipes learned artist and genre taste scores and clears excluded items.", color = SlateSecondary) },
            confirmButton = {
                TextButton(onClick = {
                    repository?.resetRecommendationPreferences()
                    showResetPreferencesDialog = false
                }) { Text("Reset Everything", color = IosRed, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showResetPreferencesDialog = false }) { Text("Cancel", color = CrispWhite) }
            },
            containerColor = MidnightElevated
        )
    }

    if (showFeedbackManageDialog) {
        AlertDialog(
            onDismissRequest = { showFeedbackManageDialog = false },
            title = { Text("Excluded Music & Artists", color = CrispWhite, fontWeight = FontWeight.Bold) },
            text = {
                if (feedbackList.isEmpty()) {
                    Text("No songs or artists have been disliked or blocked yet.", color = SlateSecondary)
                } else {
                    LazyColumn(modifier = Modifier.fillMaxWidth().height(240.dp)) {
                        items(feedbackList) { item ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(item.targetName.ifBlank { item.targetId }, color = CrispWhite, fontSize = 14.sp)
                                    Text(item.feedbackType, color = SlateSecondary, fontSize = 12.sp)
                                }
                                IconButton(onClick = { repository?.removeFeedback(item.id) }) {
                                    Icon(Icons.Default.Close, contentDescription = "Remove", tint = IosRed)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showFeedbackManageDialog = false }) {
                    Text("Done", color = AuroraCyan, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = MidnightElevated
        )
    }

    if (showHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showHistoryDialog = false },
            title = { Text("Recent Listening History", color = CrispWhite, fontWeight = FontWeight.Bold) },
            text = {
                if (recentPlays.isEmpty()) {
                    Text("No playback history recorded yet.", color = SlateSecondary)
                } else {
                    LazyColumn(modifier = Modifier.fillMaxWidth().height(260.dp)) {
                        items(recentPlays) { play ->
                            Column(modifier = Modifier.padding(vertical = 6.dp)) {
                                Text(play.title, color = CrispWhite, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(play.artist, color = SlateSecondary, fontSize = 12.sp, maxLines = 1)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showHistoryDialog = false }) {
                    Text("Close", color = AuroraCyan, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = MidnightElevated
        )
    }
}

@Composable
private fun IosSectionLabel(title: String) {
    Text(
        text = title,
        color = SlateMuted,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.8.sp,
        modifier = Modifier.padding(start = 24.dp, end = 18.dp, top = 20.dp, bottom = 8.dp)
    )
}

@Composable
private fun IosIconBadge(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color
) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(RoundedCornerShape(9.dp))
            .background(tint.copy(alpha = 0.18f))
            .border(0.8.dp, tint.copy(alpha = 0.45f), RoundedCornerShape(9.dp)),
        contentAlignment = Alignment.Center
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun IosChevronRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    title: String,
    valueText: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IosIconBadge(icon = icon, tint = iconTint)
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = title,
            color = CrispWhite,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
        Text(text = valueText, color = SlateSecondary, fontSize = 14.sp)
        Spacer(modifier = Modifier.width(4.dp))
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = SlateSecondary,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun IosSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(text = title, color = CrispWhite, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = subtitle, color = SlateSecondary, fontSize = 12.sp)
        }
        IosAuroraSwitch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}
