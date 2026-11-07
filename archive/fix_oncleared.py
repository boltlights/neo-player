import re
with open('app/src/main/java/com/example/MusicViewModel.kt', 'r') as f:
    content = f.read()

target = r'''    override fun onCleared\(\) \{
        super\.onCleared\(\)
    \}'''

repl = r'''    override fun onCleared() {
        if (isRestoreCompleted) {
            val queue = playbackController.currentPlaybackList.value
            val baseQueue = playbackController.getBasePlaybackList()
            val track = playbackController.currentTrack.value
            val currentTrackId = track?.id ?: -1
            val pos = playbackController.currentPosition.value
            val isShuffle = playbackController.isShuffle.value
            val isRepeat = playbackController.isRepeat.value
            
            val queueStr = queue.joinToString(",") { it.id.toString() }
            val baseQueueStr = baseQueue.joinToString(",") { it.id.toString() }
            
            kotlinx.coroutines.GlobalScope.launch {
                repository.savePlaybackState(
                    com.example.data.PlaybackStateEntity(
                        id = 0,
                        baseQueueTrackIds = baseQueueStr,
                        queueTrackIds = queueStr,
                        currentTrackId = currentTrackId,
                        currentPositionMs = pos,
                        isShuffleEnabled = isShuffle,
                        isRepeatEnabled = isRepeat
                    )
                )
            }
        }
        super.onCleared()
    }'''

if "fun onCleared" in content:
    content = re.sub(target, repl, content)
else:
    # insert at the end of class before last }
    content = re.sub(r'\}\s*$', repl + '\n}', content)

with open('app/src/main/java/com/example/MusicViewModel.kt', 'w') as f:
    f.write(content)
