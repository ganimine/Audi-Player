package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.MediaItemData
import com.example.model.RepeatMode
import com.example.model.VisualizerStyle
import com.example.player.AuraPlayerController
import com.example.ui.components.AURA_GRADIENTS
import com.example.ui.components.AnimatedVisualizerView
import com.example.ui.components.ArtworkCover
import com.example.ui.components.AuraBadge
import com.example.ui.components.AuraGlassCard
import com.example.viewmodel.PlayerViewModel

@Composable
fun NowPlayingScreen(
    viewModel: PlayerViewModel,
    onCollapse: () -> Unit,
    modifier: Modifier = Modifier
) {
    val controller = viewModel.playerController
    val currentMedia by controller.currentMedia.collectAsState()
    val isPlaying by controller.isPlaying.collectAsState()
    val positionMs by controller.positionMs.collectAsState()
    val durationMs by controller.durationMs.collectAsState()
    val speed by controller.playbackSpeed.collectAsState()
    val repeatMode by controller.repeatMode.collectAsState()
    val isShuffle by controller.isShuffle.collectAsState()
    val visualizerBars by controller.visualizerAmplitudes.collectAsState()
    val visualizerStyle by viewModel.visualizerStyle.collectAsState()
    val accentTheme by viewModel.accentTheme.collectAsState()
    val accentColor = Color(accentTheme.hexColor)
    val sleepTimerSec by controller.sleepTimerSeconds.collectAsState()
    val isAbLoopActive by controller.isAbLoopActive.collectAsState()
    val isAudioOnlyForVideo by controller.isAudioOnlyForVideo.collectAsState()
    val aspectRatioMode by controller.aspectRatioMode.collectAsState()

    var showSpeedMenu by remember { mutableStateOf(false) }
    var showSleepMenu by remember { mutableStateOf(false) }
    var activeViewTab by remember { mutableStateOf(0) } // 0: Artwork, 1: Waveform / Visualizer, 2: Video (if video)

    if (currentMedia == null) return

    val item = currentMedia!!
    val isVideo = item.isVideo && !isAudioOnlyForVideo

    // Background Gradient matching artwork
    val gradientColors = AURA_GRADIENTS[item.gradientColorIndex % AURA_GRADIENTS.size]
    val ambientBackground = Brush.verticalGradient(
        colors = listOf(
            gradientColors[0].copy(alpha = 0.45f),
            gradientColors[1].copy(alpha = 0.25f),
            Color(0xFF090A10)
        )
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF090A10))
            .background(ambientBackground)
    ) {
        if (isVideo && activeViewTab == 2) {
            // Full Video View Mode
            VideoPlayerView(
                playerController = controller,
                currentMedia = item,
                isPlaying = isPlaying,
                positionMs = positionMs,
                durationMs = durationMs,
                aspectRatioMode = aspectRatioMode,
                accentColor = accentColor,
                onClose = { activeViewTab = 0 },
                onOpenCodecInfo = { viewModel.setCodecInfoOpen(true) },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // Audio / Apple Music Full Player Layout
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                // Drag indicator / Top Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onCollapse,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Collapse Player",
                            tint = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    // Playing from Library / Album Label
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "PLAYING FROM",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF9CA3AF),
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = item.album,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    IconButton(
                        onClick = { viewModel.setCodecInfoOpen(true) },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Codec Details",
                            tint = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Mode switch pills (Artwork vs Visualizer vs Video if video)
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0x33FFFFFF))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    TabPill(
                        label = "Artwork",
                        selected = activeViewTab == 0,
                        accentColor = accentColor,
                        onClick = { activeViewTab = 0 }
                    )
                    TabPill(
                        label = "Visualizer",
                        selected = activeViewTab == 1,
                        accentColor = accentColor,
                        onClick = { activeViewTab = 1 }
                    )
                    if (item.isVideo) {
                        TabPill(
                            label = "Video Screen",
                            selected = activeViewTab == 2,
                            accentColor = accentColor,
                            onClick = { activeViewTab = 2 }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Center Art / Visualizer Display
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.88f)
                        .aspectRatio(1f)
                        .shadow(24.dp, RoundedCornerShape(28.dp), ambientColor = accentColor.copy(alpha = 0.4f))
                        .clip(RoundedCornerShape(28.dp))
                        .background(Color(0x18FFFFFF))
                        .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(28.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (activeViewTab == 0) {
                        ArtworkCover(
                            media = item,
                            modifier = Modifier.fillMaxSize(),
                            cornerRadius = 28.dp,
                            showPlayingBadge = false
                        )
                    } else {
                        AnimatedVisualizerView(
                            amplitudes = visualizerBars,
                            style = visualizerStyle,
                            accentColor = accentColor,
                            isPlaying = isPlaying,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Title, Artist and Badges
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = item.artist,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFB0B5C6),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    IconButton(
                        onClick = { viewModel.toggleFavorite(item) },
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(
                            imageVector = if (item.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (item.isFavorite) accentColor else Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Tech Badges Row: Codec, File Size (>2.5MB), Lossless / Hi-Res
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AuraBadge(text = item.codec, color = accentColor)
                    AuraBadge(text = item.formattedFileSize, color = Color(0xFF60A5FA))
                    AuraBadge(text = item.bitrate, color = Color(0xFF34D399))
                    if (item.isVideo) {
                        AuraBadge(text = item.resolution ?: "HD", color = Color(0xFFFBBF24))
                    }
                }

                Spacer(modifier = Modifier.height(22.dp))

                // Progress Bar & Duration
                val progress = if (durationMs > 0) (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f
                Slider(
                    value = progress,
                    onValueChange = { frac ->
                        controller.seekTo((frac * durationMs).toLong())
                    },
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = accentColor,
                        inactiveTrackColor = Color(0x33FFFFFF)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("now_playing_slider")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatDuration(positionMs),
                        fontSize = 12.sp,
                        color = Color(0xFF9CA3AF),
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "-" + formatDuration((durationMs - positionMs).coerceAtLeast(0L)),
                        fontSize = 12.sp,
                        color = Color(0xFF9CA3AF),
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Primary Playback Controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Shuffle
                    IconButton(onClick = { controller.toggleShuffle() }) {
                        Icon(
                            imageVector = Icons.Default.Shuffle,
                            contentDescription = "Shuffle",
                            tint = if (isShuffle) accentColor else Color.White.copy(alpha = 0.55f),
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Seek Backward 10s
                    IconButton(onClick = { controller.seekRelative(-10000L) }) {
                        Icon(
                            imageVector = Icons.Default.FastRewind,
                            contentDescription = "-10s",
                            tint = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    // Previous Track
                    IconButton(onClick = { controller.playPrevious() }) {
                        Icon(
                            imageVector = Icons.Default.SkipPrevious,
                            contentDescription = "Previous",
                            tint = Color.White,
                            modifier = Modifier.size(34.dp)
                        )
                    }

                    // Big Play/Pause
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .shadow(16.dp, CircleShape, ambientColor = accentColor.copy(alpha = 0.4f))
                            .clip(CircleShape)
                            .background(accentColor)
                            .clickable { controller.togglePlayPause() }
                            .testTag("now_playing_play_pause"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = Color.White,
                            modifier = Modifier.size(40.dp)
                        )
                    }

                    // Next Track
                    IconButton(onClick = { controller.playNext() }) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = "Next",
                            tint = Color.White,
                            modifier = Modifier.size(34.dp)
                        )
                    }

                    // Seek Forward 10s
                    IconButton(onClick = { controller.seekRelative(10000L) }) {
                        Icon(
                            imageVector = Icons.Default.FastForward,
                            contentDescription = "+10s",
                            tint = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    // Repeat Mode
                    IconButton(onClick = { controller.toggleRepeatMode() }) {
                        Icon(
                            imageVector = if (repeatMode == RepeatMode.ONE) Icons.Default.RepeatOne else Icons.Default.Repeat,
                            contentDescription = "Repeat",
                            tint = if (repeatMode != RepeatMode.OFF) accentColor else Color.White.copy(alpha = 0.55f),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Bottom VLC Tools Toolbar (Equalizer, Speed, Sleep Timer, A-B Loop, Queue)
                AuraGlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 16.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Equalizer shortcut
                        IconButton(onClick = { viewModel.selectTab(3) }) {
                            Icon(
                                imageVector = Icons.Default.Equalizer,
                                contentDescription = "Equalizer",
                                tint = Color.White.copy(alpha = 0.85f),
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        // Playback Speed dropdown
                        Box {
                            IconButton(onClick = { showSpeedMenu = true }) {
                                Text(
                                    text = "${speed}x",
                                    color = if (speed != 1.0f) accentColor else Color.White.copy(alpha = 0.85f),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                            DropdownMenu(
                                expanded = showSpeedMenu,
                                onDismissRequest = { showSpeedMenu = false }
                            ) {
                                listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f, 2.5f).forEach { s ->
                                    DropdownMenuItem(
                                        text = { Text("${s}x ${if (s == 1.0f) "(Normal)" else ""}") },
                                        onClick = {
                                            controller.setSpeed(s)
                                            showSpeedMenu = false
                                        }
                                    )
                                }
                            }
                        }

                        // Sleep Timer
                        Box {
                            IconButton(onClick = { showSleepMenu = true }) {
                                Icon(
                                    imageVector = Icons.Default.Timer,
                                    contentDescription = "Sleep Timer",
                                    tint = if (sleepTimerSec != null) accentColor else Color.White.copy(alpha = 0.85f),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            DropdownMenu(
                                expanded = showSleepMenu,
                                onDismissRequest = { showSleepMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Off") },
                                    onClick = { controller.setSleepTimer(null); showSleepMenu = false }
                                )
                                listOf(15, 30, 45, 60).forEach { mins ->
                                    DropdownMenuItem(
                                        text = { Text("$mins minutes") },
                                        onClick = { controller.setSleepTimer(mins); showSleepMenu = false }
                                    )
                                }
                            }
                        }

                        // A-B Loop Button
                        IconButton(onClick = {
                            if (!isAbLoopActive) {
                                if (controller.aLoopMs.value == null) {
                                    controller.setPointA()
                                } else {
                                    controller.setPointB()
                                }
                            } else {
                                controller.clearAbLoop()
                            }
                        }) {
                            Text(
                                text = if (isAbLoopActive) "A-B On" else if (controller.aLoopMs.value != null) "Set B" else "A-B",
                                color = if (isAbLoopActive) accentColor else Color.White.copy(alpha = 0.85f),
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }

                        // Queue / Up Next Sheet
                        IconButton(onClick = { viewModel.setQueueSheetOpen(true) }) {
                            Icon(
                                imageVector = Icons.Default.QueueMusic,
                                contentDescription = "Queue",
                                tint = Color.White.copy(alpha = 0.85f),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun TabPill(
    label: String,
    selected: Boolean,
    accentColor: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) accentColor else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) Color.White else Color(0xFFD1D5DB)
        )
    }
}

private fun formatDuration(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}
