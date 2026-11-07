with open('app/src/main/java/com/example/TrackPropertiesModal.kt', 'r') as f:
    content = f.read()

content = content.replace(
"""    val durationSec = (track.durationMs / 1000f).roundToInt()
    val mins = durationSec / 60""",
"""    if (track.musicBrainzId != null) {
        props["MusicBrainz ID"] = track.musicBrainzId
    }
    if (track.lyrics != null) {
        props["Lyrics"] = if (track.lyrics.length > 50) track.lyrics.substring(0, 50) + "..." else track.lyrics
    }
    val durationSec = (track.durationMs / 1000f).roundToInt()
    val mins = durationSec / 60"""
)

with open('app/src/main/java/com/example/TrackPropertiesModal.kt', 'w') as f:
    f.write(content)
