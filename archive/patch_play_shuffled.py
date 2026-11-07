import re
with open('app/src/main/java/com/example/PlaybackController.kt', 'r') as f:
    content = f.read()

target = r'''    fun setPlaybackList\(list: List<TrackEntity>, startTrack: TrackEntity\? = null\) \{'''

repl = r'''    fun playShuffledList(list: List<TrackEntity>) {
        if (list.isEmpty()) return
        val randomFirstTrack = list.random()
        val shuffled = fairShuffle(list, randomFirstTrack)
        basePlaybackList = list
        _isShuffle.value = true
        setPlaybackList(shuffled, randomFirstTrack)
    }

    fun setPlaybackList(list: List<TrackEntity>, startTrack: TrackEntity? = null) {
        basePlaybackList = list'''

content = re.sub(target, repl, content)

with open('app/src/main/java/com/example/PlaybackController.kt', 'w') as f:
    f.write(content)
