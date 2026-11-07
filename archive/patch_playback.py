import re

with open('app/src/main/java/com/example/PlaybackController.kt', 'r') as f:
    content = f.read()

# Add _basePlaybackList
target_list = r'private val _currentPlaybackList = MutableStateFlow<List<TrackEntity>>\(emptyList\(\)\)'
repl_list = r'''private var _basePlaybackList = emptyList<TrackEntity>()
    private val _currentPlaybackList = MutableStateFlow<List<TrackEntity>>(emptyList())'''
content = re.sub(target_list, repl_list, content)

# update usages of _currentPlaybackList.value to _basePlaybackList where queue is mutated
# Wait, this requires careful patching. Let's do it manually.
