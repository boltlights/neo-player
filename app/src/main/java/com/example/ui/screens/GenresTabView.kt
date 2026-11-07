package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
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
import com.example.data.TrackEntity
import com.example.ui.components.CookieShape
import com.example.ui.components.FastScrollbar
import com.example.ui.components.MosaicCoverArt
import com.example.ui.components.SoftBurstShape

@Composable
fun GenresTabView(
    filteredTracks: List<TrackEntity>,
    totalTopPadding: Dp,
    accentColor: Color,
    libraryAlbumArtStyle: LibraryAlbumArtStyle,
    onGenreClick: (String) -> Unit
) {
    val genresMap = remember(filteredTracks) {
        filteredTracks.groupBy { getGenreForTrack(it) }
            .map { (genreName, genreTracks) -> Pair(genreName, genreTracks) }
            .sortedBy { it.first }
    }

    if (genresMap.isEmpty()) {
        Text(
            "No genres calculated",
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .padding(top = totalTopPadding + 32.dp),
            fontSize = 13.sp,
            fontFamily = FontFamily.Monospace
        )
    } else {
        val genresListState = rememberLazyListState()
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                state = genresListState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 8.dp, end = 24.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp),
                contentPadding = PaddingValues(top = totalTopPadding + 32.dp, bottom = 80.dp)
            ) {
                items(genresMap) { (genreName, genreTracks) ->
                    val artShape = when (libraryAlbumArtStyle) {
                        LibraryAlbumArtStyle.COOKIE -> CookieShape()
                        LibraryAlbumArtStyle.SOFT_BURST -> SoftBurstShape()
                        else -> RoundedCornerShape(12.dp)
                    }
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color.Transparent,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onGenreClick(genreName) }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp).fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            MosaicCoverArt(
                                imageUris = genreTracks.map { it.artPath },
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(artShape),
                                accentColor = accentColor,
                                iconIfEmpty = Icons.Default.Category
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    genreName,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    "${genreTracks.size} songs",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }
            }
            FastScrollbar(
                listState = genresListState,
                itemCount = genresMap.size,
                accentColor = accentColor,
                popupText = { index -> genresMap.getOrNull(index)?.first?.firstOrNull()?.uppercase() ?: "" },
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .padding(top = totalTopPadding + 16.dp, bottom = 80.dp, end = 2.dp)
            )
        }
    }
}
