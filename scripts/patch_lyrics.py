with open('app/src/main/java/com/example/ui/screens/FocusedNowPlayingScreen.kt', 'r') as f:
    content = f.read()

lyrics_view = """            } else if (showLyrics) {
                // Lyrics View
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .padding(vertical = 16.dp)
                ) {
                    Text(
                        "LYRICS",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = accentColor,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.Black.copy(alpha = 0.2f))
                            .padding(24.dp)
                    ) {
                        if (!track.lyrics.isNullOrBlank()) {
                            val scrollState = rememberScrollState()
                            Text(
                                text = track.lyrics,
                                color = Color.White,
                                fontSize = 16.sp,
                                lineHeight = 28.sp,
                                modifier = Modifier.verticalScroll(scrollState)
                            )
                        } else {
                            Text(
                                "No embedded lyrics found for this track",
                                color = onBgColor.copy(alpha = 0.6f),
                                fontSize = 16.sp,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.align(Alignment.Center)
                            )
                        }
                    }
                }
            } else {"""

content = content.replace(
    '            } else {\n                val currentPlaylist by viewModel.playbackController.currentPlaybackList.collectAsStateWithLifecycle()',
    lyrics_view + '\n                val currentPlaylist by viewModel.playbackController.currentPlaybackList.collectAsStateWithLifecycle()'
)

with open('app/src/main/java/com/example/ui/screens/FocusedNowPlayingScreen.kt', 'w') as f:
    f.write(content)
