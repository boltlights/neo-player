import re
with open('app/src/main/java/com/example/PlaybackController.kt', 'r') as f:
    content = f.read()

target = r'''    fun restorePlaybackListAndTrack\(list: List<TrackEntity>, activeTrack: TrackEntity\?, positionMs: Int, isShuffleEnabled: Boolean = false\) \{
        ensurePlayer\(\)
        basePlaybackList = list
        _currentPlaybackList\.value = list
        _isShuffle\.value = isShuffleEnabled
        
        exoPlayer\?\.apply \{
            setMediaItems\(getMediaItemsFromList\(list\)\)
            prepare\(\)

            if \(activeTrack != null\) \{
                val index = list\.indexOfFirst \{ it\.id == activeTrack\.id \}
                if \(index != -1\) \{
                    seekTo\(index, positionMs\.toLong\(\)\)
                \}
            \}
        \}
    \}'''

repl = r'''    fun restorePlaybackListAndTrack(list: List<TrackEntity>, baseList: List<TrackEntity>, activeTrack: TrackEntity?, positionMs: Int, isShuffleEnabled: Boolean = false, isRepeatEnabled: Boolean = false) {
        ensurePlayer()
        basePlaybackList = baseList
        _currentPlaybackList.value = list
        _isShuffle.value = isShuffleEnabled
        _isRepeat.value = isRepeatEnabled
        
        exoPlayer?.apply {
            setMediaItems(getMediaItemsFromList(list))
            repeatMode = if (isRepeatEnabled) androidx.media3.common.Player.REPEAT_MODE_ALL else androidx.media3.common.Player.REPEAT_MODE_OFF
            prepare()

            if (activeTrack != null) {
                val index = list.indexOfFirst { it.id == activeTrack.id }
                if (index != -1) {
                    seekTo(index, positionMs.toLong())
                }
            }
        }
    }'''

content = re.sub(target, repl, content)

with open('app/src/main/java/com/example/PlaybackController.kt', 'w') as f:
    f.write(content)
