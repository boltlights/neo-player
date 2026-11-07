import re

with open('app/src/main/java/com/example/PlaybackController.kt', 'r') as f:
    content = f.read()

old_code = """    fun setPlaybackList(list: List<TrackEntity>, startTrack: TrackEntity? = null) {
        _currentPlaybackList.value = list
        try {
            exoPlayer?.apply {
                setShuffleOrder(com.example.util.BetterShuffleOrder(list.size))
                setMediaItems(getMediaItemsFromList(list))
                prepare()"""

new_code = """    fun setPlaybackList(list: List<TrackEntity>, startTrack: TrackEntity? = null) {
        _currentPlaybackList.value = list
        try {
            exoPlayer?.apply {
                setMediaItems(getMediaItemsFromList(list))
                prepare()"""

content = content.replace(old_code, new_code)

with open('app/src/main/java/com/example/PlaybackController.kt', 'w') as f:
    f.write(content)
