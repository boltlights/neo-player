import re
with open('app/src/main/java/com/example/MusicViewModel.kt', 'r') as f:
    content = f.read()

target = r'    private suspend fun restoreQueueState\(\) \{.*?\n    \}'
repl = r'''    private suspend fun restoreQueueState() {
        val playbackState = repository.getPlaybackState()
        if (playbackState != null) {
            val baseQueueIdsStr = playbackState.baseQueueTrackIds
            val queueIdsStr = playbackState.queueTrackIds
            val currentTrackId = playbackState.currentTrackId
            val savedPos = playbackState.currentPositionMs
            val isShuffleEnabled = playbackState.isShuffleEnabled
            val isRepeatEnabled = playbackState.isRepeatEnabled

            if (queueIdsStr.isNotEmpty()) {
                val baseIds = if (baseQueueIdsStr.isNotEmpty()) baseQueueIdsStr.split(",").mapNotNull { it.toIntOrNull() } else emptyList()
                val ids = queueIdsStr.split(",").mapNotNull { it.toIntOrNull() }
                
                if (ids.isNotEmpty()) {
                    val allIds = (ids + baseIds).distinct()
                    val trackMap = repository.getTracksByIds(allIds).associateBy { it.id }
                    
                    val trackList = ids.mapNotNull { trackMap[it] }.toList()
                    val baseTrackList = if (baseIds.isNotEmpty()) baseIds.mapNotNull { trackMap[it] }.toList() else trackList
                    
                    if (trackList.isNotEmpty()) {
                        playbackController.restorePlaybackListAndTrack(
                            list = trackList,
                            baseList = baseTrackList,
                            activeTrack = trackList.firstOrNull { it.id == currentTrackId },
                            positionMs = savedPos,
                            isShuffleEnabled = isShuffleEnabled,
                            isRepeatEnabled = isRepeatEnabled
                        )
                    }
                }
            }
        }
        isRestoreCompleted = true
    }'''

content = re.sub(target, repl, content, flags=re.DOTALL)

with open('app/src/main/java/com/example/MusicViewModel.kt', 'w') as f:
    f.write(content)
