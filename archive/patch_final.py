import re
with open('app/src/main/java/com/example/PlaybackController.kt', 'r') as f:
    content = f.read()

target = r'''    fun toggleShuffle\(\) \{
        ensurePlayer\(\)
        val isShuffled = !_isShuffle\.value
        _isShuffle\.value = isShuffled
        
        exoPlayer\?\.let \{ player ->
            val currentTrack = _currentTrack\.value
            if \(isShuffled\) \{
                basePlaybackList = _currentPlaybackList\.value
                val shuffled = basePlaybackList\.toMutableList\(\)
                shuffled\.shuffle\(\)
                if \(currentTrack != null\) \{
                    shuffled\.remove\(currentTrack\)
                    shuffled\.add\(0, currentTrack\)
                \}
                _currentPlaybackList\.value = shuffled
                player\.setMediaItems\(getMediaItemsFromList\(shuffled\), false\)
            \} else \{
                val restored = basePlaybackList
                if \(restored\.isNotEmpty\(\)\) \{
                    _currentPlaybackList\.value = restored
                    player\.setMediaItems\(getMediaItemsFromList\(restored\), false\)
                \}
            \}
        \}
    \}'''

repl = r'''    private fun fairShuffle(tracks: List<TrackEntity>, firstTrack: TrackEntity? = null): List<TrackEntity> {
        if (tracks.isEmpty()) return emptyList()
        val remaining = tracks.toMutableList()
        firstTrack?.let { remaining.remove(it) }

        if (remaining.isEmpty()) {
            return firstTrack?.let { listOf(it) } ?: emptyList()
        }

        val byArtist = remaining.groupBy { it.artist }
            .mapValues { it.value.shuffled() }

        val totalRemaining = remaining.size
        val result = arrayOfNulls<TrackEntity>(totalRemaining)

        val sortedArtists = byArtist.entries.sortedByDescending { it.value.size }

        for ((_, artistTracks) in sortedArtists) {
            val spacing = totalRemaining.toDouble() / artistTracks.size
            var offset = kotlin.random.Random.nextDouble(spacing)

            for (track in artistTracks) {
                var targetIndex = offset.toInt() % totalRemaining
                while (result[targetIndex] != null) {
                    targetIndex = (targetIndex + 1) % totalRemaining
                }
                result[targetIndex] = track
                offset += spacing
            }
        }

        val finalList = result.filterNotNull().toMutableList()
        firstTrack?.let { finalList.add(0, it) }
        return finalList
    }

    fun toggleShuffle() {
        ensurePlayer()
        val isShuffled = !_isShuffle.value
        _isShuffle.value = isShuffled
        
        exoPlayer?.let { player ->
            val currentTrack = _currentTrack.value
            if (isShuffled) {
                basePlaybackList = _currentPlaybackList.value
                val shuffled = fairShuffle(basePlaybackList, currentTrack)
                _currentPlaybackList.value = shuffled
                player.setMediaItems(getMediaItemsFromList(shuffled), false)
            } else {
                val restored = basePlaybackList
                if (restored.isNotEmpty()) {
                    _currentPlaybackList.value = restored
                    player.setMediaItems(getMediaItemsFromList(restored), false)
                }
            }
        }
    }'''

content = re.sub(target, repl, content)

with open('app/src/main/java/com/example/PlaybackController.kt', 'w') as f:
    f.write(content)
