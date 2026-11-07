#!/bin/bash
cat << 'INNER_EOF' > script.awk
/private fun saveCurrentPlaybackState/ {
    print
    print "        if (!isRestoreCompleted) return"
    print "        val queue = playbackController.currentPlaybackList.value"
    print "        val track = playbackController.currentTrack.value"
    print "        val currentTrackId = track?.id ?: -1"
    print "        val pos = playbackController.currentPosition.value"
    print "        val isShuffle = playbackController.isShuffle.value"
    print "        "
    print "        val queueHash = queue.hashCode()"
    print "        if (queueHash != lastSavedQueueHash) {"
    print "            lastSavedQueueStr = queue.joinToString(\",\") { it.id.toString() }"
    print "            lastSavedQueueHash = queueHash"
    print "            viewModelScope.launch {"
    print "                repository.savePlaybackState("
    print "                    PlaybackStateEntity("
    print "                        id = 0,"
    print "                        queueTrackIds = lastSavedQueueStr,"
    print "                        currentTrackId = currentTrackId,"
    print "                        currentPositionMs = pos,"
    print "                        isShuffleEnabled = isShuffle"
    print "                    )"
    print "                )"
    print "            }"
    print "        } else {"
    print "            viewModelScope.launch {"
    print "                repository.updatePlaybackPosition(pos, currentTrackId, isShuffle)"
    print "            }"
    print "        }"
    
    # skip original lines
    while (getline > 0) {
        if ($0 ~ /viewModelScope.launch \{/ && prev_blank) {
            print
            break
        }
        prev_blank = ($0 ~ /^$/)
    }
    next
}
{ print }
INNER_EOF
awk -f script.awk app/src/main/java/com/example/MusicViewModel.kt > tmp.kt && mv tmp.kt app/src/main/java/com/example/MusicViewModel.kt
