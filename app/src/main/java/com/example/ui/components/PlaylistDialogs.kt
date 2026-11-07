package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.MusicViewModel
import com.example.data.PlaylistEntity
import com.example.data.TrackEntity

@Composable
fun NewPlaylistDialog(
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit,
    initialName: String = "",
    accentColor: Color,
    onBgColor: Color
) {
    var name by remember { mutableStateOf(initialName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Create Playlist", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
        },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = accentColor,
                    cursorColor = accentColor
                )
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onCreate(name.trim()) },
                enabled = name.trim().isNotEmpty(),
                colors = ButtonDefaults.textButtonColors(contentColor = accentColor)
            ) {
                Text("CREATE", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(contentColor = onBgColor.copy(alpha = 0.6f))
            ) {
                Text("CANCEL", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Medium)
            }
        }
    )
}

@Composable
fun AddToPlaylistDialog(
    viewModel: MusicViewModel,
    track: TrackEntity,
    allPlaylists: List<PlaylistEntity>,
    accentColor: Color,
    surfaceColor: Color,
    onBgColor: Color,
    onDismiss: () -> Unit,
    onAdded: (String) -> Unit
) {
    var showNewPlaylistDialog by remember { mutableStateOf(false) }
    val crossRefs by viewModel.allPlaylistTrackCrossRefs.collectAsState(initial = emptyList())

    if (showNewPlaylistDialog) {
        // Smart naming logic like Auxio
        val baseName = "Playlist"
        var i = 1
        var proposedName = "$baseName $i"
        while (allPlaylists.any { it.name == proposedName }) {
            i++
            proposedName = "$baseName $i"
        }

        NewPlaylistDialog(
            initialName = proposedName,
            accentColor = accentColor,
            onBgColor = onBgColor,
            onDismiss = { showNewPlaylistDialog = false },
            onCreate = { name ->
                viewModel.createPlaylist(name)
                onAdded(name)
                showNewPlaylistDialog = false
                onDismiss() // Close the whole flow
            }
        )
    }

    if (!showNewPlaylistDialog) {
        Dialog(onDismissRequest = onDismiss) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = surfaceColor,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .border(1.dp, accentColor.copy(alpha = 0.3f), RoundedCornerShape(24.dp))
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Add to Playlist",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = accentColor,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    Text(
                        "Select target playlist for '${track.title}'",
                        fontSize = 12.sp,
                        color = onBgColor.copy(alpha = 0.6f),
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    LazyColumn(modifier = Modifier.heightIn(max = 240.dp)) {
                        items(allPlaylists) { plist ->
                            val isAlreadyInPlaylist = crossRefs.any { it.playlistId == plist.id && it.trackId == track.id }
                            
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = !isAlreadyInPlaylist) {
                                        viewModel.addTrackToPlaylist(plist.id, track.id)
                                        onAdded(plist.name)
                                        onDismiss()
                                    }
                                    .padding(vertical = 12.dp, horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    plist.name,
                                    fontSize = 15.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Medium,
                                    color = if (isAlreadyInPlaylist) onBgColor.copy(alpha = 0.4f) else onBgColor
                                )
                                
                                if (isAlreadyInPlaylist) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Already added",
                                        tint = accentColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(onBgColor.copy(alpha = 0.1f))
                            )
                        }
                        
                        // NEW PLAYLIST FOOTER
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showNewPlaylistDialog = true }
                                    .padding(vertical = 12.dp, horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "New Playlist",
                                    tint = accentColor,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(
                                    text = "New Playlist",
                                    fontSize = 15.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = accentColor,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            "Close",
                            color = MaterialTheme.colorScheme.background,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AddTracksToPlaylistDialog(
    playlistName: String,
    availableTracks: List<TrackEntity>,
    accentColor: Color,
    surfaceColor: Color,
    onBgColor: Color,
    onDismiss: () -> Unit,
    onTracksSelected: (List<TrackEntity>) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val selectedTrackIds = remember { mutableStateListOf<Int>() }

    val filteredTracks = remember(searchQuery, availableTracks) {
        if (searchQuery.isBlank()) {
            availableTracks
        } else {
            val terms = searchQuery.trim().lowercase().split(Regex("\\s+")).filter { it.isNotBlank() }
            if (terms.isEmpty()) {
                availableTracks
            } else {
                availableTracks.filter { track ->
                    val title = track.title.lowercase()
                    val artist = track.artist.lowercase()
                    val album = track.album.lowercase()
                    val genre = track.genre?.lowercase() ?: ""
                    val yearStr = track.year?.toString() ?: ""
                    val customTags = track.customTags?.lowercase() ?: ""

                    terms.all { rawTerm ->
                        val term = rawTerm.removePrefix("#").removePrefix("tag:").removePrefix("genre:").removePrefix("year:")
                        if (term.isEmpty()) return@all true

                        title.contains(term) ||
                        artist.contains(term) ||
                        album.contains(term) ||
                        genre.contains(term) ||
                        yearStr.contains(term) ||
                        customTags.contains(term)
                    }
                }
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = surfaceColor,
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .border(1.dp, accentColor.copy(alpha = 0.3f), RoundedCornerShape(24.dp))
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth()
            ) {
                Text(
                    text = "Add Songs to $playlistName",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = accentColor,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search songs...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = accentColor,
                        cursorColor = accentColor
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (filteredTracks.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (availableTracks.isEmpty()) "No available songs to add" else "No songs match search",
                            color = onBgColor.copy(alpha = 0.5f),
                            fontSize = 14.sp
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 280.dp)
                    ) {
                        items(filteredTracks, key = { it.id }) { track ->
                            val isChecked = selectedTrackIds.contains(track.id)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (isChecked) {
                                            selectedTrackIds.remove(track.id)
                                        } else {
                                            selectedTrackIds.add(track.id)
                                        }
                                    }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = { checked ->
                                        if (checked) {
                                            selectedTrackIds.add(track.id)
                                        } else {
                                            selectedTrackIds.remove(track.id)
                                        }
                                    },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = accentColor,
                                        checkmarkColor = MaterialTheme.colorScheme.background
                                    )
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = track.title,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = onBgColor,
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = track.artist,
                                        fontSize = 12.sp,
                                        color = onBgColor.copy(alpha = 0.6f),
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )
                                }
                            }
                            HorizontalDivider(
                                color = onBgColor.copy(alpha = 0.08f),
                                thickness = 0.5.dp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = onBgColor.copy(alpha = 0.7f))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val selectedTracks = availableTracks.filter { selectedTrackIds.contains(it.id) }
                            onTracksSelected(selectedTracks)
                        },
                        enabled = selectedTrackIds.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "Add (${selectedTrackIds.size})",
                            color = MaterialTheme.colorScheme.background,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
