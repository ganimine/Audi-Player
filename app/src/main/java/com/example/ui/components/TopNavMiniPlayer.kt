package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.MediaItemData

/**
 * Top App Bar Header dedicated to branding, navigation, search, content filtering, and page actions.
 * The currently playing mini-player is displayed exclusively in the bottom bar (AppleMiniPlayer)
 * so there is never a duplicate player interface at the top.
 */
@Composable
fun TopNavMiniPlayer(
    appName: String,
    currentMedia: MediaItemData?,
    isPlaying: Boolean,
    accentColor: Color,
    onExpandNowPlaying: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onNext: () -> Unit,
    onOpenHeadsetControls: () -> Unit,
    onOpenFilePicker: () -> Unit,
    onOpenCarDashboard: () -> Unit = {},
    onOpenSearch: () -> Unit = {},
    onOpenFilter: () -> Unit = {},
    isFilterActive: Boolean = false,
    statusMessage: String? = null,
    onDismissStatusMessage: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color(0xE60E101A),
        tonalElevation = 6.dp,
        modifier = modifier
            .fillMaxWidth()
            .border(0.5.dp, Color(0x22FFFFFF), RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            // Notification or import status banner
            AnimatedVisibility(visible = !statusMessage.isNullOrBlank()) {
                if (!statusMessage.isNullOrBlank()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xE61F2937))
                            .border(1.dp, GoogleYellow.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = statusMessage,
                                color = Color.White,
                                fontSize = 11.sp,
                                modifier = Modifier.weight(1f),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "✕",
                                color = Color(0xFF9CA3AF),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.clickable { onDismissStatusMessage() }
                            )
                        }
                    }
                }
            }

            BoxWithConstraints(
                modifier = Modifier.fillMaxWidth()
            ) {
                val availableWidth = maxWidth
                val (responsiveName, responsiveFontSize) = remember(availableWidth, appName) {
                    when {
                        availableWidth < 340.dp -> {
                            val shortName = if (appName.equals("AUDI Player", ignoreCase = true)) "AUDI" else appName.split(" ").firstOrNull() ?: appName
                            shortName to 17.sp
                        }
                        availableWidth < 520.dp -> {
                            appName to 19.sp
                        }
                        availableWidth < 740.dp -> {
                            val proName = if (appName.contains("PRO", ignoreCase = true)) appName else "$appName PRO"
                            proName to 21.sp
                        }
                        else -> {
                            val studioName = if (appName.contains("Studio", ignoreCase = true)) appName else "$appName Pro Studio"
                            studioName to 23.sp
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left: Google-colored Branding
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clickable {
                                if (currentMedia != null) {
                                    onExpandNowPlaying()
                                }
                            }
                            .testTag("app_brand_title")
                    ) {
                        GoogleColoredText(
                            text = responsiveName,
                            fontSize = responsiveFontSize,
                            fontWeight = FontWeight.Black
                        )
                    }

                    // Right: Dedicated Navigation, Search, Filter & Page Action Shortcuts
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Search full-screen shortcut
                        IconButton(
                            onClick = onOpenSearch,
                            modifier = Modifier
                                .size(34.dp)
                                .background(Color(0x22FFFFFF), CircleShape)
                                .testTag("top_search_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Content & File-Size Filter shortcut with active indicator dot
                        Box {
                            IconButton(
                                onClick = onOpenFilter,
                                modifier = Modifier
                                    .size(34.dp)
                                    .background(
                                        if (isFilterActive) accentColor.copy(alpha = 0.25f) else Color(0x22FFFFFF),
                                        CircleShape
                                    )
                                    .testTag("top_filter_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FilterList,
                                    contentDescription = "Content & Size Filter",
                                    tint = if (isFilterActive) accentColor else Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            if (isFilterActive) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .align(Alignment.TopEnd)
                                        .clip(CircleShape)
                                        .background(accentColor)
                                )
                            }
                        }

                        // Headset multi-tap controls shortcut
                        IconButton(
                            onClick = onOpenHeadsetControls,
                            modifier = Modifier
                                .size(34.dp)
                                .background(Color(0x22FFFFFF), CircleShape)
                                .testTag("headset_controls_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Headphones,
                                contentDescription = "Headset Controls",
                                tint = GoogleBlue,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Open file shortcut
                        IconButton(
                            onClick = onOpenFilePicker,
                            modifier = Modifier
                                .size(34.dp)
                                .background(Color(0x22FFFFFF), CircleShape)
                                .testTag("top_open_file_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.FolderOpen,
                                contentDescription = "Open File",
                                tint = GoogleGreen,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Car Mode Dashboard shortcut
                        IconButton(
                            onClick = onOpenCarDashboard,
                            modifier = Modifier
                                .size(34.dp)
                                .background(Color(0x33FF9800), CircleShape)
                                .testTag("top_car_dashboard_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DirectionsCar,
                                contentDescription = "Car Player Dashboard",
                                tint = Color(0xFFFF9800),
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
