import re

with open('app/src/main/java/com/example/PlaybackController.kt', 'r') as f:
    content = f.read()

old_code = """    fun setPlaybackList(list: List<TrackEntity>, startTrack: TrackEntity? = null) {
        _currentPlaybackList.value = list
        exoPlayer?.apply {
            setShuffleOrder(com.example.util.BetterShuffleOrder(list.size))
            setMediaItems(getMediaItemsFromList(list))
            prepare()
            if (startTrack != null) {
                val index = list.indexOfFirst { it.id == startTrack.id }
                if (index != -1) {
                    seekTo(index, 0L)
                }
            } else if (list.isNotEmpty()) {
                seekTo(0, 0L)
            }
            this@PlaybackController.smoothPlay()
        }
    }"""

new_code = """    fun setPlaybackList(list: List<TrackEntity>, startTrack: TrackEntity? = null) {
        _currentPlaybackList.value = list
        try {
            exoPlayer?.apply {
                setShuffleOrder(com.example.util.BetterShuffleOrder(list.size))
                setMediaItems(getMediaItemsFromList(list))
                prepare()
                if (startTrack != null) {
                    val index = list.indexOfFirst { it.id == startTrack.id }
                    if (index != -1) {
                        seekTo(index, 0L)
                    }
                } else if (list.isNotEmpty()) {
                    seekTo(0, 0L)
                }
                this@PlaybackController.smoothPlay()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            android.widget.Toast.makeText(context, "Error playing track: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
        }
    }"""

content = content.replace(old_code, new_code)

old_play = """    fun playTrack(track: TrackEntity) {
        val currentList = _currentPlaybackList.value
        val index = currentList.indexOfFirst { it.id == track.id }
        if (index != -1) {
            exoPlayer?.apply {
                seekTo(index, 0L)
            }
            smoothPlay()
        } else {
            // Track not in queue? Play as single item or set as new queue
            setPlaybackList(listOf(track), track)
        }
    }"""

new_play = """    fun playTrack(track: TrackEntity) {
        try {
            val currentList = _currentPlaybackList.value
            val index = currentList.indexOfFirst { it.id == track.id }
            if (index != -1) {
                exoPlayer?.apply {
                    seekTo(index, 0L)
                }
                smoothPlay()
            } else {
                // Track not in queue? Play as single item or set as new queue
                setPlaybackList(listOf(track), track)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            android.widget.Toast.makeText(context, "Error playing track: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
        }
    }"""

content = content.replace(old_play, new_play)

with open('app/src/main/java/com/example/PlaybackController.kt', 'w') as f:
    f.write(content)
