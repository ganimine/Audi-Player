package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.headset.CallStatus
import com.example.ui.components.AuraBadge
import com.example.ui.components.GoogleBlue
import com.example.ui.components.GoogleGreen
import com.example.ui.components.GoogleRed
import com.example.ui.components.GoogleYellow
import com.example.viewmodel.PlayerViewModel

/**
 * Headset & Earphones Controls Hub:
 * Provides live monitoring and interactive testing of headset button gestures:
 * - 1-Tap: Play / Pause (or Answer Incoming Call)
 * - 2-Tap: Next Track (or Disconnect Call while playing)
 * - 3-Tap: Previous Track
 * - Incoming Call handling & auto-pause/resume fundamentals.
 */
@Composable
fun HeadsetControlsDialog(
    viewModel: PlayerViewModel,
    onDismiss: () -> Unit
) {
    val headset = viewModel.headsetController
    val callStatus by headset.callStatus.collectAsState()
    val lastGesture by headset.lastDetectedGesture.collectAsState()
    val isPlugged by headset.isHeadsetPlugged.collectAsState()
    val isPlaying by viewModel.playerController.isPlaying.collectAsState()
    val accentTheme by viewModel.accentTheme.collectAsState()
    val accentColor = Color(accentTheme.hexColor)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(24.dp))
                .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(24.dp))
                .testTag("headset_controls_dialog"),
            color = Color(0xF2111422),
            tonalElevation = 12.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header with icon and close button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(GoogleBlue.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Headphones,
                                contentDescription = null,
                                tint = GoogleBlue,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Headset & Earphones",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                            Text(
                                text = "Multi-Tap & Call Interruption Engine",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF9CA3AF)
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Live Status Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0x22FFFFFF))
                        .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(16.dp))
                        .padding(14.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "CALL STATE",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF9CA3AF)
                            )
                            val callBadgeColor = when (callStatus) {
                                CallStatus.IDLE -> GoogleGreen
                                CallStatus.RINGING -> GoogleRed
                                CallStatus.ACTIVE_CALL -> GoogleYellow
                            }
                            AuraBadge(text = callStatus.label, color = callBadgeColor)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "HARDWARE JACK",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF9CA3AF)
                            )
                            AuraBadge(
                                text = if (isPlugged) "Connected / Ready" else "Wireless / Auto-Detect",
                                color = if (isPlugged) GoogleGreen else GoogleBlue
                            )
                        }

                        // Last detected gesture readout
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0x33000000))
                                .padding(10.dp)
                        ) {
                            Text(
                                text = "Last Detected Action:",
                                fontSize = 10.sp,
                                color = Color(0xFF9CA3AF)
                            )
                            Text(
                                text = lastGesture,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Gesture Rules Card
                Text(
                    text = "HARDWARE BUTTON CONTROLS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = accentColor,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0x1AFFFFFF))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    GestureRuleRow(
                        tap = "1 TAP",
                        normalAction = "Play / Pause Audio",
                        callAction = "Answer Incoming Call 📞",
                        badgeColor = GoogleBlue
                    )
                    GestureRuleRow(
                        tap = "2 TAPS",
                        normalAction = "Next Track ⏭️",
                        callAction = "Disconnect / Reject Call 📵",
                        badgeColor = GoogleGreen
                    )
                    GestureRuleRow(
                        tap = "3 TAPS",
                        normalAction = "Previous Track ⏮️",
                        callAction = "Previous Track",
                        badgeColor = GoogleYellow
                    )
                    GestureRuleRow(
                        tap = "UNPLUG",
                        normalAction = "Auto-Pause (Becoming Noisy)",
                        callAction = "Route to Phone Receiver",
                        badgeColor = GoogleRed
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Interactive Simulator Controls
                Text(
                    text = "INTERACTIVE GESTURE SIMULATOR",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = accentColor,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Tap buttons below to test hardware headset button signals directly:",
                    fontSize = 11.sp,
                    color = Color(0xFFD1D5DB)
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { headset.onHeadsetButtonEvent() },
                        colors = ButtonDefaults.buttonColors(containerColor = GoogleBlue),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("simulate_1_tap_button")
                    ) {
                        Text("1-Tap", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            headset.onHeadsetButtonEvent()
                            headset.onHeadsetButtonEvent()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GoogleGreen),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("simulate_2_tap_button")
                    ) {
                        Text("2-Tap (Next)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            headset.onHeadsetButtonEvent()
                            headset.onHeadsetButtonEvent()
                            headset.onHeadsetButtonEvent()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GoogleYellow),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("simulate_3_tap_button")
                    ) {
                        Text("3-Tap (Prev)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Call Simulation Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (callStatus == CallStatus.IDLE) {
                        Button(
                            onClick = { headset.simulateIncomingCall(isPlaying) },
                            colors = ButtonDefaults.buttonColors(containerColor = GoogleRed),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("simulate_incoming_call_button")
                        ) {
                            Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Simulate Incoming Call", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    } else if (callStatus == CallStatus.RINGING) {
                        Button(
                            onClick = { headset.answerCall() },
                            colors = ButtonDefaults.buttonColors(containerColor = GoogleGreen),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("simulate_answer_call_button")
                        ) {
                            Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("1-Tap: Answer", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { headset.disconnectCall() },
                            colors = ButtonDefaults.buttonColors(containerColor = GoogleRed),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("simulate_reject_call_button")
                        ) {
                            Icon(Icons.Default.CallEnd, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("2-Tap: Reject", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Button(
                            onClick = { headset.disconnectCall() },
                            colors = ButtonDefaults.buttonColors(containerColor = GoogleRed),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("simulate_disconnect_call_button")
                        ) {
                            Icon(Icons.Default.CallEnd, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("2-Tap: Disconnect Call & Resume Music", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GestureRuleRow(
    tap: String,
    normalAction: String,
    callAction: String,
    badgeColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(badgeColor.copy(alpha = 0.2f))
                    .border(0.5.dp, badgeColor, RoundedCornerShape(6.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(text = tap, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = badgeColor)
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(text = normalAction, fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
        }

        Text(text = callAction, fontSize = 10.sp, color = Color(0xFFD1D5DB))
    }
}
