package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.MediaItemData
import com.example.model.VisualizerStyle

// Gradient presets for track artwork and ambient backgrounds
val AURA_GRADIENTS = listOf(
    listOf(Color(0xFFFA233B), Color(0xFFFF6A00), Color(0xFF6F00FF)), // Crimson Sunset
    listOf(Color(0xFF00E5FF), Color(0xFF0072FF), Color(0xFF7000FF)), // Cyber Cyan
    listOf(Color(0xFFFF007F), Color(0xFF7928CA), Color(0xFF2A0845)), // Electric Neon
    listOf(Color(0xFF00F260), Color(0xFF0575E6), Color(0xFF0B1B3D)), // Aurora Mint
    listOf(Color(0xFFFFB300), Color(0xFFF77737), Color(0xFFD62976)), // Sunset Amber
    listOf(Color(0xFF8E2DE2), Color(0xFF4A00E0), Color(0xFF140727))  // Royal Violet
)

@Composable
fun getGradientForIndex(index: Int): Brush {
    val colors = AURA_GRADIENTS[index % AURA_GRADIENTS.size]
    return Brush.linearGradient(
        colors = colors,
        start = Offset(0f, 0f),
        end = Offset(400f, 400f)
    )
}

/**
 * Apple Music style album artwork container
 */
@Composable
fun ArtworkCover(
    media: MediaItemData?,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 16.dp,
    showPlayingBadge: Boolean = false,
    isPlaying: Boolean = false
) {
    val gradientIndex = media?.gradientColorIndex ?: 0
    val brush = getGradientForIndex(gradientIndex)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(cornerRadius))
            .background(brush),
        contentAlignment = Alignment.Center
    ) {
        if (!media?.artworkUrl.isNullOrBlank()) {
            AsyncImage(
                model = media?.artworkUrl,
                contentDescription = media?.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // Stylized vector media glyph
            Icon(
                imageVector = if (media?.isVideo == true) Icons.Default.Movie else Icons.Default.MusicNote,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.9f),
                modifier = Modifier.size(28.dp)
            )
        }

        if (showPlayingBadge && isPlaying) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(6.dp)
                    .size(20.dp)
                    .background(Color(0xCC000000), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.GraphicEq,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}

/**
 * Apple Music Floating Mini Player
 */
@Composable
fun AppleMiniPlayer(
    currentMedia: MediaItemData,
    isPlaying: Boolean,
    progressMs: Long,
    durationMs: Long,
    accentColor: Color,
    onExpand: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    val progress = if (durationMs > 0) (progressMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .shadow(16.dp, RoundedCornerShape(20.dp), ambientColor = accentColor.copy(alpha = 0.3f))
            .clip(RoundedCornerShape(20.dp))
            .border(1.dp, Color(0x2BFFFFFF), RoundedCornerShape(20.dp))
            .clickable(onClick = onExpand)
            .testTag("mini_player"),
        color = Color(0xF0181A26),
        tonalElevation = 8.dp
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    ArtworkCover(
                        media = currentMedia,
                        modifier = Modifier.size(46.dp),
                        cornerRadius = 10.dp,
                        showPlayingBadge = false
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = currentMedia.title,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = currentMedia.artist,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFB0B3C6),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            AuraBadge(
                                text = if (currentMedia.isVideo) "VIDEO" else if (currentMedia.isLargeFile) "HI-RES" else "AUDIO",
                                color = accentColor
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onTogglePlayPause,
                        modifier = Modifier
                            .size(44.dp)
                            .testTag("mini_player_play_pause")
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    IconButton(
                        onClick = onNext,
                        modifier = Modifier
                            .size(44.dp)
                            .testTag("mini_player_next")
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = "Next Track",
                            tint = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
            }

            // Slim glowing progress indicator along bottom of mini player
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.5.dp),
                color = accentColor,
                trackColor = Color(0x33FFFFFF)
            )
        }
    }
}

/**
 * Apple Music / AUDI Style Tag Badge
 */
@Composable
fun AuraBadge(
    text: String,
    color: Color = Color(0xFFFA233B),
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color.copy(alpha = 0.18f))
            .border(0.5.dp, color.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = color,
            letterSpacing = 0.5.sp
        )
    }
}

/**
 * Glass Card Container
 */
@Composable
fun AuraGlassCard(
    modifier: Modifier = Modifier,
    backgroundColor: Color = Color(0x18FFFFFF),
    borderColor: Color = Color(0x22FFFFFF),
    cornerRadius: Dp = 20.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val clickableModifier = if (onClick != null) {
        Modifier.clickable(onClick = onClick)
    } else Modifier

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .border(1.dp, borderColor, RoundedCornerShape(cornerRadius))
            .background(backgroundColor)
            .then(clickableModifier)
    ) {
        content()
    }
}

/**
 * Dynamic Audio Visualizer View (Liquid Wave, AUDI Spectrum, Vinyl Disc)
 */
@Composable
fun AnimatedVisualizerView(
    amplitudes: List<Float>,
    style: VisualizerStyle,
    accentColor: Color,
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    when (style) {
        VisualizerStyle.WAVEFORM -> {
            Canvas(modifier = modifier) {
                val width = size.width
                val height = size.height
                val midY = height / 2f
                val count = amplitudes.size.coerceAtLeast(2)
                val step = width / (count - 1)

                val pathTop = Path()
                val pathBottom = Path()

                pathTop.moveTo(0f, midY)
                pathBottom.moveTo(0f, midY)

                amplitudes.forEachIndexed { i, amp ->
                    val x = i * step
                    val ampHeight = (amp * (height * 0.45f))
                    pathTop.lineTo(x, midY - ampHeight)
                    pathBottom.lineTo(x, midY + ampHeight)
                }

                drawPath(
                    path = pathTop,
                    brush = Brush.verticalGradient(
                        colors = listOf(accentColor, accentColor.copy(alpha = 0.3f)),
                        startY = 0f,
                        endY = midY
                    ),
                    style = Stroke(width = 3.dp.toPx())
                )
                drawPath(
                    path = pathBottom,
                    brush = Brush.verticalGradient(
                        colors = listOf(accentColor.copy(alpha = 0.3f), accentColor),
                        startY = midY,
                        endY = height
                    ),
                    style = Stroke(width = 3.dp.toPx())
                )
            }
        }
        VisualizerStyle.SPECTRUM -> {
            Canvas(modifier = modifier) {
                val totalBars = amplitudes.size
                val barWidth = (size.width / (totalBars * 1.5f))
                val spacing = barWidth * 0.5f

                amplitudes.forEachIndexed { i, amp ->
                    val x = i * (barWidth + spacing)
                    val barHeight = amp * size.height
                    val y = size.height - barHeight

                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color(0xFFFF6A00), accentColor),
                            startY = y,
                            endY = size.height
                        ),
                        topLeft = Offset(x, y),
                        size = Size(barWidth, barHeight),
                        cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                    )
                }
            }
        }
        VisualizerStyle.VINYL -> {
            val infiniteTransition = rememberInfiniteTransition(label = "vinyl")
            val angle by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = if (isPlaying) 6000 else 600000, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "vinylRotation"
            )

            Box(
                modifier = modifier,
                contentAlignment = Alignment.Center
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .rotate(angle)
                ) {
                    val radius = size.minDimension / 2f
                    val center = Offset(size.width / 2f, size.height / 2f)

                    // Vinyl Outer Rim
                    drawCircle(
                        color = Color(0xFF14151B),
                        radius = radius,
                        center = center
                    )

                    // Vinyl Grooves
                    for (r in listOf(0.85f, 0.7f, 0.55f, 0.4f)) {
                        drawCircle(
                            color = Color(0x22FFFFFF),
                            radius = radius * r,
                            center = center,
                            style = Stroke(width = 1.dp.toPx())
                        )
                    }

                    // Center label
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(accentColor, Color(0xFFFA233B)),
                            center = center,
                            radius = radius * 0.3f
                        ),
                        radius = radius * 0.3f,
                        center = center
                    )

                    // Center spindle hole
                    drawCircle(
                        color = Color(0xFF0F1117),
                        radius = radius * 0.08f,
                        center = center
                    )
                }
            }
        }
        VisualizerStyle.MINIMAL_PULSE -> {
            Canvas(modifier = modifier) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val baseRadius = size.minDimension * 0.3f
                val avgAmp = amplitudes.take(8).average().toFloat()

                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(accentColor.copy(alpha = 0.5f * avgAmp), Color.Transparent),
                        center = center,
                        radius = baseRadius * (1f + avgAmp)
                    ),
                    center = center,
                    radius = baseRadius * (1f + avgAmp)
                )
            }
        }
    }
}
