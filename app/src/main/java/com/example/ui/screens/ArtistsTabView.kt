package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
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
import com.example.data.IndexedArtist
import com.example.data.TrackEntity
import com.example.ui.components.FastScrollbar
import com.example.ui.components.MosaicCoverArt

@Composable
fun ArtistsTabView(
    filteredTracks: List<TrackEntity>,
    indexedArtists: List<IndexedArtist>,
    searchQuery: String,
    totalTopPadding: Dp,
    accentColor: Color,
    onArtistClick: (String) -> Unit
) {
    val artistsMap = if (searchQuery.isBlank() && indexedArtists.isNotEmpty()) {
        indexedArtists.map { Pair(it.name, it.tracks) }
    } else {
        remember(filteredTracks) {
            filteredTracks.groupBy {
                val effective = if (!it.albumArtist.isNullOrBlank()) it.albumArtist else it.artist
                effective
            }
                .map { (artistName, artistTracks) -> Pair(artistName, artistTracks) }
                .sortedBy { it.first.lowercase() }
        }
    }

    if (artistsMap.isEmpty()) {
        Text(
            "No artists loaded",
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .padding(top = totalTopPadding + 32.dp),
            fontSize = 13.sp,
            fontFamily = FontFamily.Monospace
        )
    } else {
        val artistsListState = rememberLazyListState()
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                state = artistsListState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 8.dp, end = 24.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp),
                contentPadding = PaddingValues(top = totalTopPadding + 32.dp, bottom = 80.dp)
            ) {
                items(artistsMap) { (artistName, artistTracks) ->
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color.Transparent,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onArtistClick(artistName) }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp).fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val artShape = CircleShape
                            MosaicCoverArt(
                                imageUris = artistTracks.map { it.artPath },
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(artShape),
                                accentColor = accentColor,
                                iconIfEmpty = Icons.Default.Person
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    artistName,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    com.example.util.songCount(artistTracks.size),
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }
            }
            FastScrollbar(
                listState = artistsListState,
                itemCount = artistsMap.size,
                accentColor = accentColor,
                popupText = { index -> artistsMap.getOrNull(index)?.first?.firstOrNull()?.uppercase() ?: "" },
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .padding(top = totalTopPadding + 16.dp, bottom = 80.dp, end = 2.dp)
            )
        }
    }
}
