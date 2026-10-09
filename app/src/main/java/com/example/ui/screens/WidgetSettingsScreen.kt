package com.example.ui.screens

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.os.Build
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.WidgetSettingsConfig
import com.example.ui.components.ArtworkCover
import com.example.ui.components.AuraBadge
import com.example.ui.components.AuraGlassCard
import com.example.viewmodel.PlayerViewModel
import com.example.widget.AudiPlayerWidgetProvider

@Composable
fun WidgetSettingsScreen(
    viewModel: PlayerViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val widgetConfig by viewModel.widgetConfig.collectAsState()
    val controller = viewModel.playerController
    val currentMedia by controller.currentMedia.collectAsState()
    val isPlaying by controller.isPlaying.collectAsState()
    val accentTheme by viewModel.accentTheme.collectAsState()
    val accentColor = Color(accentTheme.hexColor)

    val widgetThemes = listOf("Matches Skin", "Apple Frosted Glass", "AMOLED Black", "VLC Orange")

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
            Icon(Icons.Default.Widgets, contentDescription = null, tint = accentColor, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = "Home Screen Widget Settings",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
                Text(
                    text = "Customize layout, theme, and transparency",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF9CA3AF)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Live Interactive Widget Preview
        Text(
            text = "LIVE WIDGET PREVIEW",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = accentColor,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(8.dp))

        val previewBgColor = when (widgetConfig.widgetTheme) {
            "AMOLED Black" -> Color.Black
            "VLC Orange" -> Color(0xFF1E140C)
            "Apple Frosted Glass" -> Color(0x33FFFFFF)
            else -> Color(0xF2131522)
        }

        val previewAccent = when (widgetConfig.widgetTheme) {
            "VLC Orange" -> Color(0xFFFF6A00)
            "Apple Frosted Glass" -> Color(0xFFFA233B)
            "AMOLED Black" -> Color(0xFF30D158)
            else -> accentColor
        }

        // Simulated Widget Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(16.dp, RoundedCornerShape(widgetConfig.cornerRadiusDp.dp))
                .clip(RoundedCornerShape(widgetConfig.cornerRadiusDp.dp))
                .background(previewBgColor.copy(alpha = widgetConfig.widgetOpacity))
                .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(widgetConfig.cornerRadiusDp.dp))
                .padding(14.dp)
                .testTag("live_widget_preview")
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Artwork thumbnail
                if (widgetConfig.showArtwork) {
                    ArtworkCover(
                        media = currentMedia,
                        modifier = Modifier.size(56.dp),
                        cornerRadius = 10.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                }

                // Title & Artist
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = currentMedia?.title ?: "AUDI Player",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = currentMedia?.artist ?: "Lossless & 4K Media Player",
                        color = Color(0xFFD1D5DB),
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (widgetConfig.showEqualizerBars) {
                        Spacer(modifier = Modifier.height(3.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = currentMedia?.album ?: "Hi-Res Audio",
                                color = Color(0xFF9CA3AF),
                                fontSize = 10.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            AuraBadge(text = currentMedia?.codec ?: "FLAC", color = previewAccent)
                        }
                    }

                    if (widgetConfig.showProgressBar) {
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { 0.45f },
                            color = previewAccent,
                            trackColor = Color(0x33FFFFFF),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(3.dp)
                                .clip(CircleShape)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Playback Buttons
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { controller.playPrevious() },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.SkipPrevious, contentDescription = "Prev", tint = Color.White, modifier = Modifier.size(20.dp))
                    }

                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(previewAccent)
                            .clickable { controller.togglePlayPause() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Play/Pause",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    IconButton(
                        onClick = { controller.playNext() },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.SkipNext, contentDescription = "Next", tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Pin to Home Screen Action Button
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            if (appWidgetManager.isRequestPinAppWidgetSupported) {
                Button(
                    onClick = {
                        val provider = ComponentName(context, AudiPlayerWidgetProvider::class.java)
                        appWidgetManager.requestPinAppWidget(provider, null, null)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Pin Widget to Home Screen", fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Widget Theme Selection
        Text(
            text = "WIDGET COLOR THEME",
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
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                widgetThemes.forEach { theme ->
                    val isSelected = widgetConfig.widgetTheme == theme
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) accentColor.copy(alpha = 0.2f) else Color.Transparent)
                            .clickable {
                                viewModel.updateWidgetConfig(widgetConfig.copy(widgetTheme = theme))
                            }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(theme, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        if (isSelected) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = accentColor, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Widget Customization Sliders
        AuraGlassCard(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 20.dp
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Appearance & Geometry", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(12.dp))

                // Opacity
                Text("Widget Opacity (${(widgetConfig.widgetOpacity * 100).toInt()}%)", fontSize = 12.sp, color = Color(0xFFD1D5DB))
                Slider(
                    value = widgetConfig.widgetOpacity,
                    onValueChange = {
                        viewModel.updateWidgetConfig(widgetConfig.copy(widgetOpacity = it))
                    },
                    valueRange = 0.2f..1.0f,
                    colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = accentColor)
                )

                // Corner Radius
                Text("Corner Radius (${widgetConfig.cornerRadiusDp}dp)", fontSize = 12.sp, color = Color(0xFFD1D5DB))
                Slider(
                    value = widgetConfig.cornerRadiusDp.toFloat(),
                    onValueChange = {
                        viewModel.updateWidgetConfig(widgetConfig.copy(cornerRadiusDp = it.toInt()))
                    },
                    valueRange = 8f..28f,
                    colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = accentColor)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Content Toggles
        AuraGlassCard(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 20.dp
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("Display Options", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)

                // Show Artwork Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Show Album Artwork", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Text("Display embedded album cover thumbnail", color = Color(0xFF9CA3AF), fontSize = 11.sp)
                    }
                    Switch(
                        checked = widgetConfig.showArtwork,
                        onCheckedChange = {
                            viewModel.updateWidgetConfig(widgetConfig.copy(showArtwork = it))
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = accentColor)
                    )
                }

                // Show Progress Bar Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Show Playback Progress", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Text("Display live track scrubber progress bar", color = Color(0xFF9CA3AF), fontSize = 11.sp)
                    }
                    Switch(
                        checked = widgetConfig.showProgressBar,
                        onCheckedChange = {
                            viewModel.updateWidgetConfig(widgetConfig.copy(showProgressBar = it))
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = accentColor)
                    )
                }

                // Show Album / Codec Badges Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Show Codec & Album Info", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Text("Include album title and audio format pill", color = Color(0xFF9CA3AF), fontSize = 11.sp)
                    }
                    Switch(
                        checked = widgetConfig.showEqualizerBars,
                        onCheckedChange = {
                            viewModel.updateWidgetConfig(widgetConfig.copy(showEqualizerBars = it))
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = accentColor)
                    )
                }
            }
        }
    }
}
