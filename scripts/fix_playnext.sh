#!/bin/bash
cat << 'INNER_EOF' > script.awk
/fun playNext\(tracks: List<TrackEntity>\)/ {
    inPlayNextTracks = 1
}
inPlayNextTracks && /tracks.forEach \{ track ->/ {
    print "            val currentIndex = exoPlayer?.currentMediaItemIndex ?: -1"
    print "            val filteredTracks = tracks.filter { track ->"
    print "                val oldIndex = currentList.indexOfFirst { it.id == track.id }"
    print "                if (oldIndex != -1) {"
    print "                    if (oldIndex != currentIndex) {"
    print "                        exoPlayer?.removeMediaItem(oldIndex)"
    print "                        currentList.removeAt(oldIndex)"
    print "                        true"
    print "                    } else {"
    print "                        false // Do not add currently playing track to queue again"
    print "                    }"
    print "                } else {"
    print "                    true"
    print "                }"
    print "            }"
    print "            val index = currentList.indexOfFirst { it.id == current.id }"
    print "            val mediaItemsToAdd = filteredTracks.map { createMediaItem(it) }"
    print "            if (index != -1) {"
    print "                currentList.addAll(index + 1, filteredTracks)"
    print "                exoPlayer?.addMediaItems(index + 1, mediaItemsToAdd)"
    print "            } else {"
    print "                currentList.addAll(0, filteredTracks)"
    print "                exoPlayer?.addMediaItems(0, mediaItemsToAdd)"
    print "            }"
    print "            _currentPlaybackList.value = currentList"
    print "        }"
    print "    }"
    
    # skip lines until the next function
    skip_until = 1
    next
}
inPlayNextTracks && skip_until {
    if ($0 ~ /fun playNext\(track: TrackEntity\)/) {
        inPlayNextTracks = 0
        skip_until = 0
        print
    }
    next
}

/fun playNext\(track: TrackEntity\)/ {
    inPlayNextTrack = 1
}
inPlayNextTrack && /val oldIndex = _currentPlaybackList.value.indexOfFirst \{ it.id == track.id \}/ {
    print "            val currentIndex = exoPlayer?.currentMediaItemIndex ?: -1"
    print "            val oldIndex = _currentPlaybackList.value.indexOfFirst { it.id == track.id }"
    print "            if (oldIndex != -1 && oldIndex != currentIndex) {"
    print "                exoPlayer?.removeMediaItem(oldIndex)"
    print "                currentList.remove(track)"
    print "            }"
    print "            if (oldIndex != currentIndex) {"
    print "                val index = currentList.indexOfFirst { it.id == current.id }"
    print "                if (index != -1) {"
    print "                    currentList.add(index + 1, track)"
    print "                    exoPlayer?.addMediaItem(index + 1, mediaItem)"
    print "                } else {"
    print "                    currentList.add(0, track)"
    print "                    exoPlayer?.addMediaItem(0, mediaItem)"
    print "                }"
    print "            }"
    print "            _currentPlaybackList.value = currentList"
    print "        }"
    print "    }"
    
    skip_until_end = 1
    next
}
inPlayNextTrack && skip_until_end {
    if ($0 ~ /fun playLater\(track: TrackEntity\)/ || $0 ~ /fun addTrackToPlaylist/) {
        inPlayNextTrack = 0
        skip_until_end = 0
        print
    }
    next
}
{ print }
INNER_EOF
awk -f script.awk app/src/main/java/com/example/PlaybackController.kt > tmp.kt && mv tmp.kt app/src/main/java/com/example/PlaybackController.kt
