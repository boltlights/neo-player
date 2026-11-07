#!/bin/bash
sed -i 's/return parts.joinToString(" • ")/return@withContext parts.joinToString(" • ")/g' app/src/main/java/com/example/util/AudioQualityUtil.kt
sed -i 's/return "MP3 • 16-BIT \/ 44.1KHZ"/return@withContext "MP3 • 16-BIT \/ 44.1KHZ"/g' app/src/main/java/com/example/util/AudioQualityUtil.kt
sed -i 's/return cleanVal.toFloatOrNull()/return@withContext cleanVal.toFloatOrNull()/g' app/src/main/java/com/example/util/AudioQualityUtil.kt
