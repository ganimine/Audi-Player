package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Loop
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.SurroundSound
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.DEFAULT_EQUALIZER_PRESETS
import com.example.model.EqualizerPreset
import com.example.ui.components.AuraGlassCard
import com.example.viewmodel.PlayerViewModel

@Composable
fun EqualizerScreen(
    viewModel: PlayerViewModel,
    modifier: Modifier = Modifier
) {
    val controller = viewModel.playerController
    val bands by controller.equalizerBands.collectAsState()
    val currentPreset by controller.currentPreset.collectAsState()
    val bassBoost by controller.bassBoostStrength.collectAsState()
    val virtualizer by controller.virtualizerStrength.collectAsState()
    val speed by controller.playbackSpeed.collectAsState()
    val aLoopMs by controller.aLoopMs.collectAsState()
    val bLoopMs by controller.bLoopMs.collectAsState()
    val isAbLoopActive by controller.isAbLoopActive.collectAsState()
    val accentTheme by viewModel.accentTheme.collectAsState()
    val accentColor = Color(accentTheme.hexColor)

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState())
            .padding(bottom = 120.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Title
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.GraphicEq,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = "VLC Audio Equalizer",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
                Text(
                    text = "Studio DSP & Spatial Audio Enhancement",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF9CA3AF)
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Preset Chips
        Text(
            text = "EQUALIZER PRESETS",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = accentColor,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(8.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(DEFAULT_EQUALIZER_PRESETS) { preset ->
                val selected = currentPreset == preset.name
                FilterChip(
                    selected = selected,
                    onClick = { controller.applyPreset(preset) },
                    label = { Text(preset.name, fontSize = 12.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = accentColor,
                        selectedLabelColor = Color.White,
                        containerColor = Color(0x22FFFFFF),
                        labelColor = Color(0xFFD1D5DB)
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        borderColor = Color(0x33FFFFFF),
                        selectedBorderColor = accentColor,
                        enabled = true,
                        selected = selected
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 5-Band Equalizer Sliders Card
        AuraGlassCard(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 20.dp
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Graphic Frequency Spectrum (-10dB to +10dB)",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(14.dp))

                bands.forEach { band ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = band.freqDisplay,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFD1D5DB),
                            modifier = Modifier.width(55.dp)
                        )

                        Slider(
                            value = band.levelDb,
                            onValueChange = { newVal ->
                                controller.setBandLevel(band.bandIndex, newVal)
                            },
                            valueRange = -10f..10f,
                            colors = SliderDefaults.colors(
                                thumbColor = Color.White,
                                activeTrackColor = accentColor,
                                inactiveTrackColor = Color(0x33FFFFFF)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("eq_band_${band.bandIndex}")
                        )

                        Text(
                            text = String.format("%+.1fdB", band.levelDb),
                            fontSize = 11.sp,
                            color = if (band.levelDb != 0f) accentColor else Color(0xFF9CA3AF),
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.width(55.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Bass Boost & 3D Virtualizer Controls
        Text(
            text = "ACOUSTIC EFFECTS",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = accentColor,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Bass Boost Card
            AuraGlassCard(
                modifier = Modifier.weight(1f),
                cornerRadius = 18.dp
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.VolumeUp, contentDescription = null, tint = accentColor, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Bass Boost", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = "${bassBoost / 10}%", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = accentColor)
                    Slider(
                        value = bassBoost.toFloat(),
                        onValueChange = { controller.setBassBoost(it.toInt()) },
                        valueRange = 0f..1000f,
                        colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = accentColor)
                    )
                }
            }

            // 3D Virtualizer Card
            AuraGlassCard(
                modifier = Modifier.weight(1f),
                cornerRadius = 18.dp
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.SurroundSound, contentDescription = null, tint = Color(0xFF60A5FA), modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Spatial 3D", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = "${virtualizer / 10}%", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF60A5FA))
                    Slider(
                        value = virtualizer.toFloat(),
                        onValueChange = { controller.setVirtualizer(it.toInt()) },
                        valueRange = 0f..1000f,
                        colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = Color(0xFF60A5FA))
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Playback Speed Slider
        AuraGlassCard(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 18.dp
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Speed, contentDescription = null, tint = accentColor, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Playback Speed", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                    }
                    Text("${speed}x", fontWeight = FontWeight.Bold, color = accentColor, fontSize = 14.sp)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Slider(
                    value = speed,
                    onValueChange = { controller.setSpeed(it) },
                    valueRange = 0.5f..2.5f,
                    steps = 7,
                    colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = accentColor)
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // VLC A-B Repeat Loop Controller
        AuraGlassCard(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 18.dp
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Loop, contentDescription = null, tint = accentColor, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("VLC A-B Repeat Loop", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = if (isAbLoopActive) "Loop active between Point A and Point B" else "Pinpoint a clip loop for study or music practice",
                    fontSize = 12.sp,
                    color = Color(0xFF9CA3AF)
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = { controller.setPointA() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (aLoopMs != null) accentColor else Color(0x33FFFFFF)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (aLoopMs != null) "A: ${(aLoopMs!! / 1000)}s" else "Set Point A", fontSize = 12.sp)
                    }

                    Button(
                        onClick = { controller.setPointB() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (bLoopMs != null) accentColor else Color(0x33FFFFFF)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (bLoopMs != null) "B: ${(bLoopMs!! / 1000)}s" else "Set Point B", fontSize = 12.sp)
                    }

                    Button(
                        onClick = { controller.clearAbLoop() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0x22FFFFFF)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Clear", fontSize = 12.sp, color = Color(0xFFD1D5DB))
                    }
                }
            }
        }
    }
}
