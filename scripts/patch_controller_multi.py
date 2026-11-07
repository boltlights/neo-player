with open('app/src/main/java/com/example/PlaybackController.kt', 'r') as f:
    content = f.read()

add_to_queue_multi = """
    fun addToQueue(tracks: List<TrackEntity>) {
        if (tracks.isEmpty()) return
        val currentList = _currentPlaybackList.value.toMutableList()
        currentList.addAll(tracks)
        _currentPlaybackList.value = currentList
        
        val mediaItems = tracks.map { createMediaItem(it) }
        exoPlayer?.addMediaItems(mediaItems)
        
        if (_currentTrack.value == null) {
            exoPlayer?.prepare()
            smoothPlay()
        }
    }
"""

play_next_multi = """
    fun playNext(tracks: List<TrackEntity>) {
        if (tracks.isEmpty()) return
        val current = _currentTrack.value
        val currentList = _currentPlaybackList.value.toMutableList()
        val mediaItems = tracks.map { createMediaItem(it) }

        if (current == null) {
            for (track in tracks) {
                if (!currentList.contains(track)) {
                    currentList.add(track)
                }
            }
            _currentPlaybackList.value = currentList
            exoPlayer?.setMediaItems(getMediaItemsFromList(currentList))
            exoPlayer?.prepare()
            smoothPlay()
        } else {
            // Remove duplicates
            tracks.forEach { track ->
                val oldIndex = currentList.indexOfFirst { it.id == track.id }
                if (oldIndex != -1) {
                    exoPlayer?.removeMediaItem(oldIndex)
                    currentList.removeAt(oldIndex)
                }
            }
            val index = currentList.indexOfFirst { it.id == current.id }
            if (index != -1) {
                currentList.addAll(index + 1, tracks)
                exoPlayer?.addMediaItems(index + 1, mediaItems)
            } else {
                currentList.addAll(0, tracks)
                exoPlayer?.addMediaItems(0, mediaItems)
            }
            _currentPlaybackList.value = currentList
        }
    }
"""

content = content.replace(
    '    fun addToQueue(track: TrackEntity) {',
    add_to_queue_multi + '\n    fun addToQueue(track: TrackEntity) {'
)

content = content.replace(
    '    fun playNext(track: TrackEntity) {',
    play_next_multi + '\n    fun playNext(track: TrackEntity) {'
)

with open('app/src/main/java/com/example/PlaybackController.kt', 'w') as f:
    f.write(content)
