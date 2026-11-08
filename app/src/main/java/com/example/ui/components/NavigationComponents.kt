package com.example.ui.components

import com.example.ui.components.SkipNextIcon
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.MusicViewModel
import com.example.Screen
import com.example.data.TrackEntity
import kotlin.math.roundToInt
import kotlinx.coroutines.isActive

// Simple Draw icons classes for custom inline icons mapping
@Composable
fun PlayIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val path = Path().apply {
            moveTo(0f, 0f)
            lineTo(w, h / 2f)
            lineTo(0f, h)
            close()
        }
        drawPath(path, color)
    }
}

@Composable
fun PauseIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val barWidth = w * 0.3f
        drawRect(color, topLeft = Offset(0f, 0f), size = Size(barWidth, h))
        drawRect(color, topLeft = Offset(w - barWidth, 0f), size = Size(barWidth, h))
    }
}

@Composable
fun SkipNextIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val triWidth = w * 0.7f
        val path = Path().apply {
            moveTo(0f, 0f)
            lineTo(triWidth, h / 2f)
            lineTo(0f, h)
            close()
        }
        drawPath(path, color)
        drawRect(color, topLeft = Offset(w - w * 0.2f, 0f), size = Size(w * 0.2f, h))
    }
}

@Composable
fun SkipPrevIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val triWidth = w * 0.7f
        val path = Path().apply {
            moveTo(w, 0f)
            lineTo(w - triWidth, h / 2f)
            lineTo(w, h)
            close()
        }
        drawPath(path, color)
        drawRect(color, topLeft = Offset(0f, 0f), size = Size(w * 0.2f, h))
    }
}

@Composable
fun ShuffleIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = 4.5f
        
        // Crossed lines design with custom offsets
        drawLine(color, start = Offset(0f, h * 0.2f), end = Offset(w * 0.3f, h * 0.2f), strokeWidth = stroke)
        drawLine(color, start = Offset(w * 0.3f, h * 0.2f), end = Offset(w * 0.7f, h * 0.8f), strokeWidth = stroke)
        drawLine(color, start = Offset(w * 0.7f, h * 0.8f), end = Offset(w, h * 0.8f), strokeWidth = stroke)

        drawLine(color, start = Offset(0f, h * 0.8f), end = Offset(w * 0.3f, h * 0.8f), strokeWidth = stroke)
        drawLine(color, start = Offset(w * 0.3f, h * 0.8f), end = Offset(w * 0.7f, h * 0.2f), strokeWidth = stroke)
        drawLine(color, start = Offset(w * 0.7f, h * 0.2f), end = Offset(w, h * 0.2f), strokeWidth = stroke)
        
        // Arrow heads
        val arrowHeadWidth = w * 0.2f
        val pathTop = Path().apply {
            moveTo(w, h * 0.2f)
            lineTo(w - arrowHeadWidth, h * 0.2f - 7f)
            lineTo(w - arrowHeadWidth, h * 0.2f + 7f)
            close()
        }
        drawPath(pathTop, color)
        
        val pathBottom = Path().apply {
            moveTo(w, h * 0.8f)
            lineTo(w - arrowHeadWidth, h * 0.8f - 7f)
            lineTo(w - arrowHeadWidth, h * 0.8f + 7f)
            close()
        }
        drawPath(pathBottom, color)
    }
}

@Composable
fun RepeatIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = 4.5f
        val loopL = w * 0.2f
        val loopR = w * 0.8f
        val loopT = h * 0.3f
        val loopB = h * 0.7f
        
        drawLine(color, start = Offset(loopL, loopT), end = Offset(loopR, loopT), strokeWidth = stroke)
        drawLine(color, start = Offset(loopL, loopB), end = Offset(loopR, loopB), strokeWidth = stroke)
        drawLine(color, start = Offset(loopL, loopT), end = Offset(loopL, loopB), strokeWidth = stroke)
        drawLine(color, start = Offset(loopR, loopT), end = Offset(loopR, loopB), strokeWidth = stroke)
        
        // Arrowhead on loop top
        val path = Path().apply {
            moveTo(w * 0.55f, loopT)
            lineTo(w * 0.45f, loopT - 7f)
            lineTo(w * 0.45f, loopT + 7f)
            close()
        }
        drawPath(path, color = color)
    }
}

@Composable
fun ActiveBottomBar(
    track: TrackEntity,
    isPlaying: Boolean,
    progressVal: Float,
    accentColor: Color,
    surfaceColor: Color,
    onBgColor: Color,
    onSkipPrevious: () -> Unit,
    onPlayPauseToggle: () -> Unit,
    onSkipNext: () -> Unit,
    onOpenPlayer: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 8.dp, clip = false)
            .clickable { onOpenPlayer() },
        color = surfaceColor,
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(68.dp)
                    .padding(start = 16.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Mini Art
                TrackArtworkWidget(
                    artPath = track.artPath,
                    trackTitle = track.title,
                    accentColor = accentColor,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    sizeLimit = 180
                )
                Spacer(modifier = Modifier.width(12.dp))
                
                // Metadata block
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        track.title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        track.artist,
                        fontSize = 12.sp,
                        color = onBgColor.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onSkipPrevious,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            androidx.compose.material.icons.Icons.Default.SkipPrevious,
                            contentDescription = "Previous",
                            tint = onBgColor.copy(alpha = 0.8f),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    IconButton(
                        onClick = onPlayPauseToggle,
                        modifier = Modifier.size(40.dp).semantics { contentDescription = if (isPlaying) "Pause" else "Play" }
                    ) {
                        if (isPlaying) {
                            PauseIcon(color = Color.White, modifier = Modifier.size(16.dp))
                        } else {
                            PlayIcon(color = Color.White, modifier = Modifier.size(16.dp))
                        }
                    }
                    IconButton(
                        onClick = onSkipNext,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            androidx.compose.material.icons.Icons.Default.SkipNext,
                            contentDescription = "Next",
                            tint = onBgColor.copy(alpha = 0.8f),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
            
            // Linear Progress Indicator at the bottom edge (white playbar)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
                    .padding(bottom = 8.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.25f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(fraction = progressVal)
                        .background(Color.White)
                )
            }
        }
    }
}


