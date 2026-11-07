with open('app/src/main/java/com/example/MusicViewModel.kt', 'r') as f:
    content = f.read()

old_restore = '''                if (ids.isNotEmpty()) {
                    val trackList = mutableListOf<TrackEntity>()
                    for (id in ids) {
                        val track = repository.getTrackById(id)
                        if (track != null) {
                            trackList.add(track)
                        }
                    }'''

new_restore = '''                if (ids.isNotEmpty()) {
                    val trackMap = repository.getTracksByIds(ids).associateBy { it.id }
                    val trackList = ids.mapNotNull { trackMap[it] }.toMutableList()'''

content = content.replace(old_restore, new_restore)

with open('app/src/main/java/com/example/MusicViewModel.kt', 'w') as f:
    f.write(content)
