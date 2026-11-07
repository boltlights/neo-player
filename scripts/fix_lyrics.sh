#!/bin/bash
cat << 'INNER_EOF' > script.awk
/lyricsListState.animateScrollToItem\(targetScrollIndex\)/ {
    print "                        if (!lyricsListState.isScrollInProgress) {"
    print "                            lyricsListState.animateScrollToItem(targetScrollIndex)"
    print "                        }"
    next
}
{ print }
INNER_EOF
awk -f script.awk app/src/main/java/com/example/ui/screens/FocusedNowPlayingScreen.kt > tmp.kt && mv tmp.kt app/src/main/java/com/example/ui/screens/FocusedNowPlayingScreen.kt
