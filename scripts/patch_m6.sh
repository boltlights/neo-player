#!/bin/bash
awk '
BEGIN { output = 1 }
/private fun saveCurrentPlaybackState()/ {
    print "    private var lastSavedQueueHash = 0"
    print "    private var lastSavedQueueStr = \"\""
    print ""
    print "    private fun saveCurrentPlaybackState() {"
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
    print "        }"
    print "        "
    print "        viewModelScope.launch {"
    print "            repository.savePlaybackState("
    print "                PlaybackStateEntity("
    print "                    id = 0,"
    print "                    queueTrackIds = lastSavedQueueStr,"
    print "                    currentTrackId = currentTrackId,"
    print "                    currentPositionMs = pos,"
    print "                    isShuffleEnabled = isShuffle"
    print "                )"
    print "            )"
    print "        }"
    output = 0
}
/private fun observeAndSaveQueue()/ {
    output = 1
}
{
    if (output && $0 !~ /private fun saveCurrentPlaybackState()/) print $0
}
' app/src/main/java/com/example/MusicViewModel.kt > app/src/main/java/com/example/MusicViewModel.kt.tmp
mv app/src/main/java/com/example/MusicViewModel.kt.tmp app/src/main/java/com/example/MusicViewModel.kt
