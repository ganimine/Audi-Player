package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.model.MediaItemData
import com.example.ui.components.ArtworkCover
import com.example.ui.components.AuraBadge
import com.example.ui.components.AuraGlassCard
import com.example.viewmodel.PlayerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    viewModel: PlayerViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val mediaList by viewModel.filteredMediaList.collectAsState()
    val controller = viewModel.playerController
    val currentMedia by controller.currentMedia.collectAsState()
    val isPlaying by controller.isPlaying.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val activeFilter by viewModel.activeFilter.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    val accentTheme by viewModel.accentTheme.collectAsState()
    val accentColor = Color(accentTheme.hexColor)

    val filters = listOf("All", "Audio", "Video", "Albums", "Large Files (>2.5MB)")

    // Storage Permissions
    val requiredPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(Manifest.permission.READ_MEDIA_AUDIO, Manifest.permission.READ_MEDIA_VIDEO)
    } else {
        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    var showPermissionDialog by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val anyGranted = results.values.any { it }
        if (anyGranted) {
            viewModel.scanDeviceStorage()
        }
    }

    fun checkAndScanStorage() {
        val allGranted = requiredPermissions.all { perm ->
            ContextCompat.checkSelfPermission(context, perm) == PackageManager.PERMISSION_GRANTED
        }
        if (allGranted) {
            viewModel.scanDeviceStorage()
        } else {
            showPermissionDialog = true
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Title & Scan button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Media Library",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
                Text(
                    text = "${mediaList.size} tracks & albums loaded",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF9CA3AF)
                )
            }

            Surface(
                onClick = { checkAndScanStorage() },
                shape = RoundedCornerShape(12.dp),
                color = Color(0x33FFFFFF),
                modifier = Modifier.testTag("scan_device_storage_button")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    if (isScanning) {
                        CircularProgressIndicator(
                            color = accentColor,
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Scan Storage",
                            tint = accentColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isScanning) "Scanning..." else "Scan Storage",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Search Input
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.setSearchQuery(it) },
            placeholder = { Text("Search songs, albums, artists, codecs...", color = Color(0xFF6B7280), fontSize = 14.sp) },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = "Search", tint = Color(0xFF9CA3AF))
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { viewModel.setSearchQuery("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Color(0xFF9CA3AF))
                    }
                }
            },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = accentColor,
                unfocusedBorderColor = Color(0x33FFFFFF),
                focusedContainerColor = Color(0x1AFFFFFF),
                unfocusedContainerColor = Color(0x12FFFFFF),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("library_search_input")
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Filter Chips Row
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(filters) { f ->
                val selected = f == activeFilter
                FilterChip(
                    selected = selected,
                    onClick = { viewModel.setActiveFilter(f) },
                    label = { Text(f, fontSize = 12.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal) },
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

        Spacer(modifier = Modifier.height(12.dp))

        // Content Area: Either Albums Grid or Tracks List
        if (activeFilter == "Albums") {
            // Group by Album Name and show Album Cards with Art
            val albumGroups = mediaList.groupBy { it.album }
            if (albumGroups.isEmpty()) {
                EmptyLibraryView(onScan = { checkAndScanStorage() })
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(bottom = 120.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(albumGroups.entries.toList()) { (albumName, tracks) ->
                        val sampleTrack = tracks.first()
                        AuraGlassCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.playMedia(sampleTrack, tracks)
                                },
                            cornerRadius = 16.dp
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                ArtworkCover(
                                    media = sampleTrack,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(1f),
                                    cornerRadius = 12.dp
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = albumName,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = sampleTrack.artist,
                                    fontSize = 11.sp,
                                    color = Color(0xFF9CA3AF),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    AuraBadge(text = "${tracks.size} tracks", color = accentColor)
                                    sampleTrack.year?.let {
                                        Text(text = it, fontSize = 10.sp, color = Color(0xFF6B7280))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Standard Track List
            if (mediaList.isEmpty()) {
                EmptyLibraryView(onScan = { checkAndScanStorage() })
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 120.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(mediaList) { item ->
                        val isCurrent = currentMedia?.id == item.id
                        LibraryMediaRow(
                            media = item,
                            isCurrent = isCurrent,
                            isPlaying = isCurrent && isPlaying,
                            accentColor = accentColor,
                            onClick = { viewModel.playMedia(item) },
                            onToggleFavorite = { viewModel.toggleFavorite(item) },
                            onAddToPlaylist = { viewModel.setSelectedPlaylistForAdd(item) }
                        )
                    }
                }
            }
        }
    }

    // Storage Permission Dialog
    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = "Storage Access Permission",
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 18.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "AUDI Player needs storage access to scan your local device files for music, 4K videos, album covers, artist metadata, and ID3 tags.",
                        color = Color(0xFFD1D5DB),
                        fontSize = 13.sp
                    )
                    Text(
                        text = "• Extracts album name, artist, and embedded cover art\n• Plays files of any size (> 2.5MB, FLAC, MKV, MP4)\n• All media stays private on your device",
                        color = Color(0xFF9CA3AF),
                        fontSize = 12.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showPermissionDialog = false
                        permissionLauncher.launch(requiredPermissions)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Grant Permission", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionDialog = false }) {
                    Text("Not Now", color = Color(0xFF9CA3AF))
                }
            },
            containerColor = Color(0xFF1B1D2C)
        )
    }
}

@Composable
private fun EmptyLibraryView(onScan: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.FolderOpen, contentDescription = null, tint = Color(0xFF6B7280), modifier = Modifier.size(48.dp))
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "No media files found",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Tap 'Scan Storage' to scan device music and videos, or open files above 2.5MB using the file picker.",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF9CA3AF),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onScan,
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Scan Device Storage")
            }
        }
    }
}

@Composable
private fun LibraryMediaRow(
    media: MediaItemData,
    isCurrent: Boolean,
    isPlaying: Boolean,
    accentColor: Color,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onAddToPlaylist: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (isCurrent) accentColor.copy(alpha = 0.15f) else Color(0x0CFFFFFF))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ArtworkCover(
            media = media,
            modifier = Modifier.size(50.dp),
            cornerRadius = 12.dp,
            showPlayingBadge = true,
            isPlaying = isPlaying
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = media.title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = if (isCurrent) accentColor else Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(3.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "${media.artist} • ${media.album}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF9CA3AF),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                AuraBadge(text = media.codec, color = Color(0xFF60A5FA))
                AuraBadge(text = media.formattedFileSize, color = accentColor)
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = media.formattedDuration,
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF9CA3AF)
        )

        // More Actions Menu
        Box {
            IconButton(
                onClick = { showMenu = true },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(Icons.Default.MoreVert, contentDescription = "More", tint = Color(0xFF9CA3AF), modifier = Modifier.size(20.dp))
            }

            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text(if (media.isFavorite) "Remove from Favorites" else "Add to Favorites") },
                    leadingIcon = {
                        Icon(
                            imageVector = if (media.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = null,
                            tint = accentColor
                        )
                    },
                    onClick = {
                        onToggleFavorite()
                        showMenu = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("Add to Playlist") },
                    leadingIcon = {
                        Icon(Icons.Default.PlaylistAdd, contentDescription = null, tint = Color.White)
                    },
                    onClick = {
                        onAddToPlaylist()
                        showMenu = false
                    }
                )
            }
        }
    }
}
