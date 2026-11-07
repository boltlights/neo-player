package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.LibraryAlbumArtStyle
import com.example.R
import com.example.data.TrackEntity
import com.example.ui.components.CookieShape
import com.example.ui.components.FastScrollbar
import com.example.ui.components.TrackArtworkWidget
import com.example.ui.components.TrackRowItem
import com.example.ui.components.rememberReorderableLazyListState
import com.example.ui.components.reorderDragHandle
import com.example.ui.components.reorderableItem
import java.util.Calendar

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SongsTabView(
    tracks: List<TrackEntity>,
    filteredTracks: List<TrackEntity>,
    currentTrack: TrackEntity?,
    isPlaying: Boolean,
    accentColor: Color,
    libraryAlbumArtStyle: LibraryAlbumArtStyle,
    sortOption: String,
    onSortOptionChange: (String) -> Unit,
    onReorderLibrary: (List<TrackEntity>) -> Unit,
    totalTopPadding: Dp,
    searchQuery: String,
    isMultiSelectActive: Boolean,
    selectedTracks: SnapshotStateList<TrackEntity>,
    onToggleSelection: (TrackEntity) -> Unit,
    onStartMultiSelect: (TrackEntity) -> Unit,
    onCancelMultiSelect: () -> Unit,
    onTrackPlay: (List<TrackEntity>, TrackEntity) -> Unit,
    onTrackOptionClick: (TrackEntity) -> Unit,
    onPlayNext: (TrackEntity) -> Unit,
    onAddToQueue: (TrackEntity) -> Unit,
    onDeleteTrack: (TrackEntity) -> Unit,
    onShowProperties: (TrackEntity) -> Unit,
    onPlayArtist: (String) -> Unit,
    onPlayGenre: (String) -> Unit,
    onPlayNextTracks: (List<TrackEntity>) -> Unit,
    onAddToQueueTracks: (List<TrackEntity>) -> Unit,
    onOpenPlaylistSelectForGroupAdd: () -> Unit,
    onOpenBatchDeleteConfirm: () -> Unit
) {
    val context = LocalContext.current
    val songsListState = rememberLazyListState()
    var currentTracks by remember(filteredTracks) { mutableStateOf(filteredTracks) }

    val selectedTrackIds = remember(selectedTracks.size, isMultiSelectActive) {
        if (isMultiSelectActive) selectedTracks.map { it.id }.toSet() else emptySet()
    }
    val yearCalendar = remember { Calendar.getInstance() }

    var activeMenuTrack by remember { mutableStateOf<TrackEntity?>(null) }
    var showArtistPicker by remember { mutableStateOf(false) }
    var showGenrePicker by remember { mutableStateOf(false) }
    var artistPickerOptions by remember { mutableStateOf<List<String>>(emptyList()) }
    var genrePickerOptions by remember { mutableStateOf<List<String>>(emptyList()) }

    // Stable remembered callbacks to guarantee Compose skipping across all 3,000+ items
    val onPlayTrack = remember<(TrackEntity) -> Unit>(currentTracks, onTrackPlay) {
        { track -> onTrackPlay(currentTracks, track) }
    }
    val onToggleTrack = remember<(TrackEntity) -> Unit>(onToggleSelection) {
        { track -> onToggleSelection(track) }
    }
    val onLongClickTrack = remember<(TrackEntity) -> Unit>(onStartMultiSelect) {
        { track -> onStartMultiSelect(track) }
    }
    val onOptionsTrack = remember<(TrackEntity) -> Unit> {
        { track -> activeMenuTrack = track }
    }
    val onPlayNextTrack = remember<(TrackEntity) -> Unit>(onPlayNext) {
        { track -> onPlayNext(track) }
    }

    val fastScrollbarPopupText: (Int) -> String = remember(filteredTracks, sortOption) {
        { index ->
            val song = filteredTracks.getOrNull(index)
            if (song != null) {
                when (sortOption) {
                    "Artist" -> song.artist.firstOrNull()?.uppercase() ?: "?"
                    "Album" -> song.album?.firstOrNull()?.uppercase() ?: "?"
                    "Oldest", "Newest" -> {
                        yearCalendar.timeInMillis = song.addedAt
                        yearCalendar.get(Calendar.YEAR).toString()
                    }
                    "Track Number" -> song.trackNumber?.toString() ?: "?"
                    else -> song.title.firstOrNull()?.uppercase() ?: "?"
                }
            } else ""
        }
    }

    LaunchedEffect(filteredTracks) {
        currentTracks = filteredTracks
    }

    val isCustomSort = sortOption == "Custom"
    val showDragHandles = isCustomSort && !isMultiSelectActive

    val reorderState = rememberReorderableLazyListState(
        lazyListState = songsListState,
        canDragOver = { draggedOverIndex ->
            val offset = if (isCustomSort && !isMultiSelectActive) 1 else 0
            draggedOverIndex >= offset
        },
        onMove = { fromIndex, toIndex ->
            val list = currentTracks.toMutableList()
            val offset = if (isCustomSort && !isMultiSelectActive) 1 else 0
            val adjustedFrom = fromIndex - offset
            val adjustedTo = toIndex - offset
            if (adjustedFrom < 0 || adjustedFrom >= list.size || adjustedTo < 0 || adjustedTo >= list.size) return@rememberReorderableLazyListState
            val item = list.removeAt(adjustedFrom)
            list.add(adjustedTo, item)
            currentTracks = list
        },
        onDragEnd = {
            onSortOptionChange("Custom")
            onReorderLibrary(currentTracks)
        }
    )

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        if (filteredTracks.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = totalTopPadding + 100.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier
                        .size(96.dp)
                        .background(
                            color = Color.Black,
                            shape = CookieShape()
                        )
                        .padding(24.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = if (searchQuery.isNotEmpty()) "No tracks found matching query" else "Add songs",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.width(256.dp)
                )
            }
        } else {
            LazyColumn(
                state = songsListState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(end = 12.dp, start = 8.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp),
                contentPadding = PaddingValues(top = totalTopPadding + 32.dp, bottom = 80.dp)
            ) {
                if (isCustomSort && !isMultiSelectActive) {
                    item(key = "custom_order_banner") {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = accentColor.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, accentColor.copy(alpha = 0.35f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        Icons.Default.Reorder,
                                        contentDescription = null,
                                        tint = accentColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        "Custom Order • Drag handles to reorder",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                }
                                TextButton(
                                    onClick = { onSortOptionChange("Track Number") },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                                ) {
                                    Text("Reset", fontSize = 12.sp, color = accentColor, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                itemsIndexed(
                    items = currentTracks,
                    key = { _, track -> track.id },
                    contentType = { _, _ -> "track_item" }
                ) { index, track ->
                    val isSelected = isMultiSelectActive && selectedTrackIds.contains(track.id)

                    val itemModifier = if (showDragHandles) {
                        val bannerOffset = if (isCustomSort && !isMultiSelectActive) 1 else 0
                        Modifier
                            .fillMaxWidth()
                            .animateItemPlacement()
                            .reorderableItem(reorderState, key = track.id)
                    } else {
                        Modifier.fillMaxWidth()
                    }

                    Box(modifier = itemModifier) {
                        TrackRowItem(
                            track = track,
                            isPlaying = isPlaying && currentTrack?.id == track.id,
                            isActive = currentTrack?.id == track.id,
                            accentColor = accentColor,
                            libraryAlbumArtStyle = libraryAlbumArtStyle,
                            isMultiSelectActive = isMultiSelectActive,
                            isSelected = isSelected,
                            showDragHandle = showDragHandles,
                            isSwipeEnabled = !isMultiSelectActive && !showDragHandles,
                            reorderModifier = if (showDragHandles) {
                                val bannerOffset = if (isCustomSort && !isMultiSelectActive) 1 else 0
                                Modifier.reorderDragHandle(reorderState, index + bannerOffset, track.id)
                            } else {
                                Modifier
                            },
                            onTrackPlay = onPlayTrack,
                            onTrackOptionsClick = onOptionsTrack,
                            onTrackToggleSelection = onToggleTrack,
                            onTrackLongClick = onLongClickTrack,
                            onTrackPlayNext = onPlayNextTrack
                        )
                    }
                }
            }

            if (!isMultiSelectActive) {
                FastScrollbar(
                    listState = songsListState,
                    itemCount = filteredTracks.size,
                    popupText = fastScrollbarPopupText,
                    accentColor = accentColor,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .fillMaxHeight()
                        .padding(top = totalTopPadding + 16.dp, bottom = 80.dp, end = 2.dp)
                )
            }

            AnimatedVisibility(
                visible = isMultiSelectActive,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 96.dp)
                    .padding(horizontal = 24.dp),
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = Color(0xFF1E1E1E),
                            shape = RoundedCornerShape(32.dp)
                        )
                        .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(32.dp))
                        .padding(horizontal = 20.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "${selectedTracks.size} Selected",
                        color = accentColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(
                            onClick = {
                                selectedTracks.clear()
                                selectedTracks.addAll(tracks)
                            }
                        ) {
                            Icon(Icons.Default.SelectAll, contentDescription = "Select All", tint = Color.White)
                        }
                        IconButton(onClick = onOpenPlaylistSelectForGroupAdd) {
                            Icon(Icons.AutoMirrored.Filled.PlaylistAdd, contentDescription = "Add to playlist", tint = Color.White)
                        }
                        IconButton(
                            onClick = {
                                if (selectedTracks.isNotEmpty()) {
                                    onPlayNextTracks(selectedTracks.toList())
                                    onCancelMultiSelect()
                                }
                            }
                        ) {
                            Icon(Icons.AutoMirrored.Filled.PlaylistPlay, contentDescription = "Play next", tint = Color.White)
                        }
                        IconButton(
                            onClick = {
                                if (selectedTracks.isNotEmpty()) {
                                    onAddToQueueTracks(selectedTracks.toList())
                                    onCancelMultiSelect()
                                }
                            }
                        ) {
                            Icon(Icons.AutoMirrored.Filled.QueueMusic, contentDescription = "Add to queue", tint = Color.White)
                        }
                        IconButton(
                            onClick = {
                                if (selectedTracks.isNotEmpty()) {
                                    onOpenBatchDeleteConfirm()
                                } else {
                                    Toast.makeText(context, context.getString(R.string.toast_no_tracks_selected), Toast.LENGTH_SHORT).show()
                                }
                            }
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Batch delete tracks", tint = Color.Red)
                        }
                        IconButton(onClick = onCancelMultiSelect) {
                            Icon(Icons.Default.Close, contentDescription = "Cancel", tint = Color.White)
                        }
                    }
                }
            }
        }
    }

    if (activeMenuTrack != null) {
        val track = activeMenuTrack!!
        ModalBottomSheet(
            onDismissRequest = { activeMenuTrack = null }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    TrackArtworkWidget(
                        artPath = track.artPath,
                        trackTitle = track.title,
                        accentColor = accentColor,
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        sizeLimit = 180
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = track.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = com.example.util.artistAndAlbum(track.artist, track.album),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(bottom = 8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            activeMenuTrack = null
                            onPlayNext(track)
                        }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.AutoMirrored.Filled.PlaylistPlay, contentDescription = null, tint = accentColor)
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("Play Next", fontSize = 16.sp)
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            activeMenuTrack = null
                            onAddToQueue(track)
                        }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.AutoMirrored.Filled.QueueMusic, contentDescription = null, tint = accentColor)
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("Add to Queue", fontSize = 16.sp)
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            activeMenuTrack = null
                            onTrackOptionClick(track)
                        }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.AutoMirrored.Filled.PlaylistAdd, contentDescription = null, tint = accentColor)
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("Add to Playlist", fontSize = 16.sp)
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            activeMenuTrack = null
                            val artists = track.artist.split(Regex("[,&;/]")).map { it.trim() }.filter { it.isNotEmpty() }
                            if (artists.size > 1) {
                                artistPickerOptions = artists
                                showArtistPicker = true
                            } else {
                                onPlayArtist(artists.firstOrNull() ?: track.artist)
                            }
                        }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = accentColor)
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("Play Artist", fontSize = 16.sp)
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            activeMenuTrack = null
                            val genres = (track.genre ?: "Unknown").split(Regex("[,&;/]")).map { it.trim() }.filter { it.isNotEmpty() }
                            if (genres.size > 1) {
                                genrePickerOptions = genres
                                showGenrePicker = true
                            } else {
                                onPlayGenre(genres.firstOrNull() ?: track.genre ?: "Unknown")
                            }
                        }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Category, contentDescription = null, tint = accentColor)
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("Play Genre", fontSize = 16.sp)
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            activeMenuTrack = null
                            onShowProperties(track)
                        }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = accentColor)
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("Properties", fontSize = 16.sp)
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            activeMenuTrack = null
                            onDeleteTrack(track)
                        }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red.copy(alpha = 0.8f))
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("Delete", fontSize = 16.sp, color = Color.Red.copy(alpha = 0.8f))
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    if (showArtistPicker && artistPickerOptions.isNotEmpty()) {
        ModalBottomSheet(onDismissRequest = { showArtistPicker = false }) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Text(
                    "Play from Artist",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                LazyColumn {
                    items(artistPickerOptions.size) { index ->
                        val artist = artistPickerOptions[index]
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showArtistPicker = false
                                    onPlayArtist(artist)
                                }
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Person, contentDescription = null, tint = accentColor)
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(artist, fontSize = 16.sp)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    if (showGenrePicker && genrePickerOptions.isNotEmpty()) {
        ModalBottomSheet(onDismissRequest = { showGenrePicker = false }) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Text(
                    "Play from Genre",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                LazyColumn {
                    items(genrePickerOptions.size) { index ->
                        val genre = genrePickerOptions[index]
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showGenrePicker = false
                                    onPlayGenre(genre)
                                }
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Category, contentDescription = null, tint = accentColor)
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(genre, fontSize = 16.sp)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
