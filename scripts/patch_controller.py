with open('app/src/main/java/com/example/PlaybackController.kt', 'r') as f:
    content = f.read()

content = content.replace(
"""                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        _isPlaying.value = isPlaying
                    }""",
"""                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        _isPlaying.value = isPlaying
                        com.example.widget.MusicWidgetProvider.updateAllWidgets(context)
                    }"""
)

content = content.replace(
"""                    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                        super.onMediaItemTransition(mediaItem, reason)
                        _currentPosition.value = 0
                        _duration.value = 0
                        updateCurrentTrackState(mediaItem)
                        if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_SEEK) {
                            this@apply.play()
                        }
                    }""",
"""                    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                        super.onMediaItemTransition(mediaItem, reason)
                        _currentPosition.value = 0
                        _duration.value = 0
                        updateCurrentTrackState(mediaItem)
                        if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_SEEK) {
                            this@apply.play()
                        }
                        com.example.widget.MusicWidgetProvider.updateAllWidgets(context)
                    }"""
)

with open('app/src/main/java/com/example/PlaybackController.kt', 'w') as f:
    f.write(content)
