package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.firestore.FirestoreSyncManager
import com.example.ui.theme.AiThemeManager
import com.example.ui.theme.CustomAppTheme
import com.example.ui.theme.DarkGlass
import com.example.ui.theme.DeepBlack
import com.example.ui.theme.DisplayFontFamily
import com.example.ui.theme.Graphite
import com.example.ui.theme.LocalAppCustomTheme
import com.example.ui.theme.MidnightBlack
import com.example.ui.theme.MutedGrey
import com.example.ui.theme.Silver
import com.example.ui.theme.SoftWhite
import com.example.ui.theme.WhiteHighlight
import com.example.ui.theme.WhiteHighlightSoft
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
    val currentTheme = LocalAppCustomTheme.current

    var promptInput by remember { mutableStateOf("") }
    var previewTheme by remember { mutableStateOf<CustomAppTheme?>(null) }
    var appliedSuccess by remember { mutableStateOf(false) }

    val glassFinishes = listOf(
        "Midnight Glass",
        "Frosted Silver",
        "Obsidian Matte",
        "Crystal Mist"
    )

    fun handleGenerate(prompt: String) {
        if (prompt.isBlank()) return
        keyboardController?.hide()
        previewTheme = AiThemeManager.generateThemeFromPrompt(prompt)
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
        containerColor = DeepBlack,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Midnight Liquid Glass",
                        color = SoftWhite,
                        fontFamily = DisplayFontFamily,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Select a monochromatic glass & contrast finish",
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

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "Glass Finishes",
                color = Silver,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(10.dp))

            val curatedRows = AiThemeManager.CuratedThemes.chunked(2)
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                curatedRows.forEach { rowThemes ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        rowThemes.forEach { presetTheme ->
                            val isActive = currentTheme.id == presetTheme.id
                            val tag = if (presetTheme.id == "apple_liquid_glass") {
                                "chip_apple_liquid_glass"
                            } else {
                                "chip_${presetTheme.id}"
                            }
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 50.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(
                                        if (isActive) Graphite else DarkGlass
                                    )
                                    .border(
                                        width = if (isActive) 1.2.dp else 0.7.dp,
                                        color = if (isActive) SoftWhite else WhiteHighlightSoft,
                                        shape = RoundedCornerShape(14.dp)
                                    )
                                    .clickable {
                                        previewTheme = presetTheme
                                        handleApply(presetTheme)
                                    }
                                    .padding(horizontal = 12.dp, vertical = 12.dp)
                                    .testTag(tag),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clip(CircleShape)
                                            .background(presetTheme.primaryColor)
                                            .border(0.8.dp, WhiteHighlight, CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = presetTheme.name,
                                        color = SoftWhite,
                                        fontSize = 13.sp,
                                        fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Medium
                                    )
                                }
                                if (isActive) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Active",
                                        tint = SoftWhite,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            Text(
                text = "Custom Finish Note",
                color = Silver,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = promptInput,
                onValueChange = { promptInput = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_ai_theme_prompt"),
                placeholder = {
                    Text(
                        text = "Try \"Frosted Silver\" or \"Obsidian Matte\"",
                        color = MutedGrey,
                        fontSize = 14.sp
                    )
                },
                singleLine = false,
                maxLines = 2,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { handleGenerate(promptInput) }),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Graphite,
                    unfocusedContainerColor = DarkGlass,
                    focusedBorderColor = WhiteHighlight,
                    unfocusedBorderColor = WhiteHighlightSoft,
                    focusedTextColor = SoftWhite,
                    unfocusedTextColor = SoftWhite,
                    cursorColor = SoftWhite
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = { handleGenerate(promptInput) },
                    enabled = promptInput.isNotBlank(),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("btn_ai_generate_theme"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SoftWhite,
                        contentColor = MidnightBlack
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Preview Finish",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                OutlinedButton(
                    onClick = {
                        AiThemeManager.resetToDefault(context)
                        previewTheme = null
                        appliedSuccess = false
                        scope.launch {
                            firestoreSyncManager?.saveSettings(
                                customThemePrompt = "",
                                primaryColorHex = "#F4F5F7",
                                accentColorHex = "#AEB4C0"
                            )
                        }
                    },
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("btn_reset_theme"),
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, WhiteHighlight),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = SoftWhite)
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

            if (previewTheme != null) {
                val pTheme = previewTheme!!
                Spacer(modifier = Modifier.height(18.dp))
                LiquidGlassSurface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    elevation = 6.dp
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = pTheme.name,
                                color = SoftWhite,
                                fontFamily = DisplayFontFamily,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Box(modifier = Modifier.size(14.dp).clip(CircleShape).background(pTheme.primaryColor))
                                Box(modifier = Modifier.size(14.dp).clip(CircleShape).background(pTheme.primaryBright))
                                Box(modifier = Modifier.size(14.dp).clip(CircleShape).background(pTheme.cardBackground).border(0.6.dp, WhiteHighlight, CircleShape))
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(pTheme.cardBackground)
                                .border(0.7.dp, WhiteHighlightSoft, RoundedCornerShape(12.dp))
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(WhiteHighlightSoft),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MusicNote,
                                    contentDescription = null,
                                    tint = pTheme.primaryColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Preview Track",
                                    color = SoftWhite,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Monochrome glass & silver contrast",
                                    color = Silver,
                                    fontSize = 11.sp
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(pTheme.primaryColor)
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    text = "Active",
                                    color = MidnightBlack,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = { handleApply(pTheme) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("btn_apply_ai_theme"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SoftWhite,
                                contentColor = MidnightBlack
                            )
                        ) {
                            if (appliedSuccess) {
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                            }
                            Text(
                                text = if (appliedSuccess) "Applied" else "Apply Finish",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            Text(
                text = "Quick Finishes",
                color = Silver,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(10.dp))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                glassFinishes.forEach { preset ->
                    Box(
                        modifier = Modifier
                            .heightIn(min = 44.dp)
                            .clip(RoundedCornerShape(50))
                            .background(DarkGlass)
                            .border(0.7.dp, WhiteHighlightSoft, RoundedCornerShape(50))
                            .clickable {
                                promptInput = preset
                                handleGenerate(preset)
                            }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = preset,
                            color = SoftWhite,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}
