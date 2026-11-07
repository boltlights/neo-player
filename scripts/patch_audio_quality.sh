#!/bin/bash
cat << 'INNER_EOF' > script.awk
/object AudioQualityUtil \{/ {
    print
    print "    private val reflectionCache = mutableMapOf<String, java.lang.reflect.Method>()"
    print "    private var classCache: Class<*>? = null"
    next
}
/fun getQualityString/ {
    print "    suspend fun getQualityString(context: Context, track: TrackEntity): String = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {"
    in_get_quality = 1
    next
}
in_get_quality && /return "MP3 • 16-BIT \/ 44.1KHZ"/ {
    print "        return@withContext \"MP3 • 16-BIT / 44.1KHZ\""
    next
}
in_get_quality && /return "\$displayExt • \$bits-BIT \/ \$sampleRateKHZ"/ {
    print "            return@withContext \"$displayExt • $bits-BIT / $sampleRateKHZ\""
    next
}
in_get_quality && /return "\$displayExt • 16-BIT \/ 44.1KHZ"/ {
    print "        return@withContext \"$displayExt • 16-BIT / 44.1KHZ\""
    next
}
in_get_quality && /^    \}/ {
    print "    }"
    in_get_quality = 0
    next
}
/val getSupportedMixerAttributesMethod = audioManager.javaClass.getMethod\(/ {
    print "                        val getSupportedMixerAttributesMethod = reflectionCache.getOrPut(\"getSupportedMixerAttributes\") {"
    print "                            audioManager.javaClass.getMethod(\"getSupportedMixerAttributes\", android.media.AudioDeviceInfo::class.java)"
    print "                        }"
    getline
    getline
    next
}
/val getMixerBehaviorMethod = mixer.javaClass.getMethod\("getMixerBehavior"\)/ {
    print "                                    val getMixerBehaviorMethod = reflectionCache.getOrPut(\"getMixerBehavior\") {"
    print "                                        mixer.javaClass.getMethod(\"getMixerBehavior\")"
    print "                                    }"
    next
}
/val builderClass = Class.forName\("android.media.AudioMixerAttributes\\\$Builder"\)/ {
    print "                            val builderClass = classCache ?: Class.forName(\"android.media.AudioMixerAttributes\\\$Builder\").also { classCache = it }"
    next
}
/val setMixerBehaviorMethod = builderClass.getMethod\("setMixerBehavior", Int::class.javaPrimitiveType\)/ {
    print "                            val setMixerBehaviorMethod = reflectionCache.getOrPut(\"setMixerBehavior\") {"
    print "                                builderClass.getMethod(\"setMixerBehavior\", Int::class.javaPrimitiveType)"
    print "                            }"
    next
}
/val buildMethod = builderClass.getMethod\("build"\)/ {
    print "                            val buildMethod = reflectionCache.getOrPut(\"build\") {"
    print "                                builderClass.getMethod(\"build\")"
    print "                            }"
    next
}
/val setPreferredMixerAttributesMethod = audioManager.javaClass.getMethod\(/ {
    print "                            val setPreferredMixerAttributesMethod = reflectionCache.getOrPut(\"setPreferredMixerAttributes\") {"
    print "                                audioManager.javaClass.getMethod("
    print "                                    \"setPreferredMixerAttributes\","
    print "                                    android.media.AudioAttributes::class.java,"
    print "                                    android.media.AudioDeviceInfo::class.java,"
    print "                                    Class.forName(\"android.media.AudioMixerAttributes\")"
    print "                                )"
    print "                            }"
    getline
    getline
    getline
    getline
    getline
    next
}
/val clearPreferredMixerAttributesMethod = audioManager.javaClass.getMethod\(/ {
    print "                            val clearPreferredMixerAttributesMethod = reflectionCache.getOrPut(\"clearPreferredMixerAttributes\") {"
    print "                                audioManager.javaClass.getMethod("
    print "                                    \"clearPreferredMixerAttributes\","
    print "                                    android.media.AudioAttributes::class.java,"
    print "                                    android.media.AudioDeviceInfo::class.java"
    print "                                )"
    print "                            }"
    getline
    getline
    getline
    getline
    next
}
{ print }
INNER_EOF
awk -f script.awk app/src/main/java/com/example/util/AudioQualityUtil.kt > tmp.kt && mv tmp.kt app/src/main/java/com/example/util/AudioQualityUtil.kt
