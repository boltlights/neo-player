with open('app/src/main/java/com/example/util/MediaStoreManager.kt', 'r') as f:
    content = f.read()

helper_add = """
        fun add(track: TrackEntity) {
            if (!track.sourceUri.isNullOrEmpty()) {
                uriMap[track.sourceUri] = track
            }
            if (track.filePath.isNotEmpty()) {
                pathMap[track.filePath] = track
            }
            val key = "${track.title.trim()}|${track.artist.trim()}|${track.album.trim()}"
            metadataMap[key] = track
        }
"""

content = content.replace(
    'fun find(uriString: String, filePath: String?, title: String, artist: String, album: String): TrackEntity? {',
    helper_add + '\n        fun find(uriString: String, filePath: String?, title: String, artist: String, album: String): TrackEntity? {'
)

content = content.replace('currentTrackList.add(newTrack)', 'currentTrackList.add(newTrack)\n                                    trackMatcher.add(newTrack)')
# We need to make sure we don't break anything. Let's just do trackMatcher.add(newTrack).
content = content.replace('currentTrackList.add(track)', 'currentTrackList.add(track)\n                                    trackMatcher.add(track)')

with open('app/src/main/java/com/example/util/MediaStoreManager.kt', 'w') as f:
    f.write(content)
