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
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PhoneCallback
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AccentTheme
import com.example.model.VisualizerStyle
import com.example.ui.components.AuraBadge
import com.example.ui.components.AuraGlassCard
import com.example.ui.components.GoogleColoredText
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
    val strict4to5MbFilter by viewModel.strict4to5MbFilter.collectAsState()
    val interruptionStatus by viewModel.interruptionStatus.collectAsState()
    val isDucked by viewModel.isDucked.collectAsState()
    val activeCallState by viewModel.activeCallState.collectAsState()
    val autoResumeOnCallEnd by viewModel.autoResumeOnCallEnd.collectAsState()
    val duckVolumeFactor by viewModel.duckVolumeFactor.collectAsState()
    val dedicatedCallInterruptionOnly by viewModel.dedicatedCallInterruptionOnly.collectAsState()
    val headset = viewModel.headsetController
    val lastGesture by headset.lastDetectedGesture.collectAsState()
    val showHomeScreenOnStartup by viewModel.showHomeScreenOnStartup.collectAsState()
    val defaultStartupScreen by viewModel.defaultStartupScreen.collectAsState()
    val rememberLastScreen by viewModel.rememberLastScreen.collectAsState()
    val showVideoFiles by viewModel.showVideoFiles.collectAsState()
    val autoPlayVideos by viewModel.autoPlayVideos.collectAsState()
    val resumePreviousPlayback by viewModel.resumePreviousPlayback.collectAsState()
    val searchHistoryEnabled by viewModel.searchHistoryEnabled.collectAsState()
    val searchHistory by viewModel.searchHistory.collectAsState()
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

        // Startup Screen & Launch Behavior Card
        AuraGlassCard(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 20.dp
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(accentColor.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Home, contentDescription = null, tint = accentColor, modifier = Modifier.size(22.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Startup Screen & Navigation", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 15.sp)
                        Text("Customize launch behavior & default screen", fontSize = 12.sp, color = Color(0xFF9CA3AF))
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 1. Show Home Screen on Startup
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Show Home Screen on Startup", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Text("When disabled, opens directly to your Music Library without featured banners. Home remains accessible manually.", color = Color(0xFF9CA3AF), fontSize = 11.sp)
                    }
                    Switch(
                        checked = showHomeScreenOnStartup,
                        onCheckedChange = { viewModel.setShowHomeScreenOnStartup(it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = accentColor),
                        modifier = Modifier.testTag("show_home_startup_switch")
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 2. Open Last-Used Screen
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Open Last-Used Screen", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Text("Remember your last active section across restarts", color = Color(0xFF9CA3AF), fontSize = 11.sp)
                    }
                    Switch(
                        checked = rememberLastScreen,
                        onCheckedChange = { viewModel.setRememberLastScreen(it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = accentColor),
                        modifier = Modifier.testTag("remember_last_screen_switch")
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 3. Resume Previous Playback
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Resume Previous Playback", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Text("Restore last played track and position in mini-player", color = Color(0xFF9CA3AF), fontSize = 11.sp)
                    }
                    Switch(
                        checked = resumePreviousPlayback,
                        onCheckedChange = { viewModel.setResumePreviousPlayback(it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = accentColor)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text("Preferred Default Screen:", color = Color(0xFFD1D5DB), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val screens = listOf(1 to "Library", 2 to "Playlists", 3 to "Equalizer", 0 to "Home")
                    screens.forEach { (idx, label) ->
                        val selected = defaultStartupScreen == idx
                        Button(
                            onClick = { viewModel.setDefaultStartupScreen(idx) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selected) accentColor else Color(0x22FFFFFF)
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(label, fontSize = 10.sp, color = Color.White, maxLines = 1)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Video File Visibility & Playback Restrictions Card
        AuraGlassCard(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 20.dp
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(accentColor.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Movie, contentDescription = null, tint = accentColor, modifier = Modifier.size(22.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Video File Visibility & Restrictions", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 15.sp)
                        Text("Control video access & prevent auto-play", fontSize = 12.sp, color = Color(0xFF9CA3AF))
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Show Video Files toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Show Video Files", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Text("Disabled by default. Hides videos from standard music library, playlists, and scan results unless enabled.", color = Color(0xFF9CA3AF), fontSize = 11.sp)
                    }
                    Switch(
                        checked = showVideoFiles,
                        onCheckedChange = { viewModel.setShowVideoFiles(it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = accentColor),
                        modifier = Modifier.testTag("show_video_files_switch")
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Auto-play Videos toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Auto-play Videos", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Text("Disabled by default. Videos require an explicit tap to begin playback and never auto-play on car/headset connection.", color = Color(0xFF9CA3AF), fontSize = 11.sp)
                    }
                    Switch(
                        checked = autoPlayVideos,
                        onCheckedChange = { viewModel.setAutoPlayVideos(it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = accentColor),
                        modifier = Modifier.testTag("auto_play_videos_switch")
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Full-Page Search & Search History Card
        AuraGlassCard(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 20.dp
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Search & Recent History", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 15.sp)
                        Text("${searchHistory.size} saved searches • Full-library indexing", fontSize = 12.sp, color = Color(0xFF9CA3AF))
                    }
                    Switch(
                        checked = searchHistoryEnabled,
                        onCheckedChange = { viewModel.setSearchHistoryEnabled(it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = accentColor),
                        modifier = Modifier.testTag("search_history_switch")
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { viewModel.setFullScreenSearchOpen(true) },
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Open Full-Page Search", fontSize = 12.sp, color = Color.White)
                    }
                    Button(
                        onClick = { viewModel.clearSearchHistory() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0x33FFFFFF)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Clear History", fontSize = 12.sp, color = Color.White)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

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

        // Android Auto Car Integration Card
        AuraGlassCard(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 20.dp
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(accentColor.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.DirectionsCar, contentDescription = null, tint = accentColor, modifier = Modifier.size(24.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Android Auto (USB & Wireless)", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 15.sp)
                        Text("Car Dashboard Screen & Steering Wheel Controls", fontSize = 12.sp, color = Color(0xFF9CA3AF))
                    }
                    AuraBadge(text = "ENABLED", color = Color(0xFF34A853))
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "AUDI Player runs as an official MediaLibraryService for Android Auto. When plugged in via USB cable or connected wirelessly to your car, the player appears on the car screen with full touch controls (Play, Pause, Skip Next, Skip Previous, browse Songs and Playlists) and responds to car steering wheel buttons.",
                    fontSize = 12.sp,
                    color = Color(0xFFD1D5DB),
                    lineHeight = 17.sp
                )

                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0x22FFFFFF))
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Auto Mode Service State:", color = Color(0xFF9CA3AF), fontSize = 12.sp)
                    AuraBadge(text = "MediaLibrarySession Ready", color = Color(0xFF38BDF8))
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Background MediaStyle Notification & Audio Controls
        AuraGlassCard(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 20.dp
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(accentColor.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = accentColor, modifier = Modifier.size(22.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("MediaStyle Notification & Audio", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 15.sp)
                        Text("Background playback controls & volume", fontSize = 12.sp, color = Color(0xFF9CA3AF))
                    }
                    AuraBadge(text = "ACTIVE", color = Color(0xFF34A853))
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "When the app is in the background, an interactive MediaStyle notification is linked to the active MediaSession with Play/Pause, Skip Next, Skip Previous, and direct Audio Volume controls (Vol -, Vol +).",
                    fontSize = 12.sp,
                    color = Color(0xFFD1D5DB),
                    lineHeight = 17.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { viewModel.adjustVolumeRelative(-1) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0x33FFFFFF)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.VolumeDown, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Vol -", fontSize = 12.sp, color = Color.White)
                    }

                    Button(
                        onClick = { viewModel.adjustVolumeRelative(1) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0x33FFFFFF)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.VolumeUp, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Vol +", fontSize = 12.sp, color = Color.White)
                    }

                    Button(
                        onClick = { viewModel.playerController.togglePlayPause() },
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Play/Pause", fontSize = 11.sp)
                    }
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
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Letters styled with the iconic Google color palette:",
                    fontSize = 12.sp,
                    color = Color(0xFF9CA3AF)
                )
                Spacer(modifier = Modifier.height(10.dp))

                // Live Google-colored logo preview
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0x33000000))
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    GoogleColoredText(
                        text = editableName.ifBlank { "AUDI PLAYER" },
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Black
                    )
                }

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

        Spacer(modifier = Modifier.height(16.dp))

        // Headphones & Headset Multi-Tap Controls Card
        AuraGlassCard(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 20.dp
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(accentColor.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Headphones, contentDescription = null, tint = accentColor, modifier = Modifier.size(22.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Headset & Earphones Controls", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 15.sp)
                            Text("Multi-tap gestures & call management", fontSize = 12.sp, color = Color(0xFF9CA3AF))
                        }
                    }

                    Button(
                        onClick = { viewModel.setHeadsetControlsDialogOpen(true) },
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("launch_headset_hub_button")
                    ) {
                        Text("Open Hub", fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0x22FFFFFF))
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("• 1 Tap: Pause / Play (or Answer incoming call)", fontSize = 11.sp, color = Color.White)
                    Text("• 2 Taps: Next Song (or Disconnect call while playing)", fontSize = 11.sp, color = Color.White)
                    Text("• 3 Taps: Previous Song", fontSize = 11.sp, color = Color.White)
                    Text("• Unplugging Headphones: Auto-pauses music immediately", fontSize = 11.sp, color = Color(0xFF9CA3AF))
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Media Import & Scanning Policy Card
        AuraGlassCard(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 20.dp
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Storage, contentDescription = null, tint = accentColor, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Media Storage & Import Rules", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 15.sp)
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 4MB - 5MB Strict Size Filter
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Strict 4MB – 5MB File Filter", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Text("Only import audio/video files between 4MB and 5MB", color = Color(0xFF9CA3AF), fontSize = 11.sp)
                    }
                    Switch(
                        checked = strict4to5MbFilter,
                        onCheckedChange = { viewModel.setStrict4to5MbFilter(it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = accentColor)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Call recording exclusion
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Exclude Call Recordings", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Text("Never import voice notes or call recorded audio", color = Color(0xFF9CA3AF), fontSize = 11.sp)
                    }
                    AuraBadge(text = "STRICTLY BLOCKED", color = Color(0xFF34A853))
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Call & Notification Audio Management Card
        Text(
            text = "AUDIO INTERRUPTION & CALL AUTO-STOP",
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
            Column(modifier = Modifier.padding(16.dp)) {
                // Section Header
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(accentColor.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.PhoneCallback,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Smart Interruption Manager",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 15.sp
                        )
                        Text(
                            text = "Dialer/VoIP Auto-Stop & Notification Ducking",
                            fontSize = 12.sp,
                            color = Color(0xFF9CA3AF)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Feature 1: Dialer & In-App Call Auto-Stop
                Text(
                    text = "1. Phone Dialer & In-App Call Auto-Stop",
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Automatically stops playback when receiving or making a call via Phone Dialer or any in-app call (WhatsApp, Telegram, Meet, Zoom). Never mixes call audio with music.",
                    fontSize = 11.sp,
                    color = Color(0xFFD1D5DB),
                    lineHeight = 16.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Current Call State Indicator
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0x33000000))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Call Status:", color = Color(0xFF9CA3AF), fontSize = 12.sp)
                    AuraBadge(
                        text = activeCallState,
                        color = if (activeCallState == "Idle (No Call)") Color(0xFF34A853) else Color(0xFFEA4335)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Auto Resume Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Auto-Resume After Call", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Text("Automatically resume playback when the call ends", color = Color(0xFF9CA3AF), fontSize = 11.sp)
                    }
                    Switch(
                        checked = autoResumeOnCallEnd,
                        onCheckedChange = { viewModel.setAutoResumeOnCallEnd(it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = accentColor)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Dedicated Call Focus Mode (Ignore Other Apps)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Dedicated Call Auto-Stop", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Text("Only stop for phone calls & dialer. Duck for notifications. Never let other background apps stop player.", color = Color(0xFF9CA3AF), fontSize = 11.sp)
                    }
                    Switch(
                        checked = dedicatedCallInterruptionOnly,
                        onCheckedChange = { viewModel.setDedicatedCallInterruptionOnly(it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = accentColor)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Call Simulation Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { viewModel.simulateDialerCall() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Test Dialer Call", fontSize = 11.sp)
                    }

                    Button(
                        onClick = { viewModel.simulateInAppVoipCall() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.PhoneCallback, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Test In-App VoIP", fontSize = 11.sp)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Color(0x22FFFFFF))
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Feature 2: Notification Ducking & Auto-Raise
                Text(
                    text = "2. Notification Sound Ducking (Lower & Raise Volume)",
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "When any system notification or in-app message alert arrives, the player lowers sound to 20% smoothly, and automatically raises volume back to normal once the chime finishes.",
                    fontSize = 11.sp,
                    color = Color(0xFFD1D5DB),
                    lineHeight = 16.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Live Ducking Status Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0x33000000))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isDucked) Icons.Default.VolumeDown else Icons.Default.VolumeUp,
                            contentDescription = null,
                            tint = if (isDucked) Color(0xFFFBBC05) else Color(0xFF34A853),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Volume Output:", color = Color(0xFF9CA3AF), fontSize = 12.sp)
                    }
                    AuraBadge(
                        text = if (isDucked) "LOWERED • DUCKED (${(duckVolumeFactor * 100).toInt()}%)" else "NORMAL (100%)",
                        color = if (isDucked) Color(0xFFFBBC05) else Color(0xFF34A853)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Notification Simulation Button
                Button(
                    onClick = { viewModel.simulateNotificationDucking() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Test Notification Sound (Lower & Raise Volume)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }

                // Status banner if active
                if (!interruptionStatus.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0x44D97706))
                            .border(1.dp, Color(0xFFFBBF24), RoundedCornerShape(10.dp))
                            .padding(10.dp)
                    ) {
                        Text(
                            text = "⚡ $interruptionStatus",
                            color = Color(0xFFFDE68A),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
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

        // Driving Mode & In-Motion Safety Manager
        val drivingMode = viewModel.playerController.drivingModeManager
        val isVehicleMoving by drivingMode.isVehicleMoving.collectAsState()
        val currentSpeedKmh by drivingMode.currentSpeedKmh.collectAsState()
        val motionStatusDesc by drivingMode.motionStatusDescription.collectAsState()
        val isDrivingRestrictionEnabled by drivingMode.isDrivingRestrictionEnabled.collectAsState()
        val isSimulatingMotion by drivingMode.isSimulatingMotion.collectAsState()

        AuraGlassCard(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 20.dp
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.DirectionsCar,
                            contentDescription = null,
                            tint = if (isVehicleMoving) Color(0xFFFF9800) else accentColor,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                "Driving Mode & Safety Policy",
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 15.sp
                            )
                            Text(
                                if (isVehicleMoving) "Vehicle in Motion (${currentSpeedKmh.toInt()} km/h)" else "Parked / Stationary",
                                fontSize = 12.sp,
                                color = if (isVehicleMoving) Color(0xFFFFB74D) else Color(0xFF9CA3AF)
                            )
                        }
                    }
                    Switch(
                        checked = isDrivingRestrictionEnabled,
                        onCheckedChange = { drivingMode.setDrivingRestrictionEnabled(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = accentColor
                        )
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Restricts video display while vehicle is moving to ensure driver safety. Audio streaming continues uninterrupted.",
                    fontSize = 12.sp,
                    color = Color(0xFFD1D5DB)
                )

                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isVehicleMoving) Color(0x33FF9800) else Color(0x2210B981),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isVehicleMoving) Color(0x66FF9800) else Color(0x3310B981)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Status: $motionStatusDesc",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                        AuraBadge(
                            text = if (isVehicleMoving) "DRIVING ACTIVE" else "PARKED",
                            color = if (isVehicleMoving) Color(0xFFFF9800) else Color(0xFF10B981)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "Test & Simulation Modes:",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { drivingMode.simulateSpeed(0.0f) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0x33FFFFFF)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Parked (0)", fontSize = 11.sp, color = Color.White)
                    }
                    Button(
                        onClick = { drivingMode.simulateSpeed(45.0f) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0x33FF9800)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("City (45)", fontSize = 11.sp, color = Color.White)
                    }
                    Button(
                        onClick = { drivingMode.simulateSpeed(90.0f) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0x33EF4444)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Fast (90)", fontSize = 11.sp, color = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { drivingMode.simulateDrivingCycle(55.0f, 12) },
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1.5f)
                    ) {
                        Text("Simulate Driving Cycle", fontSize = 12.sp, color = Color.White)
                    }
                    if (isSimulatingMotion) {
                        Button(
                            onClick = { drivingMode.stopSimulation() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0x44EF4444)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Reset", fontSize = 12.sp, color = Color.White)
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

        Spacer(modifier = Modifier.height(16.dp))

        // Ganimine Publisher & App Info Card
        AuraGlassCard(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 20.dp
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Ganimine AUDI Player",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 15.sp
                        )
                        Text(
                            text = "Package: com.ganimine.player • Version 1.0",
                            fontSize = 12.sp,
                            color = Color(0xFF9CA3AF)
                        )
                    }
                    AuraBadge(text = "GANIMINE", color = accentColor)
                }
            }
        }
    }
}
