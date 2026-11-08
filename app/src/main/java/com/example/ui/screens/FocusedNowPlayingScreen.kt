@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.example.ui.screens

import com.example.R
import androidx.compose.ui.draw.alpha


import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.AlbumArtStyle
import com.example.AppThemeMode
import kotlinx.coroutines.isActive
import com.example.MusicViewModel
import com.example.PlaybackProgress
import com.example.data.TrackEntity
import com.example.ui.components.PauseIcon
import com.example.ui.components.PlayIcon
import com.example.ui.components.RepeatIcon
import com.example.ui.components.ShuffleIcon
import com.example.ui.components.SkipNextIcon
import com.example.ui.components.SleepTimerDialog
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import com.example.ui.components.rememberReorderableLazyListState
import com.example.ui.components.reorderableItem
import com.example.ui.components.reorderDragHandle
import com.example.util.LrcParser
import com.example.util.ParsedLyrics
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import android.os.SystemClock
import kotlin.math.atan2
import kotlin.math.abs
import kotlin.math.hypot
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import com.example.TrackPropertiesDialog
import com.example.ui.components.SkipPrevIcon
import com.example.ui.components.TrackArtworkWidget
import com.example.ui.components.FocusedPlayerArtworkWidget
import com.example.ui.components.coverCarouselArt
import com.example.ui.components.coverCarouselFade
import com.example.ui.components.coverCarouselParallax
import com.example.ui.components.coverCarouselPin
import com.example.util.AudioQualityUtil
import com.example.util.formatDuration

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun FocusedNowPlayingScreen(
    viewModel: MusicViewModel,
    track: TrackEntity,
    accentColor: Color,
    surfaceColor: Color,
    onBgColor: Color,
    onShowTrackProperties: ((TrackEntity) -> Unit)? = null
) {
    val context = LocalContext.current
    val appThemeMode by viewModel.appThemeMode.collectAsStateWithLifecycle()
    val allTracks by viewModel.allTracks.collectAsStateWithLifecycle()
    val updatedTrack = remember(track, allTracks) {
        allTracks.find { it.id == track.id } ?: track
    }

    if (appThemeMode == AppThemeMode.NEO_RETRO) {
        NeoRetroCassettePlayerScreen(
            viewModel = viewModel,
            track = updatedTrack,
            accentColor = accentColor,
            surfaceColor = surfaceColor,
            onBgColor = onBgColor,
            onShowTrackProperties = onShowTrackProperties
        )
        return
    }
    
    var localTrackForProperties by remember { mutableStateOf<TrackEntity?>(null) }
    val handleShowProperties: (TrackEntity) -> Unit = { targetTrack ->
        if (onShowTrackProperties != null) {
            onShowTrackProperties(targetTrack)
        } else {
            localTrackForProperties = targetTrack
        }
    }

    if (localTrackForProperties != null) {
        TrackPropertiesDialog(
            track = localTrackForProperties!!,
            accentColor = accentColor,
            surfaceColor = surfaceColor,
            onBgColor = onBgColor,
            onDismiss = { localTrackForProperties = null }
        )
    }
    
    
    val isPlaying by viewModel.playbackController.isPlaying.collectAsStateWithLifecycle()
    val playbackProgress by viewModel.playbackController.playbackProgress.collectAsStateWithLifecycle()
    val isShuffle by viewModel.playbackController.isShuffle.collectAsStateWithLifecycle()
    val isRepeat by viewModel.playbackController.isRepeat.collectAsStateWithLifecycle()

    val albumArtStyle by viewModel.albumArtStyle.collectAsStateWithLifecycle()
    val rotateCd by viewModel.rotateCd.collectAsStateWithLifecycle()

    val bgColor by viewModel.currentBgColor.collectAsStateWithLifecycle()
    val liquidPaletteEnabled by viewModel.liquidPaletteEnabled.collectAsStateWithLifecycle()
    val targetBg = if (liquidPaletteEnabled) bgColor else MaterialTheme.colorScheme.background
    val animatedBg by androidx.compose.animation.animateColorAsState(targetValue = targetBg, animationSpec = tween(1000))

    val sleepTimerRemaining by viewModel.playbackController.sleepTimerRemainingSeconds.collectAsStateWithLifecycle()
    var showQueue by remember { mutableStateOf(false) }
    var showLyrics by remember { mutableStateOf(false) }
    var showSleepTimerDialog by remember { mutableStateOf(false) }
    var activeScrubPositionMs by remember { mutableStateOf<Int?>(null) }
    
    LaunchedEffect(track.id) {
        activeScrubPositionMs = null
    }

    LaunchedEffect(track) {
        viewModel.fetchLyricsIfMissing(track)
    }
    
    if (showSleepTimerDialog) {
        SleepTimerDialog(
            onDismiss = { showSleepTimerDialog = false },
            onSetTimer = { viewModel.playbackController.startSleepTimer(it) },
            onSetEndOfTrack = { viewModel.playbackController.startSleepTimerEndOfTrack() },
            onCancelTimer = { viewModel.playbackController.cancelSleepTimer() },
            accentColor = accentColor
        )
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(animatedBg, MaterialTheme.colorScheme.background))),
        color = Color.Transparent
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header bar actions
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(horizontal = 24.dp, vertical = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.setPlayerExpanded(false) }) {
                    Icon(
                        Icons.Default.KeyboardArrowDown,
                        contentDescription = "Minimize full screen player",
                        tint = onBgColor
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (sleepTimerRemaining != null) {
                        val minutes = sleepTimerRemaining!! / 60
                        val seconds = sleepTimerRemaining!! % 60
                        Text(
                            text = String.format("%02d:%02d", minutes, seconds),
                            color = accentColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .background(accentColor.copy(alpha=0.2f), RoundedCornerShape(12.dp))
                                .padding(horizontal=8.dp, vertical=4.dp)
                                .padding(end=4.dp)
                        )
                    }
                    IconButton(onClick = { showSleepTimerDialog = true }) {
                        Icon(
                            Icons.Default.Timer,
                            contentDescription = "Sleep Timer",
                            tint = if (sleepTimerRemaining != null) accentColor else onBgColor
                        )
                    }
                    IconButton(onClick = { 
                        showLyrics = !showLyrics
                        if (showLyrics) showQueue = false
                    }) {
                        Icon(
                            if (showLyrics) Icons.Default.SubtitlesOff else Icons.Default.Subtitles,
                            contentDescription = "Toggle Lyrics",
                            tint = if (showLyrics) accentColor else onBgColor
                        )
                    }
                    IconButton(onClick = { 
                        showQueue = !showQueue 
                        if (showQueue) showLyrics = false
                    }) {
                        Icon(
                            if (showQueue) Icons.Default.Close else Icons.Default.QueueMusic,
                            contentDescription = "Toggle Queue View",
                            tint = if (showQueue) accentColor else onBgColor
                        )
                    }
                    var showShapeMenu by remember { mutableStateOf(false) }
                    Box {
                        IconButton(
                            onClick = { showShapeMenu = true },
                            modifier = Modifier.testTag("change_album_art_shape_button")
                        ) {
                            Icon(
                                Icons.Default.Layers,
                                contentDescription = "Change Album Art Shape",
                                tint = onBgColor
                            )
                        }
                        DropdownMenu(
                            expanded = showShapeMenu,
                            onDismissRequest = { showShapeMenu = false }
                        ) {
                            AlbumArtStyle.entries.forEach { style ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = style.displayName,
                                            fontWeight = if (albumArtStyle == style) FontWeight.Bold else FontWeight.Normal,
                                            color = if (albumArtStyle == style) accentColor else MaterialTheme.colorScheme.onSurface
                                        )
                                    },
                                    onClick = {
                                        viewModel.setAlbumArtStyle(style)
                                        showShapeMenu = false
                                    },
                                    trailingIcon = if (albumArtStyle == style) {
                                        { Icon(Icons.Default.Check, contentDescription = "Selected", tint = accentColor) }
                                    } else null
                                )
                            }
                        }
                    }
                    IconButton(
                        onClick = {
                            try {
                                val sessionId = viewModel.playbackController.exoPlayer?.audioSessionId ?: 0
                                val intent = android.content.Intent(android.media.audiofx.AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL).apply {
                                    putExtra(android.media.audiofx.AudioEffect.EXTRA_AUDIO_SESSION, sessionId)
                                    putExtra(android.media.audiofx.AudioEffect.EXTRA_PACKAGE_NAME, context.packageName)
                                    putExtra(android.media.audiofx.AudioEffect.EXTRA_CONTENT_TYPE, 0)
                                }
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                android.widget.Toast.makeText(context, context.getString(R.string.toast_equalizer_not_supported), android.widget.Toast.LENGTH_SHORT).show()
                            }
                        }
                    ) {
                        Icon(
                            Icons.Default.Tune,
                            contentDescription = "Tune Equalizer",
                            tint = onBgColor
                        )
                    }
                }
            }

            if (showQueue) {
                // Reorderable Queue View & Recently Played View
                val queueTracks by viewModel.playbackController.currentPlaybackList.collectAsStateWithLifecycle()
                val recentlyPlayedTracks by viewModel.recentlyPlayed.collectAsStateWithLifecycle()
                var queueTabMode by remember { mutableStateOf(0) } // 0 = Upcoming Queue, 1 = Recently Played

                val queueListState = rememberLazyListState()
                val recentListState = rememberLazyListState()
                val reorderState = rememberReorderableLazyListState(
                    lazyListState = queueListState,
                    onMove = { fromIndex, toIndex ->
                        viewModel.playbackController.moveQueueItem(fromIndex, toIndex)
                    }
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(vertical = 12.dp)
                ) {
                    // Header area above the song list: scrolling/swiping left switches to Recently Played, right to Queue
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .pointerInput(Unit) {
                                detectHorizontalDragGestures { change, dragAmount ->
                                    change.consume()
                                    if (dragAmount < -12f) {
                                        // Scrolled/swiped left above song list -> switch to Recently Played
                                        queueTabMode = 1
                                    } else if (dragAmount > 12f) {
                                        // Scrolled/swiped right above song list -> switch to Upcoming Queue
                                        queueTabMode = 0
                                    }
                                }
                            }
                            .padding(bottom = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (queueTabMode == 0) androidx.compose.ui.graphics.Color.White else androidx.compose.ui.graphics.Color.White.copy(alpha = 0.12f),
                                    modifier = Modifier.clickable { queueTabMode = 0 }
                                ) {
                                    Text(
                                        "UPCOMING QUEUE",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        color = if (queueTabMode == 0) androidx.compose.ui.graphics.Color.Black else androidx.compose.ui.graphics.Color.White,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (queueTabMode == 1) androidx.compose.ui.graphics.Color.White else androidx.compose.ui.graphics.Color.White.copy(alpha = 0.12f),
                                    modifier = Modifier.clickable { queueTabMode = 1 }
                                ) {
                                    Text(
                                        "RECENTLY PLAYED",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        color = if (queueTabMode == 1) androidx.compose.ui.graphics.Color.Black else androidx.compose.ui.graphics.Color.White,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }

                            if (queueTabMode == 1 && recentlyPlayedTracks.isNotEmpty()) {
                                TextButton(
                                    onClick = {
                                        viewModel.playbackController.addToQueue(recentlyPlayedTracks)
                                    },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Icon(
                                        Icons.Default.PlaylistAdd,
                                        contentDescription = "Add all to queue",
                                        tint = androidx.compose.ui.graphics.Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        "ADD ALL",
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        color = androidx.compose.ui.graphics.Color.White
                                    )
                                }
                            }
                        }

                        Text(
                            text = if (queueTabMode == 0) {
                                if (queueTracks.size == 1) "1 track" else "${queueTracks.size} tracks"
                            } else {
                                if (recentlyPlayedTracks.size == 1) "1 track" else "${recentlyPlayedTracks.size} tracks"
                            },
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    if (queueTabMode == 0) {
                        val currentTrackIndex = remember(queueTracks, updatedTrack) {
                            queueTracks.indexOfFirst { it.id == updatedTrack.id }
                        }

                        LaunchedEffect(currentTrackIndex) {
                            if (currentTrackIndex >= 0 && !queueListState.isScrollInProgress) {
                                queueListState.animateScrollToItem((currentTrackIndex - 1).coerceAtLeast(0))
                            }
                        }

                        if (queueTracks.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(
                                    "Queue is empty",
                                    color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.7f),
                                    fontSize = 14.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        } else {
                            LazyColumn(
                                state = queueListState,
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                itemsIndexed(queueTracks, key = { _, trackItem -> trackItem.id }) { index, qTrack ->
                                    val isCurrentlyPlaying = index == currentTrackIndex
                                    val isPast = currentTrackIndex != -1 && index < currentTrackIndex
                                    val itemAlpha = if (isPast) 0.4f else 1.0f

                                    val dismissState = androidx.compose.material3.rememberSwipeToDismissBoxState(
                                        confirmValueChange = { value ->
                                            if (value == androidx.compose.material3.SwipeToDismissBoxValue.EndToStart) {
                                                viewModel.playbackController.removeTrackFromQueue(qTrack.id)
                                                true
                                            } else {
                                                false
                                            }
                                        }
                                    )

                                    androidx.compose.material3.SwipeToDismissBox(
                                        state = dismissState,
                                        modifier = Modifier.reorderableItem(state = reorderState, key = qTrack.id),
                                        enableDismissFromStartToEnd = false,
                                        backgroundContent = {
                                            val color = androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.5f)
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(color)
                                            ) {
                                            }
                                        },
                                        content = {
                                            Surface(
                                                shape = RoundedCornerShape(12.dp),
                                                color = androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.5f),
                                                border = if (isCurrentlyPlaying) androidx.compose.foundation.BorderStroke(1.dp, androidx.compose.ui.graphics.Color.White.copy(alpha = 0.5f)) else null,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .alpha(itemAlpha)
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clickable { viewModel.playbackController.playTrack(qTrack) }
                                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    // Track Number / Playing indicator
                                                    Box(
                                                        modifier = Modifier
                                                            .size(32.dp)
                                                            .clip(CircleShape)
                                                            .background(if (isCurrentlyPlaying) androidx.compose.ui.graphics.Color.White else androidx.compose.ui.graphics.Color.White.copy(alpha = 0.1f)),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        if (isCurrentlyPlaying) {
                                                            Icon(
                                                                Icons.Default.GraphicEq,
                                                                contentDescription = "Playing",
                                                                tint = androidx.compose.ui.graphics.Color.Black,
                                                                modifier = Modifier.size(18.dp)
                                                            )
                                                        } else {
                                                            Text(
                                                                "${index + 1}",
                                                                fontSize = 12.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                fontFamily = FontFamily.Monospace,
                                                                color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.7f)
                                                            )
                                                        }
                                                    }
                                                    Spacer(modifier = Modifier.width(12.dp))
                                                    // Title and Artist
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                            qTrack.title,
                                                            fontSize = 14.sp,
                                                            fontWeight = if (isCurrentlyPlaying) FontWeight.Bold else FontWeight.Medium,
                                                            fontFamily = FontFamily.Monospace,
                                                            color = androidx.compose.ui.graphics.Color.White,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                        Text(
                                                            qTrack.artist,
                                                            fontSize = 11.sp,
                                                            fontFamily = FontFamily.Monospace,
                                                            color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.7f),
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                    }
                                                    // Drag Reorder Handle
                                                    IconButton(
                                                        onClick = {},
                                                        modifier = Modifier
                                                            .size(36.dp)
                                                            .reorderDragHandle(state = reorderState, index = index, key = qTrack.id)
                                                    ) {
                                                        Icon(
                                                            Icons.Default.DragHandle,
                                                            contentDescription = "Drag to reorder",
                                                            tint = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.7f),
                                                            modifier = Modifier.size(20.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    } else {
                        // Recently Played Songs List in the exact same style as the queue list
                        if (recentlyPlayedTracks.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(
                                    "No recently played tracks",
                                    color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.7f),
                                    fontSize = 14.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        } else {
                            LazyColumn(
                                state = recentListState,
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                itemsIndexed(recentlyPlayedTracks, key = { index, rTrack -> "recent_${rTrack.id}_$index" }) { index, rTrack ->
                                    val isCurrentlyPlaying = rTrack.id == updatedTrack.id

                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.5f),
                                        border = if (isCurrentlyPlaying) androidx.compose.foundation.BorderStroke(1.dp, androidx.compose.ui.graphics.Color.White.copy(alpha = 0.5f)) else null,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { viewModel.playbackController.playTrack(rTrack) }
                                                .padding(horizontal = 12.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(32.dp)
                                                    .clip(CircleShape)
                                                    .background(if (isCurrentlyPlaying) androidx.compose.ui.graphics.Color.White else androidx.compose.ui.graphics.Color.White.copy(alpha = 0.1f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (isCurrentlyPlaying) {
                                                    Icon(
                                                        Icons.Default.GraphicEq,
                                                        contentDescription = "Playing",
                                                        tint = androidx.compose.ui.graphics.Color.Black,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                } else {
                                                    Text(
                                                        "${index + 1}",
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        fontFamily = FontFamily.Monospace,
                                                        color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.7f)
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    rTrack.title,
                                                    fontSize = 14.sp,
                                                    fontWeight = if (isCurrentlyPlaying) FontWeight.Bold else FontWeight.Medium,
                                                    fontFamily = FontFamily.Monospace,
                                                    color = androidx.compose.ui.graphics.Color.White,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    rTrack.artist,
                                                    fontSize = 11.sp,
                                                    fontFamily = FontFamily.Monospace,
                                                    color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.7f),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            // Add to Queue button
                                            IconButton(
                                                onClick = { viewModel.playbackController.addToQueue(rTrack) },
                                                modifier = Modifier.size(36.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.PlaylistAdd,
                                                    contentDescription = "Add to queue",
                                                    tint = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.85f),
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else if (showLyrics) {
                // Synchronized Lyrics View (.lrc supported)
                val parsedLyrics = remember(updatedTrack.lyrics) {
                    LrcParser.parse(updatedTrack.lyrics)
                }
                val lyricsListState = rememberLazyListState()
                val currentPositionMs = playbackProgress.position.toLong()
                val activeLineIndex = remember(currentPositionMs, parsedLyrics) {
                    if (parsedLyrics.isSynchronized) {
                        LrcParser.findActiveLineIndex(parsedLyrics.lines, currentPositionMs)
                    } else {
                        -1
                    }
                }

                // Smoothly auto-scroll to keep active line centered
                LaunchedEffect(activeLineIndex) {
                    if (activeLineIndex >= 0) {
                        val targetScrollIndex = (activeLineIndex - 2).coerceAtLeast(0)
                        if (!lyricsListState.isScrollInProgress) {
                            lyricsListState.animateScrollToItem(targetScrollIndex)
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "LYRICS",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = accentColor
                            )
                            Text(
                                if (parsedLyrics.isSynchronized) "Synchronized (.lrc) • Tap line to seek" else "Plain Text Lyrics",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = onBgColor.copy(alpha = 0.5f)
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.Black.copy(alpha = 0.3f))
                            .padding(horizontal = 16.dp, vertical = 16.dp)
                    ) {
                        if (parsedLyrics.lines.isNotEmpty()) {
                            if (parsedLyrics.isSynchronized) {
                                LazyColumn(
                                    state = lyricsListState,
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.spacedBy(14.dp),
                                    contentPadding = PaddingValues(top = 40.dp, bottom = 120.dp)
                                ) {
                                    itemsIndexed(parsedLyrics.lines) { idx, line ->
                                        val isActiveLine = idx == activeLineIndex
                                        val minutes = (line.timeMs / 1000) / 60
                                        val seconds = (line.timeMs / 1000) % 60
                                        val timeLabel = String.format("%02d:%02d", minutes, seconds)

                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(if (isActiveLine) accentColor.copy(alpha = 0.18f) else Color.Transparent)
                                                .clickable {
                                                    viewModel.playbackController.seekTo(line.timeMs.toInt())
                                                }
                                                .padding(horizontal = 12.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = timeLabel,
                                                fontSize = 11.sp,
                                                fontFamily = FontFamily.Monospace,
                                                color = if (isActiveLine) accentColor else onBgColor.copy(alpha = 0.35f),
                                                modifier = Modifier.width(44.dp)
                                            )
                                            Text(
                                                text = line.text,
                                                color = if (isActiveLine) accentColor else onBgColor.copy(alpha = 0.55f),
                                                fontSize = if (isActiveLine) 19.sp else 15.sp,
                                                fontWeight = if (isActiveLine) FontWeight.Bold else FontWeight.Normal,
                                                lineHeight = if (isActiveLine) 28.sp else 22.sp,
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    }
                                }
                            } else {
                                val scrollState = rememberScrollState()
                                Text(
                                    text = updatedTrack.lyrics ?: "",
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    lineHeight = 28.sp,
                                    modifier = Modifier
                                        .verticalScroll(scrollState)
                                        .fillMaxSize()
                                        .padding(8.dp)
                                )
                            }
                        } else {
                            Column(
                                modifier = Modifier.align(Alignment.Center),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Default.Subtitles,
                                    contentDescription = null,
                                    tint = onBgColor.copy(alpha = 0.3f),
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "No lyrics found for this track",
                                    color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.7f),
                                    fontSize = 15.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    // Neo reads lyrics embedded in the file's tags only; sidecar .lrc files are
                                    // not supported (the old hint suggested placing one next to the song).
                                    "Embed lyrics (plain or synced LRC) in the song's tags to show them here",
                                    color = onBgColor.copy(alpha = 0.35f),
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                }
            } else {
                val currentPlaylist by viewModel.playbackController.currentPlaybackList.collectAsStateWithLifecycle()

                val currentIndex = remember(track, currentPlaylist) {
                    currentPlaylist.indexOfFirst { it.id == updatedTrack.id }.coerceAtLeast(0)
                }

                val pagerState = androidx.compose.foundation.pager.rememberPagerState(
                    initialPage = currentIndex,
                    pageCount = { currentPlaylist.size }
                )

                LaunchedEffect(track, currentPlaylist) {
                    val newIndex = currentPlaylist.indexOfFirst { it.id == updatedTrack.id }
                    if (newIndex != -1 && pagerState.currentPage != newIndex) {
                        if (kotlin.math.abs(pagerState.currentPage - newIndex) == 1) {
                            pagerState.animateScrollToPage(newIndex)
                        } else {
                            pagerState.scrollToPage(newIndex)
                        }
                    }
                }

                // Only a swipe the *user* made may change the song. Programmatic scrolls (track changed from
                // the notification, auto-advance, queue edits) must never feed back into playback.
                var userSwiped by remember { mutableStateOf(false) }
                val latestPlaylist by rememberUpdatedState(currentPlaylist)
                val latestTrackId by rememberUpdatedState(updatedTrack.id)
                LaunchedEffect(pagerState) {
                    pagerState.interactionSource.interactions.collect { interaction ->
                        if (interaction is androidx.compose.foundation.interaction.DragInteraction.Start) userSwiped = true
                    }
                }
                LaunchedEffect(pagerState) {
                    snapshotFlow { pagerState.isScrollInProgress }.collect { scrolling ->
                        if (!scrolling && userSwiped) {
                            userSwiped = false
                            val selected = latestPlaylist.getOrNull(pagerState.currentPage)
                            if (selected != null && selected.id != latestTrackId) {
                                viewModel.playbackController.playTrack(selected)
                            }
                        }
                    }
                }

                val pageSpacing = 16.dp
                val density = androidx.compose.ui.platform.LocalDensity.current
                val pageSpacingPx = with(density) { pageSpacing.toPx() }

                androidx.compose.foundation.pager.HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp),
                    beyondViewportPageCount = 1,
                    pageSpacing = pageSpacing,
                    verticalAlignment = Alignment.CenterVertically
                ) { page ->
                    val pageTrack = currentPlaylist.getOrNull(page)
                    if (pageTrack != null) {
                        val isCurrentPage = page == currentIndex
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .coverCarouselPin(pagerState, page, pageSpacingPx)
                        ) {
                            TrackArtworkAndInfoWrapper(
                                track = pageTrack,
                                isPlaying = isPlaying && isCurrentPage,
                                albumArtStyle = albumArtStyle,
                                rotateCd = rotateCd,
                                accentColor = accentColor,
                                onBgColor = onBgColor,
                                onLongClick = { handleShowProperties(pageTrack) },
                                // Neighbour pages don't need the 4x/second position ticks; feeding them
                                // the live position recomposed every visible page mid-swipe (jank).
                                currentPositionMs = if (isCurrentPage) playbackProgress.position else 0,
                                durationMs = if (isCurrentPage) playbackProgress.duration else 0,
                                onSeekTo = { viewModel.playbackController.seekTo(it) },
                                onScrubPositionUpdate = { activeScrubPositionMs = it },
                                carouselPagerState = pagerState,
                                carouselPage = page,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Timers & seeks info
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
                

                // Audio Quality Info above the playback progress bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val context = LocalContext.current
                    var qualityText by remember(track) { androidx.compose.runtime.mutableStateOf("Loading...") }
                    androidx.compose.runtime.LaunchedEffect(track) {
                        qualityText = AudioQualityUtil.getQualityString(context, track)
                    }
                    val indicatorColor = remember(track.filePath, qualityText) {
                        val path = track.filePath ?: ""
                        val ext = if (path.isNotEmpty()) {
                            try {
                                java.io.File(path).extension.lowercase(java.util.Locale.ROOT)
                            } catch (e: Exception) {
                                ""
                            }
                        } else {
                            ""
                        }
                        val isFlac = ext == "flac" || qualityText.contains("FLAC", ignoreCase = true)
                        val is24BitOrHigher = qualityText.contains("24-BIT", ignoreCase = true) ||
                                              qualityText.contains("24 BIT", ignoreCase = true) ||
                                              qualityText.contains("24BIT", ignoreCase = true) ||
                                              qualityText.contains("24-bit", ignoreCase = true) ||
                                              qualityText.contains("24 bit", ignoreCase = true) ||
                                              qualityText.contains("32-BIT", ignoreCase = true) ||
                                              qualityText.contains("32 BIT", ignoreCase = true) ||
                                              path.contains("24bit", ignoreCase = true) ||
                                              path.contains("24-bit", ignoreCase = true) ||
                                              path.contains("24_bit", ignoreCase = true)

                        when {
                            isFlac && is24BitOrHigher -> Color(0xFFB388FF) // Purple for FLAC 24-Bit
                            ext == "mp3" -> Color(0xFF39FF14) // Neon Green
                            ext == "flac" -> Color(0xFF00E5FF) // Neon Blue (Standard 16-bit FLAC)
                            ext == "wav" -> Color(0xFFFFD600) // Vibrant Gold
                            else -> Color(0xFF8D6E63) // Brown
                        }
                    }
                    Surface(
                        color = indicatorColor.copy(alpha = 0.12f), // Soft background glow matching indicator
                        shape = RoundedCornerShape(6.dp),
                        border = androidx.compose.foundation.BorderStroke(0.8.dp, indicatorColor.copy(alpha = 0.45f))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            // High resolution / quality dot indicator
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(indicatorColor, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = qualityText,
                                color = indicatorColor,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                PlaybackProgressSlider(
                    viewModel = viewModel,
                    accentColor = accentColor,
                    onBgColor = onBgColor,
                    scrubPositionMs = activeScrubPositionMs
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Playback Actions Controllers grid layout
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(bottom = 24.dp, start = 24.dp, end = 24.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Repeat Icon (Left Side)
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clickable { viewModel.playbackController.toggleRepeat() }
                        .semantics { contentDescription = if (isRepeat) "Repeat on" else "Repeat off" },
                    contentAlignment = Alignment.Center
                ) {
                    val repeatColor = if (isRepeat) accentColor else Color.White.copy(alpha = 0.5f)
                    RepeatIcon(color = repeatColor, modifier = Modifier.size(18.dp))
                }

                // Previous Icon
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clickable { viewModel.playbackController.playPrevious() }
                        .semantics { contentDescription = "Previous" },
                    contentAlignment = Alignment.Center
                ) {
                    SkipPrevIcon(color = Color.White, modifier = Modifier.size(26.dp))
                }

                // Center Main Play/Pause Sphere
                Surface(
                    modifier = Modifier
                        .size(72.dp)
                        .clickable { viewModel.playbackController.togglePlayPause() }
                        .semantics { contentDescription = if (isPlaying) "Pause" else "Play" }
                        .shadow(8.dp, CircleShape),
                    color = Color.White,
                    shape = CircleShape
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isPlaying) {
                            PauseIcon(color = Color(0xFF1E1E1E), modifier = Modifier.size(24.dp))
                        } else {
                            PlayIcon(color = Color(0xFF1E1E1E), modifier = Modifier.size(24.dp).offset(x = 1.5.dp))
                        }
                    }
                }

                // Next Icon
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clickable { viewModel.playbackController.playNext() }
                        .semantics { contentDescription = "Next" },
                    contentAlignment = Alignment.Center
                ) {
                    SkipNextIcon(color = Color.White, modifier = Modifier.size(26.dp))
                }

                // Shuffle Icon (Right Side)
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clickable { viewModel.playbackController.toggleShuffle() }
                        .semantics { contentDescription = if (isShuffle) "Shuffle on" else "Shuffle off" },
                    contentAlignment = Alignment.Center
                ) {
                    val shuffleColor = if (isShuffle) accentColor else Color.White.copy(alpha = 0.5f)
                    ShuffleIcon(color = shuffleColor, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TrackArtworkAndInfoWrapper(
    track: TrackEntity,
    isPlaying: Boolean,
    albumArtStyle: AlbumArtStyle,
    rotateCd: Boolean,
    accentColor: Color,
    onBgColor: Color,
    onLongClick: (TrackEntity) -> Unit = {},
    currentPositionMs: Int = 0,
    durationMs: Int = 0,
    onSeekTo: (Int) -> Unit = {},
    onScrubPositionUpdate: (Int?) -> Unit = {},
    // Set when shown inside the cover pager: drives the masked-carousel swipe transition.
    carouselPagerState: androidx.compose.foundation.pager.PagerState? = null,
    carouselPage: Int = 0,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val density = androidx.compose.ui.platform.LocalDensity.current
    val coroutineScope = rememberCoroutineScope()
    var isUserRotatingCd by remember { mutableStateOf(false) }
    // The CD gesture below is keyed only on track.id, so it must read these through updated state;
    // otherwise it captures the position from when the track started and scrubbing jumps back to ~0:00.
    val latestPositionMs by rememberUpdatedState(currentPositionMs)
    val latestDurationMs by rememberUpdatedState(durationMs)
    val latestOnSeekTo by rememberUpdatedState(onSeekTo)
    val latestOnScrubPositionUpdate by rememberUpdatedState(onScrubPositionUpdate)
    val latestOnLongClick by rememberUpdatedState(onLongClick)

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Big centered cover album artwork disc/square
        val horizontalPadding = if (albumArtStyle == AlbumArtStyle.CD || albumArtStyle == AlbumArtStyle.SQUARE || albumArtStyle == AlbumArtStyle.ROUNDED) 0.dp else 32.dp
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = horizontalPadding, vertical = 32.dp),
            contentAlignment = Alignment.Center
        ) {
            // Background radial/pulsing shadow ring
            val scalePulse by animateFloatAsState(
                targetValue = if (isPlaying) 1.05f else 1.0f,
                animationSpec = spring(dampingRatio = 0.6f),
                label = "pulse"
            )

            var currentRotation by remember { mutableFloatStateOf(0f) }
            LaunchedEffect(isPlaying, rotateCd, isUserRotatingCd) {
                if (isPlaying && rotateCd && !isUserRotatingCd) {
                    var lastTime = kotlinx.coroutines.android.awaitFrame()
                    while (isActive) {
                        val time = kotlinx.coroutines.android.awaitFrame()
                        val delta = (time - lastTime) / 1_000_000_000f // seconds
                        currentRotation = (currentRotation + (360f / 10f) * delta) % 360f
                        lastTime = time
                    }
                }
            }

            BoxWithConstraints(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                val maxW = maxWidth
                val maxH = maxHeight
                val minDim = if (maxW < maxH) maxW else maxH
                val artSize = remember(minDim, albumArtStyle) {
                    if (albumArtStyle == AlbumArtStyle.SQUARE || albumArtStyle == AlbumArtStyle.ROUNDED) {
                        // Square and Rounded styles use Auxio spacing logic: 16dp horizontal margins
                        (minDim - 32.dp).coerceAtLeast(180.dp)
                    } else {
                        (minDim - 32.dp).coerceAtMost(320.dp).coerceAtLeast(120.dp)
                    }
                }

                if (albumArtStyle == AlbumArtStyle.CD) {
                    // Maximize CD size so the edge is exactly 4 dp from screen edge (with 4 dp HorizontalPager contentPadding), minus 12dp for 6dp radius reduction
                    val cdArtSize = minDim - 12.dp
                    Box(
                        modifier = Modifier
                            .size(cdArtSize)
                            .scale(scalePulse)
                            .then(
                                if (carouselPagerState != null) {
                                    Modifier.coverCarouselArt(
                                        carouselPagerState, carouselPage, CircleShape,
                                        windowCornerPx = with(density) { 28.dp.toPx() },
                                        shadowElevationPx = with(density) { 24.dp.toPx() }
                                    )
                                } else {
                                    Modifier.shadow(24.dp, CircleShape).clip(CircleShape)
                                }
                            )
                            .pointerInput(track.id) {
                                awaitEachGesture {
                                    val down = awaitFirstDown(requireUnconsumed = false)
                                    val center = Offset(size.width / 2f, size.height / 2f)
                                    val radius = size.width / 2f
                                    val downDist = kotlin.math.hypot(down.position.x - center.x, down.position.y - center.y)

                                    // Only interact within the circular CD border
                                    if (downDist > radius) {
                                        return@awaitEachGesture
                                    }

                                    var isRotating = false
                                    var accumulatedAngle = 0f
                                    var lastAngle = Math.toDegrees(
                                        atan2((down.position.y - center.y).toDouble(), (down.position.x - center.x).toDouble())
                                    ).toFloat()
                                    var lastTime = SystemClock.uptimeMillis()
                                    var lastSeekTime = 0L
                                    var localScrubPos = latestPositionMs

                                    // Long press detection job
                                    val longPressJob = coroutineScope.launch {
                                        delay(500)
                                        if (!isRotating) {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            latestOnLongClick(track)
                                        }
                                    }

                                    try {
                                        while (true) {
                                            val event = awaitPointerEvent()
                                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                            if (!change.pressed) {
                                                break
                                            }

                                            val currentPos = change.position
                                            val dxFromDown = currentPos.x - down.position.x
                                            val dyFromDown = currentPos.y - down.position.y

                                            // If the user does a prominent straight horizontal swipe across before rotating,
                                            // treat it as a pager swipe (next/prev track) and let it pass through without consuming
                                            if (!isRotating && abs(dxFromDown) > 35.dp.toPx() && abs(dyFromDown) < 18.dp.toPx()) {
                                                longPressJob.cancel()
                                                break
                                            }

                                            val currentAngle = Math.toDegrees(
                                                atan2((currentPos.y - center.y).toDouble(), (currentPos.x - center.x).toDouble())
                                            ).toFloat()
                                            var delta = currentAngle - lastAngle
                                            while (delta > 180f) delta -= 360f
                                            while (delta < -180f) delta += 360f

                                            val now = SystemClock.uptimeMillis()
                                            val dt = (now - lastTime).coerceAtLeast(1L)

                                            if (!isRotating) {
                                                accumulatedAngle += delta
                                                if (abs(accumulatedAngle) >= 6f) {
                                                    isRotating = true
                                                    isUserRotatingCd = true
                                                    longPressJob.cancel()
                                                    localScrubPos = latestPositionMs
                                                    latestOnScrubPositionUpdate(localScrubPos)
                                                    change.consume()
                                                }
                                            }

                                            if (isRotating) {
                                                change.consume()
                                                longPressJob.cancel()

                                                // Rotate the CD in the same direction
                                                currentRotation = (currentRotation + delta) % 360f

                                                // Velocity scaling: faster rotational scrolling rewinds / fast-forwards faster
                                                val safeDt = dt.coerceAtLeast(8L)
                                                val angularSpeed = abs(delta) / (safeDt / 1000f) // degrees / second
                                                val speedMultiplier = 1f + (angularSpeed / 120f).coerceIn(0f, 4f)
                                                val seekDeltaMs = (delta * 40f * speedMultiplier).toInt()
                                                localScrubPos = (localScrubPos + seekDeltaMs).coerceIn(0, latestDurationMs.coerceAtLeast(1))
                                                latestOnScrubPositionUpdate(localScrubPos)

                                                // Throttle seeks to ExoPlayer to ~100ms for smooth audio with zero buffer thrashing
                                                if (now - lastSeekTime >= 100L) {
                                                    latestOnSeekTo(localScrubPos)
                                                    lastSeekTime = now
                                                }
                                            }

                                            lastAngle = currentAngle
                                            lastTime = now
                                        }
                                    } finally {
                                        longPressJob.cancel()
                                        if (isRotating) {
                                            isUserRotatingCd = false
                                            latestOnSeekTo(localScrubPos)
                                            // Keep scrub position pinned for 250ms while ExoPlayer finishes async seek, preventing snapback
                                            coroutineScope.launch {
                                                delay(250)
                                                latestOnScrubPositionUpdate(null)
                                            }
                                        }
                                    }
                                }
                            }
                            .background(accentColor.copy(alpha = 0.05f), CircleShape)
                            .border(
                                2.dp,
                                Brush.sweepGradient(listOf(accentColor, accentColor.copy(alpha = 0.1f), accentColor)),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        FocusedPlayerArtworkWidget(
                            artPath = track.artPath,
                            trackTitle = track.title,
                            accentColor = accentColor,
                            modifier = Modifier
                                .size(cdArtSize - 16.dp)
                                .then(if (carouselPagerState != null) Modifier.coverCarouselParallax(carouselPagerState, carouselPage) else Modifier)
                                .clip(CircleShape)
                                .rotate(currentRotation)
                        )

                        // Minimal physical disc spindle center hole representation
                        Box(
                            modifier = Modifier
                                .size((cdArtSize * 0.1f).coerceIn(16.dp, 32.dp))
                                .background(MaterialTheme.colorScheme.background, CircleShape)
                                .border(1.dp, accentColor, CircleShape)
                        )
                    }
                } else {
                    val shape = when (albumArtStyle) {
                        AlbumArtStyle.ROUNDED -> RoundedCornerShape(24.dp)
                        AlbumArtStyle.COOKIE -> com.example.ui.components.CookieShape()
                        AlbumArtStyle.SOFT_BURST -> com.example.ui.components.SoftBurstShape()
                        else -> androidx.compose.ui.graphics.RectangleShape
                    }
                    Box(
                        modifier = Modifier
                            .size(artSize)
                            .scale(scalePulse)
                            .then(
                                if (carouselPagerState != null) {
                                    Modifier.coverCarouselArt(
                                        carouselPagerState, carouselPage, shape,
                                        // Cut edges match the cover's own corners (square art stays square).
                                        windowCornerPx = with(density) { (if (albumArtStyle == AlbumArtStyle.SQUARE) 0.dp else 24.dp).toPx() },
                                        shadowElevationPx = with(density) { 16.dp.toPx() }
                                    )
                                } else {
                                    Modifier.shadow(16.dp, shape).clip(shape)
                                }
                            )
                            .combinedClickable(
                                onClick = {},
                                onLongClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onLongClick(track)
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        FocusedPlayerArtworkWidget(
                            artPath = track.artPath,
                            trackTitle = track.title,
                            accentColor = accentColor,
                            modifier = Modifier
                                .fillMaxSize()
                                .then(if (carouselPagerState != null) Modifier.coverCarouselParallax(carouselPagerState, carouselPage) else Modifier)
                        )
                    }
                }
            }
        }
        
        // Text track descriptions
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (carouselPagerState != null) Modifier.coverCarouselFade(carouselPagerState, carouselPage) else Modifier),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                track.title,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                track.artist.uppercase(),
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Medium,
                color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.7f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun PlaybackProgressSlider(
    viewModel: MusicViewModel,
    accentColor: Color,
    onBgColor: Color,
    scrubPositionMs: Int? = null
) {
    val playbackProgress by viewModel.playbackController.playbackProgress.collectAsStateWithLifecycle()
    var sliderDragPositionMs by remember { mutableStateOf<Int?>(null) }

    val displayedPosition = scrubPositionMs ?: sliderDragPositionMs ?: playbackProgress.position
    val duration = playbackProgress.duration.coerceAtLeast(1)

    // Keep showing the last known position while buffering (e.g. right after a seek) instead of
    // snapping the bar back to 0.
    val progressVal = (displayedPosition.toFloat() / duration.toFloat()).coerceIn(0f, 1f)

    Column {
        Slider(
            value = progressVal,
            onValueChange = { targetRatio ->
                // Only move the thumb while dragging; seeking on every frame thrashes ExoPlayer's buffer.
                sliderDragPositionMs = (targetRatio * duration).toInt()
            },
            onValueChangeFinished = {
                sliderDragPositionMs?.let { viewModel.playbackController.seekTo(it) }
                sliderDragPositionMs = null
            },
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = Color.White,
                inactiveTrackColor = Color.White.copy(alpha = 0.25f),
                inactiveTickColor = Color.Transparent,
                activeTickColor = Color.Transparent
            ),
            modifier = Modifier.fillMaxWidth().testTag("playback_seekbar"),
            thumb = {
                Box(
                    modifier = Modifier
                        .offset(y = 1.dp)
                        .size(12.dp)
                        .background(Color.White, CircleShape)
                )
            },
            track = { sliderState ->
                val fraction = if (sliderState.valueRange.endInclusive > sliderState.valueRange.start) {
                    (sliderState.value - sliderState.valueRange.start) / (sliderState.valueRange.endInclusive - sliderState.valueRange.start)
                } else 0f
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.25f)),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction.coerceIn(0f, 1f))
                            .fillMaxHeight()
                            .background(Color.White)
                    )
                }
            }
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                formatDuration(displayedPosition.toLong()),
                color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.7f),
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Medium
            )
            Text(
                formatDuration(playbackProgress.duration.toLong()),
                color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.7f),
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
