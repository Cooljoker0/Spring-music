package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.DestructiveRose
import com.example.ui.theme.DisplayFontFamily
import com.example.ui.theme.Graphite
import com.example.ui.theme.MidnightBlack
import com.example.ui.theme.Silver
import com.example.ui.theme.SoftWhite
import com.example.ui.theme.WhiteHighlight
import com.example.ui.theme.WhiteHighlightSoft
import com.example.updater.AppUpdateInfo
import com.example.updater.AppUpdateManager

@Composable
fun AppUpdateDialog(
    updateInfo: AppUpdateInfo,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val isDownloading by AppUpdateManager.isDownloading.collectAsState()
    val progress by AppUpdateManager.downloadProgress.collectAsState()
    val statusMessage by AppUpdateManager.statusMessage.collectAsState()

    val downloadedApk by AppUpdateManager.downloadedApkFile.collectAsState()
    val speedText by AppUpdateManager.downloadSpeedText.collectAsState()
    val clipboardManager = LocalClipboardManager.current
    var copiedLink by remember { mutableStateOf(false) }

    LaunchedEffect(updateInfo.versionCode) {
        AppUpdateManager.findExistingDownloadedApk(context, updateInfo.versionCode)
    }

    Dialog(
        onDismissRequest = {
            if (!updateInfo.isMandatory && !isDownloading) {
                onDismiss()
            }
        },
        properties = DialogProperties(
            dismissOnBackPress = !updateInfo.isMandatory && !isDownloading,
            dismissOnClickOutside = !updateInfo.isMandatory && !isDownloading
        )
    ) {
        LiquidGlassSurface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .testTag("app_update_dialog"),
            shape = RoundedCornerShape(22.dp),
            elevation = 20.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(WhiteHighlightSoft)
                            .border(0.7.dp, WhiteHighlight, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.SystemUpdate,
                            contentDescription = "Update Available",
                            tint = SoftWhite,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    if (!updateInfo.isMandatory && !isDownloading) {
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
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Update Available",
                    fontFamily = DisplayFontFamily,
                    color = SoftWhite,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Spring Music v${updateInfo.versionName} is ready to install",
                    color = SoftWhite,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Text(
                    text = "Current version: v${AppUpdateManager.currentVersionName}",
                    color = Silver,
                    fontSize = 12.sp
                )

                if (updateInfo.changelog.isNotBlank()) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Graphite)
                            .border(0.6.dp, WhiteHighlightSoft, RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Column {
                            Text(
                                text = "Release notes",
                                color = Silver,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = updateInfo.changelog,
                                color = SoftWhite,
                                fontSize = 13.sp,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                if (isDownloading) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (progress != null) {
                            LinearProgressIndicator(
                                progress = { progress ?: 0f },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = SoftWhite,
                                trackColor = Graphite
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "${((progress ?: 0f) * 100).toInt()}% downloaded",
                                color = Silver,
                                fontSize = 12.sp
                            )
                            if (speedText != null) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = speedText!!,
                                    color = SoftWhite,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        } else {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = SoftWhite,
                                strokeWidth = 2.dp
                            )
                        }

                        if (!statusMessage.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = statusMessage!!,
                                color = if (statusMessage!!.startsWith("Error")) DestructiveRose else Silver,
                                fontSize = 11.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = { AppUpdateManager.openInBrowser(context, updateInfo.downloadUrl) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, WhiteHighlight)
                        ) {
                            Icon(
                                imageVector = Icons.Default.OpenInBrowser,
                                contentDescription = null,
                                tint = SoftWhite,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Open in Browser",
                                color = SoftWhite,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            )
                        }
                    }
                } else {
                    if (!statusMessage.isNullOrBlank()) {
                        val isError = statusMessage!!.contains("Error", ignoreCase = true) ||
                            statusMessage!!.contains("404") ||
                            statusMessage!!.contains("failed", ignoreCase = true)

                        if (isError) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 14.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF28161A)),
                                border = androidx.compose.foundation.BorderStroke(1.dp, DestructiveRose.copy(alpha = 0.5f))
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = statusMessage!!,
                                        color = DestructiveRose,
                                        fontSize = 12.sp,
                                        lineHeight = 17.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        } else {
                            Text(
                                text = statusMessage!!,
                                color = SoftWhite,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                        }
                    }

                    val isDownloadedReady = downloadedApk != null && downloadedApk!!.exists()

                    Button(
                        onClick = { AppUpdateManager.downloadAndInstall(context, updateInfo) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("btn_confirm_update"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SoftWhite,
                            contentColor = MidnightBlack
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.SystemUpdate,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isDownloadedReady) "Install Update" else "Download & Install",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = { AppUpdateManager.openInBrowser(context, updateInfo.downloadUrl) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("btn_browser_update"),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, WhiteHighlight)
                    ) {
                        Icon(
                            imageVector = Icons.Default.OpenInBrowser,
                            contentDescription = null,
                            tint = SoftWhite,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Download via Browser",
                            color = SoftWhite,
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { AppUpdateManager.downloadWithSystemManager(context, updateInfo) },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("btn_system_update"),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, WhiteHighlightSoft)
                        ) {
                            Text(
                                text = "System DL",
                                color = Silver,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(updateInfo.downloadUrl))
                                copiedLink = true
                                android.widget.Toast.makeText(context, "Download link copied!", android.widget.Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, WhiteHighlightSoft)
                        ) {
                            Text(
                                text = if (copiedLink) "Copied" else "Copy Link",
                                color = if (copiedLink) SoftWhite else Silver,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    if (!updateInfo.isMandatory) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Button(
                            onClick = onDismiss,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Transparent,
                                contentColor = Silver
                            )
                        ) {
                            Text(text = "Later", fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}
