with open('app/src/main/java/com/example/util/MediaStoreManager.kt', 'r') as f:
    content = f.read()

helper = """
    class TrackMatcher(existingTracks: List<TrackEntity>) {
        private val uriMap = HashMap<String, TrackEntity>()
        private val pathMap = HashMap<String, TrackEntity>()
        private val metadataMap = HashMap<String, TrackEntity>()

        init {
            for (track in existingTracks) {
                if (!track.sourceUri.isNullOrEmpty()) {
                    uriMap[track.sourceUri] = track
                }
                if (track.filePath.isNotEmpty()) {
                    pathMap[track.filePath] = track
                }
                val key = "${track.title.trim()}|${track.artist.trim()}|${track.album.trim()}"
                metadataMap[key] = track
            }
        }

        fun find(uriString: String, filePath: String?, title: String, artist: String, album: String): TrackEntity? {
            uriMap[uriString]?.let { return it }
            if (!filePath.isNullOrEmpty()) {
                pathMap[filePath]?.let { return it }
            }
            val key = "${title.trim()}|${artist.trim()}|${album.trim()}"
            metadataMap[key]?.let { return it }
            return null
        }
    }
"""

content = content.replace('    private fun findMatchingExistingTrack(', helper + '\n    private fun findMatchingExistingTrack(')

# Replace all calls. They look like:
# val matched = findMatchingExistingTrack(
#     currentTrackList, ... )
import re

# Need to inject `val matcher = TrackMatcher(currentTrackList)` at the start of loops.
# But it's easier to just replace `findMatchingExistingTrack` globally and inject `TrackMatcher` initialization.

content = content.replace(
    'val currentTrackList = repository.allTracks.first()',
    'val currentTrackList = repository.allTracks.first()\n                val trackMatcher = TrackMatcher(currentTrackList)'
)

# In importAudioFiles, the variables are slightly different but wait, both have `val currentTrackList = repository.allTracks.first()`.
content = content.replace(
    'val matched = findMatchingExistingTrack(\n                            currentTrackList,\n                            item.uriString,\n                            item.filePath,\n                            title,\n                            artist,\n                            album,\n                            duration\n                        )',
    'val matched = trackMatcher.find(item.uriString, item.filePath, title, artist, album)'
)

content = content.replace(
    'val matched = findMatchingExistingTrack(\n                                    currentTrackList,\n                                    uriString,\n                                    row.data,\n                                    title,\n                                    artist,\n                                    album,\n                                    duration\n                                )',
    'val matched = trackMatcher.find(uriString, row.data, title, artist, album)'
)

content = content.replace(
    'val matched = findMatchingExistingTrack(\n                        currentTrackList,\n                        item.uriString,\n                        null,\n                        title,\n                        artist,\n                        album,\n                        duration\n                    )',
    'val matched = trackMatcher.find(item.uriString, null, title, artist, album)'
)

with open('app/src/main/java/com/example/util/MediaStoreManager.kt', 'w') as f:
    f.write(content)
