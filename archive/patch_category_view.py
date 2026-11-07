import re

with open('app/src/main/java/com/example/ui/screens/LibraryCategoryDetailView.kt', 'r') as f:
    content = f.read()

content = content.replace(
'''        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(tracksList) { track ->
                TrackRowItem(
                    track = track,
                    isPlaying = isPlaying,
                    isActive = currentTrack?.id == track.id,
                    accentColor = accentColor,
                    libraryAlbumArtStyle = libraryAlbumArtStyle,
                    onPlay = { onTrackPlay(track) },
                    onAddToPlaylist = { onTrackOptionClick(track) },
                    onPlayNext = { onPlayNext(track) },
                    onAddToQueue = { onAddToQueue(track) },
                    onDelete = { onDeleteTrack(track) },
                    onShowProperties = { onShowProperties(track) }
                )
            }
        }''',
'''        // Check if there are multiple discs
        val distinctDiscs = tracksList.mapNotNull { it.discNumber }.distinct().sorted()
        val showDiscHeaders = distinctDiscs.size > 1

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            if (showDiscHeaders) {
                val grouped = tracksList.groupBy { it.discNumber ?: 1 }.toSortedMap()
                grouped.forEach { (discNum, discTracks) ->
                    item(key = "disc_header_$discNum") {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f),
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Album,
                                    contentDescription = null,
                                    tint = accentColor,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Disc $discNum",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                    color = accentColor,
                                    letterSpacing = 1.sp
                                )
                            }
                        }
                    }
                    items(discTracks) { track ->
                        TrackRowItem(
                            track = track,
                            isPlaying = isPlaying,
                            isActive = currentTrack?.id == track.id,
                            accentColor = accentColor,
                            libraryAlbumArtStyle = libraryAlbumArtStyle,
                            onPlay = { onTrackPlay(track) },
                            onAddToPlaylist = { onTrackOptionClick(track) },
                            onPlayNext = { onPlayNext(track) },
                            onAddToQueue = { onAddToQueue(track) },
                            onDelete = { onDeleteTrack(track) },
                            onShowProperties = { onShowProperties(track) }
                        )
                    }
                }
            } else {
                items(tracksList) { track ->
                    TrackRowItem(
                        track = track,
                        isPlaying = isPlaying,
                        isActive = currentTrack?.id == track.id,
                        accentColor = accentColor,
                        libraryAlbumArtStyle = libraryAlbumArtStyle,
                        onPlay = { onTrackPlay(track) },
                        onAddToPlaylist = { onTrackOptionClick(track) },
                        onPlayNext = { onPlayNext(track) },
                        onAddToQueue = { onAddToQueue(track) },
                        onDelete = { onDeleteTrack(track) },
                        onShowProperties = { onShowProperties(track) }
                    )
                }
            }
        }'''
)

with open('app/src/main/java/com/example/ui/screens/LibraryCategoryDetailView.kt', 'w') as f:
    f.write(content)
