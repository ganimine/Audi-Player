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
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.MediaItemData
import com.example.ui.components.ArtworkCover
import com.example.ui.components.AuraBadge
import com.example.ui.components.AuraGlassCard
import com.example.viewmodel.PlayerViewModel

/**
 * In-car dashboard player interface optimized for vehicle touchscreens and driver accessibility.
 * Features oversized high-visibility touch controls, speed/motion safety telemetry,
 * full library song browser with one-touch song selection, and driving restriction enforcement.
 */
@Composable
fun CarDashboardScreen(
    viewModel: PlayerViewModel,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val controller = viewModel.playerController
    val currentMedia by controller.currentMedia.collectAsState()
    val isPlaying by controller.isPlaying.collectAsState()
    val positionMs by controller.positionMs.collectAsState()
    val durationMs by controller.durationMs.collectAsState()
    val queue by controller.queue.collectAsState()
    val masterList by viewModel.masterMediaList.collectAsState()
    val playlists by viewModel.playlists.collectAsState()
    val showVideoFiles by viewModel.showVideoFiles.collectAsState()
    val strict4to5MbFilter by viewModel.strict4to5MbFilter.collectAsState()
    val accentTheme by viewModel.accentTheme.collectAsState()
    val accentColor = Color(accentTheme.hexColor)

    val drivingMode = controller.drivingModeManager
    val isMoving by drivingMode.isVehicleMoving.collectAsState()
    val currentSpeed by drivingMode.currentSpeedKmh.collectAsState()
    val motionDesc by drivingMode.motionStatusDescription.collectAsState()
    val drivingWarning by controller.drivingWarning.collectAsState()

    val streamVolume by viewModel.streamVolume.collectAsState()
    val maxVolume = viewModel.maxStreamVolume

    var carSearchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All Songs") } // "All Songs", "4MB - 5MB", "Queue", "Playlists", "Albums"

    // Filter available playable tracks in car mode (strictly exclude videos unless enabled, and exclude call recordings)
    val availableAudioTracks = remember(masterList, showVideoFiles, strict4to5MbFilter) {
        masterList.filter { item ->
            if (item.isCallRecording()) return@filter false
            if (item.isVideo && !showVideoFiles) return@filter false
            true
        }
    }

    val displayTracks = remember(availableAudioTracks, queue, selectedCategory, carSearchQuery) {
        val baseList = when (selectedCategory) {
            "4MB - 5MB" -> availableAudioTracks.filter { it.isTargetSize4to5Mb }
            "Queue" -> if (queue.isNotEmpty()) queue else availableAudioTracks
            else -> availableAudioTracks
        }
        if (carSearchQuery.isBlank()) {
            baseList
        } else {
            baseList.filter {
                it.title.contains(carSearchQuery, ignoreCase = true) ||
                it.artist.contains(carSearchQuery, ignoreCase = true) ||
                it.album.contains(carSearchQuery, ignoreCase = true)
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF07080C))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Bar: Car Header, Safety Speedometer & Exit Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(if (isMoving) Color(0x33FF9800) else Color(0x3310B981)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.DirectionsCar,
                            contentDescription = "Car Player Dashboard",
                            tint = if (isMoving) Color(0xFFFF9800) else Color(0xFF10B981),
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "AUDI CAR DASHBOARD",
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.2.sp,
                            fontSize = 15.sp,
                            color = Color.White
                        )
                        Text(
                            text = if (isMoving) "In Motion • ${currentSpeed.toInt()} km/h (Audio Safe)" else "Parked • Audio System Ready",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isMoving) Color(0xFFFFB74D) else Color(0xFF34D399)
                        )
                    }
                }

                // Exit Car Mode Button
                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color(0x33FFFFFF))
                        .testTag("exit_car_dashboard")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Exit Car Mode",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // In-Motion Video Restriction Alert (if applicable)
            if (drivingWarning != null || (isMoving && currentMedia?.isVideo == true)) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0x33FF9800),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF9800)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color(0xFFFF9800),
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "DRIVING MODE ACTIVE",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Color.White
                            )
                            Text(
                                drivingWarning ?: "Video playback restricted while driving. Streaming audio only for safety.",
                                fontSize = 12.sp,
                                color = Color(0xFFFFD180),
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Current Track Glass Card with Large Artwork & Driving Controls
            val media = currentMedia
            AuraGlassCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 24.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (media != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(90.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .border(1.5.dp, Color(0x33FFFFFF), RoundedCornerShape(16.dp))
                            ) {
                                ArtworkCover(
                                    media = media,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = media.title,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${media.artist} • ${media.album}",
                                    fontSize = 13.sp,
                                    color = Color(0xFFD1D5DB),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    AuraBadge(text = media.codec, color = accentColor)
                                    AuraBadge(text = media.bitrate, color = Color(0xFF60A5FA))
                                }
                            }
                        }
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(vertical = 12.dp)
                        ) {
                            Icon(Icons.Default.MusicNote, contentDescription = null, tint = accentColor, modifier = Modifier.size(36.dp))
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Select any song below to start playing in car mode",
                                fontSize = 14.sp,
                                color = Color(0xFF9CA3AF),
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Large Touch Scrub Bar
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Slider(
                            value = if (durationMs > 0) (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f,
                            onValueChange = { frac ->
                                controller.seekTo((frac * durationMs).toLong())
                            },
                            colors = SliderDefaults.colors(
                                thumbColor = accentColor,
                                activeTrackColor = accentColor,
                                inactiveTrackColor = Color(0x33FFFFFF)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = formatTime(positionMs),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF9CA3AF)
                            )
                            Text(
                                text = formatTime(durationMs),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF9CA3AF)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Giant Driving Touch Controls (68dp - 88dp touch targets)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Prev Track Button (64dp)
                        IconButton(
                            onClick = { controller.playPrevious() },
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(Color(0x22FFFFFF))
                                .testTag("car_prev_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.SkipPrevious,
                                contentDescription = "Previous Song",
                                tint = Color.White,
                                modifier = Modifier.size(34.dp)
                            )
                        }

                        // Rewind 15s Button (52dp)
                        IconButton(
                            onClick = { controller.seekRelative(-15000L) },
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(Color(0x1AFFFFFF))
                        ) {
                            Icon(
                                imageVector = Icons.Default.FastRewind,
                                contentDescription = "-15s",
                                tint = Color(0xFFE5E7EB),
                                modifier = Modifier.size(26.dp)
                            )
                        }

                        // Giant Play/Pause Primary Action Button (84dp)
                        Box(
                            modifier = Modifier
                                .size(84.dp)
                                .clip(CircleShape)
                                .background(accentColor)
                                .clickable {
                                    if (currentMedia == null) {
                                        val first = displayTracks.firstOrNull() ?: masterList.firstOrNull()
                                        first?.let { viewModel.playMedia(it, displayTracks) }
                                    } else {
                                        controller.togglePlayPause()
                                    }
                                }
                                .testTag("car_play_pause_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                tint = Color.White,
                                modifier = Modifier.size(44.dp)
                            )
                        }

                        // Forward 15s Button (52dp)
                        IconButton(
                            onClick = { controller.seekRelative(15000L) },
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(Color(0x1AFFFFFF))
                        ) {
                            Icon(
                                imageVector = Icons.Default.FastForward,
                                contentDescription = "+15s",
                                tint = Color(0xFFE5E7EB),
                                modifier = Modifier.size(26.dp)
                            )
                        }

                        // Next Track Button (64dp)
                        IconButton(
                            onClick = { controller.playNext() },
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(Color(0x22FFFFFF))
                                .testTag("car_next_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.SkipNext,
                                contentDescription = "Next Song",
                                tint = Color.White,
                                modifier = Modifier.size(34.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Vehicle Volume Slider
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (streamVolume == 0) Icons.Default.VolumeMute else Icons.Default.VolumeDown,
                            contentDescription = null,
                            tint = Color(0xFF9CA3AF),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Slider(
                            value = streamVolume.toFloat(),
                            onValueChange = { viewModel.setStreamVolume(it.toInt()) },
                            valueRange = 0f..maxVolume.toFloat(),
                            colors = SliderDefaults.colors(
                                thumbColor = Color.White,
                                activeTrackColor = Color.White,
                                inactiveTrackColor = Color(0x33FFFFFF)
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Default.VolumeUp,
                            contentDescription = null,
                            tint = Color(0xFF9CA3AF),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // ==========================================
            // CAR DASHBOARD TO PLAY SONGS (LIBRARY & QUEUE)
            // ==========================================
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "DASHBOARD TO PLAY SONGS",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = Color.White
                )
                Text(
                    text = "${displayTracks.size} Songs Available",
                    fontSize = 11.sp,
                    color = accentColor,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Category Filter Chips
            val carCategories = listOf("All Songs", "4MB - 5MB", "Queue (${queue.size})", "Albums", "Playlists")
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(carCategories) { cat ->
                    val selected = (cat == selectedCategory) || (cat.startsWith("Queue") && selectedCategory == "Queue")
                    FilterChip(
                        selected = selected,
                        onClick = {
                            selectedCategory = if (cat.startsWith("Queue")) "Queue" else cat
                        },
                        label = {
                            Text(
                                text = cat,
                                fontSize = 12.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = accentColor,
                            selectedLabelColor = Color.White,
                            containerColor = Color(0x22FFFFFF),
                            labelColor = Color(0xFFD1D5DB)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Quick In-Car Search Bar
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0x18FFFFFF),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x33FFFFFF)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = accentColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedTextField(
                        value = carSearchQuery,
                        onValueChange = { carSearchQuery = it },
                        placeholder = { Text("Search songs to play in car...", color = Color(0xFF6B7280), fontSize = 13.sp) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    if (carSearchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = { carSearchQuery = "" },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Color(0xFF9CA3AF), modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Display Playlists if Playlists category is selected
            if (selectedCategory == "Playlists") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    playlists.forEach { pl ->
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0x221D212E),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x22FFFFFF)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val plTracks = availableAudioTracks.filter { pl.mediaIds.contains(it.id) }
                                    if (plTracks.isNotEmpty()) {
                                        viewModel.playMedia(plTracks.first(), plTracks)
                                    }
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(accentColor.copy(alpha = 0.25f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.PlaylistPlay, contentDescription = null, tint = accentColor, modifier = Modifier.size(26.dp))
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(pl.name, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 15.sp)
                                    Text("${pl.mediaIds.size} songs • Tap to play entire playlist", fontSize = 12.sp, color = Color(0xFF9CA3AF))
                                }
                                Icon(Icons.Default.PlayArrow, contentDescription = "Play", tint = accentColor, modifier = Modifier.size(28.dp))
                            }
                        }
                    }
                }
            } else if (selectedCategory == "Albums") {
                // Display Albums
                val albums = availableAudioTracks.groupBy { it.album }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    albums.entries.forEach { (albumName, tracks) ->
                        val sampleTrack = tracks.first()
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0x221D212E),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x22FFFFFF)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.playMedia(sampleTrack, tracks)
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                ) {
                                    ArtworkCover(media = sampleTrack, modifier = Modifier.fillMaxSize())
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(albumName, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text("${sampleTrack.artist} • ${tracks.size} tracks", fontSize = 12.sp, color = Color(0xFF9CA3AF))
                                }
                                Icon(Icons.Default.PlayArrow, contentDescription = "Play Album", tint = accentColor, modifier = Modifier.size(28.dp))
                            }
                        }
                    }
                }
            } else {
                // Large Driving-Friendly Song List: Direct 1-tap playback
                if (displayTracks.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No matching songs found in library.", color = Color(0xFF9CA3AF), fontSize = 14.sp)
                    }
                } else {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        displayTracks.forEach { track ->
                            val isCurrent = track.id == currentMedia?.id
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = if (isCurrent) Color(0x33FF2D55) else Color(0x221D212E),
                                border = androidx.compose.foundation.BorderStroke(
                                    if (isCurrent) 1.5.dp else 1.dp,
                                    if (isCurrent) accentColor else Color(0x22FFFFFF)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.playMedia(track, displayTracks)
                                    }
                                    .testTag("car_track_${track.id}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(54.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                    ) {
                                        ArtworkCover(media = track, modifier = Modifier.fillMaxSize())
                                        if (isCurrent) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .background(Color.Black.copy(alpha = 0.4f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                                    contentDescription = null,
                                                    tint = accentColor,
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = track.title,
                                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.SemiBold,
                                            fontSize = 15.sp,
                                            color = if (isCurrent) accentColor else Color.White,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "${track.artist} • ${track.album}",
                                            fontSize = 12.sp,
                                            color = Color(0xFF9CA3AF),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = track.formattedDuration,
                                                fontSize = 11.sp,
                                                color = Color(0xFFD1D5DB),
                                                fontWeight = FontWeight.Medium
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            AuraBadge(text = track.codec, color = if (isCurrent) accentColor else Color(0xFF60A5FA))
                                            if (track.isTargetSize4to5Mb) {
                                                Spacer(modifier = Modifier.width(4.dp))
                                                AuraBadge(text = "4-5MB", color = Color(0xFF10B981))
                                            }
                                        }
                                    }

                                    // Action Play Button
                                    IconButton(
                                        onClick = {
                                            if (isCurrent) {
                                                controller.togglePlayPause()
                                            } else {
                                                viewModel.playMedia(track, displayTracks)
                                            }
                                        },
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(CircleShape)
                                            .background(if (isCurrent) accentColor else Color(0x33FFFFFF))
                                    ) {
                                        Icon(
                                            imageVector = if (isCurrent && isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                            contentDescription = if (isCurrent && isPlaying) "Pause" else "Play",
                                            tint = Color.White,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Test & Simulation Mode Bar in Car Dashboard
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0x1AFFFFFF),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Motion Telemetry: $motionDesc",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { drivingMode.simulateSpeed(0f) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0x33FFFFFF)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Park (0)", fontSize = 11.sp, color = Color.White)
                        }
                        Button(
                            onClick = { drivingMode.simulateSpeed(55f) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0x33FF9800)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Drive (55)", fontSize = 11.sp, color = Color.White)
                        }
                        Button(
                            onClick = { drivingMode.stopSimulation() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0x33EF4444)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Reset", fontSize = 11.sp, color = Color.White)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}
