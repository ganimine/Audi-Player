package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.PermMedia
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.ContentTypeFilter
import com.example.model.MediaFilterConfig
import com.example.model.SizePreset
import com.example.model.SizeUnit
import com.example.ui.components.AuraBadge
import com.example.viewmodel.PlayerViewModel

/**
 * Dedicated Content Selection and File-Size Filtering Panel.
 * Step 1: Select Content Type (Audio files, Video files, Both audio and video).
 * Step 2: Configure File-Size Limits (Include all sizes, Quick Presets: Small, 4MB-5MB, Medium, Large, Custom,
 *         Minimum & Maximum size with KB, MB, GB units and min <= max validation).
 * Automatically closes upon applying or resetting filters without interrupting active playback.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ContentFilterSheet(
    viewModel: PlayerViewModel,
    onDismiss: () -> Unit
) {
    val activeConfig by viewModel.mediaFilterConfig.collectAsState()
    val masterList by viewModel.masterMediaList.collectAsState()
    val accentTheme by viewModel.accentTheme.collectAsState()
    val accentColor = Color(accentTheme.hexColor)

    var selectedContentType by remember(activeConfig) { mutableStateOf(activeConfig.contentType) }
    var selectedPreset by remember(activeConfig) { mutableStateOf(activeConfig.sizePreset) }
    var includeAllSizes by remember(activeConfig) { mutableStateOf(activeConfig.includeAllSizes) }

    var minText by remember(activeConfig) {
        mutableStateOf(
            if (activeConfig.minSizeValue % 1.0 == 0.0) activeConfig.minSizeValue.toLong().toString()
            else activeConfig.minSizeValue.toString()
        )
    }
    var minUnit by remember(activeConfig) { mutableStateOf(activeConfig.minSizeUnit) }

    var maxText by remember(activeConfig) {
        mutableStateOf(
            if (activeConfig.maxSizeValue % 1.0 == 0.0) activeConfig.maxSizeValue.toLong().toString()
            else activeConfig.maxSizeValue.toString()
        )
    }
    var maxUnit by remember(activeConfig) { mutableStateOf(activeConfig.maxSizeUnit) }

    val parsedMin = minText.toDoubleOrNull() ?: -1.0
    val parsedMax = maxText.toDoubleOrNull() ?: -1.0

    val draftConfig = remember(
        selectedContentType,
        selectedPreset,
        includeAllSizes,
        parsedMin,
        minUnit,
        parsedMax,
        maxUnit
    ) {
        MediaFilterConfig(
            contentType = selectedContentType,
            sizePreset = if (includeAllSizes) SizePreset.ALL_SIZES else selectedPreset,
            includeAllSizes = includeAllSizes,
            minSizeValue = if (parsedMin >= 0.0) parsedMin else 0.0,
            minSizeUnit = minUnit,
            maxSizeValue = if (parsedMax >= 0.0) parsedMax else 0.0,
            maxSizeUnit = maxUnit
        )
    }

    val hasInputParseError = !includeAllSizes && (minText.toDoubleOrNull() == null || maxText.toDoubleOrNull() == null)
    val validationError = when {
        hasInputParseError -> "Please enter valid numeric values for minimum and maximum file size."
        else -> draftConfig.validationErrorMessage
    }
    val isDraftValid = !hasInputParseError && draftConfig.isValid

    val matchingCount by remember(masterList, draftConfig, isDraftValid) {
        derivedStateOf {
            if (!isDraftValid) 0
            else masterList.count { !it.isCallRecording() && draftConfig.matches(it) }
        }
    }

    fun selectPreset(preset: SizePreset) {
        selectedPreset = preset
        if (preset == SizePreset.ALL_SIZES) {
            includeAllSizes = true
        } else {
            includeAllSizes = false
            val template = MediaFilterConfig.fromPreset(selectedContentType, preset)
            minText = if (template.minSizeValue % 1.0 == 0.0) template.minSizeValue.toLong().toString() else template.minSizeValue.toString()
            minUnit = template.minSizeUnit
            maxText = if (template.maxSizeValue % 1.0 == 0.0) template.maxSizeValue.toLong().toString() else template.maxSizeValue.toString()
            maxUnit = template.maxSizeUnit
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0x99000000))
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.BottomCenter
        ) {
            Surface(
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                color = Color(0xFF141624),
                tonalElevation = 12.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = false) {}
                    .border(
                        1.dp,
                        Color(0x33FFFFFF),
                        RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
                    )
                    .testTag("content_filter_panel")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Drag Handle Pill
                    Box(
                        modifier = Modifier
                            .width(42.dp)
                            .height(4.dp)
                            .clip(CircleShape)
                            .background(Color(0x55FFFFFF))
                            .align(Alignment.CenterHorizontally)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Header Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(accentColor.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FilterList,
                                    contentDescription = null,
                                    tint = accentColor,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Content & File-Size Filter",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White
                                )
                                Text(
                                    text = "Filter media without interrupting playback",
                                    fontSize = 12.sp,
                                    color = Color(0xFF9CA3AF)
                                )
                            }
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color(0x1AFFFFFF), CircleShape)
                                .testTag("close_filter_panel_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close Filter Panel",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // STEP 1: Select Content Type
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AuraBadge(text = "STEP 1", color = accentColor)
                        Text(
                            text = "Select Content Type",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        ContentTypeOptionCard(
                            filter = ContentTypeFilter.AUDIO_ONLY,
                            icon = Icons.Default.AudioFile,
                            selected = selectedContentType == ContentTypeFilter.AUDIO_ONLY,
                            accentColor = accentColor,
                            onClick = { selectedContentType = ContentTypeFilter.AUDIO_ONLY },
                            testTag = "filter_type_audio"
                        )
                        ContentTypeOptionCard(
                            filter = ContentTypeFilter.VIDEO_ONLY,
                            icon = Icons.Default.VideoFile,
                            selected = selectedContentType == ContentTypeFilter.VIDEO_ONLY,
                            accentColor = accentColor,
                            onClick = { selectedContentType = ContentTypeFilter.VIDEO_ONLY },
                            testTag = "filter_type_video"
                        )
                        ContentTypeOptionCard(
                            filter = ContentTypeFilter.BOTH,
                            icon = Icons.Default.PermMedia,
                            selected = selectedContentType == ContentTypeFilter.BOTH,
                            accentColor = accentColor,
                            onClick = { selectedContentType = ContentTypeFilter.BOTH },
                            testTag = "filter_type_both"
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // STEP 2: Configure File-Size Limits
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AuraBadge(text = "STEP 2", color = accentColor)
                            Text(
                                text = "Configure File-Size Limits",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color.White
                            )
                        }

                        // Include All File Sizes Toggle
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "All Sizes",
                                fontSize = 12.sp,
                                color = if (includeAllSizes) accentColor else Color(0xFF9CA3AF),
                                fontWeight = FontWeight.SemiBold
                            )
                            Switch(
                                checked = includeAllSizes,
                                onCheckedChange = { checked ->
                                    includeAllSizes = checked
                                    if (checked) {
                                        selectedPreset = SizePreset.ALL_SIZES
                                    } else if (selectedPreset == SizePreset.ALL_SIZES) {
                                        selectPreset(SizePreset.CUSTOM)
                                    }
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = accentColor,
                                    uncheckedThumbColor = Color(0xFF9CA3AF),
                                    uncheckedTrackColor = Color(0xFF26293E)
                                ),
                                modifier = Modifier.testTag("include_all_sizes_switch")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Quick Size Presets
                    Text(
                        text = "Quick Size Presets",
                        fontSize = 12.sp,
                        color = Color(0xFF9CA3AF),
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        SizePreset.entries.forEach { preset ->
                            val isSelected = (includeAllSizes && preset == SizePreset.ALL_SIZES) ||
                                    (!includeAllSizes && selectedPreset == preset)
                            Surface(
                                onClick = { selectPreset(preset) },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) accentColor else Color(0x1EFFFFFF),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isSelected) accentColor else Color(0x33FFFFFF)
                                ),
                                modifier = Modifier.testTag("size_preset_${preset.name.lowercase()}")
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = preset.label,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = preset.subtitle,
                                        fontSize = 10.sp,
                                        color = if (isSelected) Color.White.copy(alpha = 0.85f) else Color(0xFF9CA3AF)
                                    )
                                }
                            }
                        }
                    }

                    // Custom / Specific Min & Max File Size Inputs
                    AnimatedVisibility(visible = !includeAllSizes) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 14.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0x14FFFFFF))
                                .border(1.dp, Color(0x26FFFFFF), RoundedCornerShape(16.dp))
                                .padding(14.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Storage,
                                    contentDescription = null,
                                    tint = accentColor,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Custom Minimum & Maximum Size Range",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Minimum File Size Row
                            SizeLimitInputRow(
                                label = "Minimum Size",
                                valueText = minText,
                                onValueChange = {
                                    minText = it
                                    selectedPreset = SizePreset.CUSTOM
                                },
                                selectedUnit = minUnit,
                                onUnitChange = {
                                    minUnit = it
                                    selectedPreset = SizePreset.CUSTOM
                                },
                                accentColor = accentColor,
                                inputTestTag = "min_size_input"
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Maximum File Size Row
                            SizeLimitInputRow(
                                label = "Maximum Size",
                                valueText = maxText,
                                onValueChange = {
                                    maxText = it
                                    selectedPreset = SizePreset.CUSTOM
                                },
                                selectedUnit = maxUnit,
                                onUnitChange = {
                                    maxUnit = it
                                    selectedPreset = SizePreset.CUSTOM
                                },
                                accentColor = accentColor,
                                inputTestTag = "max_size_input"
                            )

                            // Validation Error Message if min > max
                            if (validationError != null) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0x33EF4444))
                                        .border(1.dp, Color(0xFFEF4444), RoundedCornerShape(10.dp))
                                        .padding(horizontal = 10.dp, vertical = 8.dp)
                                        .testTag("filter_validation_error"),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = Color(0xFFF87171),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = validationError,
                                        color = Color(0xFFFECACA),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Summary & Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isDraftValid) "$matchingCount matching files in library" else "Invalid size range",
                            fontSize = 12.sp,
                            color = if (isDraftValid) Color(0xFF9CA3AF) else Color(0xFFF87171),
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = draftConfig.summaryLabel,
                            fontSize = 11.sp,
                            color = accentColor,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.resetMediaFilter() },
                            shape = RoundedCornerShape(14.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x44FFFFFF)),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("reset_filters_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Reset Filters",
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            )
                        }

                        Button(
                            onClick = { viewModel.applyMediaFilter(draftConfig) },
                            enabled = isDraftValid,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = accentColor,
                                disabledContainerColor = Color(0x33FFFFFF)
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1.3f)
                                .height(48.dp)
                                .testTag("apply_filters_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Apply Filters",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
private fun ContentTypeOptionCard(
    filter: ContentTypeFilter,
    icon: ImageVector,
    selected: Boolean,
    accentColor: Color,
    onClick: () -> Unit,
    testTag: String
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = if (selected) accentColor.copy(alpha = 0.18f) else Color(0x12FFFFFF),
        border = androidx.compose.foundation.BorderStroke(
            width = if (selected) 1.5.dp else 1.dp,
            color = if (selected) accentColor else Color(0x28FFFFFF)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(if (selected) accentColor else Color(0x26FFFFFF)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = filter.label,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = filter.description,
                        color = Color(0xFF9CA3AF),
                        fontSize = 11.sp
                    )
                }
            }

            if (selected) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(accentColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SizeLimitInputRow(
    label: String,
    valueText: String,
    onValueChange: (String) -> Unit,
    selectedUnit: SizeUnit,
    onUnitChange: (SizeUnit) -> Unit,
    accentColor: Color,
    inputTestTag: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        OutlinedTextField(
            value = valueText,
            onValueChange = onValueChange,
            label = { Text(label, fontSize = 11.sp) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = accentColor,
                unfocusedBorderColor = Color(0x33FFFFFF),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedLabelColor = accentColor,
                unfocusedLabelColor = Color(0xFF9CA3AF)
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .weight(1f)
                .testTag(inputTestTag)
        )

        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0x22FFFFFF))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            SizeUnit.entries.forEach { unit ->
                val isSelected = unit == selectedUnit
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) accentColor else Color.Transparent)
                        .clickable { onUnitChange(unit) }
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                        .testTag("${inputTestTag}_unit_${unit.symbol.lowercase()}"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = unit.symbol,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) Color.White else Color(0xFFD1D5DB)
                    )
                }
            }
        }
    }
}
