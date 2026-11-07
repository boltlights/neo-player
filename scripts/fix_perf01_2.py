with open('app/src/main/java/com/example/util/MediaStoreManager.kt', 'r') as f:
    content = f.read()

content = content.replace(
    'val currentTrackList = activeExistingTracks.toMutableList()',
    'val currentTrackList = activeExistingTracks.toMutableList()\n                val trackMatcher = TrackMatcher(currentTrackList)'
)

with open('app/src/main/java/com/example/util/MediaStoreManager.kt', 'w') as f:
    f.write(content)
