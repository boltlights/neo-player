with open('app/src/main/java/com/example/PlaybackController.kt', 'r') as f:
    content = f.read()

content = content.replace(
'''    init {
        initPlayer()
        startPositionTracker()
    }''',
'''    init {
        startPositionTracker()
    }
    
    private fun ensurePlayer() {
        if (exoPlayer == null) {
            initPlayer()
        }
    }'''
)

with open('app/src/main/java/com/example/PlaybackController.kt', 'w') as f:
    f.write(content)
