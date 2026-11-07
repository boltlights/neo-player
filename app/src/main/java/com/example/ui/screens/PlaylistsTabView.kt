package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.LibraryAlbumArtStyle
import com.example.data.PlaylistEntity
import com.example.data.PlaylistTrackCrossRef
import com.example.data.TrackEntity
import com.example.ui.components.CookieShape
import com.example.ui.components.FastScrollbar
import com.example.ui.components.MosaicCoverArt
import com.example.ui.components.SoftBurstShape

@Composable
fun PlaylistsTabView(
    playlists: List<PlaylistEntity>,
    filteredTracks: List<TrackEntity>,
    playlistCrossRefs: List<PlaylistTrackCrossRef>,
    recentlyPlayed: List<TrackEntity>,
    mostPlayed: List<TrackEntity>,
    recentlyAdded: List<TrackEntity>,
    totalTopPadding: Dp,
    accentColor: Color,
    libraryAlbumArtStyle: LibraryAlbumArtStyle,
    onCreatePlaylistClick: () -> Unit,
    onPlaylistClick: (PlaylistEntity) -> Unit
) {
    val playlistsMap = remember(playlists, filteredTracks, playlistCrossRefs, recentlyPlayed, mostPlayed, recentlyAdded) {
        val list = mutableListOf<Pair<PlaylistEntity, List<TrackEntity>>>()

        // Smart playlists defaults
        list.add(Pair(PlaylistEntity(id = -1, name = "Recently Played"), recentlyPlayed))
        list.add(Pair(PlaylistEntity(id = -2, name = "Most Played"), mostPlayed))
        list.add(Pair(PlaylistEntity(id = -3, name = "Recently Added"), recentlyAdded))

        playlists.forEach { pl ->
            val trackIdsInPl = playlistCrossRefs.filter { it.playlistId == pl.id }.map { it.trackId }
            val tracksInPl = filteredTracks.filter { trackIdsInPl.contains(it.id) }
            list.add(Pair(pl, tracksInPl))
        }
        list
    }

    if (playlistsMap.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = totalTopPadding),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.AutoMirrored.Filled.QueueMusic,
                contentDescription = "Empty Playlists",
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.15f)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                "No Playlists",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(alpha = 0.5f)
            )
            Text(
                "Create custom playlists.",
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.3f),
                modifier = Modifier.padding(top = 4.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onCreatePlaylistClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Create",
                        modifier = Modifier.size(14.dp),
                        tint = Color.Black
                    )
                    Text(
                        "CREATE",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }
            }
        }
    } else {
        val playlistsListState = rememberLazyListState()
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                state = playlistsListState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 8.dp, end = 24.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp),
                contentPadding = PaddingValues(top = totalTopPadding + 32.dp, bottom = 80.dp)
            ) {
                item {
                    Button(
                        onClick = onCreatePlaylistClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                            .height(48.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Create New Playlist",
                                modifier = Modifier.size(18.dp),
                                tint = Color.Black
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "CREATE PLAYLIST",
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }
                items(playlistsMap) { (pl, playlistTracks) ->
                    val isSmartPlaylist = pl.id in listOf(-1, -2, -3)
                    val artShape = if (isSmartPlaylist) {
                        RoundedCornerShape(12.dp)
                    } else {
                        when (libraryAlbumArtStyle) {
                            LibraryAlbumArtStyle.COOKIE -> CookieShape()
                            LibraryAlbumArtStyle.SOFT_BURST -> SoftBurstShape()
                            else -> RoundedCornerShape(12.dp)
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color.Transparent,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPlaylistClick(pl) }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp).fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            MosaicCoverArt(
                                imageUris = playlistTracks.map { it.artPath },
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(artShape),
                                accentColor = accentColor,
                                iconIfEmpty = Icons.AutoMirrored.Filled.QueueMusic
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    pl.name,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    com.example.util.songCount(playlistTracks.size),
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }
            }
            FastScrollbar(
                listState = playlistsListState,
                itemCount = playlistsMap.size,
                accentColor = accentColor,
                popupText = { index -> playlistsMap.getOrNull(index)?.first?.name?.firstOrNull()?.uppercase() ?: "" },
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .padding(top = totalTopPadding + 16.dp, bottom = 80.dp, end = 2.dp)
            )
        }
    }
}
