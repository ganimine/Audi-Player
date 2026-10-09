package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.MediaItemData
import com.example.ui.components.AuraBadge

@Composable
fun CodecInfoDialog(
    media: MediaItemData?,
    accentColor: Color,
    onDismiss: () -> Unit
) {
    if (media == null) return

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, contentDescription = null, tint = accentColor, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Media & Codec Stream Info", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(media.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(media.artist, color = Color(0xFF9CA3AF), fontSize = 12.sp)

                Divider(color = Color(0x33FFFFFF), thickness = 0.8.dp)

                InfoRow(label = "Container", value = media.container)
                InfoRow(label = "Primary Codec", value = media.codec)
                InfoRow(label = "Bitrate", value = media.bitrate)
                InfoRow(label = "File Size", value = "${media.formattedFileSize} (${media.fileSize} bytes)")
                InfoRow(label = "Audio Sample Rate", value = media.sampleRate)
                InfoRow(label = "Audio Channels", value = media.channels)
                if (media.isVideo && media.resolution != null) {
                    InfoRow(label = "Video Resolution", value = media.resolution)
                }
                InfoRow(label = "Hardware Decoding", value = "Enabled (ExoPlayer MediaCodec)")
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Close")
            }
        },
        containerColor = Color(0xFF161824)
    )
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 12.sp, color = Color(0xFF9CA3AF))
        Text(text = value, fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
    }
}
