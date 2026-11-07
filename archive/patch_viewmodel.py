import re
with open('app/src/main/java/com/example/MusicViewModel.kt', 'r') as f:
    content = f.read()

# Modify saveCurrentPlaybackState
target = r'''    private var lastSavedQueueHash = 0
    private var lastSavedQueueStr = ""
    private fun saveCurrentPlaybackState\(\) \{
        if \(!isRestoreCompleted\) return
        val queue = playbackController\.currentPlaybackList\.value
        val track = playbackController\.currentTrack\.value
        val currentTrackId = track\?\.id \?: -1
        val pos = playbackController\.currentPosition\.value
        val isShuffle = playbackController\.isShuffle\.value

        val queueHash = queue\.hashCode\(\)
        if \(queueHash != lastSavedQueueHash\) \{
            lastSavedQueueStr = queue\.joinToString\(","\) \{ it\.id\.toString\(\) \}
            lastSavedQueueHash = queueHash
            viewModelScope\.launch \{
                repository\.savePlaybackState\(
                    PlaybackStateEntity\(
                        id = 0,
                        queueTrackIds = lastSavedQueueStr,
                        currentTrackId = currentTrackId,
                        currentPositionMs = pos,
                        isShuffleEnabled = isShuffle
                    \)
                \)
            \}
        \} else \{
            viewModelScope\.launch \{
                repository\.updatePlaybackPosition\(pos, currentTrackId, isShuffle\)
            \}
        \}
    \}'''

repl = r'''    private var lastSavedQueueHash = 0
    private var lastSavedQueueStr = ""
    private var lastSavedBaseQueueStr = ""
    private var lastSavedRepeat = false
    private fun saveCurrentPlaybackState() {
        if (!isRestoreCompleted) return
        val queue = playbackController.currentPlaybackList.value
        val baseQueue = playbackController.getBasePlaybackList()
        val track = playbackController.currentTrack.value
        val currentTrackId = track?.id ?: -1
        val pos = playbackController.currentPosition.value
        val isShuffle = playbackController.isShuffle.value
        val isRepeat = playbackController.isRepeat.value

        val queueHash = queue.hashCode()
        if (queueHash != lastSavedQueueHash || isRepeat != lastSavedRepeat) {
            lastSavedQueueStr = queue.joinToString(",") { it.id.toString() }
            lastSavedBaseQueueStr = baseQueue.joinToString(",") { it.id.toString() }
            lastSavedQueueHash = queueHash
            lastSavedRepeat = isRepeat
            viewModelScope.launch {
                repository.savePlaybackState(
                    PlaybackStateEntity(
                        id = 0,
                        baseQueueTrackIds = lastSavedBaseQueueStr,
                        queueTrackIds = lastSavedQueueStr,
                        currentTrackId = currentTrackId,
                        currentPositionMs = pos,
                        isShuffleEnabled = isShuffle,
                        isRepeatEnabled = isRepeat
                    )
                )
            }
        } else {
            viewModelScope.launch {
                repository.updatePlaybackPosition(pos, currentTrackId, isShuffle)
            }
        }
    }'''

content = re.sub(target, repl, content)

# Modify restoreQueueState
target_restore = r'''    private suspend fun restoreQueueState\(\) \{
        val playbackState = repository\.getPlaybackState\(\)
        if \(playbackState != null\) \{
            val queueIdsStr = playbackState\.queueTrackIds
            val currentTrackId = playbackState\.currentTrackId
            val savedPos = playbackState\.currentPositionMs
            val isShuffleEnabled = playbackState\.isShuffleEnabled

            if \(queueIdsStr\.isNotEmpty\(\)\) \{
                val ids = queueIdsStr\.split\(","\)\.mapNotNull \{ it\.toIntOrNull\(\) \}
                if \(ids\.isNotEmpty\(\)\) \{
                    val trackMap = repository\.getTracksByIds\(ids\)\.associateBy \{ it\.id \}
                    val trackList = ids\.mapNotNull \{ trackMap\[it\] \}\.toMutableList\(\)
                    if \(trackList\.isNotEmpty\(\)\) \{
                        playbackController\.restorePlaybackListAndTrack\(
                            trackList,
                            trackList\.firstOrNull \{ it\.id == currentTrackId \},
                            savedPos,
                            isShuffleEnabled
                        \)
                    \}
                \}
            \}
        \}
        isRestoreCompleted = true
    \}'''

repl_restore = r'''    private suspend fun restoreQueueState() {
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
                            trackList,
                            baseTrackList,
                            trackList.firstOrNull { it.id == currentTrackId },
                            savedPos,
                            isShuffleEnabled,
                            isRepeatEnabled
                        )
                    }
                }
            }
        }
        isRestoreCompleted = true
    }'''

content = re.sub(target_restore, repl_restore, content)

with open('app/src/main/java/com/example/MusicViewModel.kt', 'w') as f:
    f.write(content)
