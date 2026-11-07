with open('app/src/main/java/com/example/ui/screens/FocusedNowPlayingScreen.kt', 'r') as f:
    content = f.read()

content = content.replace(
    'allTracks.find { it.id == updatedTrack.id } ?: track',
    'allTracks.find { it.id == track.id } ?: track'
)

with open('app/src/main/java/com/example/ui/screens/FocusedNowPlayingScreen.kt', 'w') as f:
    f.write(content)
