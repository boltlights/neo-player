with open('app/src/main/java/com/example/PlaybackController.kt', 'r') as f:
    content = f.read()

old_metadata = '''                    override fun onMetadata(metadata: androidx.media3.common.Metadata) {
                        super.onMetadata(metadata)
                        val gainDb = com.example.util.AudioQualityUtil.getReplayGainFromMetadata(metadata)
                        if (gainDb != null) {
                            replayGainProcessor.trackGainDb = gainDb
                            android.util.Log.d("PlaybackController", "ReplayGain Processor activated: ${gainDb}dB")
                        } else {
                            replayGainProcessor.trackGainDb = 0f
                        }
                    }'''

new_metadata = '''                    override fun onMetadata(metadata: androidx.media3.common.Metadata) {
                        super.onMetadata(metadata)
                        playScope.launch {
                            val gainDb = com.example.util.AudioQualityUtil.getReplayGainFromMetadata(metadata)
                            val peak = com.example.util.AudioQualityUtil.getReplayGainPeakFromMetadata(metadata)
                            
                            if (gainDb != null) {
                                replayGainProcessor.trackGainDb = gainDb
                                replayGainProcessor.trackPeak = peak ?: 1.0f
                                android.util.Log.d("PlaybackController", "ReplayGain Processor activated: ${gainDb}dB, peak: ${peak ?: 1.0f}")
                            } else {
                                replayGainProcessor.trackGainDb = 0f
                                replayGainProcessor.trackPeak = 1.0f
                            }
                        }
                    }'''

content = content.replace(old_metadata, new_metadata)
with open('app/src/main/java/com/example/PlaybackController.kt', 'w') as f:
    f.write(content)
