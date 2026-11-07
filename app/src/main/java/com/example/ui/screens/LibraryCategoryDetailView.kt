package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.filled.Album
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import com.example.LibraryAlbumArtStyle
import com.example.data.TrackEntity
import com.example.ui.components.TrackRowItem

@Composable
fun LibraryCategoryDetailView(
    title: String,
    subtitle: String,
    tracksList: List<TrackEntity>,
    currentTrack: TrackEntity?,
    isPlaying: Boolean,
    accentColor: Color,
    libraryAlbumArtStyle: LibraryAlbumArtStyle,
    onBack: () -> Unit,
    onTrackPlay: (TrackEntity) -> Unit,
    onTrackOptionClick: (TrackEntity) -> Unit,
    onPlayNext: (TrackEntity) -> Unit,
    onAddToQueue: (TrackEntity) -> Unit,
    onDeleteTrack: (TrackEntity) -> Unit,
    onShowProperties: (TrackEntity) -> Unit,
    onPlayArtist: (String) -> Unit = {},
    onPlayGenre: (String) -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = accentColor)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
            }
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 120.dp)
        ) {
            items(tracksList, key = { it.id }, contentType = { "track_item" }) { track ->
                TrackRowItem(
                    track = track,
                    isPlaying = isPlaying && currentTrack?.id == track.id,
                    isActive = currentTrack?.id == track.id,
                    accentColor = accentColor,
                    libraryAlbumArtStyle = libraryAlbumArtStyle,
                    onPlay = { onTrackPlay(track) },
                    onOptionsClick = { onTrackOptionClick(track) },
                    onPlayNext = { onPlayNext(track) }
                )
            }
        }
    }
}
