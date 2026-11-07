import re

with open('app/src/main/java/com/example/MusicViewModel.kt', 'r') as f:
    content = f.read()

old_lyrics = """    fun fetchLyricsIfMissing(track: TrackEntity) {
        if (!track.lyrics.isNullOrBlank()) return
        if (track.isMetadataPending) return"""

new_lyrics = """    fun fetchLyricsIfMissing(track: TrackEntity) {
        if (track.lyrics != null) return // If it's "" (empty string), we already checked and found no lyrics
        if (track.isMetadataPending) return"""

content = content.replace(old_lyrics, new_lyrics)

old_update = """                if (!extracted.lyrics.isNullOrBlank()) {
                    val updatedTrack = track.copy(lyrics = extracted.lyrics)
                    repository.updateTrack(updatedTrack)
                    // If it is the current track, playback controller might need it updated?
                    // The Flow will automatically emit the updated track!
                }"""

new_update = """                val lyricsToSave = extracted.lyrics ?: ""
                val updatedTrack = track.copy(lyrics = lyricsToSave)
                repository.updateTrack(updatedTrack)
                // If it is the current track, playback controller might need it updated?
                // The Flow will automatically emit the updated track!"""

content = content.replace(old_update, new_update)

with open('app/src/main/java/com/example/MusicViewModel.kt', 'w') as f:
    f.write(content)
