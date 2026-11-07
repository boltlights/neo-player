with open('app/src/main/java/com/example/TrackPropertiesModal.kt', 'r') as f:
    content = f.read()

content = content.replace(
'''    props["Volume Leveling"] = "N/A"
    
    // Attempt to read replay gain
    try {
        val metadata = com.example.util.MetadataExtractor.extract(context, track.sourceUri ?: track.filePath, track.filePath)
        
        // Since we already have jaudiotagger via MetadataExtractor, we can use it to get replay gain.
        // Wait, MetadataExtractor returns ExtractedMetadata, which doesn't have replayGain.
        // We'll have to read ExoPlayer's metadata here if we want it, or just use MediaMetadataRetriever
    } catch(e: Exception) {}''',
'''    val rg = com.example.util.AudioQualityUtil.getReplayGainFromAudioFile(context, track)
    if (rg != null) {
        props["Volume Leveling"] = if (rg > 0) "+${rg} dB" else "${rg} dB"
    } else {
        props["Volume Leveling"] = "Not Available"
    }'''
)

with open('app/src/main/java/com/example/TrackPropertiesModal.kt', 'w') as f:
    f.write(content)
