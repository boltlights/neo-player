import re
with open('app/src/main/java/com/example/PlaybackController.kt', 'r') as f:
    content = f.read()

if 'fun getBasePlaybackList()' not in content:
    target = r'''    fun setPlaybackList\(list: List<TrackEntity>, activeTrack: TrackEntity\?\) \{'''
    repl = r'''    fun getBasePlaybackList(): List<TrackEntity> = basePlaybackList
    
    fun setPlaybackList(list: List<TrackEntity>, activeTrack: TrackEntity?) {'''
    content = re.sub(target, repl, content)

with open('app/src/main/java/com/example/PlaybackController.kt', 'w') as f:
    f.write(content)
