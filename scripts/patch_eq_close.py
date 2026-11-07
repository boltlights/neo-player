with open('app/src/main/java/com/example/PlaybackController.kt', 'r') as f:
    content = f.read()

close_code = """        if (currentAudioSessionId != 0) {
            try {
                val closeIntent = Intent(android.media.audiofx.AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION).apply {
                    putExtra(android.media.audiofx.AudioEffect.EXTRA_AUDIO_SESSION, currentAudioSessionId)
                    putExtra(android.media.audiofx.AudioEffect.EXTRA_PACKAGE_NAME, context.packageName)
                }
                context.sendBroadcast(closeIntent)
                currentAudioSessionId = 0
            } catch (e: Exception) {
                // ignore
            }
        }
        exoPlayer?.release()"""

content = content.replace('        exoPlayer?.release()', close_code)

with open('app/src/main/java/com/example/PlaybackController.kt', 'w') as f:
    f.write(content)
