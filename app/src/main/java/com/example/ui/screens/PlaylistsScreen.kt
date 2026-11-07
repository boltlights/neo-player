package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.PlaylistEntity
import com.example.data.PlaylistTrackCrossRef
import com.example.data.TrackEntity
import com.example.ui.components.AlbumArtworkFiller
import com.example.ui.components.TrackArtworkWidget

@Composable
fun PlaylistArtworkWidget(
    playlistName: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
    ) {
        AlbumArtworkFiller(
            trackTitle = playlistName,
            accentColor = accentColor,
            modifier = Modifier.fillMaxSize(),
            alpha = 0.15f
        )
        Icon(
            imageVector = Icons.Default.QueueMusic,
            contentDescription = null,
            tint = accentColor,
            modifier = Modifier
                .size(24.dp)
                .align(Alignment.Center)
        )
    }
}

@Composable
fun PlaylistsScreen(
    playlists: List<PlaylistEntity>,
    crossRefs: List<PlaylistTrackCrossRef>,
    allTracks: List<TrackEntity>,
    accentColor: Color,
    surfaceColor: Color,
    onCreatePlaylistClick: () -> Unit,
    onImportPlaylistClick: () -> Unit = {},
    onPlaylistSelect: (PlaylistEntity) -> Unit,
    onPlaylistPlay: (PlaylistEntity) -> Unit,
    onPlaylistDelete: (PlaylistEntity) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 8.dp)
        ) {
            Text(
                "PLAYLISTS",
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                color = accentColor
            )
            Text(
                "CHOOSE OR COMPILE GROUPS",
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onCreatePlaylistClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("create_playlist_button"),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Create",
                        modifier = Modifier.size(18.dp),
                        tint = Color.Black
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "CREATE",
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            OutlinedButton(
                onClick = onImportPlaylistClick,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = accentColor
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, accentColor.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("import_playlist_button"),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.FileDownload,
                        contentDescription = "Import",
                        modifier = Modifier.size(18.dp),
                        tint = accentColor
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "IMPORT M3U",
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = accentColor,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            if (playlists.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.QueueMusic,
                        contentDescription = "Empty lists",
                        modifier = Modifier.size(80.dp),
                        tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.15f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "No Playlists",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                    )
                    Text(
                        "Organize your customized music loops.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(playlists, key = { it.id }) { playlist ->
                        PlaylistRowItem(
                            playlist = playlist,
                            crossRefs = crossRefs,
                            allTracks = allTracks,
                            accentColor = accentColor,
                            onClick = { onPlaylistSelect(playlist) },
                            onPlay = { onPlaylistPlay(playlist) },
                            onDelete = { onPlaylistDelete(playlist) }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun PlaylistDetailScreen(
    playlist: PlaylistEntity,
    tracksInPlaylist: List<TrackEntity>,
    allAvailableTracks: List<TrackEntity> = emptyList(),
    currentTrack: TrackEntity? = null,
    isPlaying: Boolean = false,
    accentColor: Color,
    onBgColor: Color,
    surfaceColor: Color,
    onBack: () -> Unit,
    onPlayTrack: (TrackEntity) -> Unit,
    onShufflePlay: () -> Unit,
    onRemoveTrack: (TrackEntity) -> Unit,
    onShowProperties: (TrackEntity) -> Unit,
    onDeletePlaylist: () -> Unit,
    onRenamePlaylist: (String) -> Unit,
    onUpdateCoverArt: (String?) -> Unit = {},
    onAddTracks: (List<TrackEntity>) -> Unit = {},
    onExportPlaylist: () -> Unit = {},
    onPlayNext: () -> Unit = {},
    onAddToQueue: () -> Unit = {},
    onPlayTrackNext: (TrackEntity) -> Unit = {}
) {
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    val isScrolled by remember {
        derivedStateOf { listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 200 }
    }
    var showOptionsMenu by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showAddTracksDialog by remember { mutableStateOf(false) }
    var renameInput by remember { mutableStateOf(playlist.name) }

    val context = androidx.compose.ui.platform.LocalContext.current

    val photoPicker = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            uri?.let {
                // Photo-picker grants are temporary; without persisting, the cover broke after an app restart.
                try {
                    context.contentResolver.takePersistableUriPermission(it, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                } catch (e: Exception) {
                    timber.log.Timber.tag("PlaylistsScreen").w(e, "Could not persist cover art permission for %s", it)
                }
                onUpdateCoverArt(it.toString())
                android.widget.Toast.makeText(context, context.getString(com.example.R.string.toast_updated_cover_art), android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    )

    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    
    val totalDurationMs = tracksInPlaylist.sumOf { it.durationMs }
    val durationHours = totalDurationMs / (1000 * 60 * 60)
    val durationMinutes = (totalDurationMs / (1000 * 60)) % 60
    val durationStr = if (durationHours > 0) "${durationHours}h ${durationMinutes}m" else "${durationMinutes}m"

    if (showAddTracksDialog) {
        val tracksNotInPlaylist = remember(allAvailableTracks, tracksInPlaylist) {
            val existingIds = tracksInPlaylist.map { it.id }.toSet()
            allAvailableTracks.filter { !existingIds.contains(it.id) }
        }
        com.example.ui.components.AddTracksToPlaylistDialog(
            playlistName = playlist.name,
            availableTracks = tracksNotInPlaylist,
            accentColor = accentColor,
            surfaceColor = surfaceColor,
            onBgColor = onBgColor,
            onDismiss = { showAddTracksDialog = false },
            onTracksSelected = { selected ->
                showAddTracksDialog = false
                if (selected.isNotEmpty()) {
                    onAddTracks(selected)
                    android.widget.Toast.makeText(
                        context,
                        context.getString(com.example.R.string.toast_added_songs_to_playlist, selected.size, playlist.name),
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            }
        )
    }

    if (showRenameDialog) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Rename playlist", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = renameInput,
                    onValueChange = { renameInput = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (renameInput.isNotBlank()) {
                            onRenamePlaylist(renameInput)
                        }
                        showRenameDialog = false
                    }
                ) {
                    Text("OK", color = accentColor, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) {
                    Text("Cancel", color = onBgColor.copy(alpha = 0.7f))
                }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        androidx.compose.foundation.lazy.LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 120.dp)
        ) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.statusBars)
                        .padding(top = 24.dp, start = 16.dp, end = 16.dp, bottom = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onBack, modifier = Modifier.align(Alignment.Top)) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = onBgColor)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .size(140.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .combinedClickable(
                                    onClick = {},
                                    onLongClick = {
                                        if (playlist.id > 0) {
                                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                            photoPicker.launch(
                                                androidx.activity.result.PickVisualMediaRequest(
                                                    androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly
                                                )
                                            )
                                        }
                                    }
                                )
                        ) {
                            if (playlist.coverArtUri != null) {
                                coil.compose.AsyncImage(
                                    model = playlist.coverArtUri,
                                    contentDescription = "Playlist Cover",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = coil.size.Scale.FILL.let { androidx.compose.ui.layout.ContentScale.Crop }
                                )
                            } else {
                                com.example.ui.components.MosaicCoverArt(
                                    imageUris = tracksInPlaylist.take(4).map { it.artPath },
                                    modifier = Modifier.fillMaxSize(),
                                    accentColor = accentColor,
                                    iconIfEmpty = Icons.Default.MusicNote
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = playlist.name,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = onBgColor,
                            lineHeight = 36.sp,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Language, // Globe icon approx
                            contentDescription = null,
                            tint = onBgColor.copy(alpha = 0.6f),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = durationStr,
                            color = onBgColor.copy(alpha = 0.6f),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onShufflePlay) {
                            Icon(
                                Icons.Default.Shuffle,
                                contentDescription = "Shuffle",
                                tint = onBgColor,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(accentColor)
                                .clickable {
                                    if (tracksInPlaylist.isNotEmpty()) onPlayTrack(tracksInPlaylist.first())
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.PlayArrow,
                                contentDescription = "Play",
                                tint = MaterialTheme.colorScheme.background,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            if (playlist.id > 0) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showAddTracksDialog = true }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(4.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add", tint = onBgColor.copy(alpha = 0.6f), modifier = Modifier.size(32.dp))
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            "Add to this playlist",
                            fontSize = 16.sp,
                            color = onBgColor,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            items(tracksInPlaylist, key = { it.id }) { song ->
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    PlaylistTrackItem(
                        song = song,
                        accentColor = accentColor,
                        isActive = currentTrack?.id == song.id,
                        isPlaying = isPlaying && currentTrack?.id == song.id,
                        onPlay = { onPlayTrack(song) },
                        onPlayNext = { onPlayTrackNext(song) },
                        onShowProperties = { onShowProperties(song) }
                    )
                }
            }
        }

        // Custom TopAppBar Overlay
        val appBarColor by animateColorAsState(if (isScrolled) MaterialTheme.colorScheme.surface else Color.Transparent)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(appBarColor)
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(top = 16.dp, bottom = 16.dp, start = 8.dp, end = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = onBgColor)
                    }
                    AnimatedVisibility(visible = isScrolled, enter = fadeIn(), exit = fadeOut()) {
                        Text(playlist.name, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = onBgColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AnimatedVisibility(visible = isScrolled && tracksInPlaylist.isNotEmpty(), enter = fadeIn(), exit = fadeOut()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { onPlayTrack(tracksInPlaylist.first()) }
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = "Play", tint = onBgColor)
                            }
                        }
                    }
                    IconButton(onClick = { showOptionsMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Options", tint = onBgColor)
                    }
                }
            }
        }

        if (showOptionsMenu) {
            val sheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = false)
            ModalBottomSheet(
                onDismissRequest = { showOptionsMenu = false },
                sheetState = sheetState,
                containerColor = MaterialTheme.colorScheme.surface,
                scrimColor = Color.Black.copy(alpha = 0.6f),
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp, top = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (playlist.coverArtUri != null) {
                            coil.compose.AsyncImage(
                                model = playlist.coverArtUri,
                                contentDescription = "Playlist Cover",
                                modifier = Modifier.size(56.dp).clip(RoundedCornerShape(8.dp)),
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                            )
                        } else {
                            com.example.ui.components.MosaicCoverArt(
                                imageUris = tracksInPlaylist.take(4).map { it.artPath },
                                modifier = Modifier.size(56.dp).clip(RoundedCornerShape(8.dp)),
                                accentColor = accentColor,
                                iconIfEmpty = Icons.Default.MusicNote
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text("Playlist", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha=0.6f))
                            Text(playlist.name, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            Text("${tracksInPlaylist.size} songs", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha=0.6f))
                        }
                    }
                    
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    val items = listOf(
                        "Play" to Icons.Default.PlayArrow,
                        "Shuffle" to Icons.Default.Shuffle,
                        "Play next" to Icons.Default.PlaylistPlay,
                        "Add to queue" to Icons.Default.QueueMusic,
                        "Export (.m3u8)" to Icons.Default.FileDownload,
                        "Rename" to Icons.Default.Edit,
                        "Delete" to Icons.Default.Delete,
                        "Share" to Icons.Default.Share
                    ).filter { (label, _) ->
                        if (playlist.id < 0) {
                            label != "Rename" && label != "Delete"
                        } else {
                            true
                        }
                    }
                    
                    items.forEach { (label, icon) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { 
                                    if (label == "Delete") {
                                        onDeletePlaylist()
                                    } else if (label == "Play" && tracksInPlaylist.isNotEmpty()) {
                                        onPlayTrack(tracksInPlaylist.first())
                                    } else if (label == "Shuffle" && tracksInPlaylist.isNotEmpty()) {
                                        onShufflePlay()
                                    } else if (label == "Play next" && tracksInPlaylist.isNotEmpty()) {
                                        onPlayNext()
                                    } else if (label == "Add to queue" && tracksInPlaylist.isNotEmpty()) {
                                        onAddToQueue()
                                    } else if (label == "Export (.m3u8)" && tracksInPlaylist.isNotEmpty()) {
                                        onExportPlaylist()
                                    } else if (label == "Rename") {
                                        renameInput = playlist.name
                                        showRenameDialog = true
                                    }
                                    showOptionsMenu = false
                                }
                                .padding(horizontal = 24.dp, vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(icon, contentDescription = label, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(24.dp))
                            Text(label, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistTrackItem(
    song: TrackEntity,
    accentColor: Color,
    isActive: Boolean = false,
    isPlaying: Boolean = false,
    onPlay: () -> Unit,
    onPlayNext: () -> Unit = {},
    onShowProperties: () -> Unit = {}
) {
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.StartToEnd) {
                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                onPlayNext()
                false
            } else {
                false
            }
        },
        positionalThreshold = { totalDistance -> totalDistance * 0.35f }
    )

    val isDark = MaterialTheme.colorScheme.background.red + MaterialTheme.colorScheme.background.green + MaterialTheme.colorScheme.background.blue < 1.5f
    val targetBackgroundColor = when {
        isActive && isPlaying -> if (isDark) Color(0xFFE0E0E0).copy(alpha = 0.22f) else Color(0xFF707070).copy(alpha = 0.24f)
        isActive -> if (isDark) Color(0xFF888888).copy(alpha = 0.14f) else Color(0xFF9E9E9E).copy(alpha = 0.14f)
        else -> Color.Transparent
    }
    val animatedBackground by animateColorAsState(
        targetValue = targetBackgroundColor,
        animationSpec = tween(durationMillis = 200),
        label = "playlist_track_bg"
    )

    SwipeToDismissBox(
        state = dismissState,
        modifier = Modifier.fillMaxWidth(),
        enableDismissFromStartToEnd = true,
        enableDismissFromEndToStart = false,
        backgroundContent = {
            val isSwiping = dismissState.dismissDirection == SwipeToDismissBoxValue.StartToEnd
            // Nothing behind the row at rest: even at 28% alpha this layer leaked through the row's
            // anti-aliased rounded corners as faint arcs at the screen edges.
            if (!isSwiping) return@SwipeToDismissBox
            val progress = dismissState.progress
            val bgColor = accentColor.copy(alpha = (0.28f + progress * 0.6f).coerceIn(0.28f, 0.95f))

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(12.dp))
                    .background(bgColor)
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlaylistPlay,
                        contentDescription = "Play Next",
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                    Text(
                        text = "PLAY NEXT",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        },
        content = {
            val baseBg = MaterialTheme.colorScheme.background
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(baseBg)
                    .background(animatedBackground)
                    .clickable { onPlay() }
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TrackArtworkWidget(
                    artPath = song.artPath,
                    trackTitle = song.title,
                    accentColor = accentColor,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    sizeLimit = 180
                )
                
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        song.title,
                        fontSize = 16.sp,
                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // If you have explicit tag logic, place here. For now just Artist
                        Text(song.artist.ifEmpty { "Unknown Artist" }, fontSize = 14.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                
                IconButton(onClick = onShowProperties) {
                    Icon(Icons.Default.MoreHoriz, contentDescription = "More", tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
                }
            }
        }
    )
}

@Composable
fun PlaylistRowItem(
    playlist: PlaylistEntity,
    crossRefs: List<PlaylistTrackCrossRef>,
    allTracks: List<TrackEntity>,
    accentColor: Color,
    onClick: () -> Unit,
    onPlay: () -> Unit,
    onDelete: () -> Unit
) {
    val songCount = androidx.compose.runtime.remember(crossRefs, playlist.id) { crossRefs.count { it.playlistId == playlist.id } }
    var tracksInPlaylist by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<List<TrackEntity>>(emptyList()) }
    androidx.compose.runtime.LaunchedEffect(crossRefs, allTracks, playlist.id) {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
            val playlistTrackIds = crossRefs.filter { it.playlistId == playlist.id }.map { it.trackId }.toSet()
            val filtered = allTracks.filter { playlistTrackIds.contains(it.id) }.take(4)
            tracksInPlaylist = filtered
        }
    }
    
    val tracksLabel = if (songCount == 1) "1 song" else "$songCount songs"

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .border(
                1.dp,
                Color.White.copy(alpha = 0.05f),
                RoundedCornerShape(16.dp)
            )
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Playlist Art Thumbnail
            if (playlist.coverArtUri != null) {
                coil.compose.AsyncImage(
                    model = playlist.coverArtUri,
                    contentDescription = "Playlist Cover",
                    modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                )
            } else {
                com.example.ui.components.MosaicCoverArt(
                    imageUris = tracksInPlaylist.map { it.artPath },
                    modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)),
                    accentColor = accentColor,
                    iconIfEmpty = androidx.compose.material.icons.Icons.Default.MusicNote
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = playlist.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = tracksLabel,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
            
            IconButton(onClick = onPlay) {
                Icon(
                    androidx.compose.material.icons.Icons.Default.PlayArrow,
                    contentDescription = "Play",
                    tint = accentColor
                )
            }
            
            IconButton(onClick = onDelete) {
                Icon(
                    androidx.compose.material.icons.Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        }
    }
}
