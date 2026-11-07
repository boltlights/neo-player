with open('app/src/main/java/com/example/PlaybackController.kt', 'r') as f:
    content = f.read()

content = content.replace(
'''    init {
        initPlayer()
    }''',
'''    init {
        // Player initialized lazily on first intent
    }

    private fun ensurePlayer() {
        if (exoPlayer == null) {
            initPlayer()
        }
    }'''
)

# Add ensurePlayer() to public methods
methods_to_patch = [
    'fun setPlaybackList',
    'fun playTrack',
    'fun addToQueue',
    'fun playTracksNext',
    'fun playTrackNext',
    'fun togglePlayPause',
    'fun playNext',
    'fun playPrevious',
    'fun seekTo',
    'fun toggleShuffle',
    'fun toggleRepeat',
    'fun moveQueueItem',
    'fun removeTrackFromQueue'
]

for method in methods_to_patch:
    content = content.replace(method + '(', method + '(\n        ensurePlayer()')

# Some methods don't have params
content = content.replace('fun togglePlayPause() {', 'fun togglePlayPause() {\n        ensurePlayer()')
content = content.replace('fun playNext() {', 'fun playNext() {\n        ensurePlayer()')
content = content.replace('fun playPrevious() {', 'fun playPrevious() {\n        ensurePlayer()')
content = content.replace('fun toggleShuffle() {', 'fun toggleShuffle() {\n        ensurePlayer()')
content = content.replace('fun toggleRepeat() {', 'fun toggleRepeat() {\n        ensurePlayer()')

# Actually, the replacement for methods with parameters was `method + '(\n        ensurePlayer()'` but some methods look like `fun setPlaybackList(list: List<TrackEntity>, startTrack: TrackEntity? = null) {`.
# The replace above will make it `fun setPlaybackList(
#        ensurePlayer()list: ...` which is a syntax error!

