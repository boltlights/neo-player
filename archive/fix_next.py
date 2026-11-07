import re

with open('app/src/main/java/com/example/PlaybackController.kt', 'r') as f:
    content = f.read()

target_play_tracks = r'''    fun playTracksNext\(tracks: List<TrackEntity>\) \{.*?    fun playTrackNext\(track: TrackEntity\) \{'''

repl_play_tracks = r'''    fun playTracksNext(tracks: List<TrackEntity>) {
        ensurePlayer()
        if (tracks.isEmpty()) return
        val current = _currentTrack.value
        val currentList = _currentPlaybackList.value.toMutableList()
        val baseList = basePlaybackList.toMutableList()
        
        if (current == null) {
            for (track in tracks) {
                if (!currentList.contains(track)) {
                    currentList.add(track)
                    if (!baseList.contains(track)) baseList.add(track)
                }
            }
            _currentPlaybackList.value = currentList
            basePlaybackList = baseList
            
            exoPlayer?.setMediaItems(getMediaItemsFromList(currentList))
            exoPlayer?.prepare()
            smoothPlay()
        } else {
            val filteredTracks = mutableListOf<TrackEntity>()
            for (track in tracks) {
                if (track.id == current.id) continue
                filteredTracks.add(track)
            }
            
            for (track in filteredTracks) {
                val oldIndex = currentList.indexOfFirst { it.id == track.id }
                if (oldIndex != -1) {
                    exoPlayer?.removeMediaItem(oldIndex)
                    currentList.removeAt(oldIndex)
                }
                
                val oldBase = baseList.indexOfFirst { it.id == track.id }
                if (oldBase != -1) baseList.removeAt(oldBase)
            }
            
            val currentIndex = exoPlayer?.currentMediaItemIndex ?: -1
            val mediaItemsToAdd = filteredTracks.map { createMediaItem(it) }
            
            if (currentIndex != -1) {
                currentList.addAll(currentIndex + 1, filteredTracks)
                exoPlayer?.addMediaItems(currentIndex + 1, mediaItemsToAdd)
            } else {
                currentList.addAll(0, filteredTracks)
                exoPlayer?.addMediaItems(0, mediaItemsToAdd)
            }
            _currentPlaybackList.value = currentList
            
            val baseIndex = baseList.indexOfFirst { it.id == current.id }
            if (baseIndex != -1) {
                baseList.addAll(baseIndex + 1, filteredTracks)
            } else {
                baseList.addAll(filteredTracks)
            }
            basePlaybackList = baseList
        }
    }

    fun playTrackNext(track: TrackEntity) {'''

content = re.sub(target_play_tracks, repl_play_tracks, content, flags=re.DOTALL)

target_play_track = r'''    fun playTrackNext\(track: TrackEntity\) \{.*?    fun removeTrackFromQueue\(trackId: Int\) \{'''

repl_play_track = r'''    fun playTrackNext(track: TrackEntity) {
        ensurePlayer()
        val current = _currentTrack.value
        if (current?.id == track.id) {
            return
        }
        val currentList = _currentPlaybackList.value.toMutableList()
        val baseList = basePlaybackList.toMutableList()
        val mediaItem = createMediaItem(track)

        if (current == null) {
            if (!currentList.contains(track)) {
                currentList.add(track)
                if (!baseList.contains(track)) baseList.add(track)
            }
            _currentPlaybackList.value = currentList
            basePlaybackList = baseList
            
            exoPlayer?.setMediaItems(getMediaItemsFromList(currentList))
            exoPlayer?.prepare()
            smoothPlay()
        } else {
            val oldIndex = currentList.indexOfFirst { it.id == track.id }
            if (oldIndex != -1) {
                exoPlayer?.removeMediaItem(oldIndex)
                currentList.removeAt(oldIndex)
            }
            val oldBase = baseList.indexOfFirst { it.id == track.id }
            if (oldBase != -1) baseList.removeAt(oldBase)
            
            val currentIndex = exoPlayer?.currentMediaItemIndex ?: -1
            if (currentIndex != -1) {
                currentList.add(currentIndex + 1, track)
                exoPlayer?.addMediaItem(currentIndex + 1, mediaItem)
            } else {
                currentList.add(0, track)
                exoPlayer?.addMediaItem(0, mediaItem)
            }
            _currentPlaybackList.value = currentList
            
            val baseIndex = baseList.indexOfFirst { it.id == current.id }
            if (baseIndex != -1) {
                baseList.add(baseIndex + 1, track)
            } else {
                baseList.add(track)
            }
            basePlaybackList = baseList
        }
    }

    fun removeTrackFromQueue(trackId: Int) {'''

content = re.sub(target_play_track, repl_play_track, content, flags=re.DOTALL)

with open('app/src/main/java/com/example/PlaybackController.kt', 'w') as f:
    f.write(content)

