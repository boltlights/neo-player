#!/bin/bash
cat << 'INNER_EOF' > script.awk
/val totalDurationMs = tracksInPlaylist.sumOf \{ it.durationMs \}/ {
    print "                    val totalDurationMs = androidx.compose.runtime.remember(tracksInPlaylist) { tracksInPlaylist.sumOf { it.durationMs } }"
    next
}
/val songCount = crossRefs.count \{ it.playlistId == playlist.id \}/ {
    print "    val songCount = androidx.compose.runtime.remember(crossRefs, playlist.id) { crossRefs.count { it.playlistId == playlist.id } }"
    next
}
/val tracksInPlaylist = allTracks.filter \{ track ->/ {
    print "    var tracksInPlaylist by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<List<TrackEntity>>(emptyList()) }"
    print "    androidx.compose.runtime.LaunchedEffect(crossRefs, allTracks, playlist.id) {"
    print "        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {"
    print "            val playlistTrackIds = crossRefs.filter { it.playlistId == playlist.id }.map { it.trackId }.toSet()"
    print "            val filtered = allTracks.filter { playlistTrackIds.contains(it.id) }.take(4)"
    print "            tracksInPlaylist = filtered"
    print "        }"
    print "    }"
    
    skip = 1
    next
}
skip && /crossRefs.any \{ it.playlistId == playlist.id && it.trackId == track.id \}/ {
    skip = 2
    next
}
skip == 2 && /^\s*\}/ {
    skip = 0
    next
}
!skip { print }
INNER_EOF
awk -f script.awk app/src/main/java/com/example/ui/screens/PlaylistsScreen.kt > tmp.kt && mv tmp.kt app/src/main/java/com/example/ui/screens/PlaylistsScreen.kt
