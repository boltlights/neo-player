#!/bin/bash
cat << 'INNER_EOF' > script.awk
/fun importAudioFolder/ {
    print
    getline
    print
    getline
    print "        if (!includedFolders.value.contains(uriStr)) {"
    print "            viewModelScope.launch {"
    print "                settingsRepository.addIncludedFolder(uriStr)"
    print "                scanMediaStore(context, onCompleted)"
    print "            }"
    print "            return"
    print "        }"
    print "        scanMediaStore(context, onCompleted)"
    print "    }"
    
    # skip the messed up lines
    while (getline > 0) {
        if ($0 ~ /private var lastSavedQueueHash = 0/) {
            print
            break
        }
    }
    next
}
{ print }
INNER_EOF
awk -f script.awk app/src/main/java/com/example/MusicViewModel.kt > tmp.kt && mv tmp.kt app/src/main/java/com/example/MusicViewModel.kt
