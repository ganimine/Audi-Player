package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AccentTheme
import com.example.model.VisualizerStyle
import com.example.ui.components.AuraBadge
import com.example.ui.components.AuraGlassCard
import com.example.viewmodel.PlayerViewModel

@Composable
fun SettingsScreen(
    viewModel: PlayerViewModel,
    onOpenWidgetSettings: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val appName by viewModel.appName.collectAsState()
    val accentTheme by viewModel.accentTheme.collectAsState()
    val visualizerStyle by viewModel.visualizerStyle.collectAsState()
    val widgetConfig by viewModel.widgetConfig.collectAsState()
    val accentColor = Color(accentTheme.hexColor)

    var editableName by remember(appName) { mutableStateOf(appName) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState())
            .padding(bottom = 120.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Title
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Settings, contentDescription = null, tint = accentColor, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = "Player Settings",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
                Text(
                    text = "Configurable Identity, Codecs & Visualizer",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF9CA3AF)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Home Screen Widget Settings Card
        AuraGlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onOpenWidgetSettings() }
                .testTag("open_widget_settings_card"),
            cornerRadius = 20.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(accentColor.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Widgets, contentDescription = null, tint = accentColor, modifier = Modifier.size(24.dp))
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text("Home Screen Widget Settings", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 15.sp)
                        Text("Theme: ${widgetConfig.widgetTheme} • Opacity ${(widgetConfig.widgetOpacity * 100).toInt()}%", fontSize = 12.sp, color = Color(0xFF9CA3AF))
                    }
                }

                Button(
                    onClick = onOpenWidgetSettings,
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Configure", fontSize = 12.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Application Identity Configuration
        AuraGlassCard(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 20.dp
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Application Name & Branding",
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 15.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Customize the player header and title display",
                    fontSize = 12.sp,
                    color = Color(0xFF9CA3AF)
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = editableName,
                        onValueChange = { editableName = it },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = accentColor,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("app_name_input")
                    )

                    Button(
                        onClick = { viewModel.setAppName(editableName) },
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Save")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Accent Color Selection
        Text(
            text = "ACCENT COLOR THEME",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = accentColor,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(8.dp))

        AuraGlassCard(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 20.dp
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AccentTheme.values().forEach { theme ->
                    val isSelected = accentTheme == theme
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) Color(theme.hexColor).copy(alpha = 0.2f) else Color.Transparent)
                            .clickable { viewModel.setAccentTheme(theme) }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(Color(theme.hexColor))
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(theme.displayName, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        }

                        if (isSelected) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = Color(theme.hexColor), modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Visualizer Style Configuration
        Text(
            text = "AUDIO VISUALIZER STYLE",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = accentColor,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(8.dp))

        AuraGlassCard(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 20.dp
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                VisualizerStyle.values().forEach { style ->
                    val isSelected = visualizerStyle == style
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) accentColor.copy(alpha = 0.2f) else Color.Transparent)
                            .clickable { viewModel.setVisualizerStyle(style) }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Waves, contentDescription = null, tint = accentColor, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(style.displayName, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        }

                        if (isSelected) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = accentColor, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Codec Support Reference Box
        AuraGlassCard(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 20.dp
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = accentColor, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Advanced Codec Engine Specification", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Powered by AndroidX Media3 ExoPlayer with Hardware & Software decoders:",
                    fontSize = 12.sp,
                    color = Color(0xFFD1D5DB)
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    AuraBadge(text = "H.264 / AVC", color = accentColor)
                    AuraBadge(text = "H.265 / HEVC", color = accentColor)
                    AuraBadge(text = "AV1", color = accentColor)
                    AuraBadge(text = "VP9", color = accentColor)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    AuraBadge(text = "FLAC Lossless", color = Color(0xFF60A5FA))
                    AuraBadge(text = "ALAC Apple", color = Color(0xFF60A5FA))
                    AuraBadge(text = "AAC-LC", color = Color(0xFF60A5FA))
                    AuraBadge(text = "Opus / Vorbis", color = Color(0xFF60A5FA))
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    AuraBadge(text = "MP4 / MKV / WebM", color = Color(0xFF34D399))
                    AuraBadge(text = "HLS m3u8", color = Color(0xFF34D399))
                    AuraBadge(text = "Files >2.5MB", color = Color(0xFFFBBF24))
                }
            }
        }
    }
}
