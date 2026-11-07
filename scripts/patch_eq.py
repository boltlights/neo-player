with open('app/src/main/java/com/example/PlaybackController.kt', 'r') as f:
    content = f.read()

content = content.replace(
    'private var currentTrackIdForLogging: Int? = null',
    'private var currentTrackIdForLogging: Int? = null\n    private var currentAudioSessionId: Int = 0'
)

new_listener = """                    override fun onAudioSessionIdChanged(audioSessionId: Int) {
                        super.onAudioSessionIdChanged(audioSessionId)
                        if (currentAudioSessionId != 0 && currentAudioSessionId != audioSessionId) {
                            try {
                                val closeIntent = Intent(android.media.audiofx.AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION).apply {
                                    putExtra(android.media.audiofx.AudioEffect.EXTRA_AUDIO_SESSION, currentAudioSessionId)
                                    putExtra(android.media.audiofx.AudioEffect.EXTRA_PACKAGE_NAME, context.packageName)
                                }
                                context.sendBroadcast(closeIntent)
                            } catch (e: Exception) {
                                android.util.Log.e("PlaybackController", "Failed to broadcast close eq session", e)
                            }
                        }
                        
                        currentAudioSessionId = audioSessionId
                        
                        try {
                            val openIntent = Intent(android.media.audiofx.AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION).apply {
                                putExtra(android.media.audiofx.AudioEffect.EXTRA_AUDIO_SESSION, audioSessionId)
                                putExtra(android.media.audiofx.AudioEffect.EXTRA_PACKAGE_NAME, context.packageName)
                            }
                            context.sendBroadcast(openIntent)
                        } catch (e: Exception) {
                            android.util.Log.e("PlaybackController", "Failed to broadcast open eq session", e)
                        }
                    }"""

content = content.replace(
    '                    override fun onIsPlayingChanged(isPlaying: Boolean) {',
    new_listener + '\n\n                    override fun onIsPlayingChanged(isPlaying: Boolean) {'
)

with open('app/src/main/java/com/example/PlaybackController.kt', 'w') as f:
    f.write(content)
