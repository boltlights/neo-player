with open('app/src/main/java/com/example/util/AudioQualityUtil.kt', 'r') as f:
    content = f.read()

import re

# We will replace from "        // Use MediaMetadataRetriever for other tracks" to "        val displayExt = if (extension.isNotEmpty()) extension else "AUDIO"" or something similar

new_code = """        // Use Jaudiotagger for other tracks if file exists
        try {
            if (file.exists()) {
                val audioFile = org.jaudiotagger.audio.AudioFileIO.read(file)
                val header = audioFile.audioHeader
                
                val format = if (extension.isNotEmpty()) extension else header.encodingType.uppercase(Locale.US)
                val parts = mutableListOf<String>()
                parts.add(format)
                
                val bitrateKbps = header.bitRate.toLongOrNull()
                if (bitrateKbps != null && bitrateKbps > 0) {
                    parts.add("${bitrateKbps}KBPS")
                }
                
                val sampleRateDouble = header.sampleRate.toDoubleOrNull()
                val rateKhz = if (sampleRateDouble != null) {
                    String.format(Locale.US, "%.1f", sampleRateDouble / 1000.0)
                } else {
                    if (extension == "FLAC") "48.0" else "44.1"
                }
                
                val bits = header.bitsPerSample
                val bitsStr = if (bits > 0) bits.toString() else if (extension == "FLAC") "24" else "16"
                
                parts.add("$bitsStr-BIT / ${rateKhz}KHZ")
                return parts.joinToString(" • ")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        // Fallback to MediaMetadataRetriever
        val retriever = MediaMetadataRetriever()"""

content = content.replace("        // Use MediaMetadataRetriever for other tracks\n        val retriever = MediaMetadataRetriever()", new_code)

with open('app/src/main/java/com/example/util/AudioQualityUtil.kt', 'w') as f:
    f.write(content)
