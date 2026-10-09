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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Podcasts
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.MediaItemData
import com.example.ui.components.ArtworkCover
import com.example.ui.components.AuraBadge
import com.example.ui.components.AuraGlassCard
import com.example.ui.components.getGradientForIndex
import com.example.viewmodel.PlayerViewModel

@Composable
fun ListenNowScreen(
    viewModel: PlayerViewModel,
    onOpenFilePicker: () -> Unit,
    modifier: Modifier = Modifier
) {
    val masterList by viewModel.masterMediaList.collectAsState()
    val controller = viewModel.playerController
    val currentPlaying by controller.currentMedia.collectAsState()
    val isPlaying by controller.isPlaying.collectAsState()
    val accentTheme by viewModel.accentTheme.collectAsState()
    val appName by viewModel.appName.collectAsState()
    val accentColor = Color(accentTheme.hexColor)

    val heroItem = masterList.firstOrNull { it.isVideo } ?: masterList.firstOrNull()
    val audioItems = masterList.filter { !it.isVideo }
    val videoItems = masterList.filter { it.isVideo }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // App Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = appName,
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = Color.White
                    )
                    Text(
                        text = "VLC Power • Apple Music Elegance",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF9CA3AF)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Open File Button (SAF - Any size > 2.5MB)
                    Surface(
                        onClick = onOpenFilePicker,
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0x33FFFFFF),
                        modifier = Modifier.testTag("open_storage_file_button")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FolderOpen,
                                contentDescription = "Open File",
                                tint = accentColor,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Open File",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        }
                    }

                    // Open Stream URL
                    Surface(
                        onClick = { viewModel.setStreamUrlDialogOpen(true) },
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0x33FFFFFF),
                        modifier = Modifier.testTag("open_stream_button")
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Link,
                                contentDescription = "Stream URL",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }

        // Featured Hero Card
        if (heroItem != null) {
            item {
                Column {
                    Text(
                        text = "FEATURED MEDIA",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = accentColor,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    AuraGlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.playMedia(heroItem) }
                            .testTag("featured_hero_card"),
                        cornerRadius = 24.dp
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                        ) {
                            if (!heroItem.artworkUrl.isNullOrBlank()) {
                                AsyncImage(
                                    model = heroItem.artworkUrl,
                                    contentDescription = heroItem.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(getGradientForIndex(heroItem.gradientColorIndex))
                                )
                            }

                            // Dark Gradient Scrim
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(Color.Transparent, Color(0xCC000000), Color(0xF00A0B10))
                                        )
                                    )
                            )

                            // Content inside hero
                            Column(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(16.dp)
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    AuraBadge(text = if (heroItem.isVideo) "VIDEO" else "AUDIO", color = accentColor)
                                    AuraBadge(text = heroItem.codec, color = Color(0xFF60A5FA))
                                    AuraBadge(text = heroItem.formattedFileSize, color = Color(0xFF34D399))
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = heroItem.title,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = heroItem.artist,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFD1D5DB)
                                )
                            }

                            // Play Floating Pill
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(16.dp)
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(accentColor),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Play",
                                    tint = Color.White,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Quick Picks (Lossless & Hi-Res Music)
        item {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Lossless Audio Showcase",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                    Text(
                        text = "See All",
                        fontSize = 12.sp,
                        color = accentColor,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable { viewModel.selectTab(1) }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(end = 16.dp)
                ) {
                    items(audioItems) { audio ->
                        val isThisPlaying = currentPlaying?.id == audio.id && isPlaying
                        Column(
                            modifier = Modifier
                                .width(140.dp)
                                .clickable { viewModel.playMedia(audio) }
                        ) {
                            ArtworkCover(
                                media = audio,
                                modifier = Modifier
                                    .size(140.dp),
                                cornerRadius = 18.dp,
                                showPlayingBadge = true,
                                isPlaying = isThisPlaying
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = audio.title,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = audio.artist,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF9CA3AF),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            AuraBadge(text = audio.formattedFileSize, color = accentColor)
                        }
                    }
                }
            }
        }

        // 4K & HD Video Cinema Showcase
        item {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "VLC Video Theater (H.264 / HEVC)",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                    Text(
                        text = "Videos",
                        fontSize = 12.sp,
                        color = accentColor,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable { viewModel.selectTab(1) }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(end = 16.dp)
                ) {
                    items(videoItems) { video ->
                        Column(
                            modifier = Modifier
                                .width(220.dp)
                                .clickable {
                                    viewModel.playMedia(video)
                                    viewModel.setNowPlayingExpanded(true)
                                }
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(125.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color(0x22FFFFFF))
                            ) {
                                if (!video.artworkUrl.isNullOrBlank()) {
                                    AsyncImage(
                                        model = video.artworkUrl,
                                        contentDescription = video.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(getGradientForIndex(video.gradientColorIndex)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Movie, contentDescription = null, tint = Color.White, modifier = Modifier.size(36.dp))
                                    }
                                }

                                // Play icon pill
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.Center)
                                        .size(36.dp)
                                        .background(Color(0xAA000000), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                                }

                                // Video duration badge
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(6.dp)
                                        .background(Color(0xCC000000), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(text = video.formattedDuration, fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = video.title,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = video.artist,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF9CA3AF),
                                    maxLines = 1
                                )
                                AuraBadge(text = video.formattedFileSize, color = accentColor)
                            }
                        }
                    }
                }
            }
        }
    }
}
