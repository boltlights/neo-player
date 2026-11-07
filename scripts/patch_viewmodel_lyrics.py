import re

with open('app/src/main/java/com/example/MusicViewModel.kt', 'r') as f:
    content = f.read()

lyrics_func = """    fun fetchLyricsIfMissing(track: TrackEntity) {
        if (!track.lyrics.isNullOrBlank()) return
        if (track.isMetadataPending) return
        
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val extracted = MetadataExtractor.extract(context, track.sourceUri ?: track.filePath, track.filePath)
                if (!extracted.lyrics.isNullOrBlank()) {
                    val updatedTrack = track.copy(lyrics = extracted.lyrics)
                    repository.updateTrack(updatedTrack)
                    // If it is the current track, playback controller might need it updated?
                    // The Flow will automatically emit the updated track!
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }"""

content = content.replace(
    '    fun deletePlaylist(playlistId: Int) {',
    lyrics_func + '\n\n    fun deletePlaylist(playlistId: Int) {'
)

with open('app/src/main/java/com/example/MusicViewModel.kt', 'w') as f:
    f.write(content)
