package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.firestore.FirestoreSyncManager
import com.example.ui.theme.AiThemeManager
import com.example.ui.theme.AppThemePreset
import com.example.ui.theme.CustomAppTheme
import com.example.ui.theme.SpotifyBlack
import com.example.ui.theme.SpotifyCard
import com.example.ui.theme.SpotifyCardElevated
import com.example.ui.theme.SpotifyGreen
import com.example.ui.theme.SpotifyTextMuted
import com.example.ui.theme.SpotifyTextPrimary
import com.example.ui.theme.SpotifyTextSecondary
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AiCustomizerSheet(
    firestoreSyncManager: FirestoreSyncManager?,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val keyboardController = LocalSoftwareKeyboardController.current
    val scope = rememberCoroutineScope()

    var promptInput by remember { mutableStateOf("") }
    var previewTheme by remember { mutableStateOf<CustomAppTheme?>(null) }
    var appliedSuccess by remember { mutableStateOf(false) }

    val presetPrompts = listOf(
        "Cyberpunk Neon Cyan & Hot Pink",
        "Sunset Synthwave Orange & Magenta",
        "Midnight Ocean Deep Blue & Mint",
        "Deep Violet & Lavender Dream",
        "Emerald Forest & Jade Teal",
        "Royal Gold & Velvet Rose",
        "Blossom Cherry Pastel Pink",
        "Matrix Phosphor Green & Void Black"
    )

    fun handleGenerate(prompt: String) {
        if (prompt.isBlank()) return
        keyboardController?.hide()
        val theme = AiThemeManager.generateThemeFromPrompt(prompt)
        previewTheme = theme
        appliedSuccess = false
    }

    fun handleApply(theme: CustomAppTheme) {
        AiThemeManager.applyTheme(context, theme)
        appliedSuccess = true
        scope.launch {
            firestoreSyncManager?.saveSettings(
                customThemePrompt = theme.prompt,
                primaryColorHex = AiThemeManager.colorToHex(theme.primaryColor),
                accentColorHex = AiThemeManager.colorToHex(theme.accentColor)
            )
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SpotifyBlack,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFF8E2DE2), Color(0xFF4A00E0))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "AI Theme Studio",
                            color = SpotifyTextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Personalize your app aesthetic with prompts",
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

            Spacer(modifier = Modifier.height(16.dp))

            // Quick Theme Switcher Cards (Spotify Dark vs Apple Liquid Glass)
            Text(
                text = "Choose App Theme",
                color = SpotifyTextSecondary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Spotify Dark
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(SpotifyCard)
                        .border(1.dp, SpotifyGreen.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .clickable {
                            previewTheme = AiThemeManager.SpotifyDarkTheme
                            handleApply(AiThemeManager.SpotifyDarkTheme)
                        }
                        .padding(12.dp)
                        .testTag("chip_spotify_dark"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(SpotifyGreen)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Obsidian Gold",
                            color = SpotifyTextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Apple Liquid Glass
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(SpotifyCard)
                        .border(1.dp, Color(0xFFFA2D48).copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                        .clickable {
                            previewTheme = AiThemeManager.AppleLiquidGlassTheme
                            handleApply(AiThemeManager.AppleLiquidGlassTheme)
                        }
                        .padding(12.dp)
                        .testTag("chip_apple_liquid_glass"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFA2D48))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Apple Glass",
                            color = SpotifyTextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "Or Prompt AI for Any Aesthetic",
                color = SpotifyTextSecondary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Text input
            OutlinedTextField(
                value = promptInput,
                onValueChange = { promptInput = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_ai_theme_prompt"),
                placeholder = {
                    Text(
                        text = "e.g. Cyberpunk neon glow with cyan and hot pink...",
                        color = SpotifyTextMuted,
                        fontSize = 14.sp
                    )
                },
                singleLine = false,
                maxLines = 3,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { handleGenerate(promptInput) }),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = SpotifyCard,
                    unfocusedContainerColor = SpotifyCard,
                    focusedBorderColor = previewTheme?.primaryColor ?: SpotifyGreen,
                    unfocusedBorderColor = Color(0xFF333333),
                    focusedTextColor = SpotifyTextPrimary,
                    unfocusedTextColor = SpotifyTextPrimary
                )
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Action Buttons (Generate + Reset)
            Row(modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = { handleGenerate(promptInput) },
                    enabled = promptInput.isNotBlank(),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("btn_ai_generate_theme"),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = previewTheme?.primaryColor ?: SpotifyGreen,
                        contentColor = Color.Black
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Generate Theme",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Button(
                    onClick = {
                        AiThemeManager.resetToDefault(context)
                        previewTheme = null
                        appliedSuccess = false
                        scope.launch {
                            firestoreSyncManager?.saveSettings(
                                customThemePrompt = "",
                                primaryColorHex = "#E8C57B",
                                accentColorHex = "#8B6EFF"
                            )
                        }
                    },
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("btn_reset_theme"),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SpotifyCardElevated,
                        contentColor = SpotifyTextPrimary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Reset",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Reset", fontSize = 13.sp)
                }
            }

            // Preview Section
            if (previewTheme != null) {
                Spacer(modifier = Modifier.height(20.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(SpotifyCardElevated)
                        .border(1.dp, previewTheme!!.primaryColor.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Live Theme Preview: ${previewTheme!!.name}",
                        color = SpotifyTextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Color Swatches
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(previewTheme!!.primaryColor)
                                    .border(2.dp, Color.White.copy(alpha = 0.3f), CircleShape)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(previewTheme!!.accentColor)
                                    .border(2.dp, Color.White.copy(alpha = 0.3f), CircleShape)
                            )
                        }

                        // Sample Play Button in New Theme Color
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(previewTheme!!.primaryColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = { handleApply(previewTheme!!) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("btn_apply_ai_theme"),
                        shape = RoundedCornerShape(22.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = previewTheme!!.primaryColor,
                            contentColor = Color.Black
                        )
                    ) {
                        Text(
                            text = if (appliedSuccess) "✓ Applied to App!" else "Apply This Theme",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Preset Inspiration Chips
            Text(
                text = "Try Aesthetic Presets",
                color = SpotifyTextSecondary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(10.dp))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                presetPrompts.forEach { preset ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(SpotifyCard)
                            .border(1.dp, Color(0xFF383838), RoundedCornerShape(20.dp))
                            .clickable {
                                promptInput = preset
                                handleGenerate(preset)
                            }
                            .padding(horizontal = 12.dp, vertical = 7.dp)
                    ) {
                        Text(
                            text = preset,
                            color = SpotifyTextPrimary,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}
