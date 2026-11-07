#!/bin/bash
cat << 'INNER_EOF' > script.awk
/viewModelScope.launch \{/ {
    if (in_save) {
        if (!observe_started) {
            observe_started = 1
            print "    }"
            print ""
            print "    private fun observeAndSaveQueue() {"
        }
        print
        next
    }
}
/private fun saveCurrentPlaybackState\(\) \{/ {
    in_save = 1
}
/private suspend fun restoreQueueState\(\) \{/ {
    in_save = 0
}
{ print }
INNER_EOF
awk -f script.awk app/src/main/java/com/example/MusicViewModel.kt > tmp.kt && mv tmp.kt app/src/main/java/com/example/MusicViewModel.kt
