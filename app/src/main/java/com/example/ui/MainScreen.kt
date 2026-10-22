package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.CustomSkinConfig
import com.example.model.SkinPreset
import com.example.ui.components.AppleMiniPlayer
import com.example.ui.components.TopNavMiniPlayer
import com.example.ui.screens.AddToPlaylistDialog
import com.example.ui.screens.CarDashboardScreen
import com.example.ui.screens.CodecInfoDialog
import com.example.ui.screens.ContentFilterSheet
import com.example.ui.screens.EqualizerScreen
import com.example.ui.screens.FullScreenSearchScreen
import com.example.ui.screens.HeadsetControlsDialog
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.ListenNowScreen
import com.example.ui.screens.NowPlayingScreen
import com.example.ui.screens.PlaylistsScreen
import com.example.ui.screens.QueueSheet
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SkinGalleryScreen
import com.example.ui.screens.StreamUrlDialog
import com.example.ui.screens.WidgetSettingsScreen
import com.example.viewmodel.PlayerViewModel

@Composable
fun MainScreen(
    viewModel: PlayerViewModel,
    modifier: Modifier = Modifier
) {
    val appName by viewModel.appName.collectAsState()
    val selectedTab by viewModel.selectedTab.collectAsState()
    val controller = viewModel.playerController
    val currentMedia by controller.currentMedia.collectAsState()
    val isPlaying by controller.isPlaying.collectAsState()
    val positionMs by controller.positionMs.collectAsState()
    val durationMs by controller.durationMs.collectAsState()
    val isNowPlayingExpanded by viewModel.isNowPlayingExpanded.collectAsState()
    val isCarDashboardOpen by viewModel.isCarDashboardOpen.collectAsState()
    val isFullScreenSearchOpen by viewModel.isFullScreenSearchOpen.collectAsState()
    val isFilterSheetOpen by viewModel.isFilterSheetOpen.collectAsState()
    val mediaFilterConfig by viewModel.mediaFilterConfig.collectAsState()
    val isCodecInfoOpen by viewModel.isCodecInfoOpen.collectAsState()
    val isQueueSheetOpen by viewModel.isQueueSheetOpen.collectAsState()
    val isStreamUrlOpen by viewModel.isStreamUrlDialogOpen.collectAsState()
    val selectedPlaylistForAdd by viewModel.selectedPlaylistForAdd.collectAsState()
    val isHeadsetControlsOpen by viewModel.isHeadsetControlsDialogOpen.collectAsState()
    val importStatusMessage by viewModel.importStatusMessage.collectAsState()
    val interruptionStatus by viewModel.interruptionStatus.collectAsState()
    val playlists by viewModel.playlists.collectAsState()

    val currentSkin by viewModel.currentSkinPreset.collectAsState()
    val customSkinConfig by viewModel.customSkinConfig.collectAsState()
    val accentTheme by viewModel.accentTheme.collectAsState()
    val accentColor = Color(accentTheme.hexColor)

    // Storage Access Framework launcher for files of any size (> 2.5MB)
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.importFileUri(it) }
    }

    var isWidgetSettingsOpen by remember { mutableStateOf(false) }

    // Intercept Back button if modal/sheet or dialog is open
    BackHandler(enabled = isFilterSheetOpen || isFullScreenSearchOpen || isNowPlayingExpanded || isCarDashboardOpen || isWidgetSettingsOpen || isHeadsetControlsOpen) {
        if (isFilterSheetOpen) {
            viewModel.setFilterSheetOpen(false)
        } else if (isFullScreenSearchOpen) {
            viewModel.setFullScreenSearchOpen(false)
        } else if (isCarDashboardOpen) {
            viewModel.setCarDashboardOpen(false)
        } else if (isNowPlayingExpanded) {
            viewModel.setNowPlayingExpanded(false)
        } else if (isHeadsetControlsOpen) {
            viewModel.setHeadsetControlsDialogOpen(false)
        } else if (isWidgetSettingsOpen) {
            isWidgetSettingsOpen = false
        }
    }

    // Background Image Wallpaper from Skin Preset or Custom Studio
    val bgPhotoUrl = if (currentSkin == SkinPreset.CUSTOM_STUDIO) {
        customSkinConfig.customWallpaperUrl ?: currentSkin.backgroundPhotoUrl
    } else {
        currentSkin.backgroundPhotoUrl
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Color(currentSkin.backgroundColor))
    ) {
        val isWideScreen = maxWidth > 650.dp

        // Ambient Wallpaper Background
        if (!bgPhotoUrl.isNullOrBlank()) {
            AsyncImage(
                model = bgPhotoUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            // Adaptive Scrim
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 1f - (if (currentSkin == SkinPreset.CUSTOM_STUDIO) customSkinConfig.wallpaperOpacity else 0.45f)))
            )
        }

        Row(modifier = Modifier.fillMaxSize()) {
            // Navigation Rail for Wide Screens
            if (isWideScreen) {
                NavigationRail(
                    containerColor = Color(0xF0131522),
                    contentColor = Color.White,
                    modifier = Modifier.fillMaxHeight()
                ) {
                    Spacer(modifier = Modifier.height(16.dp))
                    NavItemsList.forEachIndexed { index, item ->
                        NavigationRailItem(
                            selected = selectedTab == index,
                            onClick = { viewModel.selectTab(index) },
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label, fontSize = 11.sp) },
                            colors = NavigationRailItemDefaults.colors(
                                selectedIconColor = Color.White,
                                selectedTextColor = accentColor,
                                indicatorColor = accentColor,
                                unselectedIconColor = Color(0xFF9CA3AF),
                                unselectedTextColor = Color(0xFF9CA3AF)
                            )
                        )
                    }
                }
            }

            // Main Content Area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
            ) {
                Scaffold(
                    containerColor = Color.Transparent,
                    topBar = {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .statusBarsPadding()
                        ) {
                            TopNavMiniPlayer(
                                appName = appName,
                                currentMedia = currentMedia,
                                isPlaying = isPlaying,
                                accentColor = accentColor,
                                onExpandNowPlaying = { viewModel.setNowPlayingExpanded(true) },
                                onTogglePlayPause = { controller.togglePlayPause() },
                                onNext = { controller.playNext() },
                                onOpenHeadsetControls = { viewModel.setHeadsetControlsDialogOpen(true) },
                                onOpenFilePicker = {
                                    filePickerLauncher.launch(arrayOf("audio/*", "video/*", "*/*"))
                                },
                                onOpenCarDashboard = { viewModel.setCarDashboardOpen(true) },
                                onOpenSearch = { viewModel.setFullScreenSearchOpen(true) },
                                onOpenFilter = { viewModel.setFilterSheetOpen(true) },
                                isFilterActive = mediaFilterConfig.isCustomOrNonDefault,
                                statusMessage = importStatusMessage ?: interruptionStatus,
                                onDismissStatusMessage = {
                                    viewModel.clearImportStatusMessage()
                                    viewModel.clearInterruptionStatus()
                                }
                            )
                        }
                    },
                    bottomBar = {
                        if (!isWideScreen) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .navigationBarsPadding()
                            ) {
                                // Floating Mini Player (Bottom Bar Only)
                                if (currentMedia != null && !isNowPlayingExpanded) {
                                    AppleMiniPlayer(
                                        currentMedia = currentMedia!!,
                                        isPlaying = isPlaying,
                                        progressMs = positionMs,
                                        durationMs = durationMs,
                                        accentColor = accentColor,
                                        onExpand = { viewModel.setNowPlayingExpanded(true) },
                                        onTogglePlayPause = { controller.togglePlayPause() },
                                        onNext = { controller.playNext() }
                                    )
                                }

                                // Apple Music Frosted Bottom Navigation Bar
                                NavigationBar(
                                    containerColor = Color(0xEB131522),
                                    tonalElevation = 8.dp,
                                    modifier = Modifier.height(64.dp)
                                ) {
                                    NavItemsList.forEachIndexed { index, item ->
                                        NavigationBarItem(
                                            selected = selectedTab == index,
                                            onClick = { viewModel.selectTab(index) },
                                            icon = { Icon(item.icon, contentDescription = item.label, modifier = Modifier.size(22.dp)) },
                                            label = { Text(item.label, fontSize = 10.sp, fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal) },
                                            colors = NavigationBarItemDefaults.colors(
                                                selectedIconColor = Color.White,
                                                selectedTextColor = accentColor,
                                                indicatorColor = accentColor,
                                                unselectedIconColor = Color(0xFF9CA3AF),
                                                unselectedTextColor = Color(0xFF9CA3AF)
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        when (selectedTab) {
                            0 -> ListenNowScreen(
                                viewModel = viewModel,
                                onOpenFilePicker = {
                                    filePickerLauncher.launch(arrayOf("audio/*", "video/*", "*/*"))
                                }
                            )
                            1 -> LibraryScreen(viewModel = viewModel)
                            2 -> PlaylistsScreen(viewModel = viewModel)
                            3 -> EqualizerScreen(viewModel = viewModel)
                            4 -> SkinGalleryScreen(viewModel = viewModel)
                            5 -> {
                                if (isWidgetSettingsOpen) {
                                    Column(modifier = Modifier.fillMaxSize()) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(start = 12.dp, top = 8.dp)
                                        ) {
                                            IconButton(onClick = { isWidgetSettingsOpen = false }) {
                                                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                                            }
                                            Text(
                                                text = "Back to Settings",
                                                color = Color.White,
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 14.sp
                                            )
                                        }
                                        WidgetSettingsScreen(viewModel = viewModel)
                                    }
                                } else {
                                    SettingsScreen(
                                        viewModel = viewModel,
                                        onOpenWidgetSettings = { isWidgetSettingsOpen = true }
                                    )
                                }
                            }
                        }
                    }
                }

                // If wide screen, render Mini Player floating at bottom right
                if (isWideScreen && currentMedia != null && !isNowPlayingExpanded) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(20.dp)
                            .width(420.dp)
                    ) {
                        AppleMiniPlayer(
                            currentMedia = currentMedia!!,
                            isPlaying = isPlaying,
                            progressMs = positionMs,
                            durationMs = durationMs,
                            accentColor = accentColor,
                            onExpand = { viewModel.setNowPlayingExpanded(true) },
                            onTogglePlayPause = { controller.togglePlayPause() },
                            onNext = { controller.playNext() }
                        )
                    }
                }
            }
        }

        // Full Screen Apple Music Now Playing Sheet
        AnimatedVisibility(
            visible = isNowPlayingExpanded && currentMedia != null,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier.fillMaxSize()
        ) {
            NowPlayingScreen(
                viewModel = viewModel,
                onCollapse = { viewModel.setNowPlayingExpanded(false) }
            )
        }

        // Full Screen Car Dashboard Player
        AnimatedVisibility(
            visible = isCarDashboardOpen,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier.fillMaxSize()
        ) {
            CarDashboardScreen(
                viewModel = viewModel,
                onClose = { viewModel.setCarDashboardOpen(false) }
            )
        }

        // Dedicated Full-Page Search Screen
        AnimatedVisibility(
            visible = isFullScreenSearchOpen,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier.fillMaxSize()
        ) {
            FullScreenSearchScreen(
                viewModel = viewModel,
                onDismiss = { viewModel.setFullScreenSearchOpen(false) }
            )
        }

        // Dedicated Content Selection & File-Size Filter Panel (auto-closes on apply/reset)
        if (isFilterSheetOpen) {
            ContentFilterSheet(
                viewModel = viewModel,
                onDismiss = { viewModel.setFilterSheetOpen(false) }
            )
        }

        // Codec & Stream Info Dialog
        if (isCodecInfoOpen) {
            CodecInfoDialog(
                media = currentMedia,
                accentColor = accentColor,
                onDismiss = { viewModel.setCodecInfoOpen(false) }
            )
        }

        // Up Next Queue Sheet
        if (isQueueSheetOpen) {
            QueueSheet(
                viewModel = viewModel,
                onDismiss = { viewModel.setQueueSheetOpen(false) }
            )
        }

        // Network Stream URL Dialog
        if (isStreamUrlOpen) {
            StreamUrlDialog(
                accentColor = accentColor,
                onDismiss = { viewModel.setStreamUrlDialogOpen(false) },
                onPlayUrl = { url, title ->
                    viewModel.openNetworkStream(url, title)
                }
            )
        }

        // Add to Playlist Dialog
        if (selectedPlaylistForAdd != null) {
            AddToPlaylistDialog(
                media = selectedPlaylistForAdd,
                playlists = playlists,
                accentColor = accentColor,
                onDismiss = { viewModel.setSelectedPlaylistForAdd(null) },
                onSelectPlaylist = { plId ->
                    viewModel.addToPlaylist(plId, selectedPlaylistForAdd!!.id)
                }
            )
        }

        // Headset & Earphones Controls Hub Dialog
        if (isHeadsetControlsOpen) {
            HeadsetControlsDialog(
                viewModel = viewModel,
                onDismiss = { viewModel.setHeadsetControlsDialogOpen(false) }
            )
        }
    }
}

private data class NavItem(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

private val NavItemsList = listOf(
    NavItem("Home", Icons.Default.PlayCircleFilled),
    NavItem("Library", Icons.Default.LibraryMusic),
    NavItem("Playlists", Icons.Default.QueueMusic),
    NavItem("Equalizer", Icons.Default.Equalizer),
    NavItem("Skins", Icons.Default.Palette),
    NavItem("Settings", Icons.Default.Settings)
)
