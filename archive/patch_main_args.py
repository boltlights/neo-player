import re

with open('app/src/main/java/com/example/MainActivity.kt', 'r') as f:
    content = f.read()

content = re.sub(
r'''                    if \(currentTrack != null\) \{
                        ActiveBottomBar\(
                            track = currentTrack!!,
                            isPlaying = isPlaying,
                            accentColor = accentColor,
                            surfaceColor = surfaceColor,
                            onBgColor = onBgColor,
                            onPlayPauseToggle = \{ viewModel\.playbackController\.togglePlayPause\(\) \},
                            onOpenPlayer = \{ viewModel\.setPlayerExpanded\(true\) \}
                        \)
                    \}''',
r'''                    if (currentTrack != null) {
                        val playbackProgress by viewModel.playbackController.playbackProgress.collectAsStateWithLifecycle()
                        val progressVal = if (playbackProgress.duration <= 0 || playbackProgress.isBuffering) {
                            0f
                        } else {
                            (playbackProgress.position.toFloat() / playbackProgress.duration.toFloat()).coerceIn(0f, 1f)
                        }
                        
                        ActiveBottomBar(
                            track = currentTrack!!,
                            isPlaying = isPlaying,
                            progressVal = progressVal,
                            accentColor = accentColor,
                            surfaceColor = surfaceColor,
                            onBgColor = onBgColor,
                            onSkipPrevious = { viewModel.playbackController.playPrevious() },
                            onPlayPauseToggle = { viewModel.playbackController.togglePlayPause() },
                            onSkipNext = { viewModel.playbackController.playNext() },
                            onOpenPlayer = { viewModel.setPlayerExpanded(true) }
                        )
                    }''', content)

with open('app/src/main/java/com/example/MainActivity.kt', 'w') as f:
    f.write(content)
