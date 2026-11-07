import re

with open('app/src/main/java/com/example/PlaybackController.kt', 'r') as f:
    content = f.read()

# Fix playbackProgress leak
content = content.replace(
'''    ).stateIn(
        scope = CoroutineScope(Dispatchers.Main + SupervisorJob()),
        started = SharingStarted.Lazily,
        initialValue = PlaybackProgress(0, 0, false)
    )''',
'''    ).stateIn(
        scope = playScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = PlaybackProgress(0, 0, false)
    )'''
)

# Fix onMediaItemTransition smoothPause
content = content.replace(
'''                            sleepTimerEndOfTrack = false
                            this@apply.pause()
                            _sleepTimerRemainingSeconds.value = null''',
'''                            sleepTimerEndOfTrack = false
                            this@PlaybackController.smoothPause()
                            _sleepTimerRemainingSeconds.value = null'''
)

# Fix startForegroundService exception
content = content.replace(
'''        try {
            val serviceIntent = Intent(context, PlaybackService::class.java)
            androidx.core.content.ContextCompat.startForegroundService(context, serviceIntent)
        } catch (e: Exception) {
            e.printStackTrace()
        }''',
'''        try {
            val serviceIntent = Intent(context, PlaybackService::class.java)
            androidx.core.content.ContextCompat.startForegroundService(context, serviceIntent)
        } catch (e: Exception) {
            android.util.Log.e("PlaybackController", "Failed to start foreground service", e)
        }'''
)

# Fix File.exists in createMediaItem
content = content.replace(
'''                            val file = java.io.File(realArt)
                            if (file.exists()) {
                                builder.setArtworkUri(android.net.Uri.fromFile(file))
                            }''',
'''                            val file = java.io.File(realArt)
                            builder.setArtworkUri(android.net.Uri.fromFile(file))'''
)

content = content.replace(
'''        val bestUri = if (track.filePath.isNotEmpty() && !track.filePath.startsWith("content://") && java.io.File(track.filePath).exists()) {
            track.filePath
        } else {
            track.sourceUri ?: track.filePath
        }''',
'''        val bestUri = track.sourceUri ?: track.filePath'''
)

content = content.replace(
'''        val bestUri = if (track.filePath.isNotEmpty() && !track.filePath.startsWith("content://") && java.io.File(track.filePath).exists()) {
            track.filePath
        } else if (!track.sourceUri.isNullOrEmpty()) {
            track.sourceUri
        } else {
            track.filePath
        }''',
'''        val bestUri = track.sourceUri ?: track.filePath'''
)


# Fix naming collision
content = content.replace('fun playNext(tracks: List<TrackEntity>) {', 'fun playTracksNext(tracks: List<TrackEntity>) {')
content = content.replace('fun playNext(track: TrackEntity) {', 'fun playTrackNext(track: TrackEntity) {')


with open('app/src/main/java/com/example/PlaybackController.kt', 'w') as f:
    f.write(content)
