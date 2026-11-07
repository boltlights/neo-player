with open('app/src/main/java/com/example/util/AudioQualityUtil.kt', 'r') as f:
    content = f.read()

import re
content = re.sub(r'suspend fun getReplayGainFromAudioFile.*?return@withContext null\n    }', '', content, flags=re.DOTALL)

with open('app/src/main/java/com/example/util/AudioQualityUtil.kt', 'w') as f:
    f.write(content)

with open('app/src/main/java/com/example/TrackPropertiesModal.kt', 'r') as f:
    content = f.read()

content = content.replace(
'''    val rg = com.example.util.AudioQualityUtil.getReplayGainFromAudioFile(context, track)
    if (rg != null) {
        props["Volume Leveling"] = if (rg > 0) "+${rg} dB" else "${rg} dB"
    } else {
        props["Volume Leveling"] = "Not Available"
    }''',
'''    props["Volume Leveling"] = "Not Available"'''
)

with open('app/src/main/java/com/example/TrackPropertiesModal.kt', 'w') as f:
    f.write(content)

