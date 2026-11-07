import re

with open('app/src/main/java/com/example/util/AudioQualityUtil.kt', 'r') as f:
    content = f.read()

# Add getReplayGainPeakFromMetadata
new_func = '''    suspend fun getReplayGainPeakFromMetadata(metadata: Metadata): Float? {
        for (i in 0 until metadata.length()) {
            val entry = metadata.get(i)
            val className = entry.javaClass.simpleName
            if (className.endsWith("TextInformationFrame")) {
                try {
                    val descField = entry.javaClass.getField("description")
                    val description = descField.get(entry) as? String
                    if ("REPLAYGAIN_TRACK_PEAK".equals(description, ignoreCase = true)) {
                        var valueStr: String? = null
                        try {
                            val valuesField = entry.javaClass.getField("values")
                            val values = valuesField.get(entry) as? List<*>
                            valueStr = values?.firstOrNull() as? String
                        } catch (e: Exception) {
                            try {
                                val valueField = entry.javaClass.getField("value")
                                valueStr = valueField.get(entry) as? String
                            } catch (ex: Exception) {}
                        }
                        
                        if (!valueStr.isNullOrBlank()) {
                            return valueStr.toFloatOrNull()
                        }
                    }
                } catch (e: Exception) {}
            } else if (className.endsWith("VorbisComment")) {
                try {
                    val keyField = entry.javaClass.getField("key")
                    val key = keyField.get(entry) as? String
                    if ("REPLAYGAIN_TRACK_PEAK".equals(key, ignoreCase = true)) {
                        val valueField = entry.javaClass.getField("value")
                        val valueStr = valueField.get(entry) as? String
                        if (!valueStr.isNullOrBlank()) {
                            return valueStr.toFloatOrNull()
                        }
                    }
                } catch (e: Exception) {}
            }
        }
        return null
    }

    /**
     * Bit-Perfect/Mixer Attributes (Android 14+):'''

content = content.replace('    /**\n     * Bit-Perfect/Mixer Attributes (Android 14+):', new_func)

# Fix quality classification logic in AudioQualityUtil (Wait, the user says the logic is in TrackPropertiesModal.kt)
# Let's check TrackPropertiesModal.kt

with open('app/src/main/java/com/example/util/AudioQualityUtil.kt', 'w') as f:
    f.write(content)
