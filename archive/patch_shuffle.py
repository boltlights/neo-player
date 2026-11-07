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
            
            if \(currentTrack != null\) \{
                val newIndex = _currentPlaybackList\.value\.indexOf\(currentTrack\)
                if \(newIndex != -1\) \{
                    player\.seekTo\(newIndex, player\.currentPosition\)
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

        // Group tracks by artist and randomize each artist's tracks internally
        val byArtist = remaining.groupBy { it.artist }
            .mapValues { it.value.shuffled() }

        val totalRemaining = remaining.size
        val result = arrayOfNulls<TrackEntity>(totalRemaining)

        // Sort artists by number of tracks descending, to place the most frequent artists first
        val sortedArtists = byArtist.entries.sortedByDescending { it.value.size }

        for ((_, artistTracks) in sortedArtists) {
            val spacing = totalRemaining.toDouble() / artistTracks.size
            // Start with a random offset within the spacing window to prevent identical shuffles
            var offset = kotlin.random.Random.nextDouble(spacing)

            for (track in artistTracks) {
                var targetIndex = offset.toInt() % totalRemaining
                // Find next available slot using linear probing
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
                // Apply our smart/fair shuffle algorithm instead of a naive random shuffle
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
            
            if (currentTrack != null) {
                val newIndex = _currentPlaybackList.value.indexOf(currentTrack)
                if (newIndex != -1) {
                    player.seekTo(newIndex, player.currentPosition)
                }
            }
        }
    }'''

content = re.sub(target, repl, content)

with open('app/src/main/java/com/example/PlaybackController.kt', 'w') as f:
    f.write(content)
