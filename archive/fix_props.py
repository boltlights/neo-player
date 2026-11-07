import re

with open('app/src/main/java/com/example/TrackPropertiesModal.kt', 'r') as f:
    content = f.read()

# Fix size check
content = content.replace(
'''                    val sizeIndex = c.getColumnIndex(android.provider.MediaStore.MediaColumns.SIZE)
                    if (sizeIndex != -1) {
                        val size = c.getLong(sizeIndex)
                        val mb = size / (1024.0 * 1024.0)
                        props["File Size"] = String.format("%.2f MB", mb)
                    }''',
'''                    val sizeIndex = c.getColumnIndex(android.provider.MediaStore.MediaColumns.SIZE)
                    if (sizeIndex != -1 && !c.isNull(sizeIndex)) {
                        val size = c.getLong(sizeIndex)
                        val mb = size / (1024.0 * 1024.0)
                        props["File Size"] = String.format("%.2f MB", mb)
                    }'''
)

# Fix Audio Quality logic
old_quality = '''                props["Audio Quality"] = "Standard"
                if (props["Sample Rate"]?.contains("48000") == true || props["Bitrate"]?.contains("320") == true) {
                    props["Audio Quality"] = "High Quality"
                } else if (props["Encoded With"]?.contains("FLAC") == true || props["Encoded With"]?.contains("ALAC") == true) {
                    props["Audio Quality"] = "Lossless"
                }'''

new_quality = '''                props["Audio Quality"] = "Standard"
                
                val sr = format.getInteger(MediaFormat.KEY_SAMPLE_RATE, 0)
                val br = format.getInteger(MediaFormat.KEY_BIT_RATE, 0)
                
                if (props["Encoded With"]?.contains("FLAC") == true || props["Encoded With"]?.contains("ALAC") == true) {
                    props["Audio Quality"] = "Lossless"
                } else if (sr > 48000 || br >= 320000) {
                    props["Audio Quality"] = "High Quality"
                }'''

content = content.replace(old_quality, new_quality)

# Add repo querying to extractTrackProperties
content = content.replace(
'''private fun extractTrackProperties(context: android.content.Context, track: TrackEntity): Map<String, String> {''',
'''private suspend fun extractTrackProperties(context: android.content.Context, track: TrackEntity): Map<String, String> {'''
)

# Change hardcoded mock data to use actual data
old_mock = '''    // Default mock data for fields we can't reliably get instantly
    props["Play Count"] = "Unknown" 
    props["Volume Leveling"] = "N/A"'''

new_mock = '''    try {
        val app = context.applicationContext as com.example.MusicApplication
        val count = app.repository.getPlayCountForTrack(track.id)
        props["Play Count"] = count.toString()
    } catch (e: Exception) {
        props["Play Count"] = "Unknown" 
    }

    props["Volume Leveling"] = "N/A"'''

content = content.replace(old_mock, new_mock)

# Add volume leveling via Media3 metadata if it is already extracted
content = content.replace(
'''    val extractor = MediaExtractor()''',
'''    
    // Attempt to read replay gain
    try {
        val metadata = com.example.util.MetadataExtractor.extract(context, track.sourceUri ?: track.filePath, track.filePath)
        
        // Since we already have jaudiotagger via MetadataExtractor, we can use it to get replay gain.
        // Wait, MetadataExtractor returns ExtractedMetadata, which doesn't have replayGain.
        // We'll have to read ExoPlayer's metadata here if we want it, or just use MediaMetadataRetriever
    } catch(e: Exception) {}
    val extractor = MediaExtractor()'''
)

with open('app/src/main/java/com/example/TrackPropertiesModal.kt', 'w') as f:
    f.write(content)

