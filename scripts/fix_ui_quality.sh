#!/bin/bash
cat << 'INNER_EOF' > script.awk
/val qualityText = remember\(track\) \{ AudioQualityUtil.getQualityString\(context, track\) \}/ {
    print "                    var qualityText by remember(track) { androidx.compose.runtime.mutableStateOf(\"Loading...\") }"
    print "                    androidx.compose.runtime.LaunchedEffect(track) {"
    print "                        qualityText = AudioQualityUtil.getQualityString(context, track)"
    print "                    }"
    next
}
{ print }
INNER_EOF
awk -f script.awk app/src/main/java/com/example/ui/screens/FocusedNowPlayingScreen.kt > tmp.kt && mv tmp.kt app/src/main/java/com/example/ui/screens/FocusedNowPlayingScreen.kt
