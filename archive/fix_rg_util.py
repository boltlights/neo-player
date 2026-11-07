with open('app/src/main/java/com/example/util/AudioQualityUtil.kt', 'r') as f:
    content = f.read()

new_func = '''    suspend fun getReplayGainFromAudioFile(context: Context, track: TrackEntity): Float? = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        try {
            val path = track.filePath
            if (!path.isNullOrEmpty() && !path.startsWith("content://")) {
                val f = org.jaudiotagger.audio.AudioFileIO.read(File(path))
                val tag = f.tag ?: return@withContext null
                val rg = tag.getFirst(org.jaudiotagger.tag.FieldKey.REPLAYGAIN_TRACK_GAIN)
                if (!rg.isNullOrBlank()) {
                    return@withContext rg.replace("dB", "", ignoreCase = true).trim().toFloatOrNull()
                }
            } else if (track.sourceUri != null) {
                // For SAF, jaudiotagger can't read directly unless we copy it.
                // We'll skip for now or rely on ExoPlayer during playback
            }
        } catch(e: Exception) {}
        return@withContext null
    }

    suspend fun getQualityString'''

content = content.replace('    suspend fun getQualityString', new_func)

with open('app/src/main/java/com/example/util/AudioQualityUtil.kt', 'w') as f:
    f.write(content)

