import re

with open('app/src/main/java/com/example/PlaybackController.kt', 'r') as f:
    content = f.read()

content = content.replace(
'''    init {
        initPlayer()
    }''',
'''    init {
        // Player initialized lazily
    }

    fun ensurePlayer() {
        if (exoPlayer == null) {
            initPlayer()
        }
    }'''
)

methods = [
    r'(fun setPlaybackList\([^)]*\)\s*\{)',
    r'(fun playTrack\([^)]*\)\s*\{)',
    r'(fun addToQueue\([^)]*\)\s*\{)',
    r'(fun playTracksNext\([^)]*\)\s*\{)',
    r'(fun playTrackNext\([^)]*\)\s*\{)',
    r'(fun playNext\(\)\s*\{)',
    r'(fun playPrevious\(\)\s*\{)',
    r'(fun seekTo\([^)]*\)\s*\{)',
    r'(fun togglePlayPause\(\)\s*\{)',
    r'(fun toggleShuffle\(\)\s*\{)',
    r'(fun toggleRepeat\(\)\s*\{)',
    r'(fun moveQueueItem\([^)]*\)\s*\{)',
    r'(fun removeTrackFromQueue\([^)]*\)\s*\{)',
    r'(fun restorePlaybackListAndTrack\([^)]*\)\s*\{)'
]

for m in methods:
    content = re.sub(m, r'\1\n        ensurePlayer()', content)

with open('app/src/main/java/com/example/PlaybackController.kt', 'w') as f:
    f.write(content)
