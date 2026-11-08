package com.example.ui.screens

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.SubtitlesOff
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.MusicViewModel
import com.example.TrackPropertiesDialog
import com.example.data.TrackEntity
import com.example.ui.components.SleepTimerDialog
import kotlinx.coroutines.isActive
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Distinctive cassette door window shape matching reference design:
 * Rounded top-left corner, horizontal top edge, 45-degree top-right chamfer,
 * straight right edge, and a continuous semicircular U-shaped bottom.
 */
val CassetteDoorShape = GenericShape { size, _ ->
    val w = size.width
    val h = size.height
    val rTop = 26f
    val chamferX = w * 0.48f
    val chamferY = w * 0.58f
    val bottomRadius = (w * 0.48f).coerceAtMost(h * 0.36f)

    moveTo(rTop, 0f)
    lineTo(chamferX, 0f)
    lineTo(w, chamferY)
    lineTo(w, h - bottomRadius)
    // Semicircular / U-shaped bottom arc
    arcTo(
        rect = Rect(0f, h - bottomRadius * 2f, w, h),
        startAngleDegrees = 0f,
        sweepAngleDegrees = 180f,
        forceMoveTo = false
    )
    lineTo(0f, rTop)
    quadraticBezierTo(0f, 0f, rTop, 0f)
    close()
}

@Composable
fun NeoRetroCassettePlayerScreen(
    viewModel: MusicViewModel,
    track: TrackEntity,
    accentColor: Color,
    surfaceColor: Color,
    onBgColor: Color,
    onShowTrackProperties: ((TrackEntity) -> Unit)? = null
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    val allTracks by viewModel.allTracks.collectAsStateWithLifecycle()
    val updatedTrack = remember(track, allTracks) {
        allTracks.find { it.id == track.id } ?: track
    }

    val isPlaying by viewModel.playbackController.isPlaying.collectAsStateWithLifecycle()
    val playbackProgress by viewModel.playbackController.playbackProgress.collectAsStateWithLifecycle()
    val currentPositionMs by viewModel.playbackController.currentPosition.collectAsStateWithLifecycle()
    val durationMs by viewModel.playbackController.duration.collectAsStateWithLifecycle()
    val isShuffle by viewModel.playbackController.isShuffle.collectAsStateWithLifecycle()
    val isRepeat by viewModel.playbackController.isRepeat.collectAsStateWithLifecycle()
    val currentQueue by viewModel.playbackController.currentPlaybackList.collectAsStateWithLifecycle()
    var isRecordMode by remember { mutableStateOf(false) }

    val sleepTimerRemaining by viewModel.playbackController.sleepTimerRemainingSeconds.collectAsStateWithLifecycle()
    var showQueue by remember { mutableStateOf(false) }
    var showLyrics by remember { mutableStateOf(false) }
    var showSleepTimerDialog by remember { mutableStateOf(false) }
    var showSoundEffectDialog by remember { mutableStateOf(false) }
    var localTrackForProperties by remember { mutableStateOf<TrackEntity?>(null) }
    var activeScrubPositionMs by remember { mutableStateOf<Int?>(null) }

    val handleShowProperties: (TrackEntity) -> Unit = { targetTrack ->
        if (onShowTrackProperties != null) {
            onShowTrackProperties(targetTrack)
        } else {
            localTrackForProperties = targetTrack
        }
    }

    // Smooth Spindle Rotation: Continuous smooth rotation that freezes in place when paused
    var spindleRotation by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            var lastTime = withFrameNanos { it }
            while (isActive && isPlaying) {
                withFrameNanos { now ->
                    val deltaSeconds = (now - lastTime) / 1_000_000_000f
                    lastTime = now
                    // Rotate at realistic tape speed (~100 degrees per second)
                    spindleRotation = (spindleRotation + deltaSeconds * 100f) % 360f
                }
            }
        }
    }

    if (showSleepTimerDialog) {
        SleepTimerDialog(
            onDismiss = { showSleepTimerDialog = false },
            onSetTimer = { viewModel.playbackController.startSleepTimer(it) },
            onSetEndOfTrack = { viewModel.playbackController.startSleepTimerEndOfTrack() },
            onCancelTimer = { viewModel.playbackController.cancelSleepTimer() },
            accentColor = Color(0xFFC7D3C0)
        )
    }

    if (localTrackForProperties != null) {
        TrackPropertiesDialog(
            track = localTrackForProperties!!,
            accentColor = Color(0xFFD4AF37),
            surfaceColor = Color(0xFF222325),
            onBgColor = Color.White,
            onDismiss = { localTrackForProperties = null }
        )
    }

    // Main Cassette Player Chassis Container
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF383A3D),
                        Color(0xFF2C2D2F),
                        Color(0xFF202123),
                        Color(0xFF17181A)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 1. Top Control Bar (Pill capsule)
            RetroTopBar(
                onCollapse = { viewModel.setPlayerExpanded(false) },
                onTimerClick = { showSleepTimerDialog = true },
                sleepTimerRemaining = sleepTimerRemaining,
                showLyrics = showLyrics,
                onToggleLyrics = {
                    showLyrics = !showLyrics
                    if (showLyrics) showQueue = false
                },
                showQueue = showQueue,
                onToggleQueue = {
                    showQueue = !showQueue
                    if (showQueue) showLyrics = false
                },
                onEqualizerClick = { showSoundEffectDialog = true },
                onOptionsClick = { handleShowProperties(updatedTrack) }
            )

            Spacer(modifier = Modifier.height(10.dp))

            // 2. Middle Body: Slate chassis with "neo PLAYER" branding + Cassette Door Window
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = Color(0xFF424346),
                shadowElevation = 6.dp,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFF65676B),
                            Color(0xFF38393C),
                            Color(0xFF262729)
                        )
                    )
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFF4B4C4F),
                                    Color(0xFF3F4043),
                                    Color(0xFF353638)
                                ),
                                radius = 700f
                            )
                        )
                ) {
                    // Subtle slate texture & vignette
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height
                        drawRect(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.20f)
                                ),
                                center = Offset(w * 0.5f, h * 0.5f),
                                radius = w * 0.9f
                            )
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        // Left vertical "neo PLAYER" branding (matching reference image)
                        NeoPlayerVerticalLogo(
                            modifier = Modifier.padding(start = 4.dp, end = 6.dp)
                        )

                        // Cassette Door Window
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .padding(end = 6.dp, top = 3.dp, bottom = 3.dp)
                                .clip(CassetteDoorShape)
                                .background(Color(0xFF141517))
                                .border(
                                    width = 3.5.dp,
                                    brush = Brush.linearGradient(
                                        listOf(
                                            Color(0xFF6E7176),
                                            Color(0xFF2B2C2F),
                                            Color(0xFF5A5C61),
                                            Color(0xFF1B1C1E)
                                        )
                                    ),
                                    shape = CassetteDoorShape
                                )
                        ) {
                            // Cassette Tape inside the door
                            val progressFraction = if (playbackProgress.duration > 0) {
                                (playbackProgress.position.toFloat() / playbackProgress.duration.toFloat()).coerceIn(0f, 1f)
                            } else 0f
                            CassetteTapeView(
                                track = updatedTrack,
                                isPlaying = isPlaying,
                                spindleRotation = spindleRotation,
                                playbackProgress = progressFraction
                            )

                            // Glass specular reflection highlight & deep inner drop shadows across door cutout
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val w = size.width
                                val h = size.height

                                // Deep top inner drop shadow
                                drawRect(
                                    brush = Brush.verticalGradient(
                                        colors = listOf(
                                            Color.Black.copy(alpha = 0.72f),
                                            Color.Black.copy(alpha = 0.35f),
                                            Color.Transparent
                                        ),
                                        startY = 0f,
                                        endY = 24.dp.toPx()
                                    ),
                                    size = Size(w, 24.dp.toPx())
                                )

                                // Deep left inner drop shadow
                                drawRect(
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(
                                            Color.Black.copy(alpha = 0.65f),
                                            Color.Black.copy(alpha = 0.28f),
                                            Color.Transparent
                                        ),
                                        startX = 0f,
                                        endX = 20.dp.toPx()
                                    ),
                                    size = Size(20.dp.toPx(), h)
                                )

                                // Diagonal glass specular highlight
                                val glarePath = Path().apply {
                                    moveTo(w * 0.22f, 0f)
                                    lineTo(w * 0.42f, 0f)
                                    lineTo(0f, h * 0.50f)
                                    lineTo(0f, h * 0.30f)
                                    close()
                                }
                                drawPath(
                                    path = glarePath,
                                    color = Color.White.copy(alpha = 0.045f)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 3. LCD Display Screen
            RetroLcdDisplay(
                track = updatedTrack,
                queueIndex = currentQueue.indexOfFirst { it.id == updatedTrack.id }.coerceAtLeast(0),
                queueSize = currentQueue.size.coerceAtLeast(1),
                isPlaying = isPlaying,
                isShuffle = isShuffle,
                isRepeat = isRepeat,
                currentPositionMs = activeScrubPositionMs ?: currentPositionMs,
                durationMs = durationMs,
                onToggleShuffle = { viewModel.playbackController.toggleShuffle() },
                onToggleRepeat = { viewModel.playbackController.toggleRepeat() },
                onScrub = { newPosMs ->
                    activeScrubPositionMs = newPosMs
                },
                onScrubFinished = { finalPosMs ->
                    viewModel.playbackController.seekTo(finalPosMs)
                    activeScrubPositionMs = null
                },
                onSoundEffectClick = { showSoundEffectDialog = true }
            )

            Spacer(modifier = Modifier.height(10.dp))

            // 4. Mechanical Tape Deck Push Buttons (Bottom)
            RetroMechanicalButtonsRow(
                isPlaying = isPlaying,
                isRecordActive = isRecordMode || isPlaying,
                onSettings = { handleShowProperties(updatedTrack) },
                onRewind = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    viewModel.playbackController.playPrevious()
                },
                onPlayPause = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    viewModel.playbackController.togglePlayPause()
                },
                onFastForward = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    viewModel.playbackController.playNext()
                },
                onRecord = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    isRecordMode = !isRecordMode
                }
            )
        }

        // Overlay: Queue Panel
        AnimatedVisibility(
            visible = showQueue,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
        ) {
            QueueOverlay(
                viewModel = viewModel,
                currentTrack = updatedTrack,
                onClose = { showQueue = false }
            )
        }

        // Overlay: Lyrics Panel
        AnimatedVisibility(
            visible = showLyrics,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
        ) {
            LyricsOverlay(
                viewModel = viewModel,
                track = updatedTrack,
                onClose = { showLyrics = false }
            )
        }

        // Overlay: Sound Effect info dialog
        if (showSoundEffectDialog) {
            SoundEffectInfoDialog(
                viewModel = viewModel,
                onDismiss = { showSoundEffectDialog = false }
            )
        }
    }
}

/**
 * Authentic "neo PLAYER" vertical branding matching the user's reference image:
 * Custom geometric retro-futuristic display typography:
 * - Uniform height and stroke thickness for all glyphs.
 * - Bold geometric 'n', 'e', 'O' at full height.
 * - 'P' with 45-degree inner chamfer.
 * - Bold 'L'.
 * - Signature 'A' formed as a rounded arch framing a floating solid circular dot.
 * - Chalice-shaped curved 'Y'.
 * - 'E' with beveled middle crossbar.
 * - 'R' matching 'P' with diagonal leg.
 * - Rotated -90 degrees, reading vertically upwards from bottom ('n') to top ('R').
 * - Precision vector Paths with crisp dark stroke and solid off-white fill.
 */
@Composable
fun NeoPlayerVerticalLogo(
    modifier: Modifier = Modifier
) {
    val glyphs = remember { createNeoPlayerLogoGlyphs() }
    val totalDesignLength = 593f
    val letterHeight = 100f

    Box(
        modifier = modifier
            .fillMaxHeight()
            .width(52.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasW = size.width
            val canvasH = size.height

            // Calculate scale to fit comfortably vertically and horizontally
            val scale = minOf(
                (canvasH * 0.94f) / totalDesignLength,
                (canvasW * 0.85f) / letterHeight
            )

            // Center and rotate -90 degrees so text runs from bottom (n) to top (R)
            // with tops of letters facing left and baseline facing right
            translate(left = canvasW / 2f, top = canvasH / 2f) {
                rotate(degrees = -90f, pivot = Offset.Zero) {
                    scale(scale = scale, pivot = Offset.Zero) {
                        translate(
                            left = -totalDesignLength / 2f,
                            top = -letterHeight / 2f
                        ) {
                            var currentX = 0f
                            val strokeColor = Color(0xFF202123)
                            val fillColor = Color(0xFFF1F3F6)
                            val strokeWidthVal = 2.4f

                            glyphs.forEachIndexed { index, glyph ->
                                translate(left = currentX, top = 0f) {
                                    // 1. Crisp dark border outline
                                    drawPath(
                                        path = glyph.path,
                                        color = strokeColor,
                                        style = Stroke(
                                            width = strokeWidthVal,
                                            cap = StrokeCap.Round,
                                            join = StrokeJoin.Round
                                        )
                                    )
                                    // 2. Off-white solid fill
                                    drawPath(
                                        path = glyph.path,
                                        color = fillColor
                                    )

                                    // Floating dot inside signature 'A'
                                    if (glyph.dotCenter != null && glyph.dotRadius > 0f) {
                                        drawCircle(
                                            color = strokeColor,
                                            radius = glyph.dotRadius,
                                            center = glyph.dotCenter,
                                            style = Stroke(width = strokeWidthVal)
                                        )
                                        drawCircle(
                                            color = fillColor,
                                            radius = glyph.dotRadius,
                                            center = glyph.dotCenter
                                        )
                                    }
                                }

                                val gap = when {
                                    index == 2 -> 24f // Gap between "neo" and "PLAYER"
                                    index < glyphs.lastIndex -> 7f
                                    else -> 0f
                                }
                                currentX += glyph.width + gap
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Data structure holding a single letter's vector path and dimensions.
 */
private data class AuthenticLogoGlyph(
    val path: Path,
    val width: Float,
    val dotCenter: Offset? = null,
    val dotRadius: Float = 0f
)

/**
 * Constructs the 9 exact mathematical vector glyphs for "neo PLAYER".
 */
private fun createNeoPlayerLogoGlyphs(): List<AuthenticLogoGlyph> {
    val h = 100f
    val s = 20f

    // 1. 'n' (w = 66f)
    val pathN = Path().apply {
        val w = 66f
        val rOuter = 33f
        val rInner = 13f
        moveTo(0f, h)
        lineTo(0f, rOuter)
        arcTo(Rect(0f, 0f, w, w), 180f, 180f, false)
        lineTo(w, h)
        lineTo(w - s, h)
        lineTo(w - s, rOuter)
        arcTo(Rect(s, rOuter - rInner, w - s, rOuter + rInner), 0f, -180f, false)
        lineTo(s, h)
        close()
    }

    // 2. 'e' (w = 66f)
    val pathLowerE = Path().apply {
        fillType = PathFillType.EvenOdd
        val w = 66f
        val rInner = 13f

        // Outer shape with lower mouth opening
        moveTo(0f, 33f)
        arcTo(Rect(0f, 0f, w, w), 180f, 180f, false)
        lineTo(w, 64f)
        lineTo(s, 64f)
        lineTo(s, 67f)
        arcTo(Rect(s, 67f - rInner, w - s, 67f + rInner), 180f, -180f, false)
        lineTo(w, 67f)
        arcTo(Rect(0f, 34f, w, 100f), 0f, 180f, false)
        lineTo(0f, 33f)
        close()

        // Upper closed loop cutout
        val topLoop = Path().apply {
            moveTo(s, 44f)
            lineTo(w - s, 44f)
            lineTo(w - s, 33f)
            arcTo(Rect(s, 33f - rInner, w - s, 33f + rInner), 0f, -180f, false)
            lineTo(s, 44f)
            close()
        }
        addPath(topLoop)
    }

    // 3. 'o' (w = 66f)
    val pathO = Path().apply {
        fillType = PathFillType.EvenOdd
        val w = 66f
        val rInner = 13f

        // Outer stadium
        moveTo(0f, 33f)
        arcTo(Rect(0f, 0f, w, w), 180f, 180f, false)
        lineTo(w, 67f)
        arcTo(Rect(0f, 34f, w, 100f), 0f, 180f, false)
        lineTo(0f, 33f)
        close()

        // Inner stadium cutout
        val inner = Path().apply {
            moveTo(s, 33f)
            arcTo(Rect(s, 33f - rInner, w - s, 33f + rInner), 180f, 180f, false)
            lineTo(w - s, 67f)
            arcTo(Rect(s, 67f - rInner, w - s, 67f + rInner), 0f, 180f, false)
            lineTo(s, 33f)
            close()
        }
        addPath(inner)
    }

    // 4. 'P' (w = 66f)
    val pathP = Path().apply {
        fillType = PathFillType.EvenOdd
        val w = 66f
        val loopH = 60f

        moveTo(0f, 0f)
        lineTo(44f, 0f)
        arcTo(Rect(22f, 0f, w, 44f), 270f, 90f, false)
        lineTo(w, 38f)
        arcTo(Rect(22f, 16f, w, loopH), 0f, 90f, false)
        lineTo(s, loopH)
        lineTo(s, h)
        lineTo(0f, h)
        close()

        // Inner counter with signature diagonal chamfer
        val inner = Path().apply {
            moveTo(s, s)
            lineTo(40f, s)
            arcTo(Rect(20f, s, w - s, s + 24f), 270f, 90f, false)
            lineTo(w - s, 32f)
            lineTo(w - s - 10f, 40f)
            lineTo(s, 40f)
            close()
        }
        addPath(inner)
    }

    // 5. 'L' (w = 62f)
    val pathL = Path().apply {
        val w = 62f
        moveTo(0f, 0f)
        lineTo(s, 0f)
        lineTo(s, h - s)
        lineTo(w, h - s)
        lineTo(w, h)
        lineTo(0f, h)
        close()
    }

    // 6. 'A' (w = 66f) with iconic center floating dot
    val pathA = Path().apply {
        val w = 66f
        val rOuter = 33f
        val rInner = 13f
        moveTo(0f, h)
        lineTo(0f, rOuter)
        arcTo(Rect(0f, 0f, w, w), 180f, 180f, false)
        lineTo(w, h)
        lineTo(w - s, h)
        lineTo(w - s, rOuter)
        arcTo(Rect(s, rOuter - rInner, w - s, rOuter + rInner), 0f, -180f, false)
        lineTo(s, h)
        close()
    }

    // 7. 'Y' (w = 66f)
    val pathY = Path().apply {
        val w = 66f
        val stemLeft = 23f
        val stemRight = 43f

        moveTo(0f, 12f)
        arcTo(Rect(0f, 0f, 24f, 24f), 180f, 90f, false)
        lineTo(s, 0f)
        quadraticBezierTo(28f, 44f, 33f, 48f)
        quadraticBezierTo(38f, 44f, w - s, 0f)
        lineTo(42f, 0f)
        arcTo(Rect(42f, 0f, w, 24f), 270f, 90f, false)
        lineTo(w, 12f)
        quadraticBezierTo(52f, 52f, stemRight, 58f)
        lineTo(stemRight, h)
        lineTo(stemLeft, h)
        lineTo(stemLeft, 58f)
        quadraticBezierTo(14f, 52f, 0f, 12f)
        close()
    }

    // 8. 'E' (w = 62f)
    val pathUpperE = Path().apply {
        val w = 62f
        moveTo(0f, 0f)
        lineTo(w, 0f)
        lineTo(w, s)
        lineTo(s, s)
        lineTo(s, 40f)
        lineTo(54f, 40f)
        lineTo(54f, 40f + s)
        lineTo(42f, 40f + s) // Diagonal cut
        lineTo(s, 40f + s)
        lineTo(s, h - s)
        lineTo(w, h - s)
        lineTo(w, h)
        lineTo(0f, h)
        close()
    }

    // 9. 'R' (w = 66f)
    val pathR = Path().apply {
        fillType = PathFillType.EvenOdd
        val w = 66f
        val loopH = 58f

        moveTo(0f, 0f)
        lineTo(44f, 0f)
        arcTo(Rect(22f, 0f, w, 44f), 270f, 90f, false)
        lineTo(w, 38f)
        arcTo(Rect(24f, 18f, w, loopH), 0f, 90f, false)
        lineTo(32f, loopH)
        // Leg
        lineTo(w, h)
        lineTo(w - s - 2f, h)
        lineTo(s + 12f, loopH + 4f)
        lineTo(s, loopH + 4f)
        lineTo(s, h)
        lineTo(0f, h)
        close()

        val inner = Path().apply {
            moveTo(s, s)
            lineTo(40f, s)
            arcTo(Rect(20f, s, w - s, s + 24f), 270f, 90f, false)
            lineTo(w - s, 32f)
            lineTo(w - s - 10f, 40f)
            lineTo(s, 40f)
            close()
        }
        addPath(inner)
    }

    return listOf(
        AuthenticLogoGlyph(pathN, 66f),
        AuthenticLogoGlyph(pathLowerE, 66f),
        AuthenticLogoGlyph(pathO, 66f),
        AuthenticLogoGlyph(pathP, 66f),
        AuthenticLogoGlyph(pathL, 62f),
        AuthenticLogoGlyph(pathA, 66f, dotCenter = Offset(33f, 34f), dotRadius = 8.5f),
        AuthenticLogoGlyph(pathY, 66f),
        AuthenticLogoGlyph(pathUpperE, 62f),
        AuthenticLogoGlyph(pathR, 66f)
    )
}

/**
 * Top control bar with metallic beveled pill container.
 */
@Composable
fun RetroTopBar(
    onCollapse: () -> Unit,
    onTimerClick: () -> Unit,
    sleepTimerRemaining: Long?,
    showLyrics: Boolean,
    onToggleLyrics: () -> Unit,
    showQueue: Boolean,
    onToggleQueue: () -> Unit,
    onEqualizerClick: () -> Unit,
    onOptionsClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF242628),
        border = androidx.compose.foundation.BorderStroke(
            1.5.dp,
            Brush.verticalGradient(
                listOf(Color(0xFF55585C), Color(0xFF1B1C1E))
            )
        ),
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onCollapse) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "Collapse Player",
                    tint = Color(0xFFC7CBD1),
                    modifier = Modifier.size(24.dp)
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (sleepTimerRemaining != null) {
                    val m = sleepTimerRemaining / 60
                    val s = sleepTimerRemaining % 60
                    Text(
                        text = String.format("%02d:%02d", m, s),
                        color = Color(0xFF9EA995),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier
                            .background(Color(0xFF1A2218), RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                IconButton(onClick = onTimerClick) {
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = "Sleep Timer",
                        tint = if (sleepTimerRemaining != null) Color(0xFF9EA995) else Color(0xFFB5B9BF),
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(onClick = onToggleLyrics) {
                    Icon(
                        imageVector = if (showLyrics) Icons.Default.SubtitlesOff else Icons.Default.Subtitles,
                        contentDescription = "Lyrics",
                        tint = if (showLyrics) Color(0xFF9EA995) else Color(0xFFB5B9BF),
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(onClick = onToggleQueue) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                        contentDescription = "Queue",
                        tint = if (showQueue) Color(0xFF9EA995) else Color(0xFFB5B9BF),
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(onClick = onEqualizerClick) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "Equalizer & Sound Effect",
                        tint = Color(0xFFB5B9BF),
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(onClick = onOptionsClick) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Track Info & Properties",
                        tint = Color(0xFFB5B9BF),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

/**
 * Authentic Vintage Cassette Tape inside the cassette door window, matching the reference design:
 * - Two-tone body: deep matte charcoal left section & curved off-white right section.
 * - Rotated vertical "STEREO" and "DIGITAIL AUDIO TAPE PLAYER CF-DT1" on the black panel.
 * - Center recessed spindle well with top & bottom rotating reels.
 * - Spindles feature outer gear teeth, middle hub pockets, and 3 iconic BRIGHT WHITE drive teeth.
 * - Center inspection window with tinted glass, magnetic tape spool, and "FLOW / CFF8 001" markings.
 * - Dynamic magnetic tape winding based on playback progress.
 */
@Composable
fun CassetteTapeView(
    track: TrackEntity,
    isPlaying: Boolean,
    spindleRotation: Float,
    playbackProgress: Float
) {
    BoxWithConstraints(
        modifier = Modifier.fillMaxSize()
    ) {
        val boxWidth = maxWidth
        val boxHeight = maxHeight

        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // 1. Base dark matte background for left section and center spindle well
            drawRect(
                color = Color(0xFF141517),
                size = Size(w, h)
            )

            // 2. Centered Spindle and Window Coordinates (tighter inner dimensions, perfectly centered)
            val spindleCenterX = w * 0.50f
            val topSpindleCenter = Offset(spindleCenterX, h * 0.25f)
            val bottomSpindleCenter = Offset(spindleCenterX, h * 0.75f)
            val spindleRadius = 24.dp.toPx()

            // Dynamic tape spool radii based on playback progress
            val progress = playbackProgress.coerceIn(0f, 1f)
            val maxSpoolExtra = 14.dp.toPx()
            val topTapeRadius = (spindleRadius + maxSpoolExtra * (1f - progress)).coerceAtLeast(spindleRadius + 2.dp.toPx())
            val bottomTapeRadius = (spindleRadius + maxSpoolExtra * progress).coerceAtLeast(spindleRadius + 2.dp.toPx())

            // Center Inspection Window (vertically and horizontally centered)
            val windowW = w * 0.28f
            val windowH = h * 0.18f
            val windowLeft = spindleCenterX - windowW / 2f
            val windowTop = h * 0.50f - windowH / 2f
            val windowBottom = windowTop + windowH

            // 3. Right off-white / light eggshell titanium shell with tight, centered contour
            val whitePath = Path().apply {
                val startX = w * 0.62f
                moveTo(startX, 0f)
                lineTo(w, 0f)
                lineTo(w, h)
                lineTo(w * 0.46f, h)
                // Smooth sweep into lower body below bottom spindle
                quadraticBezierTo(w * 0.58f, h * 0.90f, w * 0.63f, h * 0.83f)
                // Snug contour around bottom spindle
                quadraticBezierTo(w * 0.63f, h * 0.67f, w * 0.63f, windowBottom)
                // Straight vertical boundary along right side of inspection window
                lineTo(w * 0.63f, windowTop)
                // Snug contour around top spindle
                quadraticBezierTo(w * 0.63f, h * 0.33f, w * 0.63f, h * 0.17f)
                quadraticBezierTo(w * 0.63f, h * 0.08f, startX, 0f)
                close()
            }

            // Drop shadow cast by the white shell onto the dark recessed chassis bed
            drawPath(
                path = whitePath,
                color = Color.Black.copy(alpha = 0.42f),
                style = Stroke(width = 6.dp.toPx())
            )

            // Solid white shell fill with smooth warm titanium/eggshell gradient
            drawPath(
                path = whitePath,
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color(0xFFE2E5E9),
                        Color(0xFFEFF1F5),
                        Color(0xFFF7F8FA),
                        Color(0xFFE6E9ED)
                    ),
                    startX = w * 0.46f,
                    endX = w
                )
            )

            // Molded edge bevel on the white shell
            drawPath(
                path = whitePath,
                brush = Brush.verticalGradient(
                    listOf(
                        Color(0xFFA6ABB2),
                        Color(0xFF2E3033),
                        Color(0xFFB4B9C0)
                    )
                ),
                style = Stroke(width = 1.2.dp.toPx())
            )

            // 4. Connecting magnetic tape band running vertically between reels
            drawLine(
                color = Color(0xFF261812),
                start = Offset(spindleCenterX - spindleRadius * 0.70f, topSpindleCenter.y),
                end = Offset(spindleCenterX - spindleRadius * 0.70f, bottomSpindleCenter.y),
                strokeWidth = 5.dp.toPx()
            )

            // 5. Center Inspection Window
            // Dark tinted glass background
            drawRoundRect(
                color = Color(0xFF0F1012),
                topLeft = Offset(windowLeft, windowTop),
                size = Size(windowW, windowH),
                cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
            )
            // Recessed bevel border
            drawRoundRect(
                color = Color(0xFF35373B),
                topLeft = Offset(windowLeft, windowTop),
                size = Size(windowW, windowH),
                cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
                style = Stroke(width = 1.2.dp.toPx())
            )

            // Visible magnetic tape path through the inspection window (left side)
            drawRect(
                color = Color(0xFF241712),
                topLeft = Offset(windowLeft + 2.dp.toPx(), windowTop + 2.dp.toPx()),
                size = Size(windowW * 0.38f, windowH - 4.dp.toPx())
            )

            // Center window label plate backing (right side)
            drawRoundRect(
                color = Color(0xFF4E5157),
                topLeft = Offset(windowLeft + windowW * 0.40f, windowTop + 2.dp.toPx()),
                size = Size(windowW * 0.56f, windowH - 4.dp.toPx()),
                cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
            )

            // 6. Draw Top Spindle (Centered) with 3 Bright White Drive Teeth
            drawAuthenticSpindle(
                center = topSpindleCenter,
                radius = spindleRadius,
                tapeRadius = topTapeRadius,
                rotation = spindleRotation
            )

            // 7. Draw Bottom Spindle (Centered) with 3 Bright White Drive Teeth
            drawAuthenticSpindle(
                center = bottomSpindleCenter,
                radius = spindleRadius,
                tapeRadius = bottomTapeRadius,
                rotation = spindleRotation
            )
        }

        // Left Black Section: Vertical "STEREO" and "DIGITAIL AUDIO TAPE PLAYER CF-DT1" text
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(0.38f)
                .padding(start = 10.dp, end = 2.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier
                    .rotate(-90f)
                    .offset(x = (-4).dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(28.dp)
            ) {
                Text(
                    text = "STEREO",
                    color = Color(0xFFF0F3F6),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.SansSerif,
                    letterSpacing = 1.5.sp
                )
                Column {
                    Text(
                        text = "DIGITAIL AUDIO",
                        color = Color(0xFF8B8F97),
                        fontSize = 7.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "TAPE PLAYER CF-DT1",
                        color = Color(0xFF8B8F97),
                        fontSize = 7.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }

        // Center Inspection Window: "CFF0 001 / ▶FLOW" markings matching reference image (Centered)
        Box(
            modifier = Modifier
                .offset(
                    x = boxWidth * 0.55f - boxWidth / 2f,
                    y = 0.dp
                )
                .rotate(-90f),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "CFF0 001",
                    color = Color(0xFF7E828A),
                    fontSize = 6.5.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "▶FLOW",
                    color = Color(0xFF1B1C20),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.SansSerif,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}

/**
 * Draws an authentic cassette deck spindle matching the reference design:
 * - Dynamic wound magnetic tape coil behind the hub.
 * - Outer dark gear ring with 6 perimeter notch teeth.
 * - Middle dark gray hub with 3 recessed pockets.
 * - 3 BRIGHT WHITE drive teeth / spokes at 120° intervals (0°, 120°, 240°).
 * - Center axle hole with silver pin.
 */
fun DrawScope.drawAuthenticSpindle(
    center: Offset,
    radius: Float,
    tapeRadius: Float,
    rotation: Float
) {
    // 1. Magnetic tape wound on spool behind hub
    drawCircle(
        color = Color(0xFF241813),
        radius = tapeRadius,
        center = center
    )
    drawCircle(
        color = Color(0xFF140D0A),
        radius = tapeRadius,
        center = center,
        style = Stroke(1.5.dp.toPx())
    )
    // Concentric tape winding lines
    if (tapeRadius > radius + 4.dp.toPx()) {
        drawCircle(
            color = Color(0xFF38241C).copy(alpha = 0.45f),
            radius = (radius + tapeRadius) * 0.5f,
            center = center,
            style = Stroke(1.dp.toPx())
        )
    }

    // 2. Outer dark gear ring
    drawCircle(
        color = Color(0xFF2B2C2F),
        radius = radius,
        center = center
    )
    drawCircle(
        color = Color(0xFF505358),
        radius = radius,
        center = center,
        style = Stroke(1.2.dp.toPx())
    )

    // 6 Outer gear teeth notches at 60-degree intervals
    for (i in 0 until 6) {
        val angle = ((i * 60.0 + rotation) * PI / 180.0).toFloat()
        val notchLen = radius * 0.16f
        val outerX = center.x + cos(angle) * radius
        val outerY = center.y + sin(angle) * radius
        val innerX = center.x + cos(angle) * (radius - notchLen)
        val innerY = center.y + sin(angle) * (radius - notchLen)
        drawLine(
            color = Color(0xFF141517),
            start = Offset(outerX, outerY),
            end = Offset(innerX, innerY),
            strokeWidth = 2.dp.toPx(),
            cap = StrokeCap.Round
        )
    }

    // 3. Middle wheel body
    drawCircle(
        color = Color(0xFF202123),
        radius = radius * 0.74f,
        center = center
    )
    // Recessed circular groove
    drawCircle(
        color = Color(0xFF141517),
        radius = radius * 0.52f,
        center = center,
        style = Stroke(1.2.dp.toPx())
    )

    // 3 Recessed circular pockets between the spokes (at 60°, 180°, 300°)
    for (i in 0 until 3) {
        val pocketAngle = ((i * 120.0 + 60.0 + rotation) * PI / 180.0).toFloat()
        val pocketDist = radius * 0.40f
        val px = center.x + cos(pocketAngle) * pocketDist
        val py = center.y + sin(pocketAngle) * pocketDist
        drawCircle(
            color = Color(0xFF101113),
            radius = radius * 0.10f,
            center = Offset(px, py)
        )
    }

    // 4. The 3 BRIGHT WHITE Drive Teeth (Spokes) at 120-degree intervals
    for (i in 0 until 3) {
        val spokeAngle = ((i * 120.0 + rotation) * PI / 180.0).toFloat()
        val innerDist = radius * 0.22f
        val outerDist = radius * 0.65f
        val startX = center.x + cos(spokeAngle) * innerDist
        val startY = center.y + sin(spokeAngle) * innerDist
        val endX = center.x + cos(spokeAngle) * outerDist
        val endY = center.y + sin(spokeAngle) * outerDist

        // Drop shadow under white spoke for realistic 3D depth
        drawLine(
            color = Color.Black.copy(alpha = 0.75f),
            start = Offset(startX + 0.8f, startY + 0.8f),
            end = Offset(endX + 0.8f, endY + 0.8f),
            strokeWidth = 4.dp.toPx(),
            cap = StrokeCap.Round
        )

        // Solid bright white spoke body
        drawLine(
            color = Color(0xFFFFFFFF),
            start = Offset(startX, startY),
            end = Offset(endX, endY),
            strokeWidth = 3.2.dp.toPx(),
            cap = StrokeCap.Round
        )

        // Specular highlight tip
        drawCircle(
            color = Color.White,
            radius = 1.6.dp.toPx(),
            center = Offset(endX, endY)
        )
    }

    // 5. Center axle hole
    drawCircle(
        color = Color(0xFF0E0F11),
        radius = radius * 0.20f,
        center = center
    )
    drawCircle(
        color = Color(0xFF55575C),
        radius = radius * 0.20f,
        center = center,
        style = Stroke(1.dp.toPx())
    )

    // Center silver axle pin
    drawCircle(
        color = Color(0xFF9CA0A8),
        radius = radius * 0.08f,
        center = center
    )
}

/**
 * Authentic backlit vintage LCD digital matrix display.
 */
@Composable
fun RetroLcdDisplay(
    track: TrackEntity,
    queueIndex: Int,
    queueSize: Int,
    isPlaying: Boolean,
    isShuffle: Boolean,
    isRepeat: Boolean,
    currentPositionMs: Int,
    durationMs: Int,
    onToggleShuffle: () -> Unit,
    onToggleRepeat: () -> Unit,
    onScrub: (Int) -> Unit,
    onScrubFinished: (Int) -> Unit,
    onSoundEffectClick: () -> Unit
) {
    val lcdBg = Color(0xFF98A693)
    val lcdInk = Color(0xFF1A241A)
    val lcdDim = Color(0xFF1A241A).copy(alpha = 0.25f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF1B1C1E))
            .border(
                width = 3.dp,
                brush = Brush.verticalGradient(
                    listOf(Color(0xFF383A3D), Color(0xFF111213))
                ),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(4.dp)
    ) {
        // Inner LCD screen panel with realistic olive backlight
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(lcdBg)
                .border(1.dp, Color(0xFF7D8B78), RoundedCornerShape(8.dp))
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            // Top Row: PGM NO. X/Y + SOUND EFFECT badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "♬ PGM NO. ${queueIndex + 1}/$queueSize",
                        color = lcdInk,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // SOUND EFFECT Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .border(1.5.dp, lcdInk, RoundedCornerShape(4.dp))
                        .clickable { onSoundEffectClick() }
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "SOUND EFFECT",
                        color = lcdInk,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Middle Row: Repeat & Shuffle status + Track Title & Artist
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Repeat icon
                Icon(
                    imageVector = Icons.Default.Repeat,
                    contentDescription = "Repeat",
                    tint = if (isRepeat) lcdInk else lcdDim,
                    modifier = Modifier
                        .size(18.dp)
                        .clickable { onToggleRepeat() }
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Track Title and Artist centered
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = track.title,
                        color = lcdInk,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = track.artist,
                        color = lcdInk.copy(alpha = 0.85f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Shuffle icon
                Icon(
                    imageVector = Icons.Default.Shuffle,
                    contentDescription = "Shuffle",
                    tint = if (isShuffle) lcdInk else lcdDim,
                    modifier = Modifier
                        .size(18.dp)
                        .clickable { onToggleShuffle() }
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Time Row: Elapsed Time + Interactive Scrubbable LCD Progress Tracker + Remaining Time
            val elapsedSec = (currentPositionMs / 1000).coerceAtLeast(0)
            val durSec = (durationMs / 1000).coerceAtLeast(0)
            val remSec = (durSec - elapsedSec).coerceAtLeast(0)
            val elapsedStr = String.format("%02d:%02d", elapsedSec / 60, elapsedSec % 60)
            val remStr = String.format("-%02d:%02d", remSec / 60, remSec % 60)

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = elapsedStr,
                    color = lcdInk,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Interactive Progress Bar with tape head indicator
                BoxWithConstraints(
                    modifier = Modifier
                        .weight(1f)
                        .height(24.dp)
                        .pointerInput(durationMs) {
                            detectTapGestures { offset ->
                                val pct = (offset.x / size.width).coerceIn(0f, 1f)
                                onScrubFinished((pct * durationMs).toInt())
                            }
                        }
                        .pointerInput(durationMs) {
                            detectDragGestures(
                                onDragEnd = {
                                    // finished
                                },
                                onDrag = { change, _ ->
                                    change.consume()
                                    val pct = (change.position.x / size.width).coerceIn(0f, 1f)
                                    val newPos = (pct * durationMs).toInt()
                                    onScrub(newPos)
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    val progressFraction = if (durationMs > 0) {
                        (currentPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
                    } else 0f

                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val trackY = size.height * 0.5f
                        val w = size.width

                        // Base track line
                        drawLine(
                            color = lcdDim,
                            start = Offset(0f, trackY),
                            end = Offset(w, trackY),
                            strokeWidth = 2.dp.toPx()
                        )

                        // Elapsed track line
                        val playheadX = w * progressFraction
                        drawLine(
                            color = lcdInk,
                            start = Offset(0f, trackY),
                            end = Offset(playheadX, trackY),
                            strokeWidth = 3.dp.toPx()
                        )

                        // Segment ticks
                        val tickCount = 16
                        for (i in 0..tickCount) {
                            val x = w * (i.toFloat() / tickCount)
                            drawLine(
                                color = if (x <= playheadX) lcdInk else lcdDim,
                                start = Offset(x, trackY - 2.dp.toPx()),
                                end = Offset(x, trackY + 2.dp.toPx()),
                                strokeWidth = 1.dp.toPx()
                            )
                        }

                        // Tape head indicator (diamond / cursor)
                        val diamondPath = Path().apply {
                            moveTo(playheadX, trackY - 5.dp.toPx())
                            lineTo(playheadX + 4.dp.toPx(), trackY)
                            lineTo(playheadX, trackY + 5.dp.toPx())
                            lineTo(playheadX - 4.dp.toPx(), trackY)
                            close()
                        }
                        drawPath(diamondPath, color = lcdInk)
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = remStr,
                    color = lcdInk,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

/**
 * Row of 5 heavy metallic push buttons with tactile depression animation and milled grip ribs.
 */
@Composable
fun RetroMechanicalButtonsRow(
    isPlaying: Boolean,
    isRecordActive: Boolean,
    onSettings: () -> Unit,
    onRewind: () -> Unit,
    onPlayPause: () -> Unit,
    onFastForward: () -> Unit,
    onRecord: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF161718))
            .border(
                width = 2.dp,
                brush = Brush.verticalGradient(
                    listOf(Color(0xFF383A3D), Color(0xFF0F1011))
                ),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 6.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // 1. Settings / Menu Key
            TapeDeckButton(
                modifier = Modifier.weight(1f),
                icon = {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = Color(0xFF3C3E42),
                        modifier = Modifier.size(20.dp)
                    )
                },
                onClick = onSettings
            )

            // 2. Rewind Key
            TapeDeckButton(
                modifier = Modifier.weight(1f),
                icon = {
                    Icon(
                        imageVector = Icons.Default.FastRewind,
                        contentDescription = "Previous Track",
                        tint = Color(0xFF3C3E42),
                        modifier = Modifier.size(22.dp)
                    )
                },
                onClick = onRewind
            )

            // 3. Play / Pause Key (Engages down when playing)
            TapeDeckButton(
                modifier = Modifier.weight(1f),
                isEngaged = isPlaying,
                icon = {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play/Pause",
                        tint = if (isPlaying) Color(0xFF1E6C38) else Color(0xFF3C3E42),
                        modifier = Modifier.size(24.dp)
                    )
                },
                onClick = onPlayPause
            )

            // 4. Fast Forward Key
            TapeDeckButton(
                modifier = Modifier.weight(1f),
                icon = {
                    Icon(
                        imageVector = Icons.Default.FastForward,
                        contentDescription = "Next Track",
                        tint = Color(0xFF3C3E42),
                        modifier = Modifier.size(22.dp)
                    )
                },
                onClick = onFastForward
            )

            // 5. Record / Jewel Key with glowing red lens
            TapeDeckButton(
                modifier = Modifier.weight(1f),
                icon = {
                    // Glowing Ruby Jewel Lens
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .shadow(
                                elevation = if (isRecordActive) 6.dp else 2.dp,
                                shape = CircleShape,
                                ambientColor = Color(0xFFFF2222),
                                spotColor = Color(0xFFFF0000)
                            )
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    listOf(
                                        if (isRecordActive) Color(0xFFFF6666) else Color(0xFFDD3333),
                                        if (isRecordActive) Color(0xFFCC0000) else Color(0xFF880000),
                                        Color(0xFF440000)
                                    )
                                )
                            )
                            .border(
                                1.5.dp,
                                Brush.linearGradient(
                                    listOf(Color(0xFFFF9999), Color(0xFF440000))
                                ),
                                CircleShape
                            )
                    )
                },
                onClick = onRecord
            )
        }
    }
}

/**
 * Single mechanical tactile cassette deck push button:
 * Implements physical vertical displacement travel on click, metallic gradient, and milled horizontal ribs.
 */
@Composable
fun TapeDeckButton(
    modifier: Modifier = Modifier,
    isEngaged: Boolean = false,
    icon: @Composable () -> Unit,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // Tactile button depression animation
    val travelY by animateDpAsState(
        targetValue = when {
            isPressed -> 5.dp
            isEngaged -> 2.5.dp
            else -> 0.dp
        },
        animationSpec = tween(durationMillis = 80),
        label = "ButtonTravel"
    )

    val shadowElevation by animateDpAsState(
        targetValue = when {
            isPressed -> 1.dp
            isEngaged -> 2.dp
            else -> 5.dp
        },
        label = "ButtonShadow"
    )

    Box(
        modifier = modifier
            .height(58.dp)
            .offset(y = travelY)
            .shadow(
                elevation = shadowElevation,
                shape = RoundedCornerShape(4.dp),
                ambientColor = Color.Black,
                spotColor = Color.Black
            )
            .clip(RoundedCornerShape(4.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        if (isPressed) Color(0xFF9E9FA4) else Color(0xFFD6D7DC),
                        if (isPressed) Color(0xFFB0B2B8) else Color(0xFFECEEF2),
                        if (isPressed) Color(0xFF8D8E94) else Color(0xFFC0C2C8)
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = 0.6f),
                        Color(0xFF6B6D74)
                    )
                ),
                shape = RoundedCornerShape(4.dp)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Upper half: Engraved symbol
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                icon()
            }

            // Lower half: 4 milled horizontal grip ribs
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp, start = 6.dp, end = 6.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                repeat(4) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.5.dp)
                            .background(Color(0xFF82848A))
                    )
                }
            }
        }
    }
}

/**
 * Sound effect info dialog for the Neo Retro theme (unrelated to the removed Driver Mode).
 */
@Composable
fun SoundEffectInfoDialog(
    viewModel: MusicViewModel,
    onDismiss: () -> Unit
) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF222326),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF4A4D52)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "SOUND EFFECT & EQUALIZER",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFFDFE2E5)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "ReplayGain, Smart Dynamic Normalization & Peak Limiting are automatically active with bit-perfect reproduction.",
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFF9EA2A8),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                androidx.compose.material3.Button(
                    onClick = onDismiss,
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF383A3E)
                    )
                ) {
                    Text("CLOSE", fontFamily = FontFamily.Monospace, color = Color.White)
                }
            }
        }
    }
}

/**
 * Sliding Queue Overlay.
 */
@Composable
fun QueueOverlay(
    viewModel: MusicViewModel,
    currentTrack: TrackEntity,
    onClose: () -> Unit
) {
    val queue by viewModel.playbackController.currentPlaybackList.collectAsStateWithLifecycle()

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xCC000000)),
        color = Color(0xFF1E2022)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "PLAYBACK QUEUE (${queue.size})",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFFDFE2E5)
                )
                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Close Queue",
                        tint = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            androidx.compose.foundation.lazy.LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(queue.size) { index ->
                    val item = queue[index]
                    val isCurrent = item.id == currentTrack.id

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isCurrent) Color(0xFF2C322B) else Color(0xFF17181A),
                        border = if (isCurrent) androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF98A693)) else null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.playbackController.playTrack(item)
                                onClose()
                            }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${index + 1}",
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                color = if (isCurrent) Color(0xFF98A693) else Color.Gray,
                                modifier = Modifier.width(30.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.title,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = if (isCurrent) Color(0xFFDFE2E5) else Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = item.artist,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color.Gray,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Sliding Lyrics Overlay.
 */
@Composable
fun LyricsOverlay(
    viewModel: MusicViewModel,
    track: TrackEntity,
    onClose: () -> Unit
) {
    val lyricsText = track.lyrics ?: "No lyrics available for this track."

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xCC000000)),
        color = Color(0xFF1E2022)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "LYRICS",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFFDFE2E5)
                )
                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Close Lyrics",
                        tint = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                val text = lyricsText
                androidx.compose.foundation.lazy.LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    item {
                        Text(
                            text = text,
                            color = Color(0xFFDFE2E5),
                            fontSize = 16.sp,
                            fontFamily = FontFamily.Monospace,
                            lineHeight = 26.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}
