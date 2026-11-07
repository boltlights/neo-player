package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import com.example.data.IndexedAlbum
import com.example.data.TrackEntity
import com.example.ui.components.CookieShape
import com.example.ui.components.FastScrollbar
import com.example.ui.components.SoftBurstShape
import com.example.ui.components.TrackArtworkWidget

@Composable
fun AlbumsTabView(
    filteredTracks: List<TrackEntity>,
    indexedAlbums: List<IndexedAlbum>,
    searchQuery: String,
    totalTopPadding: Dp,
    accentColor: Color,
    libraryAlbumArtStyle: LibraryAlbumArtStyle,
    onAlbumClick: (String) -> Unit
) {
    val albumsMap = if (searchQuery.isBlank() && indexedAlbums.isNotEmpty()) {
        indexedAlbums.map { Triple(it.name, it.tracks, it.artist) }
    } else {
        remember(filteredTracks) {
            filteredTracks.groupBy { it.album.ifBlank { "Unknown Album" } }
                .map { (albumName, albumTracks) ->
                    Triple(albumName, albumTracks, albumTracks.firstOrNull()?.artist ?: "Unknown Artist")
                }
                .sortedBy { it.first }
        }
    }

    if (albumsMap.isEmpty()) {
        Text(
            "No albums loaded",
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .padding(top = totalTopPadding + 32.dp),
            fontSize = 13.sp,
            fontFamily = FontFamily.Monospace
        )
    } else {
        val albumsListState = rememberLazyListState()
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                state = albumsListState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 8.dp, end = 24.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp),
                contentPadding = PaddingValues(top = totalTopPadding + 32.dp, bottom = 80.dp)
            ) {
                items(albumsMap) { (albumName, albumTracks, artist) ->
                    val artShape = when (libraryAlbumArtStyle) {
                        LibraryAlbumArtStyle.ROUND -> RoundedCornerShape(16.dp)
                        LibraryAlbumArtStyle.COOKIE -> CookieShape()
                        LibraryAlbumArtStyle.SOFT_BURST -> SoftBurstShape()
                        else -> RoundedCornerShape(2.dp)
                    }
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color.Transparent,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onAlbumClick(albumName) }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp).fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TrackArtworkWidget(
                                artPath = albumTracks.firstOrNull()?.artPath,
                                trackTitle = albumName,
                                accentColor = accentColor,
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(artShape),
                                sizeLimit = 180
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    albumName,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    "$artist  •  ${albumTracks.size} songs",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }
            }
            FastScrollbar(
                listState = albumsListState,
                itemCount = albumsMap.size,
                accentColor = accentColor,
                popupText = { index -> albumsMap.getOrNull(index)?.first?.firstOrNull()?.uppercase() ?: "" },
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .padding(top = totalTopPadding + 16.dp, bottom = 80.dp, end = 2.dp)
            )
        }
    }
}
