package com.example.ui.screens

import android.app.Activity
import android.content.Context
import android.media.AudioManager
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.BrightnessHigh
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.model.AspectRatioMode
import com.example.model.MediaItemData
import com.example.player.AuraPlayerController
import com.example.ui.components.AuraBadge
import kotlinx.coroutines.delay

@OptIn(UnstableApi::class)
@Composable
fun VideoPlayerView(
    playerController: AuraPlayerController,
    currentMedia: MediaItemData,
    isPlaying: Boolean,
    positionMs: Long,
    durationMs: Long,
    aspectRatioMode: AspectRatioMode,
    accentColor: Color,
    onClose: () -> Unit,
    onOpenCodecInfo: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val maxVolume = remember { audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).toFloat() }

    var areControlsVisible by remember { mutableStateOf(true) }
    var hudText by remember { mutableStateOf<String?>(null) }
    var hudIcon by remember { mutableStateOf(Icons.Default.VolumeUp) }
    var hudProgress by remember { mutableFloatStateOf(0.5f) }
    var isHudVisible by remember { mutableStateOf(false) }

    // Auto hide controls timer
    LaunchedEffect(areControlsVisible, isPlaying) {
        if (areControlsVisible && isPlaying) {
            delay(4000L)
            areControlsVisible = false
        }
    }

    // Hide HUD after 1.5 seconds
    LaunchedEffect(isHudVisible) {
        if (isHudVisible) {
            delay(1500L)
            isHudVisible = false
        }
    }

    val resizeMode = when (aspectRatioMode) {
        AspectRatioMode.FIT -> AspectRatioFrameLayout.RESIZE_MODE_FIT
        AspectRatioMode.FILL -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
        AspectRatioMode.FIXED_16_9 -> AspectRatioFrameLayout.RESIZE_MODE_FIT
        AspectRatioMode.FIXED_4_3 -> AspectRatioFrameLayout.RESIZE_MODE_FIT
        AspectRatioMode.STRETCH -> AspectRatioFrameLayout.RESIZE_MODE_FILL
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        areControlsVisible = !areControlsVisible
                    },
                    onDoubleTap = { offset ->
                        val screenWidth = size.width
                        if (offset.x < screenWidth * 0.4f) {
                            // Double tap left: rewind 10s
                            playerController.seekRelative(-10000L)
                            hudText = "-10s"
                            hudIcon = Icons.Default.FastRewind
                            hudProgress = 0f
                            isHudVisible = true
                        } else if (offset.x > screenWidth * 0.6f) {
                            // Double tap right: forward 10s
                            playerController.seekRelative(10000L)
                            hudText = "+10s"
                            hudIcon = Icons.Default.FastForward
                            hudProgress = 1f
                            isHudVisible = true
                        } else {
                            playerController.togglePlayPause()
                        }
                    }
                )
            }
    ) {
        // AndroidView Media3 PlayerView
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = playerController.exoPlayer
                    useController = false
                    this.resizeMode = resizeMode
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            update = { view ->
                view.resizeMode = resizeMode
            },
            modifier = Modifier.fillMaxSize()
        )

        // Gesture Overlay: Left for Brightness, Right for Volume
        Row(modifier = Modifier.fillMaxSize()) {
            // Left Half: Screen Brightness swipe
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull() ?: continue
                                if (change.pressed && change.previousPosition.y != change.position.y) {
                                    val deltaY = change.previousPosition.y - change.position.y
                                    val activity = context as? Activity
                                    activity?.let { act ->
                                        val lp = act.window.attributes
                                        var currentB = lp.screenBrightness
                                        if (currentB < 0f) currentB = 0.5f
                                        val newB = (currentB + deltaY / 800f).coerceIn(0.05f, 1.0f)
                                        lp.screenBrightness = newB
                                        act.window.attributes = lp

                                        hudText = "Brightness ${(newB * 100).toInt()}%"
                                        hudIcon = Icons.Default.BrightnessHigh
                                        hudProgress = newB
                                        isHudVisible = true
                                    }
                                }
                            }
                        }
                    }
            )

            // Right Half: Volume swipe
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull() ?: continue
                                if (change.pressed && change.previousPosition.y != change.position.y) {
                                    val deltaY = change.previousPosition.y - change.position.y
                                    val currentVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                                    val changeSteps = (deltaY / 50f).toInt()
                                    if (changeSteps != 0) {
                                        val newVol = (currentVol + changeSteps).coerceIn(0, maxVolume.toInt())
                                        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVol, 0)
                                        hudText = "Volume ${(newVol / maxVolume * 100).toInt()}%"
                                        hudIcon = Icons.Default.VolumeUp
                                        hudProgress = newVol / maxVolume
                                        isHudVisible = true
                                    }
                                }
                            }
                        }
                    }
            )
        }

        // HUD Pill in Center when gestures trigger
        AnimatedVisibility(
            visible = isHudVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Surface(
                color = Color(0xDD12131C),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x33FFFFFF)),
                modifier = Modifier.padding(24.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
                ) {
                    Icon(
                        imageVector = hudIcon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = hudText ?: "",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    if (hudIcon == Icons.Default.VolumeUp || hudIcon == Icons.Default.BrightnessHigh) {
                        Spacer(modifier = Modifier.height(10.dp))
                        LinearProgressIndicator(
                            progress = { hudProgress },
                            color = accentColor,
                            trackColor = Color(0x44FFFFFF),
                            modifier = Modifier
                                .width(120.dp)
                                .height(4.dp)
                                .clip(CircleShape)
                        )
                    }
                }
            }
        }

        // Sleek Video Controls Overlay (Apple Music frosted glass + AUDI controls)
        AnimatedVisibility(
            visible = areControlsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x66000000))
            ) {
                // Top Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .padding(horizontal = 16.dp, vertical = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        IconButton(
                            onClick = onClose,
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color(0x66000000), CircleShape)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Close Video", tint = Color.White)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = currentMedia.title,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                fontSize = 15.sp
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = currentMedia.artist,
                                    color = Color(0xFFD1D5DB),
                                    fontSize = 12.sp,
                                    maxLines = 1
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                currentMedia.resolution?.let {
                                    AuraBadge(text = it, color = accentColor)
                                }
                            }
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Aspect Ratio Toggle
                        IconButton(
                            onClick = {
                                val nextMode = when (aspectRatioMode) {
                                    AspectRatioMode.FIT -> AspectRatioMode.FILL
                                    AspectRatioMode.FILL -> AspectRatioMode.FIXED_16_9
                                    AspectRatioMode.FIXED_16_9 -> AspectRatioMode.STRETCH
                                    AspectRatioMode.FIXED_4_3 -> AspectRatioMode.FIT
                                    AspectRatioMode.STRETCH -> AspectRatioMode.FIT
                                }
                                playerController.setAspectRatio(nextMode)
                                hudText = nextMode.label
                                hudIcon = Icons.Default.AspectRatio
                                isHudVisible = true
                            },
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color(0x66000000), CircleShape)
                        ) {
                            Icon(Icons.Default.AspectRatio, contentDescription = "Aspect Ratio", tint = Color.White)
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Audio Only / Background Playback
                        IconButton(
                            onClick = {
                                playerController.toggleAudioOnlyForVideo()
                                hudText = if (playerController.isAudioOnlyForVideo.value) "Audio Only Active" else "Video Mode"
                                hudIcon = Icons.Default.Headphones
                                isHudVisible = true
                            },
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color(0x66000000), CircleShape)
                        ) {
                            Icon(Icons.Default.Headphones, contentDescription = "Audio Only", tint = Color.White)
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Codec Info
                        IconButton(
                            onClick = onOpenCodecInfo,
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color(0x66000000), CircleShape)
                        ) {
                            Icon(Icons.Default.Info, contentDescription = "Codec Info", tint = Color.White)
                        }
                    }
                }

                // Center Play/Pause button
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(28.dp)
                ) {
                    IconButton(
                        onClick = { playerController.seekRelative(-10000L) },
                        modifier = Modifier
                            .size(52.dp)
                            .background(Color(0x77000000), CircleShape)
                    ) {
                        Icon(Icons.Default.FastRewind, contentDescription = "-10s", tint = Color.White, modifier = Modifier.size(30.dp))
                    }

                    IconButton(
                        onClick = { playerController.togglePlayPause() },
                        modifier = Modifier
                            .size(72.dp)
                            .background(accentColor, CircleShape)
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = Color.White,
                            modifier = Modifier.size(42.dp)
                        )
                    }

                    IconButton(
                        onClick = { playerController.seekRelative(10000L) },
                        modifier = Modifier
                            .size(52.dp)
                            .background(Color(0x77000000), CircleShape)
                    ) {
                        Icon(Icons.Default.FastForward, contentDescription = "+10s", tint = Color.White, modifier = Modifier.size(30.dp))
                    }
                }

                // Bottom Scrubber Bar & Timestamps
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 20.dp, vertical = 24.dp)
                ) {
                    val progress = if (durationMs > 0) (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f

                    Slider(
                        value = progress,
                        onValueChange = { frac ->
                            playerController.seekTo((frac * durationMs).toLong())
                        },
                        colors = SliderDefaults.colors(
                            thumbColor = Color.White,
                            activeTrackColor = accentColor,
                            inactiveTrackColor = Color(0x55FFFFFF)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = formatTime(positionMs),
                            color = Color(0xFFE5E7EB),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = formatTime(durationMs),
                            color = Color(0xFF9CA3AF),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    val totalSecs = (ms / 1000).coerceAtLeast(0)
    val mins = totalSecs / 60
    val secs = totalSecs % 60
    val hours = mins / 60
    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, mins % 60, secs)
    } else {
        String.format("%02d:%02d", mins, secs)
    }
}
