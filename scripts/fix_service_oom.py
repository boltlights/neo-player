with open('app/src/main/java/com/example/PlaybackService.kt', 'r') as f:
    content = f.read()

replacement = """                            val tracks = app.repository.allTracks.first()
                            val fromIndex = page * pageSize
                            val toIndex = minOf(fromIndex + pageSize.toLong(), tracks.size.toLong()).toInt()
                            val pagedTracks = if (fromIndex < tracks.size) tracks.subList(fromIndex, toIndex) else emptyList()
                            val children = mutableListOf<MediaItem>()
                            pagedTracks.forEach { track ->"""

content = content.replace(
    '                            val tracks = app.repository.allTracks.first()\n                            val children = mutableListOf<MediaItem>()\n                            tracks.forEach { track ->',
    replacement
)

with open('app/src/main/java/com/example/PlaybackService.kt', 'w') as f:
    f.write(content)
