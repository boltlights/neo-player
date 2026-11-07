import re

with open('app/src/main/java/com/example/ui/screens/FocusedNowPlayingScreen.kt', 'r') as f:
    content = f.read()

track_obs = """    val allTracks by viewModel.allTracks.collectAsStateWithLifecycle()
    val updatedTrack = remember(track, allTracks) {
        allTracks.find { it.id == track.id } ?: track
    }
    
    val playbackProgress by viewModel.playbackController.playbackProgress.collectAsStateWithLifecycle()"""

content = content.replace(
    '    val playbackProgress by viewModel.playbackController.playbackProgress.collectAsStateWithLifecycle()',
    track_obs
)

content = content.replace('track.lyrics', 'updatedTrack.lyrics')
content = content.replace('track.id', 'updatedTrack.id')
content = content.replace('track.title', 'updatedTrack.title')
content = content.replace('track.artist', 'updatedTrack.artist')
content = content.replace('track.artPath', 'updatedTrack.artPath')

with open('app/src/main/java/com/example/ui/screens/FocusedNowPlayingScreen.kt', 'w') as f:
    f.write(content)
