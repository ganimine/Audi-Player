package com.example.ui.screens

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.AccentTheme
import com.example.model.CustomSkinConfig
import com.example.model.SkinPreset
import com.example.ui.components.AuraGlassCard
import com.example.viewmodel.PlayerViewModel

@Composable
fun SkinGalleryScreen(
    viewModel: PlayerViewModel,
    modifier: Modifier = Modifier
) {
    val currentSkin by viewModel.currentSkinPreset.collectAsState()
    val customConfig by viewModel.customSkinConfig.collectAsState()
    val accentTheme by viewModel.accentTheme.collectAsState()
    val accentColor = Color(accentTheme.hexColor)

    var previewSkin by remember(currentSkin) { mutableStateOf(currentSkin) }
    var blurVal by remember { mutableFloatStateOf(customConfig.wallpaperBlurRadius) }
    var opacityVal by remember { mutableFloatStateOf(customConfig.wallpaperOpacity) }
    var cornerRadiusVal by remember { mutableIntStateOf(customConfig.cornerRadius) }

    val wallpapers = listOf(
        "https://images.unsplash.com/photo-1531366936337-7c912a4589a7?w=1200", // Aurora
        "https://images.unsplash.com/photo-1508700115892-45ecd05ae2ad?w=1200", // Neon Synthwave
        "https://images.unsplash.com/photo-1448375240586-882707db888b?w=1200", // Nature Forest
        "https://images.unsplash.com/photo-1539375665275-f9de415ef9ac?w=1200", // Vinyl Warmth
        "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=1200", // Cosmic Sci-Fi
        "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=1200"  // Liquid Abstract
    )

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
            Icon(Icons.Default.Palette, contentDescription = null, tint = accentColor, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = "Skin & Theme Gallery",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
                Text(
                    text = "10 Pro Presets & Deep Visual Customization",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF9CA3AF)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Active Theme Preview Card
        Text(
            text = "LIVE THEME PREVIEW",
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
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .background(Color(previewSkin.backgroundColor))
            ) {
                if (!previewSkin.backgroundPhotoUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = previewSkin.backgroundPhotoUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xBB000000))
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = previewSkin.title,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(previewSkin.textColor)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = previewSkin.description,
                            fontSize = 12.sp,
                            color = Color(previewSkin.secondaryTextColor)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(18.dp)
                                    .clip(CircleShape)
                                    .background(Color(previewSkin.accentColor))
                            )
                            Box(
                                modifier = Modifier
                                    .size(18.dp)
                                    .clip(CircleShape)
                                    .background(Color(previewSkin.backgroundColor))
                                    .border(1.dp, Color.White, CircleShape)
                            )
                        }
                    }

                    Button(
                        onClick = { viewModel.setSkinPreset(previewSkin) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(previewSkin.accentColor)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Apply Theme", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 10 Built-in Theme Presets Grid / List
        Text(
            text = "BUILT-IN THEME PRESETS",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = accentColor,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(10.dp))

        SkinPreset.values().forEach { preset ->
            val isCurrent = currentSkin == preset
            val isPreview = previewSkin == preset

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (isPreview) Color(0x33FFFFFF) else Color(0x14FFFFFF))
                    .border(
                        1.dp,
                        if (isCurrent) Color(preset.accentColor) else Color(0x22FFFFFF),
                        RoundedCornerShape(16.dp)
                    )
                    .clickable { previewSkin = preset }
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Color swatch
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(preset.backgroundColor))
                        .border(1.5.dp, Color(preset.accentColor), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (isCurrent) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = Color(preset.accentColor), modifier = Modifier.size(20.dp))
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = preset.title,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 15.sp
                        )
                        if (isCurrent) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "ACTIVE",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(preset.accentColor)
                            )
                        }
                    }
                    Text(
                        text = preset.description,
                        fontSize = 12.sp,
                        color = Color(0xFF9CA3AF)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Custom Studio Wallpaper Gallery
        Text(
            text = "PHOTO WALLPAPERS & BACKDROPS",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = accentColor,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(10.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(wallpapers) { wp ->
                Box(
                    modifier = Modifier
                        .size(100.dp, 80.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(14.dp))
                        .clickable {
                            viewModel.updateCustomSkin(customConfig.copy(customWallpaperUrl = wp))
                            viewModel.setSkinPreset(SkinPreset.CUSTOM_STUDIO)
                        }
                ) {
                    AsyncImage(
                        model = wp,
                        contentDescription = "Wallpaper",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Custom Studio Sliders (Blur, Opacity, Corner Radius)
        AuraGlassCard(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 20.dp
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Custom Studio Sliders",
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Wallpaper Opacity
                Text("Wallpaper Opacity (${(opacityVal * 100).toInt()}%)", fontSize = 12.sp, color = Color(0xFFD1D5DB))
                Slider(
                    value = opacityVal,
                    onValueChange = {
                        opacityVal = it
                        viewModel.updateCustomSkin(customConfig.copy(wallpaperOpacity = it))
                    },
                    valueRange = 0.05f..0.9f,
                    colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = accentColor)
                )

                // Corner Radius
                Text("Interface Corner Radius (${cornerRadiusVal}dp)", fontSize = 12.sp, color = Color(0xFFD1D5DB))
                Slider(
                    value = cornerRadiusVal.toFloat(),
                    onValueChange = {
                        cornerRadiusVal = it.toInt()
                        viewModel.updateCustomSkin(customConfig.copy(cornerRadius = it.toInt()))
                    },
                    valueRange = 8f..32f,
                    colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = accentColor)
                )
            }
        }
    }
}
