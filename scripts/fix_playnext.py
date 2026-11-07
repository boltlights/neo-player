import re

with open('app/src/main/java/com/example/PlaybackController.kt', 'r') as f:
    content = f.read()

tracks_block = """        } else {
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
        }"""

content = re.sub(r'\} else \{\n\s*// Remove duplicates.*?\n\s*_currentPlaybackList.value = currentList\n\s*\}', tracks_block, content, flags=re.DOTALL)

track_block = """        } else {
            val oldIndex = currentList.indexOfFirst { it.id == track.id }
            if (oldIndex != -1) {
                exoPlayer?.removeMediaItem(oldIndex)
                currentList.removeAt(oldIndex)
            }
            
            val currentIndex = exoPlayer?.currentMediaItemIndex ?: -1
            
            if (currentIndex != -1) {
                currentList.add(currentIndex + 1, track)
                exoPlayer?.addMediaItem(currentIndex + 1, mediaItem)
            } else {
                currentList.add(0, track)
                exoPlayer?.addMediaItem(0, mediaItem)
            }
            _currentPlaybackList.value = currentList
        }"""

content = re.sub(r'\} else \{\n\s*// Find old index in the running playlist.*?\n\s*_currentPlaybackList.value = currentList\n\s*\}', track_block, content, flags=re.DOTALL)

with open('app/src/main/java/com/example/PlaybackController.kt', 'w') as f:
    f.write(content)
