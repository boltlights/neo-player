with open('app/src/main/java/com/example/PlaybackController.kt', 'r') as f:
    content = f.read()

if 'val queueMutex = kotlinx.coroutines.sync.Mutex()' not in content:
    content = content.replace(
        'class PlaybackController(private val context: Context) {',
        'class PlaybackController(private val context: Context) {\n    private val queueMutex = kotlinx.coroutines.sync.Mutex()'
    )

with open('app/src/main/java/com/example/PlaybackController.kt', 'w') as f:
    f.write(content)
